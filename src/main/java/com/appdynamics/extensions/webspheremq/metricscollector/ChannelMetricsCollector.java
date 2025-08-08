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
import com.google.common.collect.Lists;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * This class is responsible for channel metric collection.
 *
 * @author rajeevsingh ,James Schneider
 * @version 2.0
 *
 */
public class ChannelMetricsCollector extends MetricsCollector implements Runnable {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(ChannelMetricsCollector.class);
	private final String artifact = "Channels";

	/*
	 * The Channel Status values are mentioned here http://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.dev.doc/q090880_.htm
	 */

	public ChannelMetricsCollector(Map<String, WMQMetricOverride> metricsToReport, MonitorContextConfiguration monitorContextConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper, CountDownLatch countDownLatch) {
		this.metricsToReport = metricsToReport;
		this.monitorContextConfig = monitorContextConfig;
		this.agent = agent;
		this.metricWriteHelper = metricWriteHelper;
		this.queueManager = queueManager;
		this.countDownLatch = countDownLatch;
	}

	public void run() {
		try {
			this.process();
		} catch (TaskExecutionException e) {
			logger.error("Error in ChannelMetricsCollector ", e);
		} finally {
			countDownLatch.countDown();
		}
	}

	@Override
	protected void publishMetrics() throws TaskExecutionException {
		long entryTime = System.currentTimeMillis();

		if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
			logger.debug("Channel metrics to report from the config is null or empty, nothing to publish");
			return;
		}

		// Separate metrics by command type
		Map<String, WMQMetricOverride> statusMetrics = new HashMap<>();
		Map<String, WMQMetricOverride> definitionMetrics = new HashMap<>();
		
		for (Map.Entry<String, WMQMetricOverride> entry : getMetricsToReport().entrySet()) {
			WMQMetricOverride override = entry.getValue();
			if (override.getIbmCommand() != null && override.getIbmCommand().equals("MQCMD_INQUIRE_CHANNEL")) {
				definitionMetrics.put(entry.getKey(), override);
			} else {
				// Default to status metrics for backward compatibility
				statusMetrics.put(entry.getKey(), override);
			}
		}

		logger.debug("Channel status metrics count: {}, Channel definition metrics count: {}", 
				statusMetrics.size(), definitionMetrics.size());

		Set<String> channelGenericNames = this.queueManager.getChannelFilters().getInclude();
		List<String> activeChannels = Lists.newArrayList();
		Set<String> allChannelNames = new HashSet<>();

		// First, collect channel definition metrics (static attributes)
		if (!definitionMetrics.isEmpty()) {
			collectChannelDefinitionMetrics(channelGenericNames, definitionMetrics, allChannelNames);
		}

		// Then, collect channel status metrics (running instance attributes)
		if (!statusMetrics.isEmpty()) {
			collectChannelStatusMetrics(channelGenericNames, statusMetrics, activeChannels, allChannelNames);
		}

		logger.info("Active Channels in queueManager {} are {}", WMQUtil.getQueueManagerNameFromConfig(queueManager), activeChannels);
		Metric activeChannelsCountMetric = createMetric(queueManager,"ActiveChannelsCount", activeChannels.size(), null, getAtrifact(), "ActiveChannelsCount");
		publishMetrics(Arrays.asList(activeChannelsCountMetric));

