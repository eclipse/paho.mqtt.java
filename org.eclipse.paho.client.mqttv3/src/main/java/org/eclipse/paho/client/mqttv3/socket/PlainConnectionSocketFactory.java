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
import javax.net.SocketFactory;

/**
 * The default class for creating plain (unencrypted) sockets.
 */
public class PlainConnectionSocketFactory implements ConnectionSocketFactory {
	private SocketFactory factory;

	public PlainConnectionSocketFactory() {
		super();
	}
	
	public PlainConnectionSocketFactory(SocketFactory factory) {
		super();
		this.factory = factory;
	}

	public Socket createSocket() throws IOException {
		if (factory != null) {
			return factory.createSocket();
		} else {
			return new Socket();
		}
	}

	public Socket connectSocket(
			final int connectTimeout,
			final Socket socket,
			final String host,
			final InetSocketAddress remoteAddress,
			final InetSocketAddress localAddress) throws IOException {
		final Socket s = socket != null ? socket : createSocket();
		if (localAddress != null) {
			s.bind(localAddress);
		}
		try {
			s.connect(remoteAddress, connectTimeout);
		} catch (final IOException e) {
			try {
				s.close();
			} catch (final IOException ignore) {
			}
			throw e;
		}
		return s;
	}
}
