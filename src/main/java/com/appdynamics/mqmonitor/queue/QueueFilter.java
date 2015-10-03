package com.appdynamics.mqmonitor.queue;

public class QueueFilter {
	
	private String queueFilterType;
	
	private String queueFilterValue;
	
	private String queueFilterExcludeType;
	
	private String queueFilterExcludeValue;

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

	public String getQueueFilterExcludeType() {
		return queueFilterExcludeType;
	}

	public void setQueueFilterExcludeType(String queueFilterExcludeType) {
		this.queueFilterExcludeType = queueFilterExcludeType;
	}

	public String getQueueFilterExcludeValue() {
		return queueFilterExcludeValue;
	}

	public void setQueueFilterExcludeValue(String queueFilterExcludeValue) {
		this.queueFilterExcludeValue = queueFilterExcludeValue;
	}

	
}
