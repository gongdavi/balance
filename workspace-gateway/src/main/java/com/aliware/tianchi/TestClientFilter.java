package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author daofeng.xjf
 *
 * 客户端过滤器
 * 可选接口
 * 用户可以在客户端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.CONSUMER)
public class TestClientFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String url = invoker.getUrl().toString();
        String server = url.substring(url.indexOf("-")+1);
        server = server.substring(0,server.indexOf(":"));
        long startTime = System.currentTimeMillis();
        try{
            Result result = invoker.invoke(invocation);
            long endTime = System.currentTimeMillis();
            Integer duration = Long.valueOf(endTime - startTime).intValue();
            if (result.hasException()) {
                duration = UserLoadBalance.errorDelayTime;
            }
            Map<Long, Integer> map0 =
            UserLoadBalance.rspTimeMap.get(server);
            if (map0 == null) {
                map0 = new HashMap<>();
            }
            map0.put(endTime, duration);
            UserLoadBalance.rspTimeMap.put(server, map0);

            return result;
        }catch (Exception e){
            long endTime = System.currentTimeMillis();
            Integer duration = UserLoadBalance.errorDelayTime;
            Map<Long, Integer> map0 =
                    UserLoadBalance.rspTimeMap.get(server);
            if (map0 == null) {
                map0 = new HashMap<>();
            }
            map0.put(endTime, duration);
            UserLoadBalance.rspTimeMap.put(server, map0);

            throw e;
        }
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        return result;
    }
}
