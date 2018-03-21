/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.webspheremq.config;

import java.util.List;

/**
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class QueueManager {
	
	private String host;
	private int port = -1;
	private String name;
	private String channelName;
	private String transportType;
	private String username;
	private String password;
	private String sslKeyRepository;
	private int ccsid = Integer.MIN_VALUE;
	private int encoding = Integer.MIN_VALUE;
	private String cipherSuite;
	private String cipherSpec;
	private String encryptedPassword;
	private String encryptionKey;
	private String replyQueuePrefix;
	private String modelQueueName;

	private ResourceFilters queueFilters;

	private ResourceFilters channelFilters;

	private ResourceFilters listenerFilters;

	private ResourceFilters topicFilters;

	
	List<String> writeStatsDirectory;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getTransportType() {
		return transportType;
	}

	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	public ResourceFilters getQueueFilters() {
		if(queueFilters == null){
			return new ResourceFilters();
		}
		return queueFilters;
	}

	public void setQueueFilters(ResourceFilters queueFilters) {
		this.queueFilters = queueFilters;
	}


	public List<String> getWriteStatsDirectory() {
		return writeStatsDirectory;
	}

	public void setWriteStatsDirectory(List<String> writeStatsDirectory) {
		this.writeStatsDirectory = writeStatsDirectory;
	}


	public String getSslKeyRepository() {
		return sslKeyRepository;
	}

	public void setSslKeyRepository(String sslKeyRepository) {
		this.sslKeyRepository = sslKeyRepository;
	}

	public String getCipherSuite() {
		return cipherSuite;
	}

	public void setCipherSuite(String cipherSuite) {
		this.cipherSuite = cipherSuite;
	}

	public String getCipherSpec() {
		return cipherSpec;
	}

	public void setCipherSpec(String cipherSpec) {
		this.cipherSpec = cipherSpec;
	}

	public ResourceFilters getChannelFilters() {
		if(channelFilters == null){
			return new ResourceFilters();
		}
		return channelFilters;
	}

	public void setChannelFilters(ResourceFilters channelFilters) {
		this.channelFilters = channelFilters;
	}

	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public String getReplyQueuePrefix() {
		return replyQueuePrefix;
	}

	public void setReplyQueuePrefix(String replyQueuePrefix) {
		this.replyQueuePrefix = replyQueuePrefix;
	}

	public String getModelQueueName() {
		return modelQueueName;
	}

	public void setModelQueueName(String modelQueueName) {
		this.modelQueueName = modelQueueName;
	}

	public ResourceFilters getListenerFilters() {
		if(listenerFilters == null){
			return new ResourceFilters();
		}
		return listenerFilters;
	}

	public void setListenerFilters(ResourceFilters listenerFilters) {
		this.listenerFilters = listenerFilters;
	}

	public int getCcsid() {
		return ccsid;
	}

	public void setCcsid(int ccsid) {
		this.ccsid = ccsid;
	}

	public int getEncoding() {
		return encoding;
	}

	public void setEncoding(int encoding) {
		this.encoding = encoding;
	}

	public ResourceFilters getTopicFilters() {
		if(topicFilters == null){
			return new ResourceFilters();
		}
		return topicFilters;
	}

	public void setTopicFilters(ResourceFilters topicFilters) {
		this.topicFilters = topicFilters;
	}
}
