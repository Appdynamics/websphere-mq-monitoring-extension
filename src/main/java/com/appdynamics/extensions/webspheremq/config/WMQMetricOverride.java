package com.appdynamics.extensions.webspheremq.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WMQMetricOverride extends MetricOverride {

	String ibmConstant;
	String ibmCommand;
	int constantValue = -1;
	public static final Logger logger = LoggerFactory.getLogger(WMQMetricOverride.class);

	public WMQMetricOverride() {
		super();
	}

	public String getIbmConstant() {
		return ibmConstant;
	}

	public void setIbmConstant(String ibmConstant) {
		this.ibmConstant = ibmConstant;
	}

	public String getIbmCommand() {
		return ibmCommand;
	}

	public void setIbmCommand(String ibmCommand) {
		this.ibmCommand = ibmCommand;
	}

	public int getConstantValue() {
		if (constantValue == -1) {
			int lastPacSeparatorDotIdx = getIbmConstant().lastIndexOf('.');
			if (lastPacSeparatorDotIdx != -1) {
				String declaredField = getIbmConstant().substring(lastPacSeparatorDotIdx + 1);
				String classStr = getIbmConstant().substring(0, lastPacSeparatorDotIdx);
				Class clazz;
				try {
					clazz = Class.forName(classStr);
					constantValue = (Integer) clazz.getDeclaredField(declaredField).get(Integer.class);
				} catch (Exception e) {
					logger.warn(e.getMessage());
					logger.warn("ibmConstant {} is not a valid constant defaulting constant value to -1", getIbmConstant());
				}
			}
		}
		return constantValue;
	}

	public void setConstantValue(int constantValue) {
		this.constantValue = constantValue;
	}

	@Override
	public String toString() {
		StringBuilder stringRep = new StringBuilder();

		stringRep.append("[");
		stringRep.append("Metric Key=" + getMetricKey() + ",");
		stringRep.append("Alias=" + getAlias() + ",");
		stringRep.append("IbmConstant=" + getIbmConstant() + ",");
		stringRep.append("IbmCommand=" + getIbmCommand() + ",");
		stringRep.append("ConstantVal=" + getConstantValue() + ",");
		stringRep.append("Aggregator=" + getAggregator() + ",");
		stringRep.append("TimeRollup=" + getTimeRollup() + ",");
		stringRep.append("ClusterRollup=" + getClusterRollup() + ",");
		stringRep.append("Multiplier=" + getMultiplier() + ",");
		stringRep.append("Disabled=" + isDisabled());
		stringRep.append("]");

		return stringRep.toString();
	}

}
