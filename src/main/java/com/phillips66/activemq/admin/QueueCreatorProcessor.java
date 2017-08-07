package com.phillips66.activemq.admin;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.phillips66.activemq.model.QueueCreateEvent;

public class QueueCreatorProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(QueueCreatorProcessor.class);
	
	private ProducerTemplate producerTemplate;
	private String leaderNodes;
	private String username;
	private String password;
	private String queueCreateQueueName;

	private ObjectMapper objectMapper = new  ObjectMapper();
	private FabricConnections fabricConnections = new FabricConnections();
	
	public ProducerTemplate getProducerTemplate() {
		return producerTemplate;
	}
	public void setProducerTemplate(ProducerTemplate producerTemplate) {
		this.producerTemplate = producerTemplate;
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

	public String getQueueCreateQueueName() {
		return queueCreateQueueName;
	}
	public void setQueueCreateQueueName(String queueCreateQueueName) {
		this.queueCreateQueueName = queueCreateQueueName;
	}

	public void init() {
		fabricConnections.setUsername(username);
		fabricConnections.setPassword(password);
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		
		String body = exchange.getIn().getBody(String.class);
		if (body != null) {
			ObjectReader or = objectMapper.reader(QueueCreateEvent.class);
			QueueCreateEvent queueCreateEvent = or.readValue(body);
			
			FabricAdapter fabricAdapter = fabricConnections.getConnection(queueCreateEvent.getContainerJmxUrl());
			fabricAdapter.createQueue(queueCreateEvent.getQueueName());
		}
	}
	
	public void shutdown() {
		fabricConnections.shutdown();
	}
}
