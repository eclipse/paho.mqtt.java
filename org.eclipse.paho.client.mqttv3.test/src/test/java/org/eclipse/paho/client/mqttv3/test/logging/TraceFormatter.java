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
 * A log formatter which formats the LogRecord fields in a way which is suitable for tracing
 */
public class TraceFormatter extends Formatter {

  private final static SimpleDateFormat formater = new SimpleDateFormat("kk:mm:ss.SSS");
  private String NL = StringUtilities.NL;
  private Date date = new Date();

  /**
   * 
   */
  public TraceFormatter() {
    System.out.println("");
  }

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

    date.setTime(record.getMillis());
    sb.append(formater.format(date));
    sb.append(" ");

    sb.append(formatJavaName(record.getSourceClassName(), 60));
    sb.append(" ");
    sb.append(type);
    sb.append(" ");
    sb.append(formatJavaName(record.getSourceMethodName(), 30));
    sb.append(" ");
    sb.append(text);
    sb.append(NL);

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

    return sb.toString();
  }

  /**
   * @param width
   * @param n
   * @return string
   */
  private String formatJavaName(String n, int width) {
    String string = (n == null) ? "" : n;
    return StringUtilities.left(string, width);
  }

  /**
   * @param r 
   * @return string
   */
  public String[] parseLogRecord(LogRecord r) {

    String string = "   ";
    String text = "";

    String message = r.getMessage();
    if (message != null) {
      if (message.startsWith("ENTRY")) {
        string = "-->";
        text = formatParameters(r);
      }
      else if (message.startsWith("RETURN")) {
        string = "<--";
        text = formatParameters(r);
      }
      else {
        text = message;
      }
    }

    return new String[]{string, text};
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
