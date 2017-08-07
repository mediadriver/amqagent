package com.phillips66.activemq.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxAdapter implements FabricAdapter {

	private static final Logger logger = LoggerFactory.getLogger(JmxAdapter.class);
	String jmxUrl;
	private JMXConnector jmxConnector;
	private MBeanServerConnection connection;

	public boolean connect(Properties configProps) {
		boolean connected = false;

		try {

			HashMap<String, String[]> map = new HashMap<String, String[]>();
			String[] credentials = new String[2];
			credentials[0] = configProps.getProperty("username");
			String jmxPassword = configProps.getProperty("password");
			credentials[1] = jmxPassword;
			map.put(JMXConnector.CREDENTIALS, credentials);

			jmxUrl = configProps.getProperty("url");
			if (jmxUrl == null) {
				jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root";
			}

			jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl), map);

			// Get an MBeanServerConnection on the remote VM.
			connection = jmxConnector.getMBeanServerConnection();
			connected = true;

		} catch (Exception ex) {
			logger.error("Error during connect" + ex.getMessage(), ex);
			ex.printStackTrace();
		}
		return connected;
	}

	public void shutdown() {
		try {
			if (jmxConnector != null)
				jmxConnector.close();
		} catch (Exception ex) {
			logger.error("Shutting down Jmx Connector: " + ex);
		}
	}

	public List<String> getContainers() throws Exception {

		ArrayList<String> jmxUrls = new ArrayList<String>();

		ObjectName serviceConfigName = new ObjectName("io.fabric8:type=Fabric");
		Object result = connection.invoke(serviceConfigName, "containers", null, null);
		if (result != null) {
			ArrayList<TreeMap> containers = (ArrayList) result;
			for (TreeMap treeMap : containers) {
				if (treeMap.get("parent") != null) { // lets not add root nodes
					jmxUrls.add((String) treeMap.get("jmxUrl"));
				}
			}
		}
		
		return jmxUrls;
	}

	public boolean isMasterBroker() throws Exception {
		boolean broker = false;

		try {
			Set<ObjectName> brokerBeans = connection.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*"), null);

			for (ObjectName brokerName : brokerBeans) {
				broker = !(boolean) connection.getAttribute(brokerName, "Slave");
				// let's just use the first one, doesn't make sense to have two brokers in the same process
				break;
			}

		} catch (Exception e) {
			logger.error("checking if master broker" + e, e);
		}
		return broker;
	}
	
	public void createQueue(String queueName) {
		try {
			// does the queue exist already?
			Set<ObjectName> queueBeans = connection.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=" + queueName), null);
			if (queueBeans.size() == 0) {
				Set<ObjectName> brokerBeans = connection.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*"), null);
				for (ObjectName brokerName : brokerBeans) {
					logger.info("add queue call queueName:" + queueName + " jmxUrl:" + jmxUrl);
					connection.invoke(brokerName, "addQueue", new Object[] {queueName}, new String[] {String.class.getName()});
					// let's just use the first one, doesn't make sense to have two brokers in the same process
					break;
				}
			}

		} catch (Exception e) {
			logger.error("create queue" + e, e);
		}
	}
	
	public List<String> getQueues() throws Exception {

		ArrayList<String> queues = new ArrayList<String>();

		Set<ObjectName> queueBeans = connection.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*" ), null);
		for (ObjectName objectName: queueBeans) {
			queues.add(objectName.getKeyPropertyList().get("destinationName"));
		}
		return queues;
	}

}
