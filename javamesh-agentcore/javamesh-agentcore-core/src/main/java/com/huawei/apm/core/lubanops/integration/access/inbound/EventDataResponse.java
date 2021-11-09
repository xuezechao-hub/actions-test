package com.huawei.apm.core.lubanops.integration.access.inbound;

import com.huawei.apm.core.lubanops.integration.access.MessageType;

/**
 * @author
 * @since 2020/5/7
 **/
public class EventDataResponse extends CommonResponse {
    public EventDataResponse(long msgId) {
        super(MessageType.TRACE_EVENT_RESPONSE, msgId);
    }
}
