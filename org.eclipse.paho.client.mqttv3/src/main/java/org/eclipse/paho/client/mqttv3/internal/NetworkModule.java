/*
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

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * A NetworkModule provides access to a specific transport to the broker. This may be a plain socket, a TLS secured
 * connection, a serial line, etc.
 * <p>
 * TODO: Provide description about the reusability of the NetworkModule.
 */
public interface NetworkModule {
	/**
	 * Open the transport to the broker.
	 *
	 * @throws IOException
	 * @throws MqttException
	 */
	public void start() throws IOException, MqttException;

	public InputStream getInputStream() throws IOException;

	public OutputStream getOutputStream() throws IOException;

	/**
	 * Close the transport to the broker.
	 * <p>
	 * TODO: Check whether the input and output streams are to be closed as well?
	 *
	 * @throws IOException
	 */
	public void stop() throws IOException;

	/**
	 * Returns the URI of the broker which was used to create this NetworkModule.
	 *
	 * @return the URI of the broker
	 */
	public String getServerURI();
}
