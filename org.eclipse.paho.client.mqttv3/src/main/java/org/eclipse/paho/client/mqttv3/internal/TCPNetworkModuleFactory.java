/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 * https://www.eclipse.org/org/documents/edl-v10.php
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.spi.NetworkModuleFactory;

public class TCPNetworkModuleFactory implements NetworkModuleFactory {

	@Override
	public Set<String> getSupportedUriSchemes() {
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList("tcp")));
	}

	@Override
	public void validateURI(URI brokerUri) throws IllegalArgumentException {
		String path = brokerUri.getPath();
		if (path != null && !path.isEmpty()) {
			throw new IllegalArgumentException("URI path must be empty \"" + brokerUri.toString() + "\"");
		}
	}

	@Override
	public NetworkModule createNetworkModule(URI brokerUri, MqttConnectOptions options, String clientId)
			throws MqttException
	{
		String host = brokerUri.getHost();
		int port = brokerUri.getPort(); // -1 if not defined
		if (port == -1) {
			port = 1883;
		}
		String path = brokerUri.getPath();
		if (path != null && !path.isEmpty()) {
			throw new IllegalArgumentException(brokerUri.toString());
		}
		SocketFactory factory = options.getSocketFactory();
		if (factory == null) {
			factory = SocketFactory.getDefault();
		} else if (factory instanceof SSLSocketFactory) {
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
		}
		TCPNetworkModule networkModule = new TCPNetworkModule(factory, host, port, clientId);
		networkModule.setConnectTimeout(options.getConnectionTimeout());
		return networkModule;
	}
}
