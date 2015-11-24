package com.appdynamics.extensions.webspheremq.metricscollector;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.util.metrics.MetricConstants;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class QueueManagerMetricsCollector extends MetricsCollector {

	public static final Logger logger = Logger.getLogger(QueueManagerMetricsCollector.class);
	private final String artifact = "Queue Manager";

	public QueueManagerMetricsCollector(Map<String, ? extends MetricOverride> metricsToReport, AManagedMonitor monitor, PCFMessageAgent agent, QueueManager queueManager, String metricPrefix) {
		this.metricsToReport = metricsToReport;
		this.monitor = monitor;
		this.agent = agent;
		this.metricPrefix = metricPrefix;
		this.queueManager = queueManager;
	}

	public void processFilter() {
		// Filters are not applicable for Queue manager
	}

	public String getAtrifact() {
		return artifact;
	}

	public void publishMetrics() throws TaskExecutionException {
		PCFMessage request;
		PCFMessage[] responses;
		// CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS is 161
		request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS);
		// CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS is 1229
		request.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS, new int[] { CMQCFC.MQIACF_ALL });
		try {
			// Note that agent.send() method is synchronized
			logger.info("Sending PCF request... " + agent.getQManagerName());
			responses = agent.send(request);
			if (responses == null || responses.length <= 0) {
				logger.debug("Unexpected Error while PCFMessage.send(), response is either null or empty");
				return;
			}
			Iterator<String> overrideItr = getMetricsToReport().keySet().iterator();
			while (overrideItr.hasNext()) {
				String metrickey = overrideItr.next();
				WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
				int metricVal = responses[0].getIntParameterValue(wmqOverride.getConstantValue());
				if (logger.isDebugEnabled()) {
					logger.debug("Metric: " + wmqOverride.getAlias() + "=" + metricVal);
				}
				String metricName = this.metricPrefix + queueManager.getName() + MetricConstants.METRICS_SEPARATOR + wmqOverride.getAlias();
				BigInteger bigVal = toBigInteger(metricVal, getMultiplier(wmqOverride));
				printMetric(metricName, String.valueOf(bigVal.intValue()), wmqOverride.getAggregator(), wmqOverride.getTimeRollup(), wmqOverride.getClusterRollup(), monitor);
			}
		} catch (Exception e) {
			throw new TaskExecutionException(e);
		}
	}

	public Map<String, ? extends MetricOverride> getMetricsToReport() {
		return metricsToReport;
	}

}
