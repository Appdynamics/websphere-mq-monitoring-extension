package com.appdynamics.extensions.webspheremq.config;


import com.appdynamics.extensions.webspheremq.WMQMonitor;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

public class WebsphereMQMonitorTest {
    public static final String CONFIG_ARG = "config-file";

    @Test
    public void testWMQMonitor() throws ClassNotFoundException, TaskExecutionException {
        WMQMonitor mqMonitor = new WMQMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yaml");
        mqMonitor.execute(taskArgs, null);
    }


}
