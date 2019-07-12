package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;

/**
 * @author daofeng.xjf
 *
 * 客户端监听器
 * 可选接口
 * 用户可以基于获取获取服务端的推送信息，与 CallbackService 搭配使用
 *
 */
public class CallbackListenerImpl implements CallbackListener {

    @Override
    public void receiveServerMsg(String msg) {
        if (msg.indexOf("{") >= 0) {
            String serverFlag = msg.substring(msg.indexOf("{")+1, msg.indexOf("}"));
            //给服务器列表赋值
            if(UserLoadBalance.servers[UserLoadBalance.serverNum-1] == null) {
                for (int i = 0; i < UserLoadBalance.serverNum; i++) {
                    if (UserLoadBalance.servers[i] == null) {
                        UserLoadBalance.servers[i] = serverFlag;
                    }
                }
            }

            Integer threadCount = Integer.valueOf(msg.substring(msg.lastIndexOf(":")+1, msg.length()));
            UserLoadBalance.threadCountMap.put(serverFlag, threadCount);
        } else {}
        System.out.println("receive msg from server :" + msg);
    }

}
