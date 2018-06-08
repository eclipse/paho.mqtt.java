/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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
 */
package org.eclipse.paho.client.mqttv3.util;

/**
 * String helper
 */
public final class Strings {
	// Represents a failed index search.
	private static final int INDEX_NOT_FOUND = -1;

	/**
	 * Checks if the CharSequence equals any character in the given set of characters.
	 * 
	 * @param cs the CharSequence to check
	 * @param strs the set of characters to check against
	 * @return true if equals any
	 */
	public static boolean equalsAny(CharSequence cs, CharSequence[] strs) {
		boolean eq = false;
		if (cs == null) {
			eq = strs == null;
		}

		if (strs != null) {
			for (int i = 0; i < strs.length; i++) {
				eq = eq || strs[i].equals(cs);
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
	public static boolean containsAny(CharSequence cs, char[] searchChars) {
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
	private static int indexOf(CharSequence cs, CharSequence searchChar, int start) {
		return cs.toString().indexOf(searchChar.toString(), start);
	}

	private Strings() {
		// prevented from constructing objects
	}

}
