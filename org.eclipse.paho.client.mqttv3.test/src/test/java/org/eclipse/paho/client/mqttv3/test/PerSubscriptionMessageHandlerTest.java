/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
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
 *    Ian Craggs - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;

public class PerSubscriptionMessageHandlerTest {
	
	  static final Class<?> cclass = PerSubscriptionMessageHandlerTest.class;
	  static final String className = cclass.getName();
	  static final Logger log = Logger.getLogger(className);

	  private static URI serverURI;
	  private static MqttClientFactoryPaho clientFactory;
	  
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
	    }
	    catch (Exception exception) {
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
	    }
	    catch (Exception exception) {
	      log.log(Level.SEVERE, "caught exception:", exception);
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
					  }
					  catch (InterruptedException e) {
						  // empty
					  }
				  }

				  if (messages.size() == 0) {
					  return null;
				  }
				  return messages.remove(0);
			  }
		  }

		  public void messageArrived(String topic, MqttMessage message) throws Exception {
				  
			  log.info("message arrived: '" + new String(message.getPayload()) + "' "+this.hashCode()+
					  " " + (message.isDuplicate() ? "duplicate" : ""));
			  
			  if (!message.isDuplicate()) {
				  synchronized (messages) {
					  messages.add(message);
					  messages.notifyAll();
				  }
			  }
		  }
	  }
	  
	  /**
	   * Basic test with 1 subscription for the synchronous client
	   * 
	   * @throws Exception
	   */
	  @Test
	  public void testSyncSubs1() throws Exception {
	    final String methodName = Utility.getMethodName();
	    LoggingUtilities.banner(log, cclass, methodName);
	    log.entering(className, methodName);        
    
	    listener mylistener = new listener();
	    IMqttClient mqttClient = clientFactory.createMqttClient(serverURI, methodName);
	    String mytopic = "PerSubscriptionTest/topic";
	    
	    mqttClient.connect();
	    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
	    
	    mqttClient.subscribe(mytopic, 2, mylistener);
	    
	    MqttMessage message = new MqttMessage();
	    message.setPayload("testSyncSubs1".getBytes());
	    mqttClient.publish(mytopic, message);
	    
	    log.info("Checking msg");
	    MqttMessage msg = mylistener.getNextMessage();
	    Assert.assertNotNull(msg);
	    Assert.assertEquals("testSyncSubs1", msg.toString());
	    
	    mqttClient.disconnect();
	    
	    mqttClient.close();
	    
	  }
	  
	  @Test
	  public void testAsyncSubs1() throws Exception {
	    final String methodName = Utility.getMethodName();
	    LoggingUtilities.banner(log, cclass, methodName);
	    log.entering(className, methodName);
	   
	    listener mylistener = new listener();
	    IMqttAsyncClient mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
	    String mytopic = "PerSubscriptionTest/topic";
	    
	    IMqttToken token = mqttClient.connect(null, null);
	    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
	    token.waitForCompletion();
	    
	    token = mqttClient.subscribe(mytopic, 2, mylistener);
	    token.waitForCompletion();
	    
	    MqttMessage message = new MqttMessage();
	    message.setPayload("testAsyncSubs1".getBytes());
	    token = mqttClient.publish(mytopic, message);
	    token.waitForCompletion();
	    
	    log.info("Checking msg");
	    MqttMessage msg = mylistener.getNextMessage();
	    Assert.assertNotNull(msg);
	    Assert.assertEquals("testAsyncSubs1", msg.toString());
	    
	    token = mqttClient.disconnect();
	    token.waitForCompletion();
	    
	    mqttClient.close();
	    
	  }
	  
	  /*
	   *  Check handlers still exist after reconnection non-cleansession
	   */
	  
	  @Test
	  public void testSyncCleanSessionFalse() throws Exception {
		    final String methodName = Utility.getMethodName();
		    LoggingUtilities.banner(log, cclass, methodName);
		    log.entering(className, methodName);        
	    
		    listener mylistener = new listener();
		    IMqttClient mqttClient = clientFactory.createMqttClient(serverURI, methodName);
		    String mytopic = "PerSubscriptionTest/topic";
		    
		    MqttConnectOptions opts = new MqttConnectOptions();
		    opts.setCleanSession(false);
		    
		    mqttClient.connect(opts);
		    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
		    
		    mqttClient.subscribe(mytopic, 2, mylistener);
		    
		    MqttMessage message = new MqttMessage();
		    message.setPayload("testSyncCleanSessionFalse".getBytes());
		    mqttClient.publish(mytopic, message);
		    
		    log.info("Checking msg");
		    MqttMessage msg = mylistener.getNextMessage();
		    Assert.assertNotNull(msg);
		    Assert.assertEquals("testSyncCleanSessionFalse", msg.toString());
		    
		    mqttClient.disconnect();
		    
		    /* subscription handler should still exist on reconnect */
		    
		    mqttClient.connect(opts);
		    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
		    
		    message = new MqttMessage();
		    message.setPayload("testSyncCleanSessionFalse1".getBytes());
		    mqttClient.publish(mytopic, message);
		    
		    log.info("Checking msg");
		    msg = mylistener.getNextMessage();
		    Assert.assertNotNull(msg);
		    Assert.assertEquals("testSyncCleanSessionFalse1", msg.toString());
		    
		    mqttClient.disconnect();	
		    
		    /* clean up by connecting cleansession */
		    mqttClient.connect();
		    mqttClient.disconnect();
		    
		    mqttClient.close();
	  }
	  
	  @Test
	  public void testAsyncCleanSessionFalse() throws Exception {
		    final String methodName = Utility.getMethodName();
		    LoggingUtilities.banner(log, cclass, methodName);
		    log.entering(className, methodName);
		   
		    listener mylistener = new listener();
		    IMqttAsyncClient mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
		    String mytopic = "PerSubscriptionTest/topic";
		    
		    MqttConnectOptions opts = new MqttConnectOptions();
		    opts.setCleanSession(false);
		    	    
		    IMqttToken token = mqttClient.connect(opts, null, null);
		    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
		    token.waitForCompletion();
		    
		    token = mqttClient.subscribe(mytopic, 2, mylistener);
		    token.waitForCompletion();
		    
		    MqttMessage message = new MqttMessage();
		    message.setPayload("testAsyncCleanSessionFalse".getBytes());
		    token = mqttClient.publish(mytopic, message);
		    token.waitForCompletion();
		    
		    log.info("Checking msg");
		    MqttMessage msg = mylistener.getNextMessage();
		    Assert.assertNotNull(msg);
		    Assert.assertEquals("testAsyncCleanSessionFalse", msg.toString());
		    
		    token = mqttClient.disconnect();
		    token.waitForCompletion();
		    
		    /* subscription handler should still exist on reconnect */
		    
		    token = mqttClient.connect(opts, null, null);
		    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
		    token.waitForCompletion();
		    
		    message = new MqttMessage();
		    message.setPayload("testAsyncCleanSessionFalse1".getBytes());
		    token = mqttClient.publish(mytopic, message);
		    token.waitForCompletion();
		    
		    log.info("Checking msg");
		    msg = mylistener.getNextMessage();
		    Assert.assertNotNull(msg);
		    Assert.assertEquals("testAsyncCleanSessionFalse1", msg.toString());
		    
		    token = mqttClient.disconnect();
		    token.waitForCompletion();
		    
		    /* clean up by connecting cleansession */
		    token = mqttClient.connect();
		    token.waitForCompletion();
		    token = mqttClient.disconnect();
		    token.waitForCompletion();
		    
		    mqttClient.close();
	  }
	  
	  /* check unsubscribe removes handlers */
	  /*
	  @Test
	  public void testSyncUnsubscribeRemove() throws Exception {
		    final String methodName = Utility.getMethodName();
		    LoggingUtilities.banner(log, cclass, methodName);
		    log.entering(className, methodName);        
	    
		    listener mylistener = new listener();
		    IMqttClient mqttClient = clientFactory.createMqttClient(serverURI, methodName);
		    String mytopic = "PerSubscriptionTest/topic";
		    
		    MqttConnectOptions opts = new MqttConnectOptions();
		    opts.setCleanSession(false);
		    
		    mqttClient.connect(opts);
		    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
		    
		    mqttClient.subscribe(mytopic, 2, mylistener);
		    
		    MqttMessage message = new MqttMessage();
		    message.setPayload("foo".getBytes());
		    mqttClient.publish(mytopic, message);
		    
		    log.info("Checking msg");
		    MqttMessage msg = mylistener.getNextMessage();
		    Assert.assertNotNull(msg);
		    Assert.assertEquals("foo", msg.toString());
		    
		    mqttClient.unsubscribe(mytopic); // unsubscribe will remove the message handler
		    mqttClient.subscribe(mytopic, 2); // but so will this
		    
		    message = new MqttMessage();
		    message.setPayload("foo1".getBytes());
		    mqttClient.publish(mytopic, message);
		    
		    log.info("Checking msg");
		    msg = mylistener.getNextMessage();
		    Assert.assertNotNull(msg);
		    Assert.assertEquals("foo1", msg.toString());
		    
		    mqttClient.disconnect();
		    
		    mqttClient.close();

	  }*/
	  
	  /* check can resubscribe after client object recreation */

}
