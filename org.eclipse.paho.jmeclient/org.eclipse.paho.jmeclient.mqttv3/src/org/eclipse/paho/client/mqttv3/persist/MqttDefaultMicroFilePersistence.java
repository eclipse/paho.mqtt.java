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
 */
package org.eclipse.paho.client.mqttv3.persist;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData;

public class MqttDefaultMicroFilePersistence implements MqttClientPersistence {

	// TODO: Implement File Locking
	// TODO: Come up with a better validChars option
	
	private FileConnection dataDir = null;
	private FileConnection clientDir = null;
	private DataOutputStream out;
	private static final char[] validChars = new char[]{'£','$','0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','_','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'}; 
	private final static String FILESEPERATOR = "/";
	private static final String MESSAGE_FILE_EXTENSION = ".msg";
	private static final String MESSAGE_BACKUP_FILE_EXTENSION = ".bup";
	private static final String LOCK_FILENAME = ".lck"; 
	
	public MqttDefaultMicroFilePersistence() throws IOException {
		this(System.getProperty("fileconn.dir.photos") + "temp/"); // TODO: Is this valid, it represents the MIDLETs own private working directory
	}
	
	public MqttDefaultMicroFilePersistence(String directory) throws IOException { //throws MqttPersistenceException {
		dataDir = (FileConnection)Connector.open(directory); // This must be fully qualified
	}
	
	public void open(String clientId, String theConnection)
			throws MqttPersistenceException {
		if (dataDir.exists() && !dataDir.isDirectory()) {
			throw new MqttPersistenceException();
		} else if (!dataDir.exists() ) {
			try {
				dataDir.mkdir();
			} catch(Exception e) {
				throw new MqttPersistenceException();
			}
		}
		
		if (!dataDir.canWrite()) {
			throw new MqttPersistenceException();
		}
		
		StringBuffer keyBuffer = new StringBuffer();
		for (int i=0;i<clientId.length();i++) {
			char c = clientId.charAt(i);
			if (isSafeChar(c)) {
				keyBuffer.append(c);
			}
		}

		keyBuffer.append("-");
		for (int i=0;i<theConnection.length();i++) {
			char c = theConnection.charAt(i);
			if (isSafeChar(c)) {
				keyBuffer.append(c);
			}
		}
		String key = keyBuffer.toString();

		try {
			clientDir = (FileConnection) Connector.open(dataDir.getURL() + key);
		} catch (IOException e1) {
			throw new MqttPersistenceException();
		}

		if (!clientDir.exists()) {
			try {
				clientDir.mkdir();
			} catch (IOException e) {
				throw new MqttPersistenceException();
			}
		}
	
		try {
			// TODO: Implement File Locking
		} catch (Exception e) {
			throw new MqttPersistenceException(MqttPersistenceException.REASON_CODE_PERSISTENCE_IN_USE);
		}

		// Scan the directory for .backup files. These will
		// still exist if the JVM exited during addMessage, before
		// the new message was written to disk and the backup removed.
		restoreBackups(clientDir);
		
	}
	
	/**
	 * Checks whether the persistence has been opened.
	 * @throws MqttPersistenceException if the persistence has not been opened.
	 */
	private void checkIsOpen() throws MqttPersistenceException {
		if (clientDir == null) {
			throw new MqttPersistenceException();
		}
	}
	
	public void close() throws MqttPersistenceException {
//		checkIsOpen();
		//if (fileLock != null) {
		//	fileLock.release();
		//}

		if (!getFiles().hasMoreElements()) {
			try {
				clientDir.delete();
			} catch (IOException e) {
				throw new MqttPersistenceException();
			}
		}
		clientDir = null;
	}
	
	private Enumeration getFiles() throws MqttPersistenceException {
		checkIsOpen();
		Enumeration files;
		try {
			files = clientDir.list("*" + MESSAGE_FILE_EXTENSION, false);
		} catch (IOException e) {
			throw new MqttPersistenceException();
		}
		if (files == null) {
			throw new MqttPersistenceException();
		}
		return files;
	}
	
