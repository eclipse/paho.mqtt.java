/*
 * Copyright (c) 2009, 2014 IBM Corp.
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

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * A NetworkModule provides access to a specific transport to the broker. This may be a plain socket, a TLS secured
 * connection, a serial line, etc.
 *
 * <h3>Lifecycle</h3>
 * Each NetworkModule instance has a lifecycle with the following logical states:
 * <ul>
 * <li><b>CREATED</b> - the instance holds no connection to its broker, no network resources are allocated and the
 * {@code get...Stream}-methods may return {@code null} or throw an {@link IOException}.
 * <li><b>CONNECTED</b> - the instance has an active connection to its broker, underlaying network ressources (e.g.
 * socket)
 * are allocated, the {@code get...Stream}-methods return open streams for reading and writing.</li>
 * <li><b>DISCONNECTED</b> - the instance holds no connection to its broker, underlaying network resources are closed
 * and
 * released, the {@code get...Stream}-methods will always throw an {@link IOException}.</li>
 * </ul>
 * <p>
 * The following transitions may occure:
 * <ul>
 * <li>{@code CREATED -> CONNECTED} - if {@link #start()} succeeded</li>
 * <li>{@code CREATED -> CREATED} - if {@link #start()} failed</li>
 * <li>{@code CONNECTED -> DISCONNECTED} - when {@link #stop()} is called; If an {@link IOException} occures on one of
 * the input /
 * output streams the {@code stop()}-method will be called.</li>
 * <li>{@code DISCONNECTED -> CONNECTED} - if {@link #start()} succeeded during a re-connect attempt</li>
 * </ul>
 * The {@link #start()}-method of a NetworkModule instance may only be called multiple times if either the previous call
 * resulted in an exception or {@link #stop()} has been called in between. The objects returned by
 * {@link #getInputStream()} und {@link #getOutputStream()} are most likely different instances each time
 * {@link #start()} had been called.
 */
public interface NetworkModule {

	/**
	 * Open the transport to the broker. The streams provided by {@link #getInputStream()} and
	 * {@link #getOutputStream()} are expected to be open for read / write operations after this method succeed.
	 *
	 * @throws IOException of any underlaying transport
	 * @throws MqttException if the server connection cannot be established, e.g. the connection is being refused
	 */
    void start() throws IOException, MqttException;

	/**
	 * Returns the input stream to be used for receiving messages. This method is usually called once directly after
	 * {@link #start()}. The returned input stream will be used in the CommsReceiver of the client connection.
	 *
	 * @return the input stream to be used for receiving messages ({@code null} may be returned if {@link #start()} had
	 *         never been sucessfully called on this instance)
	 * @throws IOException if an I/O error occurs when creating the input stream or the broker connection is not
	 *         established
	 */
    InputStream getInputStream() throws IOException;

	/**
	 * Returns the output stream to be used for sending messages. This method is usually called once directly after
	 * {@link #start()}. The returned output stream will be used in the CommsSender of the client connection.
	 *
	 * @return output stream to be used for sending messages ({@code null} may be returned if {@link #start()} had never
	 *         been sucessfully called on this instance)
	 * @throws IOException if an I/O error occurs when creating the output stream or the broker connection is not
	 *         established
	 */
    OutputStream getOutputStream() throws IOException;

	/**
	 * Close the transport to the broker. The streams provided by {@link #getInputStream()} and
	 * {@link #getOutputStream()} should be closed after this method returns.
	 * <p>
	 * TODO: Check whether the input and output streams are to be closed as well?
	 *
	 * @throws IOException if an I/O error occurs when closing the transport
	 */
    void stop() throws IOException;

	/**
	 * Returns the URI of the broker which was used to create this NetworkModule.
	 *
	 * @return the URI of the broker
	 */
    String getServerURI();
}
