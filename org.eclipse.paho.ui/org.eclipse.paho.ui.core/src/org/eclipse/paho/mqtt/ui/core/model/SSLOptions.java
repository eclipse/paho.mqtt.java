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

import java.util.Properties;

/**
 * 
 * @author Bin Zhang
 * 
 */
public final class SSLOptions extends Bindable {
	private static final long serialVersionUID = 1L;
	// copied from org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory
	// since it is internal only
	public static final String KEY_STORE="com.ibm.ssl.keyStore";
	public static final String KEY_STORE_PWD="com.ibm.ssl.keyStorePassword";
	public static final String TRUST_STORE="com.ibm.ssl.trustStore";
	public static final String TRUST_STORE_PWD="com.ibm.ssl.trustStorePassword";

	private String keyStoreLocation;
	private char[] keyStorePassword;

	private String trustStoreLocation;
	private char[] trustStorePassword;

	public String getKeyStoreLocation() {
		return keyStoreLocation;
	}

	public void setKeyStoreLocation(String keyStoreLocation) {
		this.keyStoreLocation = keyStoreLocation;
	}

	public char[] getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(char[] keyStorePassword) {
		this.keyStorePassword = keyStorePassword.clone();
	}

	public String getTrustStoreLocation() {
		return trustStoreLocation;
	}

	public void setTrustStoreLocation(String trustStoreLocation) {
		this.trustStoreLocation = trustStoreLocation;
	}

	public char[] getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(char[] trustStorePassword) {
		this.trustStorePassword = trustStorePassword.clone();
	}

	/**
	 * Returns properties
	 */
	public Properties toProperties() {
		if (keyStoreLocation == null) {
			throw new IllegalStateException("Key store location required!");//$NON-NLS-1$
		}
		if (trustStoreLocation == null) {
			throw new IllegalStateException("Trust store location required!");//$NON-NLS-1$
		}

		Properties props = new Properties();
		props.put(KEY_STORE, keyStoreLocation);
		if (keyStorePassword != null) {
			props.put(KEY_STORE_PWD, new String(keyStorePassword));
		}
		props.put(TRUST_STORE, trustStoreLocation);
		if (trustStorePassword != null) {
			props.put(TRUST_STORE_PWD, new String(trustStorePassword));
		}

		return props;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("keyStore=")
				.append(keyStoreLocation).append(",").append("trustStore=").append(trustStoreLocation).append("]")
				.toString();
	}

}
