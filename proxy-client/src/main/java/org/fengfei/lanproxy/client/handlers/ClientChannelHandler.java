package org.fengfei.lanproxy.client.handlers;

import org.fengfei.lanproxy.client.ClientChannelMannager;
import org.fengfei.lanproxy.client.listener.ChannelStatusListener;
import org.fengfei.lanproxy.client.listener.ProxyChannelBorrowListener;
import org.fengfei.lanproxy.common.Config;
import org.fengfei.lanproxy.protocol.Constants;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author fengfei
 */
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private static Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private Bootstrap bootstrap;

    private Bootstrap proxyBootstrap;

    private ChannelStatusListener channelStatusListener;

    public ClientChannelHandler(Bootstrap bootstrap, Bootstrap proxyBootstrap, ChannelStatusListener channelStatusListener) {
        this.bootstrap = bootstrap;
        this.proxyBootstrap = proxyBootstrap;
        this.channelStatusListener = channelStatusListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {
        logger.debug("recieved proxy message, type is {}", proxyMessage.getType());
        switch (proxyMessage.getType()) {
            case ProxyMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.P_TYPE_TRANSFER:
                handleTransferMessage(ctx, proxyMessage);
                break;
            default:
                break;
        }
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        //向后端服务发送数据
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null) {
            ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
            buf.writeBytes(proxyMessage.getData());
            logger.debug("write data to real server, {}", realServerChannel);
            realServerChannel.writeAndFlush(buf);
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        //获取与服务的真实连接并关闭
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        logger.debug("handleDisconnectMessage, {}", realServerChannel);
        if (realServerChannel != null) {
            //解除绑定,通过服务的真实连接发送一条空消息
            ctx.channel().attr(Constants.NEXT_CHANNEL).remove();
            //归还代理服务的连接通道
            ClientChannelMannager.returnProxyChanel(ctx.channel());
            realServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        final Channel cmdChannel = ctx.channel();
        final String userId = proxyMessage.getUri();
        String[] serverInfo = new String(proxyMessage.getData()).split(":");
        String ip = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
        //与后端服务器建立真实连接
        bootstrap.connect(ip, port).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {

                // 连接后端服务器成功
                if (future.isSuccess()) {
                    //与真实服务器通信的channel
                    final Channel realServerChannel = future.channel();
                    logger.debug("connect realserver success, {}", realServerChannel);

                    realServerChannel.config().setOption(ChannelOption.AUTO_READ, false);

                    // 获取连接
                    ClientChannelMannager.borrowProxyChanel(proxyBootstrap, new ProxyChannelBorrowListener() {

                        @Override
                        public void success(Channel channel) {
                            // 连接双向绑定
                            channel.attr(Constants.NEXT_CHANNEL).set(realServerChannel);
                            realServerChannel.attr(Constants.NEXT_CHANNEL).set(channel);

                            // 远程绑定，向代理服务器发送连接成功消息
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
//                            proxyMessage.setUri(userId + "@" + Config.getInstance().getProjectSn());
//                            proxyMessage.setUri(userId + "@" + "123456");
                            proxyMessage.setUri(userId + "@" + Config.getInstance().getStringValue("client.key"));
                            channel.writeAndFlush(proxyMessage);

                            realServerChannel.config().setOption(ChannelOption.AUTO_READ, true);
                            //保存客户端id与真实服务连接通道关系保存客户端id与真实服务连接通道关系
                            ClientChannelMannager.addRealServerChannel(userId, realServerChannel);
                            //真实服务连接通道上设置userId属性
                            ClientChannelMannager.setRealServerChannelUserId(realServerChannel, userId);
                        }

                        @Override
                        public void error(Throwable cause) {
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                            proxyMessage.setUri(userId);
                            cmdChannel.writeAndFlush(proxyMessage);
                        }
                    });

                } else {
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                    proxyMessage.setUri(userId);
                    cmdChannel.writeAndFlush(proxyMessage);
                }
            }
        });
    }

    @Override
    // 在{@link Channel}的可写状态改变后调用。你可以用{@linkChannel# isWritable()}。
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null) {
            realServerChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 如果是控制连接关闭了，即与代理服务器的连接非borrow连接，此时需要清理所有与后端的真实连接
        if (ClientChannelMannager.getCmdChannel() == ctx.channel()) {
            ClientChannelMannager.setCmdChannel(null);
            //清理所有与后端的真实连接,并发起重连
            ClientChannelMannager.clearRealServerChannels();
            channelStatusListener.channelInactive(ctx);
        } else {
            //如果仅仅是与代理服务器的borrow连接关闭，关闭绑定的真实连接即可
            Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
            if (realServerChannel != null && realServerChannel.isActive()) {
                realServerChannel.close();
            }
        }
        //在borrow连接池中清理此连接
        ClientChannelMannager.removeProxyChanel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }

}