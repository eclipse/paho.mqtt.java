package org.eclipse.paho.client.mqttv3.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttDataTypesTest {

	public static void printBytesAsHex(byte[] byteArrayInput) {
		System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(byteArrayInput));
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeNegativeVBI() {
		// Attempt to encode a negative number
		MqttWireMessage.encodeMBI(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeOversizeVBI() {
		// Attempt to encode a negative number
		MqttWireMessage.encodeMBI(268435456);
	}

	@Test
	public void TestEncodeAndDecodeVBI() throws IOException {
		int numberToEncode = 268435442;
		byte[] encodedNumber = MqttWireMessage.encodeMBI(numberToEncode);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encodedNumber));
		int decodedVBI = MqttWireMessage.readMBI(dis).getValue();
		Assert.assertEquals(numberToEncode, decodedVBI);
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeInvalidUTF8String() throws MqttException {
		final char invalid = '\u0001';
		String invalidString = "" + invalid;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttWireMessage.encodeUTF8(dos, invalidString);
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeInvalidUTF8StringInDifferentRange() throws MqttException {
		final char invalid = '\u008C';
		String invalidString = "" + invalid;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttWireMessage.encodeUTF8(dos, invalidString);
	}

	@Test
	public void TestEncodeAndDecodeUTF8String() throws MqttException {
		String testString = "Answer to life the universe and everything";
		System.out.println(String.format("'%s' is %d bytes long", testString, testString.getBytes().length));
		// Encode The String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttWireMessage.encodeUTF8(dos, testString);
		// Decode the String
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		String decodedUTF8 = MqttWireMessage.decodeUTF8(input);
		Assert.assertEquals(testString, decodedUTF8);
	}

	@Test
	public void TestEncodeAndDecodeChineseUTF8String() throws MqttException {
		String testString = "葛渚噓";
		System.out.println(String.format("'%s' is %d bytes long", testString, testString.getBytes().length));
		// Encode The String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttWireMessage.encodeUTF8(dos, testString);
		// Decode the String
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		String decodedUTF8 = MqttWireMessage.decodeUTF8(input);
		Assert.assertEquals(testString, decodedUTF8);

	}
	
	@Test
	public void TestEncodeAndDecodeEmojiString() throws MqttException {
		String testString = "👁🐝Ⓜ️️";
		System.out.println(String.format("'%s' is %d bytes long", testString, testString.getBytes().length));
		// Encode The String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttWireMessage.encodeUTF8(dos, testString);
		// Decode the String
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		String decodedUTF8 = MqttWireMessage.decodeUTF8(input);
		Assert.assertEquals(testString, decodedUTF8);

	}
	
	@Test
	public void TestEncodeAndDecodeComplexUTF8String() throws MqttException {
		String testString = "$shared/葛渚噓/GVTDurTopic02/葛渚噓";
		System.out.println(String.format("'%s' is %d bytes long", testString, testString.getBytes().length));
		// Encode The String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttWireMessage.encodeUTF8(dos, testString);
		// Decode the String
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		String decodedUTF8 = MqttWireMessage.decodeUTF8(input);
		Assert.assertEquals(testString, decodedUTF8);

	}

}
