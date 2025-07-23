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
import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.common.WMQUtil;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.collect.Lists;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.pcf.PCFParameter;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.Enumeration;
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

	public QueueManagerMetricsCollector(MonitorContextConfiguration monitorContextConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper, CountDownLatch countDownLatch) {
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

	public void publishMetrics() throws TaskExecutionException {
		long entryTime = System.currentTimeMillis();
		logger.debug("publishMetrics entry time for queuemanager {} is {} milliseconds", agent.getQManagerName(), entryTime);
		PCFMessage request;
		PCFMessage[] responses;
		// CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS is 161
		request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS);
		// CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS is 1229
		request.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS, new int[] { CMQCFC.MQIACF_ALL });
		try {
			// Note that agent.send() method is synchronized
			logger.debug("sending PCF agent request to query queuemanager {}", agent.getQManagerName());
			long startTime = System.currentTimeMillis();
			responses = agent.send(request);
			long endTime = System.currentTimeMillis() - startTime;
			logger.debug("PCF agent queuemanager metrics query response for {} received in {} milliseconds", agent.getQManagerName(), endTime);
			if (responses == null || responses.length <= 0) {
				logger.debug("Unexpected Error while PCFMessage.send(), response is either null or empty");
				return;
			}

			Enumeration<PCFParameter> pcfParameters = responses[0].getParameters();
			List<Metric> metrics = Lists.newArrayList();
			List<Map> mqMetrics = (List<Map>) this.monitorContextConfig.getConfigYml().get("mqMetrics");
			List<String> excludedMetrics = WMQUtil.getMetricsToExcludeFromConfigYml(mqMetrics, Constants.METRIC_TYPE_QUEUE_MANAGER);
			while (pcfParameters.hasMoreElements()) {
				PCFParameter pcfParam = pcfParameters.nextElement();
				String metrickey = pcfParam.getParameterName();
				if (!WMQUtil.isMetricExcluded(metrickey, excludedMetrics)) {
					try {
						if (pcfParam != null) {
							// create metric objects from PCF parameter
							metrics.addAll(createMetrics(queueManager, null, pcfParam));
						} else {
							logger.warn("PCF parameter is null in response for Queue Manager: {} for metric: {}", agent.getQManagerName(), metrickey);
						}
					} catch (Exception pcfe) {
						logger.error("Exception caught while collecting metric for Queue Manager: {} for metric: {}", agent.getQManagerName(), metrickey, pcfe);
					}
				}
				else {
					logger.debug("Queue Manager metric key {} is excluded.",metrickey);
				}
			}
			publishMetrics(metrics);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new TaskExecutionException(e);
		} finally {
			long exitTime = System.currentTimeMillis() - entryTime;
			logger.debug("Time taken to publish metrics for queuemanager is {} milliseconds", exitTime);
		}
	}
}
