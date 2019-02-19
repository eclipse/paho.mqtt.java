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
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import org.eclipse.paho.mqttv5.common.MqttException;

public class MqttPingReq extends MqttWireMessage{

	public static final String KEY = "Ping";
	
	public MqttPingReq(){
		super(MqttWireMessage.MESSAGE_TYPE_PINGREQ);
	}
	
	/**
	 * Returns <code>false</code> as message IDs are not required for MQTT
	 * PINGREQ messages.
	 */
	@Override
	public boolean isMessageIdRequired() {
		return false;
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		return new byte[0];
	}
	
	@Override
	protected byte getMessageInfo() {
		return 0;
	}
	
	@Override
	public String getKey() {
		return KEY;
	}
}
