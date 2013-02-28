/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttReceivedMessage extends MqttMessage {
	
	private int messageId;
	
	public void setMessageId(int msgId) {
		this.messageId = msgId;
	}

	public int getMessageId() {
		return messageId;
	}
	
	// This method exists here to get around the protected visibility of the
	// super class method.
	public void setDuplicate(boolean value) {
		super.setDuplicate(value);
	}
}
