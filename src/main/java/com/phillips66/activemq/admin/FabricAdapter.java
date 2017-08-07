package com.phillips66.activemq.admin;

import java.util.List;
import java.util.Properties;

public interface FabricAdapter {
	boolean connect(Properties properties);
	void shutdown();
	void createQueue(String queueName) throws Exception;
	boolean isMasterBroker() throws Exception;
	public List<String> getContainers() throws Exception;
	public List<String> getQueues() throws Exception;
}
