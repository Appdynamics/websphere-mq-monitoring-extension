AppDynamics Monitoring Extension for use with IBM WebSphere MQ
==============================================================

Use case
-------------


The WebSphere MQ monitoring extension can monitor multiple queues managers and their resources, namely queues, topics, channels and listeners.

The metrics for queue manager, queue, topic, channel and listener can be configured.

The MQ Monitor currently supports IBM Websphere MQ version 7.x, 8.x and 9.x.

 
Prerequisites
-------------
 
In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Java+Agent) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).  For more details on downloading these products, please  visit https://download.appdynamics.com/.

If this extension is configured for **CLIENT** transport type (more on that later), please make sure the MQ's host and port is accessible. 
 
If this extension is configured for **CLIENT** transport type (more on that later), credentials of user with correct access rights would be needed in config.yaml (more on that later). If the hosting OS for IBM MQ is Windows, Windows user credentials will be needed. 

This extension has been tested on Windows and Unix systems. Customers have successfully gotten it to work on AIX systems as well. 

Dependencies
------------
  
The monitor has a dependency on the following seven JAR files from the IBM MQ distribution:

``` 
com.ibm.mq.commonservices.jar
com.ibm.mq.jar
com.ibm.mq.jmqi.jar
dhbcore.jar
com.ibm.mq.headers.jar
connector.jar
com.ibm.mq.pcf.jar
```

