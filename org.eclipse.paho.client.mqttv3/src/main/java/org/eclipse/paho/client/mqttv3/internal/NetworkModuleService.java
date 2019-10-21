/*
 * Copyright (c) 2009, 2019 IBM Corp.
 * 
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

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;
import org.eclipse.paho.client.mqttv3.spi.NetworkModuleFactory;

/**
 * The NetworkModuleService uses the installed {@link NetworkModuleFactory}s to create {@link NetworkModule} instances.
 * <p>
 * The selection of the appropriate NetworkModuleFactory is based on the URI scheme.
 *
 * @author Maik Scheibler
 */
public class NetworkModuleService {
	private static Logger LOG = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT,
			NetworkModuleService.class.getSimpleName());
	private static final ServiceLoader<NetworkModuleFactory> FACTORY_SERVICE_LOADER = ServiceLoader.load(
			NetworkModuleFactory.class, NetworkModuleService.class.getClassLoader());

	/** Pattern to match URI authority parts: {@code authority = [userinfo"@"]host[":"port]} */
	private static final Pattern AUTHORITY_PATTERN = Pattern.compile("((.+)@)?([^:]*)(:(\\d+))?");
	private static final int AUTH_GROUP_USERINFO = 2;
	private static final int AUTH_GROUP_HOST = 3;
	private static final int AUTH_GROUP_PORT = 5;

	private NetworkModuleService() {
		// no instances
	}

	/**
	 * Validates the provided URI to be valid and that a NetworkModule is installed to serve it.
	 *
	 * @param brokerUri to be validated
	 * @throws IllegalArgumentException is case the URI is invalid or there is no {@link NetworkModule} installed for
	 * the URI scheme
	 */
	public synchronized static void validateURI(String brokerUri) throws IllegalArgumentException {
		try {
			URI uri = new URI(brokerUri);
			String scheme = uri.getScheme();
			if (scheme == null || scheme.isEmpty()) {
				throw new IllegalArgumentException("missing scheme in broker URI: " + brokerUri);
			}
			scheme = scheme.toLowerCase();
			for (NetworkModuleFactory factory : FACTORY_SERVICE_LOADER) {
				if (factory.getSupportedUriSchemes().contains(scheme)) {
					factory.validateURI(uri);
					return;
				}
			}
			throw new IllegalArgumentException("no NetworkModule installed for scheme \"" + scheme
					+ "\" of URI \"" + brokerUri + "\"");
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Can't parse string to URI \"" + brokerUri + "\"", e);
		}
	}

	/**
	 * Creates a {@link NetworkModule} instance for the provided address, using the given options.
	 *
	 * @param address must be a valid URI
	 * @param options used to initialize the NetworkModule
	 * @param clientId a client identifier that is unique on the server being connected to
	 * @return a new NetworkModule instance
	 * @throws MqttException if the initialization fails
	 * @throws IllegalArgumentException if the provided {@code address} is invalid
	 */
	public static NetworkModule createInstance(String address, MqttConnectOptions options, String clientId)
			throws MqttException, IllegalArgumentException
	{
		try {
			URI brokerUri = new URI(address);
			applyRFC3986AuthorityPatch(brokerUri);
			String scheme = brokerUri.getScheme().toLowerCase();
			for (NetworkModuleFactory factory : FACTORY_SERVICE_LOADER) {
				if (factory.getSupportedUriSchemes().contains(scheme)) {
					return factory.createNetworkModule(brokerUri, options, clientId);
				}
			}
			/*
			 * To throw an IllegalArgumentException exception matches the previous behavior of
			 * MqttConnectOptions.validateURI(String), but it would be nice to provide something more meaningful.
			 */
			throw new IllegalArgumentException(brokerUri.toString());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(address, e);
		}
	}

	/**
	 * Java does URI parsing according to RFC2396 and thus hostnames are limited to alphanumeric characters and '-'.
	 * But the current &quot;Uniform Resource Identifier (URI): Generic Syntax&quot; (RFC3986) allows for a much wider
	 * range of valid characters. This causes Java to fail parsing the authority part and thus the user-info, host and
	 * port will not be set on an URI which does not conform to RFC2396.
	 * <p>
	 * This workaround tries to detect such a parsing failure and does tokenize the authority parts according to
	 * RFC3986, but does not enforce any character restrictions (for sake of simplicity).
	 *
	 * @param toPatch - The URI To patch
	 * @see <a href="https://tools.ietf.org/html/rfc3986#section-3.2">rfc3986 - section-3.2</a>
	 */
	public static void applyRFC3986AuthorityPatch(URI toPatch) {
		if (toPatch == null
				|| toPatch.getHost() != null // already successfully parsed
				|| toPatch.getAuthority() == null
				|| toPatch.getAuthority().isEmpty())
		{
			return;
		}
		Matcher matcher = AUTHORITY_PATTERN.matcher(toPatch.getAuthority());
		if (matcher.find()) {
			setURIField(toPatch, "userInfo", matcher.group(AUTH_GROUP_USERINFO));
			setURIField(toPatch, "host", matcher.group(AUTH_GROUP_HOST));
			String portString = matcher.group(AUTH_GROUP_PORT);
			setURIField(toPatch, "port", portString != null ? Integer.parseInt(portString) : -1);
		}
	}

	/**
	 * Reflective manipulation of a URI field to work around the URI parser, because all fields are validated even on
	 * the full qualified URI constructor.
	 *
	 * @see URI#URI(String, String, String, int, String, String, String)
	 */
	private static void setURIField(URI toManipulate, String fieldName, Object newValue) {
		try {
			Field field = URI.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(toManipulate, newValue);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			LOG.warning(NetworkModuleService.class.getName(), "setURIField", "115", new Object[] {
					toManipulate.toString() }, e);
		}
	}
}
