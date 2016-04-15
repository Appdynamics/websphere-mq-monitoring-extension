package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.appdynamics.extensions.webspheremq.config.ExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class QueueMetricsCollector extends MetricsCollector {

	public static final Logger logger = LoggerFactory.getLogger(QueueMetricsCollector.class);
	private final String artifact = "Queues";

	public QueueMetricsCollector(Map<String, ? extends MetricOverride> metricsToReport, MonitorConfiguration writer, PCFMessageAgent agent, QueueManager queueManager, String metricPrefix) {
		this.metricsToReport = metricsToReport;
		this.writer = writer;
		this.agent = agent;
		this.metricPrefix = metricPrefix;
		this.queueManager = queueManager;
	}

	@Override
	protected void publishMetrics() throws TaskExecutionException {
		/*
		 * attrs = { CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH, CMQC.MQIA_MAX_Q_DEPTH, CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT };
		 */
		long entryTime = System.currentTimeMillis();

		if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
			logger.debug("Queue metrics to report from the config is null or empty, nothing to publish");
			return;
		}


		int[] attrs = getIntArrtibutesArray(CMQC.MQCA_Q_NAME);
		logger.debug("Attributes being sent along PCF agent request to query channel metrics: " + Arrays.toString(attrs));

		Set<String> queueGenericNames = this.queueManager.getQueueFilters().getInclude();
		for(String queueGenericName : queueGenericNames){
			// list of all metrics extracted through MQCMD_INQUIRE_Q is mentioned here https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.adm.doc/q087810_.htm
			PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
			request.addParameter(CMQC.MQCA_Q_NAME, queueGenericName);
			request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_ALL);
			request.addParameter(CMQCFC.MQIACF_Q_ATTRS, attrs);
			PCFMessage[] response;

			try {
				logger.debug("sending PCF agent request to query metrics for generic queue {}",queueGenericName);
				long startTime = System.currentTimeMillis();
				response = agent.send(request);
				long endTime = System.currentTimeMillis() - startTime;
				logger.debug("PCF agent queue metrics query response for generic queue {} received in {} milliseconds", queueGenericName, endTime);
				if (response == null || response.length <= 0) {
					logger.debug("Unexpected Error while PCFMessage.send(), response is either null or empty");
					return;
				}
				for (int i = 0; i < response.length; i++) {
					String queueName = response[i].getStringParameterValue(CMQC.MQCA_Q_NAME).trim();
					Set<ExcludeFilters> excludeFilters = this.queueManager.getQueueFilters().getExclude();
					if(!isExcluded(queueName,excludeFilters)) { //check for exclude filters
						logger.debug("Pulling out metrics for queue name {}",queueName);
						Iterator<String> itr = getMetricsToReport().keySet().iterator();
						while (itr.hasNext()) {
							String metrickey = itr.next();
							WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
							int metricVal = response[i].getIntParameterValue(wmqOverride.getConstantValue());
							publishMetric(wmqOverride, metricVal, queueManager.getName(), getAtrifact(), queueName, wmqOverride.getAlias());
						}
					}
					else{
						logger.debug("Queue name {} is excluded.",queueName);
					}
				}
			} catch (PCFException pcfe) {
				logger.error("PCFException caught while collecting metric for Queue: " + queueGenericName, pcfe);
				PCFMessage[] msgs = (PCFMessage[]) pcfe.exceptionSource;
				for (int i = 0; i < msgs.length; i++) {
					logger.error(msgs[i].toString());
				}
				// Dont throw exception as it will stop queuemetric colloection
			} catch (Exception mqe) {
				logger.error("MQException caught", mqe);
				// Dont throw exception as it will stop queuemetric colloection
			}
			// #TODO extract metrics like CMQC.MQIA_MSG_DEQ_COUNT, CMQC.MQIA_MSG_ENQ_COUNT  using the CMQCFC.MQCMD_RESET_Q_STATS through a new PCF request
		}
		long exitTime = System.currentTimeMillis() - entryTime;
		logger.debug("Time taken to publish metrics for all queues is {} milliseconds", exitTime);
	}



	@Override
	public String getAtrifact() {
		return artifact;
	}

	@Override
	public Map<String, ? extends MetricOverride> getMetricsToReport() {
		return this.metricsToReport;
	}


}
