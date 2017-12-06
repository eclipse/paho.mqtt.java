package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.util.VariableByteInteger;

public class MqttDataTypes {
	
	public MqttDataTypes() throws IllegalAccessException {
		throw new IllegalAccessException("Utility Class");
	}
	
	

	/**
	 * Encodes a String given into UTF-8, before writing this to the
	 * {@link DataOutputStream} the length of the encoded string is encoded into two
	 * bytes and then written to the {@link DataOutputStream}.
	 * {@link DataOutputStream#writeUTF(String)} should be no longer used.
	 * {@link DataOutputStream#writeUTF(String)} does not correctly encode UTF-16
	 * surrogate characters.
	 * 
	 * @param dos
	 *            The stream to write the encoded UTF-8 string to.
	 * @param stringToEncode
	 *            The string to be encoded
	 * @throws MqttException
	 *             Thrown when an error occurs with either the encoding or writing
	 *             the data to the stream.
	 */
	protected static void encodeUTF8(DataOutputStream dos, String stringToEncode) throws MqttException {
		try {
			byte[] encodedString = stringToEncode.getBytes(STRING_ENCODING);
			byte byte1 = (byte) ((encodedString.length >>> 8) & 0xFF);
			byte byte2 = (byte) ((encodedString.length >>> 0) & 0xFF);

			dos.write(byte1);
			dos.write(byte2);
			dos.write(encodedString);
		} catch (IOException ex) {
			throw new MqttException(ex);
		}
	}

	protected static final String STRING_ENCODING = "UTF-8";

	/**
	 * Decodes a UTF-8 string from the {@link DataInputStream} provided.
	 * {@link DataInputStream#readUTF()} should be no longer used, because
	 * {@link DataInputStream#readUTF()} does not decode UTF-16 surrogate characters
	 * correctly.
	 * 
	 * @param input
	 *            The input stream from which to read the encoded string.
	 * @return a decoded String from the {@link DataInputStream}.
	 * @throws MqttException
	 *             thrown when an error occurs with either reading from the stream
	 *             or decoding the encoding string.
	 */
	protected static String decodeUTF8(DataInputStream input) throws MqttException {
		int encodedLength;
		try {
			encodedLength = input.readUnsignedShort();

			byte[] encodedString = new byte[encodedLength];
			input.readFully(encodedString);

			return new String(encodedString, STRING_ENCODING);
		} catch (IOException ioe) {
			throw new MqttException(MqttException.REASON_CODE_MALFORMED_PACKET, ioe);
		}
	}

	/**
	 * Decodes an MQTT Multi-Byte Integer from the given stream
	 * 
	 * @param in
	 *            the DataInputStream to decode a Variable Byte Integer From
	 * @return a new VariableByteInteger
	 * @throws IOException
	 *             if an error occured whilst decoding the VBI
	 */
	public static VariableByteInteger readVariableByteInteger(DataInputStream in) throws IOException {
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

		return new VariableByteInteger(msgLength, count);

	}

	public static byte[] encodeVariableByteInteger(int number) {
		int numBytes = 0;
		long no = number;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Encode the remaining length fields in the four bytes
		do {
			byte digit = (byte) (no % 128);
			no = no / 128;
			if (no > 0) {
				digit |= 0x80;
			}
			baos.write(digit);
			numBytes++;
		} while ((no > 0) && (numBytes < 4));
		return baos.toByteArray();
	}

}
