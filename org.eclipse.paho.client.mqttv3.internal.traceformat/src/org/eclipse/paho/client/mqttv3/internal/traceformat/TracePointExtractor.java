/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal.traceformat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TracePointExtractor {

	public static void main(String[] args) {
		if (args == null) {
			args = new String[] {};
		}
		if (args.length != 0 && args.length != 2 && args.length != 4) {
			usageAndExit();
		}
		String dir = ".";
		String file = "./trace.properties";
		
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
			TracePointExtractor tpe = new TracePointExtractor(dir, file);
			tpe.parse();
		} catch (Exception e) {
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
	
	public TracePointExtractor(String basedir, String outputfile) {
		this.basedir = (new File(basedir)).getAbsolutePath();
		this.outputfile = outputfile;
		this.pattern = Pattern.compile("^\\s*//\\s*@TRACE\\s*(\\d+)=(.*?)\\s*$");
		this.points = new HashMap();
	}
	public void parse() throws Exception {
		this.out = new PrintStream(new FileOutputStream(this.outputfile));
		scanDirectory(new File(this.basedir));
		this.out.close();
	}
	
	public void scanDirectory(File f) throws Exception {
		if (f.isFile() && f.getName().endsWith(".java")) {
			parseFile(f);
		} else {
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				for (int i=0;i<files.length;i++) {
					scanDirectory(files[i]);
				}
			}
		}
	}
	
	public void parseFile(File f) throws Exception {
		String filename = f.getAbsolutePath();
		String classname = filename.substring(this.basedir.length()+1).replaceAll("/",".");
		classname = classname.substring(0,classname.length()-5);
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line;
		int lineNo = 1;
		while ( (line = in.readLine()) != null) {
			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				String number = m.group(1);
				if (this.points.containsKey(number)) {
					System.out.println("Duplicate Trace Point: "+number);
					System.out.println(" "+this.points.get(number));
					System.out.println(" "+classname+":"+lineNo);
					throw new Exception();
				}
				out.println(number+".class="+classname);
				out.println(number+".line="+lineNo);
				out.println(number+".value="+m.group(2));
				this.points.put(number, classname+":"+lineNo);
			}
			lineNo++;
		}
	}
}
