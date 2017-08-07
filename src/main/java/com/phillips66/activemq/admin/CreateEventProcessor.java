package com.phillips66.activemq.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.phillips66.activemq.model.QueueCreateEvent;

public class CreateEventProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(CreateEventProcessor.class);

	private ProducerTemplate producerTemplate;

	private FabricConnections fabricConnections = new FabricConnections();
	private ObjectMapper objectMapper = new  ObjectMapper();  

	private String leaderNodes;
	private String username;
	private String password;
	private String createEventQueue;

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
		return createEventQueue;
	}

	public void setQueueCreateQueueName(String queueCreateQueueName) {
		this.createEventQueue = queueCreateQueueName;
	}

	public void init() {
		fabricConnections.setUsername(username);
		fabricConnections.setPassword(password);
	}

	@Override
	public void process(Exchange exchange) throws Exception {

		if (exchange != null && exchange.getIn() != null && exchange.getIn().getHeader("AMQ_AGENT_QUEUE_NAME") != null) {

			String queueName = exchange.getIn().getHeader("AMQ_AGENT_QUEUE_NAME",String.class);

			List<String> containerUrls = new ArrayList<String>();
			int searchCount = 0;
			
			// get the container urls from a leader
			String leaders[] = leaderNodes.split(",");
			Collections.shuffle(Arrays.asList(leaders));
			
			searchloop:
			while (searchCount < 3) {
				for (String leader : leaders) { // lets try each leader until one returns
					try {
						FabricAdapter fabricAdapter = fabricConnections.getConnection(leader);
						List<String> urls = fabricAdapter.getContainers();
						if (urls.size() > 0) {
							containerUrls = urls;
							break searchloop; // we have a list now
						}
					} catch (Exception ex) {
						logger.warn("couldn't get containers from leader, lets continue on checking other leaders: "+ ex.getMessage(), ex);
					}
				}
				searchCount++;
			}

			if (containerUrls.size() > 0) {
				// now lets contact each container and see if it's a amq instance,
				// if so create an queue create event for it
				for (String jmxUrl : containerUrls) {
					FabricAdapter container = fabricConnections.getConnection(jmxUrl);
					if (container.isMasterBroker()) {
						// create a message to create this queue
	
						QueueCreateEvent queueCreateEvent = new QueueCreateEvent();
						queueCreateEvent.setQueueName(queueName);
						queueCreateEvent.setContainerJmxUrl(jmxUrl);
	
						ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
						String json = ow.writeValueAsString(queueCreateEvent);
						producerTemplate.sendBody("amqtx:queue:" + createEventQueue, json);
	
					}
				}
			} else {
				throw new Exception("was not able to retrive container list");
			}

		}

	}
	
	public void shutdown() {
		fabricConnections.shutdown();
	}
}
