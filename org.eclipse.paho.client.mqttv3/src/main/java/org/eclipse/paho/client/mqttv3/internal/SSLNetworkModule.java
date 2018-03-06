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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * A network module for connecting over SSL.
 */
public class SSLNetworkModule extends TCPNetworkModule {
	private static final String CLASS_NAME = SSLNetworkModule.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private String[] enabledCiphers;
	private int handshakeTimeoutSecs;
	private HostnameVerifier hostnameVerifier;
	private boolean httpsHostnameVerificationEnabled = false;

	

	private String host;
	private int port;

	/**
	 * Constructs a new SSLNetworkModule using the specified host and port. The
	 * supplied SSLSocketFactory is used to supply the network socket.
	 * 
	 * @param factory
	 *            the {@link SSLSocketFactory} to be used in this SSLNetworkModule
	 * @param host
	 *            the Hostname of the Server
	 * @param port
	 *            the Port of the Server
	 * @param resourceContext
	 *            Resource Context
	 */
	public SSLNetworkModule(SSLSocketFactory factory, String host, int port, String resourceContext) {
		super(factory, host, port, resourceContext);
		this.host = host;
		this.port = port;
		log.setResourceName(resourceContext);
	}

	/**
	 * Returns the enabled cipher suites.
	 * 
	 * @return a string array of enabled Cipher suites
	 */
	public String[] getEnabledCiphers() {
		return enabledCiphers;
	}

	/**
	 * Sets the enabled cipher suites on the underlying network socket.
	 * 
	 * @param enabledCiphers
	 *            a String array of cipher suites to enable
	 */
	public void setEnabledCiphers(String[] enabledCiphers) {
		final String methodName = "setEnabledCiphers";
		if (enabledCiphers != null) {
			this.enabledCiphers = enabledCiphers.clone();
		}
		if ((socket != null) && (this.enabledCiphers != null)) {
			if (log.isLoggable(Logger.FINE)) {
				String ciphers = "";
				for (int i = 0; i < this.enabledCiphers.length; i++) {
					if (i > 0) {
						ciphers += ",";
					}
					ciphers += this.enabledCiphers[i];
				}
				// @TRACE 260=setEnabledCiphers ciphers={0}
				log.fine(CLASS_NAME, methodName, "260", new Object[] { ciphers });
			}
			((SSLSocket) socket).setEnabledCipherSuites(this.enabledCiphers);
		}
	}

	public void setSSLhandshakeTimeout(int timeout) {
		super.setConnectTimeout(timeout);
		this.handshakeTimeoutSecs = timeout;
	}

	public HostnameVerifier getSSLHostnameVerifier() {
		return hostnameVerifier;
	}

	public void setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
		this.hostnameVerifier = hostnameVerifier;
	}
	
	public boolean isHttpsHostnameVerificationEnabled() {
		return httpsHostnameVerificationEnabled;
	}

	public void setHttpsHostnameVerificationEnabled(boolean httpsHostnameVerificationEnabled) {
		this.httpsHostnameVerificationEnabled = httpsHostnameVerificationEnabled;
	}

	public void start() throws IOException, MqttException {
		super.start();
		setEnabledCiphers(enabledCiphers);
		int soTimeout = socket.getSoTimeout();
		// RTC 765: Set a timeout to avoid the SSL handshake being blocked indefinitely
		socket.setSoTimeout(this.handshakeTimeoutSecs * 1000);
		
		// If default Hostname verification is enabled, use the same method that is used with HTTPS
		if(this.httpsHostnameVerificationEnabled) {
			SSLParameters sslParams = new SSLParameters();
			sslParams.setEndpointIdentificationAlgorithm("HTTPS");
			((SSLSocket) socket).setSSLParameters(sslParams);
		}
		((SSLSocket) socket).startHandshake();
		if (hostnameVerifier != null && !this.httpsHostnameVerificationEnabled) {
			SSLSession session = ((SSLSocket) socket).getSession();
			hostnameVerifier.verify(host, session);
		}
		// reset timeout to default value
		socket.setSoTimeout(soTimeout);
	}

	public String getServerURI() {
		return "ssl://" + host + ":" + port;
	}
}
