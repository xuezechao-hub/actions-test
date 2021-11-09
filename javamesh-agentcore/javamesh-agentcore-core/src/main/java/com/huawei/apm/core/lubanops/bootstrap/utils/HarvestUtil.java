package com.huawei.apm.core.lubanops.bootstrap.utils;

import java.util.concurrent.atomic.AtomicLong;

import com.huawei.apm.core.lubanops.bootstrap.collector.api.MonitorDataRow;

public class HarvestUtil {

    public static long getMetricCount(AtomicLong allCount, long oldCount, String key, MonitorDataRow row) {
        long newCount = allCount.get();
        if (row != null) {
            row.put(key, newCount - oldCount);
        }
        return newCount;
    }

}
