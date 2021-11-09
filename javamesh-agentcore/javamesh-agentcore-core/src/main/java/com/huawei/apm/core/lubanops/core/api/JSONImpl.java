package com.huawei.apm.core.lubanops.core.api;

import com.huawei.apm.core.lubanops.bootstrap.api.JSONAPI;
import com.huawei.apm.core.lubanops.integration.utils.JSON;

import java.util.List;

public class JSONImpl implements JSONAPI {

    @Override
    public String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }

    @Override
    public String toJSONString(Object obj, List<String> excludeKeys) {
        return JSON.toJSONString(obj, excludeKeys);
    }

    @Override
    public <T> T parseObject(String text, Class<T> type) {
        return JSON.parseObject(text, type);
    }

    @Override
    public int[] parseIntArray(String s) {
        return JSON.parseIntArray(s);
    }

    @Override
    public <T> List<T> parseList(String s, Class<T> type) {
        return JSON.parseList(s, type);
    }

}
