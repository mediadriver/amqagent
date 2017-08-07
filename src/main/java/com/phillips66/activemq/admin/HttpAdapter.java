package com.phillips66.activemq.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jolokia.client.BasicAuthenticator;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.jolokia.client.request.J4pSearchResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpAdapter implements FabricAdapter {

	private static final Logger logger = LoggerFactory.getLogger(HttpAdapter.class);
	private J4pClient client = null;
	
	public boolean connect(Properties configProps) {
		boolean connected = false;

		try {
			client = J4pClient.url(configProps.getProperty("url"))
					.user(configProps.getProperty("username"))
					.password(configProps.getProperty("password"))
					.authenticator(new BasicAuthenticator())
					.connectionTimeout(Integer.valueOf(configProps.getProperty("connectionTimeout", "5000")))
					.build();
			connected = true;
		} catch (Exception ex) {
			logger.error("Error during connect" + ex.getMessage(), ex);
			ex.printStackTrace();
		}
		return connected;
	}
	
	public void shutdown() {
		client = null;
	}
	
	public List<String> getContainers() throws Exception {

		ArrayList<String> jolokiaUrls = new ArrayList<String>();

		J4pExecRequest request = new J4pExecRequest("io.fabric8:type=Fabric", "containers()");
		J4pExecResponse response = client.execute(request);
		Object payload = response.getValue();
		if (payload == null || (payload != null && !payload.getClass().isAssignableFrom(JSONArray.class))) {
			throw new Exception("Invalid payload: " + (payload != null ? payload.getClass().getName() : null) + " JSONArray expected");
		}

		JSONArray array = (JSONArray) payload;
		for (Object item : array) {
			JSONObject container = (JSONObject) item;
			String jolokiaUrl = (String) container.get("jolokiaUrl");
			Boolean status = (Boolean) container.get("alive");
			if(status) {
				jolokiaUrls.add(jolokiaUrl);
				logger.info("Detected available container jolokiaUrl: " + jolokiaUrl);
			} else {
				logger.warn("Detected unavailable container jolokiaUrl: " + jolokiaUrl);
			}
		}
		return jolokiaUrls;
	}

	public boolean isMasterBroker() {
		boolean isMaster = true;
		List<String> brokerUrls = null;
		
		try {
			brokerUrls = getBrokerUrls(client);
		} catch (Exception e) {
			logger.error("Error detecting brokerUrls msg: " + e.getMessage(), e);
		}
		
		for(String brokerUrl : brokerUrls) {
			try {
				J4pReadRequest brokerRead = new J4pReadRequest(brokerUrl);
				J4pReadResponse brokerResponse = client.execute(brokerRead);
				Object isSlave = brokerResponse.getValue("Slave");
				if(isSlave != null && !isSlave.getClass().isAssignableFrom(Boolean.class)) {
					logger.warn("Invalid isSlave entry returned when string expected: " + isSlave.getClass().getName());
					continue;
				}
				logger.debug("Broker brokerUrl " + brokerUrl + " isSlave returned: " + isSlave);
				
				if(((Boolean)isSlave)) {
					isMaster = false;
				}
			} catch (Exception e) {
				logger.error("Error detecting broker master status brokerUrl: " + brokerUrl + " msg: " + e.getMessage(), e);
			}
		}
		return isMaster;
	}

	public void createQueue(String queueName) throws Exception {
		// http://localhost:8182/jolokia/exec/org.apache.activemq:brokerName=root,type=Broker/addQueue/Foo2
		
		for(String brokerUrl : getBrokerUrls(client)) {
			J4pExecRequest addQueueRequest = new J4pExecRequest(brokerUrl, "addQueue", queueName);
			try {
				client.execute(addQueueRequest);
			} catch (Exception e) {
				logger.error("Error adding queue: " + queueName + " to brokerUrl: " + brokerUrl + " msg: " + e.getMessage(), e);
				throw new Exception("Error adding queue: " + queueName + " to brokerUrl: " + brokerUrl + " msg: " + e.getMessage(), e);
			}
			logger.debug("Added queue to brokerUrl " + brokerUrl + " queueName: " + queueName);
		}
	}

	public List<String> getQueues() throws Exception {
		
		List<String> queues = new ArrayList<String>();

		for(String brokerUrl : getBrokerUrls(client)) {
			String brokerName = getBrokerNameFromJmxUrl(brokerUrl);
			J4pSearchRequest request = new J4pSearchRequest(String.format("org.apache.activemq:brokerName=%s,destinationName=*,destinationType=Queue,type=Broker", brokerName));
			J4pSearchResponse response = client.execute(request);
			Object payload = response.getValue();
			if (payload == null || (payload != null && !payload.getClass().isAssignableFrom(JSONArray.class))) {
				throw new Exception("Invalid payload: " + (payload != null ? payload.getClass().getName() : null) + " JSONArray expected");
			}

			JSONArray array = (JSONArray) payload;
			for (Object item : array) {
				if(item != null && !item.getClass().isAssignableFrom(String.class)) {
					logger.warn("Invalid queue entry returned when string expected: " + item.getClass().getName());
					continue;
				}
				logger.debug("Detected queue jmxUrl: " + (String)item);
				// "org.apache.activemq:brokerName=root,destinationName=vSub.Foo2,destinationType=Queue,type=Broker"
				String queueName = getDestinationNameFromJmxUrl((String)item);
				if(queueName != null) {
					queues.add(queueName);
					logger.debug("Detected queue: " + queueName);
				}
			}
		}
		return queues;
	}
	
	protected List<String> getBrokerUrls(J4pClient client) throws Exception {
		ArrayList<String> brokerUrls = new ArrayList<String>();
		
		J4pSearchRequest request = new J4pSearchRequest("org.apache.activemq:type=Broker,brokerName=*");
		J4pSearchResponse response = client.execute(request);
		Object payload = response.getValue();
		if (payload == null || (payload != null && !payload.getClass().isAssignableFrom(JSONArray.class))) {
			throw new Exception("Invalid payload: " + (payload != null ? payload.getClass().getName() : null) + " JSONArray expected");
		}

		JSONArray array = (JSONArray) payload;
		for (Object item : array) {
			if(item != null && !item.getClass().isAssignableFrom(String.class)) {
				logger.warn("Invalid broker entry returned when string expected: " + item.getClass().getName());
				continue;
			}
			logger.info("Detected broker: " + (String)item);
			brokerUrls.add((String) item);
		}
		return brokerUrls;
	}
 	
	protected String getDestinationNameFromJmxUrl(String jmxUrl) {
		if(jmxUrl == null || jmxUrl.trim().length() == 0) {
			return jmxUrl;
		}
		
		String[] splits = jmxUrl.split(",", 16);
		if(splits == null || splits.length < 2 || !splits[1].startsWith("destinationName=")) {
			logger.error("Invalid queue uri: " + jmxUrl);
			return null;
		}
		
		String[] destAttr = splits[1].split("=");
		if(destAttr == null || destAttr.length < 2) {
			logger.error("Invalid destinationName attribute: " + splits[1]);
			return null;
		}
		return destAttr[1];
	}
	
	protected String getBrokerNameFromJmxUrl(String jmxUrl) {
		if(jmxUrl == null || jmxUrl.trim().length() == 0) {
			return jmxUrl;
		}
		
		String[] splits = jmxUrl.split(",", 16);
		if(splits == null || splits.length < 2 || !splits[0].startsWith("org.apache.activemq:brokerName=")) {
			logger.error("Invalid broker uri: " + jmxUrl);
			return null;
		}
		
		String[] destAttr = splits[0].split("=");
		if(destAttr == null || destAttr.length < 2) {
			logger.error("Invalid brokerName attribute: " + splits[1]);
			return null;
		}
		return destAttr[1];
	}
}
