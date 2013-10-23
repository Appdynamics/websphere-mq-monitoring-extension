package com.appdynamics.monitors.mqmonitor.common;

import java.util.Map;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public abstract class JavaServersMonitor extends AManagedMonitor
{
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	protected volatile long oldTime = 0;
	protected volatile long currentTime = 0;

	public abstract TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
			throws TaskExecutionException;


}
