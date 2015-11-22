package com.appdynamics.extensions.webspheremq.metricscollector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public abstract class MetricsCollector {
	
	protected Map<String, ? extends MetricOverride> metricsToReport;
	protected AManagedMonitor monitor;
	protected PCFMessageAgent agent;
	protected String metricPrefix;
	protected QueueManager queueManager;

	public static final Logger logger = Logger.getLogger(MetricsCollector.class);
	
	protected abstract void processFilter() throws TaskExecutionException;
	protected abstract void publishMetrics() throws TaskExecutionException;
	
	public abstract String getAtrifact();
	public abstract Map<String,? extends MetricOverride> getMetricsToReport();
	
	public final void  processFilterAndPublishMetrics() throws TaskExecutionException{
		processFilter();
		publishMetrics();
	}
	
	public void printMetric(String name, String value, String aggType, String timeRollup, String clusterRollup, AManagedMonitor monitor) {
		String metricName = getMetricPrefix() + name;
		MetricWriter metricWriter = monitor.getMetricWriter(metricName, aggType, timeRollup, clusterRollup);
		metricWriter.printMetric(value);
		logger.info("METRIC:  NAME:" + metricName + " VALUE:" + value + " :" + aggType + ":" + timeRollup + ":" + clusterRollup);
	}

	private String getMetricPrefix() {
		return "";
	}

	public BigInteger toBigInteger(Object value, Double multiplier) {
		try {
			BigDecimal bigD = new BigDecimal(value.toString());
			if (multiplier != null && multiplier != Constants.DEFAULT_MULTIPLIER) {
				bigD = bigD.multiply(new BigDecimal(multiplier));
			}
			return bigD.setScale(0, RoundingMode.HALF_UP).toBigInteger();
		} catch (NumberFormatException nfe) {
		}
		return BigInteger.ZERO;
	}

	public Double getMultiplier(MetricOverride override) {
		if (override.getMultiplier() == 0.0) {
			return Constants.DEFAULT_MULTIPLIER;
		}
		return override.getMultiplier();
	}

	public static enum FilterType {
		STARTSWITH, EQUALS, ENDSWITH, CONTAINS, NONE;
	}

	public List<String> evalExcludeFilter(String type, List<String> artifactList, Set<String> filterValueList) {
		List<String> filteredList = new ArrayList<String>();

		for (String artifact : artifactList) {
			boolean exclude = false;
			switch (FilterType.valueOf(type)) {
			case CONTAINS:
				for (String filterValue : filterValueList) {
					if (artifact.contains(filterValue)) {
						exclude = true;
						break;
					}
				}
				break;
			case STARTSWITH:
				for (String filterValue : filterValueList) {
					if (artifact.startsWith(filterValue)) {
						exclude = true;
						break;
					}
				}
				break;
			case NONE:
				return artifactList;
			case EQUALS:
				for (String filterValue : filterValueList) {
					if (artifact.equals(filterValue)) {
						exclude = true;
						break;
					}
				}
				break;
			case ENDSWITH:
				for (String filterValue : filterValueList) {
					if (artifact.endsWith(filterValue)) {
						exclude = true;
						break;
					}
				}
			}
			if (!exclude) {
				filteredList.add(artifact);
			}
		}

		return filteredList;
	}

	public List<String> evalIncludeFilter(String type, List<String> artifactList, Set<String> filterValueList) {
		List<String> filteredList = new ArrayList<String>();

		for (String artifact : artifactList) {
			boolean include = false;
			switch (FilterType.valueOf(type)) {
			case CONTAINS:
				for (String filterValue : filterValueList) {
					if (artifact.contains(filterValue)) {
						include = true;
						break;
					}
				}
				break;
			case STARTSWITH:
				for (String filterValue : filterValueList) {
					if (artifact.startsWith(filterValue)) {
						include = true;
						break;
					}
				}
				break;
			case NONE:
				return artifactList;
			case EQUALS:
				for (String filterValue : filterValueList) {
					if (artifact.equals(filterValue)) {
						include = true;
						break;
					}
				}
				break;
			case ENDSWITH:
				for (String filterValue : filterValueList) {
					if (artifact.endsWith(filterValue)) {
						include = true;
						break;
					}
				}
			}
			if (include) {
				filteredList.add(artifact);
			}
		}

		return filteredList;
	}
}
