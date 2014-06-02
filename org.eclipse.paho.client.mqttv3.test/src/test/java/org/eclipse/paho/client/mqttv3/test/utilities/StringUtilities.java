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
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test.utilities;

import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * Static utility functions 
 */
public class StringUtilities {

  private static final String className = StringUtilities.class.getName();
  private final static Logger log = Logger.getLogger(className);

  /** Lookup the line separator once */
  public static final String NL = System.getProperty("line.separator");

  /**
   * @param bytes
   * @return string
   */
  public static String localizedByteCount(long bytes) {
    MessageFormat form = new MessageFormat("{0,number,integer}");
    Object[] args = {bytes};
    return form.format(args);
  }

  /**
   * @param bytes
   * @param si
   * @return string
   */
  public static String humanReadableByteCount(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    if (bytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  /**
   * Helper method to convert a byte[] array (such as a MsgId) to a hex string
   *
   * @param array
   * @return hex string
   */
  public static String arrayToHexString(byte[] array) {
    return arrayToHexString(array, 0, array.length);
  }

  /**
   * Helper method to convert a byte[] array (such as a MsgId) to a hex string
   *
   * @param array
   * @param offset 
   * @param limit 
   * @return hex string
   */
  public static String arrayToHexString(byte[] array, int offset, int limit) {
    String retVal;
    if (array != null) {
      StringBuffer hexString = new StringBuffer(array.length);
      int hexVal;
      char hexChar;
      int length = Math.min(limit, array.length);
      for (int i = offset; i < length; i++) {
        hexVal = (array[i] & 0xF0) >> 4;
        hexChar = (char) ((hexVal > 9) ? ('A' + (hexVal - 10)) : ('0' + hexVal));
        hexString.append(hexChar);
        hexVal = array[i] & 0x0F;
        hexChar = (char) ((hexVal > 9) ? ('A' + (hexVal - 10)) : ('0' + hexVal));
        hexString.append(hexChar);
      }
      retVal = hexString.toString();
    }
    else {
      retVal = "<null>";
    }
    return retVal;
  }

  /**
   * @param text
   * @return a Java string
   */
  public static String toJavaString(String text) {

    String string = text;
    if (string != null) {
      string = string.replaceAll("\n", "\\\\n");
      string = string.replaceAll("\r", "\\\\r");
      string = string.replaceAll("\"", "\\\\\"");
    }

    StringBuilder sb = new StringBuilder();
    sb.append("\"");
    sb.append(string);
    sb.append("\"");

    return sb.toString();
  }

  /**
   * @param object
   * @return a non-null string based on the given string
   */
  public static String safeString(Object object) {
    return (object == null) ? "<null>" : object.toString();
  }

  /**
   * Left justify a string, padding with spaces.
   *
   * @param s the string to justify
   * @param width the field width to justify within
   *
   * @return the justified string.
   */
  public static String left(String s, int width) {
    return left(s, width, ' ');
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
   * Right justify a string, padding with spaces.
   *
   * @param s the string to justify
   * @param width the field width to justify within
   *
   * @return the justified string.
   */
  public static String right(String s, int width) {
    return right(s, width, ' ');
  }

  /**
   * Right justify a string.
   *
   * @param s the string to justify
   * @param width the field width to justify within
   * @param fillChar the character to fill with
   *
   * @return the justified string.
   */
  public static String right(String s, int width, char fillChar) {
    if (s.length() >= width) {
      return s;
    }
    StringBuffer sb = new StringBuffer(width);
    for (int i = width - s.length(); --i >= 0;) {
      sb.append(fillChar);
    }
    sb.append(s);
    return sb.toString();
  }

  /**
   * @param level1
   * @param level2
   * @return the higher level
   */
  public static String getHigherLevel(String level1, String level2) {
    int idx;
    String rlevel = level1;

    idx = 6;
    if (level1 == null) {
      rlevel = level2;
    }
    else if (level2 == null) {
      rlevel = level1;
    }
    else {

      while (idx < Math.min(level1.length(), level2.length())) {
        int f = Integer.parseInt(level1.substring(idx, idx + 1));
        int c = Integer.parseInt(level2.substring(idx, idx + 1));
        if (f > c) {
          rlevel = level1;
          break;
        }
        else if (f < c) {
          rlevel = level2;
          break;
        }
        idx++;
      }
    }
    return rlevel;
  }
}
