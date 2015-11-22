package com.appdynamics.extensions.webspheremq.config;

import org.junit.Test;

import com.appdynamics.extensions.yml.YmlReader;

public class ConfigFileTest {

	@Test
	public void loadConfigFileTest() {
		YmlReader.readFromFile(this.getClass().getResource("/conf/config.yaml").getFile(),Configuration.class);
	}

}
