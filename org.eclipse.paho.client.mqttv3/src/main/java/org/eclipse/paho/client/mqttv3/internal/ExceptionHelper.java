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
package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

/**
 * Utility class to help create exceptions of the correct type.
 */
public class ExceptionHelper {
	public static MqttException createMqttException(int reasonCode) {
		if ((reasonCode == MqttException.REASON_CODE_FAILED_AUTHENTICATION) || 
			(reasonCode == MqttException.REASON_CODE_NOT_AUTHORIZED)) {
			return new MqttSecurityException(reasonCode);
		}
		
		return new MqttException(reasonCode);
	}

	public static MqttException createMqttException(Throwable cause) {
		if (cause.getClass().getName().equals("java.security.GeneralSecurityException")) {
			return new MqttSecurityException(cause);
		}
		return new MqttException(cause);
	}
	
	/**
	 * Returns whether or not the specified class is available to the current
	 * class loader.  This is used to protect the code against using Java SE
	 * APIs on Java ME.
	 * @param className The name of the class
	 * @return If true, the class is available
	 */
	public static boolean isClassAvailable(String className) {
		boolean result = false;
		try {
			Class.forName(className);
			result = true;
		}
		catch (ClassNotFoundException ex) {
		}
		return result;
	}

	// Utility classes should not have a public or default constructor.
	private ExceptionHelper() {
	}
}
