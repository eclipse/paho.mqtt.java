package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;

/**
 * MQTT v5 Properties Class. This Class contains all of the available MQTTv5
 * Properties accessible through getters and setters.
 * 
 * It also contains the logic to encode and decode the properties into the
 * appropriate byte arrays that can then be appended to the main MQTT packets.
 * 
 * This class will only encode valid properties for a packet, so although you
 * may set a value on a property, it will only be sent if it is valid for that
 * message type.
 *
 * When a property has not yet been set, or was not included in a packet being
 * decoded, it will remain as {@code null}.
 * 
 * @author James Sutton
 *
 */
public class MqttProperties {

	/** 1 - Payload format indicator (Byte) */
	public static final byte PAYLOAD_FORMAT_INDICATOR_IDENTIFIER = 0x01;

	/** 2 - Message Expiry Interval. (Four Byte Int). */
	public static final byte MESSAGE_EXPIRY_INTERVAL_IDENTIFIER = 0x02;

	/** 3 - Content Type (UTF-8). */
	public static final byte CONTENT_TYPE_IDENTIFIER = 0x03;

	/** 8 - Response Topic (UTF-8). */
	public static final byte RESPONSE_TOPIC_IDENTIFIER = 0x08;

	/** 9 - Correlation Data. (UTF-8). */
	public static final byte CORRELATION_DATA_IDENTIFIER = 0x09;

	/** 11 - Subscription Identifier (Variable Byte Int). */
	public static final byte SUBSCRIPTION_IDENTIFIER = 0x0B;

	/** 126 - Subscription Identifier Multi Flag (NOT ACTUAL PROPERTY). */
	public static final byte SUBSCRIPTION_IDENTIFIER_MULTI = 0x7E;

	/** 127 - Subscription Identifier Single Flag (NOT ACTUAL PROPERTY). */
	public static final byte SUBSCRIPTION_IDENTIFIER_SINGLE = 0x7F;

	/** 17 - Session Expiry Interval (Four Byte Int). */
	public static final byte SESSION_EXPIRY_INTERVAL_IDENTIFIER = 0x11;

	/** 18 - Assigned Client Identifier (UTF-8). */
	public static final byte ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER = 0x12;

	/** 19 - Server Keep Alive (Two Byte Int). */
	public static final byte SERVER_KEEP_ALIVE_IDENTIFIER = 0x13;

	/** 21 - Authentication Method (UTF-8). */
	public static final byte AUTH_METHOD_IDENTIFIER = 0x15;

	/** 22 - Authentication Data (Binary Data). */
	public static final byte AUTH_DATA_IDENTIFIER = 0x16;

	/** 23 - Request Problem Information (Byte). */
	public static final byte REQUEST_PROBLEM_INFO_IDENTIFIER = 0x17;

	/** 24 - Will Delay Interval (Four Byte Int). */
	public static final byte WILL_DELAY_INTERVAL_IDENTIFIER = 0x18;

	/** 25 - Request Response Information (Byte). */
	public static final byte REQUEST_RESPONSE_INFO_IDENTIFIER = 0x19;

	/** 26 - Response Information (UTF-8). */
	public static final byte RESPONSE_INFO_IDENTIFIER = 0x1A;

	/** 28 - Server Reference (UTF-8). */
	public static final byte SERVER_REFERENCE_IDENTIFIER = 0x1C;

	/** 31 - Reason String (UTF-8). */
	public static final byte REASON_STRING_IDENTIFIER = 0x1F;

	/** 33 - Receive Maximum (Two Byte Int). */
	public static final byte RECEIVE_MAXIMUM_IDENTIFIER = 0x21;

	/** 34 - Topic Alias Maximum (Two Byte Int). */
	public static final byte TOPIC_ALIAS_MAXIMUM_IDENTIFIER = 0x22;

	/** 35 - Topic Alias (Two Byte Int). */
	public static final byte TOPIC_ALIAS_IDENTIFIER = 0x23;

	/** 36 - Maximum QOS (Byte). */
	public static final byte MAXIMUM_QOS_IDENTIFIER = 0x24;

	/** 37 - Retain Available (Byte). */
	public static final byte RETAIN_AVAILABLE_IDENTIFIER = 0x25;

	/** 38 - User Defined Pair (UTF-8 key value). */
	public static final byte USER_DEFINED_PAIR_IDENTIFIER = 0x26;

	/** 39 - Maximum Packet Size (Four Byte Int). */
	public static final byte MAXIMUM_PACKET_SIZE_IDENTIFIER = 0x27;

	/** 40 - Wildcard Subscriptions available (Byte). */
	public static final byte WILDCARD_SUB_AVAILABLE_IDENTIFIER = 0x28;

	/** 41 - Subscription Identifier Available (Byte). */
	public static final byte SUBSCRIPTION_AVAILABLE_IDENTIFIER = 0x29;

	/** 42 - Shared Subscription Available (Byte). */
	public static final byte SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER = 0x2A;

	private List<Byte> validProperties;

	// Properties, sorted by Type
	// Byte
	private Boolean payloadFormat = false;
	private Boolean requestProblemInfo;
	private Boolean requestResponseInfo;
	private Integer maximumQoS;
	private Boolean retainAvailable = null;
	private Boolean wildcardSubscriptionsAvailable = null;
	private Boolean subscriptionIdentifiersAvailable = null;
	private Boolean sharedSubscriptionAvailable = null;

	// Two Byte Integer
	private Integer serverKeepAlive;
	private Integer receiveMaximum;
	private Integer topicAliasMaximum;
	private Integer topicAlias;

	// Four Byte Integer
	private Long messageExpiryInterval;
	private Long sessionExpiryInterval;
	private Long willDelayInterval;
	private Long maximumPacketSize;

	// UTF-8 encoded String
	private String contentType;
	private String responseTopic;
	private String assignedClientIdentifier;
	private String authenticationMethod;
	private String responseInfo;
	private String serverReference;
	private String reasonString;
	private List<UserProperty> userProperties = new ArrayList<>();

	// Binary Data
	private byte[] correlationData;
	private byte[] authenticationData;

	// Variable Byte Integer
	private List<Integer> publishSubscriptionIdentifiers = new ArrayList<>();
	private Integer subscribeSubscriptionIdentifier;

