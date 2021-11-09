/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 */

package com.huawei.flowcontrol;

import com.huawei.apm.core.agent.common.BeforeResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;

import java.lang.reflect.Method;

/**
 * apache dubbo拦截后的增强类,埋点定义sentinel资源
 *
 * @author liyi
 * @since 2020-08-26
 */
public class ApacheDubboInterceptor extends DubboInterceptor  {
    @Override
    public void before(Object obj, Method method, Object[] allArguments, BeforeResult result) {
        Invoker invoker = null;
        if (allArguments[0] instanceof Invoker) {
            invoker = (Invoker) allArguments[0];
        }
        Invocation invocation = null;
        if (allArguments[1] instanceof Invocation) {
            invocation = (Invocation) allArguments[1];
        }
        if (invocation == null || invoker == null) {
            return;
        }
        RpcContext rpcContext = RpcContext.getContext();
        entry(rpcContext.isConsumerSide(), invoker.getInterface().getName(), invocation.getMethodName(), result);
    }

    @Override
    public Object after(Object obj, Method method, Object[] allArguments, Object ret) {
        Result result = (Result) ret;
        // 记录dubbo的exception
        if (result != null && result.hasException()) {
            handleException(result.getException());
        }
        removeThreadLocalEntry();
        return ret;
    }

}
