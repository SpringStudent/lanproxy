package org.fengfei.lanproxy.server.config.web.jdbc.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @author zhouning
 * @date 2021/10/15 11:20
 */
public class Client implements Serializable {

    private static final long serialVersionUID = 1L;
    private String id;

    /**
     * 客户端备注名称
     */
    private String name;

    /**
     * 代理客户端唯一标识key
     */
    private String clientKey;

    /**
     * 代理客户端与其后面的真实服务器映射关系
     */
    private List<ClientProxyMapping> proxyMappings;

    private Integer status;

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

    public List<ClientProxyMapping> getProxyMappings() {
        return proxyMappings;
    }

    public void setProxyMappings(List<ClientProxyMapping> proxyMappings) {
        this.proxyMappings = proxyMappings;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
