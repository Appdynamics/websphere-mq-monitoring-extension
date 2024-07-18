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
import com.google.common.collect.Lists;
import com.ibm.mq.pcf.*;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.text.ParseException;
import java.time.format.DateTimeParseException;
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

	protected MonitorContextConfiguration monitorContextConfig;
	protected PCFMessageAgent agent;
	protected MetricWriteHelper metricWriteHelper;
	protected QueueManager queueManager;
	protected CountDownLatch countDownLatch;

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(MetricsCollector.class);

	protected abstract void publishMetrics() throws TaskExecutionException;

	public abstract String getAtrifact();

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

	protected Metric createMetric(QueueManager queueManager, String metricName, String metricValue, WMQMetricOverride wmqOverride, String... pathelements) {
		String metricPath = getMetricsName(WMQUtil.getQueueManagerNameFromConfig(queueManager), pathelements);
		Metric metric;
		if (wmqOverride != null && wmqOverride.getMetricProperties() != null) {
			metric = new Metric(metricName, String.valueOf(metricValue), metricPath, wmqOverride.getMetricProperties());
		} else {
			metric = new Metric(metricName, String.valueOf(metricValue), metricPath);
		}
		return metric;
	}

	protected List<Metric> createMetrics(QueueManager queueManager, String mqObjectName, PCFParameter pcfParam) {
		List<Metric> metrics = Lists.newArrayList();

		String metrickey = pcfParam.getParameterName();
		String metricValueString = pcfParam.getStringValue();

		// ignore the metric if the metric value is NULL
		if (metricValueString == null) {
			return metrics;
		} else {
			metricValueString = metricValueString.trim();
		}

		if(pcfParam instanceof MQCFIN || pcfParam instanceof MQCFST){
			int metricVal = 0;

			// if value type is 'String i.e. MQCFST', process only if the String value represents DATE or TIME
			// ignore the metric if the value is not valid DATE or TIME value
			if (pcfParam instanceof MQCFST) {
				try {
					if (WMQUtil.isDateValue(metricValueString)) {
						metricVal = WMQUtil.getDateDifferenceInDays(metricValueString);
					} else if (WMQUtil.isTimeValue(metricValueString)) {
						metricVal = WMQUtil.getTimeDifferenceInHours(metricValueString);
					} else {
						logger.info("Found string metric value:{}, ignoring the metric {} for {}: {}",metricValueString, metrickey, getAtrifact(), mqObjectName != null ? mqObjectName:queueManager.getName());
						return metrics;
					}
				}
				catch (Exception parseException) {
					logger.error("Exception parsing the date or time metric value:{} for metric {} for {}: {}",metricValueString, metrickey, getAtrifact(), mqObjectName != null ? mqObjectName:queueManager.getName());
					return metrics;
				}
			} else {
				// ignore the metric if the metric value is negative
				metricVal = ((MQCFIN) pcfParam).getIntValue();
				if (metricVal < 0) {
					logger.info("Found negative metric value:{}, ignoring the metric {} for {}: {}",metricValueString, metrickey, getAtrifact(), mqObjectName != null ? mqObjectName:queueManager.getName());
					return  metrics;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Metric: " + metrickey + "=" + metricVal);
			}
			// 'mqObjectName' would be null if the metric is related to 'Queue Manager'
			Metric metric = null;
			if (mqObjectName != null) {
				// used for all MQ objects metrics other than 'QueueManager'
				metric = createMetric(queueManager, metrickey, metricVal, null, getAtrifact(), mqObjectName, metrickey);
			} else {
				// used for 'QueuMmanager' metrics
				metric = createMetric(queueManager, metrickey, metricVal, null, metrickey);
			}
			metrics.add(metric);
		}
		else if(pcfParam instanceof MQCFIL){
			int[] metricVals = ((MQCFIL) pcfParam).getValues();
			if(metricVals != null){
				int count=0;
				for(int val : metricVals){
					if (val < 0) {
						logger.info("Found negative metric value:{}, ignoring the metric {} for {}: {}", val, metrickey, getAtrifact(), mqObjectName != null ? mqObjectName : queueManager.getName());
						continue;
					}
					count++;
					String metricKey = metrickey+ "_" + Integer.toString(count);
					String metricVal = String.valueOf(val);
					if (logger.isDebugEnabled()) {
						logger.debug("Metric: " + metrickey + "=" + metricVal);
					}
					Metric metric = createMetric(queueManager, metricKey, metricVal, null, getAtrifact(), mqObjectName, metricKey);
					metrics.add(metric);
				}
			}
		}

		return metrics;
	}

	protected Metric getMetricByKey(String metricKey, List<Metric> metrics) {
		Metric foundMetric = null;

		if (metrics == null || metrics.isEmpty()) {
			return foundMetric;
		}

		for (Metric metric : metrics) {
			if (StringUtils.equalsIgnoreCase(metricKey, metric.getMetricName())) {
				foundMetric = metric;
				break;
			}
		}

		return foundMetric;
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
}
