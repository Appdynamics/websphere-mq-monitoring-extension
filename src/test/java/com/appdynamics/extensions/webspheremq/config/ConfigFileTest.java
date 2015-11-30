package com.appdynamics.extensions.webspheremq.config;

import org.junit.Test;

import com.appdynamics.extensions.webspheremq.WMQauthenticationAndAuthorization;
import com.appdynamics.extensions.yml.YmlReader;

public class ConfigFileTest {

	@Test
	public void loadConfigFileTest() {
		YmlReader.readFromFile(this.getClass().getResource("/conf/config.yaml").getFile(),Configuration.class);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void loadClientNoHostConfigTest(){
		Configuration config = YmlReader.readFromFile(this.getClass().getResource("/conf/configClientNoHost.yaml").getFile(),Configuration.class);
		QueueManager queueManager = config.getQueueManagers()[0];
		WMQauthenticationAndAuthorization auth = new WMQauthenticationAndAuthorization(queueManager);		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void loadBindingNoQueuemanagerConfigTest(){
		Configuration config = YmlReader.readFromFile(this.getClass().getResource("/conf/configBindingNoQm.yaml").getFile(),Configuration.class);
		QueueManager queueManager = config.getQueueManagers()[0];
		WMQauthenticationAndAuthorization auth = new WMQauthenticationAndAuthorization(queueManager);		
	}
	

}
