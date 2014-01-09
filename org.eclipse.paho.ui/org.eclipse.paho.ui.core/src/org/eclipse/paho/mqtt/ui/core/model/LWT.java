/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
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
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.core.model;

/**
 * Last Will and Testament, when a client connects to the server it can define a topic and a message that needs to be
 * published automatically when it unexpectedly disconnects.
 * 
 * @author Bin Zhang
 */
public final class LWT extends Bindable {
	private static final long serialVersionUID = 1L;

	private QoS qos;
	private boolean retain;
	private String topic;
	private byte[] payload;

	public QoS getQos() {
		if (qos == null) {
			setQos(QoS.AT_MOST_ONCE);
		}
		return qos;
	}

	public void setQos(QoS qos) {
		this.qos = qos;
	}

	public boolean isRetain() {
		return retain;
	}

	public void setRetain(boolean retain) {
		this.retain = retain;
	}

	public String getTopic() {
		if (topic == null) {
			setTopic("lwt");// default value
		}
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("topic=").append(topic)
				.append(",").append("qos=").append(qos).append(",").append("retain=").append(retain).append(",")
				.append("payload=").append(payload).append("]").toString();
	}

}
