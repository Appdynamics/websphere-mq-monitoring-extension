package com.appdynamics.extensions.webspheremq.metricscollector;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.webspheremq.config.ExcludeFilters;
import com.appdynamics.extensions.webspheremq.config.MetricOverride;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.webspheremq.config.WMQMetricOverride;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import java.util.concurrent.*;

public class QueueMetricsCollector extends MetricsCollector {

	public static final Logger logger = LoggerFactory.getLogger(QueueMetricsCollector.class);
	private final String artifact = "Queues";

	public QueueMetricsCollector(Map<String, ? extends MetricOverride> metricsToReport, MonitorConfiguration monitorConfig, PCFMessageAgent agent, QueueManager queueManager, String metricPrefix) {
		this.metricsToReport = metricsToReport;
		this.monitorConfig = monitorConfig;
		this.agent = agent;
		this.metricPrefix = metricPrefix;
		this.queueManager = queueManager;
	}

	@Override
	protected void publishMetrics() throws TaskExecutionException {
		logger.info("Collecting queue metrics...");
		List<Future> futures = Lists.newArrayList();
		Map<String, ? extends MetricOverride>  metricsForInquireQCmd = getMetricsToReport(InquireQCmdCollector.COMMAND);
		if(!metricsForInquireQCmd.isEmpty()){
			futures.add(monitorConfig.getExecutorService().submit(new InquireQCmdCollector(this,metricsForInquireQCmd)));
		}
		Map<String, ? extends MetricOverride>  metricsForInquireQStatusCmd = getMetricsToReport(InquireQStatusCmdCollector.COMMAND);
		if(!metricsForInquireQStatusCmd.isEmpty()){
			futures.add(monitorConfig.getExecutorService().submit(new InquireQStatusCmdCollector(this,metricsForInquireQStatusCmd)));
		}
		Map<String, ? extends MetricOverride>  metricsForResetQStatsCmd = getMetricsToReport(ResetQStatsCmdCollector.COMMAND);
		if(!metricsForResetQStatsCmd.isEmpty()){
			futures.add(monitorConfig.getExecutorService().submit(new ResetQStatsCmdCollector(this,metricsForResetQStatsCmd)));
		}
		for(Future f: futures){
			try {
				long timeout = 20;
				if(monitorConfig.getConfigYml().get("threadTimeoutInSeconds") != null){
					timeout = Long.parseLong((String)monitorConfig.getConfigYml().get("threadTimeoutInSeconds"));
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

	@Override
	public String getAtrifact() {
		return artifact;
	}

	@Override
	public Map<String, ? extends MetricOverride> getMetricsToReport() {
		return this.metricsToReport;
	}

	protected void processPCFRequestAndPublishQMetrics(String queueGenericName, PCFMessage request, String command) throws MQException, IOException {
		PCFMessage[] response;
		logger.debug("sending PCF agent request to query metrics for generic queue {} for command {}",queueGenericName,command);
		long startTime = System.currentTimeMillis();
		response = agent.send(request);
		long endTime = System.currentTimeMillis() - startTime;
		logger.debug("PCF agent queue metrics query response for generic queue {} for command {} received in {} milliseconds", queueGenericName, command,endTime);
		if (response == null || response.length <= 0) {
			logger.debug("Unexpected Error while PCFMessage.send() for command {}, response is either null or empty",command);
			return;
		}
		for (int i = 0; i < response.length; i++) {
			String queueName = response[i].getStringParameterValue(CMQC.MQCA_Q_NAME).trim();
			Set<ExcludeFilters> excludeFilters = this.queueManager.getQueueFilters().getExclude();
			if(!isExcluded(queueName,excludeFilters)) { //check for exclude filters
				logger.debug("Pulling out metrics for queue name {} for command {}",queueName,command);
				Iterator<String> itr = getMetricsToReport().keySet().iterator();
				while (itr.hasNext()) {
					String metrickey = itr.next();
					WMQMetricOverride wmqOverride = (WMQMetricOverride) getMetricsToReport().get(metrickey);
					PCFParameter pcfParam = response[i].getParameter(wmqOverride.getConstantValue());
					if(pcfParam instanceof MQCFIN){
						int metricVal = response[i].getIntParameterValue(wmqOverride.getConstantValue());
						publishMetric(wmqOverride, metricVal, queueManager.getName(), getAtrifact(), queueName, wmqOverride.getAlias());
					}
					else if(pcfParam instanceof MQCFIL){
						int[] metricVals = response[i].getIntListParameterValue(wmqOverride.getConstantValue());
						if(metricVals != null){
							int count=0;
							for(int val : metricVals){
								count++;
								publishMetric(wmqOverride, val, queueManager.getName(), getAtrifact(), queueName, wmqOverride.getAlias(),"_" + Integer.toString(count));
							}
						}


					}
				}
			}
			else{
				logger.debug("Queue name {} is excluded.",queueName);
			}
		}

	}
}
