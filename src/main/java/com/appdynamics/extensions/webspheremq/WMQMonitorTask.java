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
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.StringUtils;
import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.common.WMQUtil;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.appdynamics.extensions.webspheremq.metricscollector.*;
import com.google.common.base.Strings;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Encapsulates all metrics collection for all artifacts related to a queue manager.
 * 
 * @author rajeevsingh,kunalgup
 * @version 2.0
 *
 */
public class WMQMonitorTask implements AMonitorTaskRunnable {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(WMQMonitorTask.class);
	private QueueManager queueManager;
	private MonitorContextConfiguration monitorContextConfig;
	private Map<String, ?> configMap;
	private MetricWriteHelper metricWriteHelper;
	private BigDecimal heartBeatMetricValue = BigDecimal.ZERO;

	public WMQMonitorTask(TasksExecutionServiceProvider tasksExecutionServiceProvider, MonitorContextConfiguration monitorContextConfig, QueueManager queueManager) {
		this.monitorContextConfig = monitorContextConfig;
		this.queueManager = queueManager;
		this.configMap = monitorContextConfig.getConfigYml();
		this.metricWriteHelper = tasksExecutionServiceProvider.getMetricWriteHelper();
	}

	public void run() {
		String queueManagerTobeDisplayed = WMQUtil.getQueueManagerNameFromConfig(queueManager);
		logger.debug("WMQMonitor thread for queueManager " + queueManagerTobeDisplayed + " started.");
		long startTime = System.currentTimeMillis();
		MQQueueManager ibmQueueManager = null;
		PCFMessageAgent agent = null;
		try {
			ibmQueueManager = initMQQueueManager();
			if (ibmQueueManager != null) {
				logger.debug("MQQueueManager connection initiated for queueManager {} in thread {}", queueManagerTobeDisplayed, Thread.currentThread().getName());
				heartBeatMetricValue = BigDecimal.ONE;
				agent = initPCFMesageAgent(ibmQueueManager);
				extractAndReportMetrics(agent);
			} else {
				logger.error("MQQueueManager connection could not be initiated for queueManager {} in thread {} ", queueManagerTobeDisplayed, Thread.currentThread().getName());
			}
		} catch (Exception e) {
			logger.error("Error in run of " + Thread.currentThread().getName(), e);
		} finally {
			cleanUp(ibmQueueManager, agent);
			metricWriteHelper.printMetric(StringUtils.concatMetricPath(monitorContextConfig.getMetricPrefix(), queueManagerTobeDisplayed, "HeartBeat"), heartBeatMetricValue, "AVG.AVG.IND");
			long endTime = System.currentTimeMillis() - startTime;
			logger.debug("WMQMonitor thread for queueManager " + queueManagerTobeDisplayed + " ended. Time taken = " + endTime + " ms");
		}
	}

	private MQQueueManager initMQQueueManager() throws TaskExecutionException {
		MQQueueManager ibmQueueManager = null;
		// encryptionKey is global but encryptedPassword is queueManager specific
		queueManager.setEncryptionKey((String) configMap.get("encryptionKey"));
		WMQContext auth = new WMQContext(queueManager);
		Hashtable env = auth.getMQEnvironment();
		try {
			if (env != null) {
				ibmQueueManager = new MQQueueManager(queueManager.getName(), env);
			} else {
				ibmQueueManager = new MQQueueManager(queueManager.getName());
			}
		} catch (MQException mqe) {
			logger.error(mqe.getMessage(), mqe);
			throw new TaskExecutionException(mqe.getMessage());
		}
		return ibmQueueManager;
	}

