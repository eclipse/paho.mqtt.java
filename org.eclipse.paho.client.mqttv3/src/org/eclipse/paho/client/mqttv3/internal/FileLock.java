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
package org.eclipse.paho.client.mqttv3.internal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;

public class FileLock {
	private RandomAccessFile file;
	private Object fileLock;
	
	/**
	 * Creates an NIO FileLock on the specified file.
	 * @param lockFile the file to lock
	 * @throws Exception if the lock could not be obtained for any reason
	 */
	public FileLock(File lockFile) throws Exception {
		if (ExceptionHelper.isClassAvailable("java.nio.channels.FileLock")) {
			try {
				this.file = new RandomAccessFile(lockFile,"rw");
				Method m = file.getClass().getMethod("getChannel",new Class[]{});
				Object channel = m.invoke(file,new Object[]{});
				m = channel.getClass().getMethod("tryLock",new Class[]{});
				this.fileLock = m.invoke(channel, new Object[]{});
				file.close();
			} catch(NoSuchMethodException nsme) {
				this.fileLock = null;
			} catch(IllegalArgumentException iae) {
				this.fileLock = null;
			} catch(IllegalAccessException iae) {
				this.fileLock = null;
			}
		}
	}
	
	/**
	 * Releases the lock.
	 */
	public void release() {
		try {
			if (fileLock != null) {
				Method m = fileLock.getClass().getMethod("release",new Class[]{});
				m.invoke(fileLock, new Object[]{});
			}
		} catch (Exception e) {
			// Ignore exceptions
		}
		if (file != null) {
			try {
				file.close();
			} catch (IOException e) {
			}
		}
	}
	
}
