package org.eclipse.paho.mqttv5.common;

public class Validators {

	public boolean validateClientId(String clientId) {
		// Count characters, surrogate pairs count as one character.
		int clientIdLength = 0;
		for (int i = 0; i < clientId.length() - 1; i++) {
			if (isCharacterHighSurrogate(clientId.charAt(i))) {
				i++;
			}
			clientIdLength++;
		}	
		return clientIdLength < 65535;
	}

	/**
	 * @param ch
	 *            the character to check.
	 * @return returns 'true' if the character is a high-surrogate code unit
	 */
	protected static boolean isCharacterHighSurrogate(char ch) {
		final char minHighSurrogate = '\uD800';
		final char maxHighSurrogate = '\uDBFF';
		return (ch >= minHighSurrogate) && (ch <= maxHighSurrogate);
	}

}
