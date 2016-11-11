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
package org.eclipse.paho.client.mqttv3.internal.logBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 
 * Scan all Paho source files and extract NLSable trace and log records. 
 * 
 * This needs to be run any time new trace/log records are added
 * or changed. The logcat.properties file in the mqttv3.internal.nls 
 * is updated to match the trace records in the paho source files. 
 */
public class LogMessageExtractor {

	public static void main(String[] args) {
		if (args == null) {
			args = new String[] {};
		}
		if (args.length != 0 && args.length != 2 && args.length != 4) {
			usageAndExit();
		}
		// Set defaults by assuming this is run from an eclipse workspace with paho projects loaded
		String dir = "../org.eclipse.paho.client.mqttv3/src";
		String file = dir+"/org/eclipse/paho/client/mqttv3/internal/nls/logcat.properties";
				
		for (int i=0;i<args.length; i+=2) {
			if (args[i].equals("-d")) {
				dir = args[i+1];
			} else if (args[i].equals("-o")) {
				file = args[i+1];
			} else {
				System.out.println("Unknown arg: "+args[i]);
				usageAndExit();
			}
		}
		
		try {
			LogMessageExtractor tpe = new LogMessageExtractor(dir, file);
			tpe.parse();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void usageAndExit() {
		System.out.println("usage:\n org.eclipse.paho.client.mqttv3.internal.trace.TracePointExtractor [-d baseDir] [-o outputFile]");
		System.out.println("  -d baseDir        the source base directory [.]");
		System.out.println("  -o outputFile     the output file.          [./trace.properties]");
		System.exit(1);
	}
	
	
	private String basedir;
	private String outputfile;
	private Pattern pattern;
	private PrintStream out;
	private HashMap points;
	
	public LogMessageExtractor(String basedir, String outputfile) {
		this.basedir = (new File(basedir)).getAbsolutePath();
		this.outputfile = outputfile;
		this.pattern = Pattern.compile("^\\s*//\\s*@TRACE\\s*(\\d+)=(.*?)\\s*$");
		this.points = new HashMap();
	}
	public void parse() throws Exception {
		System.out.println("Scanning source directories: "+this.basedir);
		System.out.println("Outputing results to: "+this.outputfile);
		this.out = new PrintStream(new FileOutputStream(this.outputfile));
		out.println("0=MQTT Catalog");
		short rc = scanDirectory(new File(this.basedir));
		this.out.close();
		if (rc == 0 ) {
			System.out.println("Finished");
		} else {
			System.out.println("Problems found");
			throw new Exception();
		}
	}
	
	public short scanDirectory(File f) throws Exception {
		short rc = 0;
		if (f.isFile() && f.getName().endsWith(".java")) {
			short rc1 = parseFile(f);
			if (rc1>0) {
				rc = rc1;;
			}
		} else {
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				for (int i=0;i<files.length;i++) {
					short rc1 = scanDirectory(files[i]);
					if (rc1>0) {
						rc = rc1;;
					}
				}
			}
		}
		return rc;
	}
	
	public short parseFile(File f) throws Exception {
		String filename = f.getAbsolutePath();
		String classname = filename.substring(this.basedir.length()+1).replaceAll("/",".");
		classname = classname.substring(0,classname.length()-5);
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line;
		int lineNo = 1;
		short rc = 0;
		
		while ( (line = in.readLine()) != null) {
			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				String number = m.group(1);
				if (this.points.containsKey(number)) {
					System.out.println("Duplicate Trace Point: "+number);
					System.out.println(" "+this.points.get(number));
					System.out.println(" "+classname+":"+lineNo);
					rc=1;
				}
				// The original extractor put out 4 values for each trace point
//				out.println(number+".class="+classname);
//				out.println(number+".line="+lineNo);
//				out.println(number+".value="+m.group(2));
				this.points.put(number, classname+":"+lineNo);
				out.println(number+"="+m.group(2));
			}
			lineNo++;
		}
		in.close();
		
		return rc;
	}
}
