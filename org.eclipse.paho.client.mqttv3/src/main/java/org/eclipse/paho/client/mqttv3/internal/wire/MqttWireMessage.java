/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.wire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;

/**
 * An on-the-wire representation of an MQTT message.
 */
public abstract class MqttWireMessage {
	public static final byte MESSAGE_TYPE_CONNECT = 1;
	public static final byte MESSAGE_TYPE_CONNACK = 2;
	public static final byte MESSAGE_TYPE_PUBLISH = 3;
	public static final byte MESSAGE_TYPE_PUBACK = 4;
	public static final byte MESSAGE_TYPE_PUBREC = 5;
	public static final byte MESSAGE_TYPE_PUBREL = 6;
	public static final byte MESSAGE_TYPE_PUBCOMP = 7;
	public static final byte MESSAGE_TYPE_SUBSCRIBE = 8;
	public static final byte MESSAGE_TYPE_SUBACK = 9;
	public static final byte MESSAGE_TYPE_UNSUBSCRIBE = 10;
	public static final byte MESSAGE_TYPE_UNSUBACK = 11;
	public static final byte MESSAGE_TYPE_PINGREQ = 12;
	public static final byte MESSAGE_TYPE_PINGRESP = 13;
	public static final byte MESSAGE_TYPE_DISCONNECT = 14;

	protected static final Charset STRING_ENCODING = StandardCharsets.UTF_8;

	private static final String[] PACKET_NAMES = {"reserved", "CONNECT", "CONNACK", "PUBLISH", "PUBACK", "PUBREC",
			"PUBREL", "PUBCOMP", "SUBSCRIBE", "SUBACK", "UNSUBSCRIBE", "UNSUBACK", "PINGREQ", "PINGRESP",
			"DISCONNECT"};
	

	private static final long FOUR_BYTE_INT_MAX = 4294967295L;
	private static final int VARIABLE_BYTE_INT_MAX = 268435455;

	// The type of the message (e.g. CONNECT, PUBLISH, PUBACK)
	private byte type;
	// The MQTT message ID
	protected int msgId;

	protected boolean duplicate = false;

	/**
	 * The token associated with the message. It needs to be stored here,
	 * because QoS 0 messages do not have an ID, and tokens for these messages
	 * can thus not be stored in the Token Store.
	 */
	private MqttToken token;


	public MqttWireMessage(byte type) {
		this.type = type;
		// Use zero as the default message ID. Can't use -1, as that is serialized
		// as 65535, which would be a valid ID.
		this.msgId = 0;
	}

	/**
	 * Sub-classes should override this to encode the message info. Only the
	 * least-significant four bits will be used.
	 * 
	 * @return The Message information byte
	 */
	protected abstract byte getMessageInfo();

	/**
	 * Sub-classes should override this method to supply the payload bytes.
	 * 
	 * @return The payload byte array
	 * @throws MqttException
	 *             if an exception occurs whilst getting the payload
	 */
	public byte[] getPayload() throws MqttException {
		return new byte[0];
	}

	/**
	 * @return the type of the message.
	 */
	public byte getType() {
		return type;
	}

	/**
	 * @return the MQTT message ID.
	 */
	public int getMessageId() {
		return msgId;
	}

	/**
	 * Sets the MQTT message ID.
	 * 
	 * @param msgId
	 *            the MQTT message ID
	 */
	public void setMessageId(int msgId) {
		this.msgId = msgId;
	}

	/**
	 * Returns a key associated with the message. For most message types this will
	 * be unique. For connect, disconnect and ping only one message of this type is
	 * allowed so a fixed key will be returned
	 * 
	 * @return key a key associated with the message
	 */
	public String getKey() {
		return Integer.toString(getMessageId());
	}

