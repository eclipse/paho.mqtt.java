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

import com.oracle.util.logging.LogRecord;
import com.oracle.util.logging.SimpleFormatter;
import com.oracle.util.logging.StreamHandler;

public class ConsoleHandler extends StreamHandler {

	  /**
	   * Constructs a ConsoleHandler object.
	   */
	  public ConsoleHandler() {
	    super(System.out, new SimpleFormatter());
	  }

	  /**
	   * Logs a record if necessary. A flush operation will be done.
	   * @param record
	   */
	  public void publish(LogRecord record) {
	    super.publish(record);
	    super.flush();
	  }
	  
	  public void close() {
	  // Do nothing (the default implementation would close the stream!)
	  }
	}
