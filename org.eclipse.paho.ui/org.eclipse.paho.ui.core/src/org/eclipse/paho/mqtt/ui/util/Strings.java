/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
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
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

/**
 * String helper
 * 
 * @author Bin Zhang
 */
public final class Strings {
	// UTF-8: eight-bit UCS Transformation Format.
	private static final Charset UTF_8 = Charset.forName("UTF-8"); //$NON-NLS-1$
	// Represents a failed index search.
	private static final int INDEX_NOT_FOUND = -1;
	private static final char[] hexArray = "0123456789ABCDEF".toCharArray(); //$NON-NLS-1$

	/**
	 * Try to decode the bytes as UTF-8 string, and converted to HEX string if the bytes cannot be decoded, which
	 * probably means the bytes are binary data
	 * 
	 * @param bytes
	 */
	public static String of(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try {
			return UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
		}
		catch (CharacterCodingException e) {
			return toHex(bytes);
		}
	}

	/**
	 * Hex to string
	 * 
	 * @param hex
	 */
	public static String hexToString(String hex) {
		ByteBuffer buffer = ByteBuffer.allocate(hex.length() / 2);
		for (int i = 0; i < hex.length(); i += 2) {
			buffer.put((byte) Integer.parseInt(hex.substring(i, i + 2), 16));
		}
		buffer.rewind();
		return UTF_8.decode(buffer).toString();
	}

	/**
	 * String to hex
	 * 
	 * @param str
	 */
	public static String toHex(String str) {
		return toHex(str, UTF_8);
	}

	/**
	 * @param str
	 * @param charset
	 */
	public static String toHex(String str, Charset charset) {
		return toHex(str.getBytes(charset));
	}

	/**
	 * Bytes to hex
	 * 
	 * @param bytes
	 */
	public static String toHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * String to bytes
	 * 
	 * @param s
	 */
	public static byte[] toUFT8(String s) {
		if (s == null) {
			return null;
		}
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(byteOut);
		try {
			dos.writeUTF(s);
			dos.flush();
		}
		catch (IOException e) {
			// SHOULD never happen
			return null;
		}
		return byteOut.toByteArray();
	}

	/**
	 * @param cs
	 * @param first
	 * @param rest
	 * @return true if equals any
	 */
	public static boolean equalsAny(CharSequence cs, CharSequence first, CharSequence... rest) {
		boolean eq = false;
		if (cs == null) {
			eq = first == null;
		}
		else {
			eq = cs.equals(first);
		}

		if (rest != null) {
			for (CharSequence str : rest) {
				eq = eq || str.equals(cs);
			}
		}

		return eq;
	}

	/**
	 * Checks if the CharSequence contains any character in the given set of characters.
	 * 
	 * @param cs the CharSequence to check, may be null
	 * @param searchChars the chars to search for, may be null
	 * @return the {@code true} if any of the chars are found, {@code false} if no match or null input
	 */
	public static boolean containsAny(CharSequence cs, CharSequence searchChars) {
		if (searchChars == null) {
			return false;
		}
		return containsAny(cs, toCharArray(searchChars));
	}

	/**
	 * Checks if the CharSequence contains any character in the given set of characters.
	 * 
	 * @param cs the CharSequence to check, may be null
	 * @param searchChars the chars to search for, may be null
	 * @return the {@code true} if any of the chars are found, {@code false} if no match or null input
	 */
	public static boolean containsAny(CharSequence cs, char... searchChars) {
		if (isEmpty(cs) || isEmpty(searchChars)) {
			return false;
		}
		int csLength = cs.length();
		int searchLength = searchChars.length;
		int csLast = csLength - 1;
		int searchLast = searchLength - 1;
		for (int i = 0; i < csLength; i++) {
			char ch = cs.charAt(i);
			for (int j = 0; j < searchLength; j++) {
				if (searchChars[j] == ch) {
					if (Character.isHighSurrogate(ch)) {
						if (j == searchLast) {
							// missing low surrogate, fine, like String.indexOf(String)
							return true;
						}
						if (i < csLast && searchChars[j + 1] == cs.charAt(i + 1)) {
							return true;
						}
					}
					else {
						// ch is in the Basic Multilingual Plane
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if a CharSequence is empty ("") or null.
	 * 
	 * 
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is empty or null
	 */
	public static boolean isEmpty(CharSequence cs) {
		return cs == null || cs.length() == 0;
	}

	/**
	 * @param array
	 */
	private static boolean isEmpty(char[] array) {
		return array == null || array.length == 0;
	}

	/**
	 * Green implementation of toCharArray.
	 * 
	 * @param cs the {@code CharSequence} to be processed
	 * @return the resulting char array
	 */
	private static char[] toCharArray(CharSequence cs) {
		if (cs instanceof String) {
			return ((String) cs).toCharArray();
		}
		else {
			int sz = cs.length();
			char[] array = new char[cs.length()];
			for (int i = 0; i < sz; i++) {
				array[i] = cs.charAt(i);
			}
			return array;
		}
	}

	/**
	 * Counts how many times the substring appears in the larger string.
	 * 
	 * @param str the CharSequence to check, may be null
	 * @param sub the substring to count, may be null
	 * @return the number of occurrences, 0 if either CharSequence is {@code null}
	 */
	public static int countMatches(CharSequence str, CharSequence sub) {
		if (isEmpty(str) || isEmpty(sub)) {
			return 0;
		}
		int count = 0;
		int idx = 0;
		while ((idx = indexOf(str, sub, idx)) != INDEX_NOT_FOUND) {
			count++;
			idx += sub.length();
		}
		return count;
	}

	/**
	 * Used by the indexOf(CharSequence methods) as a green implementation of indexOf.
	 * 
	 * @param cs the {@code CharSequence} to be processed
	 * @param searchChar the {@code CharSequence} to be searched for
	 * @param start the start index
	 * @return the index where the search sequence was found
	 */
	static int indexOf(CharSequence cs, CharSequence searchChar, int start) {
		return cs.toString().indexOf(searchChar.toString(), start);
	}

	private Strings() {
		// prevented from constructing objects
	}

}
