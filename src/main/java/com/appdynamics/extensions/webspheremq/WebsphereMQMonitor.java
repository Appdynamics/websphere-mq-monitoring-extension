package com.appdynamics.extensions.webspheremq;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.file.FileLoader;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.appdynamics.extensions.webspheremq.config.Configuration;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This class is responsible for executing AManagedMonitor task. 
 * It reads websphere MQ config.yaml file and populates configuration related beans 
 * and subsequently creates and runs thread for each Queue Manager.
 * 
 *
 */
public class WebsphereMQMonitor extends AManagedMonitor {

	public static final Logger logger = LoggerFactory.getLogger(WebsphereMQMonitor.class);
	private static final String CONFIG_ARG = "config-file";
	private ExecutorService executorService;
	private Configuration config;
	private int executorServiceSize;
	private volatile boolean initialized;
	private MonitorConfiguration writer;

	public WebsphereMQMonitor() throws ClassNotFoundException {
		System.out.println(logVersion());
	}



	public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext arg1) throws TaskExecutionException {
		logVersion();
		if (taskArgs != null) {
			logger.info("Starting the WebsphereMQ Monitoring task.");
				logger.debug("Task Arguments Passed :: {}",taskArgs);
			try {
				initialize(taskArgs);
				runConcurrentTasks();
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
			File configFile = PathResolver.getFile(configFilePath, AManagedMonitor.class);
			logger.debug("config file path:"+configFilePath );
			if(configFile != null && configFile.exists()){
				FileLoader.load(new FileLoader.Listener() {
					public void load(File file) {
						String path = file.getAbsolutePath();
						try {
							if (path.contains(configFilePath)) {
								logger.info("The file [{}] has changed, reloading the config", file.getAbsolutePath());
								reloadConfig(file);
							}
							else {
								logger.warn("Unknown file [{}] changed, ignoring", file.getAbsolutePath());
							}
						} catch (Exception e) {
							logger.error("Exception while reloading the file " + file.getAbsolutePath(), e);
						}
					}
				}, configFilePath);
				MonitorConfiguration conf = new MonitorConfiguration("Custom Metrics|WebsphereMQ");
				conf.setMetricWriter(MetricWriteHelperFactory.create(this));
				conf.checkIfInitialized(MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
				this.writer = conf;
			}
			else{
				logger.error("Config file is not found.The config file path {} is resolved to {}",
						taskArgs.get(CONFIG_ARG), configFile != null ? configFile.getAbsolutePath() : null);
			}
			initialized = true;
		}else{
			logger.debug("config.yaml is already initialized" );
		}
	}
	
	private void reloadConfig(File file) {
		config = YmlReader.readFromFile(file, Configuration.class);
		if (config != null) {
			int numOfThreads = config.getNumberOfThreads();
			if (executorService == null) {
				executorService = createThreadPool(numOfThreads);
				logger.info("Initializing the ThreadPool with size {}", config.getNumberOfThreads());
			}
			else if (numOfThreads != executorServiceSize) {
				logger.info("The ThreadPool size has been updated from {} -> {}", executorServiceSize, numOfThreads);
				executorService.shutdown();
				executorService = createThreadPool(numOfThreads);
			}
			executorServiceSize = numOfThreads;
		}
		else {
			throw new IllegalArgumentException("The config cannot be initialized from the file " + file.getAbsolutePath());
		}
	}
	
	
	private ExecutorService createThreadPool(int noOfThreads) {
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat("WebsphereMq-Task-Thread-%d")
				.build();
		return Executors.newFixedThreadPool(noOfThreads,threadFactory);
	}

	/**
	 * Executes concurrent tasks
	 * @return Handles to concurrent tasks.
	 */
	private void runConcurrentTasks() {
		if (config != null && config.getQueueManagers() != null) {
			for (QueueManager queueManager : config.getQueueManagers()) {
				WebsphereMQMonitorTask websphereMqTask = new WebsphereMQMonitorTask(queueManager, config.getMetricPrefix(), config.getMqMertics(), writer);
				executorService.execute(websphereMqTask);
				//#TODO remove this
				/*try {
					Thread.sleep(100000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/
			}
		}else{
			logger.error("The config cannot be null");
			throw new IllegalArgumentException("The config cannot be null");
		}
	}



	private static String getImplementationVersion() {
		return WebsphereMQMonitor.class.getPackage().getImplementationTitle();
	}


	private String logVersion() {
		String msg = "Using WebsphereMQ Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		return msg;
	}

}
