package qqclient.service;

import qqcommon.Message;
import qqcommon.MessageType;
import qqcommon.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @author xiaochen
 * @version 1.0
 * @create 2021-09-13 9:26
 *
 * 该类完成用户登录验证和用户注册等功能
 */
public class UserClientService {

    //因为我们可能在其他地方使用 user信息，因此作出成员属性
    private User u = new User();
    //因为 socket在其他地方也可能使用，因此作出属性
    private Socket socket;


    //根据 userId 和 pwd 到服务器验证该用户是否合法
    public boolean checkUser(String userId, String pwd) {
        boolean b = false;
        //创建一个User对象
        u.setUserId(userId);
        u.setPassword(pwd);

        try {
            //连接到服务端发送 u对象.
            socket = new Socket(InetAddress.getByName("127.0.0.1"), 9999);
            //得到ObjectOutputStream 对象
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(u); //发送 User对象

            //读取从服务器回复的Message对象。
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message ms = (Message) ois.readObject();

            if (ms.getMesType().equals(MessageType.MESSAGE_LOGIN_SUCCEED)) { //登录成功

                //创建一个和服务端保持通信的线程 -> 创建一个类 ClientConnectServerThread
                ClientConnectServerThread clientConnectServerThread = new ClientConnectServerThread(socket);
                //启动客户端的线程
                clientConnectServerThread.start();
                //为了客户端的扩展，我们将线程放入到集合管理
                ManageClientConnectServerThread.addClientConnectServerThread(userId, clientConnectServerThread);

                b = true;
            } else {
                //如果登录失败... 就不能启动和服务器通信的线程。
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return b;
    }

    //向服务器请求在线用户列表
    public void onlineFriendList() {
        //发送一个 Message。类型为 MESSAGE_GET_ONLINE_FRIEND
        Message message = new Message();
        message.setSender(u.getUserId());
        message.setMesType(MessageType.MESSAGE_GET_ONLINE_FRIEND);

        //发送给服务器
        //应该得到当前线程的Socket，对应的 ObjectOutputStream对象
        try {
            // 从管理线程的集合中, 通过 userId, 得到这个线程对象
            ClientConnectServerThread clientConnectServerThread = ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId());
            // 通过这个线程得到关联的 socket
            Socket socket = clientConnectServerThread.getSocket();
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message); //发送一个 Message对象，向服务端要求在线用户列表。
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //编写方法，退出客户端，给服务器发送退出系统的message对象
    public void logout() {
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_CLIENT_EXIT);
        message.setSender(u.getUserId()); //一定要指定是哪个客户端id要退出。

        //发送message
        try {
            ObjectOutputStream oos = new ObjectOutputStream(ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId()).getSocket().getOutputStream());
            oos.writeObject(message);
            System.out.println(u.getUserId() + "退出系统");
            System.exit(0); //结束进程

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
