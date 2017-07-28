package com.phillips66.activemq.admin;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.DestinationInfo;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* processes income advisory message, astracting queuename into body message */

public class AdvisorySubscriberProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(AdvisorySubscriberProcessor.class);
	
	private String queuePrefix;
	
	public String getQueuePrefix() {
		return queuePrefix;
	}
	public void setQueuePrefix(String queuePrefix) {
		this.queuePrefix = queuePrefix;
	}
	
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
				message.getDataStructureType();
				if (message.getDataStructure() instanceof DestinationInfo) {
					DestinationInfo info = (DestinationInfo) message.getDataStructure();
					// only interested in create queue events
					if (info.getOperationType() == DestinationInfo.ADD_OPERATION_TYPE) {
						String queueName = info.getDestination().getPhysicalName();

						if (queuePrefix != null && queueName.startsWith(queuePrefix)) {					
							exchange.getIn().setBody(queueName);
							exchange.getIn().setHeader("PROCESS_ADVISORY", "true");
						}
					}
				}
			}

		}
	}
}
