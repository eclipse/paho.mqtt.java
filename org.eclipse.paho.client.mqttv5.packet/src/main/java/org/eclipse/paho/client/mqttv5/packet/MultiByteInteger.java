
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
package org.eclipse.paho.client.mqttv5.packet;

/**
 * Represents a Multi-Byte Integer (MBI), as defined by the MQTT v5
 * specification.
 */
public class MultiByteInteger {
	private long value;
	private int length;
	
	public MultiByteInteger(long value){
		this(value, -1);
	}
	
	public MultiByteInteger(long value, int length){
		this.value = value;
		this.length = length;
	}
	
	/**
	 * Returns the number of bytes read when decoding this MBI
	 */
	public int getEncodedLength(){
		return length;
	}
	
	/**
	 * Returns the value of this MBI.
	 */
	public long getValue() {
		return value;
	}
}
