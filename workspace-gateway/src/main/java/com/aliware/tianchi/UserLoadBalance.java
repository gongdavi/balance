package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.math.BigDecimal;
import java.util.*;
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

    public static final int serverNum = 3;
    //线程Map，key=服务标识，value=服务对应的线程数
    public static Map<String, Integer> threadCountMap = new HashMap<String, Integer>();

    //权重相关
    //public static String[] servers = new String[serverNum];//服务器
    public static String[] servers = new String[]{"small","large","medium"};//服务器
    public static Integer[] weight = new Integer[serverNum];//服务器的权重
    public long weightDuration = 50;//多久计算一次权重
    public static long time0 = System.currentTimeMillis();
    public static Integer weightCount;//权重的和


    //响应时间，key=服务标识，value=每个响应的时间、响应的时长。如果失败则认为响应时长800ms
    public static Map<String, Map<String, Long>> rspTimeMap = new HashMap<String, Map<String, Long>>();
    public static int errorDelayTime = 800;
    public static Integer[] rspAvgTime = new Integer[serverNum];//一段时间内的请求平均响应时间
    public static Integer rspAvgTimeCount = 0;
    public static Integer[] rspNum = new Integer[serverNum];//一段时间内的请求个数
    public static Map<String, Integer> validNumMap = new HashMap<>();//一段时间内的活跃请求数
    public static Integer rspNumCount = 0;
//    private static final

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        if (invokers == null || invokers.isEmpty()) {
            return null;
        }
        // 如果只有一个元素，无需负载均衡算法
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        // 负载均衡算法：该方法为抽象方法，由子类实现
        return doSelect(invokers, url, invocation);
    }

    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        //如果权重条件不符合，则随机传递
//        if (threadCountMap.size() < serverNum || threadCountMap.values().iterator().next() < 50) {
//            return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
//        } else {
//            long curTime = System.currentTimeMillis();
//            if (curTime - time0 > weightDuration) {
//                //重新计算权重，间隔1s
//                countWeight();
//                //time0 = curTime;//线程权重，不加这行可以达到120W，加上后仅17W，实时计算的话会比较好。
//                time0 = curTime;
//            }
//            int randomValue = ThreadLocalRandom.current().nextInt(weightCount);
//            int curValue = 0;
//            String serverFlag = "";
//            for(int i=0; i<serverNum; i++) {
//                curValue += weight[i];
//                if (randomValue <= curValue) {
//                    serverFlag = servers[i];
//                    break;
//                }
//            }
//            for(int i=0; i<invokers.size(); i++) {
//                Invoker invoker = invokers.get(i);
//                if(invoker.getUrl().toString().indexOf(serverFlag) > 0) {
//                    return invoker;
//                }
//                if (i == invokers.size() -1) {
//                    return invoker;
//                }
//            }
//        }
        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }

    //计算权重
    public void countWeight() {

        weightCount = 0;

        //根据服务器线程数计算权重
//        for (int i = 0; i < serverNum; i++) {
//            weight[i] = threadCountMap.get(servers[i]);
//            weightCount += weight[i];
//        }

        //根据响应时间及个数判断权重

        //计算所有服务器的平均响应时间、请求个数
        long currTime = System.currentTimeMillis();
        rspAvgTimeCount = 0;
        rspNumCount = 0;
        for (int i = 0; i < serverNum; i++) {
            Map<String, Long> rspAllMap = rspTimeMap.get(servers[i]);
            List<String> invalidList = new ArrayList<>();
            int validNum = 1;//仍然有效的响应时长的个数，不能为0
            int validTime = errorDelayTime;//仍然有效的响应时长的总时长
            Set rspAllSet = rspAllMap.keySet();
            Iterator rspAllIter = rspAllSet.iterator();
            while (rspAllIter.hasNext()) {
                String key = (String)rspAllIter.next();
                long mapTime = Long.valueOf(key.substring(0, key.indexOf("-")));
                if (currTime - mapTime > 1000) {//超过1s则认为无效
                    //rspTimeMap.remove(key);
                    invalidList.add(key);
                } else {
                    validNum ++;
                    validTime += rspAllMap.get(key);
                }
            }
            for (String invalidInfo: invalidList) {
                rspTimeMap.remove(invalidInfo);
            }
            rspAvgTime[i] = 500 - validTime/validNum;
            rspAvgTime[i] = rspAvgTime[i]<10?10:rspAvgTime[i];
            rspAvgTimeCount += rspAvgTime[i];
            rspNum[i] = validNum;
            rspNumCount += rspNum[i];
        }
        for (int i = 0; i < serverNum; i++) {
            weight[i] = rspNum[i]*rspAvgTimeCount/rspNumCount+rspAvgTime[i];
            weightCount += weight[i];
            System.out.println(System.currentTimeMillis()+" "+servers[i]+" weight:"+weight[i]+"   rspNum[i]:"+rspNum[i]+" rspNumCount:"+rspNumCount+"   rspAvgTime[i]:"+rspAvgTime[i]+" rspAvgTimeCount:"+rspAvgTimeCount);
        }

    }
}
