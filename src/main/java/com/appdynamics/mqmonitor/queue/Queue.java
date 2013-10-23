/**
 * 
 */
package com.appdynamics.monitors.mqmonitor.queue;

import com.ibm.mq.MQQueue;

/**
 * @author James Schneider
 *
 */
public class Queue {

	protected MQQueue mqQueue;
	protected QueueManager queueManager;
	protected String queueName;
	protected int currentDepth = -1;
	protected int maximumDepth = -1;
	
	
	/**
	 * 
	 */
	public Queue() {
		
	}


	public MQQueue getMQ() {
		return mqQueue;
	}


	public void setMQ(MQQueue mqQueue) {
		this.mqQueue = mqQueue;
	}


	public QueueManager getQueueManager() {
		return queueManager;
	}


	public void setQueueManager(QueueManager queueManager) {
		this.queueManager = queueManager;
	}


	public String getQueueName() {
		return queueName;
	}


	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}


	public int getCurrentDepth() {
		return currentDepth;
	}


	public void setCurrentDepth(int currentDepth) {
		this.currentDepth = currentDepth;
	}


	public int getMaximumDepth() {
		return maximumDepth;
	}


	public void setMaximumDepth(int maximumDepth) {
		this.maximumDepth = maximumDepth;
	}

	
}
