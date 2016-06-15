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
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * A network module for connecting over TCP. 
 */
public class TCPNetworkModule implements NetworkModule {
	private static final String CLASS_NAME = TCPNetworkModule.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,CLASS_NAME);

	protected Socket socket;
	private SocketFactory factory;
	private String host;
	private int port;
	private int conTimeout;
	
	/**
	 * Constructs a new TCPNetworkModule using the specified host and
	 * port.  The supplied SocketFactory is used to supply the network
	 * socket.
	 */
	public TCPNetworkModule(SocketFactory factory, String host, int port, String resourceContext) {
		log.setResourceName(resourceContext);
		this.factory = factory;
		this.host = host;
		this.port = port;
		
	}

	/**
	 * Starts the module, by creating a TCP socket to the server.
	 */
	public void start() throws IOException, MqttException {
		final String methodName = "start";
		try {
//			InetAddress localAddr = InetAddress.getLocalHost();
//			socket = factory.createSocket(host, port, localAddr, 0);
			// @TRACE 252=connect to host {0} port {1} timeout {2}
			log.fine(CLASS_NAME,methodName, "252", new Object[] {host, new Integer(port), new Long(conTimeout*1000)});
			SocketAddress sockaddr = new InetSocketAddress(host, port);
			socket = factory.createSocket();
			socket.connect(sockaddr, conTimeout*1000);
		
			// SetTcpNoDelay was originally set ot true disabling Nagle's algorithm. 
			// This should not be required.
//			socket.setTcpNoDelay(true);	// TCP_NODELAY on, which means we do not use Nagle's algorithm
		}
		catch (ConnectException ex) {
			//@TRACE 250=Failed to create TCP socket
			log.fine(CLASS_NAME,methodName,"250",null,ex);
			throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR, ex);
		}
	}

	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}
	
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	/**
	 * Stops the module, by closing the TCP socket.
	 */
	public void stop() throws IOException {
		if (socket != null) {
			socket.close();
		}
	}
	
	/**
	 * Set the maximum time to wait for a socket to be established
	 * @param timeout
	 */
	public void setConnectTimeout(int timeout) {
		this.conTimeout = timeout;
	}

	public String getServerURI() {
		return "tcp://" + host + ":" + port;
	}
}
