package com.appdynamics.mqmonitor.queue;

/**
 * 
 * @author Vikash Kumar
 * Flow--->  Computer --> IBM MQ Server ---> IBM MQ Queue Manager----> Channel, Listener, Queue
 */
public class Channel {

	protected String channelName;
	
	protected int chennalStatus;
	
	protected int messages;
	
	protected int byteSent;
	
	protected int byteReceived;
	
	protected int buffersSent;
	
	protected int buffersReceived;

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public int getChennalStatus() {
		return chennalStatus;
	}

	public void setChennalStatus(int chennalStatus) {
		this.chennalStatus = chennalStatus;
	}

	public int getMessages() {
		return messages;
	}

	public void setMessages(int messages) {
		this.messages = messages;
	}

	public int getByteSent() {
		return byteSent;
	}

	public void setByteSent(int byteSent) {
		this.byteSent = byteSent;
	}

	public int getByteReceived() {
		return byteReceived;
	}

	public void setByteReceived(int byteReceived) {
		this.byteReceived = byteReceived;
	}

	public int getBuffersSent() {
		return buffersSent;
	}

	public void setBuffersSent(int buffersSent) {
		this.buffersSent = buffersSent;
	}

	public int getBuffersReceived() {
		return buffersReceived;
	}

	public void setBuffersReceived(int buffersReceived) {
		this.buffersReceived = buffersReceived;
	}
}
