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

package org.eclipse.paho.client.mqttv3.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * SimpleLogFormatter prints a single line 
 * log record in human readable form.
 */
public class SimpleLogFormatter extends Formatter {
	
	private static final String LS = System.getProperty("line.separator");
	/**
	 * Constructs a <code>SimpleFormatter</code> object.
	 */
	public SimpleLogFormatter() {
		super();
	}

	/**
	 * Format the logrecord as a single line with well defined columns.
	 */
	public String format(LogRecord r) {
		StringBuffer sb = new StringBuffer();
		sb.append(r.getLevel().getName()).append("\t");
		sb.append(MessageFormat.format("{0, date, yy-MM-dd} {0, time, kk:mm:ss.SSSS} ",
			new Object[] { new Date(r.getMillis()) })+"\t");
		String cnm = r.getSourceClassName();
		String cn="";
		if (cnm != null) {
			int cnl = cnm.length();
			if (cnl>20) {
				cn = r.getSourceClassName().substring(cnl-19);
			} else {
				char sp[] = {' '};
				StringBuffer sb1= new StringBuffer().append(cnm);
				cn = sb1.append(sp,0, 1).toString();
			}
		}
		sb.append(cn).append("\t").append(" ");
		sb.append(left(r.getSourceMethodName(),23,' ')).append("\t");
		sb.append(r.getThreadID()).append("\t");
		sb.append(formatMessage(r)).append(LS);
		if (null != r.getThrown()) {
			sb.append("Throwable occurred: "); 
			Throwable t = r.getThrown();
			PrintWriter pw = null;
			try {
				StringWriter sw = new StringWriter();
				pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				sb.append(sw.toString());
			} finally {
				if (pw != null) {
					try {
						pw.close();
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}
		return sb.toString();
	}
	
	/**
	   * Left justify a string.
	   *
	   * @param s the string to justify
	   * @param width the field width to justify within
	   * @param fillChar the character to fill with
	   *
	   * @return the justified string.
	   */
	public static String left(String s, int width, char fillChar) {
		if (s.length() >= width) {
			return s;
		}
		StringBuffer sb = new StringBuffer(width);
		sb.append(s);
		for (int i = width - s.length(); --i >= 0;) {
			sb.append(fillChar);
		}
		return sb.toString();
	}

}
