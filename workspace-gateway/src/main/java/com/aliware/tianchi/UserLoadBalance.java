package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collector;

/**
 * @author daofeng.xjf
 *
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {

    public static BigDecimal small = new BigDecimal(0);
    public static BigDecimal large = new BigDecimal(0);
    public static BigDecimal medium = new BigDecimal(0);

    public static BigDecimal smallThread = new BigDecimal(0);
    public static BigDecimal largeThread = new BigDecimal(0);
    public static BigDecimal mediumThread = new BigDecimal(0);

    private static final int serverNum = 3;
    //线程Map，key=服务标识，value=服务对应的线程数
    public static Map<String, Integer> threadCountMap = new HashMap<String, Integer>();

    //权重相关
    //public static String[] servers = new String[serverNum];//服务器
    public static String[] servers = new String[]{"small","large","medium"};//服务器
    public static Integer[] weight = new Integer[serverNum];//服务器的权重
    public long weightDuration = 1000;//多久计算一次权重
    public static long time0 = System.currentTimeMillis();
    public static Integer weightCount;//权重的和


    //响应时间，key=服务标识，value=每个响应的时间、响应的时长。如果失败则认为响应时长800ms
    public static Map<String, Map<Long, Integer>> rspTimeMap = new HashMap<String, Map<Long, Integer>>();
    public static int errorDelayTime = 800;

//    private static final

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (threadCountMap.size() < serverNum || threadCountMap.values().iterator().next() < 50) {
            return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        } else {
            long curTime = System.currentTimeMillis();
            if (curTime - time0 > weightDuration) {
                //重新计算权重
                countWeight();
            }
            int randomValue = ThreadLocalRandom.current().nextInt(weightCount);
            int curValue = 0;
            String serverFlag = "";
            for(int i=0; i<serverNum; i++) {
                curValue += weight[i];
                if (randomValue <= curValue) {
                    serverFlag = servers[i];
                    break;
                }
            }
            for(int i=0; i<invokers.size(); i++) {
                Invoker invoker = invokers.get(i);
                if(invoker.getUrl().toString().indexOf(serverFlag) > 0) {
                    return invoker;
                }
                if (i == invokers.size() -1) {
                    return invoker;
                }
            }
        }
        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }

    public void countWeight() {
        weightCount = 0;
        for (int i = 0; i < serverNum; i++) {
            weight[i] = threadCountMap.get(servers[i]);
            weightCount += weight[i];
        }

        //计算响应时间的map，将里面的过期时间去掉，并计算剩余的时间的平均值
//        for(int i=0; i<serverNum; i++) {
//            rspTimeMap.keySet();
//        }
    }
}
