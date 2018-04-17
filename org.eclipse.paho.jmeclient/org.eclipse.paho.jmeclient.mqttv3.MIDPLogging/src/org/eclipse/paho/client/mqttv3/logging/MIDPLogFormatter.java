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

import net.sf.microlog.core.Formatter;
import net.sf.microlog.core.Level;

public class MIDPLogFormatter implements Formatter {
	
	final String ls = System.getProperty("line.separator") != null ? System.getProperty("line.separator") : "\n";

	public String format(String clientID, String name, long time, Level level, Object message, Throwable t) {
		
		
		String[] MessageParts = split((String)message);
		
		StringBuffer sb = new StringBuffer();
		sb.append(level.toString()+"\t");
        
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(Long.parseLong(MessageParts[0])));
		StringBuffer timeBuffer = new StringBuffer();
		timeBuffer.append(format(cal.get(Calendar.YEAR),4) + "-");
		timeBuffer.append(format(cal.get(Calendar.MONTH) +1, 2)).append("-");
		timeBuffer.append(format(cal.get(Calendar.DATE),2)).append(" ");
		timeBuffer.append(format(cal.get(Calendar.HOUR_OF_DAY),2)).append(':');
		timeBuffer.append(format(cal.get(Calendar.MINUTE),2)).append(':');
		timeBuffer.append(format(cal.get(Calendar.SECOND),2)).append(".");
		timeBuffer.append(format(cal.get(Calendar.MILLISECOND),3));
	    sb.append(timeBuffer.toString() + " \t");
	    
	    String cnm = MessageParts[1];
        String cn="";
        if (cnm != null) {
	        int cnl = cnm.length();
	        if (cnl>20) {
	        	cn = MessageParts[1].substring(cnl-19);
	        } else {
	        	char sp[] = {' '};
	        	StringBuffer sb1= new StringBuffer().append(cnm);
	        	cn = sb1.append(sp,0, 1).toString();
	        }        
        }
        sb.append(cn+"\t").append(" ");
        sb.append(left(MessageParts[2],23,' ')+"\t");
        sb.append(left(MessageParts[3],30,' ')+"\t"); 
        sb.append((MessageParts[4]));
        if (null != t) {
            sb.append("Throwable occurred: "); 
            sb.append(t.toString());
        }
		
		return sb.toString();
	}

	private static String[] split(String message) {
		String[] parts = new String[5];
		for (int i=0;i<parts.length;i++) {
			int index = message.indexOf("###");
			String temp = "";
			if (index != -1) {
				temp = message.substring(0,index);
				message = message.substring(index+3);
			} else {
				temp = message;
			}
			parts[i] = temp;
		}
		return parts;
	}

	public String[] getPropertyNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setProperty(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
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
