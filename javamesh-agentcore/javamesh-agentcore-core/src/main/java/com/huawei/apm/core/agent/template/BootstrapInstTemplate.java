/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.core.agent.template;

import java.lang.reflect.Method;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.logging.Logger;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import com.huawei.apm.core.agent.common.BeforeResult;
import com.huawei.apm.core.agent.interceptor.InstanceMethodInterceptor;
import com.huawei.apm.core.lubanops.bootstrap.Interceptor;
import com.huawei.apm.core.lubanops.bootstrap.log.LogFactory;

/**
 * 启动类实例方法模板
 * <p>启动类加载器加载类的实例方法如果需要增强，则需要使用该模板
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/10/27
 */
public class BootstrapInstTemplate {
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
    public static List<InstanceMethodInterceptor> INTERCEPTORS;

    /**
     * 方法执行前调用
     * <p>由于类加载器限制，需要使用反射调用外部方法，需要构建出动态advice类的全限定名，再用当前类加载器加载
     * <p>由于jvm重定义的限制，不能添加静态属性，动态advice类只能通过局部参数传递
     *
     * @param obj                被增强对象
     * @param method             被增强方法
     * @param arguments          所有参数
     * @param adviceCls          动态advice类
     * @param instInterceptorItr 实例拦截器的双向迭代器
     * @return 是否进行主要流程
     * @throws Exception 发生异常
     */
    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnDefaultValue.class)
    public static boolean onMethodEnter(
            @Advice.This(typing = Assigner.Typing.DYNAMIC) Object obj,
            @Advice.Origin Method method,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] arguments,
            @Advice.Local(value = "ADVICE_CLS") Class<?> adviceCls,
            @Advice.Local(value = "INST_INTERCEPTOR_ITR") ListIterator<?> instInterceptorItr
    ) throws Exception {
        final StringBuilder builder = new StringBuilder()
                .append(method.getDeclaringClass().getName())
                .append('#')
                .append(method.getName())
                .append("(");
        final Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes[i].getName());
        }
        builder.append(')');
        final String adviceClsName = "com.huawei.apm.core.agent.template.BootstrapInstTemplate_" +
                Integer.toHexString(builder.toString().hashCode());
        adviceCls = ClassLoader.getSystemClassLoader().loadClass(adviceClsName);
        instInterceptorItr = (ListIterator<?>) adviceCls.getDeclaredMethod("getInstInterceptorItr").invoke(null);
        final Object[] dynamicArgs = arguments;
        final Boolean res = (Boolean) adviceCls.getDeclaredMethod("beforeInstMethod",
                Object.class, Method.class, Object[].class, ListIterator.class
        ).invoke(null, obj, method, dynamicArgs, instInterceptorItr);
        arguments = dynamicArgs;
        return res;
    }

    /**
     * 方法执行后调用
     *
     * @param obj                被拦截对象
     * @param method             被拦截方法
     * @param arguments          所有参数
     * @param result             调用结果
     * @param throwable          抛出异常
     * @param adviceCls          动态advice类
     * @param instInterceptorItr 实例拦截器的双向迭代器
     * @throws Exception 调用异常
     */
    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void OnMethodExit(
            @Advice.This(typing = Assigner.Typing.DYNAMIC) Object obj,
            @Advice.Origin Method method,
            @Advice.AllArguments Object[] arguments,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result,
            @Advice.Thrown Throwable throwable,
            @Advice.Local(value = "ADVICE_CLS") Class<?> adviceCls,
            @Advice.Local(value = "INST_INTERCEPTOR_ITR") ListIterator<?> instInterceptorItr) throws Exception {
        result = adviceCls.getDeclaredMethod("afterInstMethod",
                Object.class, Method.class, Object[].class, Object.class, Throwable.class, ListIterator.class
        ).invoke(null, obj, method, arguments, result, throwable, instInterceptorItr);
    }

    /**
     * 获取实例拦截器的双向迭代器
     *
     * @return 实例拦截器的双向迭代器
     */
    public static ListIterator<InstanceMethodInterceptor> getInstInterceptorItr() {
        return INTERCEPTORS.listIterator();
    }

    /**
     * 调用luban拦截器的onStart方法和实例拦截器的before方法
     *
     * @param obj                被拦截的对象
     * @param method             被拦截的方法
     * @param arguments          所有参数
     * @param instInterceptorItr 实例拦截器的双向迭代器
     * @return 是否进行主要流程
     */
    public static boolean beforeInstMethod(Object obj, Method method, Object[] arguments,
            ListIterator<InstanceMethodInterceptor> instInterceptorItr) {
        final Object[] dynamicArgs = beforeOriginIntercept(obj, method, arguments);
        if (dynamicArgs != arguments && dynamicArgs != null && dynamicArgs.length == arguments.length) {
            System.arraycopy(dynamicArgs, 0, arguments, 0, arguments.length);
        }
        return beforeInstIntercept(obj, method, arguments, instInterceptorItr);
    }

    /**
     * 调用luban拦截器的onStart方法
     *
     * @param obj       被拦截的对象
     * @param method    被拦截的方法
     * @param arguments 所有参数
     * @return 修正的参数列表
     */
    private static Object[] beforeOriginIntercept(Object obj, Method method, Object[] arguments) {
        if (ORIGIN_INTERCEPTOR == null) {
            return arguments;
        }
        try {
            final Object[] dynamicArgs = ORIGIN_INTERCEPTOR.onStart(
                    obj, arguments, obj.getClass().getName(), method.getName());
            if (dynamicArgs != null && dynamicArgs.length == arguments.length) {
                return dynamicArgs;
            }
        } catch (Throwable t) {
            LOGGER.severe(String.format(Locale.ROOT,
                    "invoke onStart method failed, class name:[{%s}], method name:[{%s}], reason:[{%s}]",
                    obj.getClass().getName(), method.getName(), t.getMessage()));
        }
        return arguments;
    }

    /**
     * 调用实例拦截器的before方法
     *
     * @param obj                被拦截的对象
     * @param method             被拦截的方法
     * @param arguments          所有参数
     * @param instInterceptorItr 实例拦截器的双向迭代器
     * @return 是否进行主要流程
     */
    private static boolean beforeInstIntercept(Object obj, Method method, Object[] arguments,
            ListIterator<InstanceMethodInterceptor> instInterceptorItr) {
        final BeforeResult beforeResult = new BeforeResult();
        while (instInterceptorItr.hasNext()) {
            final InstanceMethodInterceptor interceptor = instInterceptorItr.next();
            try {
                interceptor.before(obj, method, arguments, beforeResult);
            } catch (Throwable t) {
                LOGGER.severe(String.format(Locale.ROOT,
                        "An error occurred before [{%s}#{%s}] in interceptor [{%s}]: [{%s}]",
                        obj.getClass().getName(), method.getName(), interceptor.getClass().getName(),
                        t.getMessage()));
            }
            if (!beforeResult.isContinue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 调用luban拦截器的onFinally、onError方法和实例拦截器的after、onThrow方法
     *
     * @param obj                被拦截的对象
     * @param method             被拦截方法
     * @param arguments          所有参数
     * @param result             调用结果
     * @param throwable          抛出异常
     * @param instInterceptorItr 实例拦截器的双向迭代器
     * @return 调用结果
     */
    public static Object afterInstMethod(Object obj, Method method, Object[] arguments, Object result,
            Throwable throwable, ListIterator<InstanceMethodInterceptor> instInterceptorItr) {
        result = afterInstIntercept(obj, method, arguments, result, throwable, instInterceptorItr);
        afterOriginIntercept(obj, method, arguments, result, throwable);
        return result;
    }

    /**
     * 调用luban拦截器的onFinally、onError方法
     *
     * @param obj       被拦截的对象
     * @param method    被拦截方法
     * @param arguments 所有参数
     * @param result    调用结果
     * @param throwable 抛出异常
     */
    private static void afterOriginIntercept(Object obj, Method method, Object[] arguments, Object result,
            Throwable throwable) {
        if (ORIGIN_INTERCEPTOR == null) {
            return;
        }
        if (throwable != null) {
            try {
                ORIGIN_INTERCEPTOR.onError(obj, arguments, throwable, obj.getClass().getName(), method.getName());
            } catch (Throwable t) {
                LOGGER.severe(String.format(Locale.ROOT,
                        "invoke onError method failed, class name:[{%s}], method name:[{%s}], reason:[{%s}]",
                        obj.getClass().getName(), method.getName(), t.getMessage()));
            }
        }
        try {
            ORIGIN_INTERCEPTOR.onFinally(obj, arguments, result, obj.getClass().getName(), method.getName());
        } catch (Throwable t) {
            LOGGER.severe(String.format(Locale.ROOT,
                    "invoke onFinally method failed, class name:[{%s}], method name:[{%s}], reason:[{%s}]",
                    obj.getClass().getName(), method.getName(), t.getMessage()));
        }
    }

    /**
     * 调用实例拦截器的after、onThrow方法
     *
     * @param obj                被拦截的对象
     * @param method             被拦截方法
     * @param arguments          所有参数
     * @param result             调用结果
     * @param throwable          抛出异常
     * @param instInterceptorItr 实例拦截器的双向迭代器
     * @return 调用结果
     */
    private static Object afterInstIntercept(Object obj, Method method, Object[] arguments, Object result,
            Throwable throwable, ListIterator<InstanceMethodInterceptor> instInterceptorItr) {
        while (instInterceptorItr.hasPrevious()) {
            final InstanceMethodInterceptor interceptor = instInterceptorItr.previous();
            if (throwable != null) {
                try {
                    interceptor.onThrow(obj, method, arguments, throwable);
                } catch (Throwable t) {
                    LOGGER.severe(String.format(Locale.ROOT, "An error occurred while handling throwable thrown " +
                                    "by [{%s}#{%s}] in interceptor [{%s}]: [{%s}].", obj.getClass().getName(),
                            method.getName(), interceptor.getClass().getName(), t.getMessage()));
                }
            }
            try {
                result = interceptor.after(obj, method, arguments, result);
            } catch (Throwable t) {
                LOGGER.severe(String.format(Locale.ROOT,
                        "An error occurred after [{%s}#{%s}] in interceptor [{%s}]: [{%s}].",
                        obj.getClass().getName(), method.getName(), interceptor.getClass().getName(),
                        t.getMessage()));
            }
        }
        return result;
    }
}
