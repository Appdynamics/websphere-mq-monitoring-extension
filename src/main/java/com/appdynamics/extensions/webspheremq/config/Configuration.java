/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

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
	private MqMetric[] mqMetrics;
	private int numberOfThreads = Constants.DEFAULT_NUMBER_OF_THREADS;
	private int queueMetricsCollectionTimeoutInSeconds = 20;
	private String encryptionKey;
	
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

	public MqMetric[] getMqMetrics() {
		return mqMetrics;
	}

	public void setMqMetrics(MqMetric[] mqMetrics) {
		this.mqMetrics = mqMetrics;
	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public int getQueueMetricsCollectionTimeoutInSeconds() {
		return queueMetricsCollectionTimeoutInSeconds;
	}

	public void setQueueMetricsCollectionTimeoutInSeconds(int queueMetricsCollectionTimeoutInSeconds) {
		this.queueMetricsCollectionTimeoutInSeconds = queueMetricsCollectionTimeoutInSeconds;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}
}
