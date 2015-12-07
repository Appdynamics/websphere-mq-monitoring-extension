package com.appdynamics.extensions.webspheremq.metricscollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.appdynamics.extensions.webspheremq.config.QueueExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.QueueIncludeFilters;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.ibm.mq.MQException;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class QueueMetricsCollector extends MetricsCollector {

	public static final Logger logger = LoggerFactory.getLogger(QueueMetricsCollector.class);
	private final String artifact = "Queues";
	List<String> queueList;

	public QueueMetricsCollector(Map<String, ? extends MetricOverride> metricsToReport, AManagedMonitor monitor, PCFMessageAgent agent, QueueManager queueManager, String metricPrefix) {
		this.metricsToReport = metricsToReport;
		this.monitor = monitor;
		this.agent = agent;
		this.metricPrefix = metricPrefix;
		this.queueManager = queueManager;
	}

	@Override
	protected void processFilter() throws TaskExecutionException {
		List<String> allQueues = getQueueList();

		// First evaluate include filters and then exclude filters
		QueueIncludeFilters includeFilters = this.queueManager.getQueueIncludeFilters();
		List<String> includedQueues = evalIncludeFilter(includeFilters.getType(), allQueues, includeFilters.getValues());

		QueueExcludeFilters excludeFilters = this.queueManager.getQueueExcludeFilters();
		queueList = evalExcludeFilter(excludeFilters.getType(), includedQueues, excludeFilters.getValues());

	}

	@Override
	protected void publishMetrics() throws TaskExecutionException {
		if (queueList == null || queueList.isEmpty()) {
			logger.debug("queue List empty");
			return;
		}
		for (String queueName : queueList) {
			publishQueueMetrics(queueName);
		}
	}

	private void publishQueueMetrics(String queueName) {
		/*
		 * attrs = { CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH, CMQC.MQIA_MAX_Q_DEPTH, CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT };
		 */
		if (getMetricsToReport() == null || getMetricsToReport().isEmpty()) {
			logger.debug("Queue metrics to report is null or empty, nothing to publish");
		}
		int[] attrs = getIntArrtibutesArray(CMQC.MQCA_Q_NAME);

		PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
		request.addParameter(CMQC.MQCA_Q_NAME, queueName);
		request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_ALL);
		request.addParameter(CMQCFC.MQIACF_Q_ATTRS, attrs);
		PCFMessage[] response;

		try {
			response = agent.send(request);
			for (int i = 0; i < response.length; i++) {
				Iterator<String> itr = getMetricsToReport().keySet().iterator();
				while (itr.hasNext()) {
					String metrickey = itr.next();
					WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
					int metricVal = response[i].getIntParameterValue(wmqOverride.getConstantValue());
					publishMetric(wmqOverride, metricVal, queueManager.getName(), getAtrifact(), queueName, wmqOverride.getAlias());
				}
			}
		} catch (PCFException pcfe) {
			logger.error("PCFException caught while collecting metric for Queue: " + queueName, pcfe);
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

	@Override
	public String getAtrifact() {
		return artifact;
	}

	@Override
	public Map<String, ? extends MetricOverride> getMetricsToReport() {
		return this.metricsToReport;
	}

	private List<String> getQueueList() throws TaskExecutionException {

		List<String> allQueueList = new ArrayList<String>();
		List<String> localQueueList = getQueueListByType(MQConstants.MQQT_LOCAL);

		allQueueList.addAll(localQueueList);

		return allQueueList;

	}

	/**
	 * @param queueType
	 *            May be one of following types type MQConstants.MQQT_LOCAL, MQConstants.MQQT_ALIAS, MQConstants.MQQT_REMOTE
	 * @return List of queues of specified type
	 * @throws TaskExecutionException
	 */
	private List<String> getQueueListByType(int queueType) throws TaskExecutionException {
		List<String> queueList = new ArrayList<String>();

		try {

			PCFMessage inquireNames = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_NAMES);

			inquireNames.addParameter(CMQC.MQCA_Q_NAME, "*");

			// TODO see if this filters out the model queues
			inquireNames.addParameter(CMQC.MQIA_Q_TYPE, queueType);

			PCFMessage[] responseMsgs = agent.send(inquireNames);

			if (responseMsgs == null || responseMsgs.length == 0) {
				logger.error("Unable to get response from PCF");
				throw new TaskExecutionException("Unable to get response from PCF");
			}
			String[] names = (String[]) responseMsgs[0].getParameterValue(CMQCFC.MQCACF_Q_NAMES);

			for (int i = 0; i < names.length; i++) {
				names[i] = names[i].trim();

				if (!queueList.contains(names[i])) {
					queueList.add(names[i]);
				}

			}
		} catch (PCFException e) {
			logger.error(e.getMessage());
			throw new TaskExecutionException(e);
		} catch (MQException e) {
			logger.error(e.getMessage());
			throw new TaskExecutionException(e);
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw new TaskExecutionException(e);
		}

		return queueList;
	}

}
