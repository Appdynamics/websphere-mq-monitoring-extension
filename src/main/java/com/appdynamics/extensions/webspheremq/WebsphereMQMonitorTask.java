package com.appdynamics.extensions.webspheremq;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.config.MqMetric;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.appdynamics.extensions.webspheremq.metricscollector.ChannelMetricsCollector;
import com.appdynamics.extensions.webspheremq.metricscollector.MetricsCollector;
import com.appdynamics.extensions.webspheremq.metricscollector.QueueManagerMetricsCollector;
import com.appdynamics.extensions.webspheremq.metricscollector.QueueMetricsCollector;
import com.google.common.collect.Maps;
import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

/**
 * This class is responsible 
 * 
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class WebsphereMQMonitorTask implements Runnable {

	public static final Logger logger = Logger.getLogger(WebsphereMQMonitorTask.class);
	QueueManager queueManager;
	private String metricPrefix;
	private AManagedMonitor monitor;
	private MqMetric[] mqMetrics;

	public WebsphereMQMonitorTask(QueueManager queueManager, String metricPrefix, MqMetric[] mqMetrics, AManagedMonitor monitor) {
		this.queueManager = queueManager;
		this.metricPrefix = metricPrefix;
		this.monitor = monitor;
		this.mqMetrics = mqMetrics;
	}

	public void run() {

		long startTime = System.currentTimeMillis();
		try {
			logger.debug("WebSphereMQ monitor thread for queueManager " + queueManager.getName() + " started.");
			extractAndReportMetrics();
		} catch (Exception e) {
			logger.error("Error in run of " + Thread.currentThread().getName(), e);
		} finally {
			long endTime = System.currentTimeMillis() - startTime;
			logger.debug("WebSphereMQ monitor thread for queueManager " + queueManager.getName() + " ended. Time taken = " + endTime);
		}
	}

	
	private void extractAndReportMetrics() throws MQException, TaskExecutionException {
		Map<String,Map<String,WMQMetricOverride>> metricsMap = getMetricsToReport();
		Hashtable env = getMQEnvironment();
		extractAndReportInternal(env,metricsMap);
	}
	
	/* 
	 * Returns master data structure,This map will contain only those metrics which are to be reported to controller.
	 * It contains metric type as key and a map of metric and WMQMetricOverride as value, entryset of internal map
	 * implicitly represents metrics to be reported.
	 */
	private Map<String,Map<String,WMQMetricOverride>>  getMetricsToReport(){
		Map<String,Map<String,WMQMetricOverride>> metricsMap = Maps.newHashMap();
		for(MqMetric mqMetric : mqMetrics){
			String metricType = mqMetric.getMetricsType();
			List includeMetrics = (List)mqMetric.getMetrics().get("include");
			Map<String, WMQMetricOverride> metricToReport = Maps.newHashMap();
			if(includeMetrics != null){
				metricToReport = gatherMetricNamesByApplyingIncludeFilter(includeMetrics);
			}
			metricsMap.put(metricType, metricToReport);
		}
		return metricsMap;
	}

	private Map<String, WMQMetricOverride> gatherMetricNamesByApplyingIncludeFilter(List includeMetrics) {
		Map<String,WMQMetricOverride> overrideMap = Maps.newHashMap();
		for(Object inc : includeMetrics){
			Map metric = (Map) inc;
			//Get the First Entry which is the metric
			Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
			String metricName = firstEntry.getKey().toString();
			WMQMetricOverride override = new WMQMetricOverride();
			override.setAlias(firstEntry.getValue().toString());
			override.setMultiplier(metric.get("multiplier") != null ? Double.parseDouble(metric.get("multiplier").toString()) : Constants.DEFAULT_MULTIPLIER);
			String metricType = metric.get("metricType") != null ? metric.get("metricType").toString() : Constants.DEFAULT_METRIC_TYPE;
			String[] metricTypes = metricType.split(" ");
			override.setAggregator(metricTypes[0]);
			override.setTimeRollup(metricTypes[1]);
			override.setClusterRollup(metricTypes[2]);
			override.setName((String)metric.get("name"));
			override.setIbmConstant((String)metric.get("ibmConstant"));
			override.setConstantValue((Integer)metric.get("constantValue"));
			overrideMap.put(metricName,override);
		}
		return overrideMap;
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Hashtable getMQEnvironment(){
		Hashtable env = new Hashtable();
		env.put(MQC.HOST_NAME_PROPERTY, queueManager.getHost());
		env.put(MQC.PORT_PROPERTY, queueManager.getPort());
		env.put(MQC.CHANNEL_PROPERTY, queueManager.getChannelName());
		//TODO: make this configurable
		if(queueManager.getTransportType()==1)
			env.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES_CLIENT);
		else if(queueManager.getTransportType()==0)
			env.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES_BINDINGS);
		else
			env.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES);
		return env;	
	}

	private void extractAndReportInternal(Hashtable env, Map<String,Map<String,WMQMetricOverride>> metricsMap) throws MQException, TaskExecutionException {
		if (queueManager == null) {
			String nullQManagerMsg = "Queue manager cannot be null";
			logger.debug(nullQManagerMsg);
			throw new IllegalArgumentException(nullQManagerMsg);
		}
		MQQueueManager ibmQueueManager = new MQQueueManager(queueManager.getName(),env);
		logger.debug("Intializing PCFMessageAgent...");
		PCFMessageAgent agent = new PCFMessageAgent(ibmQueueManager);
		logger.debug("Sending PCFMessageAgent.connect() request....");
		// Note that agent.connect() method is synchronized
		agent.connect(queueManager.getName());
		logger.debug(" PCFMessageAgent connected");
		
		Map<String,WMQMetricOverride> qMgrMetricsToReport= metricsMap.get(Constants.METRIC_TYPE_QUEUE_MANAGER);
		MetricsCollector qMgrMetricsCollector = new QueueManagerMetricsCollector(qMgrMetricsToReport, this.monitor, agent, queueManager, this.metricPrefix);
		qMgrMetricsCollector.processFilterAndPublishMetrics();
		
		Map<String,WMQMetricOverride> channelMetricsToReport= metricsMap.get(Constants.METRIC_TYPE_CHANNEL);
		MetricsCollector channelMetricsCollector = new ChannelMetricsCollector(channelMetricsToReport, this.monitor, agent, queueManager, this.metricPrefix);
		channelMetricsCollector.processFilterAndPublishMetrics();
		
		Map<String,WMQMetricOverride> queueMetricsToReport= metricsMap.get(Constants.METRIC_TYPE_QUEUE);
		MetricsCollector queueMetricsCollector = new QueueMetricsCollector(queueMetricsToReport, this.monitor, agent, queueManager, this.metricPrefix);
		queueMetricsCollector.processFilterAndPublishMetrics();		
	}

}
