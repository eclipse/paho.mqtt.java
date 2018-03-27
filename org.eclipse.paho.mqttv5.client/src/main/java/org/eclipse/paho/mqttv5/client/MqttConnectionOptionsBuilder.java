package org.eclipse.paho.mqttv5.client;

import java.util.ArrayList;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

public class MqttConnectionOptionsBuilder {
	private MqttConnectionOptions mqttConnectionOptions;

	public MqttConnectionOptionsBuilder() {
		mqttConnectionOptions = new MqttConnectionOptions();
	}

	public MqttConnectionOptionsBuilder serverURI(String serverURI) {
		mqttConnectionOptions.setServerURIs(new String[] { serverURI });
		return this;
	}

	public MqttConnectionOptionsBuilder serverURIs(String[] serverURIs) {
		mqttConnectionOptions.setServerURIs(serverURIs);
		return this;
	}

	public MqttConnectionOptionsBuilder automaticReconnect(boolean enabled) {
		mqttConnectionOptions.setAutomaticReconnect(enabled);
		return this;
	}

	public MqttConnectionOptionsBuilder automaticReconnectDelay(int minimum, int maximum) {
		mqttConnectionOptions.setAutomaticReconnectDelay(minimum, maximum);
		return this;
	}

	public MqttConnectionOptionsBuilder keepAliveInterval(int keepAlive) {
		mqttConnectionOptions.setKeepAliveInterval(keepAlive);
		return this;
	}

	public MqttConnectionOptionsBuilder maxInFlight(int maxInflight) {
		mqttConnectionOptions.setMaxInflight(maxInflight);
		return this;
	}
	
	public MqttConnectionOptionsBuilder connectionTimeout(int connectionTimeout) {
		mqttConnectionOptions.setConnectionTimeout(connectionTimeout);
		return this;
	}
	
	public MqttConnectionOptionsBuilder cleanSession(boolean cleanSession) {
		mqttConnectionOptions.setCleanSession(cleanSession);
		return this;
	}
	
	public MqttConnectionOptionsBuilder username(String username) {
		mqttConnectionOptions.setUserName(username);
		return this;
	}
	
	public MqttConnectionOptionsBuilder password(byte[] password) {
		mqttConnectionOptions.setPassword(password);
		return this;
	}
	
	public MqttConnectionOptionsBuilder will(String topic, MqttMessage message) {
		mqttConnectionOptions.setWill(topic, message);
		return this;
	}
	
	public MqttConnectionOptionsBuilder sessionExpiryInterval(Long sessionExpiryInterval) {
		mqttConnectionOptions.setSessionExpiryInterval(sessionExpiryInterval);
		return this;
	}
	
	public MqttConnectionOptionsBuilder maximumPacketSize(Long maximumPacketSize) {
		mqttConnectionOptions.setMaximumPacketSize(maximumPacketSize);
		return this;
	}
	
	public MqttConnectionOptionsBuilder topicAliasMaximum(Integer topicAliasMaximum) {
		mqttConnectionOptions.setTopicAliasMaximum(topicAliasMaximum);
		return this;
	}
	
	public MqttConnectionOptionsBuilder requestReponseInfo(Boolean requestResponseInfo) {
		mqttConnectionOptions.setRequestResponseInfo(requestResponseInfo);
		return this;
	}
	
	public MqttConnectionOptionsBuilder requestProblemInfo(Boolean requestProblemInfo) {
		mqttConnectionOptions.setRequestProblemInfo(requestProblemInfo);
		return this;
	}
	
	public MqttConnectionOptionsBuilder userProperties(ArrayList<UserProperty> properties) {
		mqttConnectionOptions.setUserProperties(properties);
		return this;
	}
	
	public MqttConnectionOptionsBuilder authMethod(String authMethod) {
		mqttConnectionOptions.setAuthMethod(authMethod);
		return this;
	}
	
	public MqttConnectionOptionsBuilder authData(byte[] authData) {
		mqttConnectionOptions.setAuthData(authData);
		return this;
	}

	public MqttConnectionOptions build() {
		return mqttConnectionOptions;
	}

}
