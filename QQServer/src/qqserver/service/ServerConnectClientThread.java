package qqserver.service;

import qqcommon.Message;
import qqcommon.MessageType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiaochen
 * @version 1.0
 * @create 2021-09-13 10:35
 *
 * 该类对应的一个对象和某个客户端保持通信
 */
public class ServerConnectClientThread extends Thread{
    private Socket socket;
    private String userId; //连接到服务端的用户id

    public ServerConnectClientThread(Socket socket, String userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() { //这里线程处于run的状态，可以发送接收消息
        int i = 0;
        while (true) {

            try {
                System.out.println("服务端和客户端" + userId + "保持通信，读取数据...");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                try {
                    System.out.println((++ i) + "=============");
                    ConcurrentHashMap<String, ArrayList> offlineDB = QQServer.getOfflineDB();
                    Iterator<String> iterator = offlineDB.keySet().iterator();
                    while (iterator.hasNext()) {
                        String getterId =  iterator.next();
                        if (getterId.equals(userId)) {
                            ObjectOutputStream oos = new ObjectOutputStream(ManageClientThreads.getClientThread(getterId).getSocket().getOutputStream());
                            ArrayList messageList = offlineDB.get(getterId);
                            for (Object message1 : messageList) {
                                oos.writeObject(message1);
                            }
                        }
                    }
                    QQServer.getOfflineDB().remove(userId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Message message = (Message)ois.readObject();
                //后面会使用 message, 根据 message的类型，做相应的业务处理
                if (message.getMesType().equals(MessageType.MESSAGE_GET_ONLINE_FRIEND)) {
                    /*
                    客户端要在线用户列表
                    在线用户列表形式 100 200 至尊宝
                     */
                    System.out.println(message.getSender() + "请求在线用户列表");
                    String onlineUser = ManageClientThreads.getOnlineUser();
                    //返回message
                    //构建一个message.
                    Message message2 = new Message();
                    message2.setMesType(MessageType.MESSAGE_RET_ONLINE_FRIEND);
                    message2.setContent(onlineUser);
                    message2.setGetter(message.getSender());
                    //返回客户端
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(message2);
                } else if (message.getMesType().equals(MessageType.MESSAGE_CLIENT_EXIT)) {
                    System.out.println(message.getSender() + "要退出了..");
                    //将这个客户端的对应的线程从集合中删除
                    ManageClientThreads.removeServerConnectClientThread(message.getSender());
                    socket.close(); //关闭连接
                    //退出 while循环
                    break;
                } else if (message.getMesType().equals(MessageType.MESSAGE_COMM_MES)) {
                    /*
                    转发聊天。 根据message获取 getter id，然后再对应先得到线程转发消息
                    如果用户不在线的话，存到offlineDB表中。等待用户上线发送。
                     */
                    //判断用户是否在线.
                    ServerConnectClientThread clientThread = ManageClientThreads.getClientThread(message.getGetter());
                    if (clientThread != null) {
                        //用户在线。得到对应socket的对象输出流, 将message对象转发给指定的客户端
                        ObjectOutputStream oos = new ObjectOutputStream(clientThread.socket.getOutputStream());
                        oos.writeObject(message); //转发
                    } else {
                        System.out.println("消息内存已存离线DB中。。");
                        //用户离线，将消息推到QQserver的DB中保存。
                        ConcurrentHashMap<String, ArrayList> offlineDB = QQServer.getOfflineDB();
                        ArrayList arrayList = offlineDB.get(message.getGetter());
                        if (arrayList != null) {//已经有离线队列了
                            arrayList.add(message);
                        } else {
                            arrayList = new ArrayList();
                            arrayList.add(message);
                        }
                        offlineDB.put(message.getGetter(), arrayList);
                    }
                } else if (message.getMesType().equals(MessageType.MESSAGE_TOALL_MES)) {
                    /*
                    群发消息，需要遍历所有的线程，排除自己，然后转发给大家
                    群发离线消息。
                    */
                    HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();
                    hm.remove(message.getSender()); //排除自己
                    Iterator<String> iterator = hm.keySet().iterator();
                    while (iterator.hasNext()) {
                        //取出在线用户的id
                        String onlineUserId = iterator.next().toString();
                        ServerConnectClientThread clientThread = ManageClientThreads.getClientThread(onlineUserId);
                        ObjectOutputStream oos = new ObjectOutputStream(clientThread.getSocket().getOutputStream());
                        oos.writeObject(message);
                    }
                } else if (message.getMesType().equals(MessageType.MESSAGE_FILE_MES)) {
                    /*
                    根据getterid，获取到对应的线程，将message对象转发
                    离线文件发送
                     */
                    ServerConnectClientThread clientThread = ManageClientThreads.getClientThread(message.getGetter());
                    ObjectOutputStream oos = new ObjectOutputStream(clientThread.getSocket().getOutputStream());
                    oos.writeObject(message);
                } else {
                    System.out.println("其他类型的message ，暂时不处理");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //离线消息，成功存到了offlineDB，接下来，在用户登录的时候发消息发过去。
            //遍历offlineDB有啥东西。
            try {
                System.out.println((++ i) + "=============");
                ConcurrentHashMap<String, ArrayList> offlineDB = QQServer.getOfflineDB();
                Iterator<String> iterator = offlineDB.keySet().iterator();
                while (iterator.hasNext()) {
                    String getterId =  iterator.next();
                    System.out.print("getterId =" + getterId);
                    ArrayList messageList = offlineDB.get(getterId);
                    for (Object message1 : messageList) {
                        System.out.print(message1.toString());
                    }
                    System.out.println(" ");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
