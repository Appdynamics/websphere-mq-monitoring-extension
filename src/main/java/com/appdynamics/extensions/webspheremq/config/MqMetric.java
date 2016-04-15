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
