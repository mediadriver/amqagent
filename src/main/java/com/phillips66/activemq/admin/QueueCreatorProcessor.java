package com.phillips66.activemq.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.phillips66.activemq.model.QueueCreateEvent;

public class QueueCreatorProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(QueueCreatorProcessor.class);
	
	private ProducerTemplate producerTemplate;
	private String leaderNodes;
	private String jmxUserName;
	private String jmxPassword;
	private String queueCreateQueueName; //probably a better name

	private JmxConnections jmxConnections = new JmxConnections();
	
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
		
		String body = exchange.getIn().getBody(String.class);
		if (body != null) {
			ObjectReader or = new ObjectMapper().reader(QueueCreateEvent.class);
			QueueCreateEvent queueCreateEvent = or.readValue(body);
			
			JmxAdapter jmxAdapter = jmxConnections.getConnection(queueCreateEvent.getContainerJmxUrl());
			jmxAdapter.createQueue(queueCreateEvent.getQueueName());
		}
	}
}
