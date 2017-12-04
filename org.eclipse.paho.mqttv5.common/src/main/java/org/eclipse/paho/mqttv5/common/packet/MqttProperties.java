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

public class MqttProperties {

	public static final byte PAYLOAD_FORMAT_INDICATOR_IDENTIFIER = 0x01; // 1
	public static final byte PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER = 0x02; // 2
	public static final byte CONTENT_TYPE_IDENTIFIER = 0x03; // 3
	public static final byte RESPONSE_TOPIC_IDENTIFIER = 0x08; // 8
	public static final byte CORRELATION_DATA_IDENTIFIER = 0x09; // 9
	public static final byte SUBSCRIPTION_IDENTIFIER = 0x0B; // 11
	public static final byte SESSION_EXPIRY_INTERVAL_IDENTIFIER = 0x11; // 17
	public static final byte ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER = 0x12; // 18
	public static final byte SERVER_KEEP_ALIVE_IDENTIFIER = 0x13; // 19
	public static final byte AUTH_METHOD_IDENTIFIER = 0x15; // 21
	public static final byte AUTH_DATA_IDENTIFIER = 0x16; // 22
	public static final byte REQUEST_PROBLEM_INFO_IDENTIFIER = 0x17; // 23
	public static final byte WILL_DELAY_INTERVAL_IDENTIFIER = 0x18; // 24
	public static final byte REQUEST_RESPONSE_INFO_IDENTIFIER = 0x19; // 25
	public static final byte RESPONSE_INFO_IDENTIFIER = 0x1A; // 26
	public static final byte SERVER_REFERENCE_IDENTIFIER = 0x1C; // 28
	public static final byte REASON_STRING_IDENTIFIER = 0x1F; // 31
	public static final byte RECEIVE_MAXIMUM_IDENTIFIER = 0x21; // 33
	public static final byte TOPIC_ALIAS_MAXIMUM_IDENTIFIER = 0x22; // 34
	public static final byte TOPIC_ALIAS_IDENTIFIER = 0x23; // 35
	public static final byte MAXIMUM_QOS_IDENTIFIER = 0x24; // 36
	public static final byte RETAIN_AVAILABLE_IDENTIFIER = 0x25; // 37
	public static final byte USER_DEFINED_PAIR_IDENTIFIER = 0x26; // 38
	public static final byte MAXIMUM_PACKET_SIZE_IDENTIFIER = 0x27; // 39
	public static final byte WILDCARD_SUB_AVAILABLE_IDENTIFIER = 0x28; // 40
	public static final byte SUBSCRIPTION_AVAILABLE_IDENTIFIER = 0x29; // 41
	public static final byte SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER = 0x2A; // 42

	private List<Byte> validProperties;

	// Payload format identifiers
	public static final byte PAYLOAD_FORMAT_UNSPECIFIED = 0x00;
	public static final byte PAYLOAD_FORMAT_UTF8 = 0x01;

	private Boolean requestResponseInfo;
	private Boolean requestProblemInfo;
	private Integer willDelayInterval;
	private Integer receiveMaximum;
	private Integer maximumQoS;
	private Integer maximumPacketSize;
	private boolean retainAvailableAdvertisement = false;
	private String assignedClientIdentifier;
	private Integer topicAliasMaximum;
	private Integer topicAlias;
	private Integer serverKeepAlive;
	private String responseInfo;
	private String serverReference;
	private boolean wildcardSubscriptionsAvailable = false;
	private boolean subscriptionIdentifiersAvailable = false;
	private boolean sharedSubscriptionAvailable = false;
	private Integer sessionExpiryInterval;
	private String authMethod;
	private byte[] authData;
	private String reasonString;
	private List<UserProperty> userDefinedProperties = new ArrayList<>();
	private boolean isUTF8 = false;
	private Integer publicationExpiryInterval;
	private String contentType;
	private String responseTopic;
	private byte[] correlationData;
	private List<Integer> subscriptionIdentifiers = new ArrayList<>();

	public MqttProperties() {

	}

	public MqttProperties(Byte[] validProperties) {
		this.validProperties = Arrays.asList(validProperties);

	}

	public void setValidProperties(Byte[] validProperties) {
		this.validProperties = Arrays.asList(validProperties);

	}

	/**
	 * Encodes Non-Null Properties that are in the list of valid properties into a
	 * byte array.
	 * 
	 * @return a byte array containing encoded properties.
	 * @throws MqttException
	 */
	public byte[] encodeProperties() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// Payload Format Indicator
			if (isUTF8 && validProperties.contains(PAYLOAD_FORMAT_INDICATOR_IDENTIFIER)) {
				outputStream.write(PAYLOAD_FORMAT_INDICATOR_IDENTIFIER);
				outputStream.writeByte(PAYLOAD_FORMAT_UTF8);
			}

