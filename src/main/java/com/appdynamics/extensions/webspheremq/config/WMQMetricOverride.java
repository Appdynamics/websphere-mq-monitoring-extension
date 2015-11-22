package com.appdynamics.extensions.webspheremq.config;

import com.appdynamics.extensions.util.metrics.MetricOverride;

public class WMQMetricOverride extends MetricOverride {

	String name;
	String ibmConstant;
	int constantValue;

	public WMQMetricOverride() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIbmConstant() {
		return ibmConstant;
	}

	public void setIbmConstant(String ibmConstant) {
		this.ibmConstant = ibmConstant;
	}

	public int getConstantValue() {
		return constantValue;
	}

	public void setConstantValue(int constantValue) {
		this.constantValue = constantValue;
	}

}
