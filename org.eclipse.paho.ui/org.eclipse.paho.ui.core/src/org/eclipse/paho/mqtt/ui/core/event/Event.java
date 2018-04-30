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
package org.eclipse.paho.mqtt.ui.core.event;

/**
 * Event model for {@code IEventService}
 * 
 * @param <T>
 * 
 * @author Bin Zhang
 */
public class Event<T> {
	private final Selector selector;
	private T data;

	/**
	 * @param selector
	 */
	public Event(Selector selector) {
		this(selector, null);
	}

	/**
	 * @param selector
	 * @param data
	 */
	public Event(Selector selector, T data) {
		this.selector = selector;
		this.data = data;
	}

	public Selector getSelector() {
		return selector;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("selector=").append(selector)
				.append(",").append("data=").append(data).append("]").toString();
	}

}
