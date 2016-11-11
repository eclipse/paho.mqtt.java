/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *   
 * Contributors:
 *    Patrick Leong - export Ian Craggs's Python client_test.py to Java. 
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver;
import org.eclipse.paho.client.mqttv3.test.utilities.MqttV3Receiver.ReceivedMessage;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Conformant test for MQTT 3.1.1 protocol.
 */
public class ConformantTest {

	static final Class<?> cclass = ConformantTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);
	private static String[] topics = { "TopicA", "TopicA/B", "Topic/C",
			"TopicA/C", "/TopicA" };
	private static String[] wildTopics = { "TopicA/+", "+/C", "#", "/#", "/+",
			"+/+", "TopicA/#" };
	private static String nosubscribe_topic = "nosubscribe";

	private static URI serverURI;
	private static MqttClientFactoryPaho clientFactory;
	private static String[] clientid = { "javaclientid", "javaclientid2" };
	private IMqttClient client = null, bClient = null;

	/**
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);

			serverURI = TestProperties.getServerURI();
			clientFactory = new MqttClientFactoryPaho();
			clientFactory.open();

			cleanUpAllStatus();
		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}
	}

	/**
	 * @throws Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		try {
			if (clientFactory != null) {
				clientFactory.close();
				clientFactory.disconnect();
			}
		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
		}
	}

	@Before
	public void init() throws Exception {
		client = clientFactory.createMqttClient(serverURI, clientid[0]);
		client.setProtocolVersion(MqttProtocolVersion.V3_1_1);
		bClient = clientFactory.createMqttClient(serverURI, clientid[1]);
		bClient.setProtocolVersion(MqttProtocolVersion.V3_1_1);
	}

	@After
	public void clean() throws MqttException {
		if (client != null) {
			if (client.isConnected()) {
				client.disconnect();
			}
			client.close();
		}
		if (bClient != null) {
			if (bClient.isConnected()) {
				bClient.disconnect();
			}
			bClient.close();
		}
	}

	/**
	 * Clean up all status, retained messages
	 * 
	 * @throws Exception
	 */
	public static void cleanUpAllStatus() throws Exception {
		// Clean all clients status
		for (int i = 0; i < clientid.length; i++) {
			IMqttClient client = clientFactory.createMqttClient(serverURI,
					clientid[i]);
			client.setProtocolVersion(MqttProtocolVersion.V3_1_1);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			client.connect(options);
			Thread.sleep(100);
			client.disconnect();
			Thread.sleep(100);
		}

		// Clean all retain subscriptions.
		IMqttClient client = clientFactory.createMqttClient(serverURI,
				clientid[0]);
		client.setProtocolVersion(MqttProtocolVersion.V3_1_1);
		MqttV3Receiver receiver = new MqttV3Receiver(client,
				LoggingUtilities.getPrintStream());
		client.setCallback(receiver);
		client.connect();
		client.subscribe("#", 0);
		Thread.sleep(3000);
		List<ReceivedMessage> messageList = receiver
				.getReceivedMessagesInCopy();
		for (ReceivedMessage receivedMessage : messageList) {
			if (receivedMessage.message.isRetained()) {
				log.info("Deleting retained message for topic.");
				client.publish(receivedMessage.topic, new byte[0], 0, true);
			}
		}
		client.disconnect();
		client.close();
		Thread.sleep(100);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void basicTest() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		String clientId = "basicTest";
		String[] messages = { "qos 0", "qos 1", "qos 2" };
		int[] qoses = { 0, 1, 2 };

		log.info("Assigning callback...");
		MqttV3Receiver receiver = new MqttV3Receiver(client,
				LoggingUtilities.getPrintStream());
		client.setCallback(receiver);

		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
				+ clientId);
		client.connect();

		log.info("Subscribing to..." + topics[0]);

		client.subscribe(topics[0], 2);

		log.info("Publishing to..." + topics[0]);
		for (int i = 0; i < 3; i++) {
			client.publish(topics[0], messages[i].getBytes(), qoses[i], false);
		}
		Thread.sleep(2000);

		// Verify "TopicA/B"
		Assert.assertEquals(3, receiver.receivedMessageCount());
	}

	@Test
	public void retained_message_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		String[] messages = { "qos 0", "qos 1", "qos 2" };
		int[] qoses = { 0, 1, 2 };
		try {
			// Retained messages.
			log.info("Assigning callback...");
			MqttV3Receiver receiver = new MqttV3Receiver(client,
					LoggingUtilities.getPrintStream());
			client.setCallback(receiver);

			log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
					+ clientid[0]);
			client.connect();

			log.info("Publishing retained messages to...");
			for (int i = 0; i < 3; i++) {
				client.publish(topics[i + 1], messages[i].getBytes(), qoses[i],
						true);
			}

			Thread.sleep(500);
			// Subscribe "+/+"
			log.info("Subscribing to..." + wildTopics[5]);
			client.subscribe(wildTopics[5], 2);
			Thread.sleep(2000);

			// Verify
			List<ReceivedMessage> receivedMessages = receiver
					.getReceivedMessagesInCopy();
			Assert.assertEquals("Should receive 3 messages", 3,
					receivedMessages.size());
			client.disconnect();
		} finally {
			cleanUpAllStatus();
		}
	}

	/**
	 * @throws Exception
	 */
	/*@Test
	public void will_message_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		IMqttClient cClient = null;
		try {
			log.info("Assigning callback...");
			MqttV3Receiver receiver = new MqttV3Receiver(client,
					LoggingUtilities.getPrintStream());

			// Connect clientA
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			options.setWill(topics[2], "client not disconnected".getBytes(), 0,
					false);
			options.setKeepAliveInterval(200);
			log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
					+ clientid[0] + "with options (" + options + ")");
			client.setCallback(receiver);
			client.connect(options);

			MqttConnectOptions mqttOptions2 = new MqttConnectOptions();
			mqttOptions2.setCleanSession(false);
			MqttV3Receiver receiverB = new MqttV3Receiver(bClient,
					LoggingUtilities.getPrintStream());
			bClient.setCallback(receiverB);
			bClient.connect(mqttOptions2);
			bClient.subscribe(topics[2], 2);

			try {
				client.disconnect();
			} catch(MqttException m) {
				log.info(m.toString());
			}

			ReceivedMessage msg = receiverB.receiveNext(1000);
			Assert.assertNotNull(" Should have one will message.", msg);
		} catch (MqttException mqttException) {
			mqttException.printStackTrace();
		} finally {
			if (cClient != null) {
				if (cClient.isConnected()) {
					cClient.disconnect();
				}
				cClient.close();
			}
		}
	}*/

	/**
	 * Test when client id is zero length
	 * 
	 * @throws Exception
	 */
	@Test
	public void zero_length_clientId_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		boolean fails = false;
		client = clientFactory.createMqttClient(serverURI, "");
		client.setProtocolVersion(MqttProtocolVersion.V3_1_1);
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(false);
		try {
			client.connect(options);
			fails = false;
		} catch (MqttException e) {
			e.printStackTrace();
			fails = true;
		}
		Assert.assertTrue("Clean session = false should be rejected.", fails);

		options.setCleanSession(true);
		try {
			client.connect(options);
			fails = false;
		} catch (MqttException e) {
			e.printStackTrace();
			fails = true;
		}
		Assert.assertFalse("Clean session = true should be accepted.", fails);
	}

	@Test
	public void offline_message_queueing_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		String[] messages = { "qos 0", "qos 1", "qos 2" };
		int[] qoses = { 0, 1, 2 };

		log.info("Assigning callback...");
		MqttV3Receiver receiver = new MqttV3Receiver(client,
				LoggingUtilities.getPrintStream());
		client.setCallback(receiver);

		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
				+ clientid[0]);
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(false);
		client.connect(options);

		log.info("Subscribing to..." + wildTopics[5]);
		client.subscribe(wildTopics[5], 2);
		client.disconnect();

		bClient.connect();
		log.info("Publishing to..." + wildTopics[5]);
		for (int i = 0; i < 3; i++) {
			bClient.publish(topics[i], messages[i].getBytes(), qoses[i], false);
		}
		bClient.disconnect();

		client.connect(options);
		Thread.sleep(1000);
		client.disconnect();

		// Verify, only have two messages, QoS 0 should be ignored.
		Assert.assertEquals(
				"Should have two messages, QoS 0 should be ignored", 2,
				receiver.receivedMessageCount());
	}

	/**
	 * overlapping subscriptions. When there is more than one matching
	 * subscription for the same client for a topic, the server may send back
	 * one message with the highest QoS of any matching subscription, or one
	 * message for each subscription with a matching QoS.
	 */
	@Test
	public void overlapping_subscriptions_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		log.info("Assigning callback...");
		MqttV3Receiver receiver = new MqttV3Receiver(client,
				LoggingUtilities.getPrintStream());
		client.setCallback(receiver);

		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
				+ clientid[0]);
		client.connect();

		log.info("Subscribing to..." + wildTopics[6]);
		// "TopicA/#", "TopicA/+"
		client.subscribe(new String[] { wildTopics[6], wildTopics[0] },
				new int[] { 2, 1 });
		// Publish to "TopicA/C"
		client.publish(topics[3], "overlapping topic filters".getBytes(), 2,
				false);
		Thread.sleep(1000);

		// Verify
		List<ReceivedMessage> receivesMessages = receiver
				.getReceivedMessagesInCopy();
		int length = receivesMessages.size();
		Assert.assertTrue("Received Messages should be 1 or 2.", length >= 1
				&& length <= 2);

		if (length == 1) {
			log.info("This server is publishing one message for all matching overlapping subscriptions, not one for each.");
			Assert.assertEquals(2, receivesMessages.get(0).message.getQos());
		} else { // length == 2
			log.info("This server is publishing one message per each matching overlapping subscription.");
			int message1QoS = receivesMessages.get(0).message.getQos();
			int message2QoS = receivesMessages.get(1).message.getQos();
			Assert.assertTrue((message1QoS == 1 && message2QoS == 2)
					|| (message1QoS == 2 && message2QoS == 1));
		}

		log.info("Disconnecting...");
		client.disconnect();
	}

	/**
	 * keepalive processing. We should be kicked off by the server if we don't
	 * send or receive any data, and don't send any pings either.
	 */
	/*@Test
	public void keepalive_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		String payload = "keepalive expiry";
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setKeepAliveInterval(0);
		options.setWill(topics[4], payload.getBytes(), 2, false);

		client.setCallback(new MqttV3Receiver(client, LoggingUtilities
				.getPrintStream()));
		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
				+ clientid[0] + "with options " + options);
		client.connect(options);

		MqttV3Receiver receiver = new MqttV3Receiver(bClient,
				LoggingUtilities.getPrintStream());
		bClient.setCallback(receiver);
		options.setKeepAliveInterval(5);
		bClient.connect(options);
		bClient.subscribe(topics[4], 2);
		//hang the client for a time
		bClient.disconnect();

		ReceivedMessage message = receiver.receiveNext(500);
		Assert.assertNotNull(message);
		Assert.assertArrayEquals(payload.getBytes(),
				message.message.getPayload());
	}*/

	/**
	 * redelivery on reconnect. When a QoS 1 or 2 exchange has not been
	 * completed, the server should retry the appropriate MQTT packets.
	 */
	/*public void redelivery_on_reconnect_test() {
		// Pause? Resume?
	}*/

	/**
	 * Subscribe failure. A new feature of MQTT 3.1.1 is the ability to send
	 * back negative reponses to subscribe requests. One way of doing this is to
	 * subscribe to a topic which is not allowed to be subscribed to.
	 */
	/*@Test
	public void subscribe_failure_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		boolean succeeded = true;
		try {
			log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
					+ clientid[0]);
			client.connect();

			log.info("Subscribing to..." + nosubscribe_topic);
			client.subscribe(nosubscribe_topic, 2);
			Thread.sleep(200); // Wait for all retained messages, hopefully
			client.publish("$" + topics[1], new byte[0], 1, false);
			Thread.sleep(200);

			// TODO: No subscribe callback?
			// assert callback.subscribeds[0][1][0] == 0x80,
			// "return code should be 0x80 %s" % callback.subscribeds
		} catch (Exception exception) {
			exception.printStackTrace();
			succeeded = false;
		}
		Assert.fail();// Not implemented.
	}*/

	/**
	 * $ topics. The specification says that a topic filter which starts with a
	 * wildcard does not match topic names that begin with a $. Publishing to a
	 * topic which starts with a $ may not be allowed on some servers (which is
	 * entirely valid), so this test will not work and should be omitted in that
	 * case.
	 * 
	 * @throws Exception
	 */
	@Test
	public void dollar_topics_test() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		log.info("Assigning callback...");
		MqttV3Receiver receiver = new MqttV3Receiver(client,
				LoggingUtilities.getPrintStream());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setKeepAliveInterval(0);
		options.setCleanSession(true);
		client.setCallback(receiver);

		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"
				+ clientid[0]);
		client.connect(options);

		log.info("Subscribing to..." + topics[0]);
		client.subscribe(wildTopics[5], 2);
		Thread.sleep(1000); // Wait for all retained messages, hopefully
		client.publish("$" + topics[1], "".getBytes(), 1, false);
		Thread.sleep(200);

		// Verify
		ReceivedMessage message = receiver.receiveNext(500);
		Assert.assertNull("Should not receive any messsage for '$' topics",
				message);
	}
	
}
