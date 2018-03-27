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
 * Reference of MQTT v5 Return Codes - 2.4
 */
public class MqttReturnCode {
	
	/** Success and general Return Codes **/
	public static final int RETURN_CODE_SUCCESS 						= 0x00;	// 0
	public static final int RETURN_CODE_MAX_QOS_0 						= 0x00;	// 0
	public static final int RETURN_CODE_MAX_QOS_1 						= 0x01;	// 1
	public static final int RETURN_CODE_MAX_QOS_2 						= 0x02;	// 2
	public static final int RETURN_CODE_DISCONNECT_WITH_WILL_MESSAGE	= 0x04;	// 4
	public static final int RETURN_CODE_NO_MATCHING_SUBSCRIBERS			= 0x10;	// 16
	public static final int RETURN_CODE_NO_SUBSCRIPTION_EXISTED			= 0x11;	// 17
	public static final int RETURN_CODE_CONTINUE_AUTHENTICATION			= 0x18;	// 24
	public static final int RETURN_CODE_RE_AUTHENTICATE					= 0x19;	// 25

	/** Error Return Codes **/
	public static final int RETURN_CODE_UNSPECIFIED_ERROR 				= 0x80;	// 128
	public static final int RETURN_CODE_MALFORMED_CONTROL_PACKET 		= 0x81;	// 129
	public static final int RETURN_CODE_PROTOCOL_ERROR					= 0x82;	// 130
	public static final int RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR 	= 0x83;	// 131
	public static final int RETURN_CODE_UNSUPPORTED_PROTOCOL_VERSION 	= 0x84;	// 132
	public static final int RETURN_CODE_IDENTIFIER_NOT_VALID			= 0x85;	// 133
	public static final int RETURN_CODE_BAD_USERNAME_OR_PASSWORD		= 0x86;	// 134
	public static final int RETURN_CODE_NOT_AUTHORIZED					= 0x87;	// 135
	public static final int RETURN_CODE_SERVER_UNAVAILABLE				= 0x88;	// 136
	public static final int RETURN_CODE_SERVER_BUSY						= 0x89;	// 137
	public static final int RETURN_CODE_BANNED							= 0x8A;	// 138
	public static final int RETURN_CODE_SERVER_SHUTTING_DOWN			= 0x8B;	// 139
	public static final int RETURN_CODE_BAD_AUTHENTICATION				= 0x8C;	// 140
	public static final int RETURN_CODE_KEEP_ALIVE_TIMEOUT				= 0x8D;	// 141
	public static final int RETURN_CODE_SESSION_TAKEN_OVER				= 0x8E;	// 142
	public static final int RETURN_CODE_TOPIC_FILTER_NOT_VALID			= 0x8F;	// 143
	public static final int RETURN_CODE_TOPIC_NAME_INVALID				= 0x90;	// 144
	public static final int RETURN_CODE_PACKET_ID_IN_USE				= 0x91;	// 145
	public static final int RETURN_CODE_PACKET_ID_NOT_FOUND 			= 0x92;	// 146
	public static final int RETURN_CODE_RECEIVE_MAXIMUM_EXCEEDED		= 0x93;	// 147
	public static final int RETURN_CODE_TOPIC_ALIAS_NOT_ACCEPTED				= 0x94;	// 148
	public static final int RETURN_CODE_PACKET_TOO_LARGE				= 0x95;	// 149
	public static final int RETURN_CODE_MESSAGE_RATE_TOO_HIGH			= 0x96;	// 150
	public static final int RETURN_CODE_QUOTA_EXCEEDED					= 0x97;	// 151
	public static final int RETURN_CODE_ADMINISTRITIVE_ACTION			= 0x98;	// 152
	public static final int RETURN_CODE_PAYLOAD_FORMAT_INVALID			= 0x99;	// 153
	public static final int RETURN_CODE_RETAIN_NOT_SUPPORTED			= 0x9A;	// 154
	public static final int RETURN_CODE_QOS_NOT_SUPPORTED				= 0x9B;	// 155
	public static final int RETURN_CODE_USE_ANOTHER_SERVER				= 0x9C;	// 156
	public static final int RETURN_CODE_SERVER_MOVED					= 0x9D;	// 157
	public static final int RETURN_CODE_SHARED_SUB_NOT_SUPPORTED 		= 0x9E;	// 158
	public static final int RETURN_CODE_CONNECTION_RATE_EXCEEDED		= 0x9F;	// 159
	public static final int RETURN_CODE_MAXIMUM_CONNECT_TIME			= 0xA0;	// 160
	public static final int RETURN_CODE_SUB_IDENTIFIERS_NOT_SUPPORTED	= 0xA1;	// 161
	public static final int RETURN_CODE_WILDCARD_SUB_NOT_SUPPORTED		= 0xA2;	// 162

		
	private MqttReturnCode() {
	    throw new IllegalAccessError("Utility class");
	 }


}
