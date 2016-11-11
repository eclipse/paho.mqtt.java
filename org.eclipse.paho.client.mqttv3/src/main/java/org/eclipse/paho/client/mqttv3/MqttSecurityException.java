/*******************************************************************************
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
package org.eclipse.paho.client.mqttv3;

/**
 * Thrown when a client is not authorized to perform an operation, or
 * if there is a problem with the security configuration.
 */
public class MqttSecurityException extends MqttException {
	private static final long serialVersionUID = 300L;

	/**
	 * Constructs a new <code>MqttSecurityException</code> with the specified code
	 * as the underlying reason.
	 * @param reasonCode the reason code for the exception.
	 */
	public MqttSecurityException(int reasonCode) {
		super(reasonCode);
	}

	/**
	 * Constructs a new <code>MqttSecurityException</code> with the specified 
	 * <code>Throwable</code> as the underlying reason.
	 * @param cause the underlying cause of the exception.
	 */
	public MqttSecurityException(Throwable cause) {
		super(cause);
	}
	/**
	 * Constructs a new <code>MqttSecurityException</code> with the specified 
	 * code and <code>Throwable</code> as the underlying reason.
	 * @param reasonCode the reason code for the exception.
	 * @param cause the underlying cause of the exception.
	 */
	public MqttSecurityException(int reasonCode, Throwable cause) {
		super(reasonCode, cause);
	}
}
