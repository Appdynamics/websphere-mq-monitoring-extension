package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.webspheremq.config.MetricOverride;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

class InquireQCmdCollector extends QueueMetricsCollector implements Runnable {

    public static final Logger logger = LoggerFactory.getLogger(InquireQCmdCollector.class);

    protected static final String COMMAND = "MQCMD_INQUIRE_Q";

    public InquireQCmdCollector(QueueMetricsCollector collector, Map<String, ? extends MetricOverride> metricsToReport){
        super(metricsToReport,collector.monitorConfig,collector.agent,collector.queueManager, collector.metricWriteHelper);
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
		/*
		 * attrs = { CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH, CMQC.MQIA_MAX_Q_DEPTH, CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT };
		 */
        long entryTime = System.currentTimeMillis();

        if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
            logger.debug("Queue metrics to report from the config is null or empty, nothing to publish");
            return;
        }

        int[] attrs = getIntAttributesArray(CMQC.MQCA_Q_NAME);
        logger.debug("Attributes being sent along PCF agent request to query queue metrics: {} for command {}",Arrays.toString(attrs),COMMAND);

        Set<String> queueGenericNames = this.queueManager.getQueueFilters().getInclude();
        for(String queueGenericName : queueGenericNames){
            // list of all metrics extracted through MQCMD_INQUIRE_Q is mentioned here https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.adm.doc/q087810_.htm
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, queueGenericName);
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_ALL);
            request.addParameter(CMQCFC.MQIACF_Q_ATTRS, attrs);

            try {
                processPCFRequestAndPublishQMetrics(queueGenericName, request,COMMAND);
            } catch (PCFException pcfe) {
                logger.error("PCFException caught while collecting metric for Queue: {} for command {}",queueGenericName,COMMAND, pcfe);
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


}