		long exitTime = System.currentTimeMillis() - entryTime;
		logger.debug("Time taken to publish metrics for all channels is {} milliseconds", exitTime);
	}

	private void collectChannelDefinitionMetrics(Set<String> channelGenericNames, 
			Map<String, WMQMetricOverride> definitionMetrics, Set<String> allChannelNames) {
		
		int[] attrs = getIntAttributesArray(CMQCFC.MQCACH_CHANNEL_NAME);
		logger.debug("Attributes being sent for channel definition query: " + Arrays.toString(attrs));

		for (String channelGenericName : channelGenericNames) {
			PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CHANNEL);
			request.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, channelGenericName);
			request.addParameter(CMQCFC.MQIACF_CHANNEL_ATTRS, attrs);
			
			try {
				logger.debug("Sending PCF request to query channel definitions for generic channel {}", channelGenericName);
				long startTime = System.currentTimeMillis();
				PCFMessage[] response = agent.send(request);
				long endTime = System.currentTimeMillis() - startTime;
				logger.debug("PCF channel definition query response for {} received in {} ms", channelGenericName, endTime);
				
				if (response == null || response.length <= 0) {
					logger.debug("No channel definitions found for pattern {}", channelGenericName);
					continue;
				}
				
				for (PCFMessage pcfMessage : response) {
					String channelName = pcfMessage.getStringParameterValue(CMQCFC.MQCACH_CHANNEL_NAME).trim();
					allChannelNames.add(channelName);
					
					Set<ExcludeFilters> excludeFilters = this.queueManager.getChannelFilters().getExclude();
					if (!isExcluded(channelName, excludeFilters)) {
						logger.debug("Collecting definition metrics for channel {}", channelName);
						List<Metric> metrics = extractChannelMetrics(pcfMessage, channelName, definitionMetrics);
						publishMetrics(metrics);
					} else {
						logger.debug("Channel name {} is excluded", channelName);
					}
				}
			} catch (PCFException pcfe) {
				logger.error("PCF Exception while collecting channel definition metrics for {}: Reason '{}'", 
						channelGenericName, pcfe.getReason(), pcfe);
			} catch (Exception e) {
				logger.error("Unexpected error while collecting channel definition metrics for " + channelGenericName, e);
			}
		}
	}

	private void collectChannelStatusMetrics(Set<String> channelGenericNames, 
			Map<String, WMQMetricOverride> statusMetrics, List<String> activeChannels, Set<String> allChannelNames) {
		
		int[] attrs = getIntAttributesArray(CMQCFC.MQCACH_CHANNEL_NAME, CMQCFC.MQCACH_CONNECTION_NAME);
		logger.debug("Attributes being sent for channel status query: " + Arrays.toString(attrs));

		for (String channelGenericName : channelGenericNames) {
			PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS);
			request.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, channelGenericName);
			request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, CMQC.MQOT_CURRENT_CHANNEL);
			request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_ATTRS, attrs);
			
			try {
				logger.debug("Sending PCF request to query channel status for generic channel {}", channelGenericName);
				long startTime = System.currentTimeMillis();
				PCFMessage[] response = agent.send(request);
				long endTime = System.currentTimeMillis() - startTime;
				logger.debug("PCF channel status query response for {} received in {} ms", channelGenericName, endTime);
				
				if (response == null || response.length <= 0) {
					logger.debug("No active channel instances found for pattern {}", channelGenericName);
					continue;
				}
				
				for (PCFMessage pcfMessage : response) {
					String channelName = pcfMessage.getStringParameterValue(CMQCFC.MQCACH_CHANNEL_NAME).trim();
					allChannelNames.add(channelName);
					
					Set<ExcludeFilters> excludeFilters = this.queueManager.getChannelFilters().getExclude();
					if (!isExcluded(channelName, excludeFilters)) {
						logger.debug("Collecting status metrics for channel {}", channelName);
						List<Metric> metrics = extractChannelMetrics(pcfMessage, channelName, statusMetrics);
						publishMetrics(metrics);
						
						// Check if channel is active for active channels count
						try {
							int status = pcfMessage.getIntParameterValue(CMQCFC.MQIACH_CHANNEL_STATUS);
							if (status == 3) { // RUNNING
								activeChannels.add(channelName);
							}
						} catch (Exception e) {
							logger.debug("Could not determine status for channel {}", channelName);
						}
					} else {
						logger.debug("Channel name {} is excluded", channelName);
					}
				}
			} catch (PCFException pcfe) {
				if (pcfe.getReason() == MQConstants.MQRCCF_CHL_STATUS_NOT_FOUND) {
					String errorMsg = "Channel " + channelGenericName + ": Could not collect channel status as channel is stopped or inactive: Reason '3065'. ";
					errorMsg += "If the channel type is MQCHT_RECEIVER, MQCHT_SVRCONN or MQCHT_CLUSRCVR, then the only action is to enable the channel, not start it.";
					logger.debug(errorMsg); // Reduced to debug level since this is expected for inactive channels
				} else if (pcfe.getReason() == MQConstants.MQRC_SELECTOR_ERROR) {
					logger.error("Invalid metrics passed while collecting channel status metrics, check config.yaml: Reason '2067'", pcfe);
				} else {
					logger.error("PCF Exception while collecting channel status metrics for {}: Reason '{}'", 
							channelGenericName, pcfe.getReason(), pcfe);
				}
			} catch (Exception e) {
				logger.error("Unexpected error while collecting channel status metrics for " + channelGenericName, e);
			}
		}
	}

	private List<Metric> extractChannelMetrics(PCFMessage pcfMessage, String channelName, 
			Map<String, WMQMetricOverride> metricsToExtract) {
		List<Metric> metrics = Lists.newArrayList();
		
		for (Map.Entry<String, WMQMetricOverride> entry : metricsToExtract.entrySet()) {
			String metricKey = entry.getKey();
			WMQMetricOverride wmqOverride = entry.getValue();
			
			try {
				int metricVal = pcfMessage.getIntParameterValue(wmqOverride.getConstantValue());
				Metric metric = createMetric(queueManager, metricKey, metricVal, wmqOverride, getAtrifact(), channelName, metricKey);
				metrics.add(metric);
			} catch (Exception notInt) {
				try {
					String str = pcfMessage.getStringParameterValue(wmqOverride.getConstantValue());
					Integer parsed = null;
					String lower = metricKey.toLowerCase();
					if (lower.contains("date")) {
						parsed = parseDateStringToInt(str);
					} else if (lower.contains("time")) {
						parsed = parseTimeStringToInt(str);
					}
					if (parsed != null) {
						metrics.add(createMetric(queueManager, metricKey, parsed, wmqOverride, getAtrifact(), channelName, metricKey));
					} else {
						metrics.add(createInfoMetricFromString(queueManager, metricKey, str, wmqOverride, getAtrifact(), channelName, metricKey));
					}
				} catch (Exception ignore) {
					logger.debug("Metric {} not available as int or string for channel {}", metricKey, channelName);
				}
			}
		}
		
		return metrics;
	}

    public String getAtrifact() {
		return artifact;
	}

	public Map<String, WMQMetricOverride> getMetricsToReport() {
		return this.metricsToReport;
	}
}