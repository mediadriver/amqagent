package com.phillips66.activemq.admin;

import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore // uncomment @Ignore to test locally
public class HttpAdapterTest {

	protected FabricAdapter adapter;
	private static final Logger logger = LoggerFactory.getLogger(HttpAdapterTest.class);
	
	@Before
	public void setUp() throws Exception {
		adapter = new HttpAdapter();
		Properties configProps = new Properties();
		configProps.setProperty("url", "http://localhost:8182/jolokia");
		configProps.setProperty("username", "admin");
		configProps.setProperty("password", "admin");
		configProps.setProperty("connectionTimeout", "5000");
		adapter.connect(configProps);
	}

	@After
	public void tearDown() throws Exception {
		adapter.shutdown();
		adapter = null;
	}

	@Test
	public void testGetContainers() throws Exception {
		List<String> urls = adapter.getContainers();
		for(String url : urls) {
			System.out.println("\t jmxUrl: " + url);
		}
	}

	@Test
	public void testIsMasterBroker() throws Exception  {
		boolean isMaster = adapter.isMasterBroker();
		System.out.println("\t isMaster: " + isMaster);	
	}

	@Test
	public void testCreateQueue() throws Exception {
		adapter.createQueue("Foo66");
	}

	@Test
	public void testGetQueues() throws Exception {
		List<String> queues = adapter.getQueues();
		for(String queue : queues) {
			logger.info("Queue: " + queue);
		}
	}
	
	@Test
	public void testAdapterClassName() throws Exception {
		logger.info("Classname: " + adapter.getClass().getName());
	}

}
