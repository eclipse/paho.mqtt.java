package org.eclipse.paho.client.mqttv3.persist;

import java.io.File;
import java.io.FileFilter;

public class PersistanceFileFilter implements FileFilter{
	
	private final String fileExtension;
	
	public PersistanceFileFilter(String fileExtension){
		this.fileExtension = fileExtension;
	}

	public boolean accept(File pathname) {
		return pathname.getName().endsWith(fileExtension);
	}

}
