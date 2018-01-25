package org.eclipse.paho.mqttv5.common;

/**
 * This exception is thrown by the implementor of the persistence
 * interface if there is a problem reading or writing persistent data.
 */
public class MqttPersistenceException extends MqttException {
	private static final long serialVersionUID = 300L;

	/** Persistence is already being used by another client. */
	public static final short REASON_CODE_PERSISTENCE_IN_USE	= 32200;
	
	/**
	 * Constructs a new <code>MqttPersistenceException</code>
	 */
	public MqttPersistenceException() {
		super(REASON_CODE_CLIENT_EXCEPTION);
	}
	
	/**
	 * Constructs a new <code>MqttPersistenceException</code> with the specified code
	 * as the underlying reason.
	 * @param reasonCode the reason code for the exception.
	 */
	public MqttPersistenceException(int reasonCode) {
		super(reasonCode);
	}
	/**
	 * Constructs a new <code>MqttPersistenceException</code> with the specified 
	 * <code>Throwable</code> as the underlying reason.
	 * @param cause the underlying cause of the exception.
	 */
	public MqttPersistenceException(Throwable cause) {
		super(cause);
	}
	/**
	 * Constructs a new <code>MqttPersistenceException</code> with the specified 
	 * <code>Throwable</code> as the underlying reason.
	 * @param reason the reason code for the exception.
	 * @param cause the underlying cause of the exception.
	 */
	public MqttPersistenceException(int reason, Throwable cause) {
		super(reason, cause);
	}
}