	private PCFMessageAgent initPCFMesageAgent(MQQueueManager ibmQueueManager) {
		PCFMessageAgent agent = null;
		try {
			if(!Strings.isNullOrEmpty(queueManager.getModelQueueName()) && !Strings.isNullOrEmpty(queueManager.getReplyQueuePrefix())){
				logger.debug("Initializing the PCF agent for model queue and reply queue prefix.");
				agent = new PCFMessageAgent();
				agent.setModelQueueName(queueManager.getModelQueueName());
				agent.setReplyQueuePrefix(queueManager.getReplyQueuePrefix());
				logger.debug("Connecting to queueManager to set the modelQueueName and replyQueuePrefix.");
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
			logger.debug("Intialized PCFMessageAgent for queueManager {} in thread {}", agent.getQManagerName(), Thread.currentThread().getName());
		} catch (MQException mqe) {
			logger.error(mqe.getMessage(), mqe);
		}
		return agent;
	}

	private void extractAndReportMetrics(PCFMessageAgent agent) {
		Map<String, Map<String, WMQMetricOverride>> metricsMap = WMQUtil.getMetricsToReportFromConfigYml((List<Map>) configMap.get("mqMetrics"));

		CountDownLatch countDownLatch = new CountDownLatch(metricsMap.size());

		Map<String, WMQMetricOverride> qMgrMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_QUEUE_MANAGER);
		if (qMgrMetricsToReport != null) {
			MetricsCollector qMgrMetricsCollector = new QueueManagerMetricsCollector(qMgrMetricsToReport, this.monitorContextConfig, agent, queueManager, metricWriteHelper, countDownLatch);
			monitorContextConfig.getContext().getExecutorService().execute("QueueManagerMetricsCollector", qMgrMetricsCollector);
		} else {
			logger.warn("No queue manager metrics to report");
		}

		Map<String, WMQMetricOverride> channelMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_CHANNEL);
		if (channelMetricsToReport != null) {
			MetricsCollector channelMetricsCollector = new ChannelMetricsCollector(channelMetricsToReport, this.monitorContextConfig, agent, queueManager, metricWriteHelper, countDownLatch);
			monitorContextConfig.getContext().getExecutorService().execute("ChannelMetricsCollector", channelMetricsCollector);
		} else {
			logger.warn("No channel metrics to report");
		}

		Map<String, WMQMetricOverride> queueMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_QUEUE);
		if (queueMetricsToReport != null) {
			MetricsCollector queueMetricsCollector = new QueueMetricsCollector(queueMetricsToReport, this.monitorContextConfig, agent, queueManager, metricWriteHelper, countDownLatch);
			monitorContextConfig.getContext().getExecutorService().execute("QueueMetricsCollector", queueMetricsCollector);
		} else {
			logger.warn("No queue metrics to report");
		}

		Map<String, WMQMetricOverride> listenerMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_LISTENER);
		if (listenerMetricsToReport != null) {
			MetricsCollector listenerMetricsCollector = new ListenerMetricsCollector(listenerMetricsToReport, this.monitorContextConfig, agent, queueManager, metricWriteHelper, countDownLatch);
			monitorContextConfig.getContext().getExecutorService().execute("ListenerMetricsCollector", listenerMetricsCollector);
		} else {
			logger.warn("No listener metrics to report");
		}

		Map<String, WMQMetricOverride> topicMetricsToReport = metricsMap.get(Constants.METRIC_TYPE_TOPIC);
		if (topicMetricsToReport != null) {
			MetricsCollector topicsMetricsCollector = new TopicMetricsCollector(topicMetricsToReport, this.monitorContextConfig, agent, queueManager, metricWriteHelper, countDownLatch);
			monitorContextConfig.getContext().getExecutorService().execute("TopicMetricsCollector", topicsMetricsCollector);
		} else {
			logger.warn("No topic metrics to report");
		}

		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			logger.error("Error while the thread {} is waiting ", Thread.currentThread().getName(), e);
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
				logger.debug("PCFMessageAgent disconnected for queueManager {} in thread {}", qMgrName, Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Error occoured  while disconnting PCFMessageAgent for queueManager {} in thread {}", queueManager.getName(), Thread.currentThread().getName(), e);
			}
		}

		// Disconnect queue manager
		
		if (ibmQueueManager != null) {
			try {
				ibmQueueManager.disconnect();
				//logger.debug("Connection diconnected for queue manager {} in thread {}", ibmQueueManager.getName(), Thread.currentThread().getName());
			} catch (Exception e) {
				logger.error("Error occoured while disconnting queueManager {} in thread {}", queueManager.getName(), Thread.currentThread().getName(), e);
			}
		}
	}

	public void onTaskComplete() {
		logger.info("WebSphereMQ monitor thread completed for queueManager:" + WMQUtil.getQueueManagerNameFromConfig(queueManager));
	}

}
