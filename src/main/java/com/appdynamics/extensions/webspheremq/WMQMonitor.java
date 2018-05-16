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
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class WMQMonitor extends ABaseMonitor {

	public static final Logger logger = LoggerFactory.getLogger(WMQMonitor.class);

	protected String getDefaultMetricPrefix() {
		return "Custom Metrics|WMQMonitor|";
	}

	public String getMonitorName() {
		return "WMQMonitor";
	}

	protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
		List<Map> queueManagers = (List<Map>) this.getContextConfiguration().getConfigYml().get("queueManagers");
		AssertUtils.assertNotNull(queueManagers, "The 'queueManagers' section in config.yml is not initialised");
		ObjectMapper mapper = new ObjectMapper();
		for (Map queueManager : queueManagers) {
			QueueManager qManager = mapper.convertValue(queueManager, QueueManager.class);
			WMQMonitorTask wmqTask = new WMQMonitorTask(tasksExecutionServiceProvider, this.getContextConfiguration(), qManager);
			tasksExecutionServiceProvider.submit((String) queueManager.get("name"), wmqTask);
		}
	}

	protected int getTaskCount() {
		List queueManagers = ((List)this.getContextConfiguration().getConfigYml().get("queueManagers"));
		AssertUtils.assertNotNull(queueManagers, "The 'queueManagers' section in config.yml is not initialised");
		return queueManagers.size();
	}
}
