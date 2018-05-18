/*
 * Copyright [YEAR]. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.common.WMQUtil;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.collect.Lists;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ListenerMetricsCollector.class})
@PowerMockIgnore("javax.management.*")
public class ListenerMetricsCollectorTest {
    private ListenerMetricsCollector classUnderTest;
    @Mock
    private AMonitorJob aMonitorJob;

    @Mock
    private PCFMessageAgent pcfMessageAgent;

    @Mock
    private MetricWriteHelper metricWriteHelper;

    private MonitorContextConfiguration monitorContextConfig;
    private Map<String, WMQMetricOverride> listenerMetricsToReport;
    private QueueManager queueManager;
    ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

    @Before
    public void setup() {
        monitorContextConfig = new MonitorContextConfiguration("WMQMonitor", "Custom Metrics|WMQMonitor|", PathResolver.resolveDirectory(AManagedMonitor.class),aMonitorJob);
        monitorContextConfig.setConfigYml("src/test/resources/conf/config.yml");
        Map<String, ?> configMap = monitorContextConfig.getConfigYml();
        ObjectMapper mapper = new ObjectMapper();
        queueManager = mapper.convertValue(((List)configMap.get("queueManagers")).get(0), QueueManager.class);
        Map<String, Map<String, WMQMetricOverride>> metricsMap = WMQUtil.getMetricsToReportFromConfigYml((List<Map>) configMap.get("mqMetrics"));
        listenerMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_LISTENER);
    }

    @Test
    public void testpublishMetrics() throws MQException, IOException, TaskExecutionException {
        when(pcfMessageAgent.send(any(PCFMessage.class))).thenReturn(createPCFResponseForInquireListenerStatusCmd());
        classUnderTest = new ListenerMetricsCollector(listenerMetricsToReport, monitorContextConfig, pcfMessageAgent, queueManager, metricWriteHelper, Mockito.mock(Phaser.class));
        classUnderTest.publishMetrics();
        verify(metricWriteHelper, times(2)).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Listeners|DEV.DEFAULT.LISTENER.TCP|Status");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Listeners|DEV.LISTENER.TCP|Status");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Listeners|SYSTEM.DEFAULT.LISTENER.TCP|Status");

        for (List<Metric> metricList : pathCaptor.getAllValues()) {
            for (Metric metric : metricList) {
                if (metricPathsList.contains(metric.getMetricPath())) {
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Listeners|DEV.DEFAULT.LISTENER.TCP|Status")) {
                        Assert.assertTrue(metric.getMetricValue().equals("2"));
                        Assert.assertFalse(metric.getMetricValue().equals("10"));
                    }
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Listeners|DEV.LISTENER.TCP|Status")) {
                        Assert.assertTrue(metric.getMetricValue().equals("3"));
                    }
                }
            }
        }
    }

    /*
        Request
        PCFMessage:
        MQCFH [type: 1, strucLength: 36, version: 1, command: 98 (MQCMD_INQUIRE_LISTENER_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 2]
        MQCFST [type: 4, strucLength: 24, parameter: 3554 (MQCACH_LISTENER_NAME), codedCharSetId: 0, stringLength: 1, string: *]
        MQCFIL [type: 5, strucLength: 24, parameter: 1223 (MQIACF_LISTENER_STATUS_ATTRS), count: 2, values: {3554, 1599}]

        Response
        PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 1, command: 98 (MQCMD_INQUIRE_LISTENER_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 2]
        MQCFST [type: 4, strucLength: 48, parameter: 3554 (MQCACH_LISTENER_NAME), codedCharSetId: 819, stringLength: 27, string: SYSTEM.DEFAULT.LISTENER.TCP]
        MQCFIN [type: 3, strucLength: 16, parameter: 1599 (MQIACH_LISTENER_STATUS), value: 2]
     */

    private PCFMessage[] createPCFResponseForInquireListenerStatusCmd() {
        PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS, 1, true);
        response1.addParameter(CMQCFC.MQCACH_LISTENER_NAME, "DEV.DEFAULT.LISTENER.TCP");
        response1.addParameter(CMQCFC.MQIACH_LISTENER_STATUS, 2);

        PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS, 2, true);
        response2.addParameter(CMQCFC.MQCACH_LISTENER_NAME, "DEV.LISTENER.TCP");
        response2.addParameter(CMQCFC.MQIACH_LISTENER_STATUS, 3);

        PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS, 3, true);
        response3.addParameter(CMQCFC.MQCACH_LISTENER_NAME, "SYSTEM.LISTENER.TCP");
        response3.addParameter(CMQCFC.MQIACH_LISTENER_STATUS, 1);

        PCFMessage [] messages = {response1, response2, response3};
        return messages;
    }

}
