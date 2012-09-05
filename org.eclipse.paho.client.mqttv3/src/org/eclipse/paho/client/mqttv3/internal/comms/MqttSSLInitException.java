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
package org.eclipse.paho.client.mqttv3.internal.comms;

/**
 * Exception thrown if a problem whilst initializing a SSL socket (listener or initiator).
 * 
 */
public class MqttSSLInitException extends MqttDirectException {


	private static final long serialVersionUID = 1L;

	public MqttSSLInitException() {
	}

	public MqttSSLInitException(String reason, Throwable theCause) {
		super(reason, theCause);
	}

	public MqttSSLInitException(long theMsgId, Object[] theInserts) {
		super(theMsgId, theInserts);
	}

	public MqttSSLInitException(long theMsgId, Object[] theInserts, Throwable cause) {
		super(theMsgId, theInserts, cause);
	}

}
