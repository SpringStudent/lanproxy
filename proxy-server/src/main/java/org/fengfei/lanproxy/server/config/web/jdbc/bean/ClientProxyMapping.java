package org.fengfei.lanproxy.server.config.web.jdbc.bean;

/**
 * @author zhouning
 * @date 2021/10/15 11:18
 */
public class ClientProxyMapping {
    /**
     * 主键
     */
    private String id;
    /**
     * 客户端唯一标识
     */
    private String clientKey;
    /**
     * 代理服务器端口
     */
    private Integer inetPort;

    /**
     * 需要代理的网络信息（代理客户端能够访问），格式 192.168.1.99:80 (必须带端口)
     */
    private String lan;
    /**
     * 备注名称
     */
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public Integer getInetPort() {
        return inetPort;
    }

    public void setInetPort(Integer inetPort) {
        this.inetPort = inetPort;
    }

    public String getLan() {
        return lan;
    }

    public void setLan(String lan) {
        this.lan = lan;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
