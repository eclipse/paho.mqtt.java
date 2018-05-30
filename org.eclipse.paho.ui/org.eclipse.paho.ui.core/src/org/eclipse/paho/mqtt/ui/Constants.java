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
package org.eclipse.paho.mqtt.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constants
 * 
 * @author Bin Zhang
 */
public final class Constants {
	// system property to be over written
	public static final String PROP_DATA_DIR = "paho.data.dir";
	// data dir inside paho UI application directory
	public static final String DATA_DIR = "data";
	public static final String DATA_FILE_EXTENSION = ".paho";
	public static final String TCP_SERVER_URI = "tcp://localhost:1883";
	public static final String SSL_SERVER_URI = "ssl://localhost:8883";
	public static final String DEFAULT_SERVER_URI = TCP_SERVER_URI;
	public static final List<String> HA_SERVER_URIS = Collections.unmodifiableList(Arrays.asList(TCP_SERVER_URI,
			SSL_SERVER_URI));
	public static final List<String> MQTT_SCHEMES = Collections.unmodifiableList(Arrays.asList("tcp", "ssl", "local"));
	public static final int BUTTON_WIDTH = 100;
	public static final String DEFAULT_DATA_FORMAT = "yyyy-MM-dd HH:mm:ss";

	// Image keys
	public static class ImageKeys {
		public static final String IMG_CONNECTION = "connection";
		public static final String IMG_CONNECTION_EDIT = "connection_edit";
		public static final String IMG_CONNECTION_GRAY = "connection_gray";
		public static final String IMG_MQTT = "mqtt";
		public static final String IMG_ADD = "add";
		public static final String IMG_ADD_GRAY = "add_gray";
		public static final String IMG_REMOVE = "remove";
		public static final String IMG_REMOVE_GRAY = "remove_gray";
		public static final String IMG_CLEAR = "clear";
		public static final String IMG_OPTIONS = "options";
		public static final String IMG_HISTORY = "history";
		public static final String IMG_LASTMSG = "lastmsg";
	}

}
