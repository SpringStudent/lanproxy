package org.fengfei.lanproxy.server.config.web.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.fengfei.lanproxy.server.config.web.jdbc.C3P0Utils;
import org.fengfei.lanproxy.server.config.web.jdbc.bean.Client;
import org.fengfei.lanproxy.server.config.web.jdbc.bean.ClientProxyMapping;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author zhouning
 * @date 2021/10/15 11:23
 */
public class ClientDao {

    public static List<Client> getClient() {
        List<Client> clients = new ArrayList<>();
        try {
            clients = new QueryRunner(C3P0Utils.getInstance().getDataSource()).query("select * from client", new BeanListHandler<Client>(Client.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return clients;
    }

    public static List<ClientProxyMapping> getClientProxyMapping() {
        List<ClientProxyMapping> clientProxyMappings = new ArrayList<>();
        try {
            clientProxyMappings = new QueryRunner(C3P0Utils.getInstance().getDataSource()).query("select * from client_proxy_mapping", new BeanListHandler<ClientProxyMapping>(ClientProxyMapping.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return clientProxyMappings;
    }

    public static void deleteClient(Connection connection) throws Exception {
        new QueryRunner().update(connection, "delete from client");
    }

    public static void deleteClientProxyMapping(Connection connection) throws Exception {
        new QueryRunner().update(connection, "delete from client_proxy_mapping");
    }

    public static void saveClientAndProxyMapping(Connection connection, List<Client> clients) throws Exception {
        String sql1 = "insert into client(id,name,clientKey,status) values(?,?,?,?)";
        String sql2 = "insert into client_proxy_mapping(id,clientKey,inetPort,lan,name) values(?,?,?,?,?)";
        List<Object[]> param1 = new ArrayList<>();
        List<Object[]> param2 = new ArrayList<>();
        for (Client client : clients) {
            param1.add(new Object[]{UUID.randomUUID().toString().replace("-", ""), client.getName(), client.getClientKey(), client.getStatus() == null ? 0 : client.getStatus()});
            List<ClientProxyMapping> mappings = client.getProxyMappings();
            for (ClientProxyMapping mapping : mappings) {
                param2.add(new Object[]{UUID.randomUUID().toString().replace("-", ""), client.getClientKey(), mapping.getInetPort(), mapping.getLan(), mapping.getName()});
            }
        }
        if (!param1.isEmpty()) {
            new QueryRunner().batch(connection, sql1, param1.toArray(new Object[param1.size()][]));
        }
        if (!param2.isEmpty()) {
            new QueryRunner().batch(connection, sql2, param2.toArray(new Object[param2.size()][]));
        }
    }
}
