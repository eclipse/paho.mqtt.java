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
 

package org.eclipse.paho.jmeclient.mqttv3.test;

import java.io.IOException;
import javax.microedition.midlet.MIDlet;

import com.oracle.deviceaccess.gpio.GPIOManager;
import com.oracle.deviceaccess.gpio.GPIOPin;
import com.oracle.deviceaccess.gpio.GPIOPort;
import com.oracle.deviceaccess.gpio.PinEvent;
import com.oracle.deviceaccess.gpio.PinListener;

/**
 * Tests providing a basic general coverage for the MQTT client API
 */
public class GPIOTest extends MIDlet {
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
    private BasicSyncTestCaseMIDP testCase;
    
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
            
    		try {
    			testCase = new BasicSyncTestCaseMIDP();
    			
    		} catch (Exception e) {
    			e.printStackTrace();
    			System.out.println(e.getMessage());
    			System.out.println(e.toString());
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
                	//Run a subset of tests here!
                	testCase.setUpBeforeClass();
                	
                	System.out.println("Test Connect");
                	testCase.testConnect();
                	System.out.println("Test Connect Complete");
                	
                	System.out.println("Pub/Sub");
                	testCase.testPubSub();
                	System.out.println("Pub/Sub Test Complete");
                	
                	//End test
                	testCase.tearDownAfterClass();
                } catch (Exception ex) {
                	System.out.println("Failed");
                	System.out.println(ex.getMessage());
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
                	//Run a subset of tests here!
                	testCase.setUpBeforeClass();
                	
                	System.out.println("HA Connection");
                	testCase.testHAConnect();
                	System.out.println("HA Test Complete");
                	
                	System.out.println("Message Properties");
                	testCase.testMsgProperties();
                	System.out.println("Message Properties test complete");
                	
                	System.out.println("Conn Opt Defaults Test");
                	testCase.testConnOptDefaults();
                	System.out.println("Conn Opt Defaults Test Complete");
                	
                	//End test
                	testCase.tearDownAfterClass();
                } catch (Exception ex) {
                	System.out.println("Failed");
                	System.out.println(ex.getMessage());
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
                	// Currently no op, however could be used to kick off another test
                } catch (Exception ex) {
                    ex.printStackTrace();
                } 
            }            
        }
    };
}
