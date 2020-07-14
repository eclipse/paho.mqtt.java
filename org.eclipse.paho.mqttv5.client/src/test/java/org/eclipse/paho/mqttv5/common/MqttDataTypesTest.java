package org.eclipse.paho.mqttv5.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.eclipse.paho.mqttv5.common.packet.MqttDataTypes;
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
		MqttDataTypes.encodeUTF8(dos, testString);
		// Decode the String
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
		String decodedUTF8 = MqttDataTypes.decodeUTF8(input);
		return decodedUTF8;
	}
	

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeNegativeVBI() {
		// Attempt to encode a negative number
		MqttDataTypes.encodeVariableByteInteger(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeOversizeVBI() {
		// Attempt to encode a value which is too big
		MqttDataTypes.encodeVariableByteInteger(268435456);
	}

	@Test
	public void TestEncodeAndDecodeVBI() throws IOException {
		int numberToEncode = 268435442;
		byte[] encodedNumber = MqttDataTypes.encodeVariableByteInteger(numberToEncode);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encodedNumber));
		int decodedVBI = MqttDataTypes.readVariableByteInteger(dis).getValue();
		Assert.assertEquals(numberToEncode, decodedVBI);
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeInvalidUTF8String() throws MqttException {
		String invalidString = "abc\u0001def";
		encodeAndDecodeString(invalidString);
	}

	@Test(expected = IllegalArgumentException.class)
	public void TestEncodeInvalidUTF8StringInDifferentRange() throws MqttException {
		String invalidString = "a\u008Cd";
		encodeAndDecodeString(invalidString);
	}
	
	@Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringInDifferentRange2() throws MqttException {
	    /* ControlChar U+007F , smallest of the C1 range */
        String invalidString = "abc\u007F";
        encodeAndDecodeString(invalidString);
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringZero() throws MqttException {
        /* ControlChar U+007F , smallest of the C1 range */
        String invalidString = "abc\u0000";
        encodeAndDecodeString(invalidString);
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringNonChar1() throws MqttException {
	    /* Nonchar U+FDD0 */
        String invalidString = "\uFDD0";
        encodeAndDecodeString(invalidString);
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringNonChar2() throws MqttException {
	    /* Nonchar U+FDDF */
        String invalidString = "\uFDDF";
        encodeAndDecodeString(invalidString);
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringNonChar3() throws MqttException {
	    /* Nonchar *+FFFE */
        String invalidString = "\uFFFE";
        encodeAndDecodeString(invalidString);
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringNonChar4() throws MqttException {
	    /* Nonchar U+FFFF */
        String invalidString = "\uFFFF";
        encodeAndDecodeString(invalidString);
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringNonChar5() throws MqttException {
	    /* Nonchar U+1FFFE */
        String invalidString = "\uD83F\uDFFE";
        encodeAndDecodeString(invalidString);
    }
	
	   
    @Test(expected = IllegalArgumentException.class)
    public void TestEncodeInvalidUTF8StringNonChar6() throws MqttException {
        /* Nonchar U+1FFFF */
        String invalidString = "\uD83F\uDFFF";
        encodeAndDecodeString(invalidString);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void TestEncodeMismatchSurrogates1() throws MqttException {
        String invalidString = "abc\uD869\uD869";   /* Two high surrogates */
        encodeAndDecodeString(invalidString);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void TestEncodeMismatchSurrogates2() throws MqttException {
        String invalidString = "abc\uD869";   /* trailing high surrogates */
        encodeAndDecodeString(invalidString);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void TestEncodeMismatchSurrogates3() throws MqttException {
        String invalidString = "abc\uDFFF";   /* low surrogate */
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

	@Test
	public void TestEncodeAndDecodeEmojiString() throws MqttException {
		String testString = "👁🐝Ⓜ️️";
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
	
	@Test
    public void TestEncodeAndDecodeComplexUTF8String2() throws MqttException {
        String testString = "\u2000\u00d6\u2600\u00E0\u0444\uFF5E\uFF7B\uEE72\uD869\uDeD6";
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
		File file = new File(classLoader.getResource("i_can_eat_glass.txt").getFile());

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
