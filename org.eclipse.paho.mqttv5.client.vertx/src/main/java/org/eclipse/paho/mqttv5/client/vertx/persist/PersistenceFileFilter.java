package org.eclipse.paho.mqttv5.client.vertx.persist;

import java.io.File;
import java.io.FileFilter;

public class PersistenceFileFilter implements FileFilter{
	
	private final String fileExtension;
	
	public PersistenceFileFilter(String fileExtension){
		this.fileExtension = fileExtension;
	}

	public boolean accept(File pathname) {
		return pathname.getName().endsWith(fileExtension);
	}

}
