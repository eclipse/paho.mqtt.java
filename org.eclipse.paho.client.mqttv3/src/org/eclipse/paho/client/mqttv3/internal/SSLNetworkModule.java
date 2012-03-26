/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;


/**
 * A network module for connecting over SSL.
 */
public class SSLNetworkModule extends TCPNetworkModule {
	private String[] enabledCiphers;
	private int handshakeTimeoutSecs;
	
	/**
	 * Constructs a new SSLNetworkModule using the specified host and
	 * port.  The supplied SSLSocketFactory is used to supply the network
	 * socket.
	 */
	public SSLNetworkModule(Trace trace, SSLSocketFactory factory, String host, int port) {
		super(trace, factory, host, port);
	}

	/**
	 * Returns the enabled cipher suites.
	 */
	public String[] getEnabledCiphers() {
		return enabledCiphers;
	}

	/**
	 * Sets the enabled cipher suites on the underlying network socket.
	 */
	public void setEnabledCiphers(String[] enabledCiphers) {
		this.enabledCiphers = enabledCiphers;
		if ((socket != null) && (enabledCiphers != null)) {
			if (trace.isOn()) {
				String ciphers = "";
				for (int i=0;i<enabledCiphers.length;i++) {
					if (i>0) {
						ciphers+=",";
					}
					ciphers+=enabledCiphers[i];
				}
				//@TRACE 260=setEnabledCiphers ciphers={0}
				trace.trace(Trace.FINE,260,new Object[]{ciphers});
			}
			((SSLSocket) socket).setEnabledCipherSuites(enabledCiphers);
		}
	}
	
	public void setSSLhandshakeTimeout(int timeout) {
		this.handshakeTimeoutSecs = timeout;
	}
	
	public void start() throws IOException, MqttException {
		super.start();
		setEnabledCiphers(enabledCiphers);
		int soTimeout = socket.getSoTimeout();
		if ( soTimeout == 0 ) {
			// RTC 765: Set a timeout to avoid the SSL handshake being blocked indefinitely
			socket.setSoTimeout(this.handshakeTimeoutSecs*1000);
		}
		((SSLSocket)socket).startHandshake();
		// reset timeout to default value
		socket.setSoTimeout(soTimeout);   
	}
}
