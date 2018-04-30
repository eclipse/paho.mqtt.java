/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.paho.jmeclient.mqttv3.sampleMIDP;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;


public class WmqttMgr implements MqttCallback {
    private final static String REQ_TOPIC = "Sample/Java/request";   // The topic to which this MIDlet subscribes
    private final static String RESP_TOPIC = "Sample/Java/response"; // The topic to which this MIDlet publishes

    private MqttClient   wmqttClient   = null;
    private String       clientId      = null;
    private int[]        subQoS        = { 0 };
    private boolean      connected     = false;
    
    private Object       connLock = new Object();
    private boolean      connLockNotified = false;
    
	private Object       subLock = new Object();
	private boolean      subLockNotified = false;

    private MqttCallback callback = null;
    
    private boolean      userConnect = true; // Is connect being initialted by the user?
    // LW&T
    private final static String LWT_TOPIC = "midlet/echo/lwt";
    private String lwtMsg = null;
    
    /**
     * The constructor creates the MqttClient object which contains the
     * mqttv3 API. It also registers this class as the one that will handle mqttv3 callback events.
     */
    public WmqttMgr( String theClientId, String theServer, int thePort ) throws MqttException {
    	clientId = theClientId;
    	lwtMsg = clientId + " has gone offline";
    	
    	wmqttClient = new MqttClient( "ssl://" + theServer + ":" + thePort, clientId);
    	wmqttClient.setCallback(this);
    }
    	    	
    /**
     * disconnectClient stops any connectionLost processing that is happening
     *  and disconnects the TCP/IP socket connection.
     * @throws MqttException 
     */
    public boolean disconnectClient() throws MqttException {
    	synchronized( connLock ) {
    		connLockNotified = true;
    		connLock.notify();
    	}	

		try {
			wmqttClient.disconnect();
		} catch( MqttPersistenceException mqpe ) {
			// Persistence is not used
		}		
		
		connected = false;
		
		return true;
    }
    	
    /**
     * destroyClient terminates all threads running in the WMQTT implementation.
     * Before a new connection can be made a new instance of the MqttClient class must be created.
     */	
    public boolean destroyClient() {
		
		try {
			Thread.sleep( 100 );
		} catch ( InterruptedException ie ) {
		}
		
		try {
			wmqttClient.disconnect();
			wmqttClient = null;
		} catch (MqttException e) {
			// Unable to disconnect client
		}
			
    	return true;
    }
    
	/**
	 * A user of this class may also want to receive callback events from
	 * WMQTT. Only one object may recieve the events.
	 */
    public void setCallback( MqttCallback theCallback ) {
    	callback = theCallback;
    }	
		
	/**
	 * Can be run in a separate thread to initially connect to the broker.
	 */
	public void connectToBroker() throws MqttException {
		userConnect = true;
    	try {
    		connectionLost();
    		connected = true;
    	} catch( MqttException e ) {
    		connected = false;
    		throw e;
    	} finally {
    		userConnect = false;
    	}
	}
		
	/**
	 * Subscribe to the request topic
	 */	
	public boolean subscribe() throws Exception {
		String[] topics = new String[1];
		int[] qosarr = new int[1];
		topics[0] = REQ_TOPIC;
		qosarr[0] = 1;
		
		boolean ret = false;

		try {
			synchronized( subLock ) {
				wmqttClient.subscribe( topics, qosarr );
				subLockNotified = false;
			}
			ret = true;
		} catch( Exception e ) {
			// Subscribe failed
			ret = false;
		}		
		
		return ret;
	}
			
	/**
	 * Unsubscribe from the request topic
	 */	
	public void unsubscribe() throws Exception {
		String[] topics = new String[1];
		topics[0] = REQ_TOPIC;

		synchronized( subLock ) {
			wmqttClient.unsubscribe( topics );
			subLockNotified = false;
		}
	}
		
	/**
	 * Echo the response back to the response topic
	 */	
	public void publishResponse( byte[] responseData ) throws MqttException, Exception {
		wmqttClient.publish( getRespTopic(), responseData, 1, false );
	}	
	
	/**
	 * This reconnects to the broker and resubscribes in the event of the mqttv3 connection
	 * unexpectedly breaking.
	 */
	public void connectionLost() throws MqttException {
		boolean reconnected = false;
	
		synchronized( connLock ) {
			while( !reconnected && !connLockNotified ) {
				try {
					wmqttClient.connect();
					reconnected = true;
				} catch( MqttException mqe ) {
					// Some sort of WMQTT error has occurred - retry.
					if ( userConnect ) {
						// If the connect is initiated by the user feed the execption back to the API
						throw mqe;
					}
					// An else block could display an error Alert panel on the device.
				}
			
				try {
					connLock.wait( 2000 );
				} catch( InterruptedException ie ) {
				}			
			}
			connLockNotified = false;
		}	
	
		if ( reconnected ) {
			try {
				String requestTopic[] = { REQ_TOPIC };
				wmqttClient.subscribe( requestTopic, subQoS );
				
			} catch ( MqttException e ) {
				disconnectClient();
				destroyClient();
				throw e;
			}			
		}		
			
	}
	
	/**
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(Throwable)
	 */
	public void connectionLost(Throwable cause) {
		try {
			connectionLost();
		} catch (MqttException e) {
			// We tried our best to reconnect
		}
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.MqttCallback#messageArrive(String, MqttMessage)
	 */
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {
		callback.messageArrived(topic, message);
	}

	/**
	 * @see org.eclipse.paho.client.mqttv3.deliveryComplete(String, MqttMessage)
	 */
	public void deliveryComplete(IMqttDeliveryToken token) {
		callback.deliveryComplete(token);
	}

	/**
	 * Gets the response Topic name.
	 * @return Response Topic
	 */
	public String getRespTopic() {
		return RESP_TOPIC;
	}

}
