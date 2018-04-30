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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
 
package org.eclipse.paho.jmeclient.mqttv3.sampleGPIO;

import java.io.IOException;
import javax.microedition.midlet.MIDlet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.oracle.deviceaccess.gpio.GPIOManager;
import com.oracle.deviceaccess.gpio.GPIOPin;
import com.oracle.deviceaccess.gpio.GPIOPort;
import com.oracle.deviceaccess.gpio.PinEvent;
import com.oracle.deviceaccess.gpio.PinListener;

/**
 * A sample GPIO application that demonstrates how to use the Java ME MQTT v3 Client blocking api.
 * 
 * Button 1 Subscribes to the specified topic and waits for messages indefinitely
 * Button 2 Publishes to the specified topic string and disconnects
 * Button 3 Disconnects the Subscription and client created in Button 1
 * 
 * It can be run from an MIDP Emulator or a physical device
 *  
 *  There are three versions of the sample that implement the same features
 *  but do so using using different programming styles:
 *  <ol>
 *  <li>GPIOSample (this one) which uses the API which blocks until the operation completes</li>
 *  <li>GPIOSampleAsyncWait shows how to use the asynchronous API with waiters that block until 
 *  an action completes</li>
 *  <li>GPIOSampleAsyncCallBack shows how to use the asynchronous API where events are
 *  used to notify the application when an action completes<li>
 *  </ol>
 *       
 */ 
public class GPIOSample extends MIDlet implements MqttCallback {
    static final int LED1_PIN_ID = 1;
    static final String LED1_PIN_NAME = "LED 1";
    
    static final int LED2_PIN_ID = 2;
    static final String LED2_PIN_NAME = "LED 2";
    
    static final int BUTTON1_PIN_ID = 5; 
    static final String BUTTON1_PIN_NAME = "BUTTON 1";
    
    static final int BUTTON2_PIN_ID = 6;
    static final String BUTTON2_PIN_NAME = "BUTTON 2";
    
    static final int BUTTON3_PIN_ID = 7;
    static final String BUTTON3_PIN_NAME = "BUTTON 3";
    
    static final int LED_PORT_ID = 1;
    static final String LED_PORT_NAME = "LEDS";
    
    boolean bFirst = false;
    boolean loopFlag = true; 
    
    private GPIOPin led1 = null;
    private GPIOPin led2 = null;
    private GPIOPort ledPort = null;
    private GPIOPin button1 = null;
    private GPIOPin button2 = null;
    private GPIOPin button3 = null;
    
    // Private instance variables
    private MqttClient client;
    private MqttClient pubClinet;
    private MqttConnectOptions conOpt;
	private int qos = 2;
	private String broker = "localhost";
	private int port = 1883;
	private int sslport = 8890;
	private boolean SSL = false;
    
	/**
	 * Signals the MIDlet that it has entered the Active state
	 */
    public void startApp() {
        if(bFirst == false) {
            System.out.println("Starting GPIO Sample");
            try {
                led1 = GPIOManager.getPin(LED1_PIN_ID);
                led2 = GPIOManager.getPin(LED2_PIN_ID);
                ledPort = GPIOManager.getPort(LED_PORT_ID);
                button1 = GPIOManager.getPin(BUTTON1_PIN_ID);
                button2 = GPIOManager.getPin(BUTTON2_PIN_ID);    
                button3 = GPIOManager.getPin(BUTTON3_PIN_ID);     
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Get pin and port fail");
                return;
            }
            
            System.out.println("set listener for button 1,2,3");
            try {
                button1.setInputListener(button1Listener);
                button2.setInputListener(button2Listener);
                button3.setInputListener(button3Listener);
            } catch (Exception ex) {
                ex.printStackTrace();
            } 

            bFirst = true;
        } else {
            System.out.println("GPIO Sample is already started..");
        }
    }
    
    /**
     * Signals the MIDlet to enter the Paused state
     */
    public void pauseApp() {
    }
    
