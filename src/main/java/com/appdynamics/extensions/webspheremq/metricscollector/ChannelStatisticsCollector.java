package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.collect.Lists;
import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.MQQueueManager;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Experimental: Collect channel statistics by consuming messages from SYSTEM.ADMIN.STATISTICS.QUEUE.
 * Requires MQ STATISTICS to be enabled (e.g., MONCHL/STATINT) and appropriate authorities.
 */
public class ChannelStatisticsCollector extends MetricsCollector implements Runnable {

    public static final Logger logger = ExtensionsLoggerFactory.getLogger(ChannelStatisticsCollector.class);
    private final String artifact = "ChannelStatistics";

    private final MQQueueManager ibmQueueManager;

    public ChannelStatisticsCollector(Map<String, WMQMetricOverride> metricsToReport,
                                      MonitorContextConfiguration monitorContextConfig,
                                      MQQueueManager ibmQueueManager,
                                      PCFMessageAgent agent,
                                      QueueManager queueManager,
                                      MetricWriteHelper metricWriteHelper,
                                      CountDownLatch countDownLatch) {
        this.metricsToReport = metricsToReport;
        this.monitorContextConfig = monitorContextConfig;
        this.ibmQueueManager = ibmQueueManager;
        this.agent = agent;
        this.metricWriteHelper = metricWriteHelper;
        this.queueManager = queueManager;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        try {
            this.process();
        } catch (TaskExecutionException e) {
            logger.error("Error in ChannelStatisticsCollector ", e);
        } finally {
            countDownLatch.countDown();
        }
    }

    @Override
    protected void publishMetrics() throws TaskExecutionException {
        MQQueue queue = null;
        try {
            queue = ibmQueueManager.accessQueue("SYSTEM.ADMIN.STATISTICS.QUEUE", CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_INQUIRE);
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = CMQC.MQGMO_NO_WAIT | CMQC.MQGMO_CONVERT | CMQC.MQGMO_PROPERTIES_IN_HANDLE;

            int polled = 0;
            while (polled < 20) { // bounded read per cycle
                polled++;
                MQMessage msg = new MQMessage();
                try {
                    queue.get(msg, gmo);
                } catch (MQException mqe) {
                    if (mqe.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
                        break;
                    } else {
                        logger.debug("Error while reading statistics message", mqe);
                        break;
                    }
                }

                try {
                    PCFMessage pcf = new PCFMessage(msg);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Decoded channel statistics PCF message: {}", pcf);
                    }
                    String channelName = safeString(pcf.getStringParameterValue(CMQCFC.MQCACH_CHANNEL_NAME));
                    if (channelName == null || channelName.isEmpty()) {
                        continue;
                    }
                    Iterator<String> itr = getMetricsToReport().keySet().iterator();
                    List<Metric> toPublish = Lists.newArrayList();
                    while (itr.hasNext()) {
                        String metricKey = itr.next();
                        WMQMetricOverride override = getMetricsToReport().get(metricKey);
                        try {
                            int val = pcf.getIntParameterValue(override.getConstantValue());
                            toPublish.add(createMetric(queueManager, metricKey, val, override, getAtrifact(), channelName, metricKey));
                        } catch (Exception notInt) {
                            try {
                                String s = pcf.getStringParameterValue(override.getConstantValue());
                                if (s != null) {
                                    toPublish.add(createInfoMetricFromString(queueManager, metricKey, s, override, getAtrifact(), channelName, metricKey));
                                }
                            } catch (Exception ignored) {
                                // ignore missing params
                            }
                        }
                    }
                    publishMetrics(toPublish);
                } catch (Exception ex) {
                    logger.debug("Failed to decode statistics message", ex);
                }
            }
        } catch (Exception e) {
            logger.error("ChannelStatisticsCollector error", e);
        } finally {
            if (queue != null) {
                try { queue.close(); } catch (Exception ignore) {}
            }
        }
    }

    private String safeString(String s) { return s == null ? "" : s.trim(); }

    @Override
    public String getAtrifact() {
        return artifact;
    }

    @Override
    public Map<String, WMQMetricOverride> getMetricsToReport() {
        return this.metricsToReport;
    }
}


