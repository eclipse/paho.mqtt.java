package org.eclipse.paho.mqttv5.client;

import java.util.ArrayList;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

/**
 * This Object holds relevant properties for the client to use once the TCP
 * connection has either been lost, or if the server has gracefully
 * disconnected.
 *
 */
public class MqttDisconnectResponse {
	private int returnCode;
	private String reasonString;
	private ArrayList<UserProperty> userProperties;
	private String serverReference;
	private MqttException exception;

	public MqttDisconnectResponse(MqttException exception) {
		this.exception = exception;
	}

	public MqttDisconnectResponse(MqttException exception, int returnCode, String reasonString,
			ArrayList<UserProperty> userProperties, String serverReference) {
		this.exception = exception;
		this.returnCode = returnCode;
		this.reasonString = reasonString;
		this.userProperties = userProperties;
		this.serverReference = serverReference;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public String getReasonString() {
		return reasonString;
	}

	public ArrayList<UserProperty> getUserProperties() {
		return userProperties;
	}

	public String getServerReference() {
		return serverReference;
	}

	public MqttException getException() {
		return exception;
	}

	@Override
	public String toString() {
		return "MqttDisconnectResponse [returnCode=" + returnCode + ", reasonString=" + reasonString
				+ ", userProperties=" + userProperties + ", serverReference=" + serverReference + ", exception="
				+ exception + "]";
	}

}
