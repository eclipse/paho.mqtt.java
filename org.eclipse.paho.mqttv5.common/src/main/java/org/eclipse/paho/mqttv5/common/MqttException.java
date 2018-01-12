package org.eclipse.paho.mqttv5.common;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class MqttException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Client encountered an exception. Use the {@link #getCause()} method to get
	 * the underlying reason.
	 */
	public static final short REASON_CODE_CLIENT_EXCEPTION = 0x00;

	// New MQTTv5 Packet Errors
	public static final int REASON_CODE_INVALID_IDENTIFIER = 50000; // Invalid Identifier in the IV fields
	public static final int REASON_CODE_INVALID_RETURN_CODE = 50001; // Invalid Return code
	public static final int REASON_CODE_MALFORMED_PACKET = 50002; // Packet was somehow malformed and did not comply to
																	// the MQTTv5 specification
	public static final int REASON_CODE_UNSUPPORTED_PROTOCOL_VERSION = 50003; // The CONNECT packet did not contain the
																				// correct protocol name or version
	
	/**
	 * The Server sent a publish message with an invalid topic alias.
	 */
	public static final int REASON_CODE_INVALID_TOPIC_ALAS = 50004;

	

	

	private int reasonCode;
	private Throwable cause;

	/**
	 * Constructs a new <code>MqttException</code> with the specified code as the
	 * underlying reason.
	 * 
	 * @param reasonCode
	 *            the reason code for the exception.
	 */
	public MqttException(int reasonCode) {
		super();
		this.reasonCode = reasonCode;
	}

	/**
	 * Constructs a new <code>MqttException</code> with the specified
	 * <code>Throwable</code> as the underlying reason.
	 * 
	 * @param cause
	 *            the underlying cause of the exception.
	 */
	public MqttException(Throwable cause) {
		super();
		this.reasonCode = REASON_CODE_CLIENT_EXCEPTION;
		this.cause = cause;
	}

	/**
	 * Constructs a new <code>MqttException</code> with the specified
	 * <code>Throwable</code> as the underlying reason.
	 * 
	 * @param reason
	 *            the reason code for the exception.
	 * @param cause
	 *            the underlying cause of the exception.
	 */
	public MqttException(int reason, Throwable cause) {
		super();
		this.reasonCode = reason;
		this.cause = cause;
	}

	/**
	 * Returns the reason code for this exception.
	 * 
	 * @return the code representing the reason for this exception.
	 */
	public int getReasonCode() {
		return reasonCode;
	}

	/**
	 * Returns the underlying cause of this exception, if available.
	 * 
	 * @return the Throwable that was the root cause of this exception, which may be
	 *         <code>null</code>.
	 */
	@Override
	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns the detail message for this exception.
	 * 
	 * @return the detail message, which may be <code>null</code>.
	 */
	@Override
	public String getMessage() {
		ResourceBundle bundle = ResourceBundle.getBundle("org.eclipse.paho.mqttv5.common.nls.messages");
		try {
			return bundle.getString(Integer.toString(reasonCode));
		} catch (MissingResourceException mre) {
			return "MqttException";
		}
	}

	/**
	 * Returns a <code>String</code> representation of this exception.
	 * 
	 * @return a <code>String</code> representation of this exception.
	 */
	@Override
	public String toString() {
		String result = getMessage() + " (" + reasonCode + ")";
		if (cause != null) {
			result = result + " - " + cause.toString();
		}
		return result;
	}

}
