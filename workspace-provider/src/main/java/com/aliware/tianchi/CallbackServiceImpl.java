package com.aliware.tianchi;

import org.apache.dubbo.rpc.listener.CallbackListener;
import org.apache.dubbo.rpc.service.CallbackService;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author daofeng.xjf
 * <p>
 * 服务端回调服务
 * 可选接口
 * 用户可以基于此服务，实现服务端向客户端动态推送的功能
 */
public class CallbackServiceImpl implements CallbackService {

    public CallbackServiceImpl() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!listeners.isEmpty()) {
                    for (Map.Entry<String, CallbackListener> entry : listeners.entrySet()) {
                        try {

                            Runtime runtime = Runtime.getRuntime();
                            //空闲内存
                            long freeMemory = runtime.freeMemory();
                            //内存总量
                            long totalMemory = runtime.totalMemory();
                            //最大允许使用的内存
                            long maxMemory = runtime.maxMemory();
                            //线程总数
                            int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

                            entry.getValue().receiveServerMsg(System.getProperty("quota") + " " + new Date().toString()+
                                    " 内存占比："+new BigDecimal(Double.valueOf(totalMemory-freeMemory)/Double.valueOf(maxMemory)).setScale(2, BigDecimal.ROUND_HALF_UP)
                                    +" freeMemory："+byteToM(freeMemory)
                                    +" totalMemory："+byteToM(totalMemory)
                                    + " maxMemory："+byteToM(maxMemory)+" threadCount："+threadCount);
                        } catch (Throwable t1) {
                            listeners.remove(entry.getKey());
                        }
                    }
                }
            }
        }, 0, 5000);
    }

    private static long byteToM(long bytes){
        long kb =  (bytes / 1024 / 1024);
        return kb;
    }

    private Timer timer = new Timer();

    /**
     * key: listener type
     * value: callback listener
     */
    private final Map<String, CallbackListener> listeners = new ConcurrentHashMap<>();

    @Override
    public void addListener(String key, CallbackListener listener) {
        listeners.put(key, listener);
        listener.receiveServerMsg(new Date().toString()); // send notification for change
    }
}
