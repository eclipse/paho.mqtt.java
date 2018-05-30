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

import org.eclipse.paho.mqtt.ui.nls.Messages;

/**
 * QoS of MQTT message
 * 
 * @author Bin Zhang
 */
public enum QoS {

	// At most once - Fire and Forget <=1
	AT_MOST_ONCE(0, Messages.QOS_AT_MOST_ONCE),

	// At least once - Acknowledged delivery >=1
	AT_LEAST_ONCE(1, Messages.QOS_AT_LEAST_ONCE),

	// Exactly once - Assured delivery =1
	EXACTLY_ONCE(2, Messages.QOS_EXACTLY_ONCE);

	// value 3, Reserved

	private final int value;
	private final String label;

	/**
	 * @param value
	 * @param label
	 */
	private QoS(int value, String label) {
		this.value = value;
		this.label = label;
	}

	/**
	 * The QoS value (0,1,2)
	 */
	public int getValue() {
		return value;
	}

	/**
	 * The readable name of QoS
	 */
	public String getLabel() {
		return new StringBuilder().append(value).append(" - ").append(label).toString();
	}

	/**
	 * @param value
	 * @return QoS
	 * @throws IllegalArgumentException if the value is invalid
	 */
	public static QoS valueOf(int value) {
		for (QoS q : QoS.values()) {
			if (q.value == value)
				return q;
		}
		throw new IllegalArgumentException(String.format("Invalid QoS: %d", value));
	}

	@Override
	public String toString() {
		return getLabel();
	}

}