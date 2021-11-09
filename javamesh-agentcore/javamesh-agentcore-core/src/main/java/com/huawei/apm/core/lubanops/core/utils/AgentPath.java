package com.huawei.apm.core.lubanops.core.utils;

import com.google.common.base.Preconditions;
import com.huawei.apm.core.lubanops.bootstrap.exception.ApmRuntimeException;
import com.huawei.apm.core.lubanops.bootstrap.utils.StringUtils;

import java.util.Map;

/**
 * @author
 * @date 2020/11/17 17:59
 */
public class AgentPath {
    public final static String AGENT_PATH_COMMONS = "agentPath";

    public final static String BOOT_PATH_COMMONS = "bootPath";

    public final static String PLUGINS_PATH_COMMONS = "pluginsPath";

    private static AgentPath instance;

    private String agentPath;

    private String bootPath;

    private String pluginsPath;

    private AgentPath() {

    }

    public static AgentPath getInstance() {
        if (null == instance) {
            throw new ApmRuntimeException("[APM BOOTSTRAP]AgentPath has not instantiated.");
        }
        return instance;
    }

    public static AgentPath build(Map argsMap) {
        AgentPath agentPath = new AgentPath();
        agentPath.setAgentPath(argsMap.get(AGENT_PATH_COMMONS).toString());
        agentPath.setBootPath(argsMap.get(BOOT_PATH_COMMONS).toString());
        agentPath.setPluginsPath(argsMap.get(PLUGINS_PATH_COMMONS).toString());
        agentPath.checkIntegrity();
        instance = agentPath;
        return instance;
    }

    private void checkIntegrity() {
        Preconditions.checkArgument(!StringUtils.isBlank(agentPath), "[APM BOOTSTRAP]agentPath can't be resolved.");
        Preconditions.checkArgument(!StringUtils.isBlank(bootPath), "[APM BOOTSTRAP]bootPath can't be resolved.");
        Preconditions.checkArgument(!StringUtils.isBlank(pluginsPath), "[APM BOOTSTRAP]pluginsPath can't be resolved.");
    }

    public String getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(String agentPath) {
        this.agentPath = agentPath;
    }

    public String getBootPath() {
        return bootPath;
    }

    public void setBootPath(String bootPath) {
        this.bootPath = bootPath;
    }

    public String getPluginsPath() {
        return pluginsPath;
    }

    public void setPluginsPath(String pluginsPath) {
        this.pluginsPath = pluginsPath;
    }
}
