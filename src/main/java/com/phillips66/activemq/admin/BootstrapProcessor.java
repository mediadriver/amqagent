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
	private String username;
	private String password;

	private MBeanServer mbeanServer;
	private FabricConnections fabricConnections = new FabricConnections();
	
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
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public void init() {
		fabricConnections.setUsername(username);
		fabricConnections.setPassword(password);
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
	}
	
	public void shutdown() {
		fabricConnections.shutdown();
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		
		try {
			List<String> queueNames = new ArrayList<String>();
			
			int searchCount = 0;
			int totalAddedQueueCount = 0;
			
			// get the container urls from any leader, try all nodes 3 times
			
			String leaders[] = leaderNodes.split(",");
			Collections.shuffle(Arrays.asList(leaders));
			
			searchloop:
			while (searchCount < 3) {
				for (String leader: leaders) { // lets try each leader until one returns
					try {
						FabricAdapter fabricAdapter = fabricConnections.getConnection(leader);
						List<String> urls = fabricAdapter.getContainers();
						if (urls.size() > 0) {
							// now lets get a master broker
							for (String url : urls) {
								int addedQueueCount = 0;
								FabricAdapter container = fabricConnections.getConnection(url);
								if (container.isMasterBroker()) {
									logger.info(String.format("Bootstrapping local broker with master broker url=%s", url));
									// get the list of queues
									queueNames = container.getQueues();
									for (String queueName: queueNames) {
										if (queueName.startsWith(queuePrefix)) {
											//lets check if we have that queue
											Set<ObjectName> beans = mbeanServer.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=" + queueName), null);		
											if (beans.size() == 0) {
												Set<ObjectName> brokerBeans = mbeanServer.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*"), null);		
												for (ObjectName objectName : brokerBeans) {
													logger.info(String.format("Syncing missing master broker queue %s to local broker", queueName));
													mbeanServer.invoke(objectName, "addQueue", new Object[] {queueName}, new String[] {String.class.getName()});
													totalAddedQueueCount++;
													addedQueueCount++;
													break;
												}
											}
										}
									}
								} else {
									logger.info(String.format("Container url=%s is not a master broker", url));
								}
								logger.info(String.format("Synchronized %s queues to local broker from broker url: %s", addedQueueCount, url));
							}
							if (totalAddedQueueCount > 0) {
								break searchloop; // one of the leaders has returned a container list
							}
						}
					} catch (Exception ex) {
						logger.warn("error processing leader in boostrap, lets continue on checking other leaders msg: " + ex.getMessage(), ex);
					}
				}
				searchCount++;
			}	
			logger.info(String.format("Synchronized %s total queues to local broker", totalAddedQueueCount));
		} catch (Exception ex) {
			logger.error("Error synchronizing queue names while bootstrapping msg: " + ex.getMessage() , ex);
		}
	}
}
