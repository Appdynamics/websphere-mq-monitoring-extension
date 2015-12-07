package com.appdynamics.extensions.webspheremq;

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.StringUtils;
import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.ibm.mq.MQC;

/**
 * Takes care of websphere mq connection, authentication, SSL, Cipher spec, certificate based authorization.<br>
 * It also validates the arguments passed for various scenarios.
 * 
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class WMQauthenticationAndAuthorization {

	public static final Logger logger = LoggerFactory.getLogger(WMQauthenticationAndAuthorization.class);
	private QueueManager queueManager;

	public WMQauthenticationAndAuthorization(QueueManager queueManager) {
		this.queueManager = queueManager;
		validateArgs();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Hashtable getMQEnvironment() {
		Hashtable env = new Hashtable();
		addEnvProperty(env, MQC.HOST_NAME_PROPERTY, queueManager.getHost());
		addEnvProperty(env, MQC.PORT_PROPERTY, queueManager.getPort());
		addEnvProperty(env, MQC.CHANNEL_PROPERTY, queueManager.getChannelName());
		addEnvProperty(env, MQC.USER_ID_PROPERTY, queueManager.getUsername());
		addEnvProperty(env, MQC.PASSWORD_PROPERTY, queueManager.getPassword());
		addEnvProperty(env, MQC.SSL_CERT_STORE_PROPERTY, queueManager.getSslKeyRepository());
		addEnvProperty(env, MQC.SSL_CIPHER_SUITE_PROPERTY, queueManager.getCipherSuite());
		//TODO: investigate on CIPHER_SPEC property No Available in MQ 7.5 Jar

		if (Constants.TRANSPORT_TYPE_CLIENT.equalsIgnoreCase(queueManager.getTransportType()))
			addEnvProperty(env, MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES_CLIENT);
		else if (Constants.TRANSPORT_TYPE_BINGINGS.equalsIgnoreCase(queueManager.getTransportType()))
			addEnvProperty(env, MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES_BINDINGS);
		else
			addEnvProperty(env, MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES);

		logger.debug("Transport property is " + env.get(MQC.TRANSPORT_PROPERTY));
		return env;
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void addEnvProperty(Hashtable env, String propName, Object propVal) {
		if (null != propVal) {
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
			if (Constants.TRANSPORT_TYPE_BINGINGS.equalsIgnoreCase(queueManager.getTransportType())) {
				if (!StringUtils.hasText(queueManager.getName())) {
					validArgs = false;
					errorMsg.append("queuemanager cannot be null or empty for bindings type connection. ");
				}
			}
		}

		if (!validArgs) {
			throw new IllegalArgumentException(errorMsg.toString());
		}
	}
}
