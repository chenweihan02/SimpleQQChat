package qqcommon;

import java.io.Serializable;

/**
 * @author xiaochen
 * @version 1.0
 * @create 2021-09-12 20:21
 *
 * 表示一个 用户 / 客户信息
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    private String userId; //用户Id
    private String password; //用户密码

    public User() {
    }

    public User(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
