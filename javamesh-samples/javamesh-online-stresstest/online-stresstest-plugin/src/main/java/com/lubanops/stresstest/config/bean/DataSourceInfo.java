/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 */

package com.lubanops.stresstest.config.bean;

/**
 * 数据库信息
 *
 * @author yiwei
 * @since 2021/10/21
 */
public class DataSourceInfo extends UserInfo {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
