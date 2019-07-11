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

    public static Map<String, Integer> threadCountMap = new HashMap<String, Integer>();
    public static String[] servers = new String[3];
    public static Integer[] weight = new Integer[3];
    public static Integer weightSum = 0;
    public static long time0 = System.currentTimeMillis();
    public long duration = 1000;//多久计算一次权重

//    private static final

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (threadCountMap.size() < 3 || threadCountMap.values().iterator().next() < 50) {
            return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
        } else {
            long curTime = System.currentTimeMillis();
            if (curTime - time0 > duration) {
                //重新计算权重
            }
            int randomValue = ThreadLocalRandom.current().nextInt(invokers.size());
            int curValue = 0;
            String serverFlag = "";
            for(int i=0; i<servers.length; i++) {
                curValue = curValue+weight[i];
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
}
