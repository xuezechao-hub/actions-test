/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 */

package com.huawei.flowcontrol.console.datasource.entity.rule.kie.controller;

import com.huawei.flowcontrol.console.datasource.entity.rule.kie.operator.RuleKieProvider;
import com.huawei.flowcontrol.console.datasource.entity.rule.kie.operator.RuleKiePublisher;
import com.huawei.flowcontrol.console.datasource.entity.rule.wrapper.SystemControllerWrapper;
import com.huawei.flowcontrol.console.entity.Result;
import com.huawei.flowcontrol.console.entity.SystemRuleVo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

import static com.huawei.flowcontrol.console.util.SystemUtils.ERROR_CODE;

/**
 * System rule controller (v2).
 *
 * @author Sherlockhan
 * @since 2020-12-21
 */
@Slf4j
@Component("servicecomb-kie-systemRule")
public class KieSystemControllerWrapper extends KieRuleCommonController<SystemRuleVo>
    implements SystemControllerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KieSystemControllerWrapper.class);

    @Autowired
    @Qualifier("systemRuleKieProvider")
    private RuleKieProvider<List<SystemRuleVo>> ruleProvider;
    @Autowired
    @Qualifier("systemRuleKiePublisher")
    private RuleKiePublisher<List<SystemRuleVo>> rulePublisher;

    public KieSystemControllerWrapper(
        @Autowired
        @Qualifier("systemRuleKiePublisher")
            RuleKiePublisher<List<SystemRuleVo>> rulePublisher) {
        super(rulePublisher);
    }

    @Override
    public Result<List<SystemRuleVo>> apiQueryRules(String app) {
        try {
            List<SystemRuleVo> rules = ruleProvider.getRules(app);
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            LOGGER.error("Error when querying system rules", throwable);
            return Result.ofThrowable(ERROR_CODE, throwable);
        }
    }

    @Override
    public Result<SystemRuleVo> apiUpdateRule(HttpServletRequest httpServletRequest, SystemRuleVo entity) {
        String app = entity.getApp();
        try {
            rulePublisher.update(app, Collections.singletonList(entity));
            return Result.ofSuccess(entity);
        } catch (Throwable throwable) {
            LOGGER.error("Error when update system rules", throwable);
            return Result.ofThrowable(ERROR_CODE, throwable);
        }
    }

    @Override
    public Result<SystemRuleVo> apiAddRule(HttpServletRequest httpServletRequest, SystemRuleVo entity) {
        String app = entity.getApp();
        try {
            rulePublisher.add(app, Collections.singletonList(entity));
        } catch (Throwable throwable) {
            LOGGER.error("Error when add system rules", throwable);
            return Result.ofThrowable(ERROR_CODE, throwable);
        }
        return Result.ofSuccess(entity);
    }

    @Override
    public Result<String> apiDeleteRule(HttpServletRequest httpServletRequest, long id, String extInfo, String app) {
        return super.apiDeleteRule(httpServletRequest, id, extInfo, app);
    }
}