			// Publication Expiry Interval
			if (publicationExpiryInterval != null && validProperties.contains(PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER)) {
				outputStream.write(PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(publicationExpiryInterval);
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
			if (!subscriptionIdentifiers.isEmpty() && validProperties.contains(SUBSCRIPTION_IDENTIFIER)) {
				for(Integer subscriptionIdentifier : subscriptionIdentifiers) {
					outputStream.write(SUBSCRIPTION_IDENTIFIER);
					outputStream.write(MqttDataTypes.encodeVariableByteInteger(subscriptionIdentifier));
				}
			}

			// Session Expiry Interval
			if (sessionExpiryInterval != null && validProperties.contains(SESSION_EXPIRY_INTERVAL_IDENTIFIER)) {
				outputStream.write(SESSION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(sessionExpiryInterval);
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
			if (authMethod != null && validProperties.contains(AUTH_METHOD_IDENTIFIER)) {
				outputStream.write(AUTH_METHOD_IDENTIFIER);
				MqttDataTypes.encodeUTF8(outputStream, authMethod);
			}

			// Auth Data
			if (authData != null && validProperties.contains(AUTH_DATA_IDENTIFIER)) {
				outputStream.write(AUTH_DATA_IDENTIFIER);
				outputStream.writeShort(authData.length);
				outputStream.write(authData);
			}

			// Request Problem Info
			if (requestProblemInfo != null && validProperties.contains(REQUEST_PROBLEM_INFO_IDENTIFIER)) {
				outputStream.write(REQUEST_PROBLEM_INFO_IDENTIFIER);
				outputStream.write(requestProblemInfo ? 1 : 0);
			}

			// Will Delay Interval
			if (willDelayInterval != null && validProperties.contains(WILL_DELAY_INTERVAL_IDENTIFIER)) {
				outputStream.write(WILL_DELAY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(willDelayInterval);
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
				outputStream.writeShort(maximumQoS);
			}

			// Retain Available Advertisement
			if (retainAvailableAdvertisement && validProperties.contains(RETAIN_AVAILABLE_IDENTIFIER)) {
				outputStream.write(RETAIN_AVAILABLE_IDENTIFIER);
			}

			// User Defined Properties
			if (!userDefinedProperties.isEmpty() && validProperties.contains(USER_DEFINED_PAIR_IDENTIFIER)) {
				for (UserProperty property : userDefinedProperties) {
					// outputStream.write(USER_DEFINED_PAIR_IDENTIFIER);
					outputStream.writeByte(USER_DEFINED_PAIR_IDENTIFIER);
					MqttDataTypes.encodeUTF8(outputStream, property.getKey());
					MqttDataTypes.encodeUTF8(outputStream, property.getValue());
				}
			}

			// Maximum Packet Size
			if (maximumPacketSize != null && validProperties.contains(MAXIMUM_PACKET_SIZE_IDENTIFIER)) {
				outputStream.write(MAXIMUM_PACKET_SIZE_IDENTIFIER);
				outputStream.writeInt(maximumPacketSize);
			}

			// Wildcard Subscription Available flag
			if (wildcardSubscriptionsAvailable && validProperties.contains(WILDCARD_SUB_AVAILABLE_IDENTIFIER)) {
				outputStream.write(WILDCARD_SUB_AVAILABLE_IDENTIFIER);
			}

			// Subscription Identifiers Available flag
			if (subscriptionIdentifiersAvailable && validProperties.contains(SUBSCRIPTION_AVAILABLE_IDENTIFIER)) {
				outputStream.write(SUBSCRIPTION_AVAILABLE_IDENTIFIER);
			}

			// Shared Subscription Available flag
			if (sharedSubscriptionAvailable && validProperties.contains(SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER)) {
				outputStream.write(SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER);
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	public void decodeProperties(DataInputStream dis) throws IOException, MqttException {
		// First get the length of the IV fields
		int length = MqttDataTypes.readVariableByteInteger(dis).getValue();
		if (length > 0) {
			byte[] identifierValueByteArray = new byte[length];
			dis.read(identifierValueByteArray, 0, length);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while (inputStream.available() > 0) {
				// Get the first Byte
				byte identifier = inputStream.readByte();
				if (validProperties.contains(identifier)) {

					if (identifier == PAYLOAD_FORMAT_INDICATOR_IDENTIFIER) {
						isUTF8 = (boolean) inputStream.readBoolean();
					} else if (identifier == PUBLICATION_EXPIRY_INTERVAL_IDENTIFIER) {
						publicationExpiryInterval = inputStream.readInt();
					} else if (identifier == CONTENT_TYPE_IDENTIFIER) {
						contentType = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == RESPONSE_TOPIC_IDENTIFIER) {
						responseTopic = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == CORRELATION_DATA_IDENTIFIER) {
						int correlationDataLength = (int) inputStream.readShort();
						correlationData = new byte[correlationDataLength];
						inputStream.read(correlationData, 0, correlationDataLength);
					} else if (identifier == SUBSCRIPTION_IDENTIFIER) {
						subscriptionIdentifiers.add(MqttDataTypes.readVariableByteInteger(inputStream).getValue());
					} else if (identifier == SESSION_EXPIRY_INTERVAL_IDENTIFIER) {
						sessionExpiryInterval = inputStream.readInt();
					} else if (identifier == ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER) {
						assignedClientIdentifier = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == SERVER_KEEP_ALIVE_IDENTIFIER) {
						serverKeepAlive = (int) inputStream.readShort();
					} else if (identifier == AUTH_METHOD_IDENTIFIER) {
						authMethod = MqttDataTypes.decodeUTF8(inputStream);
					} else if (identifier == AUTH_DATA_IDENTIFIER) {
						int authDataLength = inputStream.readShort();
						authData = new byte[authDataLength];
						inputStream.read(this.authData, 0, authDataLength);
					} else if (identifier == REQUEST_PROBLEM_INFO_IDENTIFIER) {
						requestProblemInfo = inputStream.read() != 0;
					} else if (identifier == WILL_DELAY_INTERVAL_IDENTIFIER) {
						willDelayInterval = inputStream.readInt();
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
						maximumQoS = (int) inputStream.readShort();
					} else if (identifier == RETAIN_AVAILABLE_IDENTIFIER) {
						retainAvailableAdvertisement = true;
					} else if (identifier == USER_DEFINED_PAIR_IDENTIFIER) {
						String key = MqttDataTypes.decodeUTF8(inputStream);
						String value = MqttDataTypes.decodeUTF8(inputStream);
						userDefinedProperties.add(new UserProperty(key, value));
					} else if (identifier == MAXIMUM_PACKET_SIZE_IDENTIFIER) {
						maximumPacketSize = inputStream.readInt();
					} else if (identifier == WILDCARD_SUB_AVAILABLE_IDENTIFIER) {
						wildcardSubscriptionsAvailable = true;
					} else if (identifier == SUBSCRIPTION_AVAILABLE_IDENTIFIER) {
						subscriptionIdentifiersAvailable = true;
					} else if (identifier == SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER) {
						sharedSubscriptionAvailable = true;
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

	public List<Byte> getValidProperties() {
		return validProperties;
	}

	public void setValidProperties(List<Byte> validProperties) {
		this.validProperties = validProperties;
	}

	public Boolean getRequestResponseInfo() {
		return requestResponseInfo;
	}

	public void setRequestResponseInfo(Boolean requestResponseInfo) {
		this.requestResponseInfo = requestResponseInfo;
	}

	public Boolean getRequestProblemInfo() {
		return requestProblemInfo;
	}

	public void setRequestProblemInfo(Boolean requestProblemInfo) {
		this.requestProblemInfo = requestProblemInfo;
	}

	public Integer getWillDelayInterval() {
		return willDelayInterval;
	}

	public void setWillDelayInterval(Integer willDelayInterval) {
		this.willDelayInterval = willDelayInterval;
	}

	public Integer getReceiveMaximum() {
		return receiveMaximum;
	}

	public void setReceiveMaximum(Integer receiveMaximum) {
		this.receiveMaximum = receiveMaximum;
	}

	public Integer getMaximumQoS() {
		return maximumQoS;
	}

	public void setMaximumQoS(Integer maximumQoS) {
		this.maximumQoS = maximumQoS;
	}

	public Integer getMaximumPacketSize() {
		return maximumPacketSize;
	}

	public void setMaximumPacketSize(Integer maximumPacketSize) {
		this.maximumPacketSize = maximumPacketSize;
	}

	public boolean isRetainAvailableAdvertisement() {
		return retainAvailableAdvertisement;
	}

	public void setRetainAvailableAdvertisement(boolean retainAvailableAdvertisement) {
		this.retainAvailableAdvertisement = retainAvailableAdvertisement;
	}

	public String getAssignedClientIdentifier() {
		return assignedClientIdentifier;
	}

	public void setAssignedClientIdentifier(String assignedClientIdentifier) {
		this.assignedClientIdentifier = assignedClientIdentifier;
	}

	public Integer getTopicAliasMaximum() {
		return topicAliasMaximum;
	}

	public void setTopicAliasMaximum(Integer topicAliasMaximum) {
		this.topicAliasMaximum = topicAliasMaximum;
	}

	public Integer getTopicAlias() {
		return topicAlias;
	}

	public void setTopicAlias(Integer topicAlias) {
		this.topicAlias = topicAlias;
	}

	public Integer getServerKeepAlive() {
		return serverKeepAlive;
	}

	public void setServerKeepAlive(Integer serverKeepAlive) {
		this.serverKeepAlive = serverKeepAlive;
	}

	public String getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(String responseInfo) {
		this.responseInfo = responseInfo;
	}

	public String getServerReference() {
		return serverReference;
	}

	public void setServerReference(String serverReference) {
		this.serverReference = serverReference;
	}

	public boolean isWildcardSubscriptionsAvailable() {
		return wildcardSubscriptionsAvailable;
	}

	public void setWildcardSubscriptionsAvailable(boolean wildcardSubscriptionsAvailable) {
		this.wildcardSubscriptionsAvailable = wildcardSubscriptionsAvailable;
	}

	public boolean isSubscriptionIdentifiersAvailable() {
		return subscriptionIdentifiersAvailable;
	}

	public void setSubscriptionIdentifiersAvailable(boolean subscriptionIdentifiersAvailable) {
		this.subscriptionIdentifiersAvailable = subscriptionIdentifiersAvailable;
	}

	public boolean isSharedSubscriptionAvailable() {
		return sharedSubscriptionAvailable;
	}

	public void setSharedSubscriptionAvailable(boolean sharedSubscriptionAvailable) {
		this.sharedSubscriptionAvailable = sharedSubscriptionAvailable;
	}

	public Integer getSessionExpiryInterval() {
		return sessionExpiryInterval;
	}

	public void setSessionExpiryInterval(Integer sessionExpiryInterval) {
		this.sessionExpiryInterval = sessionExpiryInterval;
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public byte[] getAuthData() {
		return authData;
	}

	public void setAuthData(byte[] authData) {
		this.authData = authData;
	}

	public String getReasonString() {
		return reasonString;
	}

	public void setReasonString(String reasonString) {
		this.reasonString = reasonString;
	}

	public List<UserProperty> getUserDefinedProperties() {
		return userDefinedProperties;
	}

	public void setUserDefinedProperties(List<UserProperty> userDefinedProperties) {
		this.userDefinedProperties = userDefinedProperties;
	}

	public boolean isUTF8() {
		return isUTF8;
	}

	public void setUTF8(boolean isUTF8) {
		this.isUTF8 = isUTF8;
	}

	public Integer getPublicationExpiryInterval() {
		return publicationExpiryInterval;
	}

	public void setPublicationExpiryInterval(Integer publicationExpiryInterval) {
		this.publicationExpiryInterval = publicationExpiryInterval;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getResponseTopic() {
		return responseTopic;
	}

	public void setResponseTopic(String responseTopic) {
		this.responseTopic = responseTopic;
	}

	public byte[] getCorrelationData() {
		return correlationData;
	}

	public void setCorrelationData(byte[] correlationData) {
		this.correlationData = correlationData;
	}

	public List<Integer> getSubscriptionIdentifiers() {
		return subscriptionIdentifiers;
	}

	public void setSubscriptionIdentifiers(List<Integer> subscriptionIdentifiers) {
		this.subscriptionIdentifiers = subscriptionIdentifiers;
	}

	@Override
	public String toString() {
		return "MqttProperties [validProperties=" + validProperties + ", requestResponseInfo=" + requestResponseInfo
				+ ", requestProblemInfo=" + requestProblemInfo + ", willDelayInterval=" + willDelayInterval
				+ ", receiveMaximum=" + receiveMaximum + ", maximumQoS=" + maximumQoS + ", maximumPacketSize="
				+ maximumPacketSize + ", retainAvailableAdvertisement=" + retainAvailableAdvertisement
				+ ", assignedClientIdentifier=" + assignedClientIdentifier + ", topicAliasMaximum=" + topicAliasMaximum
				+ ", topicAlias=" + topicAlias + ", serverKeepAlive=" + serverKeepAlive + ", responseInfo="
				+ responseInfo + ", serverReference=" + serverReference + ", wildcardSubscriptionsAvailable="
				+ wildcardSubscriptionsAvailable + ", subscriptionIdentifiersAvailable="
				+ subscriptionIdentifiersAvailable + ", sharedSubscriptionAvailable=" + sharedSubscriptionAvailable
				+ ", sessionExpiryInterval=" + sessionExpiryInterval + ", authMethod=" + authMethod + ", authData="
				+ Arrays.toString(authData) + ", reasonString=" + reasonString + ", userDefinedProperties="
				+ userDefinedProperties + ", isUTF8=" + isUTF8 + ", publicationExpiryInterval="
				+ publicationExpiryInterval + ", contentType=" + contentType + ", responseTopic=" + responseTopic
				+ ", correlationData=" + Arrays.toString(correlationData) + ", subscriptionIdentifiers="
				+ subscriptionIdentifiers + "]";
	}


}
