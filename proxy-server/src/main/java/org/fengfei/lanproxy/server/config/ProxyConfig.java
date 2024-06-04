package org.fengfei.lanproxy.server.config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbutils.DbUtils;
import org.fengfei.lanproxy.common.Config;
import org.fengfei.lanproxy.server.config.web.dao.ClientDao;
import org.fengfei.lanproxy.server.config.web.jdbc.C3P0Utils;
import org.fengfei.lanproxy.server.config.web.jdbc.bean.Client;
import org.fengfei.lanproxy.server.config.web.jdbc.bean.ClientProxyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server config
 *
 * @author fengfei
 */
public class ProxyConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置文件为config.json
     */
    public static final String CONFIG_FILE;

    private static Logger logger = LoggerFactory.getLogger(ProxyConfig.class);

    static {

        // 代理配置信息存放在用户根目录下
        String dataPath = System.getProperty("user.home") + "/" + ".lanproxy/";
        File file = new File(dataPath);
        if (!file.isDirectory()) {
            file.mkdir();
        }

        CONFIG_FILE = dataPath + "/config.json";
    }

    /**
     * 代理服务器绑定主机host
     */
    private String serverBind;

    /**
     * 代理服务器与代理客户端通信端口
     */
    private Integer serverPort;

    /**
     * 配置服务绑定主机host
     */
    private String configServerBind;

    /**
     * 配置服务端口
     */
    private Integer configServerPort;

    /**
     * 数据库连接驱动
     */
    private String jdbcDriverClassName;

    /**
     * 数据库地址
     */
    private String jdbcUrl;

    /**
     * 数据库连接用户名
     */
    private String jdbcUsername;

    /**
     * 数据库连接密码
     */
    private String jdbcPassword;

    /**
     * 代理客户端，支持多个客户端
     */
    private List<Client> clients;

    /**
     * 更新配置后保证在其他线程即时生效
     */
    private static ProxyConfig instance = new ProxyConfig();
    ;

    /**
     * 代理服务器为各个代理客户端（key）开启对应的端口列表（value）
     */
    private volatile Map<String, List<Integer>> clientInetPortMapping = new HashMap<String, List<Integer>>();

    /**
     * 代理服务器上的每个对外端口（key）对应的代理客户端背后的真实服务器信息（value）
     */
    private volatile Map<Integer, String> inetPortLanInfoMapping = new HashMap<Integer, String>();

    /**
     * 配置变化监听器
     */
    private List<ConfigChangedListener> configChangedListeners = new ArrayList<ConfigChangedListener>();

    private ProxyConfig() {

        // 代理服务器主机和端口配置初始化
        this.serverPort = Config.getInstance().getIntValue("server.port");
        this.serverBind = Config.getInstance().getStringValue("server.bind", "0.0.0.0");

        // 配置服务器主机和端口配置初始化
        this.configServerPort = Config.getInstance().getIntValue("config.server.port");
        this.configServerBind = Config.getInstance().getStringValue("config.server.bind", "0.0.0.0");

        // 配置服务器管理员登录认证信息
        this.jdbcDriverClassName = Config.getInstance().getStringValue("jdbc.driverClassName");
        this.jdbcUrl = Config.getInstance().getStringValue("jdbc.url");
        this.jdbcUsername = Config.getInstance().getStringValue("jdbc.username");
        this.jdbcPassword = Config.getInstance().getStringValue("jdbc.password");

        logger.info(
                "config init serverBind {}, serverPort {}, configServerBind {}, configServerPort {}, jdbcDriverClassName {}, jdbcUrl {}, jdbcUsername {}, jdbcPassword {}",
                serverBind, serverPort, configServerBind, configServerPort, jdbcDriverClassName, jdbcUrl, jdbcUsername, jdbcPassword);

        update(null, true);
    }

    public Integer getServerPort() {
        return this.serverPort;
    }

    public String getServerBind() {
        return serverBind;
    }

    public void setServerBind(String serverBind) {
        this.serverBind = serverBind;
    }

    public String getConfigServerBind() {
        return configServerBind;
    }

    public void setConfigServerBind(String configServerBind) {
        this.configServerBind = configServerBind;
    }

    public Integer getConfigServerPort() {
        return configServerPort;
    }

    public void setConfigServerPort(Integer configServerPort) {
        this.configServerPort = configServerPort;
    }

    public String getJdbcDriverClassName() {
        return jdbcDriverClassName;
    }

    public void setJdbcDriverClassName(String jdbcDriverClassName) {
        this.jdbcDriverClassName = jdbcDriverClassName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public List<Client> getClients() {
        return clients;
    }

    /**
     * 解析配置文件
     */
    public void update(List<Client> clients, boolean isInit) {
        //启动初始化从数据库读取配置
        if (isInit) {
            clients = ClientDao.getClient();
            List<ClientProxyMapping> clientProxyMappings = ClientDao.getClientProxyMapping();
            Map<String, List<ClientProxyMapping>> clientProxyMappingMap = new HashMap<>();
            for (ClientProxyMapping clientProxyMapping : clientProxyMappings) {
                List<ClientProxyMapping> clientProxyMappingList = clientProxyMappingMap.get(clientProxyMapping.getClientKey());
                if (clientProxyMappingList == null) {
                    clientProxyMappingList = new ArrayList<>();
                    clientProxyMappingMap.put(clientProxyMapping.getClientKey(), clientProxyMappingList);
                }
                clientProxyMappingList.add(clientProxyMapping);
            }
            for (Client client : clients) {
                List<ClientProxyMapping> mappings = clientProxyMappingMap.get(client.getClientKey());
                if (mappings == null) {
                    mappings = new ArrayList<>();
                }
                client.setProxyMappings(mappings);
            }
        }
        Map<String, List<Integer>> clientInetPortMapping = new HashMap<String, List<Integer>>();
        Map<Integer, String> inetPortLanInfoMapping = new HashMap<Integer, String>();
        // 构造端口映射关系
        for (Client client : clients) {
            String clientKey = client.getClientKey();
            if (clientInetPortMapping.containsKey(clientKey)) {
                throw new IllegalArgumentException("密钥同时作为客户端标识，不能重复： " + clientKey);
            }
            List<ClientProxyMapping> mappings = client.getProxyMappings();
            List<Integer> ports = new ArrayList<Integer>();
            clientInetPortMapping.put(clientKey, ports);

            for (ClientProxyMapping mapping : mappings) {
                Integer port = mapping.getInetPort();
                ports.add(port);
                if (inetPortLanInfoMapping.containsKey(port)) {
                    throw new IllegalArgumentException("一个公网端口只能映射一个后端信息，不能重复: " + port);
                }
                inetPortLanInfoMapping.put(port, mapping.getLan());
            }
        }
        // 替换之前的配置关系
        this.clientInetPortMapping = clientInetPortMapping;
        this.inetPortLanInfoMapping = inetPortLanInfoMapping;
        this.clients = clients;
        //更新代理配置
        if (!isInit) {
            Connection connection = null;
            try {
                connection = C3P0Utils.getInstance().getConnection();
                connection.setAutoCommit(false);
                ClientDao.deleteClient(connection);
                ClientDao.deleteClientProxyMapping(connection);
                ClientDao.saveClientAndProxyMapping(connection, clients);
                DbUtils.commitAndCloseQuietly(connection);
            } catch (Exception e) {
                DbUtils.rollbackAndCloseQuietly(connection);
                throw new RuntimeException(e);
            }
        }
        notifyconfigChangedListeners();
    }


    /**
     * 配置更新通知
     */
    private void notifyconfigChangedListeners() {
        List<ConfigChangedListener> changedListeners = new ArrayList<ConfigChangedListener>(configChangedListeners);
        for (ConfigChangedListener changedListener : changedListeners) {
            changedListener.onChanged();
        }
    }

    /**
     * 添加配置变化监听器
     *
     * @param configChangedListener
     */
    public void addConfigChangedListener(ConfigChangedListener configChangedListener) {
        configChangedListeners.add(configChangedListener);
    }

    /**
     * 移除配置变化监听器
     *
     * @param configChangedListener
     */
    public void removeConfigChangedListener(ConfigChangedListener configChangedListener) {
        configChangedListeners.remove(configChangedListener);
    }

    /**
     * 获取代理客户端对应的代理服务器端口
     *
     * @param clientKey
     * @return
     */
    public List<Integer> getClientInetPorts(String clientKey) {
        return clientInetPortMapping.get(clientKey);
    }

    /**
     * 获取所有的clientKey
     *
     * @return
     */
    public Set<String> getClientKeySet() {
        return clientInetPortMapping.keySet();
    }

    /**
     * 根据代理服务器端口获取后端服务器代理信息
     *
     * @param port
     * @return
     */
    public String getLanInfo(Integer port) {
        return inetPortLanInfoMapping.get(port);
    }

    /**
     * 返回需要绑定在代理服务器的端口（用于用户请求）
     *
     * @return
     */
    public List<Integer> getUserPorts() {
        List<Integer> ports = new ArrayList<Integer>();
        Iterator<Integer> ite = inetPortLanInfoMapping.keySet().iterator();
        while (ite.hasNext()) {
            ports.add(ite.next());
        }

        return ports;
    }

    public static ProxyConfig getInstance() {
        return instance;
    }

    /**
     * 配置更新回调
     *
     * @author fengfei
     */
    public static interface ConfigChangedListener {

        void onChanged();
    }
}
