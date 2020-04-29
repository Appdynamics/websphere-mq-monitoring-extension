/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.Constants;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.CryptoUtils;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class WMQMonitor extends ABaseMonitor {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(WMQMonitor.class);

	protected String getDefaultMetricPrefix() {
		return "Custom Metrics|WMQMonitor|";
	}

	public String getMonitorName() {
		return "WMQMonitor";
	}

	protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
		List<Map> queueManagers = (List<Map>) this.getContextConfiguration().getConfigYml().get("queueManagers");
		AssertUtils.assertNotNull(queueManagers, "The 'queueManagers' section in config.yml is not initialised");
		ObjectMapper mapper = new ObjectMapper();
		for (Map queueManager : queueManagers) {
			QueueManager qManager = mapper.convertValue(queueManager, QueueManager.class);
			WMQMonitorTask wmqTask = new WMQMonitorTask(tasksExecutionServiceProvider, this.getContextConfiguration(), qManager);
			tasksExecutionServiceProvider.submit((String) queueManager.get("name"), wmqTask);
		}
	}

	@Override
	protected List<Map<String, ?>> getServers() {
		List<Map<String, ?>> queueManagers = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("queueManagers");
		AssertUtils.assertNotNull(queueManagers, "The 'queueManagers' section in config.yml is not initialised");
		return queueManagers;
	}

	@Override
	protected void initializeMoreStuff(Map<String, String> args) {
		super.initializeMoreStuff(args);
		Map<String, ?> configProperties = this.getContextConfiguration().getConfigYml();
		Map<String, String> sslConnection = (Map<String, String>) configProperties.get("sslConnection");

		if (sslConnection != null ) {
			String encryptionKey = (String) configProperties.get("encryptionKey");
			logger.debug("Encryption key from config.yml set for ssl connection is " + encryptionKey);

			String trustStorePath = sslConnection.get("trustStorePath");
			if (!Strings.isNullOrEmpty(trustStorePath)) {
				System.setProperty("javax.net.ssl.trustStore",trustStorePath);
				logger.debug("System property set for javax.net.ssl.trustStore is " + trustStorePath);
				String trustStorePassword = getPassword(sslConnection.get("trustStorePassword"), sslConnection.get("trustStoreEncryptedPassword"), encryptionKey);
				if (!Strings.isNullOrEmpty(trustStorePassword)) {
					System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
					logger.debug("System property set for javax.net.ssl.trustStorePassword is xxxxx");
				}
			} else {
				logger.debug("trustStorePath is not set in config.yml, ignoring setting trustStorePath as system property");
			}

			String keyStorePath = sslConnection.get("keyStorePath");
			if (!Strings.isNullOrEmpty(keyStorePath)) {
				System.setProperty("javax.net.ssl.keyStore", keyStorePath);
				logger.debug("System property set for javax.net.ssl.keyStore is " + keyStorePath);
				String keyStorePassword = getPassword(sslConnection.get("keyStorePassword"), sslConnection.get("keyStoreEncryptedPassword"), encryptionKey);
				if (!Strings.isNullOrEmpty(keyStorePassword)) {
					System.setProperty("javax.net.ssl.keyStorePassword",keyStorePassword);
					logger.debug("System property set for javax.net.ssl.keyStorePassword is xxxxx");
				}
			} else {
				logger.debug("keyStorePath is not set in config.yml, ignoring setting keyStorePath as system property");
			}
		} else {
			logger.debug("ssl truststore and keystore are not configured in config.yml, if SSL is enabled, pass them as jvm args");
		}
	}

	private String getPassword(String password, String encryptedPassword, String encryptionKey) {
		if (!Strings.isNullOrEmpty(password)) {
			return password;
		}
		if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
			Map<String, String> cryptoMap = Maps.newHashMap();
			cryptoMap.put(Constants.ENCRYPTED_PASSWORD, encryptedPassword);
			cryptoMap.put(Constants.ENCRYPTION_KEY, encryptionKey);
			return CryptoUtils.getPassword(cryptoMap);
		}
		return null;
	}
}
