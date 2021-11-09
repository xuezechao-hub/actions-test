/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2022. All rights reserved.
 */

package com.huawei.gray.feign.definition;

import com.huawei.apm.core.agent.definition.EnhanceDefinition;
import com.huawei.apm.core.agent.definition.MethodInterceptPoint;
import com.huawei.apm.core.agent.matcher.ClassMatcher;
import com.huawei.apm.core.agent.matcher.ClassMatchers;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * 拦截feign的ApacheHttpClient和OkHttpClient请求
 *
 * @author lilai
 * @since 2021-11-03
 */
public class PathVarDefinition implements EnhanceDefinition {

    /**
     * Intercept class.
     */
    private static final String INTERCEPT_CLASS = "com.huawei.gray.feign.interceptor.PathVarInterceptor";

    @Override
    public ClassMatcher enhanceClass() {
        return ClassMatchers.named("feign.ReflectiveFeign$BuildTemplateByResolvingArgs");
    }

    @Override
    public MethodInterceptPoint[] getMethodInterceptPoints() {
        return new MethodInterceptPoint[]{
                MethodInterceptPoint.newInstMethodInterceptPoint(
                        INTERCEPT_CLASS, ElementMatchers.<MethodDescription>named("resolve")
                )
        };
    }
}

