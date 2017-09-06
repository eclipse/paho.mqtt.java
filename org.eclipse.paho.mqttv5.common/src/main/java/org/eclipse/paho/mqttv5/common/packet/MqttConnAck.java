/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * An on-the-wire representation of an MQTT CONNACK.
 */
public class MqttConnAck extends MqttAck {
	public static final String KEY = "Con";

	private static final int[] validReturnCodes = { MqttReturnCode.RETURN_CODE_SUCCESS,
			MqttReturnCode.RETURN_CODE_UNSPECIFIED_ERROR, MqttReturnCode.RETURN_CODE_MALFORMED_CONTROL_PACKET,
			MqttReturnCode.RETURN_CODE_PROTOCOL_ERROR, MqttReturnCode.RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR,
			MqttReturnCode.RETURN_CODE_UNSUPPORTED_PROTOCOL_VERSION, MqttReturnCode.RETURN_CODE_IDENTIFIER_NOT_VALID,
			MqttReturnCode.RETURN_CODE_BAD_USERNAME_OR_PASSWORD, MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED,
			MqttReturnCode.RETURN_CODE_SERVER_UNAVAILABLE, MqttReturnCode.RETURN_CODE_SERVER_BUSY,
			MqttReturnCode.RETURN_CODE_BANNED, MqttReturnCode.RETURN_CODE_BAD_AUTHENTICATION,
			MqttReturnCode.RETURN_CODE_TOPIC_NAME_INVALID, MqttReturnCode.RETURN_CODE_PACKET_TOO_LARGE,
			MqttReturnCode.RETURN_CODE_QUOTA_EXCEEDED, MqttReturnCode.RETURN_CODE_RETAIN_NOT_SUPPORTED,
			MqttReturnCode.RETURN_CODE_USE_ANOTHER_SERVER, MqttReturnCode.RETURN_CODE_SERVER_MOVED,
			MqttReturnCode.RETURN_CODE_CONNECTION_RATE_EXCEEDED };

	// Fields
	private Integer receiveMaximum;
	private Integer maximumQoS;
	private Integer maximumPacketSize;
	private boolean retainAvailableAdvertisement = false;
	private String assignedClientIdentifier;
	private Integer topicAliasMaximum;
	private String reasonString;
	private Integer serverKeepAlive;
	private String responseInfo;
	private String serverReference;
	private String authMethod;
	private byte[] authData;
	private ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
	private boolean wildcardSubscriptionsAvailable = false;
	private boolean subscriptionIdentifiersAvailable = false;
	private boolean sharedSubscriptionAvailable = false;

	private int returnCode;
	private boolean sessionPresent;

