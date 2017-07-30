package com.phillips66.activemq.admin;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueCleanupProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(QueueCleanupProcessor.class);

	private String queueCleanupPrefix;

	private MBeanServer mbeanServer;
	
	public void init() {
		mbeanServer = ManagementFactory.getPlatformMBeanServer();
	}
	
	public String getQueueCleanupPrefix() {
		return queueCleanupPrefix;
	}
	public void setQueueCleanupPrefix(String queueCleanupPrefix) {
		this.queueCleanupPrefix = queueCleanupPrefix;
	}


	@Override
	public void process(Exchange exchange) throws Exception {
		try {

			// let's get all the queues
			Set<ObjectName> beans = mbeanServer.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*"), null);		
			for (ObjectName objectName: beans) {
				Hashtable<String,String> parts = objectName.getKeyPropertyList();
				String queueName = parts.get("destinationName");

				if (queueName.startsWith(queueCleanupPrefix)) {
					// let's check if the queue has any clients
					long count = (long)mbeanServer.getAttribute(objectName, "ConsumerCount");
					if (count == 0) {
						
						Set<ObjectName> brokers = mbeanServer.queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=*"), null);

						for (ObjectName brokerObjectNames : brokers) {
							mbeanServer.invoke(brokerObjectNames, "removeQueue", new Object[] {queueName}, new String[] {String.class.getName()});
							break;
						}
					}
				} 
				
			}
		} catch (Exception ex) {
			logger.error("cleanup queues: " + ex);
		}
	}
}
