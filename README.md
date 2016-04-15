WebSphere MQ Monitoring Extension
=================================

Use case
-------------


The WebSphere MQ monitoring extension can monitor multiple queues managers and their resources, namely queues and channels.

This monitor can report the specified metrics (Configurable) for queue managers:

The MQ Monitor currently supports IBM Websphere MQ version 7.x. It may work with other Websphere MQ versions but it has not been tested in those environments.

 
Prerequisites
-------------
 
The machine where you install the monitor must have the AppDynamics Machine Agent installed and running.
 
In CLIENT mode, the machine where you install the monitor must have firewall access to each queue manager (host and port) you define in the monitor.xml file.
Also, in CLIENT mode, please provide admin level credentials in the config.yaml file to monitor the queue manager and its resources

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

These jar files are typically found in /opt/mqm/java/lib on a UNIX server but may be found in an alternate location depending upon your environment.
 
Alternatively, you may download either the Websphere MQ Server and/or Client that contain the jars here.

In case of client type connection, WMQ client must be installed as WMQ jars in turn need jni libraries.

Troubleshooting
---------------

1. Verify Machine Agent Data: Please start the Machine Agent without the extension and make sure that it reports data. Verify that the machine agent status is UP and it is reporting Hardware Metrics.
2. config.yml: Validate the file [here](http://www.yamllint.com/)
3. There have been incompatibility issues with v7.0* and v7.1* version of WebSphere Message Queue Server.Please try using jar versions > v7.5x to resolve these incompatibility issues.
   Similarly, v8.x no longer ships with connector.jar. Try using the missing jars from previous versions like v7.5x
4. Metric Limit: Please start the machine agent with the argument -Dappdynamics.agent.maxMetrics=5000 if there is a metric limit reached error in the logs. If you don't see the expected metrics, this could be the cause.
5. Check Logs: There could be some obvious errors in the machine agent logs. Please take a look.
6. Collect Debug Logs: Edit the file, <MachineAgent>/conf/logging/log4j.xml and update the level of the appender com.appdynamics to debug Let it run for 5-10 minutes and attach the logs to a support ticket

Rebuilding the Project
----------------------
1. Clone the repo websphere-mq-monitoring-extension from GitHub https://github.com/Appdynamics
2. Copy the seven MQ jar files listed above into the websphere-mq-monitoring-extension/lib/ directory. Create a lib folder if not already present.
3. Run 'mvn clean install' from the cloned websphere-mq-monitoring-extension directory.
4. The MQMonitor-<version>.zip should get built and found in the 'target' directory.


Installation
------------
The following instructions assume that you have installed the AppDynamics Machine Agent in the following directory:
 
    Unix/Linux:    /AppDynamics/MachineAgent
    Windows:     C:\AppDynamics\MachineAgent
 
1. Unzip contents of MQMonitor-<version>.zip file and copy to MachineAgent/monitors directory
2. 

 2.1 BINDINGS type connection: Requires WMQ Extension to be deployed in machine agent on the same machine where WMQ server is installed.
 Copy the following jars to the MQMonitor directory
 
  ``` 
   com.ibm.mq.commonservices.jar
   com.ibm.mq.jar
   com.ibm.mq.jmqi.jar
   dhbcore.jar
   com.ibm.mq.headers.jar
   connector.jar
   com.ibm.mq.pcf.jar
  ```
 2.2 Alternatively in case of CLIENT type connection install WMQ client and edit monitor.xml classpath section to point to above mentioned jars in WMQ client installation. This is important as jni libraries required by these jars are available in client installation only.

 If you don't want to copy the jar files, you can point to the jar files by providing the relative paths to the jar files in the monitor.xml.

3. Create a channel of type server connection in each of the queue manager you wish to query. 

4. Edit the config.yaml file.  An example config.yaml file follows these installation instructions.

5. Restart the Machine Agent.
 
Sample config.yaml
------------------
 
The following is a sample config.yaml file that depicts two different queue managers defined.
 

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

    mqMertics:
      # This Object will extract queue manager metrics
      - metricsType: "queueMgrMetrics"
        metrics:
          include:
            - Status: "Status"
              ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACF_Q_MGR_STATUS"

      # This Object will extract channel metrics
      - metricsType: "queueMetrics"
        metrics:
          include:
            - MaxQueueDepth: "Max Queue Depth"
              ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_CURRENT_Q_DEPTH"

            - CurrentQueueDepth: "Current Queue Depth"
              ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_MAX_Q_DEPTH"

            - OpenInputCount: "Open Input Count"
              ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_OPEN_INPUT_COUNT"

            - OpenOutputCount: "Open Output Count"
              ibmConstant: "com.ibm.mq.constants.CMQC.MQIA_OPEN_OUTPUT_COUNT"

      # This Object will extract queue metrics
      - metricsType: "channelMetrics"
        metrics:
          include:
            - Messages: "Messages"
              ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_MSGS"

            - Status: "Status"
              ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_CHANNEL_STATUS" #http://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.dev.doc/q090880_.htm

            - ByteSent: "Byte Sent"
              ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BYTES_SENT"

            - ByteReceived: "Byte Received"
              ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BYTES_RECEIVED"

            - BuffersSent: "Buffers Sent"
              ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BUFFERS_SENT"

            - BuffersReceived: "Buffers Received"
              ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BUFFERS_RECEIVED"

```

WorkBench
---------

Workbench is a feature by which you can preview the metrics before registering it with the controller. This is useful if you want to fine tune the configurations. Workbench is embedded into the extension jar.
To use the workbench

1. Follow all the installation steps
2. Start the workbench with the command

```
    java -jar /path/to/MachineAgent/monitors/WMQMonitor/websphere-mq-monitoring-extension.jar
```

  This starts an http server at http://host:9090/. This can be accessed from the browser.
3. If the server is not accessible from outside/browser, you can use the following end points to see the list of registered metrics and errors.

```
    #Get the stats
    curl http://localhost:9090/api/stats
    #Get the registered metrics
    curl http://localhost:9090/api/metric-paths
```
4. You can make the changes to config.yml and validate it from the browser or the API
5. Once the configuration is complete, you can kill the workbench and start the Machine Agent


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
   This might occour due to various reasons ranging from incorrect installation to applying [ibm fix packs](http://www-01.ibm.com/support/docview.wss?uid=swg21410038) but most of the time it happenes when you are trying to connect in `Bindings` mode and machine agent is not on the same machine on which WMQ server is running. If you want to connect to WMQ server from a remote machine then connect using `Client` mode.
2. Error `Completion Code '2', Reason '2035'`
   This could happen for various reasons but for most of the cases, for **Client** mode the user specified in config.yaml is not authorized to access the queue manager. Also sometimes even if userid and password are correct, channel auth (CHLAUTH) for that queue manager blocks traffics from other ips, you need to contact admin to provide you access to the queue manager.
3. `The config cannot be null` error.
   This usually happenes when on a windows machine in monitor.xml you give config.yaml file path with linux file path separator `/`. Use Windows file path separator `\` e.g. `monitors\MQMonitor\config.yaml` .

```
  <task-arguments>
    <argument name="config-file" is-required="false" default-value="monitors\MQMonitor\config.yaml"/>
  </task-arguments>
		
```

Also this might occour if you have used TABs in config.yaml, In that case yaml parsing will fail and config won't be loaded.
		
