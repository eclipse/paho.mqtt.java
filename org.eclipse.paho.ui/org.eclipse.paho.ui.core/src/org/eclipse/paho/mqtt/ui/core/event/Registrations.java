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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Event Handler registrations
 * 
 * @author Bin Zhang
 */
public final class Registrations extends ArrayList<IRegistration> {
	private static final long serialVersionUID = 1L;

	/**
	 * @param reg
	 */
	public Registrations addRegistration(IRegistration reg) {
		add(reg);
		return this;
	}

	/**
	 * Unregister all the event handlers
	 */
	public void unregister() {
		Iterator<IRegistration> iter = iterator();
		while (iter.hasNext()) {
			iter.next().unregister();
		}
	}

}
