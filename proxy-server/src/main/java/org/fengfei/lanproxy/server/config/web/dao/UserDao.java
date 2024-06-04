package org.fengfei.lanproxy.server.config.web.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.fengfei.lanproxy.server.config.web.jdbc.C3P0Utils;
import org.fengfei.lanproxy.server.config.web.jdbc.bean.User;

/**
 * @author zhouning
 * @date 2021/10/15 10:30
 */
public class UserDao {

    public static User getUser(String username) {
        User user = null;
        try {
            user = new QueryRunner(C3P0Utils.getInstance().getDataSource()).query("select * from user where username = ?", new BeanHandler<User>(User.class), username);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }
}
