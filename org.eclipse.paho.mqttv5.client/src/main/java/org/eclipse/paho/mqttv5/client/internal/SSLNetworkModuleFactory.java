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
package org.eclipse.paho.mqttv5.client.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.security.SSLSocketFactoryFactory;
import org.eclipse.paho.mqttv5.client.spi.NetworkModuleFactory;
import org.eclipse.paho.mqttv5.common.MqttException;

public class SSLNetworkModuleFactory implements NetworkModuleFactory {

	@Override
	public Set<String> getSupportedUriSchemes() {
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList("ssl")));
	}

	@Override
	public void validateURI(URI brokerUri) throws IllegalArgumentException {
		String path = brokerUri.getPath();
		if (path != null && !path.isEmpty()) {
			throw new IllegalArgumentException(brokerUri.toString());
		}
	}

	@Override
	public NetworkModule createNetworkModule(URI brokerUri, MqttConnectionOptions options, String clientId)
			throws MqttException
	{
		String host = brokerUri.getHost();
		int port = brokerUri.getPort(); // -1 if not defined
		if (port == -1) {
			port = 8883;
		}
		String path = brokerUri.getPath();
		if (path != null && !path.isEmpty()) {
			throw new IllegalArgumentException(brokerUri.toString());
		}
		SocketFactory factory = options.getSocketFactory();
		SSLSocketFactoryFactory factoryFactory = null;
		if (factory == null) {
//			try {
			factoryFactory = new SSLSocketFactoryFactory();
			Properties sslClientProps = options.getSSLProperties();
			if (null != sslClientProps) {
				factoryFactory.initialize(sslClientProps, null);
			}
			factory = factoryFactory.createSocketFactory(null);
//			}
//			catch (MqttDirectException ex) {
//				throw ExceptionHelper.createMqttException(ex.getCause());
//			}
		} else if ((factory instanceof SSLSocketFactory) == false) {
			throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
		}

		// Create the network module...
		SSLNetworkModule netModule = new SSLNetworkModule((SSLSocketFactory) factory, host, port, clientId);
		netModule.setSSLhandshakeTimeout(options.getConnectionTimeout());
		netModule.setSSLHostnameVerifier(options.getSSLHostnameVerifier());
		netModule.setHttpsHostnameVerificationEnabled(options.isHttpsHostnameVerificationEnabled());
		// Ciphers suites need to be set, if they are available
		if (factoryFactory != null) {
			String[] enabledCiphers = factoryFactory.getEnabledCipherSuites(null);
			if (enabledCiphers != null) {
				netModule.setEnabledCiphers(enabledCiphers);
			}
		}
		return netModule;
	}
}
