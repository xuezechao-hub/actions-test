/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 */

package com.huawei.flowcontrol.console.datasource.entity.rule.zookeeper;

import com.alibaba.fastjson.JSON;
import com.huawei.flowcontrol.console.entity.DegradeRuleVo;
import com.huawei.flowcontrol.console.entity.FlowRuleVo;
import com.huawei.flowcontrol.console.entity.ParamFlowRuleVo;
import com.huawei.flowcontrol.console.entity.SystemRuleVo;
import com.huawei.flowcontrol.console.rule.DynamicRuleProviderExt;
import com.huawei.flowcontrol.console.util.DataType;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * provider类
 *
 * @author XiaoLong Wang
 * @since 2020-12-21
 */
@Component
public class RuleZookeeperProvider implements DynamicRuleProviderExt<List<?>> {
    @Autowired
    private CuratorFramework zkClient;

    @Override
    public List<?> getRules(String appName) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<FlowRuleVo> getFlowRules(String appName) throws Exception {
        return JSON.parseArray(getZookeeperData(appName, DataType.FLOW.getDataType()), FlowRuleVo.class);
    }

    @Override
    public List<DegradeRuleVo> getDegradeRules(String appName) throws Exception {
        return JSON.parseArray(getZookeeperData(appName, DataType.DEGRADE.getDataType()), DegradeRuleVo.class);
    }

    @Override
    public List<ParamFlowRuleVo> getParamFlowRules(String appName) throws Exception {
        return JSON.parseArray(getZookeeperData(appName, DataType.PARAMFLOW.getDataType()), ParamFlowRuleVo.class);
    }

    @Override
    public List<SystemRuleVo> getSystemRules(String appName) throws Exception {
        return JSON.parseArray(getZookeeperData(appName, DataType.SYSTEM.getDataType()), SystemRuleVo.class);
    }

    /**
     * 根据app和规则类型，获取规则数据
     *
     * @param appName    应用名
     * @param entityType 应用类型
     * @return 规则数据
     * @throws Exception 异常
     */
    private String getZookeeperData(String appName, String entityType) throws Exception {
        // 根据应用名+规则类型获取数据
        String zkPath = ZookeeperConfigUtil.getPath(appName + entityType);
        Stat stat = zkClient.checkExists().forPath(zkPath);

        if (stat == null) {
            return "";
        }

        byte[] bytes = zkClient.getData().forPath(zkPath);
        if ((bytes == null) || (bytes.length == 0)) {
            return "";
        }

        return new String(bytes, "utf-8");
    }
}