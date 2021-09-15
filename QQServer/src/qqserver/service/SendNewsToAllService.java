package qqserver.service;

import qqcommon.Message;
import qqcommon.MessageType;
import utils.Utility;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author xiaochen
 * @version 1.0
 * @create 2021-09-15 15:45
 */
public class SendNewsToAllService implements Runnable{

    @Override
    public void run() {
        while (true) { //为了可以多次推送新闻。
            System.out.println("请输入服务器要推送的新闻/消息[输入exit表示退出推送服务]");
            String news = Utility.readString(1000);
            if ("exit".equals(news)) break;
            //构建一个消息类型，群发消息类型
            Message message = new Message();
            message.setMesType(MessageType.MESSAGE_TOALL_MES);
            message.setSender("服务器");
            message.setContent(news);
            message.setSendTime(new Date().toString());
            System.out.println("服务器 推送消息给所有人" + news);

            //遍历当前所有的通信线程，并发送message对象...
            HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();
            Iterator<String> iterator = hm.keySet().iterator();
            while (iterator.hasNext()) {
                String onlineUsers = iterator.next().toString();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(hm.get(onlineUsers).getSocket().getOutputStream());
                    message.setGetter(onlineUsers);
                    oos.writeObject(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
