/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
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
 *   Cristiano De Alti - Eurotech (Initial contribution)
 *   James Sutton - IBM (Fixing Copyright header and adding getSocketFactory)
 */

package org.eclipse.paho.client.mqttv3.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test aims to run some basic SSL functionality tests of the MQTT client
 */

public class SSLSessionResumptionTest {

	static final Class<?> cclass = SSLSessionResumptionTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static String serverURI;
	private static String serverHost;
	private static int serverPort;

	/**
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);
			serverURI = "ssl://" + TestProperties.getServerURI().getHost() + ":" +TestProperties.getServerSSLPort();
			serverHost = TestProperties.getServerURI().getHost();
			serverPort = TestProperties.getServerSSLPort();

		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}
	}

	/**
	 * This test involves inspecting the SSL debug log and the Wireshark
	 * capture.
	 *
	 * Paho defeats the default SSL session caching which allows to have
	 * abbreviated SSL handshakes on following connections.
	 * 
	 * @throws Exception
	 */
	@Test(timeout=30000)
	public void testSSLSessionInvalidated() throws Exception {
		 //System.setProperty("javax.net.debug", "all");

		SSLSocketFactory factory = getSocketFactory();

		MqttConnectOptions options = new MqttConnectOptions();
		options.setServerURIs(new String[] { serverURI });
		options.setKeepAliveInterval(60);
		options.setSocketFactory(factory);

		MqttClient mqttClient = new MqttClient(serverURI, MqttClient.generateClientId());

		log.info("Connecting to: " + serverURI);
		mqttClient.connect(options);

		Thread.sleep(2000);

		log.info("Disconnetting...");
		mqttClient.disconnect();

		Thread.sleep(2000);

		log.info("Connecting again... Paho will not be able to perform an abbreviated SSL handshake");
		mqttClient.connect(options);

		Thread.sleep(2000);

		log.info("Disconnetting...");
		mqttClient.disconnect();

	}

	/**
	 * This test involves inspecting the SSL debug log and the Wireshark
	 * capture.
	 *
	 * By default, Java caches SSL sessions which allows to have abbreviated SSL
	 * handshakes on following connections.
	 * 
	 * @throws Exception
	 */
	@Test(timeout=10000)
	public void testSSLSessionCached() throws Exception {
		// System.setProperty("javax.net.debug", "all");

		SSLSocketFactory factory = getSocketFactory();

		log.info("Do handshake...");
		doHandshake(factory, serverHost, serverPort);

		log.info("Done! Redo handshake... An abbreviated SSL handshake will be performed");
		doHandshake(factory, serverHost, serverPort);

		log.info("Done!");
	}

	private static void doHandshake(SSLSocketFactory factory, String host, int port) {
		SSLSocket socket = null;
		try {
			socket = (SSLSocket) factory.createSocket(host, port);

			socket.startHandshake();

			SSLSessionContext ctx = socket.getSession().getSessionContext();
			if (ctx != null) {
				Enumeration<byte[]> ids = ctx.getIds();
				while (ids.hasMoreElements()) {
					byte[] id = ids.nextElement();
					log.info("Session ID: " + Arrays.toString(id));
					log.info("Cypher suite: " + ctx.getSession(id).getCipherSuite());
				}
			} else {
				log.info("null SSLSessionContext");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static SSLSocketFactory getSocketFactory() throws Exception {
		InputStream keyStoreStream = new FileInputStream(TestProperties.getClientKeyStore());
		SSLContext sslContext = SSLContext.getInstance("TLS");
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(keyStoreStream, TestProperties.getClientKeyStorePassword().toCharArray());
		trustManagerFactory.init(keyStore);
		sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
		return sslContext.getSocketFactory();
	}
}
