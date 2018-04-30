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
 * 
 * @author Bin Zhang
 * 
 */
public class PublishMessage extends Bindable {
	private static final long serialVersionUID = 1L;

	// bean properties for databinding
	public static final String PROP_TOPIC = "topic";
	public static final String PROP_RETAIN = "retain";
	public static final String PROP_PAYLOAD = "payload";

	private String topic;
	private QoS qos;
	private boolean retain;
	private byte[] payload;

	public String getTopic() {
		if (topic == null) {
			setTopic("test");// default value
		}
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public QoS getQos() {
		if (qos == null) {
			setQos(QoS.AT_MOST_ONCE);// default value
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

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload.clone();
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("topic=").append(topic)
				.append(",").append("qos=").append(qos).append(",").append("retain=").append(retain).append(",")
				.append("payload=").append(payload).append("]").toString();
	}

}
