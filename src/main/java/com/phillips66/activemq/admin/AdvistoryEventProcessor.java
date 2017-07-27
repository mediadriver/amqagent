package com.phillips66.activemq.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.DestinationInfo;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.phillips66.activemq.model.QueueCreateEvent;

public class AdvistoryEventProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(AdvistoryEventProcessor.class);

	private ProducerTemplate producerTemplate;

	private JmxConnections jmxConnections = new JmxConnections();
	private String queuePrefix;
	private String leaderNodes;
	private String jmxUserName;
	private String jmxPassword;
	private String queueCreateQueueName; //probably a better name
	
	public ProducerTemplate getProducerTemplate() {
		return producerTemplate;
	}
	public void setProducerTemplate(ProducerTemplate producerTemplate) {
		this.producerTemplate = producerTemplate;
	}
	
	
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
	public String getQueueCreateQueueName() {
		return queueCreateQueueName;
	}
	public void setQueueCreateQueueName(String queueCreateQueueName) {
		this.queueCreateQueueName = queueCreateQueueName;
	}
	
	public void init() {
		jmxConnections.setJmxUsername(jmxUserName);
		jmxConnections.setJmxPassword(jmxPassword);
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
					if (info.getOperationType() == DestinationInfo.ADD_OPERATION_TYPE) {
						String queueName = info.getDestination().getPhysicalName();

						if (queuePrefix != null && queueName.startsWith(queuePrefix)) {
								
								List<String> jmxContainerUrls = new ArrayList<String>();
								
								// lets get the container urls from a leader
								String leaders[] = leaderNodes.split(",");
								for (String leader: leaders) { // lets try each leader until one returns
									try {
										JmxAdapter jmxAdapter = jmxConnections.getConnection(leader);
										List<String> jmxUrls = jmxAdapter.getContainers();
										if (jmxUrls.size() > 0) {
											jmxContainerUrls = jmxUrls;
											break; // we have a list now
										}
									} catch (Exception ex) {
										System.out.println("lets continue on checking other leaders: " + ex);
									}
								}
								
								// now lets contact each container and see if it's a amq instance
								for (String jmxUrl : jmxContainerUrls) {
									JmxAdapter container = jmxConnections.getConnection(jmxUrl);
									if (container.isMasterBroker()) {
										// create a message to create this queue
										
										QueueCreateEvent queueCreateEvent = new QueueCreateEvent();
										queueCreateEvent.setQueueName(queueName);
										queueCreateEvent.setContainerJmxUrl(jmxUrl);
										
										// TODO - reuse the mapper/writer?
										ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
										String json = ow.writeValueAsString(queueCreateEvent);
										producerTemplate.sendBody("amq:queue:" + queueCreateQueueName,json);
										
									}
								}
			
						}
					}
				} else {
					logger.warn("Unsupported o.a.activemq.command.DataStructureType: " + message.getDataStructureType());
				}

			}

		}
	}
}
