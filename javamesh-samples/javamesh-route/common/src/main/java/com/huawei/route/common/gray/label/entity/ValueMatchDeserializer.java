/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 */

package com.huawei.route.common.gray.label.entity;

import com.huawei.apm.core.lubanops.bootstrap.log.LogFactory;
import com.huawei.route.common.gray.constants.GrayConstant;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * json反序列化
 *
 * @author pengyuyi
 * @date 2021/10/27
 */
public class ValueMatchDeserializer implements ObjectDeserializer {
    private static final Logger LOGGER = LogFactory.getLogger();

    @Override
    public Map<String, List<MatchRule>> deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONObject args = parser.parseObject();
        // LinkedHashMap用为了保持顺序
        LinkedHashMap<String, List<MatchRule>> matchRuleMap = new LinkedHashMap<String, List<MatchRule>>();
        for (String key : args.keySet()) {
            matchRuleMap.put(key, getMatchRuleList(args, key));
        }
        return matchRuleMap;
    }

    private List<MatchRule> getMatchRuleList(JSONObject args, String key) {
        List<MatchRule> matchRuleList = new ArrayList<MatchRule>();
        List<JSONObject> array = args.getObject(key, new TypeReference<ArrayList<JSONObject>>() {
        });
        for (JSONObject matchRule : array) {
            matchRuleList.add(getMatchRule(matchRule));
        }
        return matchRuleList;
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }

    private void setField(MatchRule matchRule, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = MatchRule.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(matchRule, value);
    }

    private MatchRule getMatchRule(JSONObject json) {
        MatchRule matchRule = new MatchRule();
        ValueMatch valueMatch = new ValueMatch();
        for (Entry<String, Object> entry : json.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            try {
                setField(matchRule, fieldName, value);
            } catch (NoSuchFieldException e) {
                setValueMatchField(valueMatch, fieldName, value);
            } catch (IllegalAccessException e) {
                setValueMatchField(valueMatch, fieldName, value);
            }
        }
        if (GrayConstant.ENABLED_METHOD_NAME.equals(matchRule.getType())) {
            // 因为boolean型转string是小写，所以如果是.isEnabled()的类型，则强制把值转为小写，即boolean类型时强制对大小写不敏感
            ListIterator<String> listIterator = valueMatch.getValues().listIterator();
            while (listIterator.hasNext()) {
                listIterator.set(listIterator.next().toLowerCase(Locale.ROOT));
            }
        }
        matchRule.setValueMatch(valueMatch);
        return matchRule;
    }

    private void setValueMatchField(ValueMatch valueMatch, String fieldName, Object value) {
        MatchStrategy matchStrategy;
        try {
            matchStrategy = MatchStrategy.valueOf(fieldName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e1) {
            LOGGER.warning("Cannot find the match strategy " + fieldName);
            return;
        }
        List<String> values = new ArrayList<String>();
        if (MatchStrategy.IN.name().equalsIgnoreCase(fieldName)) {
            values.addAll(((JSONArray) value).toJavaList(String.class));
        } else {
            values.add((String) value);
        }
        valueMatch.setMatchStrategy(matchStrategy);
        valueMatch.setValues(values);
    }
}
