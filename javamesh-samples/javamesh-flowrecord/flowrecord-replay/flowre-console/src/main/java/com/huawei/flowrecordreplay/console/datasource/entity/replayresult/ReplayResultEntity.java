/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.flowrecordreplay.console.datasource.entity.replayresult;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 回放请求的详细比对结果
 *
 * @author luanwenfei
 * @version 0.0.1
 * @since 2021-04-08
 */
@Getter
@Setter
public class ReplayResultEntity {
    /**
     * 录制的trace id
     */
    private String traceId;

    /**
     * 请求的录制时间
     */
    private String recordTime;

    /**
     * 请求的回放时间
     */
    private String replayTime;

    /**
     * 请求的接口名
     */
    private String method;

    /**
     * 回放结果的字段比对列表
     */
    private List<FieldCompare> fieldCompare;

    /**
     * 回放结果
     */
    private boolean compareResult;

    /**
     * 用于重新进行结果比对的方法
     *
     * @param ignoreFieldEntity 接口对应的忽略字段列表
     */
    public void reCompare(IgnoreFieldEntity ignoreFieldEntity) {
        boolean reCompareResult = true;
        List<FieldCompare> fieldReCompares = new ArrayList<>();
        for (FieldCompare fieldReCompare : this.fieldCompare) {
            for (Field field : ignoreFieldEntity.getFields()) {
                if (fieldReCompare.getName().equals(field.getName())) {
                    fieldReCompare.setIgnore(field.isIgnore());
                    if (!field.isIgnore()) {
                        reCompareResult = reCompareResult && fieldReCompare.isCompare();
                    }
                    fieldReCompares.add(fieldReCompare);
                }
            }
        }
        this.compareResult = reCompareResult;
        this.fieldCompare = fieldReCompares;
    }
}

/**
 * 回放结果中每个字段的比对结果
 *
 * @author luanwenfei
 * @version 0.0.1
 * @since 2021-04-08
 */
@Getter
@Setter
class FieldCompare {
    /**
     * 字段名
     */
    private String name;

    /**
     * 录制得到的字段
     */
    private String record;

    /**
     * 回放得到的字段
     */
    private String replay;

    /**
     * 字段的忽略情况
     */
    private boolean ignore;

    /**
     * 字段的比对结果
     */
    private boolean compare;
}
