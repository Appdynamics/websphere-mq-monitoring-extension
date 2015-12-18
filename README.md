WebSphere MQ Monitoring Extension
=================================

Use Case
-------- 

The WebSphere MQ monitoring extension can monitor queues and channels in Websphere MQ queue managers.  
 
This monitor can report the specified metrics (Configurable) for queue managers.

 
The MQ Monitor currently supports IBM Websphere MQ version 7.x. It may work with other Websphere MQ versions but it has not been tested in those environments.
 
Prerequisites
-------------
 
The machine where you install the monitor must have the AppDynamics Machine Agent installed and running.
 
The machine where you install the monitor must have firewall access to each queue manager (host and port) you define in the config.yaml file.
 
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
 
Alternatively, you may download either the Websphere MQ Server and or Client that contain the jars here.
 
 
Rebuilding the Project
----------------------
1. Clone the repo websphere-mq-monitoring-extension from GitHub https://github.com/Appdynamics
2. Copy the seven MQ jar files listed above into the websphere-mq-monitoring-extension/lib/ directory. Create a lib folder if not already present.
3. Run 'mvn clean install' from the cloned websphere-mq-monitoring-extension directory.
4. The MQMonitor.zip should get built and found in the 'target' directory.


Installation
------------
The following instructions assume that you have installed the AppDynamics Machine Agent in the following directory:
 
    Unix/Linux:    /AppDynamics/MachineAgent
    Windows:     C:\AppDynamics\MachineAgent
 
1. Unzip contents of MQMonitor-2.0.zip file and copy to MachineAgent/monitors directory
2. There are two ways WMQ server can be connected using websphere-mq-monitoring-extension. One is `Bindings` mode other is `Client` mode. These modes can be set in config.yaml.

 2.1. BINDINGS type connection: Requires WMQ Extension to be deployed in machine agent on the same machine where WMQ server is installed.
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
 2.2. Alternatively in case of CLIENT type connection install WMQ client and edit monitor.xml classpath section to point to above mentioned jars in WMQ client installation. This is important as there are many libraries and other files required by these jars available in client installation only. 
 
3. Create a channel of type server connection in each of the queue manager you want to monitor. 

4. Edit the config.yaml file and configure settings for each of the queue managers. An example config.yaml file follows these installation instructions.

5. Restart the Machine Agent.
 
Sample config.yaml
------------------
 
The following is a sample config.yaml file that depicts two different queue managers defined.
 

```
# WebsphereMQ instance particulars

queueManagers:
  - host: "localhost"
    port: 1414
    #Actual name of the queue manager
    name: "queueMgr1"
    #Channel name of the queue manager, channel should be server-conn type.
    channelName: "APPD1.SVRCONN"
    #The transport type for the queue manager connection, the default is "Bindings" for a binding type connection
    #For bindings type connection WMQ extension (i.e machine agent) should be running on the same machine on which WebbsphereMQ server is running
    #for client type connection change it to "Client".
    transportType: "Bindings"
    #user with admin level access, no need to provide credentials in case of bindings transport type, it is only applicable for client type
    username:
    password:
    
    #SSL related properties
    # e.g."C:/Program Files (x86)/IBM/WebSphere MQ/ssl/key" , Please use forward slash in the path.
    sslKeyRepository:  
    # e.g. "SSL_RSA_WITH_AES_128_CBC_SHA256"
    cipherSuite:
    
    # First Include filter is applied and on the result of include filter exclude filter is applied.  
    queueIncludeFilters:
        #type value: STARTSWITH, EQUALS, ENDSWITH, CONTAINS, NONE (default all queues) 
        type: "NONE"
        #The name of the queue or queue name pattern as per queue filter, comma separated values
        values: []
          
    queueExcludeFilters:
        #type value: STARTSWITH, EQUALS, ENDSWITH, CONTAINS, NONE (default all queues) 
        type: "STARTSWITH"
        #The name of the queue or queue name pattern as per queue filter, comma separated values
        values: ["SYSTEM", "AMQ", "KMQ"]

    channelIncludeFilters:
        #type value: STARTSWITH, EQUALS, ENDSWITH, CONTAINS, NONE (default all channels) 
        type: "NONE"
        #The name of the channel or channel name pattern as per channel filter, comma separated values
        values: []    

    channelExcludeFilters:
        #type value: STARTSWITH, EQUALS, ENDSWITH, CONTAINS, NONE (default all channels) 
        type: "STARTSWITH"
        #The name of the channel or channel name pattern as per channel filter, comma separated values
        values: ["SYSTEM", "AMQ", "KMQ"]
        
  - host: "localhost"
    port: 1417
    name: "QUEUEMGR02"
    channelName: "APPD2.SVRCONN"
    transportType: "Client"
    username: "admin"
    password: "abcde"
    
    queueIncludeFilters:
        type: "STARTSWITH"
        values: ["TEST"]
          
    queueExcludeFilters:
        type: "NONE"
        values: []

    channelIncludeFilters:
        type: "NONE"
        values: []    

    channelExcludeFilters:
        type: "CONTAINS"
        values: ["SVRCONN"]
    
# metrics are now configurable please check IBM Java doc to add more metrics.
    
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
          ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_CHANNEL_STATUS"
          
        - ByteSent: "Byte Sent"
          ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BYTES_SENT"
          
        - ByteReceived: "Byte Received"
          ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BYTES_RECEIVED"
          
        - BuffersSent: "Buffers Sent"
          ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BUFFERS_SENT"
          
        - BuffersReceived: "Buffers Received"
          ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BUFFERS_RECEIVED"

numberOfThreads: 10
          
#prefix used to show up metrics in AppDynamics, Note: it was root_category_name in monitor.xml
metricPrefix: "Custom Metrics|WebsphereMQ|" 

```

Adding metrics
--------
Specify additional metrics below the `include` section under `metrics` section in config.yaml. Follow the format of other metrics. In `- BuffersReceived: "Buffers Received"`, first keyword `BuffersReceived` is the handle for that metrics and `"Buffers Received"` is the name of metrics that you want to appear in metric browser. `ibmConstant: "com.ibm.mq.constants.CMQCFC.MQIACH_BUFFERS_RECEIVED"` is the constant provided by IBM, Please [Click Here](https://www-01.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.javadoc.doc/WMQJavaClasses/index.html?lang=en) to refer ibm java doc for adding ibmConstant to the metrics.


WebSphere MQ Queue Worker

![alt tag](http://appsphere.appdynamics.com/t5/image/serverpage/image-id/111i0BBFBE1938EF2BA2/image-size/original?v=mpbl-1&px=-1)
 
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

Troubleshooting
------- 
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
		
