/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChannelMetricsCollectorTest {
    private ChannelMetricsCollector classUnderTest;

    @Mock
    private AMonitorJob aMonitorJob;

    @Mock
    private PCFMessageAgent pcfMessageAgent;

    @Mock
    private MetricWriteHelper metricWriteHelper;

    private MonitorContextConfiguration monitorContextConfig;
    private Map<String, WMQMetricOverride> channelMetricsToReport;
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
        channelMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_CHANNEL);
    }

    @Test
    public void testpublishMetrics() throws MQException, IOException, TaskExecutionException {
        when(pcfMessageAgent.send(any(PCFMessage.class))).thenReturn(createPCFResponseForInquireChannelStatusCmd());
        classUnderTest = new ChannelMetricsCollector(channelMetricsToReport, monitorContextConfig, pcfMessageAgent, queueManager, metricWriteHelper, Mockito.mock(CountDownLatch.class));
        classUnderTest.publishMetrics();
        verify(metricWriteHelper, times(3)).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Channels|DEV.ADMIN.SVRCONN|Status");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Channels|DEV.APP.SVRCONN|Status");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Channels|ActiveChannelsCount");

        for (List<Metric> metricList : pathCaptor.getAllValues()) {
            for (Metric metric : metricList) {
                if (metricPathsList.contains(metric.getMetricPath())) {
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Channels|DEV.ADMIN.SVRCONN|Status")) {
                        Assert.assertTrue(metric.getMetricValue().equals("3"));
                        Assert.assertFalse(metric.getMetricValue().equals("10"));
                    }
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Channels|DEV.APP.SVRCONN|Status")) {
                        Assert.assertTrue(metric.getMetricValue().equals("3"));
                    }
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Channels|ActiveChannelsCount")) {
                        Assert.assertTrue(metric.getMetricValue().equals("2"));
                    }
                }
            }
        }
    }

    /*
        Request
        PCFMessage:
        MQCFH [type: 1, strucLength: 36, version: 1, command: 42 (MQCMD_INQUIRE_CHANNEL_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 3]
        MQCFST [type: 4, strucLength: 24, parameter: 3501 (MQCACH_FIRST/MQCACH_CHANNEL_NAME), codedCharSetId: 0, stringLength: 1, string: *]
        MQCFIN [type: 3, strucLength: 16, parameter: 1523 (MQIACH_CHANNEL_INSTANCE_TYPE), value: 1011]
        MQCFIL [type: 5, strucLength: 48, parameter: 1524 (MQIACH_CHANNEL_INSTANCE_ATTRS), count: 8, values: {3501, 3506, 1527, 1534, 1538, 1535, 1539, 1536}]

        Response
        PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 1, command: 42 (MQCMD_INQUIRE_CHANNEL_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 11]
        MQCFST [type: 4, strucLength: 40, parameter: 3501 (MQCACH_FIRST/MQCACH_CHANNEL_NAME), codedCharSetId: 819, stringLength: 20, string: DEV.ADMIN.SVRCONN   ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1511 (MQIACH_CHANNEL_TYPE), value: 7]
        MQCFIN [type: 3, strucLength: 16, parameter: 1539 (MQIACH_BUFFERS_RCVD/MQIACH_BUFFERS_RECEIVED), value: 20]
        MQCFIN [type: 3, strucLength: 16, parameter: 1538 (MQIACH_BUFFERS_SENT), value: 19]
        MQCFIN [type: 3, strucLength: 16, parameter: 1536 (MQIACH_BYTES_RCVD/MQIACH_BYTES_RECEIVED), value: 5772]
        MQCFIN [type: 3, strucLength: 16, parameter: 1535 (MQIACH_BYTES_SENT), value: 6984]
        MQCFST [type: 4, strucLength: 284, parameter: 3506 (MQCACH_CONNECTION_NAME), codedCharSetId: 819, stringLength: 264, string: 172.17.0.1]
        MQCFIN [type: 3, strucLength: 16, parameter: 1523 (MQIACH_CHANNEL_INSTANCE_TYPE), value: 1011]
        MQCFIN [type: 3, strucLength: 16, parameter: 1534 (MQIACH_MSGS), value: 17]
        MQCFIN [type: 3, strucLength: 16, parameter: 1527 (MQIACH_CHANNEL_STATUS), value: 3]
        MQCFIN [type: 3, strucLength: 16, parameter: 1609 (MQIACH_CHANNEL_SUBSTATE), value: 300]
     */

    private PCFMessage[] createPCFResponseForInquireChannelStatusCmd() {
        PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS, 1, true);
        response1.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "DEV.ADMIN.SVRCONN");
        response1.addParameter(CMQCFC.MQIACH_CHANNEL_TYPE, 7);
        response1.addParameter(CMQCFC.MQIACH_BUFFERS_RECEIVED, 20);
        response1.addParameter(CMQCFC.MQIACH_BUFFERS_SENT, 19);
        response1.addParameter(CMQCFC.MQIACH_BYTES_RECEIVED, 5772);
        response1.addParameter(CMQCFC.MQIACH_BYTES_SENT, 6984);
        response1.addParameter(CMQCFC.MQCACH_CONNECTION_NAME, "172.17.0.1 ");
        response1.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, 1011);
        response1.addParameter(CMQCFC.MQIACH_MSGS, 17);
        response1.addParameter(CMQCFC.MQIACH_CHANNEL_STATUS, 3);
        response1.addParameter(CMQCFC.MQIACH_CHANNEL_SUBSTATE, 300);

        PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS, 2, true);
        response2.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "DEV.APP.SVRCONN");
        response2.addParameter(CMQCFC.MQIACH_CHANNEL_TYPE, 7);
        response2.addParameter(CMQCFC.MQIACH_BUFFERS_RECEIVED, 20);
        response2.addParameter(CMQCFC.MQIACH_BUFFERS_SENT, 19);
        response2.addParameter(CMQCFC.MQIACH_BYTES_RECEIVED, 5772);
        response2.addParameter(CMQCFC.MQIACH_BYTES_SENT, 6984);
        response2.addParameter(CMQCFC.MQCACH_CONNECTION_NAME, "172.17.0.2 ");
        response2.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, 1011);
        response2.addParameter(CMQCFC.MQIACH_MSGS, 17);
        response2.addParameter(CMQCFC.MQIACH_CHANNEL_STATUS, 3);
        response2.addParameter(CMQCFC.MQIACH_CHANNEL_SUBSTATE, 300);

        PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS, 2, true);
        response3.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "TEST.APP.SVRCONN");
        response3.addParameter(CMQCFC.MQIACH_CHANNEL_TYPE, 7);
        response3.addParameter(CMQCFC.MQIACH_BUFFERS_RECEIVED, 20);
        response3.addParameter(CMQCFC.MQIACH_BUFFERS_SENT, 19);
        response3.addParameter(CMQCFC.MQIACH_BYTES_RECEIVED, 5772);
        response3.addParameter(CMQCFC.MQIACH_BYTES_SENT, 6984);
        response3.addParameter(CMQCFC.MQCACH_CONNECTION_NAME, "172.17.0.2 ");
        response3.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, 1011);
        response3.addParameter(CMQCFC.MQIACH_MSGS, 17);
        response3.addParameter(CMQCFC.MQIACH_CHANNEL_STATUS, 3);
        response3.addParameter(CMQCFC.MQIACH_CHANNEL_SUBSTATE, 300);

        PCFMessage [] messages = {response1, response2, response3};
        return messages;
    }

}
