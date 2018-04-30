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

import java.util.Date;

/**
 * 
 * @author Bin Zhang
 * 
 */
public final class History extends Bindable {
	private static final long serialVersionUID = 1L;

	// bean properties for databinding
	public static final String PROP_EVENT = "event";
	public static final String PROP_TOPIC = "topic";
	public static final String PROP_MSG = "message";
	public static final String PROP_QOS = "qos";
	public static final String PROP_RETAINED = "retained";
	public static final String PROP_TIME = "time";

	private String event;
	private String topic;
	private String message;
	private QoS qos;
	private Boolean retained;
	private Date time;

	/**
	 * @param event
	 */
	public History(String event) {
		this(event, null);
	}

	/**
	 * @param event
	 * @param message
	 */
	public History(String event, String message) {
		this(event, null, message, null, null);
	}

	/**
	 * @param event
	 * @param topic
	 * @param qos
	 */
	public History(String event, String topic, QoS qos) {
		this(event, topic, null, qos, null);
	}

	/**
	 * @param event
	 * @param topic
	 * @param message
	 * @param qos
	 * @param retained
	 */
	public History(String event, String topic, String message, QoS qos, Boolean retained) {
		this(event, topic, message, qos, retained, new Date());
	}

	/**
	 * @param event
	 * @param topic
	 * @param message
	 * @param qos
	 * @param retained
	 * @param time
	 */
	public History(String event, String topic, String message, QoS qos, Boolean retained, Date time) {
		this.event = event;
		this.topic = topic;
		this.message = message;
		this.qos = qos;
		this.retained = retained;
		this.time = time;
	}

	public String getEvent() {
		return event;
	}

	public String getTopic() {
		return topic;
	}

	public String getMessage() {
		return message;
	}

	public QoS getQos() {
		return qos;
	}

	public Boolean getRetained() {
		return retained;
	}

	public Date getTime() {
		return time;
	}

	/**
	 * @param history
	 */
	public void update(History history) {
		if (history != null) {
			this.event = history.getEvent();
			this.topic = history.getTopic();
			this.message = history.getMessage();
			this.qos = history.getQos();
			this.retained = history.getRetained();
			this.time = history.getTime();
		}
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("event=").append(event)
				.append(",").append("topic=").append(topic).append(",").append("message=").append(message).append(",")
				.append("qos=").append(qos).append(",").append("retained=").append(retained).append(",")
				.append("time=").append(time).append("]").toString();
	}

}
