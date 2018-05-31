/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.config;

import java.util.Map;

public class MqMetric {
	
	String metricsType;
	Map<String,?> metrics;
	
	
	public String getMetricsType() {
		return metricsType;
	}
	public void setMetricsType(String metricsType) {
		this.metricsType = metricsType;
	}
	public Map<String, ?> getMetrics() {
		return metrics;
	}
	public void setMetrics(Map<String, ?> metrics) {
		this.metrics = metrics;
	}

}
