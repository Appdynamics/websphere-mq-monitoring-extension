/**
 * 
 */
package com.appdynamics.mqmonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.mqmonitor.common.JavaServersMonitor;
import com.appdynamics.mqmonitor.common.StringUtils;
import com.appdynamics.mqmonitor.queue.Channel;
import com.appdynamics.mqmonitor.queue.Queue;
import com.appdynamics.mqmonitor.queue.QueueFilter;
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

	protected static final String QUEUE_FILTER_TYPE = "queue_filter_type";
	protected static final String QUEUE_FILTER_VALUE = "queue_filter_value";

	protected static final String QUEUE_FILTER_EXCLUDE_INTERNAL_QUEUES = "queue_filter_exclude_internal_queues";


	protected static final String QMGR_PASSWORD_ENCRYPTED_PFX= "queue_mgr_password_encrypted_";
	public static final String PASSWORD_ENCRYPTED = "password-encrypted";
	public static final String ENCRYPTION_KEY = "encryption-key";

	protected List<QueueManager> queueManagers;
	protected String rootCategoryName = "Backends";
	protected String writeStatsDirectory = "";


	/**
	 * 
	 */
	public MQMonitor() throws ClassNotFoundException {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);

		Class.forName("com.ibm.mq.MQEnvironment");
		Class.forName("com.ibm.mq.MQException");
		Class.forName("com.ibm.mq.MQQueue");
		Class.forName("com.ibm.mq.MQQueueManager");
		Class.forName("com.ibm.mq.constants.CMQC");
	}

	public enum Filters{
		CONTAINS, STARTSWITH, EQUALS, ENDSWITH, NONE
	}

	private void printAndPopulateStats() throws TaskExecutionException{
		for(QueueManager queueManager: this.queueManagers){
			MQQueueManager mqQueueManager = queueManager.getQueueManager();
			PCFMessageAgent agent = new PCFMessageAgent();
			try {
				agent.connect(mqQueueManager);
				if(agent != null){
					try{
						printQueueManagerStatus(agent, queueManager);
					}catch(Exception e){
						logger.error("Error while getting queue manager status",e);
						throw new TaskExecutionException(e);
					}

					try{
						for (QueueManager mgr : this.queueManagers) {
							List<Queue> queues = mgr.getQueues();
							for (Queue q : queues) {
								printQueueStats(agent, queueManager, q.getQueueName());
							}
						}
					}catch(Exception e){
						logger.error("Error while populating queue metrics",e);
						throw new TaskExecutionException(e);
					}
					try{
						List<Channel> channels = loadPCFAgentStats(agent);
						for (Channel channel : channels) {
							printChannelMetrics(channel, queueManager);
						}
					}catch(Exception e){
						logger.error("Error while populating channel metrics", e);
						throw new TaskExecutionException(e);
					}
				}
			} catch (MQException e) {
				logger.error("Issues while getting PCF Message Agent", e);
				throw new TaskExecutionException(e);
				
			}
		}
	}

	private void printQueueManagerStatus(PCFMessageAgent agent,
			QueueManager queueManager) throws TaskExecutionException {
		PCFMessage request;
		PCFMessage[] responses;

		request = new PCFMessage(161);
		request.addParameter(1229, new int [] { CMQCFC.MQIACF_ALL });
		System.out.println("Sending PCF request... " + agent.getQManagerName());
		try {
			responses = agent.send(request);

			System.out.println("Received reply.");

			for(int i=0; i<responses.length; i++){
				int queueManagerStatus = responses[i].getIntParameterValue(CMQCFC.MQIACF_Q_MGR_STATUS);
				if (logger.isDebugEnabled()){
					logger.debug("Queue Manager Status" + queueManagerStatus);
				}
				printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "| Status", queueManagerStatus+"",
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

			}
		}catch (Exception e) {
			throw new TaskExecutionException(e);
		}
	}

	private void printQueueStats(PCFMessageAgent agent,QueueManager queueManager, String queueName) throws TaskExecutionException {
		int[] attrs = { CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH, CMQC.MQIA_MAX_Q_DEPTH, 
				CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT };
		PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
		request.addParameter(CMQC.MQCA_Q_NAME, queueName);
		request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
		request.addParameter(CMQCFC.MQIACF_Q_ATTRS, attrs);
		PCFMessage[] responses;

		try {
			responses = agent.send(request);

			for (int i = 0; i < responses.length; i++) {
				String name = responses[i].getStringParameterValue(CMQC.MQCA_Q_NAME);
				int currrentQDepth = responses[i].getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH);
				int maxQDepth = responses[i].getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH);
				int openInputCount = responses[i].getIntParameterValue(CMQC.MQIA_OPEN_INPUT_COUNT);
				int openOutputCount = responses[i].getIntParameterValue(CMQC.MQIA_OPEN_OUTPUT_COUNT);
				if (logger.isDebugEnabled()){
					logger.debug("Queue " + name + " Current Depth " + currrentQDepth+" max Q Depth "+maxQDepth+
							" Open Input Count "+openInputCount+" Open output count"+openOutputCount);
				}
				printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|" + queueName + "|Max Queue Depth", maxQDepth + "",
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|" + queueName + "|Current Queue Depth", currrentQDepth + "",
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|" + queueName + "|Open Input Count", openInputCount + "",
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
				printMetric(this.rootCategoryName + "|Websphere MQ|" + queueManager.getManagerName() + "|" + queueName + "|Open Output Count", openOutputCount + "",
						MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
						MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
			}
		}
		catch (PCFException pcfe) {
			logger.error("PCFException caught Queue"+queueName, pcfe);
			PCFMessage[] msgs = (PCFMessage[]) pcfe.exceptionSource;
			for (int i = 0; i < msgs.length; i++) {
				logger.error(msgs[i]);
			}
			throw new TaskExecutionException(pcfe);
		}
		catch (Exception mqe) {
			logger.error("MQException caught", mqe);
			throw new TaskExecutionException(mqe);
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



	private List<Channel> loadPCFAgentStats(PCFMessageAgent agent) throws TaskExecutionException {
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
		} catch (Exception e) {
			throw new TaskExecutionException(e);
		}
		return channels;
	}



	private List<String> loadPCFAgentQueue(MQQueueManager qMgr) throws TaskExecutionException {
		List<String> queueNames = new ArrayList<String>();
		PCFMessageAgent agent = new PCFMessageAgent();
		try {
			agent.connect(qMgr);
			PCFMessage   request = new PCFMessage (CMQCFC.MQCMD_INQUIRE_Q_NAMES);
			request.addParameter (CMQC.MQCA_Q_NAME, "*");
			//request.addParameter (CMQC.MQIA_Q_TYPE, MQC.MQQT_LOCAL);//Only for local queues
			PCFMessage []   responses = agent.send (request);
			String []   names = (String []) responses [0].getParameterValue (CMQCFC.MQCACF_Q_NAMES);
			for (int i = 0; i < names.length; i++){
				logger.debug("Queue: " + names [i]);
				queueNames.add(names [i]);
			}
		}
		catch (PCFException pcfe){
			logger.error("PCF error: " + pcfe);
			throw new TaskExecutionException(pcfe);
		}catch (Exception mqe){
			throw new TaskExecutionException(mqe);
		}
		return queueNames;
	}

	private boolean isMonitoringEnabled(QueueFilter queueFilter, String queueName) {
		boolean monitorEnabled = false;
		String filterQueueValue = queueFilter.getQueueFilterValue();
		String filterQueueType = queueFilter.getQueueFilterType();
		if(filterQueueType == null || filterQueueValue == null || queueName == null){
			logger.info("Filter queue name: "+ queueName+" filter type: "+filterQueueType+ " filter value: "+filterQueueValue);
			return monitorEnabled;
		}
		String excludePattern = queueFilter.getExcludeInternalQueuesPattern();
		if(queueName.startsWith(excludePattern)){
			if(logger.isDebugEnabled()){// temp for debugging
			logger.debug("Queue: "+queueName+" Monitoring is: "+monitorEnabled);
			}
			return monitorEnabled;
		}
		
		//handle comma separated values
		String[] filterValues =  filterQueueValue.split(",");
		for (int i = 0; i < filterValues.length; i++) {
			monitorEnabled = checkQueueNameFilter(filterQueueType, filterValues[i], queueName);
		}
		return monitorEnabled;
	}

	private boolean checkQueueNameFilter(String filterQueueType, String filterQueueValue,
			String queueName) {
		boolean monitorEnabled = false;
		switch(Filters.valueOf(filterQueueType)){
		case CONTAINS:
			if(queueName.contains(filterQueueValue))
				monitorEnabled = true;
			break;
		case STARTSWITH:	
			if(queueName.startsWith(filterQueueValue))
				monitorEnabled = true;
			break;
		case NONE:	
			monitorEnabled = true;
			break;
		case EQUALS:	
			if(queueName.equals(filterQueueValue))
				monitorEnabled = true;
			break;
		case ENDSWITH:	
			if(queueName.endsWith(filterQueueValue))
				monitorEnabled = true;
			break;
		}
		if(logger.isDebugEnabled()){
			logger.debug("Queue: "+queueName+" Monitoring is: "+monitorEnabled);
		}
		return monitorEnabled;
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

	protected void connectToQueues() throws TaskExecutionException {
		try {
			MQQueueManager qMgr;
			List<Queue> queues = new ArrayList<Queue>();

			for (QueueManager mgr : this.queueManagers) {

				qMgr = QueueHelper.getQueueManager(mgr.getManagerName(), mgr.getHost(), mgr.getPort(), mgr.getChannelName(), mgr.getUserId(), mgr.getPassword());
				mgr.setQueueManager(qMgr);

				List<String> queueNameList = new ArrayList<String>();

				String queueFilterType = mgr.getQueueFilter().getQueueFilterType();
				if(queueFilterType != null){
					if("EQUALS".equalsIgnoreCase(queueFilterType)){
						String filterValue = mgr.getQueueFilter().getQueueFilterValue();
						if(filterValue != null){
							logger.info("Queues for monitoring: "+filterValue);
							String[] queueName = filterValue.split(",");
							for (int i = 0; i < queueName.length; i++) {
								queueNameList.add(queueName[i]);
								for (String qName : queueNameList) {
									Queue queue = new Queue();
									queue.setQueueName(qName);
									queue.setQueueManager(mgr);
									queue.setMQ(QueueHelper.getQueue(qMgr, qName));
									queues.add(queue);
								}
							}
						}
					}else{
						queueNameList = loadPCFAgentQueue(qMgr);
						for (String qName : queueNameList) {
							if(isMonitoringEnabled(mgr.getQueueFilter(),qName)){
								Queue queue = new Queue();
								queue.setQueueName(qName);
								queue.setQueueManager(mgr);
								queue.setMQ(QueueHelper.getQueue(qMgr, qName));
								queues.add(queue);
							}
						}
					}
				}
				mgr.setQueues(queues);
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

			for (int mgrs = 1; mgrs < numberMgrs; mgrs++) {

				qMgr = new QueueManager();
				qMgr.setHost(taskArguments.get(QMGR_HOST_PFX + mgrs));
				qMgr.setPort(Integer.parseInt(taskArguments.get(QMGR_PORT_PFX + mgrs)));
				qMgr.setManagerName(taskArguments.get(QMGR_NAME_PFX + mgrs));
				qMgr.setChannelName(taskArguments.get(QMGR_CHANNEL_NAME_PFX + mgrs));
				qMgr.setTransportType(taskArguments.get(QMGR_TRANSPORT_TYPE_PFX + mgrs));
				qMgr.setUserId(taskArguments.get(QMGR_USER_PFX + mgrs));

				qMgr.setPassword(getPassword(taskArguments, mgrs));

				QueueFilter queueFilter = new QueueFilter();
				queueFilter.setQueueFilterType(taskArguments.get(QUEUE_FILTER_TYPE));
				queueFilter.setQueueFilterValue(taskArguments.get(QUEUE_FILTER_VALUE));
				queueFilter.setExcludeInternalQueuesPattern(taskArguments.get(QUEUE_FILTER_EXCLUDE_INTERNAL_QUEUES));
				qMgr.setQueueFilter(queueFilter);

				queueManagers.add(qMgr);

			}


		} catch (Throwable ex) {
			throw new TaskExecutionException(ex);
		}

	}

	private String getPassword(Map<String,String> taskArguments,int mgr){
		if(taskArguments.get(QMGR_PASSWORD_PFX + mgr) != null){
			return taskArguments.get(QMGR_PASSWORD_PFX + mgr);
		}
		else if(taskArguments.containsKey(QMGR_PASSWORD_ENCRYPTED_PFX + mgr)){
			Map<String,String> argsForDecryption = new HashMap<String, String>();
			argsForDecryption.put(PASSWORD_ENCRYPTED,taskArguments.get(QMGR_PASSWORD_ENCRYPTED_PFX + mgr));
			argsForDecryption.put(ENCRYPTION_KEY,taskArguments.get(ENCRYPTION_KEY));
			return CryptoUtil.getPassword(argsForDecryption);
		}
		return "";
	}

	@Override
	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext) throws TaskExecutionException {

		this.parseArgs(taskArguments);
		try{
			this.connectToQueues();
			this.printAndPopulateStats();
		}catch(TaskExecutionException tee){
			this.disconnectFromQueues();
			throw new TaskExecutionException(tee);
		}
		this.disconnectFromQueues();
		return new TaskOutput("Success");

	}

	protected void printMetric(String name, String value, String aggType, String timeRollup, String clusterRollup)
	{
		String metricName = getMetricPrefix() + name;
		MetricWriter metricWriter = getMetricWriter(metricName, aggType, timeRollup, clusterRollup);
		metricWriter.printMetric(value);
		logger.info("METRIC:  NAME:" + metricName + " VALUE:" + value + " :" + aggType + ":" + timeRollup + ":"
				+ clusterRollup);
	}

	protected String getMetricPrefix()
	{
		return "";
	}

	public static String getImplementationVersion() {
		return MQMonitor.class.getPackage().getImplementationTitle();
	}

}
