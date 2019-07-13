package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.monitor.support.MonitorFilter;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author daofeng.xjf
 *
 * 客户端过滤器
 * 可选接口
 * 用户可以在客户端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.CONSUMER)
public class TestClientFilter implements Filter {

    private static final String TIMEOUT_FILTER_START_TIME = "timeout_filter_start_time";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        //获取server
        String url = invoker.getUrl().toString();
        String server = url.substring(url.indexOf("-")+1);
        server = server.substring(0,server.indexOf(":"));

        // 记录开始时间，放入invocation的attachment里
        if (invocation.getAttachments() != null) {
            invocation.getAttachments().put(TIMEOUT_FILTER_START_TIME, String.valueOf(start));
            invocation.getAttachments().put("serverName", server);
        } else {
            if (invocation instanceof RpcInvocation) {
                RpcInvocation invc = (RpcInvocation) invocation;
                invc.setAttachment(TIMEOUT_FILTER_START_TIME, String.valueOf(start));
                invc.setAttachment("serverName", server);
            }
        }




        //RpcContext context = RpcContext.getContext();
        //String remoteValue = invoker.getUrl().getAddress();  //  provider-medium:20870
        //String remoteHost = context.getRemoteHost();//  provider-small

        //并发数+1
        incrementValidNum(server);

        try{

            System.out.println(System.currentTimeMillis()+" 发起调用："+invoker.getUrl().toString());
            Result result = invoker.invoke(invocation);


            return result;
        }catch (Exception e){
            throw e;
        } finally {
            //decrementValidNum(server);//放到onResponse里
        }
    }

    //对应服务器的活跃数+1
    private void incrementValidNum(String server) {
        Integer validNum = UserLoadBalance.validNumMap.get(server);
        validNum = validNum == null?0:validNum;
        UserLoadBalance.validNumMap.put(server, validNum+1);
    }

    //对应服务器的活跃-1
    private void decrementValidNum(String server) {
        Integer validNum = UserLoadBalance.validNumMap.get(server);
        if (validNum == null) {
            return;
        }
        validNum = validNum <= 0?1:validNum;
        UserLoadBalance.validNumMap.put(server, validNum-1);
    }


    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        // 获取开始时间
        String startAttach = invocation.getAttachment(TIMEOUT_FILTER_START_TIME);
        //获取服务名
        String serverName = invocation.getAttachment("serverName");
        //对应服务器的活跃-1
        decrementValidNum(serverName);
        if (startAttach != null) {
            // 调用服务的耗时
            long elapsed = System.currentTimeMillis() - Long.valueOf(startAttach);
            System.out.println("服务调用耗时："+elapsed+"  "+invoker.getUrl().toString()+"   当前活跃数:"+UserLoadBalance.validNumMap.get(serverName));
            // 调用耗时超过了设置的超时
            if (invoker.getUrl() != null
                    && elapsed > invoker.getUrl().getMethodParameter(invocation.getMethodName(),
                    "timeout", Integer.MAX_VALUE)) {
                System.out.println(invoker.getUrl().getMethodParameter(invocation.getMethodName(),
                        "timeout", Integer.MAX_VALUE));
            }


            //记录服务耗时
            Map<String, Long> map0 = UserLoadBalance.rspTimeMap.get(serverName);
            if (map0 == null) {
                map0 = new HashMap<>();
            }
            map0.put(startAttach + "-" + UUID.randomUUID().toString().replaceAll("-",""), elapsed);
            UserLoadBalance.rspTimeMap.put(serverName, map0);

        }
        return result;
    }
}
