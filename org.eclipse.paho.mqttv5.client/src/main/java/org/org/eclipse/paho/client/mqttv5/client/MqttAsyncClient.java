/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.org.eclipse.paho.client.mqttv5.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight client for talking to an MQTTv5 server using non-blocking methods
 * that allow an operation to run in the background.
 * 
 * <p>
 * This class is a non-blocking API, allowing applications to initiate MQTT 
 * actions and then carry on working while the MQTT action completes on a background
 * thread.
 * 
 * This implementation is compatible with all Java SE runtimes from 1.8 and up.
 * </p>
 * <p>Using this API an application can connect to an MQTTv5 server using:</p>
 * <ul>
 *     <li>A plain TCP socket</li>
 *     <li>A secure SSL/TLS socket</li>
 *     <li>A plain WebSocket (The server must support WebSockets)</li>
 *     <li>A secure WebSocket (The server must support WebSockets)</li>
 * </ul>
 * 
 */
public class MqttAsyncClient {
	private static final Logger log = LoggerFactory.getLogger(MqttAsyncClient.class);
	
	private static final String DEFAULT_CLIENT_ID_PREFIX = "paho";
	private static final long QUIESCE_TIMEOUT = 30000; // ms
	private static final long DISCONNECT_TIMEOUT = 10000; // ms
	protected static final int URI_TYPE_TCP = 0;
	protected static final int URI_TYPE_SSL = 1;
	protected static final int URI_TYPE_LOCAL = 2;
	protected static final int URI_TYPE_WS = 3;
	protected static final int URI_TYPE_WSS = 4;
	
	
	
	private String clientId;
	private String[] serverURIs;
	
	
	public MqttAsyncClient(String serverURI){
		this(serverURI, null); 
	}
	
	
	public MqttAsyncClient(String serverURI, String clientId){
		
		log.info("Instantiating MqttAsyncClient- URI: {} Client ID:{}", serverURI, clientId);
		
		validateURI(serverURI);
		this.serverURIs = new String[]{serverURI};
		
		
		
	}
	
	/**
	 * Returns a generated client identifier based upon the fixed prefix (paho)
	 * and the current system time.
	 * @return a generated client identifier
	 */
	public static String generateClientId(){
		return DEFAULT_CLIENT_ID_PREFIX + System.nanoTime();
	}
	
	
	/**
	 * Validate a URI
	 * @param srvURI The Server URI
	 * @return the URI type
	 */
	protected static int validateURI(String srvURI) {
		try {
			URI vURI = new URI(srvURI);
			
			
			if ("ws".equals(vURI.getScheme())){
				return URI_TYPE_WS;
			}
			else if ("wss".equals(vURI.getScheme())) {
				return URI_TYPE_WSS;
			}

			if (!vURI.getPath().isEmpty()) {
				throw new IllegalArgumentException(srvURI);
			}
			if ("tcp".equals(vURI.getScheme())) {
				return URI_TYPE_TCP;
			}
			else if ("ssl".equals(vURI.getScheme())) {
				return URI_TYPE_SSL;
			}
			else if ("local".equals(vURI.getScheme())) {
				return URI_TYPE_LOCAL;
			}
			else {
				throw new IllegalArgumentException(srvURI);
			}
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException(srvURI);
		}
	}
	

}
