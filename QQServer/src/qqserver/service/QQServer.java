package qqserver.service;

import qqcommon.Message;
import qqcommon.MessageType;
import qqcommon.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiaochen
 * @version 1.0
 * @create 2021-09-13 10:14
 * 这是服务器，在监听 9999端口，等待客户端的连接并保持通讯
 */
public class QQServer {
    private ServerSocket ss = null;
    //创建一个集合，存放多个用户，如果是这些用户登录，就认为是合法的
    //这里也看也是用 ConcurrentHashMap, 可以处理并发的集合，没有线程安全问题。
    //HashMap 没有处理线程安全，因此在多线程情况下是不安全的
    //ConcurrentHashMap 处理的线程安全，即线程同步处理，在多线程情况下是安全的。
    private static ConcurrentHashMap<String, User> validUsers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ArrayList> offlineDB = new ConcurrentHashMap<>();
    //<接收者, 消息列表>


    static { //在静态代码块，初始化 validUsers
        validUsers.put("100", new User("100", "123456"));
        validUsers.put("200", new User("200", "123456"));
        validUsers.put("300", new User("300", "123456"));
        validUsers.put("至尊宝", new User("至尊宝", "123456"));
    }

    //验证用户是否有效的方法
    private boolean checkUser(String userId, String password) {
        User user = validUsers.get(userId);
        if (user == null) { // 说明userId没有存在在 validUsers
            return false;
        }
        if (!user.getPassword().equals(password)) { //userId正确，但是密码错误
            return false;
        }
        return true;
    }

    public static ConcurrentHashMap<String, ArrayList> getOfflineDB() {
        return offlineDB;
    }

    public QQServer() {
        //注意，端口可以写在配置文件
        try {
            System.out.println("服务器在9999端口监听");
            //启动推送新闻的线程.
            new Thread(new SendNewsToAllService()).start();
            ss = new ServerSocket(9999);

            while (true) { //当和某个客户端连接后，会继续监听，因此 while
                Socket socket = ss.accept(); //如果没有客户端连接，就会阻塞在这里
                //得到socket关联的对象输入流
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                //得到socket关联的对象输出流
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                User u = (User) ois.readObject(); //读取客户端发送的user对象
                //创建一个 Message对象，准备回复客户端
                Message message = new Message();

                //验证用户
                if (checkUser(u.getUserId(), u.getPassword())) { //登录
                    message.setMesType(MessageType.MESSAGE_LOGIN_SUCCEED);
                    //将message对象 回复给客户端
                    oos.writeObject(message);
                    //创建一个线程，和客户端保持通信，该线程需要持有socket对象
                    ServerConnectClientThread serverConnectClientThread = new ServerConnectClientThread(socket, u.getUserId());
                    //启动线程
                    serverConnectClientThread.start();
                    //把该线程对象，放入到一个集合中，进行管理.
                    ManageClientThreads.addClientThread(u.getUserId(), serverConnectClientThread);

                } else { //登录失败
                    System.out.println("用户 id:" + u.getUserId() + " 密码: " + u.getPassword() + " 登录失败。");
                    message.setMesType(MessageType.MESSAGE_LOGIN_FAIL);
                    oos.writeObject(message);
                    // 关闭socket
                    socket.close();
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //如果服务端退出了while循环，说明服务器端不再监听，因此需要关闭资源
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
