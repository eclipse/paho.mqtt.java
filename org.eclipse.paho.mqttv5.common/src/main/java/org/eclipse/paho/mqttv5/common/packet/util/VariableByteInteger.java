
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
package org.eclipse.paho.mqttv5.common.packet.util;

/**
 * Represents a Variable Byte Integer (VBI), as defined by the MQTT v5 (1.5.5)
 * specification.
 */
public class VariableByteInteger {
	private int value;
	private int length;

	public VariableByteInteger(int value) {
		this(value, -1);
	}

	public VariableByteInteger(int value, int length) {
		this.value = value;
		this.length = length;
	}

	/**
	 * Returns the number of bytes read when decoding this MBI
	 * 
	 * @return The Encoded Length of the VBI.
	 */
	public int getEncodedLength() {
		return length;
	}

	/**
	 * Returns the value of this MBI.
	 * 
	 * @return The value of the VBI.
	 */
	public int getValue() {
		return value;
	}
}
