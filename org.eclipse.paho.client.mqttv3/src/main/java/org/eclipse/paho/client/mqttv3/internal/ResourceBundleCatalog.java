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
package org.eclipse.paho.client.mqttv3.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleCatalog extends MessageCatalog {
	
	private ResourceBundle bundle;
	
	public ResourceBundleCatalog() throws MissingResourceException {
		bundle = ResourceBundle.getBundle("org.eclipse.paho.client.mqttv3.internal.nls.messages");
	}

	protected String getLocalizedMessage(int id) {
		try {
			return bundle.getString(Integer.toString(id));
		} catch(MissingResourceException mre) {
			return "MqttException";
		}
	}
}
