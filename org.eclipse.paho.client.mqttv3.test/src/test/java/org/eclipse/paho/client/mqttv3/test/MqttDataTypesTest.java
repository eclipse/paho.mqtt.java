package org.eclipse.paho.client.mqttv3.test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.junit.Assert;
import org.junit.Test;

public class MqttDataTypesTest {


	public static void printBytesAsHex(byte[] byteArrayInput) {
		System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(byteArrayInput));
	}

	/**
	 * Utility Function to encode, then decode a UTF-8 String using the
	 * {@link MqttDataTypes#encodeUTF8} function.
	 * 
	 * @param testString
	 *            - The String to encode / decode
	 * @return - a Decoded UTF-8 string.
	 * @throws MqttException
	 *             if an error occurs whilst encoding or decoding the string.
	 */
	private static String encodeAndDecodeString(String testString) throws MqttException {
		// Encode The String
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttWireMessage.encodeUTF8(dos, testString);
		// Decode the String
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		String decodedUTF8 = MqttWireMessage.decodeUTF8(input);
		return decodedUTF8;
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
		encodeAndDecodeString(invalidString);
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeInvalidUTF8StringInDifferentRange() throws MqttException {
		final char invalid = '\u008C';
		String invalidString = "" + invalid;
		encodeAndDecodeString(invalidString);
	}

	@Test
	public void TestEncodeAndDecodeUTF8String() throws MqttException {
		String testString = "Answer to life the universe and everything";
		// System.out.println(String.format("'%s' is %d bytes, %d chars long",
		// testString, testString.getBytes().length, testString.length()));
		String decodedUTF8 = encodeAndDecodeString(testString);
		Assert.assertEquals(testString, decodedUTF8);
	}

	@Test
	public void TestEncodeAndDecodeChineseUTF8String() throws MqttException {
		String testString = "葛渚噓";
		// System.out.println(String.format("'%s' is %d bytes, %d chars long",
		// testString, testString.getBytes().length, testString.length()));
		String decodedUTF8 = encodeAndDecodeString(testString);
		Assert.assertEquals(testString, decodedUTF8);

	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeAndDecodeEmojiString() throws MqttException {
		String testString = "👁🐝Ⓜ️️";
		// System.out.println(String.format("'%s' is %d bytes, %d chars long",
		// testString, testString.getBytes().length, testString.length()));
		String decodedUTF8 = encodeAndDecodeString(testString);
		Assert.assertEquals(testString, decodedUTF8);

	}

	@Test
	public void TestEncodeAndDecodeComplexUTF8String() throws MqttException {
		String testString = "$shared/葛渚噓/GVTDurTopic02/葛渚噓";
		// System.out.println(String.format("'%s' is %d bytes, %d chars long",
		// testString, testString.getBytes().length, testString.length()));
		String decodedUTF8 = encodeAndDecodeString(testString);
		Assert.assertEquals(testString, decodedUTF8);

	}

	/**
	 * Tests that a large number of complex UTF-8 strings can be encoded and decoded
	 * successfully. Uses "i_can_eat_glass.txt" as a source of strings that are in
	 * the Language:testString format
	 * 
	 * @throws IOException
	 * @throws MqttException
	 */
	@Test
	public void testICanEatGlass() throws IOException, MqttException {
		ClassLoader classLoader = getClass().getClassLoader();
		String encodedFileName = classLoader.getResource("i_can_eat_glass.txt").getFile();
		String decodedFileName;
		try {
			decodedFileName = java.net.URLDecoder.decode( encodedFileName, StandardCharsets.UTF_8.name() );
		}
		catch ( Exception e ) {
			// can't decode the URL, passing on the encoded name hoping that it
			// actually contains something that exists in the filesystem with
			// that name.
			decodedFileName = encodedFileName;
		}
		File file = new File(decodedFileName);

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			for (String line; (line = br.readLine()) != null;) {
				if(!line.startsWith("#")) {
					String[] parts = line.split(":");
					Assert.assertEquals(2, parts.length);
					String decodedUTF8 = encodeAndDecodeString(parts[1]);
					System.out.println(String.format("Language: %s => [%s], %d chars, Decoded:  [%s]", parts[0], parts[1], parts[1].length(), decodedUTF8));
					Assert.assertEquals(parts[1], decodedUTF8);
				}
			}
		}
	}

}
