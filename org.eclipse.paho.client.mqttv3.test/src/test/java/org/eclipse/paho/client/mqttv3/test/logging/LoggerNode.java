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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The Java Util Logger package does not provide public method to dump its handlers and formatters in a useful way
 * so the LogDumper class builds up a collection of the handlers using this simple container class which it can then dump
 * in a human readable way.  
 */
public class LoggerNode {

  private LoggerNode parent;
  private Logger logger;
  private Set<LoggerNode> children;

  /**
   * @param p
   * @param l
   */
  public LoggerNode(LoggerNode p, Logger l) {
    parent = p;
    logger = l;
    children = new HashSet<LoggerNode>();
  }

  /**
   * @return the parent
   */
  public LoggerNode getParent() {
    return parent;
  }

  /**
   * @return the logger
   */
  public Logger getLogger() {
    return logger;
  }

  /**
   * @return the children
   */
  public Collection<LoggerNode> getChildren() {
    return children;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return logger.getName();
  }
}
