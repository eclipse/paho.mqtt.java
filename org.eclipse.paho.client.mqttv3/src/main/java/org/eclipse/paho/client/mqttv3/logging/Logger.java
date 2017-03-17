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
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.logging;

import java.util.ResourceBundle;

/**
 * A Logger object is used to send log and trace messages to a platform
 * specific logging implementation. Loggers are named, using a hierarchical 
 * dot-separated name-space.
 * Logger names can be arbitrary strings, but they should normally be based on
 * the component or the package name of the logged component
 * 
 * Logger objects may be obtained by calls on one of the getLogger factory
 * methods. These will either create a new Logger or return a suitable existing
 * Logger.
 * 
 * <p>
 * The int levels define a set of standard logging levels that can be used to
 * control logging output. The logging levels are ordered and are specified by
 * ordered integers. Enabling logging at a given level also enables logging at
 * all higher levels.
 * <p>
 * Clients should use the the convenience methods such as severe() and fine() or
 * one of the predefined level constants such as Logger.SEVERE and Logger.FINE
 * with the appropriate log(int level...) or trace(int level...) methods.
 * <p>
 * The levels in descending order are:</p>
 * <ul>
 * <li>SEVERE (log - highest value)</li>
 * <li>WARNING (log)</li>
 * <li>INFO (log)</li>
 * <li>CONFIG (log)</li>
 * <li>FINE (trace)</li>
 * <li>FINER (trace)</li>
 * <li>FINEST (trace - lowest value)</li>
 * </ul>
 * 
 */
public interface Logger {
	/**
	 * SEVERE is a message level indicating a serious failure.
	 * <p>
	 * In general SEVERE messages should describe events that are of
	 * considerable importance and which will prevent normal program execution.
	 * They should be reasonably intelligible to end users and to system
	 * administrators.
	 */
	public static final int	SEVERE	= 1;
	/**
	 * WARNING is a message level indicating a potential problem.
	 * <p>
	 * In general WARNING messages should describe events that will be of
	 * interest to end users or system managers, or which indicate potential
	 * problems.
	 */
	public static final int	WARNING	= 2;
	/**
	 * INFO is a message level for informational messages.
	 * <p>
	 * Typically INFO messages will be written to the console or its equivalent.
	 * So the INFO level should only be used for reasonably significant messages
	 * that will make sense to end users and system admins.
	 */
	public static final int	INFO	= 3;
	/**
	 * CONFIG is a message level for static configuration messages.
	 * <p>
	 * CONFIG messages are intended to provide a variety of static configuration
	 * information, to assist in debugging problems that may be associated with
	 * particular configurations. For example, CONFIG message might include the
	 * CPU type, the graphics depth, the GUI look-and-feel, etc.
	 */
	public static final int	CONFIG	= 4;
	/**
	 * FINE is a message level providing tracing information.
	 * <p>
	 * All of FINE, FINER, and FINEST are intended for relatively detailed
	 * tracing. The exact meaning of the three levels will vary between
	 * subsystems, but in general, FINEST should be used for the most voluminous
	 * detailed output, FINER for somewhat less detailed output, and FINE for
	 * the lowest volume (and most important) messages.
	 * <p>
	 * In general the FINE level should be used for information that will be
	 * broadly interesting to developers who do not have a specialized interest
	 * in the specific subsystem.
	 * <p>
	 * FINE messages might include things like minor (recoverable) failures.
	 * Issues indicating potential performance problems are also worth logging
	 * as FINE.
	 */
	public static final int	FINE	= 5;
	/**
	 * FINER indicates a fairly detailed tracing message. By default logging
	 * calls for entering, returning, or throwing an exception are traced at
	 * this level.
	 */
	public static final int	FINER	= 6;
	/**
	 * FINEST indicates a highly detailed tracing message.
	 */
	public static final int	FINEST	= 7;
	
	public void initialise(ResourceBundle messageCatalog, String loggerID, String resourceName);
	
	/**
	 * Set a name that can be used to provide context with each log record.
	 * This overrides the value passed in on initialise
	 * @param logContext The Log context name
	 */
	public void setResourceName(String logContext);
	
	/**
	 * Check if a message of the given level would actually be logged by this
	 * logger. This check is based on the Loggers effective level, which may be
	 * inherited from its parent.
	 * 
	 * @param level
	 *            a message logging level.
	 * @return true if the given message level is currently being logged.
	 */
	public boolean isLoggable(int level);

