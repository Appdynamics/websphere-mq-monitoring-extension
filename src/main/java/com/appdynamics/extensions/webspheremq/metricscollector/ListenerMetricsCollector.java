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
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.pcf.PCFParameter;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CountDownLatch;


public class ListenerMetricsCollector extends MetricsCollector implements Runnable {

    public static final Logger logger = ExtensionsLoggerFactory.getLogger(ListenerMetricsCollector.class);
    private final String artifact = "Listeners";

    public ListenerMetricsCollector(MonitorContextConfiguration monitorContextConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper, CountDownLatch countDownLatch) {
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
            logger.error("Error in ListenerMetricsCollector ", e);
        } finally {
            countDownLatch.countDown();
        }
    }

    protected void publishMetrics() throws TaskExecutionException {
        long entryTime = System.currentTimeMillis();

        int[] attrs = new int[] { CMQCFC.MQIACF_ALL };
        logger.debug("Attributes being sent along PCF agent request to query channel metrics: " + Arrays.toString(attrs));

        Set<String> listenerGenericNames = this.queueManager.getListenerFilters().getInclude();
        for(String listenerGenericName : listenerGenericNames){
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS);
            request.addParameter(CMQCFC.MQCACH_LISTENER_NAME, listenerGenericName);
            request.addParameter(CMQCFC.MQIACF_LISTENER_STATUS_ATTRS, attrs);
            try {
                logger.debug("sending PCF agent request to query metrics for generic listener {}", listenerGenericName);
                long startTime = System.currentTimeMillis();
                PCFMessage[] response = agent.send(request);
                long endTime = System.currentTimeMillis() - startTime;
                logger.debug("PCF agent listener metrics query response for generic listener {} received in {} milliseconds", listenerGenericName, endTime);
                if (response == null || response.length <= 0) {
                    logger.debug("Unexpected Error while PCFMessage.send(), response is either null or empty");
                    return;
                }
                for (int i = 0; i < response.length; i++) {
                    String listenerName = response[i].getStringParameterValue(CMQCFC.MQCACH_LISTENER_NAME).trim();
                    Set<ExcludeFilters> excludeFilters = this.queueManager.getListenerFilters().getExclude();
                    if(!isExcluded(listenerName,excludeFilters)) { //check for exclude filters
                        logger.debug("Pulling out metrics for listener name {}",listenerName);
                        Enumeration<PCFParameter> pcfParameters = response[i].getParameters();
                        List<Metric> metrics = Lists.newArrayList();
                        List<Map> mqMetrics = (List<Map>) this.monitorContextConfig.getConfigYml().get("mqMetrics");
                        List<String> excludedMetrics = WMQUtil.getMetricsToExcludeFromConfigYml(mqMetrics, Constants.METRIC_TYPE_LISTENER);
                        while (pcfParameters.hasMoreElements()) {
                            PCFParameter pcfParam = pcfParameters.nextElement();
                            String metrickey = pcfParam.getParameterName();
                            if (!WMQUtil.isMetricExcluded(metrickey, excludedMetrics)) {
                                try {
                                    if (pcfParam != null) {
                                        // create metric objects from PCF parameter
                                        metrics.addAll(createMetrics(queueManager, listenerName, pcfParam));
                                    } else {
                                        logger.warn("PCF parameter is null in response for Listener: {} for metric: {}", listenerName, metrickey);
                                    }
                                } catch (Exception pcfe) {
                                    logger.error("Exception caught while collecting metric for Listener: {} for metric: {}", listenerName, metrickey, pcfe);
                                }
                            }
                            else {
                                logger.debug("Listener metric key {} is excluded.",metrickey);
                            }
                        }
                        publishMetrics(metrics);
                    }
                    else{
                        logger.debug("Listener name {} is excluded.",listenerName);
                    }
                }
            }
            catch (Exception e) {
                logger.error("Unexpected Error occurred while collecting metrics for listener " + listenerGenericName, e);
            }
        }
        long exitTime = System.currentTimeMillis() - entryTime;
        logger.debug("Time taken to publish metrics for all listener is {} milliseconds", exitTime);

    }

    public String getAtrifact() {
        return artifact;
    }
}
