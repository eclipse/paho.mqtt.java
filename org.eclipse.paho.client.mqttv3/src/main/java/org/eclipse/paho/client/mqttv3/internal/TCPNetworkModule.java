/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;

import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.websocket.Base64;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * A network module for connecting over TCP.
 */
public class TCPNetworkModule implements NetworkModule {
	private static final String CLASS_NAME = TCPNetworkModule.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,CLASS_NAME);

	protected Socket socket;
	private SocketFactory factory;
	private String host;
	private int port;
	private int conTimeout;

	private String httpProxyHost;
	private int httpProxyPort;
	private String httpProxyUser;
	private String httpProxyPassword;

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

		// @TRACE 252=connect to host {0} port {1} timeout {2}
		log.fine(CLASS_NAME,methodName, "252", new Object[] {host, Integer.valueOf(port), Long.valueOf(conTimeout*1000)});

		if(httpProxyHost != null) {
			Socket tunnel;

			/*
			 * Set up a socket to do tunneling through the proxy.
			 * Start it off as a regular socket, then layer SSL
			 * over the top of it.
			 */
			try {
				tunnel = new Socket(httpProxyHost, httpProxyPort);
				doTunnelHandshake(tunnel, host, port, httpProxyUser, httpProxyPassword);
			}catch (IOException ex) {
				//@TRACE 251=Failed to create TCP tunnel
				log.fine(CLASS_NAME,methodName,"251",null,ex);
				throw new MqttException(MqttException.REASON_CODE_HTTP_PROXY_CONNECT_ERROR, ex);
			}

			try {
				socket = ((SSLSocketFactory) factory).createSocket(tunnel, host, port, true);
			} catch (ConnectException ex) {
				//@TRACE 250=Failed to create TCP socket
				log.fine(CLASS_NAME,methodName,"250",null,ex);
				throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR, ex);
			}

		} else {
			try {
				SocketAddress sockaddr = new InetSocketAddress(host, port);
				socket = factory.createSocket();
				socket.connect(sockaddr, conTimeout * 1000);
				socket.setSoTimeout(1000);
			} catch (ConnectException ex) {
				//@TRACE 250=Failed to create TCP socket
				log.fine(CLASS_NAME, methodName, "250", null, ex);
				throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR, ex);
			}
		}
	}

	/*
	 * Tell our tunnel where we want to CONNECT, and look for the
	 * right reply.  Throw IOException if anything goes wrong.
	 */
	private void doTunnelHandshake(Socket tunnel, String host, int port, String proxyUser, String proxyPassword)
			throws IOException {
		OutputStream out = tunnel.getOutputStream();

		String msg;
		if(proxyUser != null) {
			String proxyUserPass = String.format("%s:%s", proxyUser, proxyPassword);
			msg = "CONNECT " + host + ":" + port + " HTTP/1.1\n"
					+ "Proxy-Authorization: Basic " + Base64.encode(proxyUserPass) + "\n"
					+ "User-Agent: Paho MQTT3 Client\n"
					+ "Proxy-Connection: Keep-Alive"
					+ "\r\n\r\n";
		} else {
			msg = "CONNECT " + host + ":" + port + " HTTP/1.0\n"
					+ "User-Agent: "
					+ "User-Agent: Paho MQTT3 Client\n"
					+ "Proxy-Connection: Keep-Alive"
					+ "\r\n\r\n";
		}

		byte b[];
		try {
			/*
			 * We really do want ASCII7 -- the http protocol doesn't change
			 * with locale.
			 */
			b = msg.getBytes("ASCII7");
		} catch (UnsupportedEncodingException ignored) {
			/*
			 * If ASCII7 isn't there, something serious is wrong, but
			 * Paranoia Is Good (tm)
			 */
			b = msg.getBytes();
		}
		out.write(b);
		out.flush();

		/*
		 * We need to store the reply so we can create a detailed
		 * error message to the user.
		 */
		byte            reply[] = new byte[200];
		int             replyLen = 0;
		int             newlinesSeen = 0;
		boolean         headerDone = false;     /* Done on first newline */

		InputStream     in = tunnel.getInputStream();
		boolean         error = false;

		while (newlinesSeen < 2) {
			int i = in.read();
			if (i < 0) {
				throw new IOException("Unexpected EOF from proxy");
			}
			if (i == '\n') {
				headerDone = true;
				++newlinesSeen;
			} else if (i != '\r') {
				newlinesSeen = 0;
				if (!headerDone && replyLen < reply.length) {
					reply[replyLen++] = (byte) i;
				}
			}
		}

		/*
		 * Converting the byte array to a string is slightly wasteful
		 * in the case where the connection was successful, but it's
		 * insignificant compared to the network overhead.
		 */
		String replyStr;
		try {
			replyStr = new String(reply, 0, replyLen, "ASCII7");
		} catch (UnsupportedEncodingException ignored) {
			replyStr = new String(reply, 0, replyLen);
		}

		/* We asked for HTTP/1.0, so we should get that back */
//		if (!replyStr.startsWith("HTTP/1.0 200")) {
		if(replyStr.indexOf("200") == -1) {
			throw new IOException("Unable to tunnel through "
					+ tunnel.getInetAddress().getHostName() + ":" + tunnel.getPort()
					+ ".  Proxy returns \"" + replyStr + "\"");
		}

		/* tunneling Handshake was successful! */
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

	public void setHttpProxyHost(String httpProxyHost) {
		this.httpProxyHost = httpProxyHost;
	}

	public void setHttpProxyPort(int httpProxyPort) {
		this.httpProxyPort = httpProxyPort;
	}

	public void setHttpProxyUser(String httpProxyUser) {
		this.httpProxyUser = httpProxyUser;
	}

	public void setHttpProxyPassword(String httpProxyPassword) {
		this.httpProxyPassword = httpProxyPassword;
	}

	public void configHttpProxy(String proxyHost, int proxyPort, String user, String password) {
		if(proxyHost != null && proxyHost.length() > 0 &&
				proxyPort > 0){
			setHttpProxyHost(proxyHost);
			setHttpProxyPort(proxyPort);
			if(user != null && user.length() > 0 &&
					password != null && password.length() > 0) {
				setHttpProxyUser(user);
				setHttpProxyPassword(password);
			}
		}
	}
}