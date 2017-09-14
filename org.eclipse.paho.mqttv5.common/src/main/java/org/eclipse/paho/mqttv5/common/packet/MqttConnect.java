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
 * 	  Dave Locke   - Original MQTTv3 implementation
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
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class MqttConnect extends MqttWireMessage {

	public static final String KEY = "Con";

	// Fields
	private byte info;
	private String clientId;
	private boolean reservedByte;
	private boolean cleanSession;
	private MqttMessage willMessage;
	private String userName;
	private byte[] password;
	private int keepAliveInterval;
	private String willDestination;
	private int mqttVersion = DEFAULT_PROTOCOL_VERSION;
	private Integer sessionExpiryInterval;
	private Integer willDelayInterval;
	private Integer receiveMaximum;
	private Integer maximumPacketSize;

	private Integer topicAliasMaximum;
	private Boolean requestResponseInfo;
	private Boolean requestProblemInfo;

	private ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
	private String authMethod;
	private byte[] authData;

	/**
	 * Constructor for an on the wire MQTT Connect message
	 * 
	 * @param info
	 *            - Info Byte
	 * @param data
	 *            - The variable header and payload bytes.
	 * @throws IOException
	 *             - if an exception occurs when decoding an input stream
	 * @throws MqttException
	 *             - If an exception occurs decoding this packet
	 */
	public MqttConnect(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.info = info;
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);

		// Verify the Protocol name and version
		String protocolName = decodeUTF8(dis);
		if (!protocolName.equalsIgnoreCase(DEFAULT_PROTOCOL_NAME)) {
			throw new MqttPacketException(MqttPacketException.PACKET_CONNECT_ERROR_UNSUPPORTED_PROTOCOL_NAME);
		}
		mqttVersion = dis.readByte();
		if (mqttVersion != DEFAULT_PROTOCOL_VERSION) {
			throw new MqttPacketException(MqttPacketException.PACKET_CONNECT_ERROR_UNSUPPORTED_PROTOCOL_VERSION);
		}
    
		byte connectFlags = dis.readByte();
		reservedByte = (connectFlags & 0x01) != 0;
		cleanSession = (connectFlags & 0x02) != 0;
		boolean willFlag = (connectFlags & 0x04) != 0;
		int willQoS = (connectFlags >> 3) & 0x03;
		boolean willRetain = (connectFlags & 0x20) != 0;
		boolean passwordFlag = (connectFlags & 0x40) != 0;
		boolean usernameFlag = (connectFlags & 0x80) != 0;

		if (reservedByte) {
			throw new MqttPacketException(MqttPacketException.PACKET_CONNECT_ERROR_INVALID_RESERVE_FLAG);
		}

		keepAliveInterval = dis.readUnsignedShort();
		parseIdentifierValueFields(dis);
		clientId = decodeUTF8(dis);
		if (willFlag) {
			if (willQoS == 3) {
				throw new MqttPacketException(MqttPacketException.PACKET_CONNECT_ERROR_INVALID_WILL_QOS);
			}
			willDestination = decodeUTF8(dis);
			int willMessageLength = dis.readShort();
			byte[] willMessageBytes = new byte[willMessageLength];
			dis.read(willMessageBytes, 0, willMessageLength);
			willMessage = new MqttMessage(willMessageBytes);
			willMessage.setQos(willQoS);
			willMessage.setRetained(willRetain);
		}
		if (usernameFlag) {
			userName = decodeUTF8(dis);
		}
		if (passwordFlag) {
			int passwordLength = dis.readShort();
			password = new byte[passwordLength];
			dis.read(password, 0, passwordLength);
		}

		dis.close();
	}

	/**
	 * Constructor for a new MQTT Connect Message
	 * 
	 * @param clientId
	 *            - The Client Identifier
	 * @param mqttVersion
	 *            - The MQTT Protocol version
	 * @param cleanSession
	 *            - The Clean Session Identifier
	 * @param keepAliveInterval
	 *            - The Keep Alive Interval
	 * 
	 */
	public MqttConnect(String clientId, int mqttVersion, boolean cleanSession, int keepAliveInterval) {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.clientId = clientId;
		this.mqttVersion = mqttVersion;
		this.cleanSession = cleanSession;
		this.keepAliveInterval = keepAliveInterval;

	}

	@Override
	protected byte getMessageInfo() {
		return (byte) 0;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			// Encode the Protocol Name
			encodeUTF8(dos, "MQTT");

			// Encode the MQTT Version
			dos.write(mqttVersion);

			byte connectFlags = 0;

			if (cleanSession) {
				connectFlags |= 0x02;
			}

			if (willMessage != null) {
				connectFlags |= 0x04;
				connectFlags |= (willMessage.getQos() << 3);
				if (willMessage.isRetained()) {
					connectFlags |= 0x20;
				}
			}

			if (userName != null) {
				connectFlags |= 0x80;
				if (password != null) {
					connectFlags |= 0x40;
				}
			}

			dos.write(connectFlags);
			dos.writeShort(keepAliveInterval);

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

			// If Present, encode the Session Expiry Interval (3.1.2.11.2)
			if (sessionExpiryInterval != null) {
				outputStream.write(MqttPropertyIdentifiers.SESSION_EXPIRY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(sessionExpiryInterval);
			}

			// If Present, encode the Will Delay Interval (3.1.2.11.3)
			if (willDelayInterval != null) {
				outputStream.write(MqttPropertyIdentifiers.WILL_DELAY_INTERVAL_IDENTIFIER);
				outputStream.writeInt(willDelayInterval);
			}

			// If present, encode the Receive Maximum (3.1.2.11.4)
			if (receiveMaximum != null) {
				outputStream.write(MqttPropertyIdentifiers.RECEIVE_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(receiveMaximum);
			}

			// If present, encode the Maximum Packet Size (3.1.2.11.5)
			if (maximumPacketSize != null) {
				outputStream.write(MqttPropertyIdentifiers.MAXIMUM_PACKET_SIZE_IDENTIFIER);
				outputStream.writeInt(maximumPacketSize);
			}

			// If present, encode the Topic Alias Maximum (3.1.2.11.6)
			if (topicAliasMaximum != null) {
				outputStream.write(MqttPropertyIdentifiers.TOPIC_ALIAS_MAXIMUM_IDENTIFIER);
				outputStream.writeShort(topicAliasMaximum);
			}

			// If present, encode the Request Reply Info (3.1.2.11.7)
			if (requestResponseInfo != null) {
				outputStream.write(MqttPropertyIdentifiers.REQUEST_RESPONSE_INFO_IDENTIFIER);
				outputStream.write(requestResponseInfo ? 1 : 0);
			}

			// If present, encode the Request Problem Info (3.1.2.11.8)
			if (requestProblemInfo != null) {
				outputStream.write(MqttPropertyIdentifiers.REQUEST_PROBLEM_INFO_IDENTIFIER);
				outputStream.write(requestProblemInfo ? 1 : 0);
			}

			// If present, encode the User Defined Name-Value Pairs (3.1.2.11.9)
			if (userDefinedProperties.size() != 0) {
				for (UserProperty property : userDefinedProperties) {
					outputStream.write(MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(outputStream, property.getKey());
					encodeUTF8(outputStream, property.getValue());
				}
			}

			// If present, encode the Auth Method (3.1.2.11.10)
			if (authMethod != null) {
				outputStream.write(MqttPropertyIdentifiers.AUTH_METHOD_IDENTIFIER);
				encodeUTF8(outputStream, authMethod);
			}

			// If present, encode the Auth Data (3.1.2.11.11)
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
		// First get the length of the IV fields
		int lengthVBI = readVariableByteInteger(dis).getValue();
		if (lengthVBI > 0) {
			byte[] identifierValueByteArray = new byte[lengthVBI];
			dis.read(identifierValueByteArray, 0, lengthVBI);
			ByteArrayInputStream bais = new ByteArrayInputStream(identifierValueByteArray);
			DataInputStream inputStream = new DataInputStream(bais);

			while (inputStream.available() > 0) {
				// Get the first byte (Identifier)
				byte identifier = inputStream.readByte();

				if (identifier == MqttPropertyIdentifiers.SESSION_EXPIRY_INTERVAL_IDENTIFIER) {
					sessionExpiryInterval = inputStream.readInt();
				} else if (identifier == MqttPropertyIdentifiers.WILL_DELAY_INTERVAL_IDENTIFIER) {
					willDelayInterval = inputStream.readInt();
				} else if (identifier == MqttPropertyIdentifiers.RECEIVE_MAXIMUM_IDENTIFIER) {
					receiveMaximum = (int) inputStream.readShort();
				} else if (identifier == MqttPropertyIdentifiers.MAXIMUM_PACKET_SIZE_IDENTIFIER) {
					maximumPacketSize = inputStream.readInt();
				} else if (identifier == MqttPropertyIdentifiers.TOPIC_ALIAS_MAXIMUM_IDENTIFIER) {
					topicAliasMaximum = (int) inputStream.readShort();
				} else if (identifier == MqttPropertyIdentifiers.REQUEST_RESPONSE_INFO_IDENTIFIER) {
					requestResponseInfo = inputStream.read() != 0;
				} else if (identifier == MqttPropertyIdentifiers.REQUEST_PROBLEM_INFO_IDENTIFIER) {
					requestProblemInfo = inputStream.read() != 0;
				} else if (identifier == MqttPropertyIdentifiers.USER_DEFINED_PAIR_IDENTIFIER) {
					String key = decodeUTF8(inputStream);
					String value = decodeUTF8(inputStream);
					userDefinedProperties.add(new UserProperty(key, value));
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

	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			encodeUTF8(dos, clientId);

			if (willMessage != null) {
				encodeUTF8(dos, willDestination);
				dos.writeShort(willMessage.getPayload().length);
				dos.write(willMessage.getPayload());
			}

			if (userName != null) {
				encodeUTF8(dos, userName);
				if (password != null) {
					encodeUTF8(dos, new String(password));
				}
			}
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
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

	public void setWillMessage(MqttMessage willMessage) {
		this.willMessage = willMessage;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}

	public void setWillDestination(String willDestination) {
		this.willDestination = willDestination;
	}

	public void setSessionExpiryInterval(int sessionExpiryInterval) {
		this.sessionExpiryInterval = sessionExpiryInterval;
	}

	public void setWillDelayInterval(int willDelayInterval) {
		this.willDelayInterval = willDelayInterval;
	}

	public void setReceiveMaximum(int receiveMaximum) {
		this.receiveMaximum = receiveMaximum;
	}

	public void setTopicAliasMaximum(int topicAliasMaximum) {
		this.topicAliasMaximum = topicAliasMaximum;
	}

	public void setRequestReplyInfo(boolean requestReplyInfo) {
		this.requestResponseInfo = requestReplyInfo;
	}

	public void setRequestProblemInfo(boolean requestProblemInfo) {
		this.requestProblemInfo = requestProblemInfo;
	}

	public void setUserDefinedProperties(ArrayList<UserProperty> userDefinedProperties) {
		this.userDefinedProperties = userDefinedProperties;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public void setAuthData(byte[] authData) {
		this.authData = authData;
	}

	public byte getInfo() {
		return info;
	}

	public String getClientId() {
		return clientId;
	}

	public MqttMessage getWillMessage() {
		return willMessage;
	}

	public String getUserName() {
		return userName;
	}

	public byte[] getPassword() {
		return password;
	}

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public String getWillDestination() {
		return willDestination;
	}

	public int getMqttVersion() {
		return mqttVersion;
	}

	public int getSessionExpiryInterval() {
		return sessionExpiryInterval;
	}

	public int getWillDelayInterval() {
		return willDelayInterval;
	}

	public int getReceiveMaximum() {
		return receiveMaximum;
	}



	public int getTopicAliasMaximum() {
		return topicAliasMaximum;
	}

	public Boolean getRequestReplyInfo() {
		return requestResponseInfo;
	}

	public Boolean getRequestProblemInfo() {
		return requestProblemInfo;
	}

	public ArrayList<UserProperty> getUserDefinedProperties() {
		return userDefinedProperties;
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public byte[] getAuthData() {
		return authData;
	}
	
	public int getMaximumPacketSize() {
		return maximumPacketSize;
	}

	public void setMaximumPacketSize(Integer maximumPacketSize) {
		this.maximumPacketSize = maximumPacketSize;
	}

	@Override
	public String toString() {
		return "MqttConnect [info=" + info + ", clientId=" + clientId + ", reservedByte=" + reservedByte
				+ ", cleanSession=" + cleanSession + ", willMessage=" + willMessage + ", userName=" + userName
				+ ", password=" + Arrays.toString(password) + ", keepAliveInterval=" + keepAliveInterval
				+ ", willDestination=" + willDestination + ", mqttVersion=" + mqttVersion + ", sessionExpiryInterval="
				+ sessionExpiryInterval + ", willDelayInterval=" + willDelayInterval + ", receiveMaximum="
				+ receiveMaximum + ", maximumPacketSize=" + maximumPacketSize + ", topicAliasMaximum="
				+ topicAliasMaximum + ", requestResponseInfo=" + requestResponseInfo + ", requestProblemInfo="
				+ requestProblemInfo + ", userDefinedProperties=" + userDefinedProperties + ", authMethod=" + authMethod
				+ ", authData=" + Arrays.toString(authData) + "]";
	}
}
