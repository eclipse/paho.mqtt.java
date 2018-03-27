package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.util.VariableByteInteger;

public class MqttDataTypes {

	private static final int TWO_BYTE_INT_MAX = 65535;
	private static final long FOUR_BYTE_INT_MAX = 4294967295L;
	private static final int VARIABLE_BYTE_INT_MAX = 268435455;

	public MqttDataTypes() throws IllegalAccessException {
		throw new IllegalAccessException("Utility Class");
	}

	public static void validateTwoByteInt(Integer value) throws IllegalArgumentException {
		if(value == null) {
			return;
		}
		if (value >= 0 && value <= TWO_BYTE_INT_MAX) {
			return;
		} else {
			throw new IllegalArgumentException("This property must be a number between 0 and " + TWO_BYTE_INT_MAX);
		}
	}

	public static void validateFourByteInt(Long value) throws IllegalArgumentException {
		if(value == null) {
			return;
		}
		if (value >= 0 && value <= FOUR_BYTE_INT_MAX) {
			return;
		} else {
			throw new IllegalArgumentException("This property must be a number between 0 and " + FOUR_BYTE_INT_MAX);
		}
	}

	public static void validateVariableByteInt(int value) throws IllegalArgumentException {
		if (value >= 0 && value <= VARIABLE_BYTE_INT_MAX) {
			return;
		} else {
			throw new IllegalArgumentException("This property must be a number between 0 and " + VARIABLE_BYTE_INT_MAX);
		}

	}

	public static void writeUnsignedFourByteInt(long value, DataOutputStream stream) throws IOException {
		stream.writeByte((byte) (value >>> 24));
		stream.writeByte((byte) (value >>> 16));
		stream.writeByte((byte) (value >>> 8));
		stream.writeByte((byte) (value >>> 0));
	}

	/**
	 * Reads a Four Byte Integer, then converts it to a float. This is because Java
	 * doesn't have Unsigned Integers.
	 * 
	 * @param inputStream
	 *            The input stream to read from.
	 * @return a {@link Long} containing the value of the Four Byte int (Between 0
	 *         and 4294967295)
	 * @throws IOException
	 *             if an exception occurs whilst reading from the Input Stream
	 */
	public static Long readUnsignedFourByteInt(DataInputStream inputStream) throws IOException {
		byte readBuffer[] = { 0, 0, 0, 0, 0, 0, 0, 0 };
		inputStream.readFully(readBuffer, 4, 4);
		return (((long) readBuffer[0] << 56) + ((long) (readBuffer[1] & 255) << 48)
				+ ((long) (readBuffer[2] & 255) << 40) + ((long) (readBuffer[3] & 255) << 32)
				+ ((long) (readBuffer[4] & 255) << 24) + ((readBuffer[5] & 255) << 16) + ((readBuffer[6] & 255) << 8)
				+ ((readBuffer[7] & 255) << 0));
	}

	/**
	 * Reads a Two Byte Integer, this is because Java does not have unsigned
	 * integers.
	 * 
	 * @param inputStream
	 *            The input stream to read from.
	 * @return a {@link int} containing the value of the Two Byte int (Between 0 and
	 *         65535)
	 * @throws IOException
	 *             if an exception occurs whilst reading from the Input Stream
	 * 
	 */
	public static int readUnsignedTwoByteInt(DataInputStream inputStream) throws IOException {
		// byte readBuffer[] = {0,0}
		int ch1 = inputStream.read();
		int ch2 = inputStream.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return (int) ((ch1 << 8) + (ch2 << 0));
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
	public static void encodeUTF8(DataOutputStream dos, String stringToEncode) throws MqttException {
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
	public static String decodeUTF8(DataInputStream input) throws MqttException {
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
