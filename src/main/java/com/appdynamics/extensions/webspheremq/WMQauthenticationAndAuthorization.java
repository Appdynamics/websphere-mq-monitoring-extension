package com.appdynamics.extensions.webspheremq;

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.webspheremq.common.Constants;
import com.appdynamics.extensions.webspheremq.config.QueueManager;
import com.ibm.mq.MQC;

/**
 * Takes care of websphere mq connection, authentication, SSL, Cipher spec, certificate based authorization
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
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Hashtable getMQEnvironment() {
		Hashtable env = new Hashtable();
		addEnvProperty(env, MQC.HOST_NAME_PROPERTY, queueManager.getHost());
		addEnvProperty(env, MQC.PORT_PROPERTY, queueManager.getPort());
		addEnvProperty(env, MQC.CHANNEL_PROPERTY, queueManager.getChannelName());
		addEnvProperty(env, MQC.USER_ID_PROPERTY, queueManager.getUsername());
		addEnvProperty(env, MQC.PASSWORD_PROPERTY, queueManager.getPassword());

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

}
