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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.eclipse.paho.client.mqttv3.test.utilities.StringUtilities;

/**
 * A log formatter which formats most of the LogRecord fields.
 */
public class DetailFormatter extends Formatter {

  private final static SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd kkmmss.SSS");
  private String NL = StringUtilities.NL;
  private Date date = new Date();

  /**
   * Format the given LogRecord.
   * @param record the log record to be formatted.
   * @return a formatted log record
   */
  @Override
  public synchronized String format(LogRecord record) {
    StringBuffer sb = new StringBuffer();

    String[] array = parseLogRecord(record);
    String type = array[0];
    String text = array[1];

    addTimeStamp(record, sb);
    addClassName(record, sb);
    addTypeName(record, sb, type);
    addMethodName(record, sb);
    addText(record, sb, text);
    addThrown(record, sb);

    return sb.toString();
  }

  /**
   * @param record
   * @param sb
   */
  public void addThrown(LogRecord record, StringBuffer sb) {
    Throwable thrown = record.getThrown();
    if (thrown != null) {
      try {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        sb.append(sw.toString());
      }
      catch (Exception ex) {
        // do nothing
      }
    }
  }

  /**
   * @param record 
   * @param sb
   * @param text
   */
  public void addText(LogRecord record, StringBuffer sb, String text) {
    sb.append(text);
    sb.append(NL);
  }

  /**
   * @param record
   * @param sb
   */
  public void addMethodName(LogRecord record, StringBuffer sb) {
    sb.append(formatJavaName(record.getSourceMethodName(), 30));
    sb.append(" ");
  }

  /**
   * @param record 
   * @param sb
   * @param type
   */
  public void addTypeName(LogRecord record, StringBuffer sb, String type) {
    sb.append(type);
    sb.append(" ");
  }

  /**
   * @param record
   * @param sb
   */
  public void addClassName(LogRecord record, StringBuffer sb) {
    sb.append(formatJavaName(record.getSourceClassName(), 60));
    sb.append(" ");
  }

  /**
   * @param record
   * @param sb
   */
  public void addTimeStamp(LogRecord record, StringBuffer sb) {
    date.setTime(record.getMillis());
    synchronized (formater) {
      sb.append(formater.format(date));
    }
    sb.append(" ");
  }

  /**
   * @param width
   * @param n
   * @return string
   */
  public String formatJavaName(String n, int width) {
    String string = (n == null) ? "" : n;
    return StringUtilities.left(string, width);
  }

  /**
   * @param r 
   * @return string
   */
  public String[] parseLogRecord(LogRecord r) {

    String type = "   ";
    String text = "";

    String message = r.getMessage();
    Throwable throwable = r.getThrown();
    if (message != null) {
      if (message.startsWith("ENTRY")) {
        type = "-->";
        text = formatParameters(r);
      }
      else if (message.startsWith("RETURN")) {
        type = "<--";
        text = formatParameters(r);
      }
      else if ((throwable != null) && ("THROW".equals(message))) {
        text = "";
      }
      else {
        text = message;
      }
    }

    return new String[]{type, text};
  }

  /**
   * @param r 
   * @return string
   */
  public String formatParameters(LogRecord r) {
    String string = "";
    Object[] parameters = r.getParameters();
    if (parameters != null) {
      string = ObjectFormatter.format(parameters);
    }
    return string;
  }

}
