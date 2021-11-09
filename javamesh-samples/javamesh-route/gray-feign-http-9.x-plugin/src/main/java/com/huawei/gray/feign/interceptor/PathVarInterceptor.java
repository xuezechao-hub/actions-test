/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2022. All rights reserved.
 */

package com.huawei.gray.feign.interceptor;

import com.huawei.apm.core.agent.common.BeforeResult;
import com.huawei.apm.core.agent.interceptor.InstanceMethodInterceptor;
import com.huawei.gray.feign.context.FeignResolvedURL;

import feign.RequestTemplate;

import java.lang.reflect.Method;

/**
 * 拦截feign.ReflectiveFeign.BuildTemplateByResolvingArgs获得url解析前后的结果
 *
 * @author lilai
 * @since 2021-11-03
 */
public class PathVarInterceptor implements InstanceMethodInterceptor {

    static final ThreadLocal<FeignResolvedURL> URL_CONTEXT = new ThreadLocal<FeignResolvedURL>();

    /**
     * feign.ReflectiveFeign.BuildTemplateByResolvingArgs.resolve解析url路径参数前将原始url放入线程变量
     *
     * @param obj          拦截对象
     * @param method       拦截方法
     * @param arguments    方法参数
     * @param beforeResult 返回结果
     */
    @Override
    public void before(Object obj, Method method, Object[] arguments, BeforeResult beforeResult) throws Exception {
        RequestTemplate template = (RequestTemplate) arguments[1];
        URL_CONTEXT.set(new FeignResolvedURL(template.url()));
    }

    /**
     * url路径参数解析后的结果放入线程变量
     *
     * @param obj       拦截对象
     * @param method    拦截方法
     * @param arguments 方法参数
     * @param result    返回结果
     */
    @Override
    public Object after(Object obj, Method method, Object[] arguments, Object result) throws Exception {
        RequestTemplate resolvedTemplate = (RequestTemplate) result;
        URL_CONTEXT.get().setUrl(resolvedTemplate.url());
        return result;
    }

    @Override
    public void onThrow(Object obj, Method method, Object[] arguments, Throwable t) {
        if (URL_CONTEXT.get() != null) {
            URL_CONTEXT.remove();
        }
    }
}
