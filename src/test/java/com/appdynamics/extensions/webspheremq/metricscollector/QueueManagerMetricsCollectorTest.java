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
import com.google.common.collect.Lists;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.CMQC;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QueueManagerMetricsCollector.class})
@PowerMockIgnore("javax.management.*")
public class QueueManagerMetricsCollectorTest {

    private QueueManagerMetricsCollector classUnderTest;

    @Mock
    private AMonitorJob aMonitorJob;

    @Mock
    private PCFMessageAgent pcfMessageAgent;

    @Mock
    private MetricWriteHelper metricWriteHelper;

    private MonitorContextConfiguration monitorContextConfig;
    private Map<String, WMQMetricOverride> queueMgrMetricsToReport;
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
        queueMgrMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_QUEUE_MANAGER);
    }

    @Test
    public void testProcessPCFRequestAndPublishQMetricsForInquireQStatusCmd() throws MQException, IOException, TaskExecutionException {
        when(pcfMessageAgent.send(any(PCFMessage.class))).thenReturn(createPCFResponseForInquireQMgrStatusCmd());
        classUnderTest = new QueueManagerMetricsCollector(queueMgrMetricsToReport, monitorContextConfig, pcfMessageAgent, queueManager, metricWriteHelper);
        classUnderTest.publishMetrics();
        verify(metricWriteHelper, times(1)).transformAndPrintMetrics(pathCaptor.capture());
        List<String> metricPathsList = Lists.newArrayList();
        metricPathsList.add("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Status");

        for (List<Metric> metricList : pathCaptor.getAllValues()) {
            for (Metric metric : metricList) {
                if (metricPathsList.contains(metric.getMetricPath())) {
                    if (metric.getMetricPath().equals("Server|Component:Tier1|Custom Metrics|WebsphereMQ|QM1|Status")) {
                        Assert.assertTrue(metric.getMetricValue().equals("2"));
                        Assert.assertFalse(metric.getMetricValue().equals("10"));
                    }

                }
            }
        }
    }


    /*  Request
        PCFMessage:
        MQCFH [type: 1, strucLength: 36, version: 1, command: 161 (MQCMD_INQUIRE_Q_MGR_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 1]
        MQCFIL [type: 5, strucLength: 20, parameter: 1229 (MQIACF_Q_MGR_STATUS_ATTRS), count: 1, values: {1009}]

        Response
        PCFMessage:
        MQCFH [type: 2, strucLength: 36, version: 1, command: 161 (MQCMD_INQUIRE_Q_MGR_STATUS), msgSeqNumber: 1, control: 1, compCode: 0, reason: 0, parameterCount: 23]
        MQCFST [type: 4, strucLength: 68, parameter: 2015 (MQCA_Q_MGR_NAME), codedCharSetId: 819, stringLength: 48, string: QM1                                             ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1149 (MQIACF_Q_MGR_STATUS), value: 2]
        MQCFST [type: 4, strucLength: 20, parameter: 3208 (null), codedCharSetId: 819, stringLength: 0, string: ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1416 (null), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 1232 (MQIACF_CHINIT_STATUS), value: 2]
        MQCFIN [type: 3, strucLength: 16, parameter: 1233 (MQIACF_CMD_SERVER_STATUS), value: 2]
        MQCFIN [type: 3, strucLength: 16, parameter: 1230 (MQIACF_CONNECTION_COUNT), value: 23]
        MQCFST [type: 4, strucLength: 20, parameter: 3071 (MQCACF_CURRENT_LOG_EXTENT_NAME), codedCharSetId: 819, stringLength: 0, string: ]
        MQCFST [type: 4, strucLength: 20, parameter: 2115 (null), codedCharSetId: 819, stringLength: 0, string: ]
        MQCFST [type: 4, strucLength: 36, parameter: 2116 (null), codedCharSetId: 819, stringLength: 13, string: Installation1]
        MQCFST [type: 4, strucLength: 28, parameter: 2117 (null), codedCharSetId: 819, stringLength: 8, string: /opt/mqm]
        MQCFIN [type: 3, strucLength: 16, parameter: 1409 (null), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 1420 (null), value: 9]
        MQCFST [type: 4, strucLength: 44, parameter: 3074 (MQCACF_LOG_PATH), codedCharSetId: 819, stringLength: 24, string: /var/mqm/log/QM1/active/]
        MQCFIN [type: 3, strucLength: 16, parameter: 1421 (null), value: 9]
        MQCFST [type: 4, strucLength: 20, parameter: 3073 (MQCACF_MEDIA_LOG_EXTENT_NAME), codedCharSetId: 819, stringLength: 0, string: ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1417 (null), value: 0]
        MQCFST [type: 4, strucLength: 20, parameter: 3072 (MQCACF_RESTART_LOG_EXTENT_NAME), codedCharSetId: 819, stringLength: 0, string: ]
        MQCFIN [type: 3, strucLength: 16, parameter: 1418 (null), value: 1]
        MQCFIN [type: 3, strucLength: 16, parameter: 1419 (null), value: 0]
        MQCFIN [type: 3, strucLength: 16, parameter: 1325 (null), value: 0]
        MQCFST [type: 4, strucLength: 32, parameter: 3175 (null), codedCharSetId: 819, stringLength: 12, string: 2018-05-08  ]
        MQCFST [type: 4, strucLength: 28, parameter: 3176 (null), codedCharSetId: 819, stringLength: 8, string: 08.32.08]
     */

    private PCFMessage[] createPCFResponseForInquireQMgrStatusCmd() {
        PCFMessage response1 = new PCFMessage(2, CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS, 1, true);
        response1.addParameter(CMQC.MQCA_Q_MGR_NAME, "QM1");
        response1.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS, 2);
        response1.addParameter(CMQCFC.MQIACF_CHINIT_STATUS, 2);
        response1.addParameter(CMQCFC.MQIACF_CMD_SERVER_STATUS, 2);
        response1.addParameter(CMQCFC.MQIACF_CONNECTION_COUNT, 23);
        response1.addParameter(CMQCFC.MQCACF_CURRENT_LOG_EXTENT_NAME, "");
        response1.addParameter(CMQCFC.MQCACF_LOG_PATH, "/var/mqm/log/QM1/active/");
        response1.addParameter(CMQCFC.MQCACF_MEDIA_LOG_EXTENT_NAME, "");
        response1.addParameter(CMQCFC.MQCACF_RESTART_LOG_EXTENT_NAME, "");

        PCFMessage [] messages = {response1};
        return messages;
    }

}
