package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.webspheremq.config.ExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.MetricOverride;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TopicMetricsCollector extends MetricsCollector {
    public static final Logger logger = LoggerFactory.getLogger(TopicMetricsCollector.class);
    private final String artifact = "Topics";

    public TopicMetricsCollector(Map<String, ? extends MetricOverride> metricsToReport, MonitorConfiguration monitorConfig, PCFMessageAgent agent, QueueManager queueManager, String metricPrefix) {
        this.metricsToReport = metricsToReport;
        this.monitorConfig = monitorConfig;
        this.agent = agent;
        this.metricPrefix = metricPrefix;
        this.queueManager = queueManager;
    }

    protected void publishMetrics() throws TaskExecutionException {
        logger.info("Collecting Topic metrics...");
        List<Future> futures = Lists.newArrayList();

        Map<String, ? extends MetricOverride>  metricsForInquireTStatusCmd = getMetricsToReport(InquireTStatusCmdCollector.COMMAND);
        if(!metricsForInquireTStatusCmd.isEmpty()){
            futures.add(monitorConfig.getExecutorService().submit(new InquireTStatusCmdCollector(this,metricsForInquireTStatusCmd)));
        }
        for(Future f: futures){
            try {
                long timeout = 20;
                if(monitorConfig.getConfigYml().get("topicsMetricsCollectionTimeoutInSeconds") != null){
                    timeout = (Integer)monitorConfig.getConfigYml().get("topicMetricsCollectionTimeoutInSeconds");
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
        /*if (response == null || response.length <= 0) {
            logger.debug("Unexpected Error while PCFMessage.send() for command {}, response is either null or empty",command);
            return;
        }*/
        for (int i = 0; i < response.length; i++) {
            String topicString = response[i].getStringParameterValue(CMQC.MQCA_TOPIC_STRING).trim();
            Set<ExcludeFilters> excludeFilters = this.queueManager.getTopicFilters().getExclude();
            if(!isExcluded(topicString,excludeFilters)) { //check for exclude filters
                logger.debug("Pulling out metrics for topic name {} for command {}",topicString,command);
                Iterator<String> itr = getMetricsToReport().keySet().iterator();
                while (itr.hasNext()) {
                    String metrickey = itr.next();
                    WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
                    try{
                        PCFParameter pcfParam = response[i].getParameter(wmqOverride.getConstantValue());
                        if(pcfParam instanceof MQCFIN){
                            int metricVal = response[i].getIntParameterValue(wmqOverride.getConstantValue());
                            publishMetric(wmqOverride, metricVal, queueManager.getName(), getAtrifact(), topicString, wmqOverride.getAlias());
                        }
                        else if(pcfParam instanceof MQCFIL){
                            int[] metricVals = response[i].getIntListParameterValue(wmqOverride.getConstantValue());
                            if(metricVals != null){
                                int count=0;
                                for(int val : metricVals){
                                    count++;
                                    publishMetric(wmqOverride, val, queueManager.getName(), getAtrifact(), topicString, wmqOverride.getAlias(),"_" + Integer.toString(count));
                                }
                            }


                        }
                    }
                    catch (PCFException pcfe) {
                        logger.error("PCFException caught while collecting metric for Queue: {} for metric: {} in command {}",topicString, wmqOverride.getIbmCommand(),command, pcfe);
                    }

                }
            }
            else{
                logger.debug("Queue name {} is excluded.",topicString);
            }
        }

    }

    private Map<String, ? extends MetricOverride> getMetricsToReport(String command) {
        Map<String, WMQMetricOverride> commandMetrics = Maps.newHashMap();
        if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
            logger.debug("There are no metrics configured for {}",command);
            return commandMetrics;
        }
        Iterator<String> itr = getMetricsToReport().keySet().iterator();
        while (itr.hasNext()) {
            String metrickey = itr.next();
            WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
            if(wmqOverride.getIbmCommand().equalsIgnoreCase(command)){
                commandMetrics.put(metrickey,wmqOverride);
            }
        }
        return commandMetrics;
    }

    public String getAtrifact() {
        return artifact;
    }

    public Map<String, ? extends MetricOverride> getMetricsToReport() {
        return this.metricsToReport;
    }
}
