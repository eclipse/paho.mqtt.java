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

import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Class containing logging utility methods  
 */
public class LoggingUtilities {

  /**
   * Configure logging by loading the logging.properties file
   */
  public static final Class<?> cclass = LoggingUtilities.class;

  static {
    String configClass = System.getProperty("java.util.logging.config.class");
    String configFile = System.getProperty("java.util.logging.config.file");

    if ((configClass == null) && (configFile == null)) {
      try {
        InputStream inputStream = cclass.getClassLoader().getResourceAsStream("logging.properties");
        LogManager manager = LogManager.getLogManager();
        manager.readConfiguration(inputStream);
        inputStream.close();
      }
      catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

  /**
   * @return logStream
   */
  public static PrintStream getPrintStream() {
    return System.out;
  }

  /**
   * Log a banner containing the class and method name 
   * 
   * @param logger
   * @param clazz 
   * @param methodName 
   */
  public static void banner(Logger logger, Class<?> clazz, String methodName) {
    banner(logger, clazz, methodName, null);
  }

  /**
   * Log a banner containing the class and method name and text 
   *    
   * @param logger
   * @param clazz 
   * @param methodName 
   * @param text 
   */
  public static void banner(Logger logger, Class<?> clazz, String methodName, String text) {
    String string = clazz.getSimpleName() + "." + methodName;
    if (text != null) {
      string += " " + text;
    }

    logger.info("");
    logger.info("*************************************************************");
    logger.info("* " + string);
    logger.info("*************************************************************");
  }

  /**
   * Dump the configuration of the log manager
   * 
   * @throws Exception 
   */
  public static void dump() throws Exception {
    LoggerDumper loggerDumper = new LoggerDumper();
    loggerDumper.dump();
  }
}
