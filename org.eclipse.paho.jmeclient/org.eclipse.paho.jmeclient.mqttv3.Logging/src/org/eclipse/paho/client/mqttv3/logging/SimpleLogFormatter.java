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

import java.util.Calendar;
import java.util.Date;
import com.oracle.util.logging.LogRecord;


/**
 * SimpleLogFormatter prints a single line 
 * log record in human readable form.
 */
public class SimpleLogFormatter extends com.oracle.util.logging.Formatter {
	
	final String ls = System.getProperty("line.separator") != null ? System.getProperty("line.separator") : "\n";
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
        sb.append(r.getLevel().getName()+"\t");
        
        // Format the date time of the record
        Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(r.getMillis()));
		StringBuffer time = new StringBuffer();
		time.append(format(cal.get(Calendar.YEAR),4) + "-");
		time.append(format(cal.get(Calendar.MONTH) +1, 2)).append("-");
		time.append(format(cal.get(Calendar.DATE),2)).append(" ");
	    time.append(format(cal.get(Calendar.HOUR_OF_DAY),2)).append(':');
	    time.append(format(cal.get(Calendar.MINUTE),2)).append(':');
	    time.append(format(cal.get(Calendar.SECOND),2)).append(".");
	    time.append(format(cal.get(Calendar.MILLISECOND),3));
        sb.append(time.toString() + " \t");
        
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
        sb.append(cn+"\t").append(" ");
        sb.append(left(r.getSourceMethodName(),23,' ')+"\t");
        sb.append(r.getThreadID()+"\t"); 
        sb.append(formatMessage(r)).append(ls);
        if (null != r.getThrown()) {
            sb.append("Throwable occurred: "); 
            Throwable t = r.getThrown();
            sb.append(t.toString());
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
	  
	  /**
	   * Format a number to include the correct length
	   *
	   * @param number The Integer to be formatted
	   * @param format The number of leading zeros to append (if required)
	   *
	   * @return the justified string.
	   */
	  private static String format(int number, int format) {
		String retVal = ""+number;
		while (retVal.length() < format) {
		  retVal = "0" + retVal;
		}
		return retVal;
	  }

}
