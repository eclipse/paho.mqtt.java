package org.eclipse.paho.mqttv5.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.mqttv5.common.packet.MqttDataTypes;
import org.junit.Assert;
import org.junit.Test;

public class MqttDataTypesTest {
	
	public static void printBytesAsHex(byte[] byteArrayInput) {
		System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(byteArrayInput));
	}
	
	
	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeNegativeVBI(){
		// Attempt to encode a negative number
		MqttDataTypes.encodeVariableByteInteger(-1);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeOversizeVBI(){
		// Attempt to encode a negative number
		MqttDataTypes.encodeVariableByteInteger(268435456);
	}
	
	@Test
	public void TestEncodeAndDecodeVBI() throws IOException{
		int numberToEncode = 268435442;
		byte[] encodedNumber = MqttDataTypes.encodeVariableByteInteger(numberToEncode);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encodedNumber));
		int decodedVBI = MqttDataTypes.readVariableByteInteger(dis).getValue();
		Assert.assertEquals(numberToEncode, decodedVBI);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeInvalidUTF8String() throws MqttException {
		final char invalid = '\u0001';
		String invalidString = "" + invalid;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttDataTypes.encodeUTF8(dos, invalidString);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeInvalidUTF8StringInDifferentRange() throws MqttException {
		final char invalid = '\u008C';
		String invalidString = "" + invalid;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttDataTypes.encodeUTF8(dos, invalidString);
	}
	
	@Test
	public void TestEncodeAndDecodeUTF8String() throws MqttException {
		String testString = "Answer to life the universe and everything";
		// Encode The String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttDataTypes.encodeUTF8(dos, testString);
		// Decode the String
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		String decodedUTF8 = MqttDataTypes.decodeUTF8(input);
		Assert.assertEquals(testString, decodedUTF8);	
	}
	
	

}
