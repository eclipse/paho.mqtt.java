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
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * This is a Class containing all of the low level packet exceptions that may be useful in identifying the cause of protocol errors.
 * @author jamessutton
 */
public class MqttPacketException extends MqttException {
	
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Protocol Exceptions for the CONNECT Packet
	 */
	public static final int PACKET_CONNECT_ERROR_UNSUPPORTED_PROTOCOL_NAME		= 51000; // 3.1.2.1 - If Protocol name is not 'MQTT', return Unsupported Protocol Version Error.
	public static final int PACKET_CONNECT_ERROR_UNSUPPORTED_PROTOCOL_VERSION	= 51001; // 3.1.2.2 - If Protocol version is not 5, return Unsupported Protocol Version Error.
	public static final int PACKET_CONNECT_ERROR_INVALID_RESERVE_FLAG			= 51002; // 3.1.2.3 - If Reserved flag is not 0, return Malformed Packet Error.
	public static final int PACKET_CONNECT_ERROR_INVALID_WILL_QOS				= 51003; // 3.1.2.6 - If Will QoS is 0x03, return Malformed Packet Error.

	
	public MqttPacketException(int reasonCode) {
		super(reasonCode);
	}
	

}