	/**
	 * Initialises this MqttProperties Object.
	 */
	public MqttProperties() {
	}

	/**
	 * Initialises this MqttProperties Object with a list of valid properties.
	 * 
	 * @param validProperties
	 *            the valid properties for the associated packet.
	 */
	public MqttProperties(Byte[] validProperties) {
		this.validProperties = Arrays.asList(validProperties);

	}

	/**
	 * Sets the list of Valid MQTT Properties that are allowed for the associated
	 * packet.
	 * 
	 * @param validProperties
	 *            The valid properties for this packet.
	 */
	public void setValidProperties(Byte[] validProperties) {
		this.validProperties = Arrays.asList(validProperties);

	}

	/**
	 * Encodes Non-Null Properties that are in the list of valid properties into a
	 * byte array.
	 * 
	 * @return a byte array containing encoded properties.
	 * @throws MqttException
	 *             if an exception occurs whilst encoding the properties.
	 */
	public byte[] encodeProperties() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Payload Format Indicator
			if (payloadFormat && validProperties.contains(PAYLOAD_FORMAT_INDICATOR_IDENTIFIER)) {
				outputStream.write(PAYLOAD_FORMAT_INDICATOR_IDENTIFIER);
				outputStream.writeByte(0x01);
			}

			// Message Expiry Interval
			if (messageExpiryInterval != null && validProperties.contains(MESSAGE_EXPIRY_INTERVAL_IDENTIFIER)) {
				outputStream.write(MESSAGE_EXPIRY_INTERVAL_IDENTIFIER);
				MqttDataTypes.writeUnsignedFourByteInt(messageExpiryInterval, outputStream);
			}

			// Content Type
			if (contentType != null && validProperties.contains(CONTENT_TYPE_IDENTIFIER)) {
				outputStream.write(CONTENT_TYPE_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, contentType);
			}

			// Response Topic
			if (responseTopic != null && validProperties.contains(RESPONSE_TOPIC_IDENTIFIER)) {
				outputStream.write(RESPONSE_TOPIC_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, responseTopic);
			}

			// Correlation Data
			if (correlationData != null && validProperties.contains(CORRELATION_DATA_IDENTIFIER)) {
				outputStream.write(CORRELATION_DATA_IDENTIFIER);
				outputStream.writeShort(correlationData.length);
				outputStream.write(correlationData);
			}

			// Subscription Identifier
			if (!publishSubscriptionIdentifiers.isEmpty() && validProperties.contains(SUBSCRIPTION_IDENTIFIER_MULTI)) {
				for (Integer subscriptionIdentifier : publishSubscriptionIdentifiers) {
					outputStream.write(SUBSCRIPTION_IDENTIFIER);
					outputStream.write(MqttDataTypes.encodeVariableByteInteger(subscriptionIdentifier));
				}
			}
			if (subscribeSubscriptionIdentifier != null && validProperties.contains(SUBSCRIPTION_IDENTIFIER_SINGLE)) {
				outputStream.write(SUBSCRIPTION_IDENTIFIER);
				outputStream.write(MqttDataTypes.encodeVariableByteInteger(subscribeSubscriptionIdentifier));
			}

			// Session Expiry Interval
			if (sessionExpiryInterval != null && validProperties.contains(SESSION_EXPIRY_INTERVAL_IDENTIFIER)) {
				outputStream.write(SESSION_EXPIRY_INTERVAL_IDENTIFIER);
				MqttDataTypes.writeUnsignedFourByteInt(sessionExpiryInterval, outputStream);

			}

			// Assigned Client Identifier
			if (assignedClientIdentifier != null && validProperties.contains(ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER)) {
				outputStream.write(ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, assignedClientIdentifier);
			}

			// Server Keep Alive
			if (serverKeepAlive != null && validProperties.contains(SERVER_KEEP_ALIVE_IDENTIFIER)) {
				outputStream.write(SERVER_KEEP_ALIVE_IDENTIFIER);
				outputStream.writeShort(serverKeepAlive);
			}

			// Auth Method
			if (authenticationMethod != null && validProperties.contains(AUTH_METHOD_IDENTIFIER)) {
				outputStream.write(AUTH_METHOD_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, authenticationMethod);
			}

			// Auth Data
			if (authenticationData != null && validProperties.contains(AUTH_DATA_IDENTIFIER)) {
				outputStream.write(AUTH_DATA_IDENTIFIER);
				outputStream.writeShort(authenticationData.length);
				outputStream.write(authenticationData);
			}

			// Request Problem Info
			if (requestProblemInfo != null && validProperties.contains(REQUEST_PROBLEM_INFO_IDENTIFIER)) {
				outputStream.write(REQUEST_PROBLEM_INFO_IDENTIFIER);
				outputStream.write(requestProblemInfo ? 1 : 0);
			}

			// Will Delay Interval
			if (willDelayInterval != null && validProperties.contains(WILL_DELAY_INTERVAL_IDENTIFIER)) {
				outputStream.write(WILL_DELAY_INTERVAL_IDENTIFIER);
				MqttDataTypes.writeUnsignedFourByteInt(willDelayInterval, outputStream);
				// outputStream.writeInt(willDelayInterval);
			}

			// Request Response Info
			if (requestResponseInfo != null && validProperties.contains(REQUEST_RESPONSE_INFO_IDENTIFIER)) {
				outputStream.write(REQUEST_RESPONSE_INFO_IDENTIFIER);
				outputStream.write(requestResponseInfo ? 1 : 0);
			}

			// Response Info
			if (responseInfo != null && validProperties.contains(RESPONSE_INFO_IDENTIFIER)) {
				outputStream.write(RESPONSE_INFO_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, responseInfo);
			}

			// Server Reference
			if (serverReference != null && validProperties.contains(SERVER_REFERENCE_IDENTIFIER)) {
				outputStream.write(SERVER_REFERENCE_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, serverReference);
			}

			// Reason String
			if (reasonString != null && validProperties.contains(REASON_STRING_IDENTIFIER)) {
				outputStream.write(REASON_STRING_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, reasonString);
			}

