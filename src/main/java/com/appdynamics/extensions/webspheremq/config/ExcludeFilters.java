/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.config;

import java.util.HashSet;
import java.util.Set;

/**
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class ExcludeFilters {

	String type;
	private Set<String> values = new HashSet<String>();
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Set<String> getValues() {
		return values;
	}
	public void setValues(Set<String> values) {
		this.values = values;
	}
	
}
