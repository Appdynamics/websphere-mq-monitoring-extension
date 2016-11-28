package com.appdynamics.extensions.webspheremq;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.appdynamics.extensions.webspheremq.config.Configuration;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This class is responsible for executing AManagedMonitor task. 
 * It reads websphere MQ config.yaml file and populates monitorConfiguration related beans
 * and subsequently creates and runs thread for each Queue Manager.
 * 
 *
 */
public class WMQMonitor extends AManagedMonitor {

	public static final Logger logger = LoggerFactory.getLogger(WMQMonitor.class);
	private static final String CONFIG_ARG = "config-file";
	private static final String METRIC_PREFIX = "Custom Metrics|WMQMonitor|";
	private boolean initialized;
	private MonitorConfiguration monitorConfiguration;

	public WMQMonitor() throws ClassNotFoundException {
		System.out.println(logVersion());
	}



	public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext arg1) throws TaskExecutionException {
		logVersion();
		if (taskArgs != null) {
			logger.info("Starting the WebsphereMQ Monitoring task.");
			logger.debug("Task Arguments Passed :: {}",taskArgs);
			try {
				if(!initialized){
					initialize(taskArgs);
				}
				monitorConfiguration.executeTask();
				logger.info("WebsphereMQ monitoring task completed successfully.");
				return new TaskOutput("WebsphereMQ monitoring task completed successfully.");
			} catch (Exception e) {
				logger.error("WebsphereMQ Metrics Collection Failed: ", e);
			}
				
		} 
		throw new TaskExecutionException("WebsphereMQ Monitoring task terminated with failures");
	}


	private void initialize(Map<String, String> taskArgs) {
		if(!initialized){
			//read the config.
			final String configFilePath = taskArgs.get(CONFIG_ARG);
			MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
			MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
			conf.setConfigYml(configFilePath);
			conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
					MonitorConfiguration.ConfItem.METRIC_PREFIX,MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
			this.monitorConfiguration = conf;
			initialized = true;
		}
	}

	private class TaskRunnable implements Runnable{

		public void run() {
			Map<String, ?> configMap = monitorConfiguration.getConfigYml();
			if(configMap != null){
				ObjectMapper mapper = new ObjectMapper();
				Configuration config = mapper.convertValue(configMap,Configuration.class);
				if (config != null && config.getQueueManagers() != null) {
					for (QueueManager queueManager : config.getQueueManagers()) {
						WMQMonitorTask wmqTask = new WMQMonitorTask(queueManager, config.getMetricPrefix(), config.getMqMertics(), monitorConfiguration);
						monitorConfiguration.getExecutorService().execute(wmqTask);
						//#TODO remove this
						/*try {
							Thread.sleep(100000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}*/
					}
				}

			}
		}
	}

	private static String getImplementationVersion() {
		return WMQMonitor.class.getPackage().getImplementationTitle();
	}

	private String logVersion() {
		String msg = "Using WebsphereMQ Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		return msg;
	}

}