	public byte[] getHeader() throws MqttException {
		try {
			int first = ((getType() & 0x0f) << 4) ^ (getMessageInfo() & 0x0f);
			byte[] varHeader = getVariableHeader();
			int remLen = varHeader.length + getPayload().length;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeByte(first);
			dos.write(encodeMBI(remLen));
			dos.write(varHeader);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}

	protected abstract byte[] getVariableHeader() throws MqttException;

	/**
	 * @return whether or not this message needs to include a message ID.
	 */
	public boolean isMessageIdRequired() {
		return true;
	}

	public static MqttWireMessage createWireMessage(MqttPersistable data) throws MqttException {
		byte[] payload = data.getPayloadBytes();
		// The persistable interface allows a message to be restored entirely in the
		// header array
		// Need to treat these two arrays as a single array of bytes and use the
		// decoding
		// logic to identify the true header/payload split
		if (payload == null) {
			payload = new byte[0];
		}
		MultiByteArrayInputStream mbais = new MultiByteArrayInputStream(data.getHeaderBytes(), data.getHeaderOffset(),
				data.getHeaderLength(), payload, data.getPayloadOffset(), data.getPayloadLength());
		return createWireMessage(mbais);
	}

	public static MqttWireMessage createWireMessage(byte[] bytes) throws MqttException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return createWireMessage(bais);
	}

