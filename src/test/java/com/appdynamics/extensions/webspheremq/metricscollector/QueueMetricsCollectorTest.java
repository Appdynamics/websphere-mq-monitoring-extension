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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QueueMetricsCollectorTest {
    private QueueMetricsCollector classUnderTest;

    @Mock
    private AMonitorJob aMonitorJob;

    @Mock
    private PCFMessageAgent pcfMessageAgent;

    @Mock
    private MetricWriteHelper metricWriteHelper;

    @Mock
    private CountDownLatch phaser;

    private MonitorContextConfiguration monitorContextConfig;
    private Map<String, WMQMetricOverride> queueMetricsToReport;
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
        queueMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_QUEUE);
    }

    @Test
    public void testProcessPCFRequestAndPublishQMetricsForInquireQStatusCmd() throws MQException, IOException {
        PCFMessage request = createPCFRequestForInquireQStatusCmd();
        when(pcfMessageAgent.send(request)).thenReturn(createPCFResponseForInquireQStatusCmd());
        classUnderTest = new QueueMetricsCollector(queueMetricsToReport, monitorContextConfig, pcfMessageAgent, queueManager, metricWriteHelper, phaser);
        classUnderTest.processPCFRequestAndPublishQMetrics("*", request, "MQCMD_INQUIRE_Q_STATUS");

        verify(metricWriteHelper, times(2)).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.QUEUE.1|UncommittedMsgs");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|OldestMsgAge");

        for (List<Metric> metricList : pathCaptor.getAllValues()) {
            for (Metric metric : metricList) {
                if (metricPathsList.contains(metric.getMetricPath())) {
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.QUEUE.1|UncommittedMsgs")) {
                        Assert.assertTrue(metric.getMetricValue().equals("10"));
                    }
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|OldestMsgAge")) {
                        Assert.assertTrue(metric.getMetricValue().equals("-1"));
                    }
                }
            }
        }
    }

    @Test
    public void testProcessPCFRequestAndPublishQMetricsForInquireQCmd() throws MQException, IOException {
        PCFMessage request = createPCFRequestForInquireQCmd();
        when(pcfMessageAgent.send(request)).thenReturn(createPCFResponseForInquireQCmd());
        classUnderTest = new QueueMetricsCollector(queueMetricsToReport, monitorContextConfig, pcfMessageAgent, queueManager, metricWriteHelper, phaser);
        classUnderTest.processPCFRequestAndPublishQMetrics("*", request, "MQCMD_INQUIRE_Q");

        verify(metricWriteHelper, times(2)).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.QUEUE.1|CurrentQueueDepth");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|CurrentQueueDepth");

        for (List<Metric> metricList : pathCaptor.getAllValues()) {
            for (Metric metric : metricList) {
                if (metricPathsList.contains(metric.getMetricPath())) {
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.QUEUE.1|CurrentQueueDepth")) {
                        Assert.assertTrue(metric.getMetricValue().equals("3"));
                    }
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|CurrentQueueDepth")) {
                        Assert.assertTrue(metric.getMetricValue().equals("2"));
                    }
                }
            }
        }
    }

    @Test
    public void testProcessPCFRequestAndPublishQMetricsForResetQStatsCmd() throws MQException, IOException {
        PCFMessage request = createPCFRequestForResetQStatsCmd();
        when(pcfMessageAgent.send(request)).thenReturn(createPCFResponseForResetQStatsCmd());
        classUnderTest = new QueueMetricsCollector(queueMetricsToReport, monitorContextConfig, pcfMessageAgent, queueManager, metricWriteHelper, phaser);
        classUnderTest.processPCFRequestAndPublishQMetrics("*", request, "MQCMD_RESET_Q_STATS");

        verify(metricWriteHelper, times(1)).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|HighQDepth");
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|MsgEnqCount");

        for (List<Metric> metricList : pathCaptor.getAllValues()) {
            for (Metric metric : metricList) {
                if (metricPathsList.contains(metric.getMetricPath())) {
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|HighQDepth")) {
                        Assert.assertTrue(metric.getMetricValue().equals("10"));
                    }
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Queues|DEV.DEAD.LETTER.QUEUE|MsgEnqCount")) {
                        Assert.assertTrue(metric.getMetricValue().equals("3"));
                    }
                }
            }
        }
    }


    /*
        PCFMessage:
        MQCFH [type: 1, strucLength: 36, version: 1, command: 41 (MQCMD_INQUIRE_Q_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 2]
        MQCFST [type: 4, strucLength: 24, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 0, stringLength: 1, string: *]
        MQCFIL [type: 5, strucLength: 32, parameter: 1026 (MQIACF_Q_STATUS_ATTRS), count: 4, values: {2016, 1226, 1227, 1027}]
    */
    private PCFMessage createPCFRequestForInquireQStatusCmd() {
        PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_STATUS);
        request.addParameter(CMQC.MQCA_Q_NAME, "*");
        request.addParameter(CMQCFC.MQIACF_Q_STATUS_ATTRS, new int[]{2016, 1226, 1227, 1027});
        return request;
    }

    /*
        0 = {PCFMessage@6026} "PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 2, command: 41 (MQCMD_INQUIRE_Q_STATUS), msgSeqNumber: 1, control: 0, compCode: 0, reason: 0, parameterCount: 6]
        MQCFST [type: 4, strucLength: 68, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 819, stringLength: 48, string: AMQ.5AF1608820C7D76E                            ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1103 (MQIACF_Q_STATUS_TYPE), value: 1105]
        MQCFIN [type: 3, strucLength: 16, parameter: 3 (MQIA_CURRENT_Q_DEPTH), value: 12]
        MQCFIN [type: 3, strucLength: 16, parameter: 1227 (MQIACF_OLDEST_MSG_AGE), value: -1]
        MQCFIL [type: 5, strucLength: 24, parameter: 1226 (MQIACF_Q_TIME_INDICATOR), count: 2, values: {-1, -1}]
        MQCFIN [type: 3, strucLength: 16, parameter: 1027 (MQIACF_UNCOMMITTED_MSGS), value: 0]"

        1 = {PCFMessage@6029} "PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 2, command: 41 (MQCMD_INQUIRE_Q_STATUS), msgSeqNumber: 2, control: 0, compCode: 0, reason: 0, parameterCount: 6]
        MQCFST [type: 4, strucLength: 68, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 819, stringLength: 48, string: DEV.DEAD.LETTER.QUEUE                           ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1103 (MQIACF_Q_STATUS_TYPE), value: 1105]
        MQCFIN [type: 3, strucLength: 16, parameter: 3 (MQIA_CURRENT_Q_DEPTH), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 1227 (MQIACF_OLDEST_MSG_AGE), value: -1]
        MQCFIL [type: 5, strucLength: 24, parameter: 1226 (MQIACF_Q_TIME_INDICATOR), count: 2, values: {-1, -1}]
        MQCFIN [type: 3, strucLength: 16, parameter: 1027 (MQIACF_UNCOMMITTED_MSGS), value: 0]"

        2 = {PCFMessage@6030} "PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 2, command: 41 (MQCMD_INQUIRE_Q_STATUS), msgSeqNumber: 3, control: 0, compCode: 0, reason: 0, parameterCount: 6]
        MQCFST [type: 4, strucLength: 68, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 819, stringLength: 48, string: DEV.QUEUE.1                                     ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1103 (MQIACF_Q_STATUS_TYPE), value: 1105]
        MQCFIN [type: 3, strucLength: 16, parameter: 3 (MQIA_CURRENT_Q_DEPTH), value: 1]
        MQCFIN [type: 3, strucLength: 16, parameter: 1227 (MQIACF_OLDEST_MSG_AGE), value: -1]
        MQCFIL [type: 5, strucLength: 24, parameter: 1226 (MQIACF_Q_TIME_INDICATOR), count: 2, values: {-1, -1}]
        MQCFIN [type: 3, strucLength: 16, parameter: 1027 (MQIACF_UNCOMMITTED_MSGS), value: 0]"
    */
    private PCFMessage[] createPCFResponseForInquireQStatusCmd() {
        PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q_STATUS, 1, false);
        response1.addParameter(CMQC.MQCA_Q_NAME, "AMQ.5AF1608820C7D76E");
        response1.addParameter(CMQCFC.MQIACF_Q_STATUS_TYPE, 1105);
        response1.addParameter(CMQC.MQIA_CURRENT_Q_DEPTH, 12);
        response1.addParameter(CMQCFC.MQIACF_OLDEST_MSG_AGE, -1);
        response1.addParameter(CMQCFC.MQIACF_Q_TIME_INDICATOR, new int[]{-1, -1});
        response1.addParameter(CMQCFC.MQIACF_UNCOMMITTED_MSGS, 0);

        PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q_STATUS, 2, false);
        response2.addParameter(CMQC.MQCA_Q_NAME, "DEV.DEAD.LETTER.QUEUE");
        response2.addParameter(CMQCFC.MQIACF_Q_STATUS_TYPE, 1105);
        response2.addParameter(CMQC.MQIA_CURRENT_Q_DEPTH, 0);
        response2.addParameter(CMQCFC.MQIACF_OLDEST_MSG_AGE, -1);
        response2.addParameter(CMQCFC.MQIACF_Q_TIME_INDICATOR, new int[]{-1, -1});
        response2.addParameter(CMQCFC.MQIACF_UNCOMMITTED_MSGS, 0);

        PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q_STATUS, 1, false);
        response3.addParameter(CMQC.MQCA_Q_NAME, "DEV.QUEUE.1");
        response3.addParameter(CMQCFC.MQIACF_Q_STATUS_TYPE, 1105);
        response3.addParameter(CMQC.MQIA_CURRENT_Q_DEPTH, 1);
        response3.addParameter(CMQCFC.MQIACF_OLDEST_MSG_AGE, -1);
        response3.addParameter(CMQCFC.MQIACF_Q_TIME_INDICATOR, new int[]{-1, -1});
        response3.addParameter(CMQCFC.MQIACF_UNCOMMITTED_MSGS, 10);

        PCFMessage [] messages = {response1, response2, response3};
        return messages;
    }

    /*
        PCFMessage:
        MQCFH [type: 1, strucLength: 36, version: 1, command: 13 (MQCMD_INQUIRE_Q), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 3]
        MQCFST [type: 4, strucLength: 24, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 0, stringLength: 1, string: *]
        MQCFIN [type: 3, strucLength: 16, parameter: 20 (MQIA_Q_TYPE), value: 1001]
        MQCFIL [type: 5, strucLength: 36, parameter: 1002 (MQIACF_Q_ATTRS), count: 5, values: {2016, 15, 3, 17, 18}]
     */
    private PCFMessage createPCFRequestForInquireQCmd() {
        PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
        request.addParameter(CMQC.MQCA_Q_NAME, "*");
        request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_ALL);
        request.addParameter(CMQCFC.MQIACF_Q_ATTRS, new int[]{2016, 15, 3, 17, 18});
        return request;
    }

    /*
        0 = {PCFMessage@6059} "PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 1, command: 13 (MQCMD_INQUIRE_Q), msgSeqNumber: 1, control: 0, compCode: 0, reason: 0, parameterCount: 6]
        MQCFST [type: 4, strucLength: 68, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 819, stringLength: 48, string: AMQ.5AF1608820C76D80                            ]
        MQCFIN [type: 3, strucLength: 16, parameter: 20 (MQIA_Q_TYPE), value: 1]
        MQCFIN [type: 3, strucLength: 16, parameter: 3 (MQIA_CURRENT_Q_DEPTH), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 17 (MQIA_OPEN_INPUT_COUNT), value: 1]
        MQCFIN [type: 3, strucLength: 16, parameter: 15 (MQIA_MAX_Q_DEPTH), value: 5000]
        MQCFIN [type: 3, strucLength: 16, parameter: 18 (MQIA_OPEN_OUTPUT_COUNT), value: 1]"

        1 = {PCFMessage@6060} "PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 1, command: 13 (MQCMD_INQUIRE_Q), msgSeqNumber: 2, control: 0, compCode: 0, reason: 0, parameterCount: 6]
        MQCFST [type: 4, strucLength: 68, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 819, stringLength: 48, string: DEV.DEAD.LETTER.QUEUE                           ]
        MQCFIN [type: 3, strucLength: 16, parameter: 20 (MQIA_Q_TYPE), value: 1]
        MQCFIN [type: 3, strucLength: 16, parameter: 3 (MQIA_CURRENT_Q_DEPTH), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 17 (MQIA_OPEN_INPUT_COUNT), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 15 (MQIA_MAX_Q_DEPTH), value: 5000]
        MQCFIN [type: 3, strucLength: 16, parameter: 18 (MQIA_OPEN_OUTPUT_COUNT), value: 0]"

        2 = {PCFMessage@6061} "PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 1, command: 13 (MQCMD_INQUIRE_Q), msgSeqNumber: 3, control: 0, compCode: 0, reason: 0, parameterCount: 6]
        MQCFST [type: 4, strucLength: 68, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 819, stringLength: 48, string: DEV.QUEUE.1                                     ]
        MQCFIN [type: 3, strucLength: 16, parameter: 20 (MQIA_Q_TYPE), value: 1]
        MQCFIN [type: 3, strucLength: 16, parameter: 3 (MQIA_CURRENT_Q_DEPTH), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 17 (MQIA_OPEN_INPUT_COUNT), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 15 (MQIA_MAX_Q_DEPTH), value: 5000]
        MQCFIN [type: 3, strucLength: 16, parameter: 18 (MQIA_OPEN_OUTPUT_COUNT), value: 0]"
     */

    private PCFMessage[] createPCFResponseForInquireQCmd() {
        PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q, 1, false);
        response1.addParameter(CMQC.MQCA_Q_NAME, "AMQ.5AF1608820C76D80");
        response1.addParameter(CMQC.MQIA_Q_TYPE, 1);
        response1.addParameter(CMQC.MQIA_CURRENT_Q_DEPTH, 1);
        response1.addParameter(CMQC.MQIA_OPEN_INPUT_COUNT, 1);
        response1.addParameter(CMQC.MQIA_MAX_Q_DEPTH, 5000);
        response1.addParameter(CMQC.MQIA_OPEN_OUTPUT_COUNT, 1);

        PCFMessage response2 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q, 2, false);
        response2.addParameter(CMQC.MQCA_Q_NAME, "DEV.DEAD.LETTER.QUEUE");
        response2.addParameter(CMQC.MQIA_Q_TYPE, 1);
        response2.addParameter(CMQC.MQIA_CURRENT_Q_DEPTH, 2);
        response2.addParameter(CMQC.MQIA_OPEN_INPUT_COUNT, 2);
        response2.addParameter(CMQC.MQIA_MAX_Q_DEPTH, 5000);
        response2.addParameter(CMQC.MQIA_OPEN_OUTPUT_COUNT, 2);

        PCFMessage response3 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q, 3, false);
        response3.addParameter(CMQC.MQCA_Q_NAME, "DEV.QUEUE.1");
        response3.addParameter(CMQC.MQIA_Q_TYPE, 1);
        response3.addParameter(CMQC.MQIA_CURRENT_Q_DEPTH, 3);
        response3.addParameter(CMQC.MQIA_OPEN_INPUT_COUNT, 3);
        response3.addParameter(CMQC.MQIA_MAX_Q_DEPTH, 5000);
        response3.addParameter(CMQC.MQIA_OPEN_OUTPUT_COUNT, 3);

        PCFMessage [] messages = {response1, response2, response3};
        return messages;
    }

    /*
        PCFMessage:
        MQCFH [type: 1, strucLength: 36, version: 1, command: 17 (MQCMD_RESET_Q_STATS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 1]
        MQCFST [type: 4, strucLength: 24, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 0, stringLength: 1, string: *]
     */
    private PCFMessage createPCFRequestForResetQStatsCmd() {
        PCFMessage request = new PCFMessage(CMQCFC.MQCMD_RESET_Q_STATS);
        request.addParameter(CMQC.MQCA_Q_NAME, "*");
        return request;
    }

    /*
        0 = {PCFMessage@6144} "PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 1, command: 17 (MQCMD_RESET_Q_STATS), msgSeqNumber: 1, control: 0, compCode: 0, reason: 0, parameterCount: 5]
        MQCFST [type: 4, strucLength: 68, parameter: 2016 (MQCA_Q_NAME), codedCharSetId: 819, stringLength: 48, string: DEV.DEAD.LETTER.QUEUE                           ]
        MQCFIN [type: 3, strucLength: 16, parameter: 37 (MQIA_MSG_ENQ_COUNT), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 38 (MQIA_MSG_DEQ_COUNT), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 36 (MQIA_HIGH_Q_DEPTH), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 35 (MQIA_TIME_SINCE_RESET), value: 65]"
     */
    private PCFMessage[] createPCFResponseForResetQStatsCmd() {
        PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_RESET_Q_STATS, 1, false);
        response1.addParameter(CMQC.MQCA_Q_NAME, "DEV.DEAD.LETTER.QUEUE");
        response1.addParameter(CMQC.MQIA_MSG_ENQ_COUNT, 3);
        response1.addParameter(CMQC.MQIA_MSG_DEQ_COUNT, 0);
        response1.addParameter(CMQC.MQIA_HIGH_Q_DEPTH, 10);
        response1.addParameter(CMQC.MQIA_TIME_SINCE_RESET, 65);

        PCFMessage [] messages = {response1};
        return messages;
    }

}
