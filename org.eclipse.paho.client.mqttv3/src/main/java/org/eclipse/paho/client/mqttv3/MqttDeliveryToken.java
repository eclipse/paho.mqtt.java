/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3;

/**
 * Provides a mechanism to track the delivery progress of a message.
 * 
 * <p>
 * Used to track the the delivery progress of a message when a publish is 
 * executed in a non-blocking manner (run in the background)</p>
 *  
 * @see MqttToken
 */
public class MqttDeliveryToken extends MqttToken implements IMqttDeliveryToken {
		
	
	public MqttDeliveryToken() {
		super();
	}
	
	public MqttDeliveryToken(String logContext) {
		super(logContext);
	}

	/**
	 * Returns the message associated with this token.
	 * <p>Until the message has been delivered, the message being delivered will
	 * be returned. Once the message has been delivered <code>null</code> will be 
	 * returned.
	 * @return the message associated with this token or null if already delivered.
	 * @throws MqttException if there was a problem completing retrieving the message
	 */
	public MqttMessage getMessage() throws MqttException {
		return internalTok.getMessage();
	}
	
	protected void setMessage(MqttMessage msg) {
		internalTok.setMessage(msg);
	}
}
