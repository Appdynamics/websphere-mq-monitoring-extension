package com.appdynamics.extensions.webspheremq.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.util.metrics.MetricOverride;

public class WMQMetricOverride extends MetricOverride {

	String ibmConstant;
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

}
