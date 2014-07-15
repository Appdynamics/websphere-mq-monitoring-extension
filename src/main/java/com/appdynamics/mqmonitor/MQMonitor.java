/**
 * 
 */
package com.appdynamics.mqmonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.appdynamics.mqmonitor.common.JavaServersMonitor;
import com.appdynamics.mqmonitor.common.StringUtils;
import com.appdynamics.mqmonitor.queue.Channel;
import com.appdynamics.mqmonitor.queue.Queue;
import com.appdynamics.mqmonitor.queue.QueueHelper;
import com.appdynamics.mqmonitor.queue.QueueManager;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.pcf.CMQCFC;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

/**
 * @author James Schneider
 *
 */
public class MQMonitor extends JavaServersMonitor {

	protected static final String ROOT_CATEGORY = "root_category_name";
	protected static final String WRITE_STATS_DIR = "write_stats_directory";
	protected static final String QMGR_NUMBER = "number_of_queue_managers";
	protected static final String QMGR_PFX = "queue_mgr_";
	protected static final String QUEUE_PFX = "_queue_";
	protected static final String QMGR_QUEUES = "_number_of_queues";
	
	protected static final String QMGR_HOST_PFX = "queue_mgr_host_";
	protected static final String QMGR_PORT_PFX = "queue_mgr_port_";
	protected static final String QMGR_NAME_PFX = "queue_mgr_name_";
	protected static final String QMGR_CHANNEL_NAME_PFX = "queue_mgr_channel_name_";
	protected static final String QMGR_TRANSPORT_TYPE_PFX = "queue_mgr_transport_type_";
	protected static final String QMGR_USER_PFX = "queue_mgr_user_";
	protected static final String QMGR_PASSWORD_PFX = "queue_mgr_password_";
	
	protected List<QueueManager> queueManagers;
	protected String rootCategoryName = "Backends";
	protected String writeStatsDirectory = "";
	
	
	/**
	 * 
	 */
	public MQMonitor() throws ClassNotFoundException {
		Class.forName("com.ibm.mq.MQEnvironment");
		Class.forName("com.ibm.mq.MQException");
		Class.forName("com.ibm.mq.MQQueue");
		Class.forName("com.ibm.mq.MQQueueManager");
		Class.forName("com.ibm.mq.constants.CMQC");
	}

	