			// Receive Maximum
			if (receiveMaximum != null && validProperties.contains(RECEIVE_MAXIMUM_IDENTIFIER)) {
				outputStream.write(RECEIVE_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(receiveMaximum);
			}

			// Topic Alias Maximum
			if (topicAliasMaximum != null && validProperties.contains(TOPIC_ALIAS_MAXIMUM_IDENTIFIER)) {
				outputStream.write(TOPIC_ALIAS_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(topicAliasMaximum);
			}

			// Topic Alias
			if (topicAlias != null && validProperties.contains(TOPIC_ALIAS_IDENTIFIER)) {
				outputStream.write(TOPIC_ALIAS_IDENTIFIER);
				outputStream.writeShort(topicAlias);
			}

			// Maximum QoS
			if (maximumQoS != null && validProperties.contains(MAXIMUM_QOS_IDENTIFIER)) {
				outputStream.write(MAXIMUM_QOS_IDENTIFIER);
				outputStream.writeByte(maximumQoS);
			}

			// Retain Available
			if (retainAvailable != null && validProperties.contains(RETAIN_AVAILABLE_IDENTIFIER)) {
				outputStream.write(RETAIN_AVAILABLE_IDENTIFIER);
				outputStream.writeBoolean(retainAvailable);
			}

			// User Defined Properties
			if (userProperties != null && !userProperties.isEmpty() && validProperties.contains(USER_DEFINED_PAIR_IDENTIFIER)) {
				for (UserProperty property : userProperties) {
					// outputStream.write(USER_DEFINED_PAIR_IDENTIFIER);
					outputStream.writeByte(USER_DEFINED_PAIR_IDENTIFIER);
					MqttDataTypes.encodeUTF8(outputStream, property.getKey());
					MqttDataTypes.encodeUTF8(outputStream, property.getValue());
				}
			}

			// Maximum Packet Size
			if (maximumPacketSize != null && validProperties.contains(MAXIMUM_PACKET_SIZE_IDENTIFIER)) {
				outputStream.write(MAXIMUM_PACKET_SIZE_IDENTIFIER);
				MqttDataTypes.writeUnsignedFourByteInt(maximumPacketSize, outputStream);

			}

			// Wildcard Subscription Available flag
			if (wildcardSubscriptionsAvailable != null && validProperties.contains(WILDCARD_SUB_AVAILABLE_IDENTIFIER)) {
				outputStream.write(WILDCARD_SUB_AVAILABLE_IDENTIFIER);
				outputStream.writeBoolean(wildcardSubscriptionsAvailable);
			}

			// Subscription Identifiers Available flag
			if (subscriptionIdentifiersAvailable != null
					&& validProperties.contains(SUBSCRIPTION_AVAILABLE_IDENTIFIER)) {
				outputStream.write(SUBSCRIPTION_AVAILABLE_IDENTIFIER);
				outputStream.writeBoolean(subscriptionIdentifiersAvailable);
			}

			// Shared Subscription Available flag
			if (sharedSubscriptionAvailable != null
					&& validProperties.contains(SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER)) {
				outputStream.write(SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER);
				outputStream.writeBoolean(sharedSubscriptionAvailable);
			}

			int length = outputStream.size();
			outputStream.flush();
			ByteArrayOutputStream finalOutput = new ByteArrayOutputStream();
			finalOutput.write(MqttDataTypes.encodeVariableByteInteger(length));
			finalOutput.write(baos.toByteArray());

			return finalOutput.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	/**
	 * Decodes a Byte array of MQTT properties and sets them on this object.
	 * 
	 * @param dis
	 *            the {@link DataInputStream} containing the encoded Properties.
	 * @throws IOException
	 *             if an exception occurs whilst reading the Byte Array
	 * @throws MqttException
	 *             if an invalid MQTT Property Identifier is present.
	 */
	public void decodeProperties(DataInputStream dis) throws IOException, MqttException {

		// First get the length of the IV fields
		int length = MqttDataTypes.readVariableByteInteger(dis).getValue();
		if (length > 0) {
			byte[] identifierValueByteArray = new byte[length];
			dis.read(identifierValueByteArray, 0, length);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			ArrayList<Byte> decodedProperties = new ArrayList<Byte>();
			DataInputStream inputStream = new DataInputStream(bais);
			while (inputStream.available() > 0) {
				// Get the first Byte
				byte identifier = inputStream.readByte();
				if (validProperties.contains(identifier)) {
					
					// Verify that certain properties are not included more than once
					if(!decodedProperties.contains(identifier)) {
						decodedProperties.add(identifier);
					} else if(identifier!= SUBSCRIPTION_IDENTIFIER && identifier != USER_DEFINED_PAIR_IDENTIFIER) {
						// This property can only be included once
						throw new MqttException(MqttException.REASON_CODE_DUPLICATE_PROPERTY);
					}

					if (identifier == PAYLOAD_FORMAT_INDICATOR_IDENTIFIER) {
						payloadFormat = (boolean) inputStream.readBoolean();
					} else if (identifier == MESSAGE_EXPIRY_INTERVAL_IDENTIFIER) {
						messageExpiryInterval = MqttDataTypes.readUnsignedFourByteInt(inputStream);
					} else if (identifier == CONTENT_TYPE_IDENTIFIER) {
						contentType = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == RESPONSE_TOPIC_IDENTIFIER) {
						responseTopic = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == CORRELATION_DATA_IDENTIFIER) {
						int correlationDataLength = (int) inputStream.readShort();
						correlationData = new byte[correlationDataLength];
						inputStream.read(correlationData, 0, correlationDataLength);
					} else if (identifier == SUBSCRIPTION_IDENTIFIER) {
						int subscriptionIdentifier = MqttDataTypes.readVariableByteInteger(inputStream).getValue();
						publishSubscriptionIdentifiers.add(subscriptionIdentifier);
						// Bit of a hack, where we potentially write this many times, users should make
						// sure they read the JavaDoc.
						subscribeSubscriptionIdentifier = subscriptionIdentifier;
					} else if (identifier == SESSION_EXPIRY_INTERVAL_IDENTIFIER) {
						sessionExpiryInterval = MqttDataTypes.readUnsignedFourByteInt(inputStream);
					} else if (identifier == ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER) {
						assignedClientIdentifier = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == SERVER_KEEP_ALIVE_IDENTIFIER) {
						serverKeepAlive = MqttDataTypes.readUnsignedTwoByteInt(inputStream);
					} else if (identifier == AUTH_METHOD_IDENTIFIER) {
						authenticationMethod = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == AUTH_DATA_IDENTIFIER) {
						int authDataLength = inputStream.readShort();
						authenticationData = new byte[authDataLength];
						inputStream.read(this.authenticationData, 0, authDataLength);
					} else if (identifier == REQUEST_PROBLEM_INFO_IDENTIFIER) {
						requestProblemInfo = inputStream.read() != 0;
					} else if (identifier == WILL_DELAY_INTERVAL_IDENTIFIER) {
						willDelayInterval = MqttDataTypes.readUnsignedFourByteInt(inputStream);
					} else if (identifier == REQUEST_RESPONSE_INFO_IDENTIFIER) {
						requestResponseInfo = inputStream.read() != 0;
					} else if (identifier == RESPONSE_INFO_IDENTIFIER) {
						responseInfo = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == SERVER_REFERENCE_IDENTIFIER) {
						serverReference = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == REASON_STRING_IDENTIFIER) {
						reasonString = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == RECEIVE_MAXIMUM_IDENTIFIER) {
						receiveMaximum = (int) inputStream.readShort();
					} else if (identifier == TOPIC_ALIAS_MAXIMUM_IDENTIFIER) {
						topicAliasMaximum = (int) inputStream.readShort();
					} else if (identifier == TOPIC_ALIAS_IDENTIFIER) {
						topicAlias = (int) inputStream.readShort();
					} else if (identifier == MAXIMUM_QOS_IDENTIFIER) {
						maximumQoS = inputStream.read();
					} else if (identifier == RETAIN_AVAILABLE_IDENTIFIER) {
						retainAvailable = inputStream.readBoolean();
					} else if (identifier == USER_DEFINED_PAIR_IDENTIFIER) {
						String key = MqttDataTypes.decodeUTF8(inputStream);
						String value = MqttDataTypes.decodeUTF8(inputStream);
						userProperties.add(new UserProperty(key, value));
					} else if (identifier == MAXIMUM_PACKET_SIZE_IDENTIFIER) {
						maximumPacketSize = MqttDataTypes.readUnsignedFourByteInt(inputStream);
					} else if (identifier == WILDCARD_SUB_AVAILABLE_IDENTIFIER) {
						wildcardSubscriptionsAvailable = inputStream.readBoolean();
					} else if (identifier == SUBSCRIPTION_AVAILABLE_IDENTIFIER) {
						subscriptionIdentifiersAvailable = inputStream.readBoolean();
					} else if (identifier == SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER) {
						sharedSubscriptionAvailable = inputStream.readBoolean();
					} else {

						// Unidentified Identifier
						inputStream.close();
						throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
					}
				} else {
					// Unidentified Identifier
					inputStream.close();
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}

			}
		}

	}

	/**
	 * Get a list of valid Properties for the associated MQTT Packet.
	 * 
	 * @return a {@link List} of valid properties.
	 */
	public List<Byte> getValidProperties() {
		return validProperties;
	}

	/**
	 * Request Response Information.
	 * 
	 * <p>
	 * The Client uses this value to request the Server to return Response
	 * Information in the CONNACK. A value of false indicates that the Sever MUST
	 * NOT return Response Information. If the value is true, the server MAY return
	 * Response Information in the CONNACK packet. If absent, the default value is
	 * false. The Server can choose not to include Response Information in the
	 * CONNACK, even if the Client requested it.
	 * </p>
	 * 
	 * @return the Request Response Information flag. May be null.
	 */
	public Boolean requestResponseInfo() {
		return requestResponseInfo;
	}

	/**
	 * Request Response Information. See
	 * {@link MqttProperties#requestResponseInfo()}
	 * 
	 * @param requestResponseInfo
	 *            - whether to request response information.
	 */
	public void setRequestResponseInfo(Boolean requestResponseInfo) {
		this.requestResponseInfo = requestResponseInfo;
	}

	/**
	 * Request Problem Information.
	 * 
	 * <p>
	 * The Client uses this value to indicate whether the Reason String or User
	 * Properties are sent in the case of failures.
	 * </p>
	 * <p>
	 * If the value of Request Problem Information is false, the Server MAY return a
	 * Reason String or User Properties on a CONNACK or DISCONNECT packet, but MUST
	 * NOT send a Reason String or User Properties on any packet other than PUBLISH,
	 * CONNACK, or DISCONNECT. If the value is false and the Client receives a
	 * Reason String or User Properties in a packet other than PUBLISH, CONNACK, or
	 * DISCONNECT, it uses a Disconnect Packet with a Reason Code 0x82 (Protocol
	 * Error).If absent, the default value is true. If this value is true, the
	 * Server MAY return a Reason String or User Properties on any Packet where it
	 * is allowed.
	 * </p>
	 * 
	 * @return the Request Problem Information flag. May be null.
	 */
	public Boolean requestProblemInfo() {
		return requestProblemInfo;
	}

	/**
	 * Request Problem Information. See {@link MqttProperties#requestProblemInfo()}
	 * 
	 * @param requestProblemInfo
	 *            - whether to request problem information.
	 */
	public void setRequestProblemInfo(Boolean requestProblemInfo) {
		this.requestProblemInfo = requestProblemInfo;
	}

	/**
	 * Will Delay Interval.
	 * 
	 * <p>
	 * The Server delays publishing the Client's Will Message until the Will Delay
	 * Interval has passed or the Session ends, whichever happens first. If a new
	 * Network Connection to this Session is made before the Will Delay Interval has
	 * passed, the Server MUST NOT send the Will Message. If this value is absent,
	 * the default value is 0 and there is no delay before the Will Message is
	 * published.
	 * </p>
	 * 
	 * @return The Will Delay Interval in seconds. May be null.
	 */
	public Long getWillDelayInterval() {
		return willDelayInterval;
	}

	/**
	 * Will Delay Interval. See {@link MqttProperties#getWillDelayInterval()}
	 * 
	 * @param willDelayInterval
	 *            - The Will Delay Interval in seconds.
	 */
	public void setWillDelayInterval(Long willDelayInterval) {
		MqttDataTypes.validateFourByteInt(willDelayInterval);
		this.willDelayInterval = willDelayInterval;
	}

	/**
	 * The Receive Maximum.
	 * 
	 * <p>
	 * The Receive Maximum is used be the server to limit the number of QoS 1 and
	 * QoS 2 publications that it is willing to process concurrently for the Client.
	 * It does not provide a mechanism to limit the QoS 0 messages that the client
	 * might try to send.
	 * </p>
	 * <p>
	 * Valid values for this property range between 0 and 65,535. If this property
	 * is absent, the value defaults to 65,535.
	 * </p>
	 * 
	 * @return the Receive maximum. May be null if it has not been set.
	 */
	public Integer getReceiveMaximum() {
		return receiveMaximum;
	}

	/**
	 * The Receive Maximum. See {@link MqttProperties#getReceiveMaximum()}
	 * 
	 * @param receiveMaximum
	 *            - The Receive Maximum. May be null.
	 */
	public void setReceiveMaximum(Integer receiveMaximum) {
		MqttDataTypes.validateTwoByteInt(receiveMaximum);
		this.receiveMaximum = receiveMaximum;
	}

	/**
	 * Maximum QoS
	 * <p>
	 * If a Server does not support QoS 1 or QoS 2 PUBLISH packets it MUST send a
	 * Maximum Qos in the CONNACK packet specifying the highest QoS it supports. A
	 * Server that does not support QoS 1 or QoS 2 PUBLISH packets MUST still accept
	 * SUBSCRIBE packets containing a requested QoS of 0, 1 or 2.
	 * </p>
	 * 
	 * <p>
	 * If a Server receives a CONNECT packet containing a Will QoS that exceeds its
	 * capabilities, it MUST reject the connection. It SHOULD use a CONNACK packet
	 * with a Reason Code 0x9B (QoS not supported) and MUST close the network
	 * connection.
	 * </p>
	 * 
	 * <p>
	 * If the Maximum QoS is absent, the Client uses a Maximum QoS of 2.
	 * </p>
	 * 
	 * @return the maximum QoS. May be null.
	 */
	public Integer getMaximumQoS() {
		return maximumQoS;
	}

	/**
	 * Maximum QoS. See {@link MqttProperties#getMaximumQoS()}
	 * 
	 * @param maximumQoS
	 *            The Maximum QoS
	 */
	public void setMaximumQoS(Integer maximumQoS) {
		this.maximumQoS = maximumQoS;
	}

	/**
	 * Maximum Packet Size.
	 * 
	 * <p>
	 * The packet size is the total number of bytes in an MQTT Control Packet. The
	 * Server uses the Maximum Packet Size to inform the Client that it will not
	 * process packets whose size exceeds this limit. If this value is absent, there
	 * is no limit on the packet size imposed beyond the limitations in the protocol
	 * as a result of the remaining length encoding and the protocol header sizes.
	 * </p>
	 * 
	 * @return the Maximum Packet Size. May be null.
	 */
	public Long getMaximumPacketSize() {
		return maximumPacketSize;
	}

	/**
	 * Maximum Packet Size. See {@link MqttProperties#getMaximumPacketSize()}
	 * 
	 * @param maximumPacketSize
	 *            the Maximum Packet Size in bytes. May be null.
	 */
	public void setMaximumPacketSize(Long maximumPacketSize) {
		MqttDataTypes.validateFourByteInt(maximumPacketSize);
		this.maximumPacketSize = maximumPacketSize;
	}

	/**
	 * Retain Available.
	 * 
	 * <p>
	 * If present, this property declares whether ther Server supports retained
	 * messages. If not present, then retained messages are supported.
	 * </p>
	 * 
	 * <p>
	 * If a Server receives a CONNECT packet containing a Will Message with the Will
	 * Retain set to true, and it does not support retained messages, the Server
	 * MUST reject the connection request. It SHOULD send a CONNACK with the Reason
	 * Code 0x9A (Retain not supported) and then it MUST close the network
	 * connection. A client receiving Retain Available set to false from the Server
	 * MUST NOT send a PUBLISH packet with the Retain flag set to true. If the
	 * Server receives such a packet, this is a Protocol Error. The Server SHOULD
	 * send a DISCONNECT with the Reason code of 0x9A (Retain not supported).
	 * </p>
	 * 
	 * @return Retain Available Flag. May be Null.
	 */
	public Boolean isRetainAvailable() {
		if(retainAvailable == null || retainAvailable == true) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Retain Available. See {@link MqttProperties#isRetainAvailable()}
	 * 
	 * @param retainAvailable
	 *            - Whether the server supports retained messages. May be null.
	 */
	public void setRetainAvailable(boolean retainAvailable) {
		this.retainAvailable = retainAvailable;
	}

	/**
	 * Assigned Client Identifier.
	 * 
	 * <p>
	 * The Client Identifier which was assigned by the Server because a zero length
	 * Client Identifier was found in the CONNECT packet.
	 * </p>
	 * 
	 * <p>
	 * If the Client connects using a zero length Client Identifier, the Server MUST
	 * respond with a CONNACK containing an Assigned Client Identifier. The Assigned
	 * Client Identifier MUSE be a new Client Identifier not used by any other
	 * Session currently in the Server.
	 * </p>
	 * 
	 * @return The Assigned Client Identifier. May be null.
	 */
	public String getAssignedClientIdentifier() {
		return assignedClientIdentifier;
	}

	/**
	 * Assigned Client Identifier. See
	 * {@link MqttProperties#getAssignedClientIdentifier()}
	 * 
	 * @param assignedClientIdentifier
	 *            The Assigned Client Identifier.
	 */
	public void setAssignedClientIdentifier(String assignedClientIdentifier) {
		this.assignedClientIdentifier = assignedClientIdentifier;
	}

	/**
	 * Topic Alias Maximum.
	 * 
	 * <p>
	 * This value indicates the highest value that the Server will accept as a Topic
	 * Alias sent by the Client. The Server uses this value to limit the number of
	 * Topic Aliases that it is willing to hold on this Connection. The Client MUST
	 * NOT send a Topic Alias in a PUBLISH packet to the Server greater than this
	 * value. A value of 0 indicates that the Server does not accept any Topic
	 * Aliases on this connection. If Topic Alias Maximum is absent or 0, the Client
	 * MUST NOT send any Topic Aliases on to the Server.
	 * </p>
	 * 
	 * @return The Topic Alias Maximum. May be null.
	 */
	public Integer getTopicAliasMaximum() {
		return topicAliasMaximum;
	}

	/**
	 * Topic Alias Maximum. See {@link MqttProperties#getTopicAliasMaximum()}
	 * 
	 * @param topicAliasMaximum
	 *            The Topic Alias Maximum.
	 */
	public void setTopicAliasMaximum(Integer topicAliasMaximum) {
		MqttDataTypes.validateTwoByteInt(topicAliasMaximum);
		this.topicAliasMaximum = topicAliasMaximum;
	}

	/**
	 * Topic Alias.
	 * 
	 * <p>
	 * A Topic Alias is an integer value that is used to identify the Topic instead
	 * of using the Topic Name. This reduces the size of the PUBLISH packet, and is
	 * useful when the Topic Names are long and the same Topic Names are used
	 * repetitively within a network connection.
	 * </p>
	 * 
	 * <p>
	 * If enabled, this client will automatically assign Topic Aliases to outgoing
	 * messages.
	 * </p>
	 * 
	 * @return The Topic Alias.
	 */
	public Integer getTopicAlias() {
		return topicAlias;
	}

	/**
	 * Topic Alias. See {@link MqttProperties#getTopicAlias()}
	 * 
	 * @param topicAlias
	 *            The Topic Alias to set.
	 */
	public void setTopicAlias(Integer topicAlias) {
		MqttDataTypes.validateTwoByteInt(topicAlias);
		this.topicAlias = topicAlias;
	}

	/**
	 * Server Keep Alive.
	 * 
	 * <p>
	 * The Keep Alive time Assigned by the Server. If the Server sends a Server Keep
	 * Alive on the CONNACK packet, the Client MUST use this value instead of the
	 * Keep Alive value the Client send on CONNECT. The Paho Client will
	 * automatically adjust the Keep Alive Value if this property is present in the
	 * CONNACK packet.
	 * </p>
	 * 
	 * @return The Server Keep Alive Value
	 */
	public Integer getServerKeepAlive() {
		return serverKeepAlive;
	}

	/**
	 * Server Keep Alive. See {@link MqttProperties#getServerKeepAlive()}
	 * 
	 * @param serverKeepAlive
	 *            The Server Keep Alive Value.
	 */
	public void setServerKeepAlive(Integer serverKeepAlive) {
		MqttDataTypes.validateTwoByteInt(serverKeepAlive);
		this.serverKeepAlive = serverKeepAlive;

	}

	/**
	 * Response Information.
	 * 
	 * <p>
	 * A String that is used as the basis for creating a Response Topic . The way in
	 * which the Client creates a Response Topic from the Response Information is
	 * not defined by the MQTT v5 specification.
	 * </p>
	 * 
	 * @return The Response Information
	 */
	public String getResponseInfo() {
		return responseInfo;
	}

	/**
	 * Response Information. See {@link MqttProperties#getResponseInfo()}
	 * 
	 * @param responseInfo
	 *            The Response Information.
	 */
	public void setResponseInfo(String responseInfo) {
		this.responseInfo = responseInfo;
	}

	/**
	 * Server Reference.
	 * 
	 * <p>
	 * A String that can be used by the Client to identifier another Server to use.
	 * </p>
	 * 
	 * @return The Server Reference.
	 */
	public String getServerReference() {
		return serverReference;
	}

	/**
	 * Server Reference. See {@link MqttProperties#getServerReference()}
	 * 
	 * @param serverReference
	 *            The Server Reference.
	 */
	public void setServerReference(String serverReference) {
		this.serverReference = serverReference;
	}

	/**
	 * Wildcard Subscription Available.
	 * 
	 * <p>
	 * If present, this flag declares whether the Server supports Wildcard
	 * Subscriptions. False means that Wildcard Subscriptions are not supported,
	 * True means that they are supported. If not present, then Wildcard
	 * Subscriptions are supported.
	 * </p>
	 * 
	 * @return A boolean defining whether Wildcard Subscriptions are supported.
	 */
	public boolean isWildcardSubscriptionsAvailable() {
		if(wildcardSubscriptionsAvailable == null || wildcardSubscriptionsAvailable == true) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Wildcard Subscription Available. See
	 * {@link MqttProperties#isWildcardSubscriptionsAvailable()}
	 * 
	 * @param wildcardSubscriptionsAvailable
	 *            A boolean defining whether Wildcard Subscriptions are supported.
	 */
	public void setWildcardSubscriptionsAvailable(boolean wildcardSubscriptionsAvailable) {
		this.wildcardSubscriptionsAvailable = wildcardSubscriptionsAvailable;
	}

	/**
	 * Subscription Identifiers Available.
	 * 
	 * <p>
	 * If present, this flag declares whether the Server supports Subscription
	 * Identifiers. False means that Subscription Identifiers are not supported,
	 * True means that they are supported. If not present, then Subscription
	 * Identifiers are supported. The Paho Client will automatically attempt to use
	 * Subscription Identifiers if they are available.
	 * </p>
	 * 
	 * @return A boolean defining whether Subscription Identifiers are supported.
	 */
	public boolean isSubscriptionIdentifiersAvailable() {
		if(subscriptionIdentifiersAvailable == null || subscriptionIdentifiersAvailable == true) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Subscription Identifiers Available. See
	 * {@link MqttProperties#isSubscriptionIdentifiersAvailable()}
	 * 
	 * @param subscriptionIdentifiersAvailable
	 *            A boolean defining whether Subscription Identifiers are supported.
	 */
	public void setSubscriptionIdentifiersAvailable(boolean subscriptionIdentifiersAvailable) {
		this.subscriptionIdentifiersAvailable = subscriptionIdentifiersAvailable;
	}

	/**
	 * Shared Subscription Available.
	 * 
	 * <p>
	 * If present, this flag declares whether the Server supports Shared
	 * Subscriptions. False means that Shared Subscriptions are not supported, True
	 * means that they are supported. If not present, then Shared subscriptions are
	 * supported.
	 * </p>
	 * 
	 * @return A boolean defining whether Shared Subscriptions are supported.
	 */
	public boolean isSharedSubscriptionAvailable() {
		if(sharedSubscriptionAvailable == null || sharedSubscriptionAvailable == true) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Shared Subscription Available. See
	 * {@link MqttProperties#isSharedSubscriptionAvailable()}
	 * 
	 * @param sharedSubscriptionAvailable
	 *            A boolean defining whether Shared Subscriptions are supported.
	 */
	public void setSharedSubscriptionAvailable(boolean sharedSubscriptionAvailable) {
		this.sharedSubscriptionAvailable = sharedSubscriptionAvailable;
	}

	/**
	 * Session Expiry Interval.
	 * 
	 * <p>
	 * The Session Expiry Interval in seconds. If the Session Expiry interval is
	 * absent in the CONNECT packet, the default value of 0 is used, however the
	 * Server MAY set this value in the CONNACK packet if it is not present in the
	 * CONNECT.
	 * </p>
	 * 
	 * @return The Session Expiry Interval in seconds.
	 */
	public Long getSessionExpiryInterval() {
		return sessionExpiryInterval;
	}

	/**
	 * Session Expiry Interval. See
	 * {@link MqttProperties#getSessionExpiryInterval()}
	 * 
	 * @param sessionExpiryInterval
	 *            The Session Expiry Interval in seconds.
	 */
	public void setSessionExpiryInterval(Long sessionExpiryInterval) {
		MqttDataTypes.validateFourByteInt(sessionExpiryInterval);
		this.sessionExpiryInterval = sessionExpiryInterval;
	}

	/**
	 * Authentication Method.
	 * 
	 * <p>
	 * A String containing the name of the authentication method.
	 * <p>
	 * 
	 * @return The Authentication Method
	 */
	public String getAuthenticationMethod() {
		return authenticationMethod;
	}

	/**
	 * Authentication Method. See {@link MqttProperties#getAuthenticationMethod()}
	 * 
	 * @param authenticationMethod
	 *            The Authentication Method
	 */
	public void setAuthenticationMethod(String authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

	/**
	 * Authentication Data.
	 * 
	 * <p>
	 * Binary Data containing the Authentication data defined by the
	 * {@link MqttProperties#getAuthenticationMethod()}
	 * </p>
	 * 
	 * @return The Authentication Data.
	 */
	public byte[] getAuthenticationData() {
		return authenticationData;
	}

	/**
	 * Authentication Data. See {@link MqttProperties#getAuthenticationData()}
	 * 
	 * @param authenticationData
	 *            The Authentication Data.
	 */
	public void setAuthenticationData(byte[] authenticationData) {
		this.authenticationData = authenticationData;
	}

	/**
	 * Reason String.
	 * 
	 * <p>
	 * A String representing the reason associated with this response. This Reason
	 * String is a human readable string designed for diagnostics and SHOULD NOT be
	 * parsed by the client.
	 * </p>
	 * 
	 * @return The Reason String
	 */
	public String getReasonString() {
		return reasonString;
	}

	/**
	 * Reason String. See {@link MqttProperties#getReasonString()}
	 * 
	 * @param reasonString
	 *            The Reason String
	 */
	public void setReasonString(String reasonString) {
		this.reasonString = reasonString;
	}

	/**
	 * User Properties.
	 * 
	 * <p>
	 * A {@link List} of {@link UserProperty} which are String Key Value Pairs.
	 * These properties can be used to provide additional information to the Server
	 * or Client including diagnostic information.
	 * </p>
	 * 
	 * @return a {@link List} of {@link UserProperty} Objects.
	 */
	public List<UserProperty> getUserProperties() {
		return userProperties;
	}

	/**
	 * User Properties. See {@link MqttProperties#getUserProperties()}
	 * 
	 * @param userProperties
	 *            a {@link List} of {@link UserProperty} Objects.
	 */
	public void setUserProperties(List<UserProperty> userProperties) {
		this.userProperties = userProperties;
	}

	/**
	 * Payload Format.
	 * 
	 * <p>
	 * A flag defining the payload format:
	 * </p>
	 * <ul>
	 * <li>False: Unspecified bytes, which is equivalent to not sending the payload
	 * format.</li>
	 * <li>True: A UTF-8 Encoded Character Data.</li>
	 * </ul>
	 * 
	 * 
	 * @return The Payload Format flag.
	 */
	public boolean getPayloadFormat() {
		return payloadFormat;
	}

	/**
	 * Payload Format. See {@link MqttProperties#getPayloadFormat()}
	 * 
	 * @param payloadFormat
	 */
	public void setPayloadFormat(boolean payloadFormat) {
		this.payloadFormat = payloadFormat;
	}

	/**
	 * Message Expiry Interval.
	 *
	 * <p>
	 * If present, this value is the lifetime of the Application Message in seconds.
	 * If the Message Expiry Interval has passed and the Server has not managed to
	 * start onward delivery to a matching subscriber, then it MUST delete the copy
	 * of the message for that subscriber.
	 * </p>
	 * 
	 * @return The Message Expiry Interval in seconds.
	 */
	public Long getMessageExpiryInterval() {
		return messageExpiryInterval;
	}

	/**
	 * Message Expiry Interval. See
	 * {@link MqttProperties#getMessageExpiryInterval()}
	 * 
	 * @param messageExpiryInterval
	 *            The Message Expiry Interval in seconds.
	 */
	public void setMessageExpiryInterval(Long messageExpiryInterval) {
		MqttDataTypes.validateFourByteInt(messageExpiryInterval);
		this.messageExpiryInterval = messageExpiryInterval;
	}

	/**
	 * Content Type.
	 * 
	 * <p>
	 * A UTF-8 Encoded String describing the content of the Application Message.
	 * </p>
	 * 
	 * @return The Content Type.
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Content Type. See {@link MqttProperties#getContentType()}
	 * 
	 * @param contentType
	 *            The Content Type.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Response Topic.
	 * 
	 * <p>
	 * A UTF-8 Encoded String used as the Topic Name for a response message.
	 * </p>
	 * 
	 * @return The Response Topic
	 */
	public String getResponseTopic() {
		return responseTopic;
	}

	/**
	 * Response Topic. See {@link MqttProperties#getResponseTopic()}
	 * 
	 * @param responseTopic
	 *            The Response Topic.
	 */
	public void setResponseTopic(String responseTopic) {
		if(responseTopic != null) {
			MqttTopicValidator.validate(responseTopic, false, true);
		}
		this.responseTopic = responseTopic;
	}

	/**
	 * Correlation Data.
	 * 
	 * <p>
	 * Binary data used by the Sender of the Request Message to identify which
	 * request the Response Message is for when it is received.
	 * </p>
	 * 
	 * @return The Correlation Data.
	 */
	public byte[] getCorrelationData() {
		return correlationData;
	}

	/**
	 * Correlation Data. See {@link MqttProperties#getCorrelationData()}
	 * 
	 * @param correlationData
	 *            The Correlation Data.
	 */
	public void setCorrelationData(byte[] correlationData) {
		this.correlationData = correlationData;
	}

	/**
	 * Subscription Identifiers. (Publish Only)
	 * 
	 * <p>
	 * The Subscription Identifiers are associated with any subscription created or
	 * modified as the result of a SUBSCRIBE packet. If a subscription was made with
	 * a Subscription Identifier, then any incoming messages that match that
	 * subscription will contain the associated subscription identifier, if the
	 * incoming message matches multiple subscriptions made by the same client, then
	 * it will contain a list of all associated subscription identifiers. This
	 * property is ONLY for PUBLISH packets. For a Subscription Identifier sent in a
	 * SUBSCRIBE packet, see {@link MqttProperties#getSubscriptionIdentifier()}
	 * </p>
	 * 
	 * @return A {@link List} of Subscription Identifiers.
	 */
	public List<Integer> getSubscriptionIdentifiers() {
		return publishSubscriptionIdentifiers;
	}

	/**
	 * Subscription Identifiers. (Publish Only) See
	 * {@link MqttProperties#getSubscriptionIdentifiers()}
	 * 
	 * @param subscriptionIdentifiers
	 *            A {@link List} of Subscription Identifiers.
	 */
	public void setSubscriptionIdentifiers(List<Integer> subscriptionIdentifiers) {
		for (Integer subId : subscriptionIdentifiers) {
			MqttDataTypes.validateVariableByteInt(subId);
		}
		this.publishSubscriptionIdentifiers = subscriptionIdentifiers;
	}

	/**
	 * Subscription Identifier. (Subscribe Only)
	 * 
	 * <p>
	 * The Subscription identifier field can be set on a SUBSCRIBE packet and will
	 * be returned with any incoming PUBLISH packets that match the associated
	 * subscription. This property is ONLY for SUBSCRIBE packets. For Subscription
	 * Identifier(s) sent in a PUBLISH packet, see
	 * {@link MqttProperties#getSubscriptionIdentifiers()}
	 * </p>
	 * 
	 * @return The Subscription Identifier.
	 */
	public Integer getSubscriptionIdentifier() {
		return subscribeSubscriptionIdentifier;
	}

	/**
	 * Subscription Identifier. (Subscribe Only) See
	 * {@link MqttProperties#getSubscriptionIdentifier()}
	 * 
	 * @param subscriptionIdentifier
	 *            The Subscription Identifier.
	 */
	public void setSubscriptionIdentifier(Integer subscriptionIdentifier) {
		MqttDataTypes.validateVariableByteInt(subscriptionIdentifier);
		this.subscribeSubscriptionIdentifier = subscriptionIdentifier;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MqttProperties [validProperties=").append(validProperties);
		if (requestResponseInfo != null) {
			sb.append(", requestResponseInfo=").append(requestResponseInfo);
		}
		if (requestProblemInfo != null) {
			sb.append(", requestProblemInfo=").append(requestProblemInfo);
		}
		if (willDelayInterval != null) {
			sb.append(", willDelayInterval=").append(willDelayInterval);
		}
		if (receiveMaximum != null) {
			sb.append(", receiveMaximum=").append(receiveMaximum);
		}
		if (maximumQoS != null) {
			sb.append(", maximumQoS=").append(maximumQoS);
		}
		if (maximumPacketSize != null) {
			sb.append(", maximumPacketSize=").append(maximumPacketSize);
		}
		if (retainAvailable != null) {
			sb.append(", retainAvailable=").append(retainAvailable);
		}
		if (assignedClientIdentifier != null) {
			sb.append(", assignedClientIdentifier=").append(assignedClientIdentifier);
		}
		if (topicAliasMaximum != null) {
			sb.append(", topicAliasMaximum=").append(topicAliasMaximum);
		}
		if (topicAlias != null) {
			sb.append(", topicAlias=").append(topicAlias);
		}
		if (serverKeepAlive != null) {
			sb.append(", serverKeepAlive=").append(serverKeepAlive);
		}
		if (responseInfo != null) {
			sb.append(", responseInfo=").append(responseInfo);
		}
		if (serverReference != null) {
			sb.append(", serverReference=").append(serverReference);
		}
		if (wildcardSubscriptionsAvailable != null) {
			sb.append(", wildcardSubscriptionsAvailable=").append(wildcardSubscriptionsAvailable);
		}
		if (subscriptionIdentifiersAvailable != null) {
			sb.append(", subscriptionIdentifiersAvailable=").append(subscriptionIdentifiersAvailable);
		}
		if (sharedSubscriptionAvailable != null) {
			sb.append(", sharedSubscriptionAvailable=").append(sharedSubscriptionAvailable);
		}
		if (sessionExpiryInterval != null) {
			sb.append(", sessionExpiryInterval=").append(sessionExpiryInterval);
		}
		if (authenticationMethod != null) {
			sb.append(", authenticationMethod=").append(authenticationMethod);
		}
		if (authenticationData != null) {
			sb.append(", authenticationData=").append(Arrays.toString(authenticationData));
		}
		if (reasonString != null) {
			sb.append(", reasonString=").append(reasonString);
		}
		if (userProperties != null && userProperties.size() != 0) {
			sb.append(", userProperties=").append(userProperties);
		}
		if (payloadFormat) {
			sb.append(", isUTF8=").append(payloadFormat);
		}
		if (messageExpiryInterval != null) {
			sb.append(", messageExpiryInterval=").append(messageExpiryInterval);
		}
		if (contentType != null) {
			sb.append(", contentType=").append(contentType);
		}
		if (responseTopic != null) {
			sb.append(", responseTopic=").append(responseTopic);
		}
		if (correlationData != null) {
			sb.append(", correlationData=").append(Arrays.toString(correlationData));
		}
		if (publishSubscriptionIdentifiers.size() != 0) {
			sb.append(", subscriptionIdentifiers=").append(publishSubscriptionIdentifiers);
		}
		sb.append("]");
		return (sb.toString());
	}

}
