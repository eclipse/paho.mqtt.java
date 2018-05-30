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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.mqtt.ui.Constants;

/**
 * Connection model
 * 
 * @author Bin Zhang
 */
public class Connection extends Bindable {
	private static final long serialVersionUID = 1L;

	// bean properties for databinding
	public static final String PROP_CLIENTID = "clientId";
	public static final String PROP_SERVERURI = "serverURI";
	public static final String PROP_OPT_LWT_TOPIC = "options.lwt.topic";
	public static final String PROP_OPT_LWT_PAYLOAD = "options.lwt.payload";
	public static final String PROP_OPT_LWT_RETAIN = "options.lwt.retain";
	public static final String PROP_OPT_SSL_KS = "options.sslOptions.keyStoreLocation";
	public static final String PROP_OPT_SSL_KSPWD = "options.sslOptions.keyStorePassword";
	public static final String PROP_OPT_SSL_TS = "options.sslOptions.trustStoreLocation";
	public static final String PROP_OPT_SSL_TSPWD = "options.sslOptions.trustStorePassword";
	public static final String PROP_OPT_LOGIN_USERNAME = "options.login.username";
	public static final String PROP_OPT_LOGIN_PASSWORD = "options.login.password";
	public static final String PROP_OPT_PERSISTENCE_DIRECTORY = "options.persistenceDirectory";
	// flags
	public static final String PROP_OPT_CLEAN_SESSION = "options.cleanSession";
	public static final String PROP_OPT_SSL_ENABLED = "options.sslEnabled";
	public static final String PROP_OPT_HA_ENABLED = "options.haEnabled";
	public static final String PROP_OPT_LWT_ENABLED = "options.lwtEnabled";
	public static final String PROP_OPT_LOGIN_ENABLED = "options.loginEnabled";
	public static final String PROP_OPT_PERSISTENCE_ENABLED = "options.persistenceEnabled";
	public static final String PROP_OPT_KEEP_ALIVE_INTERVAL = "options.keepAliveInterval";
	public static final String PROP_OPT_CONNECTION_TIMEOUT = "options.connectionTimeout";

	// members
	private final String id;
	private String name;
	private String clientId;
	private String serverURI;
	private Options options;

	/**
	 * @param name
	 */
	public Connection(String name) {
		this.id = UUID.randomUUID().toString().replace("-", "");
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClientId() {
		if (clientId == null) {
			setClientId(MqttClient.generateClientId());
		}
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getServerURI() {
		if (serverURI == null) {
			setServerURI(Constants.DEFAULT_SERVER_URI);
		}
		return serverURI;
	}

	public void setServerURI(String serverURI) {
		this.serverURI = serverURI;
		// firePropertyChange("serverURI", this.serverURI, this.serverURI = serverURI);
	}

	public Options getOptions() {
		if (options == null) {
			options = new Options();
		}
		return options;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Connection other = (Connection) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getClass().getSimpleName()).append(" [").append("id=").append(id).append(",")
				.append("name=").append(name).append(",").append("clientId=").append(clientId).append(",")
				.append("serverURI=").append(serverURI).append("]").toString();
	}

	/**
	 * Options
	 */
	public class Options extends Bindable {
		private static final long serialVersionUID = 1L;

		private boolean cleanSession = true;
		private boolean sslEnabled;
		private boolean haEnabled;
		private boolean lwtEnabled;
		private boolean loginEnabled;
		private boolean persistenceEnabled;

		// in seconds
		private int keepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
		// in seconds
		private int connectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;

		private String persistenceDirectory;
		private Login login;
		private SSLOptions sslOptions;
		private List<ServerURI> serverURIs;
		private LWT lwt;

		public boolean isCleanSession() {
			return cleanSession;
		}

		public void setCleanSession(boolean cleanSession) {
			this.cleanSession = cleanSession;
		}

		public boolean isSslEnabled() {
			return sslEnabled;
		}

		public void setSslEnabled(boolean sslEnabled) {
			this.sslEnabled = sslEnabled;
		}

		public boolean isHaEnabled() {
			return haEnabled;
		}

		public void setHaEnabled(boolean haEnabled) {
			this.haEnabled = haEnabled;
		}

		public boolean isLwtEnabled() {
			return lwtEnabled;
		}

		public void setLwtEnabled(boolean lwtEnabled) {
			this.lwtEnabled = lwtEnabled;
		}

		public boolean isLoginEnabled() {
			return loginEnabled;
		}

		public void setLoginEnabled(boolean loginEnabled) {
			this.loginEnabled = loginEnabled;
		}

		public boolean isPersistenceEnabled() {
			return persistenceEnabled;
		}

		public void setPersistenceEnabled(boolean persistenceEnabled) {
			this.persistenceEnabled = persistenceEnabled;
		}

		public int getKeepAliveInterval() {
			return keepAliveInterval;
		}

		public void setKeepAliveInterval(int keepAliveInterval) {
			this.keepAliveInterval = keepAliveInterval;
		}

		public int getConnectionTimeout() {
			return connectionTimeout;
		}

		public void setConnectionTimeout(int connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public String getPersistenceDirectory() {
			return persistenceDirectory;
		}

		public void setPersistenceDirectory(String persistenceDirectory) {
			this.persistenceDirectory = persistenceDirectory;
		}

		public Login getLogin() {
			if (login == null) {
				login = new Login();
			}
			return login;
		}

		public void setLogin(Login login) {
			this.login = login;
		}

		public SSLOptions getSslOptions() {
			if (sslOptions == null) {
				sslOptions = new SSLOptions();
			}
			return sslOptions;
		}

		public void setSslOptions(SSLOptions sslOptions) {
			this.sslOptions = sslOptions;
		}

		public List<ServerURI> getServerURIs() {
			if (serverURIs == null) {
				serverURIs = new ArrayList<ServerURI>();
				for (String uri : Constants.HA_SERVER_URIS) {
					serverURIs.add(new ServerURI(uri));
				}
			}
			return serverURIs;
		}

		public void setServerURIs(List<ServerURI> serverURIs) {
			this.serverURIs = serverURIs;
		}

		public LWT getLwt() {
			if (lwt == null) {
				lwt = new LWT();
			}
			return lwt;
		}

		public void setLwt(LWT lwt) {
			this.lwt = lwt;
		}
	}

}
