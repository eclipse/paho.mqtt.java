package org.eclipse.paho.mqttv5.client;

import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.paho.mqttv5.client.internal.ClientComms;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;

public class MqttClient implements MqttClientInterface {

	private MqttClientPersistence persistence;
	private ScheduledExecutorService executorService;
	private MqttPingSender pingSender;

	private ClientComms comms;
	private Hashtable topics;
	
	private String clientId = null; // The Client ID, if null, an ID will be generated


	// Callbacks
	ConnectionRecoveredCallback connectionRecoveredCallback; // Callback that is called when the client completes a reconnect.
	ConnectionLostCallback connectionLostCallback; // Callback that is called when the connection is lost.

	public MqttClient() throws MqttException {
		this(null, null, null, null);
	}

	public MqttClient(String clientId, MqttClientPersistence persistence, MqttPingSender pingSender, ScheduledExecutorService executorService) 
			throws MqttException {
		final String methodName = "MqttClient";

		// Set Persistence
		this.persistence = persistence;
		if (this.persistence == null) {
			this.persistence = new MemoryPersistence();
		}

		this.executorService = executorService;
		if (this.executorService == null) {
			this.executorService = Executors.newScheduledThreadPool(10);
		}
		
		this.pingSender = pingSender;
		if(this.pingSender == null) {
			this.pingSender = new TimerPingSender(executorService);
		}
		
		
		this.clientId = clientId;
		if(this.clientId == null) {
			generateAndSetClientId();
		}
		this.persistence.open(clientId);
		this.comms = new ClientComms(this, this.persistence, this.pingSender, this.executorService);
		this.persistence.close();
		this.topics = new Hashtable();

	}
	
	public MqttToken connect(MqttActionListener actionListener, MqttConnectionOptions mqttConnectionOptions) {
		
		return null;
	}
	
	
	

	public void setReconnectCallback(ConnectionRecoveredCallback connectionRecoveredCallback) {
		this.connectionRecoveredCallback = connectionRecoveredCallback;
	}


	public String getServerURI() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Returns the Client ID
	 * 
	 * @return the Client ID
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Sets the Client ID.
	 * 
	 * The Server MUST allow clientIDs which are between 1 and 23 UTF-8 encoded
	 * bytes in length, and that contain only the characters:
	 * "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	 * 
	 * The Server MAY allow a Client ID that contains more than 23 encoded bytes,
	 * the server MAY also allow Client IDs that contain characters not contained in
	 * the list above.
	 * 
	 * A Server MAY allow a Client to supply a Client ID that has a length of zero
	 * bytes, however if it does so the Server MUST tread this as a special case and
	 * assign a unique Client ID to that client. [MQTT-2.1.2-6]. It MUST then
	 * process the CONNECT packet as if the Client had provided that unique Client
	 * ID, and MUST return the Assigned Client ID in the CONNACK packet.
	 * [MQTT-3.1.3-7]
	 * 
	 * @param clientId
	 *            the Client ID
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Generates and then sets a clientID. This helper function will automatically
	 * set it, so you don't need to set it yourself.
	 * 
	 * The Client ID will be built from a prefix and the current Nano Time.
	 * 
	 * @return the Client ID that was generated.
	 */
	public String generateAndSetClientId() {
		this.clientId = MqttConnectionOptions.generateClientId();
		return clientId;
	}

}
