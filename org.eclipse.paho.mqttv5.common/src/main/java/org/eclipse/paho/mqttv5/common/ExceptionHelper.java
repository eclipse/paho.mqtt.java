/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common;

/**
 * Utility class to help create exceptions of the correct type.
 */
public class ExceptionHelper {
	public static MqttException createMqttException(int reasonCode) {
		
		return new MqttException(reasonCode);
	}

	public static MqttException createMqttException(Throwable cause) {

		return new MqttException(cause);
	}
	
	/**
	 * Returns whether or not the specified class is available to the current
	 * class loader.  This is used to protect the code against using Java SE
	 * APIs on Java ME.
	 * @param className The ClassName
	 * @return if the class is available
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
