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

package org.eclipse.paho.client.mqttv3.test.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;

import org.eclipse.paho.client.mqttv3.test.utilities.StringUtilities;
import org.xml.sax.Attributes;

/**
 * Utility class used by the framework logger to format an arbitrary object 
 */
public class ObjectFormatter {

  private StringBuffer buffer;
  private int width1;
  private int width2;
  private String separator;

  /**
   * @param width1
   * @param width2
   * @param separator
   */
  public ObjectFormatter(int width1, int width2, String separator) {
    buffer = new StringBuffer();
    this.width1 = width1;
    this.width2 = width2;
    this.separator = separator;
  }

  private void addField(String lquote, String rquote, String name, String value) {
    buffer.append(StringUtilities.left(name, width1));
    buffer.append(StringUtilities.left(":", width2));
    buffer.append(lquote);
    buffer.append(value);
    buffer.append(rquote);
    buffer.append(separator);
  }

  private void addField(String lquote, String rquote, int name, String value) {
    buffer.append(StringUtilities.right(Integer.toString(name), width1));
    buffer.append(StringUtilities.left(":", width2));
    buffer.append(lquote);
    buffer.append(value);
    buffer.append(rquote);
    buffer.append(separator);
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, int value) {
    addField("", "", name, Integer.toString(value));
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, long value) {
    addField("", "", name, Long.toString(value));
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, boolean value) {
    addField("", "", name, Boolean.toString(value));
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, String value) {
    addField("'", "'", name, StringUtilities.safeString(value));
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, String[] value) {
    if (value != null) {
      buffer.append(StringUtilities.left(name, width1));
      buffer.append(":");
      buffer.append(separator);
      for (int i = 0; i < value.length; i++) {
        addField("(", ")", i, StringUtilities.safeString(value[i]));
      }
    }
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, int[] value) {
    buffer.append(StringUtilities.left(name, width1));
    buffer.append(StringUtilities.left(":", width2));
    buffer.append("(");
    for (int i = 0; i < value.length; i++) {
      if (i > 0) {
        buffer.append(',');
      }
      buffer.append(value[i]);
    }
    buffer.append(")");
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, byte[] value) {
    addField("", "", name, StringUtilities.arrayToHexString(value));
  }

  /**
   * @param name
   * @param value
   */
  public void add(String name, Object value) {
    addField("[", "]", name, StringUtilities.safeString(value));
  }

  /**
   * @param value
   * @return this instance 
   */
  public ObjectFormatter append(String value) {
    buffer.append(value);
    buffer.append(separator);
    return this;
  }

  /**
   * @return string
   */
  @Override
  public String toString() {
    return buffer.toString();
  }

  /**
   * @param object
   * @return string
   */
  public static String format(Object object) {
    StringBuilder sb = new StringBuilder();

    if (object == null) {
      sb.append("(null)");
    }
    else if (object instanceof Attributes) {
      sb.append(format((Attributes) object));
    }
    else if (object instanceof String) {
      sb.append(format((String) object));
    }
    else if (object instanceof StringBuffer) {
      sb.append(format((StringBuffer) object));
    }
    else if (object instanceof StringBuilder) {
      sb.append(format((StringBuilder) object));
    }
    else if (object instanceof Throwable) {
      sb.append(format((Throwable) object));
    }
    else {
      boolean isArray = object.getClass().isArray();
      if (isArray) {
        int arrayLength = Array.getLength(object);
        if (arrayLength > 1) {
          sb.append("[");
        }
        for (int i = 0; i < arrayLength; i++) {
          Object element = Array.get(object, i);
          if (i > 0) {
            sb.append(", ");
          }
          sb.append(format(element));
        }
        if (arrayLength > 1) {
          sb.append("]");
        }
      }
      else {
        sb.append(object);
      }
    }
    return sb.toString();
  }

  /**
   * @param attributes
   * @return string
   */
  public static String format(Attributes attributes) {

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < attributes.getLength(); i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append("[");
      String name = attributes.getQName(i);
      String value = attributes.getValue(i);
      sb.append(name);
      sb.append("=\"");
      sb.append(value);
      sb.append("\"");
      sb.append("]");
    }

    return sb.toString();
  }

  /**
   * @param text
   * @return string
   */
  public static String format(String text) {
    return StringUtilities.toJavaString(text);
  }

  /**
   * @param sb
   * @return string
   */
  public static String format(StringBuffer sb) {
    return format(sb.toString());
  }

  /**
   * @param sb
   * @return string
   */
  public static String format(StringBuilder sb) {
    return format(sb.toString());
  }

  /**
   * @param t
   * @return string
   */
  public static String format(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    t.printStackTrace(pw);
    pw.flush();
    sw.flush();
    return sw.toString();
  }

}
