package com.phillips66.activemq.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(JmxAdapter.class);

	private Properties configProps;
	private JMXConnector jmxConnector;
	private MBeanServerConnection connection;

	public JmxAdapter(Properties configProps) {

		try {
			this.configProps = configProps;

		} catch (Exception ex) {
			logger.error("In JmxAdapter()" + ex);
		}
	}

	public boolean connect() {
		boolean connected = false;

		try {

			HashMap<String, String[]> map = new HashMap<String, String[]>();
			String[] credentials = new String[2];
			credentials[0] = configProps.getProperty("jmxUsername");

			// check if we need to decrypt the password
			String jmxPassword = configProps.getProperty("jmxPassword");
			if (jmxPassword.startsWith("ENC(") && jmxPassword.endsWith(")")) {
				StandardPBEStringEncryptor jascrpt = new StandardPBEStringEncryptor();
				EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
				config.setPasswordEnvName("MONITOR_ENCRYPTION_PASSWORD");
				config.setAlgorithm("PBEWithMD5AndDES");
				jascrpt.setConfig(config);
				jascrpt.initialize();
				jmxPassword = jmxPassword
						.substring(4, jmxPassword.length() - 1);
				jmxPassword = jascrpt.decrypt(jmxPassword);
			}
			credentials[1] = jmxPassword;

			map.put(JMXConnector.CREDENTIALS, credentials);

			String jmxUrl = configProps.getProperty("jmxUrl");
			if (jmxUrl == null) {
				jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root";
			}

			jmxConnector = JMXConnectorFactory.connect(
					new JMXServiceURL(jmxUrl), map);

			// Get an MBeanServerConnection on the remote VM.
			connection = jmxConnector.getMBeanServerConnection();
			connected = true;

		} catch (Exception ex) {
			logger.error("In connect()" + ex);
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
				if (treeMap.get("parent") != null) { // lets not add root
														// nodes
					jmxUrls.add((String) treeMap.get("jmxUrl"));
				}
				// for(Object key: treeMap.keySet()) {
				// System.out.println(key);
				// }

			}
		}
		
		return jmxUrls;
	}

	public boolean isMasterBroker() throws Exception {
		boolean broker = false;

		try {
			Set<ObjectName> beans = connection.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*"), null);

			for (ObjectName objectName : beans) {

				ObjectInstance jmxInstance = connection.getObjectInstance(objectName);

				broker = !(boolean) connection.getAttribute(objectName, "Slave");

				// let's just use the first one, doesn't make sense to have two
				// brokers in the same process
				break;
			}

		} catch (Exception e) {
			System.out.println(e);
		}
		return broker;
	}
	
	public void createQueue(String queueName) {
		try {
			
			// does the queue exist already
			Set<ObjectName> queueBeans = connection.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=" + queueName), null);
			if (queueBeans.size() == 0) {
				
				Set<ObjectName> beans = connection.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*"), null);
	
				for (ObjectName objectName : beans) {
	
					ObjectInstance jmxInstance = connection.getObjectInstance(objectName);
	
					Object result = connection.invoke(objectName, "addQueue", new Object[] {queueName}, new String[] {String.class.getName()});
					// TODO process result (null currently on success) and handle errors
	
					// let's just use the first one, doesn't make sense to have two
					// brokers in the same process
					break;
				}
			} 

		} catch (Exception e) {
			System.out.println(e);
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
