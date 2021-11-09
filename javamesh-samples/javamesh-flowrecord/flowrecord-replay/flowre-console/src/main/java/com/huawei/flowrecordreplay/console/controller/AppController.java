/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.flowrecordreplay.console.controller;

import com.huawei.flowrecordreplay.console.domain.Result;
import com.huawei.flowrecordreplay.console.rtc.common.redis.RedisUtil;
import com.huawei.flowrecordreplay.console.util.Constant;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 查询应用机器列表接口
 *
 * @author lilai
 * @version 0.0.1
 * @since 2021-02-26
 */
@RestController
@RequestMapping("/application")
public class AppController {
    @Value("${appnameKey:appnames}")
    private String appnameKey;

    @Value("${heartbeatflag::heartbeat}")
    private String heartbeatFlag;

    /**
     * 自动注入redis工具类的对象
     */
    @Autowired
    private RedisUtil redisUtil;

    @GetMapping("/apps")
    public Result<Set<String>> getAppList() {
        Set<String> set = redisUtil.getSet(appnameKey);
        if (CollectionUtils.isEmpty(set)) {
            return Result.ofFail(Constant.ERROR_CODE, "Fail to query application list.");
        }
        return Result.ofSuccess(set);
    }

    @GetMapping("/{application}/machines")
    public Result<Set<String>> getMachineList(@PathVariable String application) {
        if (StringUtils.isBlank(application)) {
            return Result.ofFail(Constant.ERROR_CODE, "Application can't be null or empty.");
        }
        return Result.ofSuccess(redisUtil.getHashFieldsByKey(application + heartbeatFlag));
    }
}

