package org.eclipse.paho.client.mqttv3.persist;

import java.io.File;
import java.io.FilenameFilter;

public class PersistanceFileNameFilter implements FilenameFilter{
	
	private final String fileExtension;
	
	public PersistanceFileNameFilter(String fileExtension){
		this.fileExtension = fileExtension;
	}

	public boolean accept(File dir, String name) {
		return name.endsWith(fileExtension);
	}

}
