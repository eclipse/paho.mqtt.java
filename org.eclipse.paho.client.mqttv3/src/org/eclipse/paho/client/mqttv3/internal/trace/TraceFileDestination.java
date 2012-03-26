/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.trace;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class TraceFileDestination implements TraceDestination {

	private boolean enabled = false;
	
	private DataOutputStream dos;
	private ByteArrayOutputStream buffer;
	private FileOutputStream out;
	private File tracePropertiesFile;

	private File traceDirectory;
	private int fileCount;
	private int maxFileSize;
	
	private int fileIndex = 0;
	private int currentFileSize = 0;

	private Properties traceProperties;
	private long tracePropertiesUpdate = 0;
	
	/**
	 * System property "org.eclipse.paho.client.mqttv3.trace" defines the location of the trace
	 * properties files. Default: mqtt-trace.properties
	 * 
	 * org.eclipse.paho.client.mqttv3.trace.outputName
	 * 	directory to write the trace to. Default: current directory
	 * org.eclipse.paho.client.mqttv3.trace.count
	 * 	the number of trace files to write. Default: 1
	 * org.eclipse.paho.client.mqttv3.trace.limit
	 * 	maximum file size in bytes (only applies if count>1). Default: 5000000
	 * 
	 * org.eclipse.paho.client.mqttv3.trace.client.[client id].status
	 *  set trace status for a specific client
	 *  values: 'on' 'off'
	 * org.eclipse.paho.client.mqttv3.trace.client.*.status
	 *  set trace status for all clients. Setting to 'on' overrides individual clients set to 'off'
	 */
	public TraceFileDestination() {
		
		traceProperties = new Properties();

		String tracePropertiesFilename = System.getProperty("org.eclipse.paho.client.mqttv3.trace");
		if (tracePropertiesFilename == null) {
			tracePropertiesFile = new File(System.getProperty("user.dir"),"mqtt-trace.properties");
		} else {
			tracePropertiesFile = new File(tracePropertiesFilename);
		}
		updateTraceProperties();
	}

	/**
	 * Updates the trace properties from the trace properties file and
	 * initialises tracing appropriately.
	 * @return Whether trace is now enabled after the refresh. If the refresh
	 * and trace setup was successful then it will be enabled, otherwise it
	 * will have been disabled.
	 */
	private boolean updateTraceProperties() {

		//If the trace properties file was found
		if (tracePropertiesFile.exists()) {
		
			//If it has changed since the last read
			if (tracePropertiesFile.lastModified() != tracePropertiesUpdate) {
				
				//Try to load the trace properties from the file
				try {
					traceProperties.load(new FileInputStream(tracePropertiesFile));
					tracePropertiesUpdate = tracePropertiesFile.lastModified();
				}
				//Catch any problems - set trace to NOT enabled & return
				catch (Exception e) {
					traceProperties.clear();
					tracePropertiesUpdate = 0;
					enabled = false;
					return false;
				}
				
				/*
				 * Set the trace properties for this class.
				 */
				
				String directory = traceProperties.getProperty("org.eclipse.paho.client.mqttv3.trace.outputName", System.getProperty("user.dir"));
				traceDirectory = new File(directory);

				//If the trace directory does not already exist - then cannot trace
				//Note: this matches MQ default behaviour
				if (!traceDirectory.exists()) {
					traceProperties.clear();
					tracePropertiesUpdate = 0;
					enabled = false;
					return false;
				}
				
				fileCount = Integer.parseInt(traceProperties.getProperty("org.eclipse.paho.client.mqttv3.trace.count", "1"));
				maxFileSize = Integer.parseInt(traceProperties.getProperty("org.eclipse.paho.client.mqttv3.trace.limit", "5000000"));
				
				//Initialise the trace file and output stream
				initialiseFile();
				
				//If the output stream is still null - then error initialising trace file - cannot trace
				if(out == null) {
					traceProperties.clear();
					tracePropertiesUpdate = 0;
					enabled = false;
					return false;
				}
				else {
					buffer = new ByteArrayOutputStream();
					dos = new DataOutputStream(buffer);
					enabled = true;
					return true;
				}				
			}
			//Else file has NOT changed - no need to refresh anything - just return whether we are enabled from last time
			else {
				return enabled;
			}
		}
		//Else trace properties file NOT found - clear existing and set tracing to NOT enabled
		else {
			traceProperties.clear();
			tracePropertiesUpdate = 0;
			enabled = false;
			return false;
		}
	}
	
	public boolean isEnabled(String resource) {
		return enabled && ("on".equalsIgnoreCase(traceProperties.getProperty("org.eclipse.paho.client.mqttv3.trace.client.*.status")) ||
					"on".equalsIgnoreCase(traceProperties.getProperty("org.eclipse.paho.client.mqttv3.trace.client."+resource+".status")) );
	}
	
	public void initialiseFile() {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
			}
			out = null;
		}
		currentFileSize = 0;
		File traceFile = new File(traceDirectory,"mqtt-"+fileIndex+".trc");
		if (traceFile.exists()) {
			traceFile.delete();
		}
		try {
			out = new FileOutputStream(traceFile);
		} catch (FileNotFoundException e) {
			enabled = false;
			out = null;
		}

	}
	public synchronized void write(TracePoint point) {
		try {
			dos.writeShort(point.source);
			dos.writeLong(point.timestamp);
			byte meta = point.type;
			if (point.inserts != null && point.inserts.length > 0) {
				meta |= 0x20;
			}
			if (point.throwable != null) {
				meta |= 0x40;
			}
			dos.writeByte(meta);
			dos.writeShort(point.id);
			dos.writeUTF(point.threadName);
			if (point.inserts != null && point.inserts.length > 0) {
				dos.writeShort(point.inserts.length);
				for (int i=0;i<point.inserts.length;i++) {
					if (point.inserts[i]!=null) {
						dos.writeUTF(point.inserts[i].toString());
					} else {
						dos.writeUTF("null");
					}
				}
			}

			if (point.throwable != null) {
				StackTraceElement[] stack = point.throwable.getStackTrace();
				dos.writeShort(stack.length+1);
				dos.writeUTF(point.throwable.toString());
				for (int i=0;i<stack.length;i++) {
					dos.writeUTF(stack[i].toString());
				}
			}
			if (fileCount > 1 && currentFileSize + buffer.size() > maxFileSize) {
				// bump the file
				fileIndex++;
				if (fileIndex == fileCount) {
					fileIndex = 0;
				}
				initialiseFile();
			}
			if (out != null) {
				currentFileSize += buffer.size();
				buffer.writeTo(out);
				out.flush();
			}
			buffer.reset();
		} catch(Exception e) {
			enabled = false;
		}
	}

}
