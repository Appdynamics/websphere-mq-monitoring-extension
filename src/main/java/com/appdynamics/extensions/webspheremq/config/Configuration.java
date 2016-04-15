package com.appdynamics.extensions.webspheremq.config;

import com.appdynamics.extensions.webspheremq.common.Constants;

/**
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class Configuration {

	private QueueManager[] queueManagers;
	private String metricPrefix;
	private MqMetric[] mqMertics;
	private int numberOfThreads = Constants.DEFAULT_NUMBER_OF_THREADS;
	
	public QueueManager[] getQueueManagers() {
		return queueManagers;
	}
	public void setQueueManagers(QueueManager[] queueManagers) {
		this.queueManagers = queueManagers;
	}
	public String getMetricPrefix() {
		return metricPrefix;
	}
	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}
	public MqMetric[] getMqMertics() {
		return mqMertics;
	}
	public void setMqMertics(MqMetric[] mqMertics) {
		this.mqMertics = mqMertics;
	}
	public int getNumberOfThreads() {
		return numberOfThreads;
	}
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}
	
	
}
