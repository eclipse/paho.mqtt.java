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
 
package org.eclipse.paho.jmeclient.mqttv3.sampleGPIO.AsyncWait;

import java.io.IOException;
import javax.microedition.midlet.MIDlet;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.oracle.deviceaccess.gpio.GPIOManager;
import com.oracle.deviceaccess.gpio.GPIOPin;
import com.oracle.deviceaccess.gpio.GPIOPort;
import com.oracle.deviceaccess.gpio.PinEvent;
import com.oracle.deviceaccess.gpio.PinListener;

/**
 * A sample application that demonstrates how to use the Java ME MQTT v3 Client api in
 * non-blocking callback/notification mode.
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
public class GPIOSampleAsyncWait extends MIDlet {
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
    
    MqttClient client = null;
    MqttClient pubClinet = null;
    
    // Private instance variables
    private SampleAsyncWait sampleClientPub = null;
    private SampleAsyncWait sampleClientSub = null;
	private boolean quietMode 	= false;
	private String broker 		= "m2m.eclipse.org";
	private int port 			= 1883;
	private String clientIdPub 	= "SampleAsynCallBackPub";
	private String clientIdSub 	= "SampleAsynCallBackSub";
	private boolean cleanSession = true;			// Non durable subscriptions 
	private String password = null;
	private String userName = null;
	private String protocol = "tcp://";
	private String url = protocol + broker + ":" + port;
    
	/**
	 * Signals the MIDlet that it has entered the Active state
	 */
    public void startApp() {
        if(bFirst == false) {

            System.out.println("Starting GPIO Demo");
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
            System.out.println("GPIO Demo is already started..");
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
        if(button2 != null){
            try {
                button2.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            button2 = null;
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
                	System.out.println("Failed");
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
    	sampleClientPub = new SampleAsyncWait(url,clientIdPub,cleanSession, quietMode,userName,password);

    	if (sampleClientPub != null) {
    		String topic 		= "Sample/Java/v3";
    		int qos 			= 2;
			String message 		= "Message from async calback MQTTv3 Java client sample";
    		try {
    			sampleClientPub.publish(topic,qos,message.getBytes());
			} catch (Throwable e) {
				e.printStackTrace();
			}
    	}
    }

    /**
     * Subscribe to a topic on an MQTT server
     * Once subscribed this method waits for the messages to arrive from the server 
     * that match the subscription. It continues listening for messages until pin 3
     * is pressed
     * @throws IOException, MqttException
     */
	protected void subscribe() throws IOException {
		if (sampleClientSub == null) {
    		try {
				sampleClientSub = new SampleAsyncWait(url,clientIdSub,cleanSession, quietMode,userName,password);
			} catch (MqttException e) {
				e.printStackTrace();
			}
    	}
    	if (sampleClientSub != null) {
    		String topic		= "Sample/#";
    		int qos 			= 2;
    		try {
    			sampleClientSub.subscribe(topic,qos);
			} catch (Throwable e) {
				e.printStackTrace();
			}
    	}
	}

    /**
     * Disconnect the client that has subscribed from the server
     * @throws MqttException
     */
	private void disconnect() throws MqttException {
		if (sampleClientSub != null) {
			// Disconnect the client from the server
			sampleClientSub.disconnect();
			sampleClientSub = null;
		}
	}
     
}
