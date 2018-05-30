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
package org.eclipse.paho.client.mqttv3.util;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import org.eclipse.paho.client.mqttv3.MqttException;
import com.oracle.util.logging.Handler;
import com.oracle.util.logging.LogRecord;

/**
 * Simple file logging, designed to write to a rotating set of log
 * file of the format paho-%. File size limit and the number of files
 * to write to can be configured and the directory where the files are
 * created must be specified.
 *
 */
public class FileHandler extends Handler {
	
	private final static int DEFAULT_SIZE = 200000;
	private final static int DEFAULT_LIMIT = 3;
	private FileConnection curLogDir = null;
	private int count = 0;
    private int noOfFiles;
    private int sizeLimit;
    private String directory;
    private DataOutputStream fos;

	/**
	 * Constructs a FileHandler object.
	 * @param Size the size of each log file
	 * @param Limit the number of files to write to before rotating
	 * @param Directory the directory to write the log files to
	 */
    public FileHandler(int size, int limit, String directory) throws MqttException {
    	sizeLimit = (size <= 0) ? DEFAULT_SIZE : size;
    	noOfFiles = (limit <= 0) ? DEFAULT_LIMIT : limit;
    	if (directory == null || directory.equals(""))
    		throw new MqttException(MqttException.REASON_CODE_UNABLE_TO_WRITE);
    	this.directory = directory;
    	
    	try {
    		validateLog();
    	} catch (Exception e) {
    		throw new MqttException(MqttException.REASON_CODE_UNABLE_TO_WRITE);
    	}
    }

    public void validateLog() throws IOException, MqttException {
    	
    	if (count > noOfFiles-1) {
			count = 0; // Wrap back to the first trace file
		}
    	
    	// Create file.
		curLogDir = (FileConnection)Connector.open(directory + "paho- " + count + ".log");
		count++;

		// Create the directory if it doesn't already exist
		if (!curLogDir.exists()) {
			curLogDir.create();
		} else {
			curLogDir.delete(); // delete a wrapped file.
			curLogDir.create();
		}
		
		// Throw an exception if we can't write to file
		if (!curLogDir.canWrite()) {
			throw new MqttException(MqttException.REASON_CODE_UNABLE_TO_WRITE);
		}
		
		// Set the inputStream:
		if (fos != null) fos.close(); // close the old stream
		fos = curLogDir.openDataOutputStream(); // open the new stream
		
    }
    
	public void publish(LogRecord record) {
		if (isLoggable(record)) {
			try {
				String msg;
				msg = getFormatter().format(record);
				int length = msg.getBytes().length;
				
				// If we can write the whole of the record to the file then do so.
				if (curLogDir.fileSize() + length > sizeLimit) {
					validateLog(); // Move to the next log directory
				}
				
				fos.write(msg.getBytes());
				fos.flush();
				
			} catch (IOException e) {
			} catch (MqttException ex) {
			}
		}
	}

	public void close() {
		try {
			curLogDir.close();
		} catch (IOException e) {
			//
		}
	}

	public void flush() {
		// TODO Auto-generated method stub
		
	}
	
}
