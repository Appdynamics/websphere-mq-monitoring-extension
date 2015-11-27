package com.appdynamics.extensions.webspheremq;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.file.FileLoader;
import com.appdynamics.extensions.webspheremq.config.Configuration;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

/**
 * This class is responsible for executing AManagedMonitor task. 
 * It reads websphere MQ config.yaml file and populates configuration related beans 
 * and subsequently creates and runs thread for each Queue Manager.
 * 
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class WebspherMqMonitor extends AManagedMonitor {

	public static final Logger logger = LoggerFactory.getLogger(WebspherMqMonitor.class);
	private static final String CONFIG_ARG = "config-file";
	private ExecutorService executorService;
	Configuration config;
	private int executorServiceSize;
	private volatile boolean initialized;
	
	public WebspherMqMonitor() throws ClassNotFoundException {
		System.out.println(logVersion());
	}

	public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext arg1) throws TaskExecutionException {
		logVersion();
		if (taskArgs != null) {
			logger.info("Starting the WebsphereMQ Monitoring task.");
			if (logger.isDebugEnabled()) {
				logger.debug("Task Arguments Passed ::" + taskArgs);
			}
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
			}
			else{
				logger.error("Config file is not found.The config file path {} is resolved to {}",
						taskArgs.get(CONFIG_ARG), configFile != null ? configFile.getAbsolutePath() : null);
			}
			initialized = true;
		}else{
			logger.debug("config already initialized" );
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
	 *
	 * @param config
	 * @return Handles to concurrent tasks.
	 */
	private void runConcurrentTasks() {
		if (config != null && config.getQueueManagers() != null) {
			for (QueueManager queueManager : config.getQueueManagers()) {
				WebsphereMQMonitorTask websphereMqTask = new WebsphereMQMonitorTask(queueManager, config.getMetricPrefix(), config.getMqMertics(), this);
				if(!executorService.isShutdown()){
					executorService.execute(websphereMqTask);
				}else{
					logger.debug("executorService is already shutdown");
				}
			}
		}else{
			logger.error("The config cannot be null");
			throw new IllegalArgumentException("The config cannot be null");
		}
	}
	
	private static String getImplementationVersion() {
		return WebspherMqMonitor.class.getPackage().getImplementationTitle();
	}


	private String logVersion() {
		String msg = "Using WEbsphereMQ Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		return msg;
	}

}
