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
import com.ibm.mq.constants.CMQC;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TopicMetricsCollectorTest {
    private TopicMetricsCollector classUnderTest;

    @Mock
    private AMonitorJob aMonitorJob;

    @Mock
    private PCFMessageAgent pcfMessageAgent;

    @Mock
    private MetricWriteHelper metricWriteHelper;

    private MonitorContextConfiguration monitorContextConfig;
    private Map<String, WMQMetricOverride> topicMetricsToReport;
    private QueueManager queueManager;
    ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

    @Before
    public void setup() {
        monitorContextConfig = new MonitorContextConfiguration("WMQMonitor", "Custom Metrics|WMQMonitor|", PathResolver.resolveDirectory(AManagedMonitor.class), aMonitorJob);
        monitorContextConfig.setConfigYml("src/test/resources/conf/config.yml");
        Map<String, ?> configMap = monitorContextConfig.getConfigYml();
        ObjectMapper mapper = new ObjectMapper();
        queueManager = mapper.convertValue(((List)configMap.get("queueManagers")).get(0), QueueManager.class);
        Map<String, Map<String, WMQMetricOverride>> metricsMap = WMQUtil.getMetricsToReportFromConfigYml((List<Map>) configMap.get("mqMetrics"));
        topicMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_TOPIC);
    }

    @Test
    public void testpublishMetrics() throws MQException, IOException, TaskExecutionException {
        when(pcfMessageAgent.send(any(PCFMessage.class))).thenReturn(createPCFResponseForInquireTopicStatusCmd());
        classUnderTest = new TopicMetricsCollector(topicMetricsToReport, monitorContextConfig, pcfMessageAgent, queueManager, metricWriteHelper, Mockito.mock(CountDownLatch.class));
        classUnderTest.publishMetrics();
        verify(metricWriteHelper, times(2)).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Topics|test|PublishCount");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Topics|dev|SubscriptionCount");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Topics|system|PublishCount");

        for (List<Metric> metricList : pathCaptor.getAllValues()) {
            for (Metric metric : metricList) {
                if (metricPathsList.contains(metric.getMetricPath())) {
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Topics|test|PublishCount")) {
                        Assert.assertTrue(metric.getMetricValue().equals("2"));
                        Assert.assertFalse(metric.getMetricValue().equals("10"));
                    }
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|dev|SubscriptionCount")) {
                        Assert.assertTrue(metric.getMetricValue().equals("4"));
                    }
                }
            }
        }
    }


    private PCFMessage[] createPCFResponseForInquireTopicStatusCmd() {
        PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, 1, false);
        response1.addParameter(CMQC.MQCA_TOPIC_STRING, "test");
        response1.addParameter(CMQC.MQIA_PUB_COUNT, 2);
        response1.addParameter(CMQC.MQIA_SUB_COUNT, 3);

        PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, 2, false);
        response2.addParameter(CMQC.MQCA_TOPIC_STRING, "dev");
        response2.addParameter(CMQC.MQIA_PUB_COUNT, 3);
        response2.addParameter(CMQC.MQIA_SUB_COUNT, 4);

        PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, 3, false);
        response3.addParameter(CMQC.MQCA_TOPIC_STRING, "system");
        response3.addParameter(CMQC.MQIA_PUB_COUNT, 5);
        response3.addParameter(CMQC.MQIA_SUB_COUNT, 6);

        PCFMessage [] messages = {response1, response2, response3};
        return messages;
    }


}
