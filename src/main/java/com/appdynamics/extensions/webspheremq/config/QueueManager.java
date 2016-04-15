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
	private String cipherSuite;
	private String cipherSpec;

	private ResourceFilters queueFilters;

	private ResourceFilters channelFilters;

	
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
		return channelFilters;
	}

	public void setChannelFilters(ResourceFilters channelFilters) {
		this.channelFilters = channelFilters;
	}
}
