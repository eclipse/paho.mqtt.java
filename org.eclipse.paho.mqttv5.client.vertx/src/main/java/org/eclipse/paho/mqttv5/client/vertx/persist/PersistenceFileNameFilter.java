package org.eclipse.paho.mqttv5.client.vertx.persist;

import java.io.File;
import java.io.FilenameFilter;

public class PersistenceFileNameFilter implements FilenameFilter{
	
	private final String fileExtension;
	
	public PersistenceFileNameFilter(String fileExtension){
		this.fileExtension = fileExtension;
	}

	public boolean accept(File dir, String name) {
		return name.endsWith(fileExtension);
	}

}
