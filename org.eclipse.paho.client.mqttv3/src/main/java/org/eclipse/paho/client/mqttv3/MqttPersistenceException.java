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
 * This exception is thrown by the implementor of the persistence
 * interface if there is a problem reading or writing persistent data.
 */
public class MqttPersistenceException extends MqttException {
	private static final long serialVersionUID = 300L;

	/** Persistence is already being used by another client. */
	public static final short REASON_CODE_PERSISTENCE_IN_USE	= 32200;
	
	/**
	 * Constructs a new <code>MqttPersistenceException</code>
	 */
	public MqttPersistenceException() {
		super(REASON_CODE_CLIENT_EXCEPTION);
	}
	
	/**
	 * Constructs a new <code>MqttPersistenceException</code> with the specified code
	 * as the underlying reason.
	 * @param reasonCode the reason code for the exception.
	 */
	public MqttPersistenceException(int reasonCode) {
		super(reasonCode);
	}
	/**
	 * Constructs a new <code>MqttPersistenceException</code> with the specified 
	 * <code>Throwable</code> as the underlying reason.
	 * @param cause the underlying cause of the exception.
	 */
	public MqttPersistenceException(Throwable cause) {
		super(cause);
	}
	/**
	 * Constructs a new <code>MqttPersistenceException</code> with the specified 
	 * <code>Throwable</code> as the underlying reason.
	 * @param reason the reason code for the exception.
	 * @param cause the underlying cause of the exception.
	 */
	public MqttPersistenceException(int reason, Throwable cause) {
		super(reason, cause);
	}
}
