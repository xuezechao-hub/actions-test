/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 */

package com.lubanops.stresstest.core;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.huawei.apm.core.lubanops.bootstrap.utils.StringUtils;
import com.lubanops.stresstest.config.ConfigFactory;

import java.util.Locale;

/**
 *  全链路压测标记
 *
 * @author yiwei
 * @since 2021/10/21
 */
public class Tester {
    /**
     * 压测标记key
     */
    public static final String TEST_FLAG = "x-test";
    /**
     * 压测标记值
     */
    public static final String TEST_VALUE = "true";
    /**
     * TransmittableThreadLocal 确保除了在本线程内部传递，还能在本地线程之间，本地线程池之间传递。
     */
    private static volatile TransmittableThreadLocal<Boolean> flags = new TransmittableThreadLocal<>();

    /**
     * Set test flag.
     *
     * @param test test flag.
     */
    public static void setTest(boolean test) {
        flags.set(test);
    }

    /**
     * Check if the test flag exists.
     *
     * @return true if this is test flag is set, false otherwise.
     */
    public static boolean isTest() {
        Boolean res = flags.get();
        return res != null && res;
    }

    /**
     * 检查当前topic是否是影子topic
     * @param topic 待检查topic
     * @return 影子topic返回true，否则 false
     */
    public static boolean isTestTopic(String topic) {
        return isTestPrefix(topic, ConfigFactory.getConfig().getTestTopicPrefix());
    }

    /**
     * 检查当前字段是否已prefix开头
     * @param topic 待检查字段
     * @param prefix  prefix
     * @return 已prefix开头返回true，否则false
     */
    public static boolean isTestPrefix(String topic, String prefix) {
        return StringUtils.isNotBlank(topic) && topic.toLowerCase(Locale.ROOT).startsWith(prefix);
    }
}
