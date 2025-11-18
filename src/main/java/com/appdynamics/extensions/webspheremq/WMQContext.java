/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq;


import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.CryptoUtils;
import com.appdynamics.extensions.util.StringUtils;
import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.ibm.mq.constants.CMQC;
import com.ibm.msg.client.wmq.WMQConstants;
import java.util.Hashtable;
import org.slf4j.Logger;

/**
 * Takes care of websphere mq connection, authentication, SSL, Cipher spec, certificate based authorization.<br>
 * It also validates the arguments passed for various scenarios.
 * 
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class WMQContext {

	public static final Logger logger = ExtensionsLoggerFactory.getLogger(WMQContext.class);
	private static final String MQCSP_PROPERTY = "com.ibm.mq.cfg.useMQCSPAuthentication";
	private QueueManager queueManager;

	static {
		// Force MQ Java layer to consider MQCSP credentials even before any connections are created.
		if (!Boolean.getBoolean(MQCSP_PROPERTY)) {
			System.setProperty(MQCSP_PROPERTY, "true");
		}
	}

	public WMQContext(QueueManager queueManager) {
		this.queueManager = queueManager;
		validateArgs();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Hashtable getMQEnvironment() {
		Hashtable env = new Hashtable();
        // Only set network properties when using Client transport. Supplying
        // host/port with bindings mode causes a forced TCP connection attempt
        // and results in 2538 (connection refused).
        if (Constants.TRANSPORT_TYPE_CLIENT.equalsIgnoreCase(queueManager.getTransportType())) {
            addEnvProperty(env, CMQC.HOST_NAME_PROPERTY, queueManager.getHost());
            addEnvProperty(env, CMQC.PORT_PROPERTY, queueManager.getPort());
            addEnvProperty(env, CMQC.CHANNEL_PROPERTY, queueManager.getChannelName());
            configureCredentials(env);
            addEnvProperty(env, CMQC.SSL_CERT_STORE_PROPERTY, queueManager.getSslKeyRepository());
            addEnvProperty(env, CMQC.SSL_CIPHER_SUITE_PROPERTY, queueManager.getCipherSuite());
            addEnvProperty(env, CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_CLIENT);
        } else if (Constants.TRANSPORT_TYPE_BINDINGS.equalsIgnoreCase(queueManager.getTransportType())) {
            // For bindings we only provide the transport type; MQ java libs will
            // connect locally using installation libraries.
            addEnvProperty(env, CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_BINDINGS);
        } else {
            // Default MQ transport
            addEnvProperty(env, CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES);
        }

		logger.debug("Transport property is " + env.get(CMQC.TRANSPORT_PROPERTY));
		return env;
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void addEnvProperty(Hashtable env, String propName, Object propVal) {
		if (null != propVal) {
			if(propVal instanceof String){
				String propString = (String)propVal;
				if(Strings.isNullOrEmpty(propString)){
					return;
				}
			}
			env.put(propName, propVal);
		}
	}

	private void validateArgs() {
		boolean validArgs = true;
		StringBuilder errorMsg = new StringBuilder();
		if (queueManager == null) {
			validArgs = false;
			errorMsg.append("Queue manager cannot be null");
		} else {
			if (Constants.TRANSPORT_TYPE_CLIENT.equalsIgnoreCase(queueManager.getTransportType())) {
				if (!StringUtils.hasText(queueManager.getHost())) {
					validArgs = false;
					errorMsg.append("Host cannot be null or empty for client type connection. ");
				}
				if (queueManager.getPort() == -1) {
					validArgs = false;
					errorMsg.append("port should be set for client type connection. ");
				}
				if (!StringUtils.hasText(queueManager.getChannelName())) {
					validArgs = false;
					errorMsg.append("Channel cannot be null or empty for client type connection. ");
				}
			}
            if (Constants.TRANSPORT_TYPE_BINDINGS.equalsIgnoreCase(queueManager.getTransportType())) {
				if (!StringUtils.hasText(queueManager.getName())) {
					validArgs = false;
					errorMsg.append("queuemanager cannot be null or empty for bindings type connection. ");
				}
                // Ensure we are not accidentally providing network props in bindings mode
                if (StringUtils.hasText(queueManager.getHost()) || queueManager.getPort() != -1 || StringUtils.hasText(queueManager.getChannelName())) {
                    logger.warn("Host/port/channel are ignored for bindings transport; remove them from config to avoid confusion.");
                }
			}
		}

		if (!validArgs) {
			throw new IllegalArgumentException(errorMsg.toString());
		}
	}

	private String getPassword() {
		String password = queueManager.getPassword();
		if (!Strings.isNullOrEmpty(password)) {
			return password;
		}
		String encryptionKey = queueManager.getEncryptionKey();
		String encryptedPassword = queueManager.getEncryptedPassword();
		if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)) {
			java.util.Map<String, String> cryptoMap = Maps.newHashMap();
			cryptoMap.put(com.appdynamics.extensions.Constants.ENCRYPTED_PASSWORD, encryptedPassword);
			cryptoMap.put(com.appdynamics.extensions.Constants.ENCRYPTION_KEY, encryptionKey);
			return CryptoUtils.getPassword(cryptoMap);
		}
		return null;
	}

	private void configureCredentials(Hashtable env) {
		String configuredUser = queueManager.getUsername();
		String resolvedPassword = getPassword();
		boolean hasUser = StringUtils.hasText(configuredUser);
		boolean hasPassword = StringUtils.hasText(resolvedPassword);
		
		if (hasUser && hasPassword) {
			// Explicitly enable MQCSP authentication (instead of OS user adoption)
			addEnvProperty(env, CMQC.USE_MQCSP_AUTHENTICATION_PROPERTY, Boolean.TRUE);
			addEnvProperty(env, CMQC.USER_ID_PROPERTY, configuredUser);
			addEnvProperty(env, CMQC.PASSWORD_PROPERTY, resolvedPassword);
			// Also mirror onto the WMQ JMS constants to support any layers that inspect JMS properties.
			addEnvProperty(env, WMQConstants.USERID, configuredUser);
			addEnvProperty(env, WMQConstants.PASSWORD, resolvedPassword);
			addEnvProperty(env, WMQConstants.USER_AUTHENTICATION_MQCSP, Boolean.TRUE);
			String osUser = System.getProperty("user.name");
			logger.info("Queue manager {} configured for MQCSP authentication with user {} (password length: {}, jvm user: {})",
					queueManager.getName(), configuredUser, resolvedPassword.length(), osUser);
		} else if (hasUser ^ hasPassword) {
			logger.warn("Username/password mismatch for queue manager {}. Credentials will not be sent; ensure channel MCAUSER is configured.",
				queueManager.getName());
		} else {
			logger.info("No credentials configured for queue manager {}. Connection will use channel MCAUSER.", queueManager.getName());
		}
	}
}
