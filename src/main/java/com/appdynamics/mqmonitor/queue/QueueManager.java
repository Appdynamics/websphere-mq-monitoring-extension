/**
 * 
 */
package com.appdynamics.monitors.mqmonitor.queue;

import java.util.List;

import com.ibm.mq.MQQueueManager;

/**
 * @author James Schneider
 *
 */
public class QueueManager {

	protected MQQueueManager mqQueueManager;
	protected String managerName;
	protected String host;
	protected int port;
	protected String channelName;
	protected String transportType;
	protected String userId;
	protected String password;
	
	private List<Queue> queues;
	
	/**
	 * 
	 */
	public QueueManager() {
		
	}

	
	public MQQueueManager getQueueManager() {
		return mqQueueManager;
	}


	public void setQueueManager(MQQueueManager mqQueueManager) {
		this.mqQueueManager = mqQueueManager;
	}


	public String getManagerName() {
		return managerName;
	}

	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getTransportType() {
		return transportType;
	}

	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<Queue> getQueues() {
		return queues;
	}

	public void setQueues(List<Queue> queueStats) {
		this.queues = queueStats;
	}

	
}
