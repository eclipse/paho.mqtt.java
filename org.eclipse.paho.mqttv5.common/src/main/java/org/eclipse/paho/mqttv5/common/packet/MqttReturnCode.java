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
 * Reference of MQTT v5 Return Codes - 2.2.2
 */
public class MqttReturnCode {
	
	public static final int RETURN_CODE_SUCCESS 						= 0x00;
	public static final int RETURN_CODE_MAX_QOS_0 						= 0x00;
	public static final int RETURN_CODE_MAX_QOS_1 						= 0x01;
	public static final int RETURN_CODE_MAX_QOS_2 						= 0x02;
	public static final int RETURN_CODE_DISCONNECT_WITH_WILL_MESSAGE	= 0x04;
	public static final int RETURN_CODE_NO_SUBSCRIPTION_EXISTED			= 0x11;
	public static final int RETURN_CODE_CONTINUE_AUTHENTICATION			= 0x18;
	public static final int RETURN_CODE_RE_AUTHENTICATE					= 0x19;
	public static final int RETURN_CODE_UNSPECIFIED_ERROR 				= 0x80;
	public static final int RETURN_CODE_MALFORMED_CONTROL_PACKET 		= 0x81;
	public static final int RETURN_CODE_PROTOCOL_ERROR					= 0x82;
	public static final int RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR 	= 0x83;
	public static final int RETURN_CODE_UNSUPPORTED_PROTOCOL_VERSION 	= 0x84;
	public static final int RETURN_CODE_IDENTIFIER_NOT_VALID			= 0x85;
	public static final int RETURN_CODE_BAD_USERNAME_OR_PASSWORD		= 0x86;
	public static final int RETURN_CODE_NOT_AUTHORIZED					= 0x87;
	public static final int RETURN_CODE_SERVER_UNAVAILABLE				= 0x88;
	public static final int RETURN_CODE_SERVER_BUSY						= 0x89;
	public static final int RETURN_CODE_BANNED							= 0x8A;
	public static final int RETURN_CODE_SERVER_SHUTTING_DOWN			= 0x8B;
	public static final int RETURN_CODE_BAD_AUTHENTICATION				= 0x8C;
	public static final int RETURN_CODE_KEEP_ALIVE_TIMEOUT				= 0x8D;
	public static final int RETURN_CODE_SESSION_TAKEN_OVER				= 0x8E;
	public static final int RETURN_CODE_TOPIC_FILTER_NOT_VALID			= 0x8F;
	public static final int RETURN_CODE_TOPIC_NAME_INVALID				= 0x90;
	public static final int RETURN_CODE_PACKET_ID_IN_USE				= 0x91;
	public static final int RETURN_CODE_PACKET_ID_NOT_FOUND 			= 0x92;
	public static final int RETURN_CODE_RECEIVE_MAXIMUM_EXCEEDED		= 0x93;
	public static final int RETURN_CODE_PACKET_TOO_LARGE				= 0x95;
	public static final int RETURN_CODE_MESSAGE_RATE_TOO_HIGH			= 0x96;
	public static final int RETURN_CODE_QUOTA_EXCEEDED					= 0x97;
	public static final int RETURN_CODE_ADMINISTRITIVE_ACTION			= 0x98;
	public static final int RETURN_CODE_PAYLOAD_FORMAT_INVALID			= 0x99;
	public static final int RETURN_CODE_RETAIN_NOT_SUPPORTED			= 0x9A;
	public static final int RETURN_CODE_QOS_NOT_SUPPORTED				= 0x9B;
	public static final int RETURN_CODE_USE_ANOTHER_SERVER				= 0x9C;
	public static final int RETURN_CODE_SERVER_MOVED					= 0x9D;
	public static final int RETURN_CODE_SHARED_SUB_NOT_SUPPORTED 		= 0x9E;
	public static final int RETURN_CODE_CONNECTION_RATE_EXCEEDED		= 0x9F;
	public static final int RETURN_CODE_SUB_IDENTIFIERS_NOT_SUPPORTED	= 0xA1;
	public static final int RETURN_CODE_WILDCARD_SUB_NOT_SUPPORTED		= 0xA2;
	
	
	// Return codes in current query...
	public static final int RETURN_CODE_NO_MATCHING_SUBSCRIBERS			= 0x10;
	public static final int RETURN_CODE_ALIAS_NOT_ACCEPTED				= 0x94;
	public static final int RETURN_CODE_MAXIMUM_CONNECT_TIME			= 0xA0;
	
	private MqttReturnCode() {
	    throw new IllegalAccessError("Utility class");
	 }


}
