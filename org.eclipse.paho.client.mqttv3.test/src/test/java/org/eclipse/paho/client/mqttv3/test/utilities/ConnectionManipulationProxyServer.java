package org.eclipse.paho.client.mqttv3.test.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.test.automaticReconnect.AutomaticReconnectTest;

public class ConnectionManipulationProxyServer implements Runnable {
	static final Class<?> cclass = AutomaticReconnectTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);
	private int localPort;
	private String host;
	private int remotePort;
	private Thread proxyThread;
	private Object enableLock = new Object();
	private boolean enableProxy = true;
	private boolean running = true;
	Socket client = null, server = null;

	public ConnectionManipulationProxyServer(String host, int remotePort, int localPort) {
		this.localPort = localPort;
		this.remotePort = remotePort;
		this.host = host;
		proxyThread = new Thread(this);

	}
	
	public void startProxy(){
		synchronized (enableLock) {
			enableProxy = true;
		}
		running = true;
		proxyThread.start();
	}
	
	public void enableProxy(){
		synchronized (enableLock) {
			enableProxy = true;
		}
		running  = true;
		if(proxyThread.isAlive() == false){
			proxyThread.start();
		}
	}
	
	public void disableProxy(){
		synchronized (enableLock) {
			enableProxy = false;
		}
		try {
			client.close();
			server.close();
		} catch (IOException e) {
		}
	}
	
	public void stopProxy(){
		synchronized (enableLock) {
			enableProxy = false;
		}
		running = false;
		try {
			client.close();
			server.close();
		} catch (IOException e) {
		}	
	}

	@Override
	public void run() {
		try {
			//Create the Listening Server
			ServerSocket serverSocket = new ServerSocket(localPort);
			final byte[] request = new byte[1024];
			byte[] reply = new byte[4096];
			boolean canIrun = true;
			while(running){
				synchronized (enableLock) {
					canIrun = enableProxy;
				}
				while(canIrun){
					log.fine("Waiting for incoming connection");
					
					try {
						// Wait for a connection on the local Port
						client = serverSocket.accept();
						log.fine("Proxy: Client Opened Connection to Proxy...");
						
						final InputStream streamFromClient = client.getInputStream();
						final OutputStream streamToClient = client.getOutputStream();
						
						// Attempt to make a connection to the real server
						try {
							server = new Socket(host, remotePort);
						} catch (IOException ex){
							log.warning("ConnectionManipulationProxyServer cannot connect to " + host + ":" + remotePort);
							client.close();
							continue;
						}
						log.fine("Proxy: Proxy Connected to Server");
						
						// Get Server Streams
						final InputStream streamFromServer = server.getInputStream();
						final OutputStream streamToServer = server.getOutputStream();
						
						Thread thread = new Thread() {
							public void run() {
								int bytesRead;
								try {
									while((bytesRead = streamFromClient.read(request)) != -1) {
										streamToServer.write(request, 0, bytesRead);
										streamToServer.flush();
									}
								} catch (IOException ex){
									//log.warning("Proxy: 1 Connection lost: " + ex.getMessage());
									try {
										client.close();
										server.close();
									} catch (IOException e) {

									}
									
								}
							}
						};
					
					thread.start();
					
					// Read the Servers responses and pass them back to the client
					int bytesRead;
					try {
							while ((bytesRead = streamFromServer.read(reply))!= -1){
								streamToClient.write(reply, 0, bytesRead);
								streamToClient.flush();
							}
						
					 } catch (IOException ex){
						 //log.warning("Proxy: 2 Connection lost: " + ex.getMessage());
						 client.close();
						 server.close();
					}
					
					streamToClient.close();
				
					
					}  catch (IOException ex) {
						//log.warning("Proxy: 3 Connection lost: " + ex.getMessage());
					} finally {
						try {
							if(server != null){
								server.close();
							}
							if(client != null){
								client.close();
							}
						} catch(IOException ex) {
							//log.warning("Proxy: 4 Connection lost: " + ex.getMessage());
						}
					}
				
	
			
				}
			}
			log.fine("Proxy: Proxy Thread finishing..");
			serverSocket.close();
		} catch(IOException ex) {
			log.warning("Proxy: 5 Thread Connection lost: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		
	}
}
