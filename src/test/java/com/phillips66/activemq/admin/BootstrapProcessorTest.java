package com.phillips66.activemq.admin;

import static org.junit.Assert.*;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore // Uncomment to perform local testing
public class BootstrapProcessorTest {

	private static BrokerService broker = null;
	private BootstrapProcessor bootstrap = null;
	
	@BeforeClass
	public static void setupClass() throws Exception {
		broker = new BrokerService();
		broker.addConnector("tcp://localhost:31313");
		broker.start();
		broker.waitUntilStarted();
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		broker.stop();
		broker.waitUntilStopped();
	}
	
	@Before
	public void setUp() throws Exception {
		bootstrap = new BootstrapProcessor();
		bootstrap.setLeaderNodes("http://localhost:8182/jolokia");
		bootstrap.setUsername("admin");
		bootstrap.setPassword("admin");
		bootstrap.setQueuePrefix("vSub.");
		bootstrap.init();
	}

	@After
	public void tearDown() throws Exception {
		bootstrap = null;
	}

	@Test
	public void testProcess() throws Exception {
		bootstrap.process(null);
	}

}
