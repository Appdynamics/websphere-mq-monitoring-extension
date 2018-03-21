/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.webspheremq.config.Configuration;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * This class is responsible for executing AManagedMonitor task. 
 * It reads websphere MQ config.yaml file and populates monitorConfiguration related beans
 * and subsequently creates and runs thread for each Queue Manager.
 * 
 *
 */
public class WMQMonitor extends ABaseMonitor {

	public static final Logger logger = LoggerFactory.getLogger(WMQMonitor.class);

	protected String getDefaultMetricPrefix() {
		return "Custom Metrics|WMQMonitor|";
	}

	public String getMonitorName() {
		return "WebsphereMQ Monitoring Extension";
	}

	protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
		try {
			Map<String, ?> configMap = configuration.getConfigYml();
			if (configMap != null) {
				ObjectMapper mapper = new ObjectMapper();
				Configuration config = mapper.convertValue(configMap,Configuration.class);
				if (config != null && config.getQueueManagers() != null) {
					QueueManager[] queueManagers = config.getQueueManagers();
					AssertUtils.assertNotNull(queueManagers, "The 'queueManagers' section in config.yml is not initialised");

					for (QueueManager queueManager : queueManagers) {
						queueManager.setEncryptionKey(config.getEncryptionKey());
						WMQMonitorTask wmqTask = new WMQMonitorTask(tasksExecutionServiceProvider, queueManager, config.getMqMetrics());
						tasksExecutionServiceProvider.submit(queueManager.getName(), wmqTask);
					}
				}
			}
			logger.info("WebsphereMQ monitoring task completed successfully.");
		} catch (Exception e) {
			logger.error("WebsphereMQ Metrics Collection Failed: ", e);
		}
	}

	protected int getTaskCount() {
		List queueManagers = ((List)configuration.getConfigYml().get("queueManagers"));
		AssertUtils.assertNotNull(queueManagers, "The 'queueManagers' section in config.yml is not initialised");
		return queueManagers.size();
	}

}
