package com.appdynamics.extensions.webspheremq.config;

import java.util.List;

/**
 * @author rajeevsingh
 * @version 2.0
 *
 */
public class QueueManager {
	
	private String host;
	private int port;
	private String name;
	private String channelName;
	private int transportType;
	private String username;
	private String password;
	
	private QueueIncludeFilters queueIncludeFilters;
	private QueueExcludeFilters queueExcludeFilters;

	private ChannelIncludeFilters channelIncludeFilters;
	private ChannelExcludeFilters channelExcludeFilters;
	
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

	public int getTransportType() {
		return transportType;
	}

	public void setTransportType(int transportType) {
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

	public QueueIncludeFilters getQueueIncludeFilters() {
		return queueIncludeFilters;
	}

	public void setQueueIncludeFilters(QueueIncludeFilters queueIncludeFilters) {
		this.queueIncludeFilters = queueIncludeFilters;
	}

	public ChannelIncludeFilters getChannelIncludeFilters() {
		return channelIncludeFilters;
	}

	public void setChannelIncludeFilters(ChannelIncludeFilters channelIncludeFilters) {
		this.channelIncludeFilters = channelIncludeFilters;
	}

	public List<String> getWriteStatsDirectory() {
		return writeStatsDirectory;
	}

	public void setWriteStatsDirectory(List<String> writeStatsDirectory) {
		this.writeStatsDirectory = writeStatsDirectory;
	}

	public QueueExcludeFilters getQueueExcludeFilters() {
		return queueExcludeFilters;
	}

	public void setQueueExcludeFilters(QueueExcludeFilters queueExcludeFilters) {
		this.queueExcludeFilters = queueExcludeFilters;
	}

	public ChannelExcludeFilters getChannelExcludeFilters() {
		return channelExcludeFilters;
	}

	public void setChannelExcludeFilters(ChannelExcludeFilters channelExcludeFilters) {
		this.channelExcludeFilters = channelExcludeFilters;
	}
		
}
