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
import com.appdynamics.extensions.webspheremq.common.Constants;
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
import com.ibm.mq.pcf.PCFParameter;
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

	public ChannelMetricsCollector(MonitorContextConfiguration monitorContextConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper, CountDownLatch countDownLatch) {
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

		int[] attrs = new int[] { CMQCFC.MQIACF_ALL };
		logger.debug("Attributes being sent along PCF agent request to query channel metrics: " + Arrays.toString(attrs));

		Set<String> channelGenericNames = this.queueManager.getChannelFilters().getInclude();

		List<String> activeChannels = Lists.newArrayList();
		for(String channelGenericName : channelGenericNames){
			PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS);
			request.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, channelGenericName);
			request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, CMQC.MQOT_CURRENT_CHANNEL);
			request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_ATTRS, attrs);
			try {
				logger.debug("sending PCF agent request to query metrics for generic channel {}", channelGenericName);
				long startTime = System.currentTimeMillis();
				PCFMessage[] response = agent.send(request);
				long endTime = System.currentTimeMillis() - startTime;
				logger.debug("PCF agent queue metrics query response for generic queue {} received in {} milliseconds", channelGenericName, endTime);
				if (response == null || response.length <= 0) {
					logger.debug("Unexpected Error while PCFMessage.send(), response is either null or empty");
					return;
				}
				for (int i = 0; i < response.length; i++) {
					String channelName = response[i].getStringParameterValue(CMQCFC.MQCACH_CHANNEL_NAME).trim();
					Set<ExcludeFilters> excludeFilters = this.queueManager.getChannelFilters().getExclude();
					if(!isExcluded(channelName,excludeFilters)) { //check for exclude filters
						logger.debug("Pulling out metrics for channel name {}",channelName);
						Enumeration<PCFParameter> pcfParameters = response[i].getParameters();
						List<Metric> metrics = Lists.newArrayList();
						List<Map> mqMetrics = (List<Map>) this.monitorContextConfig.getConfigYml().get("mqMetrics");
						List<String> excludedMetrics = WMQUtil.getMetricsToExcludeFromConfigYml(mqMetrics, Constants.METRIC_TYPE_CHANNEL);
						while (pcfParameters.hasMoreElements()) {
							PCFParameter pcfParam = pcfParameters.nextElement();
							String metrickey = pcfParam.getParameterName();
							if (!WMQUtil.isMetricExcluded(metrickey, excludedMetrics)) {
								try {
									if (pcfParam != null) {
										// create metric objects from PCF parameter
										metrics.addAll(createMetrics(queueManager, channelName, pcfParam));

										if ("MQIACH_CHANNEL_STATUS".equals(metrickey)) {
											Metric metric = getMetricByKey("MQIACH_CHANNEL_STATUS", metrics);
											if (metric.getMetricValue() != null && Integer.parseInt(metric.getMetricValue()) == 3) {
												activeChannels.add(channelName);
											}
										}
									} else {
										logger.warn("PCF parameter is null in response for Channel: {} for metric: {}", channelName, metrickey);
									}
								} catch (Exception pcfe) {
									logger.error("Exception caught while collecting metric for Channel: {} for metric: {}", channelName, metrickey, pcfe);
								}
							}
							else {
								logger.debug("Channel metric key {} is excluded.",metrickey);
							}
						}
						publishMetrics(metrics);
					}
					else{
						logger.debug("Channel name {} is excluded.",channelName);
					}
				}
			}
			catch (PCFException pcfe) {
				String errorMsg = "";
				if (pcfe.getReason() == MQConstants.MQRCCF_CHL_STATUS_NOT_FOUND) {
					errorMsg = "Channel- " + channelGenericName + " :";
					errorMsg += "Could not collect channel information as channel is stopped or inactive: Reason '3065'\n";
					errorMsg += "If the channel type is MQCHT_RECEIVER, MQCHT_SVRCONN or MQCHT_CLUSRCVR, then the only action is to enable the channel, not start it.";
					logger.error(errorMsg,pcfe);
				} else if (pcfe.getReason() == MQConstants.MQRC_SELECTOR_ERROR) {
					logger.error("Invalid metrics passed while collecting channel metrics, check config.yaml: Reason '2067'",pcfe);
				}
			} catch (Exception e) {
				logger.error("Unexpected Error occoured while collecting metrics for channel " + channelGenericName, e);
			}
		}

		logger.info("Active Channels in queueManager {} are {}", WMQUtil.getQueueManagerNameFromConfig(queueManager), activeChannels);
		Metric activeChannelsCountMetric = createMetric(queueManager,"ActiveChannelsCount", activeChannels.size(), null, getAtrifact(), "ActiveChannelsCount");
		publishMetrics(Arrays.asList(activeChannelsCountMetric));

		long exitTime = System.currentTimeMillis() - entryTime;
		logger.debug("Time taken to publish metrics for all channels is {} milliseconds", exitTime);

	}

	public String getAtrifact() {
		return artifact;
	}
}