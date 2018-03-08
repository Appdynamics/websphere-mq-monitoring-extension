package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.webspheremq.config.ExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.MetricOverride;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.appdynamics.extensions.webspheremq.metricscollector.MetricsCollector;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class ListenerMetricsCollector extends MetricsCollector {

    public static final Logger logger = LoggerFactory.getLogger(ListenerMetricsCollector.class);
    private final String artifact = "Listeners";


    public ListenerMetricsCollector(Map<String, WMQMetricOverride> metricsToReport, MonitorConfiguration monitorConfig, PCFMessageAgent agent, QueueManager queueManager, MetricWriteHelper metricWriteHelper) {
        this.metricsToReport = metricsToReport;
        this.monitorConfig = monitorConfig;
        this.agent = agent;
        this.metricWriteHelper = metricWriteHelper;
        this.queueManager = queueManager;
    }

    protected void publishMetrics() throws TaskExecutionException {
        long entryTime = System.currentTimeMillis();

        if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
            logger.debug("Listener metrics to report from the config is null or empty, nothing to publish");
            return;
        }

        int[] attrs = getIntAttributesArray(CMQCFC.MQCACH_LISTENER_NAME);
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
                    Set<ExcludeFilters> excludeFilters = this.queueManager.getChannelFilters().getExclude();
                    if(!isExcluded(listenerName,excludeFilters)) { //check for exclude filters
                        logger.debug("Pulling out metrics for listener name {}",listenerName);
                        Iterator<String> itr = getMetricsToReport().keySet().iterator();
                        while (itr.hasNext()) {
                            String metrickey = itr.next();
                            WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
                            int metricVal = response[i].getIntParameterValue(wmqOverride.getConstantValue());
                            publishMetric(wmqOverride, metricVal, queueManager.getName(), getAtrifact(), listenerName, wmqOverride.getAlias());
                        }
                    }
                    else{
                        logger.debug("Listener name {} is excluded.",listenerName);
                    }
                }
            }
            catch (Exception e) {
                logger.error("Unexpected Error occoured while collecting metrics for listener " + listenerGenericName, e);
            }
        }
        long exitTime = System.currentTimeMillis() - entryTime;
        logger.debug("Time taken to publish metrics for all listener is {} milliseconds", exitTime);

    }

    public String getAtrifact() {
        return artifact;
    }

    public Map<String, ? extends MetricOverride> getMetricsToReport() {
        return this.metricsToReport;
    }

}
