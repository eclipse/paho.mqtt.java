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

/**
 * Reference of MQTT v5 Property Identifiers - 2.2.2.2
 */
public class MqttPropertyIdentifiers {
	
	public static final byte PAYLOAD_FORMAT_INDICATOR_IDENTIFIER 		= 0x01; // 1
	public static final byte PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER 	= 0x02; // 2
	public static final byte CONTENT_TYPE_IDENTIFIER 					= 0x03; // 3
	public static final byte RESPONSE_TOPIC_IDENTIFIER 					= 0x08; // 8
	public static final byte CORRELATION_DATA_IDENTIFIER				= 0x09; // 9
	public static final byte SUBSCRIPTION_IDENTIFIER					= 0x0B; // 11
	public static final byte SESSION_EXPIRY_INTERVAL_IDENTIFIER 		= 0x11; // 17
	public static final byte ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER 		= 0x12; // 18
	public static final byte SERVER_KEEP_ALIVE_IDENTIFIER 				= 0x13; // 19
	public static final byte AUTH_METHOD_IDENTIFIER 					= 0x15; // 21
	public static final byte AUTH_DATA_IDENTIFIER 						= 0x16; // 22
	public static final byte REQUEST_PROBLEM_INFO_IDENTIFIER 			= 0x17; // 23
	public static final byte WILL_DELAY_INTERVAL_IDENTIFIER 			= 0x18; // 24
	public static final byte REQUEST_RESPONSE_INFO_IDENTIFIER 			= 0x19; // 25
	public static final byte RESPONSE_INFO_IDENTIFIER					= 0x1A; // 26
	public static final byte SERVER_REFERENCE_IDENTIFIER 				= 0x1C; // 28
	public static final byte REASON_STRING_IDENTIFIER 					= 0x1F; // 31
	public static final byte RECEIVE_MAXIMUM_IDENTIFIER 				= 0x21; // 33
	public static final byte TOPIC_ALIAS_MAXIMUM_IDENTIFIER 			= 0x22; // 34
	public static final byte TOPIC_ALIAS_IDENTIFIER						= 0x23; // 35
	public static final byte MAXIMUM_QOS_IDENTIFIER						= 0x24; // 36
	public static final byte RETAIN_AVAILABLE_IDENTIFIER 				= 0x25; // 37
	public static final byte USER_DEFINED_PAIR_IDENTIFIER				= 0x26; // 38
	public static final byte MAXIMUM_PACKET_SIZE_IDENTIFIER				= 0x27; // 39
	public static final byte WILDCARD_SUB_AVAILABLE_IDENTIFIER			= 0x28; // 40
	public static final byte SUBSCRIPTION_AVAILABLE_IDENTIFIER			= 0x29; // 41
	public static final byte SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER	= 0x2A; // 42
			
	private MqttPropertyIdentifiers() {
	    throw new IllegalAccessError("Utility class");
	 }


}
