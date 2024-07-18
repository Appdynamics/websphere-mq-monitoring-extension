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
import com.appdynamics.extensions.webspheremq.config.ExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.pcf.*;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;

public class QueueMetricsCollector extends MetricsCollector implements Runnable {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(QueueMetricsCollector.class);
	private final String artifact = "Queues";

	public QueueMetricsCollector(MonitorContextConfiguration monitorContextConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper, CountDownLatch countDownLatch) {
		this.monitorContextConfig = monitorContextConfig;
		this.agent = agent;
		this.metricWriteHelper = metricWriteHelper;
		this.queueManager = queueManager;
		this.countDownLatch = countDownLatch;
	}

	public void run() {
		try {
			this.process();
		} catch (TaskExecutionException e) {
			logger.error("Error in QueueMetricsCollector ", e);
		} finally {
			countDownLatch.countDown();
		}
	}

	@Override
	protected void publishMetrics() throws TaskExecutionException {
		logger.info("Collecting queue metrics...");
		List<Future> futures = Lists.newArrayList();
		futures.add(monitorContextConfig.getContext().getExecutorService().submit("InquireQCmdCollector", new InquireQCmdCollector(this)));
		futures.add(monitorContextConfig.getContext().getExecutorService().submit("InquireQStatusCmdCollector", new InquireQStatusCmdCollector(this)));
		futures.add(monitorContextConfig.getContext().getExecutorService().submit("ResetQStatsCmdCollector", new ResetQStatsCmdCollector(this)));
		for(Future f: futures){
			try {
				long timeout = 20;
				if(monitorContextConfig.getConfigYml().get("queueMetricsCollectionTimeoutInSeconds") != null){
					timeout = (Integer)monitorContextConfig.getConfigYml().get("queueMetricsCollectionTimeoutInSeconds");
				}
				f.get(timeout, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.error("The thread was interrupted ",e);
			} catch (ExecutionException e) {
				logger.error("Something unforeseen has happened ",e);
			} catch (TimeoutException e) {
				logger.error("Thread timed out ",e);
			}
		}
	}

	@Override
	public String getAtrifact() {
		return artifact;
	}

	protected void processPCFRequestAndPublishQMetrics(String queueGenericName, PCFMessage request, String command) throws MQException, IOException {
		PCFMessage[] response;
		logger.debug("sending PCF agent request to query metrics for generic queue {} for command {}",queueGenericName,command);
		long startTime = System.currentTimeMillis();
		response = agent.send(request);
		long endTime = System.currentTimeMillis() - startTime;
		logger.debug("PCF agent queue metrics query response for generic queue {} for command {} received in {} milliseconds", queueGenericName, command,endTime);
		if (response == null || response.length <= 0) {
			logger.debug("Unexpected Error while PCFMessage.send() for command {}, response is either null or empty",command);
			return;
		}
		for (int i = 0; i < response.length; i++) {
			String queueName = response[i].getStringParameterValue(CMQC.MQCA_Q_NAME).trim();
			Set<ExcludeFilters> excludeFilters = this.queueManager.getQueueFilters().getExclude();
			if(!isExcluded(queueName,excludeFilters)) { //check for exclude filters
				logger.debug("Pulling out metrics for queue name {} for command {}",queueName,command);
				Enumeration<PCFParameter> pcfParameters = response[i].getParameters();
				List<Metric> metrics = Lists.newArrayList();
				List<Map> mqMetrics = (List<Map>) this.monitorContextConfig.getConfigYml().get("mqMetrics");
				List<String> excludedMetrics = WMQUtil.getMetricsToExcludeFromConfigYml(mqMetrics, Constants.METRIC_TYPE_QUEUE);
				List<String> allFoundMetrics = new ArrayList<>();
				while (pcfParameters.hasMoreElements()) {
					PCFParameter pcfParam = pcfParameters.nextElement();
					String metrickey = pcfParam.getParameterName();
					allFoundMetrics.add(metrickey);
					if (!WMQUtil.isMetricExcluded(metrickey, excludedMetrics)) {
						try {
							if (pcfParam != null) {
								// create metric objects from PCF parameter
								metrics.addAll(createMetrics(queueManager, queueName, pcfParam));

								// Below code adds current queue managers 'dead letter queue' depth. Metric key:'DLQ Depth'
								// Added the logic here to avoid issuing PCF Queue Status command in 'Queue manger collector' class
								if (queueName.equalsIgnoreCase("DEV.DEAD.LETTER.QUEUE") && metrickey.equalsIgnoreCase("MQIA_CURRENT_Q_DEPTH")) {
									Metric metric = getMetricByKey(metrickey, metrics);
									logger.info("DLQ Depth for queueManager {} is {}", WMQUtil.getQueueManagerNameFromConfig(queueManager), metric.getMetricValue());
									Metric dlqDepthmetric = createMetric(queueManager, "DLQ Depth", metric.getMetricValue(), null, "DLQ Depth");
									metrics.add(dlqDepthmetric);
								}
							} else {
								logger.warn("PCF parameter is null in response for Queue: {} for metric: {} in command {}", queueName, metrickey, command);
							}
						} catch (Exception pcfe) {
							logger.error("Exception caught while collecting metric for Queue: {} for metric: {} in command {}", queueName, metrickey, command, pcfe);
						}
					}
					else {
						logger.debug("Queue metric key {} is excluded.",metrickey);
					}
				}
				Collections.sort(allFoundMetrics);
				logger.debug("start of metrics printing...");
				logger.debug(allFoundMetrics.toString());
				logger.debug("end of metrics printing...");
				publishMetrics(metrics);
			}
			else{
				logger.debug("Queue name {} is excluded.",queueName);
			}
		}
	}
}
