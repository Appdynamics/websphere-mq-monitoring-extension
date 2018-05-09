/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.StringUtils;
import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.common.WMQUtil;
import com.appdynamics.extensions.webspheremq.config.Configuration;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.appdynamics.extensions.webspheremq.metricscollector.*;
import com.google.common.base.Strings;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Map;

/**
 * Encapsulates all metrics collection for all artifacts related to a queue manager.
 * 
 * @author rajeevsingh,kunalgup
 * @version 2.0
 *
 */
public class WMQMonitorTask implements AMonitorTaskRunnable {

	public static final Logger logger = LoggerFactory.getLogger(WMQMonitorTask.class);
	private QueueManager queueManager;
	private MonitorConfiguration monitorConfig;
	Configuration configuration;
	private MetricWriteHelper metricWriteHelper;

	public WMQMonitorTask(TasksExecutionServiceProvider tasksExecutionServiceProvider, QueueManager queueManager, Configuration configuration) {
		this.monitorConfig = tasksExecutionServiceProvider.getMonitorConfiguration();
		this.queueManager = queueManager;
		this.configuration = configuration;
		metricWriteHelper = tasksExecutionServiceProvider.getMetricWriteHelper();
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		try {
			logger.debug("WebSphereMQ monitor thread for queueManager " + queueManager.getName() + " started.");
			extractAndReportMetrics();
			metricWriteHelper.printMetric(StringUtils.concatMetricPath(monitorConfig.getMetricPrefix(), queueManager.getName(), "HeartBeat"), BigDecimal.ONE, "AVG.AVG.IND");
		} catch (Exception e) {
			metricWriteHelper.printMetric(StringUtils.concatMetricPath(monitorConfig.getMetricPrefix(), queueManager.getName(), "HeartBeat"), BigDecimal.ZERO, "AVG.AVG.IND");
			logger.error("Error in run of " + Thread.currentThread().getName(), e);
		} finally {
			long endTime = System.currentTimeMillis() - startTime;
			logger.debug("WebSphereMQ monitor thread for queueManager " + queueManager.getName() + " ended. Time taken = " + endTime);
		}
	}

	private void extractAndReportMetrics() throws MQException, TaskExecutionException {
		Map<String, Map<String, WMQMetricOverride>> metricsMap = WMQUtil.getMetricsToReportFromConfigYml(configuration.getMqMetrics());
		WMQContext auth = new WMQContext(queueManager);
		Hashtable env = auth.getMQEnvironment();
		extractAndReportInternal(env, metricsMap);
	}



	private void extractAndReportInternal(Hashtable env, Map<String, Map<String, WMQMetricOverride>> metricsMap) throws TaskExecutionException {

		MQQueueManager ibmQueueManager = null;
		PCFMessageAgent agent = null;
		try {
			if (env != null) {
				ibmQueueManager = new MQQueueManager(queueManager.getName(), env);
			} else {
				ibmQueueManager = new MQQueueManager(queueManager.getName());
			}
			logger.debug("Connection initiated for queue manager {} in thread {}", queueManager.getName(), Thread.currentThread().getName());

			if(!Strings.isNullOrEmpty(queueManager.getModelQueueName()) && !Strings.isNullOrEmpty(queueManager.getReplyQueuePrefix())){
				logger.debug("Initializing the PCF agent for model queue and reply queue prefix.");
				agent = new PCFMessageAgent();
				agent.setModelQueueName(queueManager.getModelQueueName());
				agent.setReplyQueuePrefix(queueManager.getReplyQueuePrefix());
				logger.debug("Connecting to queue manager to set the modelQueueName and replyQueuePrefix.");
				agent.connect(ibmQueueManager);
			}
			else{
				agent = new PCFMessageAgent(ibmQueueManager);
			}
			if(queueManager.getCcsid() != Integer.MIN_VALUE){
				agent.setCharacterSet(queueManager.getCcsid());
			}

			if(queueManager.getEncoding() != Integer.MIN_VALUE){
				agent.setEncoding(queueManager.getEncoding());
			}

			logger.debug("Intialized PCFMessageAgent for queue manager {} in thread {}", agent.getQManagerName(), Thread.currentThread().getName());

			Map<String, WMQMetricOverride> qMgrMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_QUEUE_MANAGER);
			if (qMgrMetricsToReport != null) {
				MetricsCollector qMgrMetricsCollector = new QueueManagerMetricsCollector(qMgrMetricsToReport, this.monitorConfig, agent, queueManager, metricWriteHelper);
				qMgrMetricsCollector.process();
			} else {
				logger.warn("No queue manager metrics to report");
			}

			Map<String, WMQMetricOverride> channelMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_CHANNEL);
			if (channelMetricsToReport != null) {
				MetricsCollector channelMetricsCollector = new ChannelMetricsCollector(channelMetricsToReport, this.monitorConfig, agent, queueManager, metricWriteHelper);
				channelMetricsCollector.process();
			} else {
				logger.warn("No channel metrics to report");
			}

			Map<String, WMQMetricOverride> queueMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_QUEUE);
			if (queueMetricsToReport != null) {
				MetricsCollector queueMetricsCollector = new QueueMetricsCollector(queueMetricsToReport, this.monitorConfig, agent, queueManager, metricWriteHelper);
				queueMetricsCollector.process();
			} else {
				logger.warn("No queue metrics to report");
			}

			Map<String, WMQMetricOverride> listenerMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_LISTENER);
			if (queueMetricsToReport != null) {
				MetricsCollector listenerMetricsCollector = new ListenerMetricsCollector(listenerMetricsToReport, this.monitorConfig, agent, queueManager, metricWriteHelper);
				listenerMetricsCollector.process();
			} else {
				logger.warn("No listener metrics to report");
			}

			Map<String, WMQMetricOverride> topicMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_TOPIC);
			if (topicMetricsToReport != null) {
				MetricsCollector topicsMetricsCollector = new TopicMetricsCollector(topicMetricsToReport, this.monitorConfig, agent, queueManager, metricWriteHelper);
				topicsMetricsCollector.process();
			} else {
				logger.warn("No topic metrics to report");
			}

		} catch (MQException mqe) {
			logger.error(mqe.getMessage(),mqe);
			throw new TaskExecutionException(mqe.getMessage());
		} catch(Exception e){
			logger.error("Some unknown exception occured",e);
			throw new TaskExecutionException(e);
		}
		finally {
			cleanUp(ibmQueueManager, agent);
		}
	}

	/**
	 * Destroy the agent and disconnect from queue manager
	 * 
	 */
	private void cleanUp(MQQueueManager ibmQueueManager, PCFMessageAgent agent) {
		// Disconnect the agent.

		if (agent != null) {
			try {
				String qMgrName = agent.getQManagerName();
				agent.disconnect();
				logger.debug("PCFMessageAgent disconnected for queue manager {} in thread {}", qMgrName, Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Error occoured  while disconnting PCFMessageAgent for queue manager {} in thread {}", queueManager.getName(), Thread.currentThread().getName(), e);
			}
		}

		// Disconnect queue manager
		
		if (ibmQueueManager != null) {
			try {
				ibmQueueManager.disconnect();
				//logger.debug("Connection diconnected for queue manager {} in thread {}", ibmQueueManager.getName(), Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Error occoured while disconnting queue manager {} in thread {}", queueManager.getName(), Thread.currentThread().getName(), e);
			}
		}
	}

	public void onTaskComplete() {
		logger.info("WebSphereMQ monitor thread completed for queueManager:" + queueManager.getName());
	}
}
