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

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test aims to run some basic SSL functionality tests of the MQTT client
 */

public class TLSHostnameVerificationTest {

	static final Class<?> cclass = TLSHostnameVerificationTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static String serverHost;
	private static int serverPort;
	private static String serverIP;
	private static String certificateName;
	private static int websocketPort;

	/**
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);
			// Overriding with iot.eclipse.org for the time being.
			
			serverHost = "iot.eclipse.org";
			serverIP = "198.41.30.241";
			serverPort = 8883;
			websocketPort = 443;
			
			certificateName = "iot.eclipse.org.crt";

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
		
		SSLSocketFactory socketFactory = getSocketFactory(certificateName);

		MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setHttpsHostnameVerificationEnabled(true);
        connOpts.setSocketFactory(socketFactory);
        String serverURI = "ssl://" + serverHost + ":" + serverPort;

		MqttClient mqttClient = new MqttClient(serverURI, MqttClient.generateClientId());

		log.info("Connecting to: " + serverURI);
		mqttClient.connect(connOpts);

		Thread.sleep(2000);

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
		
		SSLSocketFactory socketFactory = getSocketFactory(certificateName);

		MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setHttpsHostnameVerificationEnabled(true);
        connOpts.setSocketFactory(socketFactory);
        String serverIPURI =  "ssl://" + serverIP + ":" + serverPort;

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
		
		SSLSocketFactory socketFactory = getSocketFactory(certificateName);

		MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setHttpsHostnameVerificationEnabled(true);
        connOpts.setSocketFactory(socketFactory);
        String serverURI = "wss://" + serverHost + ":" + websocketPort + "/ws";

		MqttClient mqttClient = new MqttClient(serverURI, MqttClient.generateClientId());

		log.info("Connecting to: " + serverURI);
		mqttClient.connect(connOpts);

		Thread.sleep(2000);

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
		
		SSLSocketFactory socketFactory = getSocketFactory(certificateName);

		MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setHttpsHostnameVerificationEnabled(true);
        connOpts.setSocketFactory(socketFactory);
        String serverIPURI =  "wss://" + serverIP + ":" + websocketPort + "/ws";

        
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

	

	 private static SSLSocketFactory getSocketFactory(String certificateName) throws Exception {
	      // Load the certificate from src/test/resources and create a Certificate object
	  		InputStream certStream = cclass.getClassLoader().getResourceAsStream(certificateName);
	  		CertificateFactory certFactory = CertificateFactory.getInstance("X509");
	      Certificate certificate =  certFactory.generateCertificate(certStream);
	      SSLContext sslContext = SSLContext.getInstance("TLS");
	      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	  		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	  		keyStore.load(null);
	  		keyStore.setCertificateEntry("alias", certificate);
	  		trustManagerFactory.init(keyStore);
	  		sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
	  		return sslContext.getSocketFactory();
	    }
}
