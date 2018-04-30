/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.client.mqttv3.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.paho.client.mqttv3.MqttException;


public class PropertyResourceBundle {
	
	private Hashtable lookup;
	private InputStream stream;
	
	public PropertyResourceBundle (InputStream stream) {
		this.stream = stream;
		lookup = new Hashtable();
		addToHash();
	}
	
	private void addToHash() {
		boolean keyFound = false;
		boolean startLine = true;
		
		try {
			StringBuffer temp = new StringBuffer();
			String key = "";
			String value = "";
			for (int i=0; ((i = stream.read()) >= 0);) {
				if (i == '#' && startLine) {
					// Comment line skip to the end
					while (i != '\n' && i >= 0) {
						i = stream.read();
					}
				} else if (i == '\n') {
					// We have reached the end of the line
					value = temp.toString();
					if (value.indexOf('\r') != -1) value = value.substring(0,value.indexOf('\r')); // trim off carage return
					temp.delete(0, temp.length());
					if (key != null) {
						lookup.put(key, value);
					}
					startLine = true;
					keyFound = false;
					key = null;
					continue;
				} else if (i == '=') {
					if (!keyFound) {
						key = temp.toString();
						temp.delete(0, temp.length());
						keyFound = true;
					} else {
						temp.append((char)i);
					}
				} else {
					temp.append((char)i);
					startLine = false;
				}
			}
			// End of file, add final property
			value = temp.toString();
			if (value.indexOf('\r') != -1) value = value.substring(0,value.indexOf('\r')); // trim off carage return
			if (key != null) lookup.put(key, value);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}


	public static PropertyResourceBundle getBundle(String name, boolean nls) throws MqttException {
		Locale locale = Locale.getDefault();
		InputStream stream = null;
		
		// Search for .properties files
		name = "/" + name.replace('.', '/') + ".properties";
		
		// Retrieve the default
	    stream = locale.getClass().getResourceAsStream(name);

		if (nls) {
		    // Retrieve the language specific properties
			InputStream temp = locale.getClass().getResourceAsStream(name + '_' + locale.language);
			if (temp != null) {
				stream = temp;
			}
			
			// Retrieve the country specific properties for this language
			temp = locale.getClass().getResourceAsStream(name + '_' + locale.language +'_' + locale.country);
			if (temp != null) {
				stream = temp;
			}
		}
		
		// Finally create the Resource
		if (stream != null) {
			return new PropertyResourceBundle(stream);
		} else {
			throw new MqttException(MqttException.REASON_CODE_CATALOG_NOT_FOUND);
		}
		
	}
	
	public String getString(String id) {
		try {
			String key = id;
			if (lookup.containsKey(key)) {
				return (String) lookup.get(key);
			}
		} catch (NumberFormatException e) {
		}
		return "";
	}
	
	private static class Locale {
		
		private static Locale defaultLocale = null; // One per JVM
		private String language;
		private String country;
		
		public Locale(String language, String country) {
			this.language = language;
			this.country = country;
		}
		
		public static Locale getDefault() {
			if (defaultLocale==null) {
				String def = System.getProperty("microedition.locale");
				if (def != null) {
					String defLanguage = def.substring(0, def.indexOf("-"));
					String defCountry = def.substring(def.indexOf("-")+1);
					defaultLocale = new Locale(defLanguage, defCountry);
				}
			}
			return defaultLocale;
		}
	}

}
