package org.eclipse.paho.client.mqttv3.test;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.test.client.MqttClientFactoryPaho;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;

public class PerSubscriptionMessageHandlerTest {
	
	  static final Class<?> cclass = SendReceiveAsyncTest.class;
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
	  
	  /**
	   * Tests that a client can be constructed and that it can connect to and
	   * disconnect from the service
	   * 
	   * @throws Exception
	   */
	  @Test
	  public void testSubs1() throws Exception {
	    final String methodName = Utility.getMethodName();
	    LoggingUtilities.banner(log, cclass, methodName);
	    log.entering(className, methodName);
	    
	    class listener implements IMqttMessageListener {
	    	
	    	public int messages = 0;
	    	
	    	public void messageArrived(String topic, MqttMessage message) throws Exception {
	    		messages++;
	    	}
	    	
	    };
	   
	    listener mylistener = new listener();
	    IMqttAsyncClient mqttClient = clientFactory.createMqttAsyncClient(serverURI, methodName);
	    String mytopic = "PerSubscriptionTest/topic";
	    
	    IMqttToken token = mqttClient.connect(null, null);
	    log.info("Connecting...(serverURI:" + serverURI + ", ClientId:" + methodName);
	    token.waitForCompletion();
	    
	    token = mqttClient.subscribe(mytopic, 2, mylistener);
	    token.waitForCompletion();
	    
	    MqttMessage message = new MqttMessage();
	    token = mqttClient.publish(mytopic, message);
	    
	    assert(mylistener.messages == 1);
	    
	  }

}
