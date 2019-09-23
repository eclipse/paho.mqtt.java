/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corp.
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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

/**
 * Abstract super-class of all acknowledgement messages.
 */
public abstract class MqttAck extends MqttPersistableWireMessage {
	public MqttAck(byte type) {
		super(type);
	}
	
	@Override
	protected byte getMessageInfo() {
		return 0;
	}
}