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
package org.eclipse.paho.mqtt.ui.core.event.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.paho.mqtt.ui.core.event.Event;
import org.eclipse.paho.mqtt.ui.core.event.IEventHandler;
import org.eclipse.paho.mqtt.ui.core.event.IEventService;
import org.eclipse.paho.mqtt.ui.core.event.IRegistration;
import org.eclipse.paho.mqtt.ui.core.event.Selector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * {@code IEventService} implementation based on OSGi {@code EventAdmin}
 * 
 * @author Bin Zhang
 */
@SuppressWarnings("unchecked")
public final class EventService implements IEventService {
	private static final String PROP_DATA = "event.data"; //$NON-NLS-1$
	private static final String PROP_SELECTOR = "event.selector"; //$NON-NLS-1$
	private final EventAdmin eventAdmin;
	private final BundleContext context;

	/**
	 * @param context
	 */
	public EventService(BundleContext context) {
		this.context = context;
		ServiceReference<EventAdmin> ref = context.getServiceReference(EventAdmin.class);
		eventAdmin = context.getService(ref);
		context.registerService(IEventService.class.getName(), this, null);
	}

	@Override
	public <T> IRegistration registerHandler(Selector selector, IEventHandler<T> handler) {
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(EventConstants.EVENT_TOPIC, selector.select());
		ServiceRegistration<?> reg = context.registerService(EventHandler.class.getName(), new EventHandlerAdaptor<T>(
				handler), props);
		return new Registration(reg);
	}

	@Override
	public <T> void sendEvent(Event<T> event) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(PROP_DATA, event.getData());
		props.put(PROP_SELECTOR, event.getSelector());
		sendEvent(event.getSelector().select(), props);
	}

	/**
	 * @param topic
	 * @param props
	 */
	private void sendEvent(String topic, Map<String, Object> props) {
		// asynchronous event delivery
		eventAdmin.postEvent(new org.osgi.service.event.Event(topic, props));
	}

	/**
	 * EventHandlerAdaptor
	 * 
	 * @param <T>
	 */
	private class EventHandlerAdaptor<T> implements EventHandler {
		private final IEventHandler<T> handler;

		public EventHandlerAdaptor(IEventHandler<T> handler) {
			this.handler = handler;
		}

		@Override
		public void handleEvent(org.osgi.service.event.Event e) {
			Event<T> event = new Event<T>((Selector) e.getProperty(PROP_SELECTOR));
			event.setData((T) e.getProperty(PROP_DATA));
			handler.handleEvent(event);
		}
	}
}
