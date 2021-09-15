package qqclient.service;

import qqcommon.Message;
import qqcommon.MessageType;

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * @author xiaochen
 * @version 1.0
 * @create 2021-09-13 9:37
 */
public class ClientConnectServerThread extends Thread{
    // 该线程需要持有 socket
    private Socket socket;

    //构造器可以接受一个Socket对象
    public ClientConnectServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        //因为Thread 需要在后台和服务器通信，因此我们需要一个 while
        while (true) {
            System.out.println("\n客户端的线程，等待读取从服务端发送的消息");
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                //如果服务端没有发送数据，就会阻塞就这里。
                Message message = (Message)ois.readObject(); //如果服务器没有发送 Message，线程会阻塞在这里
                // 后面需要去使用 message
                // 判断这个 message类型，然后做相应的业务处理
                //如果是读取到的是 服务端返回的在线用户列表
                if (message.getMesType().equals(MessageType.MESSAGE_RET_ONLINE_FRIEND)) {
                    //取出在线列表信息，并显示
                    String[] onlineUsers = message.getContent().split(" ");
                    System.out.println("\n====当前在线用户列表====");
                    for (int i = 0; i < onlineUsers.length; i ++ ) {
                        System.out.println("用户: " + onlineUsers[i]);
                    }
                } else if (message.getMesType().equals(MessageType.MESSAGE_COMM_MES)) {
                    //把从服务器转发来的消息，直接显示到客户端中
                    System.out.println("\n" + message.getSender() + " 对 " + message.getGetter() + " 说:" + message.getContent());
                } else if (message.getMesType().equals(MessageType.MESSAGE_TOALL_MES)) {
                    System.out.println("\n" + message.getSender() + " 对 大家 说:" + message.getContent());
                } else if (message.getMesType().equals(MessageType.MESSAGE_FILE_MES)) { //如果是文件消息
                    System.out.println("\n" + message.getSender() + " 给 " + message.getGetter() + " 发送文件:" + message.getSrc() + " 到我的的电脑目录" + message.getDest());
                    //取出message的文件字节数组，通过文件输出流写出到磁盘
                    FileOutputStream fileOutputStream = new FileOutputStream(message.getDest());
                    fileOutputStream.write(message.getFileBytes());
                    fileOutputStream.close();
                    System.out.println("\n保存文件成功!");
                } else {
                    System.out.println("是其他类型的 message，暂时不处理");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    //为了更方便的得到Socket
    public Socket getSocket() {
        return socket;
    }
}