	protected void printQueueStats() throws TaskExecutionException {
		
		try {
			
			List<Queue> queues;
		
			for (QueueManager mgr : this.queueManagers) {
				queues = mgr.getQueues();
				for (Queue q : queues) {
					
				    printMetric(this.rootCategoryName + "|Websphere MQ|" + mgr.getManagerName() + "|" + q.getQueueName() + "|Max Queue Depth", q.getMaximumDepth() + "",
					        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
					        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

				    printMetric(this.rootCategoryName + "|Websphere MQ|" + mgr.getManagerName() + "|" + q.getQueueName() + "|Current Queue Depth", q.getCurrentDepth() + "",
					        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
					        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				}
			}
			
		} catch (Throwable ex) {
			throw new TaskExecutionException(ex);
		}
				
	} 

	private void printAndPopulateChennalStatus(){
		for(QueueManager queueManager: this.queueManagers){
			MQQueueManager mqQueueManager = queueManager.getQueueManager();
			try {
				PCFMessageAgent agent = new PCFMessageAgent();
				agent.connect(mqQueueManager);
				if(agent != null){
					List<Channel> channels = loadPCFAgentStats(agent);
					for (Channel channel : channels) {
						printChannelMetrics(channel, queueManager);
					}
				}
			} catch (MQException e) {
				logger.error("Issues while getting PCF Message Agent", e);
			}
			
		}
	}
	
	private void printChannelMetrics(Channel channel, QueueManager queueManager) {
		if(logger.isDebugEnabled()){
			logger.debug("Started printing channel metrics...");
		}
		
		printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|Channels|" + channel.getChannelName() + "|Messages", channel.getMessages() + "",
		        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
		        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|Channels|" + channel.getChannelName() + "|Status", channel.getChennalStatus() + "",
		        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
		        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|Channels|" + channel.getChannelName() + "|Byte Sent", channel.getByteSent() + "",
		        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
		        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|Channels|" + channel.getChannelName() + "|Byte Received", channel.getByteReceived() + "",
		        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
		        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|Channels|" + channel.getChannelName() + "|Buffers Sent", channel.getBuffersSent() + "",
		        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
		        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|Channels|" + channel.getChannelName() + "|Buffers Sent", channel.getBuffersReceived() + "",
		        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
		        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		if(logger.isDebugEnabled()){
			logger.debug("Fininshed printing channel metrics...");
		}
	}



	private List<Channel> loadPCFAgentStats(PCFMessageAgent agent) {
		List<Channel> channels = new ArrayList<Channel>();
		PCFMessage request;
		PCFMessage[] response;
		int[] attrs = {
					CMQCFC.MQCACH_CHANNEL_NAME,
					CMQCFC.MQCACH_CONNECTION_NAME,
					CMQCFC.MQIACH_CHANNEL_STATUS,
					CMQCFC.MQIACH_MSGS,
					CMQCFC.MQIACH_BYTES_SENT,
					CMQCFC.MQIACH_BYTES_RECEIVED,
					CMQCFC.MQIACH_BUFFERS_SENT,
					CMQCFC.MQIACH_BUFFERS_RECEIVED
					};
		request  = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS);
		request.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "*");
		request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_TYPE, CMQC.MQOT_CURRENT_CHANNEL);
		request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_ATTRS, attrs);
		try {
			response = agent.send(request);
			for (int i = 0; i < response.length; i++) {
				Channel channel = new Channel();
				channel.setChannelName(response[i].getStringParameterValue(CMQCFC.MQCACH_CHANNEL_NAME));
				channel.setChennalStatus(response[i].getIntParameterValue(CMQCFC.MQIACH_CHANNEL_STATUS));
				channel.setMessages(response[i].getIntParameterValue(CMQCFC.MQIACH_MSGS));
				channel.setByteSent(response[i].getIntParameterValue(CMQCFC.MQIACH_BYTES_SENT));
				channel.setByteReceived(response[i].getIntParameterValue(CMQCFC.MQIACH_BYTES_RECEIVED));
				channel.setBuffersSent(response[i].getIntParameterValue(CMQCFC.MQIACH_BUFFERS_SENT));
				channel.setBuffersReceived(response[i].getIntParameterValue(CMQCFC.MQIACH_BUFFERS_RECEIVED));
				channels.add(channel);
			}
		} catch (PCFException e) {
			logger.error("PCFException",e);
		} catch (MQException e) {
			logger.error("MQException",e);
		} catch (IOException e) {
			logger.error("IOException",e);
		}
		return channels;
	}



	protected void writeQueueStats() throws TaskExecutionException {
		
		String filePath = "";
		
		try {
			
			List<Queue> queues;
			StringBuffer buff;
			
			for (QueueManager mgr : this.queueManagers) {
				
				queues = mgr.getQueues();
				for (Queue q : queues) {
					buff = new StringBuffer();
					
					buff.append("CURRENT_QUEUE_DEPTH=" + q.getCurrentDepth());
					buff.append(StringUtils.getNewLine());
					buff.append("MAX_QUEUE_DEPTH=" + q.getMaximumDepth());
					buff.append(StringUtils.getNewLine());
					
					filePath = this.writeStatsDirectory + "\\" + mgr.getManagerName() + "_" + q.getQueueName() + ".txt";
					
					StringUtils.saveStringAsFile(filePath, buff.toString());
					
					
				}
			}
			
		} catch (Throwable ex) {
			logger.warn("Unexpected error writing file : " + filePath, ex);
		}
				
	} 
	
	protected void disconnectFromQueues() throws TaskExecutionException {
		
		try {
		
			for (QueueManager mgr : this.queueManagers) {
				
				mgr.getQueueManager().disconnect();
				
			}
			
			this.queueManagers = null;
			
			
		} catch (MQException ex) {
			throw new TaskExecutionException(ex);
		}
		
		
	} 
	
	protected void populateQueueStats() throws TaskExecutionException {
		
		try {
			
			List<Queue> queues;
		
			for (QueueManager mgr : this.queueManagers) {
				
				queues = mgr.getQueues();
				for (Queue q : queues) {
					
					QueueHelper.populateQueueStats(q);
					
				}
			}
			
		} catch (MQException ex) {
			throw new TaskExecutionException(ex);
		}
		
		
	} 
	
	protected void connectToQueues() throws TaskExecutionException {
		
		try {
			
			MQQueueManager qMgr;
			List<Queue> queues;
		
			for (QueueManager mgr : this.queueManagers) {
				
				qMgr = QueueHelper.getQueueManager(mgr.getManagerName(), mgr.getHost(), mgr.getPort(), mgr.getChannelName(), mgr.getUserId(), mgr.getPassword());
				mgr.setQueueManager(qMgr);
				
				queues = mgr.getQueues();
				for (Queue q : queues) {
					
					q.setMQ(QueueHelper.getQueue(qMgr, q.getQueueName()));
					
				}
			}
			
		} catch (MQException ex) {
			throw new TaskExecutionException(ex);
		}
		
		
	} 
	
	
	protected void parseArgs(Map<String, String> taskArguments) throws TaskExecutionException {
		
		try {
			
			queueManagers = new ArrayList<QueueManager>();
			
			this.rootCategoryName = taskArguments.get(ROOT_CATEGORY);
			this.writeStatsDirectory = taskArguments.get(WRITE_STATS_DIR);
			String numMgrs = taskArguments.get(QMGR_NUMBER);
			int numberMgrs = Integer.parseInt(numMgrs);
			numberMgrs++;

			QueueManager qMgr;
			List<Queue> queueStats;
			
			for (int mgrs = 1; mgrs < numberMgrs; mgrs++) {
				
				qMgr = new QueueManager();
				qMgr.setHost(taskArguments.get(QMGR_HOST_PFX + mgrs));
				qMgr.setPort(Integer.parseInt(taskArguments.get(QMGR_PORT_PFX + mgrs)));
				qMgr.setManagerName(taskArguments.get(QMGR_NAME_PFX + mgrs));
				qMgr.setChannelName(taskArguments.get(QMGR_CHANNEL_NAME_PFX + mgrs));
				qMgr.setTransportType(taskArguments.get(QMGR_TRANSPORT_TYPE_PFX + mgrs));
				qMgr.setUserId(taskArguments.get(QMGR_USER_PFX + mgrs));
				qMgr.setPassword(taskArguments.get(QMGR_PASSWORD_PFX + mgrs));
				
				int numOfQueues = Integer.parseInt(taskArguments.get(QMGR_PFX + mgrs + QMGR_QUEUES));
				numOfQueues++;
				queueStats = new ArrayList<Queue>();				
				Queue qStats;
				
				for (int qs = 1; qs < numOfQueues; qs++) {
					
					qStats = new Queue();
					qStats.setQueueManager(qMgr);
					qStats.setQueueName(taskArguments.get(QMGR_PFX + mgrs + QUEUE_PFX + qs));
					queueStats.add(qStats);
					
				}
				
				qMgr.setQueues(queueStats);
				queueManagers.add(qMgr);
			
			}
			
			
		} catch (Throwable ex) {
			throw new TaskExecutionException(ex);
		}
		
	}
	
	@Override
	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext) throws TaskExecutionException {

		this.parseArgs(taskArguments);
		this.connectToQueues();
		this.populateQueueStats();
		this.printQueueStats();
		this.printAndPopulateChennalStatus();
		//this.writeQueueStats();
		this.disconnectFromQueues();
		return new TaskOutput("Success");
	
	}
	
	protected void printMetric(String name, String value, String aggType, String timeRollup, String clusterRollup)
	{
		String metricName = getMetricPrefix() + name;
		MetricWriter metricWriter = getMetricWriter(metricName, aggType, timeRollup, clusterRollup);
		metricWriter.printMetric(value);

		// just for debug output
		//if (logger.isDebugEnabled())
		//{
			logger.info("METRIC:  NAME:" + metricName + " VALUE:" + value + " :" + aggType + ":" + timeRollup + ":"
					+ clusterRollup);
		//}
		
	}

	protected String getMetricPrefix()
	{
		return "";
	}

	
	
}
