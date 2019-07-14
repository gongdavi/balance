package com.aliware.tianchi;

import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.transport.RequestLimiter;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;

/**
 * @author daofeng.xjf
 *
 * 服务端限流
 * 可选接口
 * 在提交给后端线程池之前的扩展，可以用于服务端控制拒绝请求
 */
public class TestRequestLimiter implements RequestLimiter {

    /**
     * @param request 服务请求
     * @param activeTaskCount 服务端对应线程池的活跃线程数
     * @return  false 不提交给服务端业务线程池直接返回，客户端可以在 Filter 中捕获 RpcException
     *          true 不限流
     */
    @Override
    public boolean tryAcquire(Request request, int activeTaskCount) {
        System.out.println(activeTaskCount);
        DecodeableRpcInvocation invocation = (DecodeableRpcInvocation)request.getData();
        String serverName = invocation.getAttachment("serverName");
        if ("small".equals(serverName) && activeTaskCount >=148) {
            return false;
        } else if ("medium".equals(serverName) && activeTaskCount >=423){
            return false;
        } else if ("large".equals(serverName) && activeTaskCount >=585) {
            return false;
        }
        return true;
    }

}
