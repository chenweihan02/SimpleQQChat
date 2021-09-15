package qqframe;

import qqserver.service.QQServer;

/**
 * @author xiaochen
 * @version 1.0
 * @create 2021-09-13 11:09
 * 该类创建QQServer，启动后台的服务
 */
public class QQFrame {
    public static void main(String[] args) {
        new QQServer();
    }
}
