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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;

/**
 * File helper
 * 
 * @author Bin Zhang
 */
public final class Files {
	// UTF-8: eight-bit UCS Transformation Format.
	private static final Charset UTF_8 = Charset.forName("UTF-8"); //$NON-NLS-1$

	/**
	 * Return jar file path
	 */
	public static File getJarPath() {
		return new File(Files.class.getProtectionDomain().getCodeSource().getLocation().getPath());
	}

	/**
	 * @param file
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object readObject(File file) throws IOException, ClassNotFoundException {
		ObjectInputStream in = null;
		Object object = null;
		try {
			in = new ObjectInputStream(new FileInputStream(file));
			object = in.readObject();
		}
		finally {
			try {
				if (in != null) {
					in.close();
				}
			}
			catch (IOException e) {
				// ignore
			}
		}
		return object;
	}

	/**
	 * @param object
	 * @param file
	 * @throws IOException
	 */
	public static void writeObject(Object object, File file) throws IOException {
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(object);
		}
		finally {
			try {
				if (out != null) {
					out.close();
				}
			}
			catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Try to read a file as string or read as hex if the data is binary
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static Content read(File file) throws IOException {
		try {
			return new Content(readText(file, UTF_8), false);
		}
		catch (MalformedInputException e) {
			return new Content(Strings.toHex(readBinary(file)), true);
		}
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	public static byte[] readBinary(File file) throws IOException {
		DataInputStream dis = null;
		try {
			byte[] bytes = new byte[(int) file.length()];
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			dis.readFully(bytes);
			return bytes;
		}
		finally {
			try {
				if (dis != null) {
					dis.close();
				}
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	public static void writeBinary(File file, byte[] bytes) throws IOException {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(bytes);
		}
		finally {
			try {
				if (bos != null) {
					bos.close();
				}
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * Try to read a file as string
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static String readText(File file) throws IOException {
		return readText(file, UTF_8);
	}

	/**
	 * @param file
	 * @param cs
	 * @throws IOException
	 */
	public static String readText(File file, Charset cs) throws IOException {
		BufferedReader reader = null;
		try {
			reader = newBufferedReader(file, cs);
			StringBuilder buf = new StringBuilder();
			for (;;) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				buf.append(line).append(newLine());
			}
			return buf.toString();
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * @param file
	 * @param cs
	 * @throws IOException
	 */
	private static BufferedReader newBufferedReader(File file, Charset cs) throws IOException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), cs.newDecoder()));
	}

	private static String newLine() {
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	/**
	 * Wrap of file content
	 */
	public static class Content {
		private final String data;
		private final boolean binary;

		/**
		 * @param data
		 * @param binary
		 */
		public Content(String data, boolean binary) {
			this.data = data;
			this.binary = binary;
		}

		public String getData() {
			return data;
		}

		public boolean isBinary() {
			return binary;
		}
	}

}
