/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.eclipse.paho.client.mqttv3.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.auth.x500.X500Principal;

import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

/**
 * Socket factory for TLS/SSL connections.
 */
public class SSLConnectionSocketFactory implements ConnectionSocketFactory {
	private static final String CLASS_NAME = SSLConnectionSocketFactory.class.getName();
	private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);
	
	private final javax.net.ssl.SSLSocketFactory socketFactory;
	private final HostnameVerifier hostnameVerifier;
	private final String[] supportedProtocols;
	private String[] supportedCipherSuites;
	private int sslHandshakeTimeout = 30000; //ms

	public SSLConnectionSocketFactory(
			final javax.net.ssl.SSLSocketFactory socketFactory,
			final HostnameVerifier hostnameVerifier) {
		this(socketFactory, null, null, hostnameVerifier);
	}

	public SSLConnectionSocketFactory(
			final javax.net.ssl.SSLSocketFactory socketFactory,
			final String[] supportedProtocols,
			final String[] supportedCipherSuites,
			final HostnameVerifier hostnameVerifier) {
		if (socketFactory == null) {
			throw new IllegalArgumentException("SSL socket factory must not be null");
		}
		this.socketFactory = socketFactory;
		this.supportedProtocols = supportedProtocols;
		this.supportedCipherSuites = supportedCipherSuites;
		this.hostnameVerifier = hostnameVerifier != null ? hostnameVerifier : NoopHostnameVerifier.INSTANCE;
	}

	/**
	 * Performs any custom initialization for a newly created SSLSocket
	 * (before the SSL handshake happens).
	 *
	 * The default implementation is a no-op, but could be overridden to, e.g.,
	 * call {@link javax.net.ssl.SSLSocket#setEnabledCipherSuites(String[])}.
	 * @throws IOException may be thrown if overridden
	 */
	protected void prepareSocket(final SSLSocket socket) throws IOException {
	}

	public Socket createSocket() throws IOException {
		return SocketFactory.getDefault().createSocket();
	}

	public Socket connectSocket(
			final int connectTimeout,
			final Socket socket,
			final String host,
			final InetSocketAddress remoteAddress,
			final InetSocketAddress localAddress) throws IOException {
		final String methodName = "connectSocket";
		
		if (host == null) {
			throw new IllegalArgumentException("Host must not be null");
		}
		if (remoteAddress == null) {
			throw new IllegalArgumentException("Remote address must not be null");
		}
		final Socket s = socket != null ? socket : createSocket();
		if (localAddress != null) {
			s.bind(localAddress);
		}
		try {
			if (log.isLoggable(Logger.FINE)) {
				this.log.fine(CLASS_NAME, methodName, "Connecting socket to " + remoteAddress + " with timeout " + connectTimeout);
			}
			s.connect(remoteAddress, connectTimeout);
		} catch (final IOException e) {
			try {
				s.close();
			} catch (final IOException ignore) {
			}
			throw e;
		}
		// Setup the SSL socket.
		if (s instanceof SSLSocket) {
			final SSLSocket sslSocket = (SSLSocket) s;
			this.log.fine(CLASS_NAME, methodName, "Starting handshake");
			int soTimeout = sslSocket.getSoTimeout();
			if (soTimeout == 0) {
				// Avoid infinite SSL handshake timeout.
				sslSocket.setSoTimeout(this.sslHandshakeTimeout);
			}
			sslSocket.startHandshake();
			sslSocket.setSoTimeout(soTimeout);
			verifyHostname(sslSocket, host);
			return s;
		} else {
			return createSSLSocket(s, host, remoteAddress.getPort());
		}
	}
	
	public void setSupportedCipherSuites(String[] supportedCipherSuites) {
		this.supportedCipherSuites = supportedCipherSuites;
	}
	
	public void setSSLHandshakeTimeout(int timeout) {
		if (timeout != 0) {
			this.sslHandshakeTimeout = timeout;
		}
	}

	private Socket createSSLSocket(
			final Socket socket,
			final String target,
			final int port) throws IOException {
		final String methodName = "createSSLSocket";
		
		final SSLSocket sslSocket = (SSLSocket) this.socketFactory.createSocket(
				socket,
				target,
				port,
				true);
		if (supportedProtocols != null) {
			sslSocket.setEnabledProtocols(supportedProtocols);
		} else {
			// If supported protocols are not explicitly set, remove all SSL protocol versions.
			final String[] allProtocols = sslSocket.getEnabledProtocols();
			final List enabledProtocols = new ArrayList(allProtocols.length);
			for (int i = 0; i < allProtocols.length; i++) {
				String protocol= allProtocols[i];
				if (!protocol.startsWith("SSL")) {
					enabledProtocols.add(protocol);
				}
			}
			if (!enabledProtocols.isEmpty()) {
				sslSocket.setEnabledProtocols((String[]) enabledProtocols.toArray(new String[enabledProtocols.size()]));
			}
		}
		if (supportedCipherSuites != null) {
			sslSocket.setEnabledCipherSuites(supportedCipherSuites);
		}

		if (log.isLoggable(Logger.FINE)) {
			this.log.fine(CLASS_NAME, methodName, "Enabled protocols: " + Arrays.asList(sslSocket.getEnabledProtocols()));
			this.log.fine(CLASS_NAME, methodName, "Enabled cipher suites:" + Arrays.asList(sslSocket.getEnabledCipherSuites()));
		}

		prepareSocket(sslSocket);
		this.log.fine(CLASS_NAME, methodName, "Starting handshake");
		int soTimeout = sslSocket.getSoTimeout();
		if (soTimeout == 0) {
			// Avoid infinite SSL handshake timeout.
			sslSocket.setSoTimeout(this.sslHandshakeTimeout);
		}
		sslSocket.startHandshake();
		sslSocket.setSoTimeout(soTimeout);
		verifyHostname(sslSocket, target);
		return sslSocket; 
	}

	private void verifyHostname(final SSLSocket sslSocket, final String hostname) throws IOException {
		final String methodName = "verifyHostname";
		
		try {
			SSLSession session = sslSocket.getSession();
			if (session == null) {
				throw new SSLHandshakeException("SSL session unavailable");
			}

			if (log.isLoggable(Logger.FINE)) {
				this.log.fine(CLASS_NAME, methodName, "Secure session established");
				this.log.fine(CLASS_NAME, methodName, " negotiated protocol: " + session.getProtocol());
				this.log.fine(CLASS_NAME, methodName, " negotiated cipher suite: " + session.getCipherSuite());

				try {
					final Certificate[] certs = session.getPeerCertificates();
					final X509Certificate x509Certificate = (X509Certificate) certs[0];
					final X500Principal peer = x509Certificate.getSubjectX500Principal();

					this.log.fine(CLASS_NAME, methodName, " peer principal: " + peer.toString());
					final Collection altNames1 = x509Certificate.getSubjectAlternativeNames();
					if (altNames1 != null) {
						final List altNames = new ArrayList();
						for (Iterator itr = altNames1.iterator(); itr.hasNext();) {
							final List aC = (List) itr.next();
							if (!aC.isEmpty()) {
								altNames.add((String) aC.get(1));
							}
						}
						this.log.fine(CLASS_NAME, methodName, " peer alternative names: " + altNames);
					}

					final X500Principal issuer = x509Certificate.getIssuerX500Principal();
					this.log.fine(CLASS_NAME, methodName, " issuer principal: " + issuer.toString());
					final Collection altNames2 = x509Certificate.getIssuerAlternativeNames();
					if (altNames2 != null) {
						final List altNames = new ArrayList();
						for (Iterator itr = altNames2.iterator(); itr.hasNext();) {
							final List aC = (List) itr.next();
							if (!aC.isEmpty()) {
								altNames.add((String) aC.get(1));
							}
						}
						this.log.fine(CLASS_NAME, methodName, " issuer alternative names: " + altNames);
					}
				} catch (Exception ignore) {
				}
			}

			if (!this.hostnameVerifier.verify(hostname, session)) {
				final Certificate[] certs = session.getPeerCertificates();
				final X509Certificate x509Certificate = (X509Certificate) certs[0];
				final X500Principal x500Principal = x509Certificate.getSubjectX500Principal();
				throw new SSLPeerUnverifiedException("Host name '" + hostname + "' does not match " +
						"the certificate subject provided by the peer (" + x500Principal.toString() + ")");
			}
		} catch (final IOException e) {
			// Close the socket before re-throwing the exception.
			try {
				sslSocket.close();
			} catch (final Exception ignore) {
			}
			throw e;
		}
	}
}
