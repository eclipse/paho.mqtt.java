package org.eclipse.paho.mqttv5.common;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.eclipse.paho.mqttv5.common.packet.MqttDataTypes;
import org.junit.Assert;
import org.junit.Test;

public class MqttDataTypesTest {
	
	
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
	
	

}
