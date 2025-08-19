/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.collect.Lists;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This class is responsible for queue metric collection.
 * 
 * @author rajeevsingh ,James Schneider
 * @version 2.0
 *
 */
public class QueueManagerMetricsCollector extends MetricsCollector implements Runnable {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(QueueManagerMetricsCollector.class);
	private final String artifact = "Queue Manager";

	public QueueManagerMetricsCollector(Map<String, WMQMetricOverride> metricsToReport, MonitorContextConfiguration monitorContextConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper, CountDownLatch countDownLatch) {
		this.metricsToReport = metricsToReport;
		this.monitorContextConfig = monitorContextConfig;
		this.agent = agent;
		this.metricWriteHelper = metricWriteHelper;
		this.queueManager = queueManager;
		this.countDownLatch = countDownLatch;
	}

	public String getAtrifact() {
		return artifact;
	}

	public void run() {
		try {
			this.process();
		} catch (TaskExecutionException e) {
			logger.error("Error in QueueManagerMetricsCollector ", e);
		} finally {
			countDownLatch.countDown();
		}
	}

    public void collectAndPublish() throws TaskExecutionException {
        long entryTime = System.currentTimeMillis();
        logger.debug("publishMetrics entry time for queuemanager {} is {} milliseconds", agent.getQManagerName(), entryTime);

        // Split metrics by command type similar to ChannelMetricsCollector
        Map<String, WMQMetricOverride> statusMetrics = new java.util.HashMap<>();
        Map<String, WMQMetricOverride> definitionMetrics = new java.util.HashMap<>();

        for (Map.Entry<String, WMQMetricOverride> entry : getMetricsToReport().entrySet()) {
            WMQMetricOverride override = entry.getValue();
            if (override.getIbmCommand() != null && override.getIbmCommand().equals("MQCMD_INQUIRE_Q_MGR")) {
                definitionMetrics.put(entry.getKey(), override);
            } else {
                // Default to status metrics for backward compatibility
                statusMetrics.put(entry.getKey(), override);
            }
        }

        List<Metric> allMetrics = Lists.newArrayList();

        // First, fetch definition attributes (e.g., Platform, MQVersion)
        if (!definitionMetrics.isEmpty()) {
            PCFMessage defReq = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR);
            defReq.addParameter(CMQCFC.MQIACF_Q_MGR_ATTRS, new int[] { CMQCFC.MQIACF_ALL });
            try {
                logger.debug("sending PCF agent request MQCMD_INQUIRE_Q_MGR for {}", agent.getQManagerName());
                long startTime = System.currentTimeMillis();
                PCFMessage[] defResp = agent.send(defReq);
                long endTime = System.currentTimeMillis() - startTime;
                logger.debug("PCF response (INQUIRE_Q_MGR) for {} received in {} ms", agent.getQManagerName(), endTime);
                if (defResp != null && defResp.length > 0) {
                    for (Map.Entry<String, WMQMetricOverride> e : definitionMetrics.entrySet()) {
                        String metricKey = e.getKey();
                        WMQMetricOverride override = e.getValue();
                        try {
                            int metricVal = defResp[0].getIntParameterValue(override.getConstantValue());
                            if ("platform".equalsIgnoreCase(metricKey)) {
                                String platformName = resolvePlatformName(metricVal);
                                allMetrics.add(createInfoMetricFromString(queueManager, metricKey, platformName, override, metricKey));
                            } else {
                                allMetrics.add(createMetric(queueManager, metricKey, metricVal, override, metricKey));
                            }
                        } catch (Exception notInt) {
                            try {
                                String str = defResp[0].getStringParameterValue(override.getConstantValue());
                                Integer parsed = null;
                                String lower = metricKey.toLowerCase();
                                if (lower.contains("date")) {
                                    parsed = parseDateStringToInt(str);
                                } else if (lower.contains("time")) {
                                    parsed = parseTimeStringToInt(str);
                                }
                                if (parsed != null) {
                                    allMetrics.add(createMetric(queueManager, metricKey, parsed, override, metricKey));
                                } else {
                                    allMetrics.add(createInfoMetricFromString(queueManager, metricKey, str, override, metricKey));
                                }
                            } catch (Exception ignore) {
                                // As a fallback, if a static value is provided in config, publish it as info metric
                                if (override.getStaticValue() != null) {
                                    allMetrics.add(createInfoMetricFromStaticValue(queueManager, metricKey, override, metricKey));
                                } else {
                                    logger.debug("Metric {} not available as int or string for queue manager (INQUIRE_Q_MGR)", metricKey);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error while fetching queue manager definition metrics", e);
                throw new TaskExecutionException(e);
            }
        }

        // Then, fetch status attributes
        if (!statusMetrics.isEmpty()) {
            PCFMessage statusReq = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS);
            statusReq.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS, new int[] { CMQCFC.MQIACF_ALL });
            try {
                logger.debug("sending PCF agent request MQCMD_INQUIRE_Q_MGR_STATUS for {}", agent.getQManagerName());
                long startTime = System.currentTimeMillis();
                PCFMessage[] statusResp = agent.send(statusReq);
                long endTime = System.currentTimeMillis() - startTime;
                logger.debug("PCF response (INQUIRE_Q_MGR_STATUS) for {} received in {} ms", agent.getQManagerName(), endTime);
                if (statusResp != null && statusResp.length > 0) {
                    for (Map.Entry<String, WMQMetricOverride> e : statusMetrics.entrySet()) {
                        String metricKey = e.getKey();
                        WMQMetricOverride override = e.getValue();
                        try {
                            int metricVal = statusResp[0].getIntParameterValue(override.getConstantValue());
                            if ("platform".equalsIgnoreCase(metricKey)) {
                                String platformName = resolvePlatformName(metricVal);
                                allMetrics.add(createInfoMetricFromString(queueManager, metricKey, platformName, override, metricKey));
                            } else {
                                allMetrics.add(createMetric(queueManager, metricKey, metricVal, override, metricKey));
                            }
                        } catch (Exception notInt) {
                            try {
                                String str = statusResp[0].getStringParameterValue(override.getConstantValue());
                                Integer parsed = null;
                                String lower = metricKey.toLowerCase();
                                if (lower.contains("date")) {
                                    parsed = parseDateStringToInt(str);
                                } else if (lower.contains("time")) {
                                    parsed = parseTimeStringToInt(str);
                                }
                                if (parsed != null) {
                                    allMetrics.add(createMetric(queueManager, metricKey, parsed, override, metricKey));
                                } else {
                                    allMetrics.add(createInfoMetricFromString(queueManager, metricKey, str, override, metricKey));
                                }
                            } catch (Exception ignore) {
                                if (override.getStaticValue() != null) {
                                    allMetrics.add(createInfoMetricFromStaticValue(queueManager, metricKey, override, metricKey));
                                } else {
                                    logger.debug("Metric {} not available as int or string for queue manager (INQUIRE_Q_MGR_STATUS)", metricKey);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error while fetching queue manager status metrics", e);
                throw new TaskExecutionException(e);
            }
        }

        publishMetrics(allMetrics);

        long exitTime = System.currentTimeMillis() - entryTime;
        logger.debug("Time taken to publish metrics for queuemanager is {} milliseconds", exitTime);
    }

	public Map<String, WMQMetricOverride> getMetricsToReport() {
		return metricsToReport;
	}

    private String resolvePlatformName(int platformCode) {
        String[] candidates = new String[] {
                "MQPL_ZOS",
                "MQPL_OS390",
                "MQPL_UNIX",
                "MQPL_WINDOWS_NT",
                "MQPL_NSK",
                "MQPL_NSS",
                "MQPL_VMS",
                "MQPL_OS2",
                "MQPL_AIX",
                "MQPL_TPF",
                "MQPL_OPEN_TP1",
                "MQPL_MVS"
        };
        for (String fieldName : candidates) {
            try {
                java.lang.reflect.Field f = CMQC.class.getField(fieldName);
                int val = f.getInt(null);
                if (val == platformCode) {
                    String friendly = fieldName.replace("MQPL_", "").replace('_', ' ');
                    if ("ZOS".equals(friendly)) return "z/OS";
                    if ("OS390".equals(friendly)) return "OS/390";
                    if ("WINDOWS NT".equals(friendly)) return "Windows";
                    return friendly;
                }
            } catch (Exception ignore) { }
        }
        return String.valueOf(platformCode);
    }
}
