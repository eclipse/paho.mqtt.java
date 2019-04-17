/**
 * Copyright (c) 2011, 2017 IBM
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
 *   James Sutton - IBM Adding HTTPS Hostname verification tests.
 */

package org.eclipse.paho.client.mqttv3.test;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLHandshakeException;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.eclipse.paho.common.test.categories.MQTTV3Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.common.test.categories.SSLTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * This test aims to run some basic SSL functionality tests of the MQTT client
 */
@Category({OnlineTest.class, MQTTV3Test.class, SSLTest.class})
public class TLSHostnameVerificationTest {

	static final Class<?> cclass = TLSHostnameVerificationTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static String serverHost;
	private static int serverPort;
	private static String serverIP;
	private static int websocketPort;

	/**
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);
			// Configured to use the Paho Testing server localhost certificate
			serverHost = "localhost";
			serverIP = "127.0.0.1";
			serverPort = TestProperties.getServerSSLPort();
			websocketPort = TestProperties.getServerSSLPort();
			// certificateName = "server.crt";
			log.info("Setting SSL properties...");
			System.setProperty("javax.net.ssl.keyStore", TestProperties.getClientKeyStore());
			System.setProperty("javax.net.ssl.keyStorePassword", TestProperties.getClientKeyStorePassword());
			System.setProperty("javax.net.ssl.trustStore", TestProperties.getClientTrustStore());

		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}
	}

	/**
	 * This test checks that the Java 7 HTTPS Style Hostname Verification works with
	 * the Paho Client. This will verify that the Hostname we are connecting to
	 * matches the one recorded in the Certificate
	 * 
	 * @throws Exception
	 */
	@Test(timeout = 10000)
	public void testValidHTTPSStyleHostnameVerification() throws Exception {

		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setHttpsHostnameVerificationEnabled(true);
		String serverURI = "ssl://" + serverHost + ":" + serverPort;

		MqttClient mqttClient = new MqttClient(serverURI, MqttClient.generateClientId());

		log.info("Connecting to: " + serverURI);
		mqttClient.connect(connOpts);
		Assert.assertTrue(mqttClient.isConnected());

		log.info("Disconnetting...");
		mqttClient.disconnect();
		mqttClient.close();

	}

	/**
	 * This test checks that the Java 7 HTTPS Style Hostname Verification works with
	 * the Paho Client. This will verify that when connecting to a server via the IP
	 * address, the Hostname verification will fail
	 * 
	 * @throws Exception
	 */
	@Test(timeout = 10000)
	public void testInvalidHTTPSStyleHostnameVerification() throws Exception {

		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setHttpsHostnameVerificationEnabled(true);
		String serverIPURI = "ssl://" + serverIP + ":" + serverPort;

		MqttClient mqttClient = new MqttClient(serverIPURI, MqttClient.generateClientId());

		log.info("Connecting to: " + serverIPURI);
		try {
			mqttClient.connect(connOpts);
		} catch (Exception ex) {
			// We want to make sure that the returned exception is an SSLHandshakeException
			Assert.assertEquals(SSLHandshakeException.class, ex.getCause().getClass());
			log.info("Expected Exception thrown: " + ex.getCause().getClass());
		} finally {
			mqttClient.close();
		}
	}

	/**
	 * This test checks that the Java 7 HTTPS Style Hostname Verification works with
	 * the Paho Client. This will verify that the Hostname we are connecting to
	 * matches the one recorded in the Certificate
	 * 
	 * @throws Exception
	 */
	@Test(timeout = 10000)
	public void testValidWebSocketHTTPSStyleHostnameVerification() throws Exception {

		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setHttpsHostnameVerificationEnabled(true);
		String serverURI = "wss://" + serverHost + ":" + websocketPort + "/ws";

		MqttClient mqttClient = new MqttClient(serverURI, MqttClient.generateClientId());

		log.info("Connecting to: " + serverURI);
		mqttClient.connect(connOpts);
		Assert.assertTrue(mqttClient.isConnected());
		log.info("Client is connected.");


		log.info("Disconnetting...");
		mqttClient.disconnect();
		mqttClient.close();

	}

	/**
	 * This test checks that the Java 7 HTTPS Style Hostname Verification works with
	 * the Paho Client. This will verify that when connecting to a server via the IP
	 * address, the Hostname verification will fail
	 * 
	 * @throws Exception
	 */
	@Test(timeout = 10000)
	public void testInvalidWebSocketHTTPSStyleHostnameVerification() throws Exception {

		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setHttpsHostnameVerificationEnabled(true);
		String serverIPURI = "wss://" + serverIP + ":" + websocketPort + "/ws";

		MqttClient mqttClient = new MqttClient(serverIPURI, MqttClient.generateClientId());

		log.info("Connecting to: " + serverIPURI);
		try {
			mqttClient.connect(connOpts);
		} catch (Exception ex) {
			// We want to make sure that the returned exception is an SSLHandshakeException
			Assert.assertEquals(SSLHandshakeException.class, ex.getCause().getClass());
			log.info("Expected Exception thrown: " + ex.getCause().getClass());
		} finally {
			mqttClient.close();
		}
	}

}
