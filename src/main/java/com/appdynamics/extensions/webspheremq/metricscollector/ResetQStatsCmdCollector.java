/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.metricscollector;


import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

class ResetQStatsCmdCollector extends QueueMetricsCollector implements Runnable{

    public static final Logger logger = ExtensionsLoggerFactory.getLogger(ResetQStatsCmdCollector.class);

    protected static final String COMMAND = "MQCMD_RESET_Q_STATS";

    public ResetQStatsCmdCollector(QueueMetricsCollector collector, Map<String, WMQMetricOverride> metricsToReport){
        super(metricsToReport,collector.monitorContextConfig,collector.agent,collector.queueManager,collector.metricWriteHelper, collector.countDownLatch);
    }

    public void run() {
        try {
            logger.info("Collecting metrics for command {}",COMMAND);
            collectAndPublish();
        } catch (TaskExecutionException e) {
            logger.error("Something unforeseen has happened ",e);
        }
    }

    protected void collectAndPublish() throws TaskExecutionException {
		/*
		 * attrs = { CMQC.MQCA_Q_NAME, MQIA_HIGH_Q_DEPTH,MQIA_MSG_DEQ_COUNT, MQIA_MSG_ENQ_COUNT };
		 */
        long entryTime = System.currentTimeMillis();

        if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
            logger.debug("Queue metrics to report from the config is null or empty, nothing to publish for command {}",COMMAND);
            return;
        }

        int[] attrs = getIntAttributesArray(CMQC.MQCA_Q_NAME);
        logger.debug("Attributes being sent along PCF agent request to query queue metrics: {} for command {}", Arrays.toString(attrs),COMMAND);

        Set<String> queueGenericNames = this.queueManager.getQueueFilters().getInclude();
        for(String queueGenericName : queueGenericNames){
            // list of all metrics extracted through MQCMD_RESET_Q_STATS is mentioned here https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.ref.adm.doc/q088310_.htm
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_RESET_Q_STATS);
            request.addParameter(CMQC.MQCA_Q_NAME, queueGenericName);
            try {
                processPCFRequestAndPublishQMetrics(queueGenericName, request,COMMAND);
            } catch (PCFException pcfe) {
                logger.error("PCFException caught while collecting metric for Queue: {} for command {}",queueGenericName,COMMAND, pcfe);
                PCFMessage[] msgs = (PCFMessage[]) pcfe.exceptionSource;
                for (int i = 0; i < msgs.length; i++) {
                    logger.error(msgs[i].toString());
                }
                // Don't throw exception as it will stop queue metric collection
            } catch (Exception mqe) {
                logger.error("MQException caught", mqe);
                // Don't throw exception as it will stop queue metric collection
            }
        }
        long exitTime = System.currentTimeMillis() - entryTime;
        logger.debug("Time taken to publish metrics for all queues is {} milliseconds for command {}", exitTime,COMMAND);
    }
}
