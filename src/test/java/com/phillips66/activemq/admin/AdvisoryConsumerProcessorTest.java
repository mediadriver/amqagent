package com.phillips66.activemq.admin;

import static org.junit.Assert.*;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.command.RemoveInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AdvisoryConsumerProcessorTest {

	private ActiveMQConnectionFactory factory;
	private Connection connection;
	private Session session;
	private MessageConsumer consumer;
	
	@Before
	public void setUp() throws Exception {
		factory = new ActiveMQConnectionFactory("admin", "admin", "tcp://localhost:61616");
		connection = factory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		consumer = session.createConsumer(session.createTopic("ActiveMQ.Advisory.Consumer.Queue.vSub.>"));
	}

	@After
	public void tearDown() throws Exception {
		consumer.close();
		session.close();
		connection.close();
	}

	@Test
	@Ignore
	public void test() throws Exception {
		Message msg = null;
		do {
			msg = consumer.receive(60000L);
			if(!(msg instanceof ActiveMQMessage)) {
				System.out.println("\t Recv'd non-ActiveMQ Message");
				continue;
			}
			
			ActiveMQMessage message = (ActiveMQMessage)msg;
			if (message != null && !message.isAdvisory()) {
				System.out.println("\t Recv'd non-ActiveMQ Advisory Message");
				continue;
			}
						
			if (message.getDataStructure() instanceof RemoveInfo) {
				String advisoryDestination = message.getDestination().toString();
				String vSubQueue = advisoryDestination.substring("topic://ActiveMQ.Advisory.Consumer.Queue.".length());
				System.out.println("\t Removed consumer: " + message.getDataStructure() + " dest: " + vSubQueue);
			}
		} while (msg != null);
		
	}

}
