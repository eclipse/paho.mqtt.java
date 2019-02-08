package org.eclipse.paho.mqttv5.client.vertx;

import java.util.ArrayList;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttDisconnect;
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

	/**
	 * Initialises a new MqttDisconnectResponse with an exception. This will only be
	 * thrown in the event of a non-clean disconnect, i.e. connection lost.
	 * 
	 * @param exception
	 *            The exception thrown containing details about the lost connection.
	 */
	public MqttDisconnectResponse(MqttException exception) {
		this.exception = exception;
	}

	/**
	 * Initialise a new MqttDisconnectResponse with parameters provided in an
	 * {@link MqttDisconnect} packet. This will be used when a clean disconnect has
	 * Occurred.
	 * 
	 * @param returnCode
	 *            The Disconnect Return Code
	 * @param reasonString
	 *            The Disconnect Reason String
	 * @param userProperties
	 *            The Disconnect User Properties
	 * @param serverReference
	 *            The Server Reference
	 */
	public MqttDisconnectResponse(int returnCode, String reasonString, ArrayList<UserProperty> userProperties,
			String serverReference) {
		this.returnCode = returnCode;
		this.reasonString = reasonString;
		this.userProperties = userProperties;
		this.serverReference = serverReference;
	}

	/**
	 * @return The Return code
	 */
	public int getReturnCode() {
		return returnCode;
	}

	/**
	 * @return The Reason String
	 */
	public String getReasonString() {
		return reasonString;
	}

	/**
	 * @return The User Properties
	 */
	public ArrayList<UserProperty> getUserProperties() {
		return userProperties;
	}

	/**
	 * @return The Server Reference, a new URI for a different server to connect to.
	 */
	public String getServerReference() {
		return serverReference;
	}

	/**
	 * @return The Exception thrown, most likely caused by a lost connection.
	 */
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
