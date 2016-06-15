/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corp.
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
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class SendReceiveAsyncCallbackTest {

	static final Class<?> cclass = SendReceiveAsyncTest.class;
	static final String className = cclass.getName();
	static final Logger log = Logger.getLogger(className);
	
	private final int messageCount = 5;
	private static URI serverURI;
	private static MqttClientFactoryPaho clientFactory;
	private boolean testFinished = false;
	private String topicFilter = "SendReceiveAsyncCallback/topic";
	private listener myListener = new listener();
	private onPublish myOnPublish = new onPublish(1);

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

	class onDisconnect implements IMqttActionListener {

		private int testno;

		onDisconnect(int testno) {
			this.testno = testno;
		}

		@Override
		public void onSuccess(IMqttToken token) {
			final String methodName = Utility.getMethodName();
			log.info("onDisconnect: test no " + testno + " " + methodName);

			if (testno == 1) {
				testFinished = true;
			} else {
				Assert.fail("Wrong test numnber:" + methodName);
				testFinished = true;
			}

		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			final String methodName = Utility.getMethodName();
			log.info("onDisconnect: test no " + testno + " " + methodName);
			testFinished = true;
		}

	}


	class listener implements IMqttMessageListener {

		ArrayList<MqttMessage> messages;

		public listener() {
			messages = new ArrayList<MqttMessage>();
		}

		public MqttMessage getNextMessage() {
			synchronized (messages) {
				if (messages.size() == 0) {
					try {
						messages.wait(1000);
					} catch (InterruptedException e) {
						// empty
					}
				}

				if (messages.size() == 0) {
					return null;
				}
				return messages.remove(0);
			}
		}

		public void messageArrived(String topic, MqttMessage message)
				throws Exception {

			log.info("message arrived: '" + new String(message.getPayload())
					+ "' " + this.hashCode() + " "
					+ (message.isDuplicate() ? "duplicate" : ""));

			if (!message.isDuplicate()) {
				synchronized (messages) {
					messages.add(message);
					messages.notifyAll();
				}
			}
		}
	}
	
	 
	
	class onPublish implements IMqttActionListener {

		private int testno;
		private int count;

		onPublish(int testno) {
			this.testno = testno;
			count = 0;
		}

		@Override
		public void onSuccess(IMqttToken token) {
			final String methodName = Utility.getMethodName();
			log.info(methodName + ": onPublish");

			if (testno == 1) {
				try {
					if (++count < messageCount) {
						token.getClient().publish(topicFilter, "my data".getBytes(), 2, false, null, myOnPublish);
					}
					else {
						log.info(methodName + ": all messages published");
						testFinished = true;
					}
				}
				catch (Exception exception) {
					log.log(Level.SEVERE, "caught exception:", exception);
					Assert.fail("Failed:" + methodName + " exception=" + exception);
				}
			} else {
				Assert.fail("Wrong test numnber:" + methodName);
				testFinished = true;
			}

		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			final String methodName = Utility.getMethodName();
			log.info("onPublish failure, test no " + testno + " " + methodName);
			testFinished = true;
		}

	}


	class onSubscribe implements IMqttActionListener {

		private int testno;

		onSubscribe(int testno) {
			this.testno = testno;
		}

		@Override
		public void onSuccess(IMqttToken token) {
			final String methodName = Utility.getMethodName();
			log.info(methodName + ": onSubscribe");

			if (testno == 1) {
				try {
					token.getClient().publish(topicFilter, "my data".getBytes(), 2, false, myListener, myOnPublish);
				}
				catch (Exception exception) {
					log.log(Level.SEVERE, "caught exception:", exception);
					Assert.fail("Failed:" + methodName + " exception=" + exception);
				}
			} else {
				Assert.fail("Wrong test numnber:" + methodName);
				testFinished = true;
			}

		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			final String methodName = Utility.getMethodName();
			log.info("Subscribe failure, test no " + testno + " " + methodName);
			testFinished = true;
		}

	}

	class onConnect implements IMqttActionListener {

		private int testno;

		onConnect(int testno) {
			this.testno = testno;
		}

		@Override
		public void onSuccess(IMqttToken token) {
			final String methodName = Utility.getMethodName();
			log.info(methodName + ": onConnect");

			try {
				if (testno == 1) {
					token.getClient().subscribe(topicFilter, 2, null, new onSubscribe(1), myListener);
				} else {
					Assert.fail("Wrong test numnber:" + methodName);
					testFinished = true;
				}
			} catch (Exception exception) {
				log.log(Level.SEVERE, "caught exception:", exception);
				Assert.fail("Failed:" + methodName + " exception=" + exception);
				testFinished = true;
			}

		}

		@Override
		public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
			final String methodName = Utility.getMethodName();
			log.log(Level.SEVERE, "connect failure:", exception);
			Assert.fail("onConnect:" + methodName + " exception=" + exception);
			testFinished = true;
		}

	}

	/**
	 * Connect, subscribe and publish
	 * 
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		final String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		log.entering(className, methodName);

		IMqttAsyncClient mqttClient = null;
		try {
			testFinished = false;

			mqttClient = clientFactory.createMqttAsyncClient(serverURI,
					methodName);

			mqttClient.connect(null, new onConnect(1));
			log.info("Connecting...(serverURI:" + serverURI + ", ClientId:"+mqttClient.getClientId()+")");

			int count = 0;
			while (!testFinished && ++count < 80) {
				Thread.sleep(500);
			}
			log.info(methodName + ": all Messages published "+testFinished);
			Assert.assertTrue("Callbacks not called", testFinished);
			
			count = 0;
			while (myListener.messages.size() < messageCount && ++count < 10) {
				Thread.sleep(500);
			}
			log.info(methodName + ": all messages received "+ (myListener.messages.size() == messageCount));
			Assert.assertTrue("All messages received", myListener.messages.size() == messageCount);

			testFinished = false;
			
			log.info("Disconnecting...(serverURI:" + serverURI + ", ClientId:"+mqttClient.getClientId()+")");
			mqttClient.disconnect(30000, null, new onDisconnect(1));
			
			count = 0;
			while (!testFinished && ++count < 80) {
				Thread.sleep(500);
			}
			Assert.assertTrue("Callbacks not called", testFinished);

		} catch (Exception exception) {
			log.info("Exception thrown"+exception);
			log.log(Level.SEVERE, "caught exception:", exception);
			Assert.fail("Failed:" + methodName + " exception=" + exception);
		} finally {
			if (mqttClient != null) {
				log.info("Close...");
				mqttClient.close();
			}
		}

		log.exiting(className, methodName);
	}

}
