/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.webspheremq.config.ExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.pcf.*;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class TopicMetricsCollector extends MetricsCollector implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(TopicMetricsCollector.class);
    private final String artifact = "Topics";

    public TopicMetricsCollector(Map<String, WMQMetricOverride> metricsToReport, MonitorContextConfiguration monitorContextConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper, CountDownLatch countDownLatch) {
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
            logger.error("Error in TopicMetricsCollector ", e);
        } finally {
            countDownLatch.countDown();
        }
    }

    protected void publishMetrics() throws TaskExecutionException {
        logger.info("Collecting Topic metrics...");
        List<Future> futures = Lists.newArrayList();

        Map<String, WMQMetricOverride>  metricsForInquireTStatusCmd = getMetricsToReport(InquireTStatusCmdCollector.COMMAND);
        if(!metricsForInquireTStatusCmd.isEmpty()){
            futures.add(monitorContextConfig.getContext().getExecutorService().submit("Topic Status Cmd Collector", new InquireTStatusCmdCollector(this, metricsForInquireTStatusCmd)));
        }
        for(Future f: futures){
            try {
                long timeout = 20;
                if(monitorContextConfig.getConfigYml().get("topicMetricsCollectionTimeoutInSeconds") != null){
                    timeout = (Integer)monitorContextConfig.getConfigYml().get("topicMetricsCollectionTimeoutInSeconds");
                }
                f.get(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("The thread was interrupted ",e);
            } catch (ExecutionException e) {
                logger.error("Something unforeseen has happened ",e);
            } catch (TimeoutException e) {
                logger.error("Thread timed out ",e);
            }
        }
    }

    protected void processPCFRequestAndPublishQMetrics(String topicGenericName, PCFMessage request, String command) throws MQException, IOException {
        PCFMessage[] response;
        logger.debug("sending PCF agent request to topic metrics for generic topic {} for command {}",topicGenericName,command);
        long startTime = System.currentTimeMillis();
        response = agent.send(request);
        long endTime = System.currentTimeMillis() - startTime;
        logger.debug("PCF agent topic metrics query response for generic topic {} for command {} received in {} milliseconds", topicGenericName, command,endTime);
        if (response == null || response.length <= 0) {
            logger.debug("Unexpected Error while PCFMessage.send() for command {}, response is either null or empty",command);
            return;
        }
        for (int i = 0; i < response.length; i++) {
            String topicString = response[i].getStringParameterValue(CMQC.MQCA_TOPIC_STRING).trim();
            Set<ExcludeFilters> excludeFilters = this.queueManager.getTopicFilters().getExclude();
            if(!isExcluded(topicString,excludeFilters)) { //check for exclude filters
                logger.debug("Pulling out metrics for topic name {} for command {}",topicString,command);
                Iterator<String> itr = getMetricsToReport().keySet().iterator();
                List<Metric> metrics = Lists.newArrayList();
                while (itr.hasNext()) {
                    String metrickey = itr.next();
                    WMQMetricOverride wmqOverride = getMetricsToReport().get(metrickey);
                    try{
                        PCFParameter pcfParam = response[i].getParameter(wmqOverride.getConstantValue());
                        if(pcfParam instanceof MQCFIN){
                            int metricVal = response[i].getIntParameterValue(wmqOverride.getConstantValue());
                            Metric metric = createMetric(metrickey, metricVal, wmqOverride, queueManager.getName(), getAtrifact(), topicString, metrickey);
                            metrics.add(metric);
                        }
                    }
                    catch (PCFException pcfe) {
                        logger.error("PCFException caught while collecting metric for Topic: {} for metric: {} in command {}",topicString, wmqOverride.getIbmCommand(),command, pcfe);
                    }

                }
                publishMetrics(metrics);
            }
            else{
                logger.debug("Topic name {} is excluded.",topicString);
            }
        }

    }

    private Map<String, WMQMetricOverride> getMetricsToReport(String command) {
        Map<String, WMQMetricOverride> commandMetrics = Maps.newHashMap();
        if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
            logger.debug("There are no metrics configured for {}",command);
            return commandMetrics;
        }
        Iterator<String> itr = getMetricsToReport().keySet().iterator();
        while (itr.hasNext()) {
            String metrickey = itr.next();
            WMQMetricOverride wmqOverride = getMetricsToReport().get(metrickey);
            if(wmqOverride.getIbmCommand().equalsIgnoreCase(command)){
                commandMetrics.put(metrickey,wmqOverride);
            }
        }
        return commandMetrics;
    }

    public String getAtrifact() {
        return artifact;
    }

    public Map<String, WMQMetricOverride> getMetricsToReport() {
        return this.metricsToReport;
    }
}
