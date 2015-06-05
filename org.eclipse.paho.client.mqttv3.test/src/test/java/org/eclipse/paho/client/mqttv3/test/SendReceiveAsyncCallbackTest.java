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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
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

  private static URI serverURI;
  private static MqttClientFactoryPaho clientFactory;
  private boolean testFinished = false;

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
  
  
  class onDisconnect implements IMqttActionListener {
	  
	  final String methodName = Utility.getMethodName();
	  private int testno;
	  
	  onDisconnect(int testno) {
		  this.testno = testno;
	  }

	  @Override
	  public void onSuccess(IMqttToken token) {
		log.info("testDisconnect: disconnect Success");
		
		if (testno == 1) {
			testFinished = true;
		}
		else {
		    Assert.fail("Wrong test numnber:" + methodName);
			testFinished = true;
		}
		
	  }

	  @Override
	  public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
		log.info("Disconnect failure, test no "+testno);
		testFinished = true;
	  }
	  
  }
  
  
  class onConnect implements IMqttActionListener {
	  
	  private int testno;
	  final String methodName = Utility.getMethodName();
	  
	  onConnect(int testno) {
		  this.testno = testno;
	  }

	  @Override
	  public void onSuccess(IMqttToken token) {
		log.info("testConnect: connect Success");
		
		try {
			if (testno == 1) {
				token.getClient().disconnect(null, new onDisconnect(1));
			}
			else {
			    Assert.fail("Wrong test numnber:" + methodName);
				testFinished = true;
			}
		}
		catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
		    Assert.fail("Failed:" + methodName + " exception=" + exception);
			testFinished = true;
		}
		
	  }

	@Override
	public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
		log.log(Level.SEVERE, "connect failure:", exception);
	    Assert.fail("Failed:" + methodName + " exception=" + exception);
		testFinished = true;
	}
	  
  }

  /**
   * Tests that a client can be constructed and that it can connect to and
   * disconnect from the service
   * 
   * @throws Exception
   */
  @Test
  public void testConnect() throws Exception {
    final String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    log.entering(className, methodName);

    IMqttAsyncClient mqttClient = null;
    try {
    		testFinished = false;
    		
    	    mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);

    		mqttClient.connect(null, null, new onConnect(1));
    		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
    		
    		int count = 0;
    		while (!testFinished && ++count < 20) {
    			Thread.sleep(500);
    		}
    		Assert.assertTrue("Callbacks not called", testFinished);
  		
        
    		testFinished = false;

    		mqttClient.connect(null, null, new onConnect(1));
    		log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
    		
    		count = 0;
    		while (!testFinished && ++count < 20) {
    			Thread.sleep(500);
    		}
    		Assert.assertTrue("Callbacks not called", testFinished);
    }
    catch (Exception exception) {
      log.log(Level.SEVERE, "caught exception:", exception);
      Assert.fail("Failed:" + methodName + " exception=" + exception);
    }
    finally {
      if (mqttClient != null) {
        log.info("Close...");
        mqttClient.close();
      }
    }

    log.exiting(className, methodName);
  }

}
