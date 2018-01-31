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
import java.util.Arrays;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class MqttConnect extends MqttWireMessage {

	public static final String KEY = "Con";

	private MqttProperties properties;

	private MqttProperties willProperties;

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

	private static final Byte[] validProperties = { MqttProperties.SESSION_EXPIRY_INTERVAL_IDENTIFIER,
			MqttProperties.WILL_DELAY_INTERVAL_IDENTIFIER, MqttProperties.RECEIVE_MAXIMUM_IDENTIFIER,
			MqttProperties.MAXIMUM_PACKET_SIZE_IDENTIFIER, MqttProperties.TOPIC_ALIAS_MAXIMUM_IDENTIFIER,
			MqttProperties.REQUEST_RESPONSE_INFO_IDENTIFIER, MqttProperties.REQUEST_PROBLEM_INFO_IDENTIFIER,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER, MqttProperties.AUTH_METHOD_IDENTIFIER,
			MqttProperties.AUTH_DATA_IDENTIFIER };

	private static final Byte[] validWillProperties = { MqttProperties.WILL_DELAY_INTERVAL_IDENTIFIER,
			MqttProperties.PAYLOAD_FORMAT_INDICATOR_IDENTIFIER, MqttProperties.MESSAGE_EXPIRY_INTERVAL_IDENTIFIER,
			MqttProperties.RESPONSE_TOPIC_IDENTIFIER, MqttProperties.CORRELATION_DATA_IDENTIFIER,
			MqttProperties.USER_DEFINED_PAIR_IDENTIFIER, MqttProperties.CONTENT_TYPE_IDENTIFIER };

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
		this.properties = new MqttProperties(validProperties);
		this.willProperties = new MqttProperties(validWillProperties);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);

		// Verify the Protocol name and version
		String protocolName = MqttDataTypes.decodeUTF8(dis);
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
		properties.decodeProperties(dis);
		clientId = MqttDataTypes.decodeUTF8(dis);
		
		if (willFlag) {
			willProperties.decodeProperties(dis);
			if (willQoS == 3) {
				throw new MqttPacketException(MqttPacketException.PACKET_CONNECT_ERROR_INVALID_WILL_QOS);
			}
			willDestination = MqttDataTypes.decodeUTF8(dis);
			int willMessageLength = dis.readShort();
			byte[] willMessageBytes = new byte[willMessageLength];
			dis.read(willMessageBytes, 0, willMessageLength);
			willMessage = new MqttMessage(willMessageBytes);
			willMessage.setQos(willQoS);
			willMessage.setRetained(willRetain);
		}
		if (usernameFlag) {
			userName = MqttDataTypes.decodeUTF8(dis);
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
	 * @param properties
	 *            - The {@link MqttProperties} for the packet.
	 * @param willProperties
	 *            - The {@link MqttProperties} for the will message.
	 *
	 */
	public MqttConnect(String clientId, int mqttVersion, boolean cleanSession, int keepAliveInterval,
			MqttProperties properties, MqttProperties willProperties) {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.clientId = clientId;
		this.mqttVersion = mqttVersion;
		this.cleanSession = cleanSession;
		this.keepAliveInterval = keepAliveInterval;
		if (properties != null) {
			this.properties = properties;
		} else {
			this.properties = new MqttProperties();
		}
		this.properties.setValidProperties(validProperties);
		this.willProperties = willProperties;
		this.willProperties.setValidProperties(validWillProperties);

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
			MqttDataTypes.encodeUTF8(dos, "MQTT");

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
			byte[] identifierValueFieldsByteArray = this.properties.encodeProperties();
			dos.write(identifierValueFieldsByteArray);
			dos.flush();
			
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			MqttDataTypes.encodeUTF8(dos, clientId);

			

			if (willMessage != null) {
				// Encode Will properties here
				byte[] willIdentifierValueFieldsByteArray = willProperties.encodeProperties();
				dos.write(willIdentifierValueFieldsByteArray);
				
				MqttDataTypes.encodeUTF8(dos, willDestination);
				dos.writeShort(willMessage.getPayload().length);
				dos.write(willMessage.getPayload());
			}

			if (userName != null) {
				MqttDataTypes.encodeUTF8(dos, userName);
				if (password != null) {
					MqttDataTypes.encodeUTF8(dos, new String(password));
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

	@Override
	public MqttProperties getProperties() {
		return properties;
	}

	public MqttProperties getWillProperties() {
		return willProperties;
	}

	@Override
	public String toString() {
		return "MqttConnect [properties=" + properties + ", willProperties=" + willProperties + ", info=" + info
				+ ", clientId=" + clientId + ", reservedByte=" + reservedByte + ", cleanSession=" + cleanSession
				+ ", willMessage=" + willMessage + ", userName=" + userName + ", password=" + Arrays.toString(password)
				+ ", keepAliveInterval=" + keepAliveInterval + ", willDestination=" + willDestination + ", mqttVersion="
				+ mqttVersion + "]";
	}
}
