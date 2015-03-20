WebSphere MQ Monitoring Extension
=================================

Use Case
-------- 

The WebSphere MQ monitoring extension can monitor multiple queues in multiple Websphere MQ queue managers.  
 
This monitor reports the following metrics for each queue manager:
 
Maximum queue depth
Current queue depth for each queue.
 
The MQ Monitor currently supports IBM Websphere MQ version 7.x. It may work with other Websphere MQ versions but it has not been tested in those environments.
 
Prerequisites
-------------
 
The machine where you install the monitor must have the AppDynamics Machine Agent installed and running.
 
The machine where you install the monitor must have firewall access to each queue manager (host and port) you define in the monitor.xml file.
 
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
 
For more information about MQ installation locations, click here.
 
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
 
1. Unzip MQMonitor.zip file and copy to MachineAgent/monitors directory
2. Copy the following jars to the MQMonitor directory

 ``` 
  com.ibm.mq.commonservices.jar
  com.ibm.mq.jar
  com.ibm.mq.jmqi.jar
  dhbcore.jar
  com.ibm.mq.headers.jar
  connector.jar
  com.ibm.mq.pcf.jar
 ```
3. Edit the monitor.xml file. The configuration supports defining multiple queue managers to connect to as well as multiple queues for a given queue manager.  An example monitor.xml file follows these installation instructions.
4. Follow the numbering convention when adding or removing queue managers and or queues.

    Note: Do not create multiple instances of the queue monitor running that monitor the same queues inside the       same queue managers.  This causes duplication data with in the AppDynamics Controller.
5. Restart the Machine Agent.
 
Sample monitor.xml
------------------
 
The following is a sample monitor.xml file that depicts two different queue managers defined and two queues defined for each queue manager.
 

```
<!-- The name of the root folder where the metrics will appear under the tier -->
<argument name="root_category_name" is-required="true" default-value="Queue Monitoring" />

<!-- The number of queue managers that are configured -->
<argument name="number_of_queue_managers" is-required="true" default-value="2" />

<!-- the host or IP address of the queue manager -->
<argument name="queue_mgr_host_1" is-required="true" default-value="127.0.0.1" />

<!-- The port number of the queue manager -->
<argument name="queue_mgr_port_1" is-required="true" default-value="1414" />	

<!-- The actual name of the queue manager -->	
<argument name="queue_mgr_name_1" is-required="true" default-value="QUEUEMGR01" />

<!-- The channel name of the queue manager -->
<argument name="queue_mgr_channel_name_1" is-required="true" default-value="SYSTEM.ADMIN.SVRCONN" />

<!-- The transport type for the queue manager connection, the default is 1 for a client type connection -->
<argument name="queue_mgr_transport_type_1" is-required="true" default-value="1" />

<!-- The user name for the connection if required -->
<argument name="queue_mgr_user_1" is-required="true" default-value="" />

<!-- The password for the connection if required -->
<argument name="queue_mgr_password_1" is-required="true" default-value="" />

<!-- The number of configured queues to be monitored for the queue manager -->
<argument name="queue_mgr_1_number_of_queues" is-required="true" default-value="2" />

<!-- The name of the first queue -->
<argument name="queue_mgr_1_queue_1" is-required="true" default-value="CREWDATA01" />

<!-- The name of the second queue -->
<argument name="queue_mgr_1_queue_2" is-required="true" default-value="CREWDATA02" />

<!-- the host or IP address of the queue manager -->
<argument name="queue_mgr_host_2" is-required="true" default-value="127.0.0.2" />

<!-- The port number of the queue manager -->
<argument name="queue_mgr_port_2" is-required="true" default-value="1415" />	

<!-- The actual name of the queue manager -->	
<argument name="queue_mgr_name_2" is-required="true" default-value="QUEUEMGR02" />

<!-- The channel name of the queue manager -->
<argument name="queue_mgr_channel_name_2" is-required="true" default-value="SYSTEM.ADMIN.SVRCONN" />

<!-- The transport type for the queue manager connection, the default is 1 for a client type connection -->
<argument name="queue_mgr_transport_type_2" is-required="true" default-value="1" />

<!-- The user name for the connection if required -->
<argument name="queue_mgr_user_2" is-required="true" default-value="" />

<!-- The password for the connection if required -->
<argument name="queue_mgr_password_2" is-required="true" default-value="" />

<!-- The number of configured queues to be monitored for the queue manager -->
<argument name="queue_mgr_2_number_of_queues" is-required="true" default-value="2" />

<!-- The name of the first queue -->
<argument name="queue_mgr_2_queue_1" is-required="true" default-value="CREWDATA03" />

<!-- The name of the second queue -->
<argument name="queue_mgr_2_queue_2" is-required="true" default-value="CREWDATA04" />

```

Password Encryption Support
---------------------------
To avoid setting the clear text password in the monitor.xml. Please follow the process to encrypt the password and set the encrypted password and 
the key in the monitor.xml

1. Download the util jar to encrypt the password from https://github.com/Appdynamics/maven-repo/blob/master/releases/com/appdynamics/appd-exts-commons/1.1.2/appd-exts-commons-1.1.2.jar and navigate to downloaded directory
2. Encrypt password from the commandline

   ```
   java -cp appd-exts-commons-1.1.2.jar com.appdynamics.extensions.crypto.Encryptor myKey myPassword
   ```
3. Use the same encryption key for each queue's password. In the monitor.xml, add the encryption key as below.

   ```
   <argument name="encryption-key" is-required="false" default-value="myKey"/>
   ```
4. For every queue manager, you can specify an encrypted password as below
   ```
   <argument name="queue_mgr_password_encrypted_1" is-required="true" default-value="<ENCRYPTED_PASSWORD>"/>
   ```




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

Support
------- 
For any questions or feature request, please contact James Schneider (james.schneider@appdynamics.com)

