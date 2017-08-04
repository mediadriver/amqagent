package com.phillips66.activemq.admin;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(BootstrapProcessor.class);
	
	private String queuePrefix;
	private String leaderNodes;
	private String jmxUserName;
	private String jmxPassword;

	private MBeanServer mbeanServer;
	private JmxConnections jmxConnections = new JmxConnections();
	
	public String getQueuePrefix() {
		return queuePrefix;
	}
	public void setQueuePrefix(String queuePrefix) {
		this.queuePrefix = queuePrefix;
	}
	public String getLeaderNodes() {
		return leaderNodes;
	}
	public void setLeaderNodes(String leaderNodes) {
		this.leaderNodes = leaderNodes;
	}
	public String getJmxUserName() {
		return jmxUserName;
	}
	public void setJmxUserName(String jmxUserName) {
		this.jmxUserName = jmxUserName;
	}
	public String getJmxPassword() {
		return jmxPassword;
	}
	public void setJmxPassword(String jmxPassword) {
		this.jmxPassword = jmxPassword;
	}

	public void init() {
		jmxConnections.setJmxUsername(jmxUserName);
		jmxConnections.setJmxPassword(jmxPassword);
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
	}
	
	
	public void shutdown() {
		jmxConnections.shutdown();
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		
		try {
			List<String> queueNames = new ArrayList<String>();
			
			int searchCount = 0;
			
			// get the container urls from any leader, try all nodes 3 times
			
			String leaders[] = leaderNodes.split(",");
			Collections.shuffle(Arrays.asList(leaders));
			
			searchloop:
			while (searchCount < 3) {
				for (String leader: leaders) { // lets try each leader until one returns
					try {
						JmxAdapter jmxAdapter = jmxConnections.getConnection(leader);
						List<String> jmxUrls = jmxAdapter.getContainers();
						if (jmxUrls.size() > 0) {
							// now lets get a master broker
							for (String jmxUrl : jmxUrls) {
								JmxAdapter container = jmxConnections.getConnection(jmxUrl);
								if (container.isMasterBroker()) {
									// get the list of queues
									queueNames = container.getQueues();
		
									for (String queueName: queueNames) {
										if (queueName.startsWith(queuePrefix)) {
											//lets check if we have that queue
											Set<ObjectName> beans = mbeanServer.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=" + queueName), null);		
											if (beans.size() == 0) {
												Set<ObjectName> brokerBeans = mbeanServer.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*"), null);		
												for (ObjectName objectName : brokerBeans) {
													mbeanServer.invoke(objectName, "addQueue", new Object[] {queueName}, new String[] {String.class.getName()});
													break;
												}
											}
										}
									}
									break;
									
								}
							}
							if (queueNames.size() > 0) {
								break searchloop; // one of the leaders has returned a container list
							}
						}
					} catch (Exception ex) {
						logger.warn("error processing leader in boostrap, lets continue on checking other leaders: " + ex, ex);
					}
				}
				searchCount++;
			}
			
			
		} catch (Exception ex) {
			logger.error("Error synchronizing queue names while bootstrapping", ex);
		}
	}
}
