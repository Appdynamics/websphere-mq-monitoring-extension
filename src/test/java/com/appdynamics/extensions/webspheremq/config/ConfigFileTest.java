package com.appdynamics.extensions.webspheremq.config;

import com.appdynamics.extensions.webspheremq.WMQContext;
import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Test;

public class ConfigFileTest {

	@Test
	public void loadConfigFileTest() {
		YmlReader.readFromFile(this.getClass().getResource("/conf/config.yaml").getFile(),Configuration.class);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void loadClientNoHostConfigTest(){
		Configuration config = YmlReader.readFromFile(this.getClass().getResource("/conf/configClientNoHost.yaml").getFile(),Configuration.class);
		QueueManager queueManager = config.getQueueManagers()[0];
		WMQContext auth = new WMQContext(queueManager);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void loadBindingNoQueuemanagerConfigTest(){
		Configuration config = YmlReader.readFromFile(this.getClass().getResource("/conf/configBindingNoQm.yaml").getFile(),Configuration.class);
		QueueManager queueManager = config.getQueueManagers()[0];
		WMQContext auth = new WMQContext(queueManager);
	}
	

}
