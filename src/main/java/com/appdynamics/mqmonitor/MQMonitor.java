/**
 * 
 */
package com.appdynamics.monitors.mqmonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.appdynamics.monitors.mqmonitor.common.JavaServersMonitor;
import com.appdynamics.monitors.mqmonitor.common.StringUtils;
import com.appdynamics.monitors.mqmonitor.queue.QueueManager;
import com.appdynamics.monitors.mqmonitor.queue.QueueHelper;
import com.appdynamics.monitors.mqmonitor.queue.Queue;

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
