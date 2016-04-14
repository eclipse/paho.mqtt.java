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
 *    James Sutton - Bug 459142 - WebSocket support for the Java client.
 */
package org.eclipse.paho.client.mqttv3.internal.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

public class WebSocketNetworkModule extends TCPNetworkModule {
	
	private static final String CLASS_NAME = WebSocketNetworkModule.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);
		
	private String uri;
	private String host;
	private int port;
	private PipedInputStream pipedInputStream;
	private WebSocketReceiver webSocketReceiver;
	ByteBuffer recievedPayload;
	
	/**
	 * Overrides the flush method.
	 * This allows us to encode the MQTT payload into a WebSocket
	 *  Frame before passing it through to the real socket.
	 */
	private ByteArrayOutputStream outputStream = new ByteArrayOutputStream(){
		
		public void flush() throws IOException {
			final ByteBuffer byteBuffer;
			synchronized (this) {
				byteBuffer = ByteBuffer.wrap(toByteArray());
				reset();
			}
			WebSocketFrame frame = new WebSocketFrame((byte)0x02, true, byteBuffer.array());
			byte[] rawFrame = frame.encodeFrame();
			getSocketOutputStream().write(rawFrame);
			getSocketOutputStream().flush();
			
		}
	};
	
	public WebSocketNetworkModule(SocketFactory factory, String uri, String host, int port, String resourceContext){
		super(factory, host, port, resourceContext);
		this.uri = uri;
		this.host = host;
		this.port = port;
		this.pipedInputStream = new PipedInputStream();
		
		log.setResourceName(resourceContext);
	}
	
	public void start() throws IOException, MqttException {
		super.start();
		WebSocketHandshake handshake = new WebSocketHandshake(getSocketInputStream(), getSocketOutputStream(), uri, host, port);
		handshake.execute();
		this.webSocketReceiver = new WebSocketReceiver(getSocketInputStream(), pipedInputStream);
		webSocketReceiver.start("webSocketReceiver");
	}
	
	private OutputStream getSocketOutputStream() throws IOException {
		return super.getOutputStream();
	}
	
	private InputStream getSocketInputStream() throws IOException {
		return super.getInputStream();
	}
	
	public InputStream getInputStream() throws IOException {
		return pipedInputStream;
	}
	
	public OutputStream getOutputStream() throws IOException {
		return outputStream;
	}
	
	/**
	 * Stops the module, by closing the TCP socket.
	 */
	public void stop() throws IOException {
		// Creating Close Frame
		WebSocketFrame frame = new WebSocketFrame((byte)0x08, true, "1000".getBytes());
		byte[] rawFrame = frame.encodeFrame();
		getSocketOutputStream().write(rawFrame);
		getSocketOutputStream().flush();

		if(webSocketReceiver != null){
			webSocketReceiver.stop();
		}
		super.stop();
	}
	
	public String getServerURI() {
		return "ws://" + host + ":" + port;
	}
	
}
