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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

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
	 * @param factory the {@link SocketFactory} to be used to set up this connection
	 * @param host The server hostname
	 * @param port The server port
	 * @param resourceContext The Resource Context
	 */
	public TCPNetworkModule(SocketFactory factory, String host, int port, String resourceContext) {
		log.setResourceName(resourceContext);
		this.factory = factory;
		this.host = host;
		this.port = port;

	}

	/**
	 * Starts the module, by creating a TCP socket to the server.
	 * @throws IOException if there is an error creating the socket
	 * @throws MqttException if there is an error connecting to the server
	 */
	public void start() throws IOException, MqttException {
		final String methodName = "start";
		try {
//			InetAddress localAddr = InetAddress.getLocalHost();
//			socket = factory.createSocket(host, port, localAddr, 0);
			// @TRACE 252=connect to host {0} port {1} timeout {2}
			log.fine(CLASS_NAME,methodName, "252", new Object[] {host, new Integer(port), new Long(conTimeout*1000)});
			SocketAddress sockaddr = new InetSocketAddress(host, port);
			if (factory instanceof SSLSocketFactory) {
				// SNI support
				Socket tempsocket = new Socket();
				tempsocket.connect(sockaddr, conTimeout*1000);
				socket = ((SSLSocketFactory)factory).createSocket(tempsocket, host, port, true);
			} else {
				socket = factory.createSocket();
				socket.connect(sockaddr, conTimeout*1000);
			}
		
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
	 * @throws IOException if there is an error closing the socket
	 */
	public void stop() throws IOException {
		if (socket != null) {
			// CDA: an attempt is made to stop the receiver cleanly before closing the socket.
			// If the socket is forcibly closed too early, the blocking socket read in
			// the receiver thread throws a SocketException.
			// While this causes the receiver thread to exit, it also invalidates the
			// SSL session preventing to perform an accelerated SSL handshake in the
			// next connection.
			//
			// Also note that due to the blocking socket reads in the receiver thread,
			// it's not possible to interrupt the thread. Using non blocking reads in
			// combination with a socket timeout (see setSoTimeout()) would be a better approach.
			//
			// Please note that the Javadoc only says that an EOF is returned on
			// subsequent reads of the socket stream.
			// Anyway, at least with Oracle Java SE 7 on Linux systems, this causes a blocked read
			// to return EOF immediately.
			// This workaround should not cause any harm in general but you might
			// want to move it in SSLNetworkModule.

			socket.shutdownInput();
			socket.close();
		}
	}

	/**
	 * Set the maximum time to wait for a socket to be established
	 * @param timeout  The connection timeout
	 */
	public void setConnectTimeout(int timeout) {
		this.conTimeout = timeout;
	}

	public String getServerURI() {
		return "tcp://" + host + ":" + port;
	}
}
