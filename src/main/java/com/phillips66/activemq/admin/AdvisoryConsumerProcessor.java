package com.phillips66.activemq.admin;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.RemoveInfo;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* processes income advisory message, abstracting the queuename into message body*/

public class AdvisoryConsumerProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(AdvisoryConsumerProcessor.class);
	
	private String queuePrefix = "vSub.nondurable";
	private String advisoryPrefix = "topic://ActiveMQ.Advisory.Consumer.Queue.";
		
	@Override
	public void process(Exchange exchange) throws Exception {
		if (exchange != null && exchange.getIn() != null && exchange.getIn().getBody() != null) {
			ActiveMQMessage message = null;
			try {
				message = exchange.getIn().getBody(ActiveMQMessage.class);
			} catch (Exception e) {
				logger.error("Exception while trying to retrieve ActiveMQMessage from exchange body: "+ e.getMessage(), e);
			}
			
			if (message != null && message.isAdvisory()) {						
				if (message.getDataStructure() instanceof RemoveInfo) {
					RemoveInfo info = (RemoveInfo)message.getDataStructure();
					String advisoryDestination = message.getDestination().toString();
					String queueName = advisoryDestination.substring(getAdvisoryPrefix().length());
					logger.info("Received remove consumer advisory on " + advisoryDestination + " consumerId: " + info.getObjectId() + " dest: " + queueName);
					if (queueName != null && queueName.startsWith(getQueuePrefix())) {
						exchange.getIn().setHeader("AMQ_AGENT_QUEUE_NAME", queueName);
						exchange.getIn().setHeader("AMQ_AGENT_PROCESS_ADVISORY", "true");
					}
				}
			}
		}
	}
	
	public String getAdvisoryPrefix() {
		return advisoryPrefix;
	}
	public void setAdvisoryPrefix(String advisoryPrefix) {
		this.advisoryPrefix = advisoryPrefix;
	}
	public String getQueuePrefix() {
		return queuePrefix;
	}
	public void setQueuePrefix(String queuePrefix) {
		this.queuePrefix = queuePrefix;
	}
}