	private static MqttWireMessage createWireMessage(InputStream inputStream) throws MqttException {
		try {
			CountingInputStream counter = new CountingInputStream(inputStream);
			DataInputStream in = new DataInputStream(counter);
			int first = in.readUnsignedByte();
			byte type = (byte) (first >> 4);
			byte info = (byte) (first &= 0x0f);
			long remLen = readMBI(in).getValue();
			long totalToRead = counter.getCounter() + remLen;

			MqttWireMessage result;
			long remainder = totalToRead - counter.getCounter();
			byte[] data = new byte[0];
			// The remaining bytes must be the payload...
			if (remainder > 0) {
				data = new byte[(int) remainder];
				in.readFully(data, 0, data.length);
			}

			if (type == MqttWireMessage.MESSAGE_TYPE_CONNECT) {
				result = new MqttConnect(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_PUBLISH) {
				result = new MqttPublish(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_PUBACK) {
				result = new MqttPubAck(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_PUBCOMP) {
				result = new MqttPubComp(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_CONNACK) {
				result = new MqttConnack(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_PINGREQ) {
				result = new MqttPingReq(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_PINGRESP) {
				result = new MqttPingResp(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE) {
				result = new MqttSubscribe(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_SUBACK) {
				result = new MqttSuback(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE) {
				result = new MqttUnsubscribe(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_UNSUBACK) {
				result = new MqttUnsubAck(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_PUBREL) {
				result = new MqttPubRel(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_PUBREC) {
				result = new MqttPubRec(info, data);
			} else if (type == MqttWireMessage.MESSAGE_TYPE_DISCONNECT) {
				result = new MqttDisconnect(info, data);
			} else {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
			}
			return result;
		} catch (IOException io) {
			throw new MqttException(io);
		}
	}

	public static byte[] encodeMBI(long number) {
		validateVariableByteInt((int) number);
		int numBytes = 0;
		long no = number;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// Encode the remaining length fields in the four bytes
		do {
			byte digit = (byte) (no % 128);
			no = no / 128;
			if (no > 0) {
				digit |= 0x80;
			}
			bos.write(digit);
			numBytes++;
		} while ((no > 0) && (numBytes < 4));

		return bos.toByteArray();
	}

	/**
	 * Decodes an MQTT Multi-Byte Integer from the given stream.
	 * 
	 * @param in
	 *            the input stream
	 * @return {@link MultiByteInteger}
	 * @throws IOException
	 *             if an exception occurs when reading the input stream
	 */
	public static MultiByteInteger readMBI(DataInputStream in) throws IOException {
		byte digit;
		int msgLength = 0;
		int multiplier = 1;
		int count = 0;

		do {
			digit = in.readByte();
			count++;
			msgLength += ((digit & 0x7F) * multiplier);
			multiplier *= 128;
		} while ((digit & 0x80) != 0);
		
		if (msgLength < 0 || msgLength > VARIABLE_BYTE_INT_MAX) {
			throw new IOException("This property must be a number between 0 and " + VARIABLE_BYTE_INT_MAX
					+ ". Read value was: " + msgLength);
		}

		return new MultiByteInteger(msgLength, count);
	}

	protected byte[] encodeMessageId() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(msgId);
			dos.flush();
			return baos.toByteArray();
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}

	public boolean isRetryable() {
		return false;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	/**
	 * Encodes a String given into UTF-8, before writing this to the
	 * DataOutputStream the length of the encoded string is encoded into two bytes
	 * and then written to the
	 * DataOutputStream. @link{DataOutputStream#writeUFT(String)} should be no
	 * longer used. @link{DataOutputStream#writeUFT(String)} does not correctly
	 * encode UTF-16 surrogate characters.
	 * 
	 * @param dos
	 *            The stream to write the encoded UTF-8 String to.
	 * @param stringToEncode
	 *            The String to be encoded
	 * @throws MqttException
	 *             Thrown when an error occurs with either the encoding or writing
	 *             the data to the stream
	 */
	public static void encodeUTF8(DataOutputStream dos, String stringToEncode) throws MqttException {
		validateUTF8String(stringToEncode);
		try {

			byte[] encodedString = stringToEncode.getBytes(STRING_ENCODING);
			byte byte1 = (byte) ((encodedString.length >>> 8) & 0xFF);
			byte byte2 = (byte) ((encodedString.length >>> 0) & 0xFF);

			dos.write(byte1);
			dos.write(byte2);
			dos.write(encodedString);
		} catch (UnsupportedEncodingException ex) {
			throw new MqttException(ex);
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}

	/**
	 * Decodes a UTF-8 string from the DataInputStream
	 * provided. @link(DataInoutStream#readUTF()) should be no longer used,
	 * because @link(DataInoutStream#readUTF()) does not decode UTF-16 surrogate
	 * characters correctly.
	 * 
	 * @param input
	 *            The input stream from which to read the encoded string
	 * @return a decoded String from the DataInputStream
	 * @throws MqttException
	 *             thrown when an error occurs with either reading from the stream
	 *             or decoding the encoded string.
	 */
	public static String decodeUTF8(DataInputStream input) throws MqttException {
		int encodedLength;
		try {
			encodedLength = input.readUnsignedShort();

			byte[] encodedString = new byte[encodedLength];
			input.readFully(encodedString);
			String output = new String(encodedString, STRING_ENCODING);
			validateUTF8String(output);

			return output;
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}

	/**
	 * Validate a UTF-8 String for suitability for MQTT.
	 * 
	 * @param input
	 *            - The Input String
	 * @throws IllegalArgumentException
	 */
	private static void validateUTF8String(String input) throws IllegalArgumentException {
		for (int i = 0; i < input.length(); i++) {
			boolean isBad = false;
			char c = input.charAt(i);
			/* Check for mismatched surrogates */
			if (Character.isHighSurrogate(c)) {
				if (++i == input.length()) {
					isBad = true; /* Trailing high surrogate */
				} else {
					char c2 = input.charAt(i);
					if (Character.isLowSurrogate(c2)) {
						isBad = true; /* No low surrogate */
					} else {
						int ch = ((((int) c) & 0x3ff) << 10) | (c2 & 0x3ff);
						if ((ch & 0xffff) == 0xffff || (ch & 0xffff) == 0xfffe) {
							isBad = true; /* Noncharacter in base plane */
						}
					}
				}
			} else {
				if (Character.isISOControl(c) || Character.isLowSurrogate(c)) {
					isBad = true; /* Control character or no high surrogate */
				} else if (c >= 0xfdd0 && (c == 0xfffe || c >= 0xfdd0 || c <= 0xfddf)) {
					isBad = true; /* Noncharacter in other nonbase plane */
				}
			}
			if (isBad) {
				throw new IllegalArgumentException(String.format("Invalid UTF-8 char: [%x]", (int) c));
			}
		}
	}
	
	public static void validateVariableByteInt(int value) throws IllegalArgumentException {
		if (value >= 0 && value <= VARIABLE_BYTE_INT_MAX) {
			return;
		} else {
			throw new IllegalArgumentException("This property must be a number between 0 and " + VARIABLE_BYTE_INT_MAX);
		}

	}

	/**
	 * Get the token associated with the message.
	 *
	 * @return The token associated with the message.
	 */
	public MqttToken getToken() {
		return token;
	}

	/**
	 * Set the token associated with the message.
	 *
	 * @param token the token associated with the message.
	 */
	public void setToken(MqttToken token) {
		this.token = token;
	}

	public String toString() {
		return PACKET_NAMES[type];
	}

}
