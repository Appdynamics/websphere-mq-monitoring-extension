package com.appdynamics.mqmonitor.queue;

import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;

/**
 * @author James Schneider
 *
 */
public class QueueHelper {

	public QueueHelper() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			
			MQQueueManager queueManager = QueueHelper.getQueueManager("QUEUEMGR01", "127.0.0.1", 1414, "SYSTEM.ADMIN.SVRCONN");
			MQQueue mqQueue;
			Queue queueStats;
			
			
			for (int timesToRun = 0; timesToRun < 1; timesToRun++) {
			
				mqQueue = QueueHelper.getQueue(queueManager, "CREWDATA01");
				queueStats = QueueHelper.getQueueStats(mqQueue);
				System.out.println("CREWDATA01 : Maximum Depth = " + queueStats.getMaximumDepth());
				System.out.println("CREWDATA01 : Current Depth = " + queueStats.getCurrentDepth());				
				System.out.println("");
				System.out.println("");
				
				mqQueue = QueueHelper.getQueue(queueManager, "CREWDATA02");
				queueStats = QueueHelper.getQueueStats(mqQueue);
				System.out.println("CREWDATA02 : Maximum Depth = " + queueStats.getMaximumDepth());
				System.out.println("CREWDATA02 : Current Depth = " + queueStats.getCurrentDepth());				
				System.out.println("");
				System.out.println("");
				
				mqQueue = QueueHelper.getQueue(queueManager, "FLIGHTDATA01");
				queueStats = QueueHelper.getQueueStats(mqQueue);
				System.out.println("FLIGHTDATA01 : Maximum Depth = " + queueStats.getMaximumDepth());
				System.out.println("FLIGHTDATA01 : Current Depth = " + queueStats.getCurrentDepth());				
				System.out.println("");
				System.out.println("");
				
				mqQueue = QueueHelper.getQueue(queueManager, "FLIGHTDATA02");
				queueStats = QueueHelper.getQueueStats(mqQueue);
				System.out.println("FLIGHTDATA02 : Maximum Depth = " + queueStats.getMaximumDepth());
				System.out.println("FLIGHTDATA02 : Current Depth = " + queueStats.getCurrentDepth());				
				System.out.println("");
				System.out.println("");
				
				
				Thread.sleep(20000);
				
			}
			
			queueManager.disconnect();
			
		} catch (Throwable ex) {
			ex.printStackTrace();
		}

	}
	
	public static void populateQueueStats(Queue queue) throws MQException {
		
		queue.setCurrentDepth(queue.getMQ().getCurrentDepth());
		queue.setMaximumDepth(queue.getMQ().getMaximumDepth());
		
	}
	
	public static Queue getQueueStats(MQQueue mqQueue) throws MQException {
		
		Queue stats = new Queue();
		stats.setCurrentDepth(mqQueue.getCurrentDepth());
		stats.setMaximumDepth(mqQueue.getMaximumDepth());
		
		return stats;
		
	}
	
	
	public static MQQueueManager getQueueManager(String queueManagerName, String queueManagerHost, int queueManagerPort, String queueManagerChannel) throws MQException {
	
		return QueueHelper.getQueueManager(queueManagerName, queueManagerHost, queueManagerPort, queueManagerChannel, null, null);
		
	}

	public static MQQueueManager getQueueManager(String queueManagerName, String queueManagerHost, int queueManagerPort, String queueManagerChannel, String userId, String password) throws MQException {
		
		MQEnvironment.hostname = queueManagerHost;
		MQEnvironment.port = queueManagerPort;
		MQEnvironment.channel = queueManagerChannel;
		
		if (userId != null && password != null) {
			if (!userId.equals("") && !password.equals("")) {
				MQEnvironment.userID = userId;
				MQEnvironment.password = password;				
			}
		}
		MQQueueManager queueManager = new MQQueueManager(queueManagerName);
		return queueManager;
		
	}
	
	public static MQQueue getQueue(MQQueueManager queueManager, String queueName) throws MQException {
		
		MQQueue mqQueue = queueManager.accessQueue(queueName, CMQC.MQOO_INQUIRE);
		
		//mqQueue.
		
		return mqQueue;
		
	}
	
	
	
}
