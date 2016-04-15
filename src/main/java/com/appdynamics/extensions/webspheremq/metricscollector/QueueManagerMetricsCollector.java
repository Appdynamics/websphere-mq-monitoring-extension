package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * This class is responsible for queue metric collection.
 * 
 * @author rajeevsingh ,James Schneider
 * @version 2.0
 *
 */
public class QueueManagerMetricsCollector extends MetricsCollector {

	public static final Logger logger = LoggerFactory.getLogger(QueueManagerMetricsCollector.class);
	private final String artifact = "Queue Manager";

	public QueueManagerMetricsCollector(Map<String, ? extends MetricOverride> metricsToReport, MonitorConfiguration writer, PCFMessageAgent agent, QueueManager queueManager, String metricPrefix) {
		this.metricsToReport = metricsToReport;
		this.writer = writer;
		this.agent = agent;
		this.metricPrefix = metricPrefix;
		this.queueManager = queueManager;
	}

	public String getAtrifact() {
		return artifact;
	}

	public void publishMetrics() throws TaskExecutionException {
		long entryTime = System.currentTimeMillis();
		logger.debug("publishMetrics entry time for queuemanager {} is {} milliseconds", agent.getQManagerName(), entryTime);
		PCFMessage request;
		PCFMessage[] responses;
		// CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS is 161
		request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS);
		// CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS is 1229
		request.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS, new int[] { CMQCFC.MQIACF_ALL });
		try {
			// Note that agent.send() method is synchronized
			logger.debug("sending PCF agent request to query queuemanager {}", agent.getQManagerName());
			long startTime = System.currentTimeMillis();
			responses = agent.send(request);
			long endTime = System.currentTimeMillis() - startTime;
			logger.debug("PCF agent queuemanager metrics query response for {} received in {} milliseconds", agent.getQManagerName(), endTime);
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
				publishMetric(wmqOverride, metricVal, queueManager.getName(), wmqOverride.getAlias());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new TaskExecutionException(e);
		} finally {
			long exitTime = System.currentTimeMillis() - entryTime;
			logger.debug("Time taken to publish metrics for queuemanager is {} milliseconds", exitTime);
		}
	}

	public Map<String, ? extends MetricOverride> getMetricsToReport() {
		return metricsToReport;
	}

}