    /**
     * Signals the MIDlet to terminate and enter the Destroyed state
     */
    public void destroyApp(boolean unconditional) {
        bFirst = false;
        if(led1 != null){
            try {
                led1.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            led1 = null;
        }
        if(led2 != null){
            try {
                led2.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            led2 = null;
        }
        if(ledPort != null){
            try {
                ledPort.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            ledPort = null;
        }
        if(button1 != null){
            try {
                button1.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            button1 = null;
        }
        if(button3 != null){
            try {
                button2.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            button2 = null;
        }
        if(button3 != null){
            try {
                button3.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            button3 = null;
        }
    }
    
    /**
     * Detects GPIO pin value changes
     */
    PinListener button1Listener = new PinListener(){

        public void valueChanged(PinEvent event) {
            GPIOPin pin = event.getPin();
            System.out.println("listener1  "+ pin.getID());
            if(pin.getID() == BUTTON1_PIN_ID ){
                try {
                  subscribe();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } 
            }            
        }
    };
    
    /**
     * Detects GPIO pin value changes
     */
    PinListener button2Listener = new PinListener(){
        public void valueChanged(PinEvent event) {
            GPIOPin pin = event.getPin();
            System.out.println("listener2  "+ pin.getID());
            if(pin.getID() == BUTTON2_PIN_ID ){
                try {
                    publish();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } 
            }            
        }
    };
    
    /**
     * Detects GPIO pin value changes
     */
    PinListener button3Listener = new PinListener(){
        public void valueChanged(PinEvent event) {
            GPIOPin pin = event.getPin();
            System.out.println("listener2  "+ pin.getID());
            if(pin.getID() == BUTTON3_PIN_ID ){
                try {
                    disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } 
            }            
        }

    };
    
    /**
     * Publish / send a message to an MQTT server
     * @throws MqttException, IOException
     */
    protected void publish() throws MqttException, IOException {
    	String payload = "Message from blocking MQTTv3 Java client sample";
		String clientId = "SampleJavaV3_pubMIDP";
		String pubTopic = "Sample/Java/v3";
		
		String url = "tcp://" + broker + ":" + port;
		if (SSL) {
			url = "ssl://"  + broker + ":" + sslport;
		}

		pubClinet = new MqttClient(url, clientId);
		pubClinet.setCallback(this);
		pubClinet.connect();
		
    	// Create and configure a message
   		MqttMessage message = new MqttMessage(payload.getBytes());
    	message.setQos(qos);
 
    	// Send the message to the server, control is not returned until
    	// it has been delivered to the server meeting the specified
    	// quality of service.
    	pubClinet.publish(pubTopic, message);
    	
    	// Disconnect the client from the server
    	pubClinet.disconnect();
    	
    	// Reassign the pubClient to null
    	pubClinet = null;
    }

    /**
     * Subscribe to a topic on an MQTT server
     * Once subscribed this method waits for the messages to arrive from the server 
     * that match the subscription. It continues listening for messages until pin 3
     * is pressed
     * @throws IOException, MqttException
     */
	protected void subscribe() throws IOException, MqttException {
		String clientId = "SampleJavaV3_subMIDP";
		String subTopic = "Sample/#";
		String url = "tcp://" + broker + ":" + port;
		
		if (SSL) {
			url = "ssl://"  + broker + ":" + sslport;
		}
		
		if (client == null) {
			// Construct an MQTT blocking mode client
			client = new MqttClient(url, clientId);
			
			// Set this wrapper as the callback handler
			client.setCallback(this);
			
			// Construct the connection options object that contains connection parameters 
    		// such as cleansession and LWAT
			conOpt = new MqttConnectOptions();
			
			// Connect to the MQTT server
			client.connect(conOpt);
		}
		
    	// Subscribe to the requested topic
    	// The QOS specified is the maximum level that messages will be sent to the client at. 
    	// For instance if QOS 1 is specified, any messages originally published at QOS 2 will 
    	// be downgraded to 1 when delivering to the client but messages published at 1 and 0 
    	// will be received at the same level they were published at. 
		client.subscribe(subTopic, qos);
	}
	
    /**
     * Disconnect the client that has subscribed from the server
     * @throws MqttException
     */
	private void disconnect() throws MqttException {
		if (client != null) {
			// Disconnect the client from the server
			client.disconnect();
			client = null;
		}
	}

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
	public void connectionLost(Throwable cause) {
		// Called when the connection to the server has been lost.
		// An application may choose to implement reconnection
		// logic at this point. This sample simply exits.
		System.out.println("Connection to " + broker + "lose!" + cause);
		System.exit(1);
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		// Called when a message has been delivered to the
		// server. The token passed in here is the same one
		// that was passed to or returned from the original call to publish.
		// This allows applications to perform asynchronous 
		// delivery without blocking until delivery completes.
		//
		// This sample demonstrates asynchronous deliver and 
		// uses the token.waitForCompletion() call in the main thread which
		// blocks until the delivery has completed. 
		// Additionally the deliveryComplete method will be called if 
		// the callback is set on the client
		// 
		// If the connection to the server breaks before delivery has completed
		// delivery of a message will complete after the client has re-connected.
		// The getPendinTokens method will provide tokens for any messages
		// that are still to be delivered.
	}

	/****************************************************************/
	/* End of MqttCallback methods                                  */
	/****************************************************************/


	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		// Called when a message arrives from the server that matches any
		// subscription made by the client		
		String time = "now";
		System.out.println("Time:\t" +time +
                           "  Topic:\t" + topic + 
                           "  Message:\t" + new String(message.getPayload()) +
                           "  QoS:\t" + message.getQos());
	}

}
