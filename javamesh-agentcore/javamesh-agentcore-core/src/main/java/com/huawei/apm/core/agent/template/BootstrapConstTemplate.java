/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.core.agent.template;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.logging.Logger;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import com.huawei.apm.core.agent.interceptor.ConstructorInterceptor;
import com.huawei.apm.core.lubanops.bootstrap.Interceptor;
import com.huawei.apm.core.lubanops.bootstrap.log.LogFactory;

/**
 * 启动类构造函数模板
 * <p>启动类加载器加载类的构造函数如果需要增强，则需要使用该模板
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/10/27
 */
public class BootstrapConstTemplate {
    /**
     * 日志
     */
    private static final Logger LOGGER = LogFactory.getLogger();

    /**
     * luban拦截器
     */
    public static Interceptor ORIGIN_INTERCEPTOR;

    /**
     * 拦截器列表
     */
    public static List<ConstructorInterceptor> INTERCEPTORS;

    /**
     * 方法执行前调用
     * <p>由于类加载器限制，需要使用反射调用外部方法，需要构建出动态advice类的全限定名，再用当前类加载器加载
     * <p>由于jvm重定义的限制，不能添加静态属性，动态advice类只能通过局部参数传递
     *
     * @param arguments           所有入参
     * @param constructor         构造函数本身
     * @param adviceCls           动态advice类
     * @param constInterceptorItr 构造拦截器双向迭代器
     * @throws Exception 发生异常
     */
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void OnMethodEnter(
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] arguments,
            @Advice.Origin Constructor<?> constructor,
            @Advice.Local(value = "ADVICE_CLS") Class<?> adviceCls,
            @Advice.Local(value = "CONST_INTERCEPTOR_ITR") ListIterator<?> constInterceptorItr
    ) throws Exception {
        final StringBuilder builder = new StringBuilder()
                .append(constructor.getName())
                .append("(");
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes[i].getName());
        }
        builder.append(')');
        final String adviceClsName = "com.huawei.apm.core.agent.template.BootstrapConstTemplate_" +
                Integer.toHexString(builder.toString().hashCode());
        adviceCls = ClassLoader.getSystemClassLoader().loadClass(adviceClsName);
        constInterceptorItr = (ListIterator<?>) adviceCls.getDeclaredMethod("getConstInterceptorItr").invoke(null);
        final Object[] dynamicArgs = arguments;
        adviceCls.getDeclaredMethod("beforeConstructor",
                Object[].class, Constructor.class, ListIterator.class
        ).invoke(null, dynamicArgs, constructor, constInterceptorItr);
        arguments = dynamicArgs;
    }

    /**
     * 方法执行后调用
     *
     * @param obj                 生成的对象
     * @param arguments           所有入参
     * @param adviceCls           动态advice类
     * @param constInterceptorItr 构造拦截器双向迭代器
     * @throws Exception 发生异常
     */
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void OnMethodExit(
            @Advice.This(typing = Assigner.Typing.DYNAMIC) Object obj,
            @Advice.AllArguments Object[] arguments,
            @Advice.Local(value = "ADVICE_CLS") Class<?> adviceCls,
            @Advice.Local(value = "CONST_INTERCEPTOR_ITR") ListIterator<?> constInterceptorItr
    ) throws Exception {
        adviceCls.getDeclaredMethod("afterConstructor",
                Object.class, Object[].class, ListIterator.class
        ).invoke(null, obj, arguments, constInterceptorItr);
    }

    /**
     * 获取构造拦截器双向迭代器
     *
     * @return 构造拦截器双向迭代器
     */
    public static ListIterator<ConstructorInterceptor> getConstInterceptorItr() {
        return INTERCEPTORS.listIterator();
    }

    /**
     * 调用luban拦截器的onStart方法
     *
     * @param arguments           所有入参
     * @param constructor         构造函数本身
     * @param constInterceptorItr 构造拦截器双向迭代器
     */
    public static void beforeConstructor(Object[] arguments, Constructor<?> constructor,
            ListIterator<ConstructorInterceptor> constInterceptorItr) {
        final Object[] dynamicArgs = beforeOriginIntercept(arguments, constructor);
        if (dynamicArgs != arguments && dynamicArgs != null && dynamicArgs.length == arguments.length) {
            System.arraycopy(dynamicArgs, 0, arguments, 0, arguments.length);
        }
        beforeConstIntercept(arguments, constructor, constInterceptorItr);
    }

    /**
     * 调用luban拦截器的onStart方法
     *
     * @param arguments   所有入参
     * @param constructor 构造函数本身
     * @return 修正的参数列表
     */
    private static Object[] beforeOriginIntercept(Object[] arguments, Constructor<?> constructor) {
        if (ORIGIN_INTERCEPTOR == null) {
            return arguments;
        }
        try {
            final Object[] dynamicArgs = ORIGIN_INTERCEPTOR.onStart(
                    constructor.getDeclaringClass(), arguments, constructor.getName(), "constructor");
            if (dynamicArgs != null && dynamicArgs.length == arguments.length) {
                return dynamicArgs;
            }
        } catch (Throwable t) {
            LOGGER.severe(String.format(Locale.ROOT,
                    "invoke onStart method failed, class name:[{%s}], method name:[{%s}], reason:[{%s}]",
                    constructor.getDeclaringClass().getName(), "constructor", t.getMessage()));
        }
        return arguments;
    }

    /**
     * 构造拦截器空迭代（预留拓展空间）
     *
     * @param arguments           所有入参
     * @param constructor         构造函数本身
     * @param constInterceptorItr 构造拦截器双向迭代器
     */
    private static void beforeConstIntercept(Object[] arguments, Constructor<?> constructor,
            ListIterator<ConstructorInterceptor> constInterceptorItr) {
        while (constInterceptorItr.hasNext()) {
            constInterceptorItr.next();
            // do something maybe
        }
    }

    /**
     * 调用luban拦截器的onFinally方法和构造拦截器的onConstruct方法
     *
     * @param obj                 生成的对象
     * @param arguments           所有入参
     * @param constInterceptorItr 构造拦截器双向迭代器
     */
    public static void afterConstructor(Object obj, Object[] arguments,
            ListIterator<ConstructorInterceptor> constInterceptorItr) {
        afterConstIntercept(obj, arguments, constInterceptorItr);
        afterOriginIntercept(obj, arguments);
    }

    /**
     * 调用luban拦截器的onFinally方法
     *
     * @param obj       生成的对象
     * @param arguments 所有入参
     */
    private static void afterOriginIntercept(Object obj, Object[] arguments) {
        if (ORIGIN_INTERCEPTOR == null) {
            return;
        }
        try {
            ORIGIN_INTERCEPTOR.onFinally(obj, arguments, null, obj.getClass().getName(), "constructor");
        } catch (Throwable t) {
            LOGGER.severe(String.format(Locale.ROOT,
                    "invoke onFinally method failed, class name:[{%s}], method name:[{%s}], reason:[{%s}]",
                    obj.getClass().getName(), "constructor", t.getMessage()));
        }
    }

    /**
     * 调用构造拦截器的onConstruct方法
     *
     * @param obj                 生成的对象
     * @param arguments           所有入参
     * @param constInterceptorItr 构造拦截器双向迭代器
     */
    private static void afterConstIntercept(Object obj, Object[] arguments,
            ListIterator<ConstructorInterceptor> constInterceptorItr) {
        while (constInterceptorItr.hasPrevious()) {
            final ConstructorInterceptor interceptor = constInterceptorItr.previous();
            try {
                interceptor.onConstruct(obj, arguments);
            } catch (Throwable t) {
                LOGGER.severe(String.format(Locale.ROOT,
                        "An error occurred on construct [{%s}] in interceptor [{%s}]: [{%s}]",
                        obj.getClass().getName(), interceptor.getClass().getName(), t.getMessage()));
            }
        }
    }
}
