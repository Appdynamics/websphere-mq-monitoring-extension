package com.appdynamics.mqmonitor.queue;

public class QueueFilter {
	
	private String queueFilterType;
	
	private String queueFilterValue;
	
	private String excludeInternalQueuesPattern;

	public String getQueueFilterType() {
		return queueFilterType;
	}

	public void setQueueFilterType(String queueFilterType) {
		this.queueFilterType = queueFilterType;
	}

	public String getQueueFilterValue() {
		return queueFilterValue;
	}

	public void setQueueFilterValue(String queueFilterValue) {
		this.queueFilterValue = queueFilterValue;
	}

	public String getExcludeInternalQueuesPattern() {
		return excludeInternalQueuesPattern;
	}

	public void setExcludeInternalQueuesPattern(String excludeInternalQueuesPattern) {
		this.excludeInternalQueuesPattern = excludeInternalQueuesPattern;
	}

	

}