	public void put(String key, MqttPersistable message) throws MqttPersistenceException {
		
		checkIsOpen();
		FileConnection file;

		try {

			file = (FileConnection) Connector.open(clientDir.getURL() + key + MESSAGE_FILE_EXTENSION);

			FileConnection backupFile = (FileConnection) Connector.open(clientDir.getURL() + key + MESSAGE_FILE_EXTENSION + MESSAGE_BACKUP_FILE_EXTENSION);

			if (file.exists()) {
				// Backup the existing file so the overwrite can be rolled-back

				try {
					file.rename(backupFile.getName());
				} catch (Exception e) {
					backupFile.delete();
					file.rename(backupFile.getName());
				}

			}

			try {
				if (!file.exists()) {
					file.create();
				}
				DataOutputStream fos = file.openDataOutputStream();
				fos.write(message.getHeaderBytes(), message.getHeaderOffset(), message.getHeaderLength());
				if (message.getPayloadBytes() != null) {
					fos.write(message.getPayloadBytes(),
							message.getPayloadOffset(),
							message.getPayloadLength());
				}
				fos.flush();
				fos.close();
				if (backupFile.exists()) {
					// The write has completed successfully, delete the backup
					backupFile.delete();
				}
			} catch (IOException ex) {
				throw new MqttPersistenceException(ex);
			} finally {
				if (backupFile.exists()) {
					// The write has failed - restore the backup
					try {
						backupFile.rename(file.getName());
					} catch (Exception e) {
						file.delete();
						backupFile.rename(file.getName());
					}
				}
			}
		} catch (IOException ex) {
			throw new MqttPersistenceException(ex);
		}
		
		
	}
	
	public MqttPersistable get(String key) throws MqttPersistenceException {
		checkIsOpen();
		MqttPersistable result;
		try {
			FileConnection file = (FileConnection) Connector.open(clientDir.getURL() + key + MESSAGE_FILE_EXTENSION);
			DataInputStream fis = file.openDataInputStream();
			int size = fis.available();
			byte[] data = new byte[size];
			int read = 0;
			while (read<size) {
				read += fis.read(data,read,size-read);
			}
			fis.close();
			result = new MqttPersistentData(key, data, 0, data.length, null, 0, 0);
		} 
		catch(IOException ex) {
			throw new MqttPersistenceException(ex);
		}
		return result;
	}
	
	/**
	 * Deletes the data with the specified key from the previously specified persistence directory.
	 */
	public void remove(String key) throws MqttPersistenceException {
		checkIsOpen();
		
		try {
		FileConnection file = (FileConnection) Connector.open(clientDir.getURL() + key + MESSAGE_FILE_EXTENSION);;
		if (file.exists()) {
			file.delete();
		}
		} catch(IOException ex) {
			throw new MqttPersistenceException(ex);
		}
	}
	
	/**
	 * Returns all of the persistent data from the previously specified persistence directory.
	 * @return all of the persistent data from the persistence directory.
	 * @throws MqttPersistenceException
	 */
	public Enumeration keys() throws MqttPersistenceException {
		checkIsOpen();
		Enumeration files = getFiles();
		Vector result = new Vector();
		while (files.hasMoreElements()) {
			String filename = (String) files.nextElement();
			String key = filename.substring(0,filename.length()-MESSAGE_FILE_EXTENSION.length());
			result.addElement(key);
		}
		return result.elements();
	}
	
	
	private void restoreBackups(FileConnection dir) throws MqttPersistenceException {
		Enumeration files = null;
		try {
			files = dir.list("*" + MESSAGE_BACKUP_FILE_EXTENSION, false);
		} catch (IOException e1) {
			throw new MqttPersistenceException();
		}

		if (files == null) {
			throw new MqttPersistenceException();
		}

		while (files.hasMoreElements()) {
			try {
				FileConnection file = (FileConnection) Connector.open(((FileConnection)files.nextElement()).getURL());
				String fileName = file.getName();
				FileConnection originalFile = (FileConnection) Connector.open(dir.getURL() + FILESEPERATOR + fileName.substring(0, fileName.length()-MESSAGE_BACKUP_FILE_EXTENSION.length()));
				
				try {
					file.rename(originalFile.getName());
				} catch (Exception e) {
					originalFile.delete();
					file.rename(originalFile.getName());
				}
			} catch (IOException e1) {
				throw new MqttPersistenceException();
			}
		}
	}

	private boolean isSafeChar(char c) {
		for (int i=0; i<validChars.length; i++) {
			if (validChars[i] == c) return true;
		}
		return false;
	}


	public boolean containsKey(String key) throws MqttPersistenceException {
		checkIsOpen();
		try {
			FileConnection file =(FileConnection) Connector.open(clientDir.getURL() + key + MESSAGE_FILE_EXTENSION);
			return file.exists();
		} catch(IOException ex) {
			throw new MqttPersistenceException(ex);
		}
	}

	public void clear() throws MqttPersistenceException {
		checkIsOpen();
		Enumeration files = getFiles();
		while (files.hasMoreElements()) {
			try {
				FileConnection next = (FileConnection) Connector.open(clientDir.getURL()+files.nextElement());
				next.delete();
			} catch(IOException ex) {
				throw new MqttPersistenceException(ex);
			}
		}
	}

}
