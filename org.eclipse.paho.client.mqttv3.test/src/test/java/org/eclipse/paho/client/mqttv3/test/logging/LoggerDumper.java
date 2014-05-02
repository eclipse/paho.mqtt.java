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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.test.utilities.StringUtilities;

/**
 * Utility class which dumps the formatters and handlers under a log manager
 * <p>
 * The Java Util Logger package does not provide public method to dump its handlers and formatters in a useful way
 * so the LogDumper class builds up a collection of the handlers using this simple container class which it can then dump
 * in a human readable way. 
 */
public class LoggerDumper {

  static final Class<?> cclass = LoggerDumper.class;

  private LoggerNode rootNode = null;

  /**
   * @throws Exception 
   * 
   */
  public LoggerDumper() throws Exception {
    LogManager mgr = LogManager.getLogManager();
    Enumeration<String> enumeration = mgr.getLoggerNames();
    while (enumeration.hasMoreElements()) {
      String name = enumeration.nextElement();
      Logger log = mgr.getLogger(name);
      findParentNode(log);
    }
  }

  /**
   * @param logger
   * @throws Exception
   */
  private LoggerNode findParentNode(Logger logger) throws Exception {
    LoggerNode parentNode = null;
    Logger parent = logger.getParent();

    if (parent == null) {
      if (rootNode == null) {
        parentNode = rootNode = new LoggerNode(null, logger);
      }
      else if (rootNode.getLogger() == logger) {
        parentNode = rootNode;
      }
      else {
        throw new Exception("duplicate root");
      }
    }
    else {
      parentNode = findParentNode(parent);

      LoggerNode found = null;
      for (Iterator<LoggerNode> iterator = parentNode.getChildren().iterator(); iterator.hasNext();) {
        LoggerNode childNode = iterator.next();
        if (childNode.getLogger() == logger) {
          found = childNode;
          break;
        }
      }
      if (found == null) {
        LoggerNode node = new LoggerNode(parentNode, logger);
        parentNode.getChildren().add(node);
      }
    }

    return parentNode;
  }

  /**
   *
   */
  public void dump() {
    StringBuilder sb = new StringBuilder();
    sb.append("-----------------------------------------------------------------" + StringUtilities.NL);
    dumpLoggerNode(rootNode, 0, sb);
    sb.append("-----------------------------------------------------------------");
    System.out.println(sb.toString());
  }

  /**
   * @param node
   * @param sb  
   */
  private void dumpLoggerNode(LoggerNode node, int indent, StringBuilder sb) {
    Logger l = node.getLogger();
    String padding = StringUtilities.left("", indent * 2);

    sb.append(padding);
    // sb.append("@" + Integer.toHexString(System.identityHashCode(l)) + " ");
    sb.append("\"" + l.getName() + "\" ");
    sb.append(l.getLevel());
    sb.append(StringUtilities.NL);

    Handler[] handlers = l.getHandlers();
    for (Handler h : handlers) {
      Formatter f = h.getFormatter();
      sb.append(padding);
      sb.append("        Handler = ");
      sb.append(StringUtilities.left(h.getClass().getName(), 40));
      sb.append("Formatter = ");
      sb.append(StringUtilities.left(f.getClass().getName(), 40));
      sb.append(StringUtilities.NL);
    }

    for (Iterator<LoggerNode> iterator = node.getChildren().iterator(); iterator.hasNext();) {
      LoggerNode child = iterator.next();
      dumpLoggerNode(child, indent + 1, sb);
    }
  }
}
