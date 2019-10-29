/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 *   https://www.eclipse.org/org/documents/edl-v10.php
 */
package org.eclipse.paho.client.mqttv3.spi;

import java.net.URI;
import java.util.Set;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;

/**
 * The NetworkModuleFactory provides a facility to discover and create {@link NetworkModule}s for URI schemes.
 *
 * @author Maik Scheibler
 */
public interface NetworkModuleFactory {

	/**
	 * Returns all URI schemes that are supported by the NetworkModules created from this factory.
	 *
	 * @return an unmodifiable set of all supported URI schemes (lower case letters)
	 * @see URI#getScheme()
	 */
	Set<String> getSupportedUriSchemes();

	/**
	 * This method validates the {@link URI#getSchemeSpecificPart() scheme specific part} of all by this factory
	 * supported URI schemes. If the provided URI does not fulfill the scheme requirements an
	 * {@link IllegalArgumentException} must be thrown with explanatory a message.
	 *
	 * @param brokerUri to be validated
	 * @throws IllegalArgumentException to signal an invalid URI; the exception transports the cause
	 */
	void validateURI(URI brokerUri) throws IllegalArgumentException;

	/**
	 * Creates a NetworkModule instance.
	 *
	 * @param brokerUri used to connect to the broker
	 * @param options used for the connection
	 * @param clientId a client identifier that is unique on the server being connected to
	 * @return a NetworkModule instance
	 * @throws MqttException in case of any error during instance creation
	 */
	NetworkModule createNetworkModule(URI brokerUri, MqttConnectOptions options, String clientId) throws MqttException;
}
