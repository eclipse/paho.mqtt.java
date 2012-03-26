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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.internal.trace.TracePoint;

public class TraceFormatter {

	private DataInputStream in;
	private PrintWriter out;
	private Properties traceProperties;
	private String traceFile;
	private String outputFile;
	
	public static void main(String[] args) {
		if (args == null) {
			args = new String[] {};
		}
		if (args.length != 0 && args.length != 2 && args.length != 4) {
			usageAndExit();
		}
		String traceFile = "mqtt-0.trc";
		String outputFile = "mqtt-0.trc.html";
		for (int i=0;i<args.length; i+=2) {
			if (args[i].equals("-i")) {
				traceFile = args[i+1];
			} else if (args[i].equals("-o")) {
				outputFile = args[i+1];
			} else {
				System.out.println("Unknown arg: "+args[i]);
				usageAndExit();
			}
		}
		
		try {
			TraceFormatter tf = new TraceFormatter(traceFile, outputFile);
			tf.format();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	private static void usageAndExit() {
		System.out.println("usage:\n org.eclipse.paho.client.mqttv3.internal.trace.TraceFormatter [-i traceFile] [-o outputFile]");
		System.out.println("  -i traceFile      the trace file to format. [./mqtt-0.trc]");
		System.out.println("  -o outputFile     the output file.          [./mqtt-0.trc.html]");
		System.exit(1);
	}
	
	
	public TraceFormatter(String traceFile, String outputFile) throws IOException {
		this.traceFile = traceFile;
		this.outputFile = outputFile;
		traceProperties = new Properties();
		InputStream propsStream = getClass().getResourceAsStream("/trace.properties");
		if (propsStream == null) {
			propsStream = new FileInputStream("trace.properties");
		}
		traceProperties.load(propsStream);

	}
	public void format() throws IOException {
		in = new DataInputStream(new FileInputStream(traceFile));
		OutputStream outputStream = new FileOutputStream(outputFile);
		out = new PrintWriter(outputStream);
		TracePoint tp = read();
		out.println("<html><head>");
		out.println("<title>org.eclipse.paho.client.mqttv3 trace</title>");
		out.println("<style>table {	empty-cells: show; border-collapse: collapse; border: solid black 1px; font-size: smaller; } th { background-color: #dedeff; } tr#f td { background-color: #eef; } th, td { border: solid black 1px; padding: 5px; color: black; } td { vertical-align: top; white-space: nowrap; }</style>");
		out.println("<script>");
		out.println("var fs = ['f_src','f_tn','f_id','f_cn','f_va'];var fe = [true,false,true,false,false];");
		out.println("function f() {var ns=document.getElementsByTagName('tr');var i=0;var j=0;var hs='';for(i in fs){hs=bh(hs,fs[i]);}window.location.hash=hs;for(i=2;i<ns.length;i++){var rs=true;for(j in fs){rs=rs&&m(ns[i].children[parseInt(j)+1],document.getElementById(fs[j]).value,fe[j]);}if(rs){ns[i].style.display='';}else{ns[i].style.display='none';}}}");
		out.println("function bh(h,n) {var v=document.getElementById(n).value;if(v!=''){if(h.length>0){h+='&'}h+=n+'='+encodeURIComponent(v);}return h;}");
		out.println("function m(n,v,e) {return v==''||(e&&n.innerHTML==v)||(!e&&n.innerHTML.indexOf(v)!= -1);} ");
		out.println("function c(i) {document.getElementById(i).value='';f();}");
		out.println("function init(){var h=window.location.hash;if(h.indexOf('#')==0){h=h.substring(1);}var p=h.split('&');var m={};var k=0;for(k in p){var t=p[k].split('=',2);m[t[0]]=decodeURIComponent(t[1]);}var j=0;for(j in fs){if(m[fs[j]]){document.getElementById(fs[j]).value=m[fs[j]];}}f();}");
		out.println("</script>");
		out.println("</head><body onload=\"init();\">");
		out.println("<table>");
		out.println("<tr><th>Timestamp</th><th>Source</th><th>Thread</th><th>ID</th><th>Class</th><th>Value</th><th>Stack</th></tr>");
		out.println("<tr id=\"f\">");
		out.println("<td>Filter:</td>");
		out.println("<td><input id=\"f_src\" type=\"text\" size=\"2\" onchange=\"f();\" /><a href=\"#\" onclick=\"c('f_src'); return false;\">clear</a></td>");
		out.println("<td><input id=\"f_tn\" type=\"text\" size=\"25\" onchange=\"f();\" /><a href=\"#\" onclick=\"c('f_tn'); return false;\">clear</a></td>");
		out.println("<td><input id=\"f_id\" type=\"text\" size=\"2\" onchange=\"f();\" /><a href=\"#\" onclick=\"c('f_id'); return false;\">clear</a></td>");
		out.println("<td><input id=\"f_cn\" type=\"text\" size=\"25\" onchange=\"f();\" /><a href=\"#\" onclick=\"c('f_cn'); return false;\">clear</a></td>");
		out.println("<td><input id=\"f_va\" type=\"text\" size=\"25\" onchange=\"f();\" /><a href=\"#\" onclick=\"c('f_va'); return false;\">clear</a></td>");
		out.println("<td></td>");
		out.println("</tr>");

		int count=0;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yy/MM/dd-HH:mm:ss.S");
		while(tp != null) {
			out.println("<tr id=\"tp-"+count+"\">");
			out.println("  <td>"+dateFormat.format(new Date(tp.timestamp))+"</td>");
			out.println("  <td>"+tp.source+"</td>");
			out.println("  <td>"+tp.threadName+"</td>");
			out.println("  <td>"+tp.id+"</td>");
			if (traceProperties.getProperty(tp.id+".value") != null) {
				out.println("  <td>"+traceProperties.getProperty(tp.id+".class")+":"+traceProperties.getProperty(tp.id+".line")+"</td>");
				out.println("  <td>"+MessageFormat.format(traceProperties.getProperty(tp.id+".value"), tp.inserts)+"</td>");
				
			} else {
				out.println("  <td>unknown</td>");
				String formattedInserts = "<ul>";
				for (int i=0;i<tp.inserts.length;i++) {
					formattedInserts+="<li>"+tp.inserts[i]+"</li>";
				}
				formattedInserts+="</ul>";
				out.println("  <td>"+formattedInserts+"</td>");
			}

			String formattedStack = "<ul>";
			for (int i=0;i<tp.stacktrace.length;i++) {
				formattedStack+="<li>"+tp.stacktrace[i]+"</li>";
			}
			formattedStack+="</ul>";
			out.println("  <td>"+formattedStack+"</td>");
			out.println("</tr>");
			tp = read();
			count++;
		}
		out.println("</table>");
		out.println("</body></head>");
		out.flush();
		out.close();
	}
	
	public TracePoint read() {
		/*
		 * 	public long timestamp;
				public byte type;
				public short id;
				public byte level;
				public String threadName;
				public Throwable throwable;
		 */
		TracePoint result = new TracePoint();
		try {
			result.source = in.readShort();
			result.timestamp = in.readLong();
			byte meta = in.readByte();
			result.type = (byte)(meta & 0x1F);
			result.id = in.readShort();
//			result.level = in.readByte();
			result.threadName = in.readUTF();
			int insertCount = 0;
			if ((meta&0x20) == 0x20) {
				insertCount = in.readShort();
			}
			result.inserts = new Object[insertCount];
			for (int i=0;i<insertCount;i++) {
				result.inserts[i] = in.readUTF();
			}
			int stackCount = 0;
			if ((meta&0x40) == 0x40) {
				stackCount = in.readShort();
			}
			result.stacktrace = new String[stackCount];
			for (int i=0;i<stackCount;i++) {
				result.stacktrace[i] = in.readUTF();
			}
		} catch(Exception e) {
			result = null;
		}
		return result;
	}
}
