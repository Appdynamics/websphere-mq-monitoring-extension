/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.common;

public class Constants {
	
	public static final int DEFAULT_NUMBER_OF_THREADS = 10;
	//TODO Please remove the following two field if they are not required.
	public static final double DEFAULT_MULTIPLIER = 1d;
	public static final String DEFAULT_METRIC_TYPE = "AVERAGE AVERAGE INDIVIDUAL";
	
	public static final String METRIC_TYPE_QUEUE_MANAGER = "queueMgrMetrics";
	public static final String METRIC_TYPE_QUEUE = "queueMetrics";
	public static final String METRIC_TYPE_CHANNEL = "channelMetrics";
	public static final String METRIC_TYPE_LISTENER = "listenerMetrics";
	public static final String METRIC_TYPE_TOPIC = "topicMetrics";
	
	public static final String TRANSPORT_TYPE_CLIENT = "Client";
	public static final String TRANSPORT_TYPE_BINGINGS = "Bindings";

}