	/**
	 * Log a message, specifying source class and method, if the logger is
	 * currently enabled for the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used.
	 */
	public void severe(String sourceClass, String sourceMethod, String msg);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments, if the logger is currently enabled for the given
	 * message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 */
	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments and a throwable, if the logger is currently enabled for
	 * the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 * @param thrown
	 *            Throwable associated with log message.
	 */
	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown);

	/**
	 * Log a message, specifying source class and method, if the logger is
	 * currently enabled for the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used.
	 */
	public void warning(String sourceClass, String sourceMethod, String msg);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments, if the logger is currently enabled for the given
	 * message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 */
	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments and a throwable, if the logger is currently enabled for
	 * the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 * @param thrown
	 *            Throwable associated with log message.
	 */
	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown);

	/**
	 * Log a message, specifying source class and method, if the logger is
	 * currently enabled for the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used.
	 */
	public void info(String sourceClass, String sourceMethod, String msg);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments, if the logger is currently enabled for the given
	 * message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 */
	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments and a throwable, if the logger is currently enabled for
	 * the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 * @param thrown
	 *            Throwable associated with log message.
	 */
	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown);

	/**
	 * Log a message, specifying source class and method, if the logger is
	 * currently enabled for the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used.
	 */
	public void config(String sourceClass, String sourceMethod, String msg);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments, if the logger is currently enabled for the given
	 * message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 */
	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments and a throwable, if the logger is currently enabled for
	 * the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 * @param thrown
	 *            Throwable associated with log message.
	 */
	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown);

	/**
	 * Trace a message, specifying source class and method, if the logger is
	 * currently enabled for the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message catalog for the message or the actual
	 *            message itself. During formatting, if the logger has a mapping
	 *            for the msg string, then the msg string is replaced by the
	 *            value. Otherwise the original msg string is used.
	 */
	public void fine(String sourceClass, String sourceMethod, String msg);

	/**
	 * Trace a message, specifying source class and method, with an array of
	 * object arguments, if the logger is currently enabled for the given
	 * message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message catalog for the message or the actual
	 *            message itself. During formatting, if the logger has a mapping
	 *            for the msg string, then the msg string is replaced by the
	 *            value. Otherwise the original msg string is used. The
	 *            formatter uses java.text.MessageFormat style formatting to
	 *            format parameters, so for example a format string "{0} {1}"
	 *            would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 */
	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts);
	
	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex);

	/**
	 * Trace a message, specifying source class and method, if the logger is
	 * currently enabled for the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message catalog for the message or the actual
	 *            message itself. During formatting, if the logger has a mapping
	 *            for the msg string, then the msg string is replaced by the
	 *            value. Otherwise the original msg string is used.
	 */
	public void finer(String sourceClass, String sourceMethod, String msg);

	/**
	 * Trace a message, specifying source class and method, with an array of
	 * object arguments, if the logger is currently enabled for the given
	 * message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message catalog for the message or the actual
	 *            message itself. During formatting, if the logger has a mapping
	 *            for the msg string, then the msg string is replaced by the
	 *            value. Otherwise the original msg string is used. The
	 *            formatter uses java.text.MessageFormat style formatting to
	 *            format parameters, so for example a format string "{0} {1}"
	 *            would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 */
	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts);
	
	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex);

	/**
	 * Trace a message, specifying source class and method, if the logger is
	 * currently enabled for the given message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message catalog for the message or the actual
	 *            message itself. During formatting, if the logger has a mapping
	 *            for the msg string, then the msg string is replaced by the
	 *            value. Otherwise the original msg string is used.
	 */
	public void finest(String sourceClass, String sourceMethod, String msg);

	/**
	 * Trace a message, specifying source class and method, with an array of
	 * object arguments, if the logger is currently enabled for the given
	 * message level.
	 * 
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message catalog for the message or the actual
	 *            message itself. During formatting, if the logger has a mapping
	 *            for the msg string, then the msg string is replaced by the
	 *            value. Otherwise the original msg string is used. The
	 *            formatter uses java.text.MessageFormat style formatting to
	 *            format parameters, so for example a format string "{0} {1}"
	 *            would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 */
	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts);
	
	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex);

	/**
	 * Log a message, specifying source class and method, with an array of
	 * object arguments and a throwable, if the logger is currently enabled for
	 * the given message level.
	 * 
	 * @param level
	 *            One of the message level identifiers, e.g. SEVERE.
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message, may be null.
	 * @param thrown
	 *            Throwable associated with log message.
	 */
	public void log(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown);

	/**
	 * Log a trace message, specifying source class and method, with an array of
	 * object arguments and a throwable, if the logger is currently enabled for
	 * the given message level.
	 * 
	 * @param level
	 *            One of the message level identifiers, e.g. SEVERE.
	 * @param sourceClass
	 *            Name of class that issued the logging request.
	 * @param sourceMethod
	 *            Name of method that issued the logging request.
	 * @param msg
	 *            The key in the message catalog for the message or the actual
	 *            message itself. During formatting, if the logger has a mapping
	 *            for the msg string, then the msg string is replaced by the
	 *            value. Otherwise the original msg string is used. The
	 *            formatter uses java.text.MessageFormat style formatting to
	 *            format parameters, so for example a format string "{0} {1}"
	 *            would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message, may be null.
	 * @param ex 
	 * 			Throwable associated with log message.
	 */
	public void trace(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex);

	/**
	 * Format a log message without causing it to be written to the log.
	 * 
	 * @param msg
	 *            The key in the message localization catalog for the message or
	 *            the actual message itself. During formatting, if the logger
	 *            has a mapping for the msg string, then the msg string is
	 *            replaced by the localized value. Otherwise the original msg
	 *            string is used. The formatter uses java.text.MessageFormat
	 *            style formatting to format parameters, so for example a format
	 *            string "{0} {1}" would format two inserts into the message.
	 * @param inserts
	 *            Array of parameters to the message.
	 * @return The formatted message for the current locale.
	 */
	public String formatMessage(String msg, Object[] inserts);
	
	public void dumpTrace();
}