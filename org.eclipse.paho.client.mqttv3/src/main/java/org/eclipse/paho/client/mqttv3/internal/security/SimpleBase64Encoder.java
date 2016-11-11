/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.security;

public class SimpleBase64Encoder {

	// if this string is changed, then the decode method must also be adapted.
	private static final String PWDCHARS_STRING = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final char[] PWDCHARS_ARRAY = PWDCHARS_STRING.toCharArray();

	/**
	 * Encodes an array of byte into a string of printable ASCII characters
	 * using a base-64 encoding.
	 * @param bytes The array of bytes to e encoded
	 * @return The encoded array.
	 */
	public static String encode(byte[] bytes) {
		// Allocate a string buffer.
		int len = bytes.length;
		final StringBuffer encoded = new StringBuffer((len+2)/3*4);		
		int i=0;
		int j=len;
		while(j>=3){
			encoded.append(to64((((bytes[i] & 0xff) << 16)
				| (int) ((bytes[i+1] & 0xff) << 8) | (int) (bytes[i+2] & 0xff)),4));
			i+=3;
			j-=3;
		}
		// j==2 | j==1 | j==0
		if(j==2) {
			// there is a rest of 2 bytes. This encodes into 3 chars.
			encoded.append(to64(((bytes[i] &0xff)<<8) | ((bytes[i+1] & 0xff)),3));
		}
		if(j==1) {
			// there is a rest of 1 byte. This encodes into 1 char.
			encoded.append(to64(((bytes[i] & 0xff)),2));
		}
		return encoded.toString();
	}

	public static byte[] decode(String string) {
		byte[] encoded=string.getBytes();
		int len=encoded.length;
		byte[] decoded=new byte[len*3/4];
		int i=0;
		int j=len;
		int k=0;
		while(j>=4) {
			long d=from64(encoded, i, 4);
			j-=4;
			i+=4;
			for(int l=2;l>=0;l--) {
				decoded[k+l]=(byte) (d & 0xff);
				d=d >>8;
			}
			k+=3;
		}
		// j==3 | j==2 
		if(j==3) {
			long d=from64(encoded, i, 3);
			for(int l=1;l>=0;l--) {
				decoded[k+l]=(byte) (d & 0xff);
				d=d >>8;
			}			
		}
		if(j==2) {
			long d=from64(encoded, i, 2);
			decoded[k]=(byte) (d & 0xff);
		}			
		return decoded;
	}

	/* the core conding routine. Translates an input integer into
	 * a string of the given length.*/
	private final static String to64(long input, int size) {
		final StringBuffer result = new StringBuffer(size);
		while (size > 0) {
			size--;
			result.append(PWDCHARS_ARRAY[((int) (input & 0x3f))]);
			input = input >> 6;
		}
		return result.toString();
	}

	/*
	 * The reverse operation of to64
	 */
	private final static long from64(byte[] encoded, int idx, int size) {
		long res=0;
		int f=0;
		while(size>0) {
			size--;
			long r=0;
			// convert encoded[idx] back into a 6-bit value.
			byte d=encoded[idx++];
			if(d=='/') {
				r=1;
			}
			if(d>='0' && d<='9') {
				r=2+d-'0';
			}
			if(d>='A' && d<='Z') {
				r=12+d-'A';
			}
			if(d>='a' && d<='z') {
				r=38+d-'a';
			}
			res=res+((long)r << f);
			f+=6;
		}
		return res;
	}

}
