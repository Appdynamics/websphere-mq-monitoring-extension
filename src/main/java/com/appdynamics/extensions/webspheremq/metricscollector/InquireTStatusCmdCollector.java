/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

class InquireTStatusCmdCollector extends TopicMetricsCollector implements AMonitorTaskRunnable{

    public static final Logger logger = LoggerFactory.getLogger(InquireTStatusCmdCollector.class);

    protected static final String COMMAND = "MQCMD_INQUIRE_TOPIC_STATUS";

    public InquireTStatusCmdCollector(TopicMetricsCollector collector, Map<String, WMQMetricOverride> metricsToReport){
        super(metricsToReport,collector.monitorConfig,collector.agent,collector.queueManager,collector.metricWriteHelper);
    }

    public void run() {
        try {
            logger.info("Collecting metrics for command {}",COMMAND);
            publishMetrics();
        } catch (TaskExecutionException e) {
            logger.error("Something unforeseen has happened ",e);
        }
    }

    protected void publishMetrics() throws TaskExecutionException {
        long entryTime = System.currentTimeMillis();

        if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
            logger.debug("Topic metrics to report from the config is null or empty, nothing to publish for command {}",COMMAND);
            return;
        }
        Set<String> topicGenericNames = this.queueManager.getTopicFilters().getInclude();
        for(String topicGenericName : topicGenericNames){
            // Request: https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.ref.adm.doc/q088140_.htm
            // list of all metrics extracted through MQCMD_INQUIRE_TOPIC_STATUS is mentioned here https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.ref.adm.doc/q088150_.htm
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS);
            request.addParameter(CMQC.MQCA_TOPIC_STRING, topicGenericName);

            try {
                processPCFRequestAndPublishQMetrics(topicGenericName, request,COMMAND);
            } catch (PCFException pcfe) {
                logger.error("PCFException caught while collecting metric for Queue: {} for command {}",topicGenericName,COMMAND, pcfe);
                PCFMessage[] msgs = (PCFMessage[]) pcfe.exceptionSource;
                for (int i = 0; i < msgs.length; i++) {
                    logger.error(msgs[i].toString());
                }
                // Dont throw exception as it will stop queuemetric colloection
            } catch (Exception mqe) {
                logger.error("MQException caught", mqe);
                // Dont throw exception as it will stop queuemetric colloection
            }
        }
        long exitTime = System.currentTimeMillis() - entryTime;
        logger.debug("Time taken to publish metrics for all queues is {} milliseconds for command {}", exitTime,COMMAND);
    }


    public void onTaskComplete() {
        logger.info("WebSphereMQ task for command MQCMD_INQUIRE_TOPIC_STATUS completed for queueManager" + queueManager.getName());
    }
}
