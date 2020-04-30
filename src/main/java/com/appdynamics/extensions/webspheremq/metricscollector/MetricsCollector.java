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
import com.appdynamics.extensions.webspheremq.common.WMQUtil;
import com.appdynamics.extensions.webspheremq.config.ExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.base.Strings;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * MetricsCollector class is abstract and serves as superclass for all types of metric collection class.<br>
 * It contains common methods to extract or transform metric value and names.
 * 
 * @author rajeevsingh
 * @version 2.0
 *
 */
public abstract class MetricsCollector implements Runnable {

	protected Map<String, WMQMetricOverride> metricsToReport;
	protected MonitorContextConfiguration monitorContextConfig;
	protected PCFMessageAgent agent;
	protected MetricWriteHelper metricWriteHelper;
	protected QueueManager queueManager;
	protected CountDownLatch countDownLatch;

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricsCollector.class);

	protected abstract void publishMetrics() throws TaskExecutionException;

	public abstract String getAtrifact();

	public abstract Map<String, WMQMetricOverride> getMetricsToReport();

	/**
	 * Applies include and exclude filters to the artifacts (i.e queue manager, q, or channel),<br>
	 * extracts and publishes the metrics to controller
	 * 
	 * @throws TaskExecutionException
	 */
	public final void process() throws TaskExecutionException {
		publishMetrics();
	}

	protected String getMetricsName(String qmNameToBeDisplayed, String... pathelements) {
		StringBuilder pathBuilder = new StringBuilder(monitorContextConfig.getMetricPrefix()).append("|").append(qmNameToBeDisplayed).append("|");
		for (int i = 0; i < pathelements.length; i++) {
			pathBuilder.append(pathelements[i]);
			if (i != pathelements.length - 1) {
				pathBuilder.append("|");
			}
		}
		return pathBuilder.toString();
	}

	protected Metric createMetric(QueueManager queueManager, String metricName, int metricValue, WMQMetricOverride wmqOverride, String... pathelements) {
		String metricPath = getMetricsName(WMQUtil.getQueueManagerNameFromConfig(queueManager), pathelements);
		Metric metric;
		if (wmqOverride != null && wmqOverride.getMetricProperties() != null) {
			metric = new Metric(metricName, String.valueOf(metricValue), metricPath, wmqOverride.getMetricProperties());
		} else {
			metric = new Metric(metricName, String.valueOf(metricValue), metricPath);
		}
		return metric;
	}

	protected void publishMetrics(List<Metric> metrics) {
		metricWriteHelper.transformAndPrintMetrics(metrics);
	}

	public static enum FilterType {
		STARTSWITH, EQUALS, ENDSWITH, CONTAINS, NONE;
	}

	public boolean isExcluded(String resourceName, Set<ExcludeFilters> excludeFilters) {
		if(excludeFilters == null){
			return false;
		}
		for(ExcludeFilters filter : excludeFilters){
			if(isExcluded(resourceName, filter)){
				return true;
			}
		}
		return false;
	}

	public boolean isExcluded(String resourceName, ExcludeFilters excludeFilter){
		if (Strings.isNullOrEmpty(resourceName))
			return true;
		String type = excludeFilter.getType();
		Set<String> filterValues = excludeFilter.getValues();
		switch (FilterType.valueOf(type)){
			case CONTAINS:
				for (String filterValue : filterValues) {
					if (resourceName.contains(filterValue)) {
						return true;
					}
				}
				break;
			case STARTSWITH:
				for (String filterValue : filterValues) {
					if (resourceName.startsWith(filterValue)) {
						return true;
					}
				}
				break;
			case NONE:
				return false;
			case EQUALS:
				for (String filterValue : filterValues) {
					if (resourceName.equals(filterValue)) {
						return true;
					}
				}
				break;
			case ENDSWITH:
				for (String filterValue : filterValues) {
					if (resourceName.endsWith(filterValue)) {
						return true;
					}
				}
		}
		return false;
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

	protected int[] getIntAttributesArray(int... inputAttrs) {
		int[] attrs = new int[inputAttrs.length+getMetricsToReport().size()];
		// fill input attrs
		for(int i=0 ; i< inputAttrs.length; i++){
			attrs[i]=inputAttrs[i];
		}
		//fill attrs from metrics to report.
		Iterator<String> overrideItr = getMetricsToReport().keySet().iterator();
		for (int count = inputAttrs.length; overrideItr.hasNext() && count < attrs.length; count++) {
			String metrickey = overrideItr.next();
			WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
			attrs[count] = wmqOverride.getConstantValue();
		}
		return attrs;
		
	}
}
