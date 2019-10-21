/*******************************************************************************
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
package org.eclipse.paho.client.mqttv3.internal.wire;

/**
 * Represents a Multi-Byte Integer (MBI), as defined by the MQTT V3
 * specification.
 */
public class MultiByteInteger {
	private int value;
	private int length;
	
	public MultiByteInteger(int value) {
		this(value, -1);
	}
	
	public MultiByteInteger(int value, int length) {
		this.value = value;
		this.length = length;
	}
	
	/**
	 * @return the number of bytes read when decoding this MBI.
	 */
	public int getEncodedLength() {
		return length;
	}

	/**
	 * @return the value of this MBI.
	 */
	public int getValue() {
		return value;
	}
}
