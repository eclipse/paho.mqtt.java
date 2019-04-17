/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corp.
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

package org.eclipse.paho.client.mqttv3.test.automaticReconnect;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.eclipse.paho.common.test.categories.MQTTV3Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.experimental.categories.Category;


@Category({OnlineTest.class, MQTTV3Test.class})
public class AutomaticReconnectTest{
	private static final Logger log = Logger.getLogger(AutomaticReconnectTest.class.getName());
	
	private static final MemoryPersistence DATA_STORE = new MemoryPersistence();

	
	private static URI serverURI;
	private String  clientId = "device-client-id";
	static ConnectionManipulationProxyServer proxy;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, AutomaticReconnectTest.class, methodName);
			serverURI = TestProperties.getServerURI();
			// Use 0 for the first time.
			proxy = new ConnectionManipulationProxyServer(serverURI.getHost(), serverURI.getPort(), 0);
			proxy.startProxy();
			while(!proxy.isPortSet()){
				Thread.sleep(0);
			}
			log.log(Level.INFO, "Proxy Started, port set to: " + proxy.getLocalPort());
		} catch (Exception exception) {
		      log.log(Level.SEVERE, "caught exception:", exception);
		      throw exception;
		    }
		
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info("Tests finished, stopping proxy");
		proxy.stopProxy();
		
	}
	

	/**
	 * Tests that if a connection is opened and then is lost that the client automatically reconnects
	 * @throws Exception
	 */
	@Test
	public void testAutomaticReconnectAfterDisconnect() throws Exception{
	    String methodName = Utility.getMethodName();
	    LoggingUtilities.banner(log, AutomaticReconnectTest.class, methodName);
    	MqttConnectOptions options = new MqttConnectOptions();
    	options.setCleanSession(true);
    	options.setAutomaticReconnect(true);
    	final MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
    	
    	proxy.enableProxy();
    	client.connect(options);
    	
    	boolean isConnected = client.isConnected();
    	log.info("First Connection isConnected: " + isConnected);
    	Assert.assertTrue(isConnected);
    	
    	proxy.disableProxy();
    	isConnected = client.isConnected();
    	log.info("Proxy Disconnect isConnected: " + isConnected);
    	Assert.assertFalse(isConnected);
    	
    	proxy.enableProxy();
    	// give it some time to reconnect
    	long currentTime = System.nanoTime();
    	long timeout = TimeUnit.SECONDS.toNanos(4);
    	while(client.isConnected() ==  false){
    		long now = System.nanoTime();
    		if((currentTime + timeout) < now){
    			log.warning("Timeout Exceeded");
    			break;
    		}
    		Thread.sleep(500);
    	}
    	isConnected = client.isConnected();
    	log.info("Proxy Re-Enabled isConnected: " + isConnected);
    	Assert.assertTrue(isConnected);
    	client.disconnect();
    	Assert.assertFalse(client.isConnected());
	}
	
	/**
	 * Tests that if a connection is opened and lost, that when the user calls reconnect() that the
	 * client will attempt to reconnect straight away
	 */
	@Test
	public void testManualReconnectAfterDisconnect() throws Exception {
	    String methodName = Utility.getMethodName();
	    LoggingUtilities.banner(log, AutomaticReconnectTest.class, methodName);
		MqttConnectOptions options = new MqttConnectOptions();
    	options.setCleanSession(true);
    	options.setAutomaticReconnect(true);
    	
    	final MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
    	
    	proxy.enableProxy();
    	client.connect(options);
    	
    	boolean isConnected = client.isConnected();
    	log.info("First Connection isConnected: " + isConnected);
    	Assert.assertTrue(isConnected);
    	
    	proxy.disableProxy();
    	isConnected = client.isConnected();
    	log.info("Proxy Disconnect isConnected: " + isConnected);
    	Assert.assertFalse(isConnected);
    	
    	proxy.enableProxy();
    	client.reconnect();
    	// give it some time to reconnect
    	Thread.sleep(4000);
    	isConnected = client.isConnected();
    	log.info("Proxy Re-Enabled isConnected: " + isConnected);
    	Assert.assertTrue(isConnected);
    	client.disconnect();
    	Assert.assertFalse(client.isConnected());  
	}


	/**
	 * Tests that if the initial connection attempt fails, that the automatic reconnect code does NOT
	 * engage.
	 */
	@Test
	public void testNoAutomaticReconnectWithNoInitialConnect() throws Exception {
		 String methodName = Utility.getMethodName();
		    LoggingUtilities.banner(log, AutomaticReconnectTest.class, methodName);
	    	MqttConnectOptions options = new MqttConnectOptions();
	    	options.setCleanSession(true);
	    	options.setAutomaticReconnect(true);
	    	options.setConnectionTimeout(15);
	    	final MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
	    	
	    	// Make sure the proxy is disabled and give it a second to close everything down
	    	proxy.disableProxy();
	    	try {
	    	client.connect(options);
	    	} catch (MqttException ex) {
	    		// Exceptions are good in this case!
	    	}
	    	boolean isConnected = client.isConnected();
	    	log.info("First Connection isConnected: " + isConnected);
	    	Assert.assertFalse(isConnected);
	    	
	    	// Enable The Proxy
	    	proxy.enableProxy();
	    	
	    	// Give it some time to make sure we are still not connected
	    	long currentTime = System.nanoTime();
	    	long timeout = TimeUnit.SECONDS.toNanos(4);
	    	while(client.isConnected() ==  false){
	    		long now = System.nanoTime();
	    		if((currentTime + timeout) < now){
	    			Assert.assertFalse(isConnected);
	    			break;
	    		}
	    		Thread.sleep(500);
	    	}
	    	isConnected = client.isConnected();
	    	log.info("Proxy Re-Enabled isConnected: " + isConnected);
	    	Assert.assertFalse(isConnected);

	}
}