In newer versions of the MQ, IBM has removed **connector.jar** & **dhbcore.jar** and merged its contents in **com.ibm.mq.allclient.jar**.
These jar files are typically found in ```/opt/mqm/java/lib``` on a UNIX server but may be found in an alternate location depending upon your environment. In case **CLIENT** transport type, IBM MQ Client must be installed to get the MQ jars. To download IBM MQ Client jars, see [here](https://www-01.ibm.com/software/integration/wmq/clients/)


Rebuilding the Project
----------------------
1. Clone the repo websphere-mq-monitoring-extension from GitHub https://github.com/Appdynamics
2. Copy the seven MQ jar files listed above into the websphere-mq-monitoring-extension/lib/ directory. Create a lib folder if not already present.
3. Run 'mvn clean install' from the cloned websphere-mq-monitoring-extension directory.
4. The MQMonitor-<version>.zip should get built and found in the 'target' directory.


Installation
------------

1. Unzip contents of WMQMonitor-<version>.zip file and copy to <code><machine-agent-dir>/monitors</code> directory. Please place the extension in the "monitors" directory of your Machine Agent installation directory. Do not place the extension in the "extensions" directory of your Machine Agent installation directory.
2. There are two transport modes in which this extension can be run

  **Binding** : Requires WMQ Extension to be deployed in machine agent on the same machine where WMQ server is installed.  

  **Client** : In this mode, the WMQ extension is installed on a different host than the IBM MQ server. Please install the [IBM MQ Client](https://www-01.ibm.com/software/integration/wmq/clients/) for this mode to get the necessary jars as mentioned previously.
  
   Copy the following jars to the WMQMonitor directory
 	```

	    com.ibm.mq.commonservices.jar
	    com.ibm.mq.jar
	    com.ibm.mq.jmqi.jar
	    dhbcore.jar
	    com.ibm.mq.headers.jar
	    connector.jar
	    com.ibm.mq.pcf.jar

 	```

   As mentioned previously, If you don't find the **connector.jar** & **dhbcore.jar**, please copy the **com.ibm.mq.allclient.jar**. If you are copying the **com.ibm.mq.allclient.jar**, please edit the monitor.xml to replace the **connector.jar** & **dhbcore.jar** entries with  **com.ibm.mq.allclient.jar**

   If you don't want to copy the jar files, you can point to the jar files by providing the absolute paths to the jar files in the  monitor.xml.


3. If you plan to use **Client** transport type, create a channel of type server connection in each of the queue manager you wish to query. 

4. Edit the config.yml file.  An example config.yml file follows these installation instructions.

5. Restart the Machine Agent.
 
Sample config.yaml
------------------
 
The following is a sample config.yaml file that depicts two different queue managers defined. The different fields are explained in the in-line comments. 
 

```

    #For most purposes, no need to change this.
    numberOfThreads: 10

    #This will create this metric in all the tiers, under this path. Please make sure to have a trailing |
    #metricPrefix: Custom Metrics|WebsphereMQ|

    #This will create it in specific Tier. Replace <TIER_ID>. Please make sure to have a trailing |
    metricPrefix: "Server|Component:<TIER_ID>|Custom Metrics|WebsphereMQ|"

    queueManagers:
      - host: "192.168.57.104"
        port: 1414
        #Actual name of the queue manager
        name: "TEST_QM_1"
        #Channel name of the queue manager
        channelName: "SYSTEM.ADMIN.SVRCONN"
        #The transport type for the queue manager connection, the default is "Bindings" for a binding type connection
        #For bindings type connection WMQ extension (i.e machine agent) need to be on the same machine on which WebbsphereMQ server is running
        #for client type connection change it to "Client".
        transportType: "Client"
        #user with admin level access, no need to provide credentials in case of bindings transport type, it is only applicable for client type
        username: "hello"
        password: "hello"

        #This is the timeout on queue metrics threads.Default value is 20 seconds. No need to change the default
        #Unless you know what you are doing.
        queueMetricsCollectionTimeoutInSeconds: 20
        
        queueFilters:
            #An asterisk on its own matches all possible names.
            include: ["*"]
            #exclude all queues that starts with SYSTEM or AMQ.
            exclude:
               - type: "STARTSWITH"
                 values: ["SYSTEM","AMQ"]

        channelFilters:
            #An asterisk on its own matches all possible names.
            include: ["*"]
            #exclude all queues that starts with SYSTEM.
            exclude:
               - type: "STARTSWITH"
                 values: ["SYSTEM"]

      - host: "102.138.37.105"
        port: 1414
        #Actual name of the queue manager
        name: "TEST_QM_2"
        #Channel name of the queue manager
        channelName: "SYSTEM.ADMIN.SVRCONN"
        #The transport type for the queue manager connection, the default is "Bindings" for a binding type connection
        #For bindings type connection WMQ extension (i.e machine agent) need to be on the same machine on which WebbsphereMQ server is running
        #for client type connection change it to "Client".
        transportType: "Client"
        #user with admin level access, no need to provide credentials in case of bindings transport type, it is only applicable for client type
        username: "hello"
        password: "hello"
        #This is the timeout on queue metrics threads.Default value is 20 seconds. No need to change the default
        #Unless you know what you are doing.
        queueMetricsCollectionTimeoutInSeconds: 20

        queueFilters:
            #Matches all queues  that starts with TACA..
            include: ["TACA*"]
            #exclude all queues that starts with SYSTEM or AMQ.
            exclude:
               - type: "STARTSWITH"
                 values: ["SYSTEM","AMQ"]

        channelFilters:
            #An asterisk on its own matches all possible names.
            include: ["*"]
            #exclude all queues that starts with SYSTEM.
            exclude:
               - type: "STARTSWITH"
                 values: ["SYSTEM"]
        
        topicFilters:
                # For topics, IBM MQ uses the topic wildcard characters ('#' and '+') and does not treat a trailing asterisk as a wildcard
                # https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_7.5.0/com.ibm.mq.pla.doc/q005020_.htm
                include: ["#"]
                exclude:
                     #type value: STARTSWITH, EQUALS, ENDSWITH, CONTAINS
                   - type: "STARTSWITH"
                     #The name of the queue or queue name pattern as per queue filter, comma separated values
                     values: ["SYSTEM","$SYS"]

    mqMetrics:
      # This Object will extract queue manager metrics
      - metricsType: "queueMgrMetrics"
        metrics:
          include:
            - Status:
                alias: "Status"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACF_Q_MGR_STATUS"
          
      # This Object will extract queue metrics
      - metricsType: "queueMetrics"
        metrics:
          include:
            - MaxQueueDepth:
                alias: "Max Queue Depth"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_MAX_Q_DEPTH"
                ibmCommand: "MQCMD_INQUIRE_Q"
              
            - CurrentQueueDepth:
                alias: "Current Queue Depth"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_CURRENT_Q_DEPTH"
                ibmCommand: "MQCMD_INQUIRE_Q"
              
            - OpenInputCount:
                alias: "Open Input Count"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_OPEN_INPUT_COUNT"
                ibmCommand: "MQCMD_INQUIRE_Q"
              
            - OpenOutputCount:
                alias: "Open Output Count"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_OPEN_OUTPUT_COUNT"
                ibmCommand: "MQCMD_INQUIRE_Q"
    
            - OldestMsgAge:
                alias: "OldestMsgAge"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACF_OLDEST_MSG_AGE"
                ibmCommand: "MQCMD_INQUIRE_Q_STATUS"
    
            - OnQTime:
                alias: "OnQTime"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACF_Q_TIME_INDICATOR"
                ibmCommand: "MQCMD_INQUIRE_Q_STATUS"
    
            - UncommittedMsgs:
                alias: "UncommittedMsgs"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACF_UNCOMMITTED_MSGS"
                ibmCommand: "MQCMD_INQUIRE_Q_STATUS"
    
            - HighQDepth:
                alias: "HighQDepth"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_HIGH_Q_DEPTH"
                ibmCommand: "MQCMD_RESET_Q_STATS"
    
            - MsgDeqCount:
                alias: "MsgDeqCount"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_MSG_DEQ_COUNT"
                ibmCommand: "MQCMD_RESET_Q_STATS"
    
            - MsgEnqCount:
                alias: "MsgEnqCount"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_MSG_ENQ_COUNT"
                ibmCommand: "MQCMD_RESET_Q_STATS"
    
          
      # This Object will extract channel metrics
      - metricsType: "channelMetrics"
        metrics:
          include:
            - Messages:
                alias: "Messages"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_MSGS"
              
            - Status:
                alias: "Status"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_CHANNEL_STATUS"  #http://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.dev.doc/q090880_.htm
              
            - ByteSent:
                alias: "Byte Sent"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BYTES_SENT"
              
            - ByteReceived:
                alias: "Byte Received"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BYTES_RECEIVED"
              
            - BuffersSent:
                alias: "Buffers Sent"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BUFFERS_SENT"
              
            - BuffersReceived:
                alias: "Buffers Received"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BUFFERS_RECEIVED"
    
    
      - metricsType: "listenerMetrics"
        metrics:
          include:
            - Status:
                alias: "Status"
                ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_LISTENER_STATUS"
    
      # This Object will extract topic metrics
      - metricsType: "topicMetrics"
        metrics:
          include:
            - PublishCount:
                alias: "Publish Count"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_PUB_COUNT"
                ibmCommand: "MQCMD_INQUIRE_TOPIC_STATUS"
            - SubscriptionCount:
                alias: "Subscription Count"
                ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_SUB_COUNT"
                ibmCommand: "MQCMD_INQUIRE_TOPIC_STATUS"


```

Internals
---------
This extension extracts metrics through [PCF framework](https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.adm.doc/q019990_.htm). A complete list of PCF commands are listed [here] (https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.adm.doc/q086870_.htm)
Each queue manager has an administration queue with a standard queue name and the extension sends PCF command messages to that queue. On Windows and Unix platforms, the PCF commands are sent is always sent to the SYSTEM.ADMIN.COMMAND.QUEUE queue. 
More details about that is mentioned [here](https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.adm.doc/q020010_.htm)

By default, the PCF responses are sent to the SYSTEM.DEFAULT.MODEL.QUEUE. Using this queue causes a temporary dynamic queue to be created. You can override the default here by using the `modelQueueName` and `replyQueuePrefix` fields in the config.yaml.
More details mentioned [here](https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.adm.doc/q083240_.htm)



Access Permissions
-------------------
If you are in **Bindings** mode, please make sure to start the MA process under a user which has permissions to inquire,get,put (since PCF responses cause dynamic queues to be created) on the broker. Similarly, for **Client** mode provide the credentials which have enough access permissions.


SSL Support
-----------
Configure the IBM SSL Cipher Suite in the config.yaml. 

Note that, to use some CipherSuites the unrestricted policy needs to be configured in JRE. Please visit [this link] (http://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/sdkpolicyfiles.html
) for more details. For Oracle JRE, please update with [JCE Unlimited Strength Jurisdiction Policy] (http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)

To configure SSL, the MA's trust store and keystore needs to be setup with the JKS filepath. 

Please add the following JVM arguments to the MA start up command or script. 

```-Dcom.ibm.mq.cfg.useIBMCipherMappings=false```  (If you are using IBM Cipher Suites, set the flag to true. Please visit [this link](http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.dev.doc/q113210_.htm) for more details.
)

```-Djavax.net.ssl.trustStore=<PATH_TO_JKS_FILE>```
```-Djavax.net.ssl.trustStorePassword=<PASS>```
```-Djavax.net.ssl.keyStore=<PATH_TO_JKS_FILE>```
```-Djavax.net.ssl.keyStorePassword=<PASS>```

Metrics
--------
The metrics will be reported under the tree ```Application Infrastructure Performance|$TIER|Custom Metrics|WebsphereMQ```

Credentials Encryption
----------------------
Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

Extensions Workbench
--------------------
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

Troubleshooting
---------------

1. Verify Machine Agent Data: Please start the Machine Agent without the extension and make sure that it reports data. Verify that the machine agent status is UP and it is reporting Hardware Metrics.
2. config.yml: Validate the file [here](http://www.yamllint.com/)
3.  MQ Version incompatibilities :  In case of any jar incompatibility issue, the rule of thumb is to **Use the jars from MQ version 7.5**. We have seen some jar incompatibility issues on IBM version 7.0.x ,version 7.1.x and version 8.x when the extension is configured in **Client** mode. However, after replacing the jars with MQ version 7.5's jars, everything worked fine. 
4. Metric Limit: Please start the machine agent with the argument -Dappdynamics.agent.maxMetrics=5000 if there is a metric limit reached error in the logs. If you don't see the expected metrics, this could be the cause.
5. Check Logs: There could be some obvious errors in the machine agent logs. Please take a look.
6. `The config cannot be null` error.
   This usually happenes when on a windows machine in monitor.xml you give config.yaml file path with linux file path separator `/`. Use Windows file path separator `\` e.g. `monitors\MQMonitor\config.yaml` .

7. Collect Debug Logs: Edit the file, <MachineAgent>/conf/logging/log4j.xml and update the level of the appender com.appdynamics to debug Let it run for 5-10 minutes and attach the logs to a support ticket


WebSphere MQ Queue Dashboard
 
![alt tag](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/117iCEAEF182B361D1AA/image-size/original?v=mpbl-1&px=-1)

WebSphere MQ Queue Metric Browser
 
![alt tag](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/121iBB4C49BE5A21431B/image-size/original?v=mpbl-1&px=-1)
WebSphere MQ Queue Policies
 
![alt tag](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/123i935D8EB16AF07B67/image-size/original?v=mpbl-1&px=-1)

WebSphere MQ Queue Alert Email
 
![alt tag](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/125iD09F37B355E70A55/image-size/original?v=mpbl-1&px=-1)
 
WebSphere MQ Queue Policy Remediation
 
![alt tag](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/127iF9FD2336797A0D2E/image-size/original?v=mpbl-1&px=-1)

More Troubleshooting
---------------------

1. Error `Completion Code '2', Reason '2495'`
   Normally this error occurs if the environment variables are not set up correctly for this extension to work MQ in Bindings Mode.
   
   If you are seeing `Failed to load the WebSphere MQ native JNI library: 'mqjbnd'`, please add the following jvm argument when starting the MA. 
   
   -Djava.library.path=<path to lib64 directory> For eg. on Unix it could -Djava.library.path=/opt/mqm/java/lib64 
   
   Sometimes you also have run the setmqenv script before using the above jvm argument to start the machine agent. 
   
   . /opt/mqm/bin/setmqenv -s 
   
   For more details, please check this [doc](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_7.1.0/com.ibm.mq.doc/zr00610_.htm)
   
   This might occour due to various reasons ranging from incorrect installation to applying [ibm fix packs](http://www-01.ibm.com/support/docview.wss?uid=swg21410038) but most of the time it happens when you are trying to connect in `Bindings` mode and machine agent is not on the same machine on which WMQ server is running. If you want to connect to WMQ server from a remote machine then connect using `Client` mode.
   
   Another way to get around this issue is to avoid using the Bindings mode. Connect using CLIENT transport type from a remote box. Make sure to provide Windows admin username and password in the config.yaml.

3. Error `Completion Code '2', Reason '2035'`
   This could happen for various reasons but for most of the cases, for **Client** mode the user specified in config.yaml is not authorized to access the queue manager. Also sometimes even if userid and password are correct, channel auth (CHLAUTH) for that queue manager blocks traffics from other ips, you need to contact admin to provide you access to the queue manager.
   For Bindings mode, please make sure that the MA is owned by a mqm user. Please check [this doc](https://www-01.ibm.com/support/docview.wss?uid=swg21636093) 
  
4. MQJE001: Completion Code '2', Reason '2195'
   This could happen in **Client** mode. One way this could be fixed is to use 7.5.2 version of the jars. 

5. MQJE001: Completion Code '2', Reason '2400'
   This could happen if unsupported cipherSuite is provided or JRE not having/enabled unlimited jurisdiction policy files. Please check SSL Support section.

Support Tickets
---------------
If after going through the Troubleshooting Document you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.  

1. Stop the running machine agent .
2. Delete all existing logs under <MachineAgent>/logs .
3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug. 
     <logger name="com.singularity">
     <logger name="com.appdynamics">
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
5. Attach the zipped <MachineAgent>/conf/* directory here.
 6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here .

For any support related questions, you can also contact help@appdynamics.com.

Contributing
------------
Always feel free to fork and contribute any changes directly via GitHub.

Version
-------
Version: 7.0

Controller Compatibility: 3.7 or Later

Product  Tested On: 7.x, 8.x, 9.x

Last updated On: 21st March, 2018

List of Changes to this extension (Link this page with the changelog.md page on github)




		