	public MqttConnAck(byte info, byte[] variableHeader) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNACK);
		ByteArrayInputStream bais = new ByteArrayInputStream(variableHeader);
		DataInputStream dis = new DataInputStream(bais);
		sessionPresent = (dis.readUnsignedByte() & 0x01) == 0x01;
		returnCode = dis.readUnsignedByte();
		validateReturnCode(returnCode, validReturnCodes);
		parseIdentifierValueFields(dis);
		dis.close();
	}

	public MqttConnAck(boolean sessionPresent, int returnCode) throws MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNACK);
		this.sessionPresent = sessionPresent;
		validateReturnCode(returnCode, validReturnCodes);
		this.returnCode = returnCode;
		validateReturnCode(returnCode, validReturnCodes);
		this.returnCode = returnCode;
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			// Encode the Session Present Flag
			byte connectAchnowledgeFlag = 0;
			if (sessionPresent) {
				connectAchnowledgeFlag |= 0x01;
			}
			dos.write(connectAchnowledgeFlag);

			// Encode the Connect Return Code
			dos.write((byte) returnCode);

			// Write Identifier / Value Fields
			byte[] identifierValueFieldsByteArray = getIdentifierValueFields();
			dos.write(encodeVariableByteInteger(identifierValueFieldsByteArray.length));
			dos.write(identifierValueFieldsByteArray);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}

	}

	private byte[] getIdentifierValueFields() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);

			// If present, encode the Receive Maximum (3.2.2.3.2)
			if (receiveMaximum != null) {
				outputStream.write(MqttPropertyIdentifiers.RECEIVE_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(receiveMaximum);
			}

			// If present, encode the Maxumum QoS (3.2.2.3.3)
			if (maximumQoS != null) {
				outputStream.write(MqttPropertyIdentifiers.MAXIMUM_QOS_IDENTIFIER);
				outputStream.writeShort(maximumQoS);
			}

			// If present, encode the Retain Available Advertisement (3.2.2.3.4)
			if (retainAvailableAdvertisement) {
				outputStream.write(MqttPropertyIdentifiers.RETAIN_AVAILABLE_IDENTIFIER);
			}

			// If present, encode the Maximum Packet Size (3.2.2.3.5)
			if (maximumPacketSize != null) {
				outputStream.write(MqttPropertyIdentifiers.MAXIMUM_PACKET_SIZE_IDENTIFIER);
				outputStream.writeInt(maximumPacketSize);
			}

			// If present, encode the Assigned Client Identifier (3.2.2.3.6)
			if (assignedClientIdentifier != null) {
				outputStream.write(MqttPropertyIdentifiers.ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER);
				encodeUTF8(outputStream, assignedClientIdentifier);
			}

			// If present, encode the Topic Alias Maximum (3.2.2.3.7)
			if (topicAliasMaximum != null) {
				outputStream.write(MqttPropertyIdentifiers.TOPIC_ALIAS_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(topicAliasMaximum);
			}

			// If present, encode the Reason String (3.2.2.3.8)
			if (reasonString != null) {
				outputStream.write(MqttPropertyIdentifiers.REASON_STRING_IDENTIFIER);
				encodeUTF8(outputStream, reasonString);
			}

			// If present, encode the User Defined Name-Value Pairs (3.2.2.3.9)
			if(userDefinedProperties.size() != 0){
				for(UserProperty property : userDefinedProperties) {
					outputStream.write(MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, property.getKey());
					encodeUTF8(outputStream, property.getValue());
				}
			}

			// If present, encode the Wildcard Subscription Available flag
			// (3.2.2.3.10)
			if (wildcardSubscriptionsAvailable) {
				outputStream.write(MqttPropertyIdentifiers.WILDCARD_SUB_AVAILABLE_IDENTIFIER);
			}

			// If present, encode the Subscription Identifiers Available flag
			// (3.2.2.3.11)
			if (subscriptionIdentifiersAvailable) {
				outputStream.write(MqttPropertyIdentifiers.SUBSCRIPTION_AVAILABLE_IDENTIFIER);
			}

			// If present, encode the Shared Subscription Available flag
			// (3.2.2.3.12)
			if (sharedSubscriptionAvailable) {
				outputStream.write(MqttPropertyIdentifiers.SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER);
			}

			// If present, encode the Server Keep Alive (3.2.2.3.13)
			if (serverKeepAlive != null) {
				outputStream.write(MqttPropertyIdentifiers.SERVER_KEEP_ALIVE_IDENTIFIER);
				outputStream.writeShort(serverKeepAlive);
			}

			// If present, encode the Response Info (3.2.2.3.14)
			if (responseInfo != null) {
				outputStream.write(MqttPropertyIdentifiers.RESPONSE_INFO_IDENTIFIER);
				encodeUTF8(outputStream, responseInfo);
			}

			// If present, encode the Server Reference (3.2.2.3.15)
			if (serverReference != null) {
				outputStream.write(MqttPropertyIdentifiers.SERVER_REFERENCE_IDENTIFIER);
				encodeUTF8(outputStream, serverReference);
			}

			// If present, encode the Auth Method (3.2.2.3.16)
			if (authMethod != null) {
				outputStream.write(MqttPropertyIdentifiers.AUTH_METHOD_IDENTIFIER);
				encodeUTF8(outputStream, authMethod);
			}

			// If present, encode the Auth Data (3.2.2.17)
			if (authData != null) {
				outputStream.write(MqttPropertyIdentifiers.AUTH_DATA_IDENTIFIER);
				outputStream.writeShort(authData.length);
				outputStream.write(authData);
			}

			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	private void parseIdentifierValueFields(DataInputStream dis) throws IOException, MqttException {
		// First, get the length of the IV fields
		int lengthVBI = readVariableByteInteger(dis).getValue();
		if (lengthVBI > 0) {
			byte[] identifierValueByteArray = new byte[lengthVBI];
			dis.read(identifierValueByteArray, 0, lengthVBI);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);
			while (inputStream.available() > 0) {
				// Get the first byte (identifier)
				byte identifier = inputStream.readByte();
				if (identifier == MqttPropertyIdentifiers.RECEIVE_MAXIMUM_IDENTIFIER) {
					receiveMaximum = (int) inputStream.readShort();
				} else if (identifier == MqttPropertyIdentifiers.MAXIMUM_QOS_IDENTIFIER) {
					maximumQoS = (int) inputStream.readShort();
				} else if (identifier == MqttPropertyIdentifiers.RETAIN_AVAILABLE_IDENTIFIER) {
					retainAvailableAdvertisement = true;
				} else if (identifier == MqttPropertyIdentifiers.MAXIMUM_PACKET_SIZE_IDENTIFIER) {
					maximumPacketSize = inputStream.readInt();
				} else if (identifier == MqttPropertyIdentifiers.ASSIGNED_CLIENT_IDENTIFIER_IDENTIFIER) {
					assignedClientIdentifier = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.TOPIC_ALIAS_MAXIMUM_IDENTIFIER) {
					topicAliasMaximum = (int) inputStream.readShort();
				} else if (identifier == MqttPropertyIdentifiers.REASON_STRING_IDENTIFIER) {
					reasonString = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER) {
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedProperties.add(new UserProperty(key,  value));
				} else if (identifier == MqttPropertyIdentifiers.WILDCARD_SUB_AVAILABLE_IDENTIFIER) {
					wildcardSubscriptionsAvailable = true;
				} else if (identifier == MqttPropertyIdentifiers.SUBSCRIPTION_AVAILABLE_IDENTIFIER) {
					subscriptionIdentifiersAvailable = true;
				} else if (identifier == MqttPropertyIdentifiers.SHARED_SUBSCRIPTION_AVAILABLE_IDENTIFIER) {
					sharedSubscriptionAvailable = true;
				} else if (identifier == MqttPropertyIdentifiers.SERVER_KEEP_ALIVE_IDENTIFIER) {
					serverKeepAlive = (int) inputStream.readShort();
				} else if (identifier == MqttPropertyIdentifiers.RESPONSE_INFO_IDENTIFIER) {
					responseInfo = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.SERVER_REFERENCE_IDENTIFIER) {
					serverReference = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.AUTH_METHOD_IDENTIFIER) {
					authMethod = decodeUTF8(inputStream);
				} else if (identifier == MqttPropertyIdentifiers.AUTH_DATA_IDENTIFIER) {
					int authDataLength = inputStream.readShort();
					authData = new byte[authDataLength];
					inputStream.read(authData, 0, authDataLength);
				} else {
					// Unidentified Identifier
					throw new MqttException(MqttException.REASON_CODE_INVALID_IDENTIFIER);
				}
			}
		}
	}

	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	@Override
	public boolean isMessageIdRequired() {
		return false;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	public boolean getSessionPresent() {
		return sessionPresent;
	}

	public void setSessionPresent(boolean sessionPresent) {
		this.sessionPresent = sessionPresent;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public int getReceiveMaximum() {
		if(receiveMaximum == null) {
			return 65535;
		}
		return receiveMaximum;
	}

	public void setReceiveMaximum(Integer receiveMaximum) {
		this.receiveMaximum = receiveMaximum;
	}

	public boolean getRetainUnavailableAdvertisement() {
		return retainAvailableAdvertisement;
	}

	public void setRetainAvailableAdvertisement(Boolean retainUnavailableAdvertisement) {
		this.retainAvailableAdvertisement = retainUnavailableAdvertisement;
	}

	public String getAssignedClientIdentifier() {
		return assignedClientIdentifier;
	}

	public void setAssignedClientIdentifier(String assignedClientIdentifier) {
		this.assignedClientIdentifier = assignedClientIdentifier;
	}

	public int getTopicAliasMaximum() {
		if(topicAliasMaximum == null) {
			return 0;
		}
		return topicAliasMaximum;
		
	}

	public void setTopicAliasMaximum(Integer topicAliasMaximum) {
		this.topicAliasMaximum = topicAliasMaximum;
	}

	public String getReasonString() {
		return reasonString;
	}

	public void setReasonString(String reasonString) {
		this.reasonString = reasonString;
	}

	public int getServerKeepAlive() {
		if(serverKeepAlive == null) {
			return -1;
		}
		return serverKeepAlive;
	}

	public void setServerKeepAlive(int serverKeepAlive) {
		this.serverKeepAlive = serverKeepAlive;
	}

	public String getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(String replyInfo) {
		this.responseInfo = replyInfo;
	}

	public String getServerReference() {
		return serverReference;
	}

	public void setServerReference(String serverReference) {
		if ((returnCode == MqttReturnCode.RETURN_CODE_USE_ANOTHER_SERVER)
				|| (returnCode == MqttReturnCode.RETURN_CODE_SERVER_MOVED)) {
			this.serverReference = serverReference;
		} else {
			// FIXME
			throw new IllegalArgumentException(
					"The Server MUST only send a Server Reference along with a Return Code of 0x9C - Use another Server or 0x9D - Server Moved. (3.2.2.12)");
		}

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

	public ArrayList<UserProperty> getUserDefinedProperties() {
		return userDefinedProperties;
	}

	public void setUserDefinedProperties(ArrayList<UserProperty> userDefinedProperties) {
		this.userDefinedProperties = userDefinedProperties;
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

	public int getMaximumQoS() {
		if(maximumQoS == null) {
			return 2;
		}
		return maximumQoS;
	}

	public void setMaximumQoS(int maximumQoS) {
		this.maximumQoS = maximumQoS;
	}

	public int getMaximumPacketSize() {
		if(maximumPacketSize == null) {
			return -1;
		}
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

	public static int[] getValidreturncodes() {
		return validReturnCodes;
	}


	@Override
	public String toString() {
		return "MqttConnAck [receiveMaximum=" + receiveMaximum + ", maximumQoS=" + maximumQoS + ", maximumPacketSize="
				+ maximumPacketSize + ", retainAvailableAdvertisement=" + retainAvailableAdvertisement
				+ ", assignedClientIdentifier=" + assignedClientIdentifier + ", topicAliasMaximum=" + topicAliasMaximum
				+ ", reasonString=" + reasonString + ", serverKeepAlive=" + serverKeepAlive + ", replyInfo=" + responseInfo
				+ ", serverReference=" + serverReference + ", authMethod=" + authMethod + ", authData="
				+ Arrays.toString(authData) + ", userDefinedProperties=" + userDefinedProperties
				+ ", wildcardSubscriptionsAvailable=" + wildcardSubscriptionsAvailable
				+ ", subscriptionIdentifiersAvailable=" + subscriptionIdentifiersAvailable
				+ ", sharedSubscriptionAvailable=" + sharedSubscriptionAvailable + ", returnCode=" + returnCode
				+ ", sessionPresent=" + sessionPresent + "]";
	}

	

}
