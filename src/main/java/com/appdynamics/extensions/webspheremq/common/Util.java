package com.appdynamics.extensions.webspheremq.common;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

public class Util {

	public static final Logger logger = Logger.getLogger(Util.class);

	public static void printMetric(String name, String value, String aggType, String timeRollup, String clusterRollup, AManagedMonitor monitor) {
		String metricName = getMetricPrefix() + name;
		MetricWriter metricWriter = monitor.getMetricWriter(metricName, aggType, timeRollup, clusterRollup);
		metricWriter.printMetric(value);
		logger.info("METRIC:  NAME:" + metricName + " VALUE:" + value + " :" + aggType + ":" + timeRollup + ":" + clusterRollup);
	}

	private static String getMetricPrefix() {
		return "";
	}

}
