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
package org.eclipse.paho.client.mqttv3.internal.security;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.logging.Logger;


/**
 * An SSLSocketFactoryFactory provides a socket factory and a server socket
 * factory that then can be used to create SSL client sockets or SSL server
 * sockets.
 * <p>
 * The SSLSocketFactoryFactory is configured using IBM SSL properties, i.e.
 * properties of the format "com.ibm.ssl.propertyName", e.g.
 * "com.ibm.ssl.keyStore". The class supports multiple configurations, each
 * configuration is identified using a name or configuration ID. The
 * configuration ID with "null" is used as a default configuration. When a
 * socket factory is being created for a given configuration, properties of that
 * configuration are first picked. If a property is not defined there, then that
 * property is looked up in the default configuration. Finally, if a property
 * element is still not found, then the corresponding system property is
 * inspected, i.e. javax.net.ssl.keyStore. If the system property is not set
 * either, then the system's default value is used (if available) or an
 * exception is thrown.
 * <p>
 * The SSLSocketFacotryFactory can be reconfigured at any time. A
 * reconfiguration does not affect existing socket factories.
 * <p>
 * All properties share the same key space; i.e. the configuration ID is not
 * part of the property keys.
 * <p>
 * The methods should be called in the following order:
 * <ol>
 * <li><b>isSupportedOnJVM()</b>: to check whether this class is supported on
 * the runtime platform. Not all runtimes support SSL/TLS.</li>
 * <li><b>SSLSocketFactoryFactory()</b>: the constructor. Clients 
 * (in the same JVM) may share an SSLSocketFactoryFactory, or have one each.</li>
 * <li><b>initialize(properties, configID)</b>: to initialize this object with
 * the required SSL properties for a configuration. This may be called multiple
 * times, once for each required configuration.It may be called again to change the required SSL
 * properties for a particular configuration</li>
 * <li><b>getEnabledCipherSuites(configID)</b>: to later set the enabled
 * cipher suites on the socket [see below].</li>
 * </ol>
 * <ul>
 * <li><i>For an MQTT server:</i>

 * <ol>
 * <li><b>getKeyStore(configID)</b>: Optionally, to check that if there is no
 * keystore, then that all the enabled cipher suits are anonymous.</li>
 * <li><b>createServerSocketFactory(configID)</b>: to create an
 * SSLServerSocketFactory.</li>
 * <li><b>getClientAuthentication(configID)</b>: to later set on the
 * SSLServerSocket (itself created from the SSLServerSocketFactory) whether
 * client authentication is needed.</li>
 * </ol>
 * </li>
 * <li><i>For an MQTT client:</i>
 * <ol>
 * <li><b>createSocketFactory(configID)</b>: to create an SSLSocketFactory.</li>
 * </ol>
 * </li>
 * </ul>
 */
public class SSLSocketFactoryFactory {
	private static final String CLASS_NAME = "org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory";
	/**
	 * Property keys specific to the client).
	 */
	public static final String SSLPROTOCOL="com.ibm.ssl.protocol";
	public static final String JSSEPROVIDER="com.ibm.ssl.contextProvider";
	public static final String KEYSTORE="com.ibm.ssl.keyStore";
	public static final String KEYSTOREPWD="com.ibm.ssl.keyStorePassword";
	public static final String KEYSTORETYPE="com.ibm.ssl.keyStoreType";
	public static final String KEYSTOREPROVIDER="com.ibm.ssl.keyStoreProvider";
	public static final String KEYSTOREMGR="com.ibm.ssl.keyManager";
	public static final String TRUSTSTORE="com.ibm.ssl.trustStore";
	public static final String TRUSTSTOREPWD="com.ibm.ssl.trustStorePassword";
	public static final String TRUSTSTORETYPE="com.ibm.ssl.trustStoreType";
	public static final String TRUSTSTOREPROVIDER="com.ibm.ssl.trustStoreProvider";
	public static final String TRUSTSTOREMGR="com.ibm.ssl.trustManager";
	public static final String CIPHERSUITES="com.ibm.ssl.enabledCipherSuites";
	public static final String CLIENTAUTH="com.ibm.ssl.clientAuthentication";
	
	/**
	 * Property keys used for java system properties
	 */
	public static final String SYSKEYSTORE="javax.net.ssl.keyStore";
	public static final String SYSKEYSTORETYPE="javax.net.ssl.keyStoreType";
	public static final String SYSKEYSTOREPWD="javax.net.ssl.keyStorePassword";
	public static final String SYSTRUSTSTORE="javax.net.ssl.trustStore";
	public static final String SYSTRUSTSTORETYPE="javax.net.ssl.trustStoreType";
	public static final String SYSTRUSTSTOREPWD="javax.net.ssl.trustStorePassword";
	public static final String SYSKEYMGRALGO="ssl.KeyManagerFactory.algorithm";
	public static final String SYSTRUSTMGRALGO="ssl.TrustManagerFactory.algorithm";
	

	public static final String DEFAULT_PROTOCOL = "TLS";  // "SSL_TLS" is not supported by DesktopEE
	
	private static final String propertyKeys[] = { SSLPROTOCOL, JSSEPROVIDER,
			KEYSTORE, KEYSTOREPWD, KEYSTORETYPE, KEYSTOREPROVIDER, KEYSTOREMGR, 
			TRUSTSTORE, TRUSTSTOREPWD, TRUSTSTORETYPE, TRUSTSTOREPROVIDER, 
			TRUSTSTOREMGR, CIPHERSUITES, CLIENTAUTH};

	private Hashtable configs; // a hashtable that maps configIDs to properties.

	private Properties defaultProperties;

	private static final byte[] key = { (byte) 0x9d, (byte) 0xa7, (byte) 0xd9,
		(byte) 0x80, (byte) 0x05, (byte) 0xb8, (byte) 0x89, (byte) 0x9c };

	private static final String xorTag = "{xor}";
	
	private Logger logger = null;


	/**
	 * Not all of the JVM/Platforms support all of its
	 * security features. This method determines if is supported.
	 * 
	 * @return whether dependent classes can be instantiated on the current
	 *         JVM/platform.
	 * 
	 * @throws Error
	 *             if any unexpected error encountered whilst checking. Note
	 *             this should not be a ClassNotFoundException, which should
	 *             cause the method to return false.
	 */
	public static boolean isSupportedOnJVM() throws LinkageError, ExceptionInInitializerError {
		String requiredClassname = "javax.net.ssl.SSLServerSocketFactory";
		try {
			Class.forName(requiredClassname);
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}


	/**
	 * Create new instance of class.
	 * Constructor used by clients.
	 */
	public SSLSocketFactoryFactory() {
		configs = new Hashtable();
	}
	
	/**
	 * Create new instance of class.
	 * Constructor used by the broker.
	 * @param logger the {@link Logger} to be used
	 */
	public SSLSocketFactoryFactory(Logger logger) {
		this();
		this.logger = logger;
	}

	/**
	 * Checks whether a key belongs to the supported IBM SSL property keys.
	 * 
	 * @param key
	 * @return whether a key belongs to the supported IBM SSL property keys.
	 */
	private boolean keyValid(String key) {
		int i = 0;
		while (i < propertyKeys.length) {
			if (propertyKeys[i].equals(key)) {
				break;
			}
			++i;
		}
		return i < propertyKeys.length;
	}

	/**
	 * Checks whether the property keys belong to the supported IBM SSL property
	 * key set.
	 * 
	 * @param properties
	 * @throws IllegalArgumentException
	 *             if any of the properties is not a valid IBM SSL property key.
	 */
	private void checkPropertyKeys(Properties properties)
			throws IllegalArgumentException {
		Set keys = properties.keySet();
		Iterator i = keys.iterator();
		while (i.hasNext()) {
			String k = (String) i.next();
			if (!keyValid(k)) {
				throw new IllegalArgumentException(k + " is not a valid IBM SSL property key.");
			}
		}
	}

	/**
	 * Convert byte array to char array, where each char is constructed from two
	 * bytes.
	 * 
	 * @param b
	 *            byte array
	 * @return char array
	 */
	public static char[] toChar(byte[] b) {
		if(b==null) return null;
		char[] c= new char[b.length/2]; 
		int i=0; int j=0;
		while(i<b.length) {
			c[j++] = (char) ((b[i++] & 0xFF) + ((b[i++] & 0xFF)<<8));
		}
		return c;
	}
	
	/**
	 * Convert char array to byte array, where each char is split into two
	 * bytes.
	 * 
	 * @param c
	 *            char array
	 * @return byte array
	 */
	public static byte[] toByte(char[] c) {
		if(c==null) return null;
		byte[] b=new byte[c.length*2];
		int i=0; int j=0;
		while(j<c.length) {
			b[i++] = (byte) (c[j] & 0xFF);
			b[i++] = (byte) ((c[j++] >> 8)& 0xFF);
		}
		return b;
	}
	
	/**
	 * Obfuscates the password using a simple and not very secure XOR mechanism.
	 * This should not be used for cryptographical purpose, it's a simple
	 * scrambler to obfuscate clear-text passwords.
	 * 
	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#deObfuscate
	 * 
	 * @param password
	 *            The password to be encrypted, as a char[] array.
	 * @return An obfuscated password as a String.
	 */
	public static String obfuscate(char[] password) {
		if (password == null)
			return null;
		byte[] bytes = toByte(password);
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) ((bytes[i] ^ key[i % key.length]) & 0x00ff);
		}
		String encryptedValue = xorTag
				+ new String(SimpleBase64Encoder.encode(bytes));
		return encryptedValue;
	}

	/**
	 * The inverse operation of obfuscate: returns a cleartext password that was
	 * previously obfuscated using the XOR scrambler.
	 * 
	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#obfuscate
	 * 
	 * @param ePassword
	 *            An obfuscated password.
	 * @return An array of char, containing the clear text password.
	 */
	public static char[] deObfuscate(String ePassword) {
		if (ePassword == null)
			return null;
		byte[] bytes = null;
		try {
			bytes = SimpleBase64Encoder.decode(ePassword.substring(xorTag
					.length()));
		} catch (Exception e) {
			return null;
		}

		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) ((bytes[i] ^ key[i % key.length]) & 0x00ff);
		}
		return toChar(bytes);
	}

	/**
	 * Converts an array of ciphers into a single String.
	 * 
	 * @param ciphers
	 *            The array of cipher names.
	 * @return A string containing the name of the ciphers, separated by comma.
	 */
	public static String packCipherSuites(String[] ciphers) {
		String cipherSet=null;
		if (ciphers != null) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < ciphers.length; i++) {
				buf.append(ciphers[i]);
				if (i < ciphers.length - 1) {
					buf.append(',');
				}
			}
			cipherSet = buf.toString();
		}
		return cipherSet;
	}

	/**
	 * Inverse operation of packCipherSuites: converts a string of cipher names
	 * into an array of cipher names
	 * 
	 * @param ciphers
	 *            A list of ciphers, separated by comma.
	 * @return An array of string, each string containing a single cipher name.
	 */
	public static String[] unpackCipherSuites(String ciphers) {
		// can't use split as split is not available on all java platforms.
		if(ciphers==null) return null;
		Vector c=new Vector();
		int i=ciphers.indexOf(',');
		int j=0;
		// handle all commas.
		while(i>-1) {
			// add stuff before and up to (but not including) the comma.
			c.add(ciphers.substring(j, i));
			j=i+1; // skip the comma.
			i=ciphers.indexOf(',',j);
		}
		// add last element after the comma or only element if no comma is present.
		c.add(ciphers.substring(j));
		String[] s = new String[c.size()];
		c.toArray(s);
		return s;
	}

	/**
	 * Obfuscate any key & trust store passwords within the given properties.
	 * 
	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#obfuscate
	 * 
	 * @param p
	 *            properties
	 */
	private void convertPassword(Properties p) {
		String pw = p.getProperty(KEYSTOREPWD);
		if (pw != null && !pw.startsWith(xorTag)) {
			String epw = obfuscate(pw.toCharArray());
			p.put(KEYSTOREPWD, epw);
		}
		pw = p.getProperty(TRUSTSTOREPWD);
		if (pw != null && !pw.startsWith(xorTag)) {
			String epw = obfuscate(pw.toCharArray());
			p.put(TRUSTSTOREPWD, epw);
		}
	}

	/**
	 * Returns the properties object for configuration configID or creates a new
	 * one if required.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return the properties object for configuration configID
	 */
//	private Properties getOrCreate(String configID) {
//		Properties res = null;
//		if (configID == null) {
//			if (this.defaultProperties == null) {
//				this.defaultProperties = new Properties();
//			}
//			res = this.defaultProperties;
//		} else {
//			res = (Properties) this.configs.get(configID);
//			if (res == null) {
//				res = new Properties();
//				this.configs.put(configID, res);
//			}
//		}
//		return res;
//	}

	/**
	 * Initializes the SSLSocketFactoryFactory with the provided properties for
	 * the provided configuration.
	 * 
	 * @param props
	 *            A properties object containing IBM SSL properties that are
	 *            qualified by one or more configuration identifiers.
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @throws IllegalArgumentException
	 *             if any of the properties is not a valid IBM SSL property key.
	 */
	public void initialize(Properties props, String configID)
			throws IllegalArgumentException {
		checkPropertyKeys(props);
		// copy the properties.
		Properties p = new Properties();
		p.putAll(props);
		convertPassword(p);
		if (configID != null) {
			this.configs.put(configID, p);
		} else {
			this.defaultProperties = p;
		}
	}

	/**
	 * Merges the given IBM SSL properties into the existing configuration,
	 * overwriting existing properties. This method is used to selectively
	 * change properties for a given configuration. The method throws an
	 * IllegalArgumentException if any of the properties is not a valid IBM SSL
	 * property key.
	 * 
	 * @param props
	 *            A properties object containing IBM SSL properties
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @throws IllegalArgumentException
	 *             if any of the properties is not a valid IBM SSL property key.
	 */
	public void merge(Properties props, String configID)
			throws IllegalArgumentException {
		checkPropertyKeys(props);
		Properties p = this.defaultProperties;
		if (configID != null) {
			p = (Properties) this.configs.get(configID);
		}
		if (p == null) {
			p = new Properties();
		}
		convertPassword(props);
		p.putAll(props);
		if (configID != null) {
			this.configs.put(configID, p);
		} else {
			this.defaultProperties = p;
		}

	}

	/**
	 * Remove the configuration of a given configuration identifier.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return true, if the configuation could be removed.
	 */
	public boolean remove(String configID) {
		boolean res = false;
		if (configID != null) {
			res = this.configs.remove(configID) != null;
		} else {
			if(null != this.defaultProperties) {
				res = true;
				this.defaultProperties = null;
			}
		}
		return res;
	}

	/**
	 * Returns the configuration of the SSLSocketFactoryFactory for a given
	 * configuration. Note that changes in the property are reflected in the
	 * SSLSocketFactoryFactory.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return A property object containing the current configuration of the
	 *         SSLSocketFactoryFactory.  Note that it could be null.
	 */
	public Properties getConfiguration(String configID) {
		return (Properties) (configID == null ? this.defaultProperties
				: this.configs.get(configID));
	}

	/**
	 * @return Returns the set of configuration IDs that exist in the SSLSocketFactoryFactory.
	 */
//	public String[] getConfigurationIDs() {
//		Set s = this.configs.keySet();
//		String[] configs = new String[s.size()];
//		configs = (String[]) s.toArray(configs);
//		return configs;
//	}

	/**
	 * If the value is not null, then put it in the properties object using the
	 * key. If the value is null, then remove the entry in the properties object
	 * with the key.
	 * 
	 * @param p
	 * @param key
	 * @param value
	 */
//	private final void putOrRemove(Properties p, String key, String value) {
//		if (value == null) {
//			p.remove(key);
//		} else {
//			p.put(key, value);
//		}
//	}

	/**
	 * Sets the SSL protocol variant. If protocol is NULL then an existing value
	 * will be removed.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param protocol
	 *            One of SSL, SSLv3, TLS, TLSv1, SSL_TLS
	 */
//	public void setSSLProtocol(String configID, String protocol) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, SSLPROTOCOL, protocol);
//	}

	/**
	 * Sets the JSSE context provider. If provider is null, then an existing
	 * value will be removed.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param provider
	 *            The JSSE provider. For example "IBMJSSE2" or "SunJSSE".
	 */
//	public void setJSSEProvider(String configID, String provider) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, JSSEPROVIDER, provider);
//	}

	/**
	 * Sets the filename of the keyStore object. A null value is ignored.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param keyStore
	 *            A filename that points to a valid keystore.
	 */
//	public void setKeyStore(String configID, String keyStore) {
//		if (keyStore == null)
//			return;
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, KEYSTORE, keyStore);
//	}

	/**
	 * Sets the password that is used for the keystore. The password must be
	 * provided in plain text, but it will be stored internally in a scrambled
	 * XOR format.
	 * 
	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#obfuscate
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param password
	 *            The keystore password
	 */
//	public void setKeyStorePassword(String configID, char[] password) {
//		if (password == null)
//			return;
//		Properties p = getOrCreate(configID);
//		// convert password, using XOR-based scrambling.
//		String ePasswd = obfuscate(password);
//		for(int i=0;i<password.length;i++) {
//			password[i]=' ';
//		}
//		putOrRemove(p, KEYSTOREPWD, ePasswd);
//	}

	/**
	 * Sets the keystore provider. The corresponding provider must be installed
	 * in the system. Example values: "IBMJCE" or "IBMJCEFIPS".
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param provider
	 *            The name of a java cryptography extension
	 */
//	public void setKeyStoreProvider(String configID, String provider) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, KEYSTOREPROVIDER, provider);
//	}

	/**
	 * Sets the keystore type. For example, PKCS12, JKS or JCEKS. The types that
	 * are supported depend on the keystore provider.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param type
	 *            The keystore type
	 */
//	public void setKeyStoreType(String configID, String type) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, KEYSTORETYPE, type);
//	}

	/**
	 * Sets a custom key manager and the algorithm that it uses. The keymanager
	 * is specified in the format "algorithm|provider", for example
	 * "IbmX509|IBMJSSE2". The provider might be empty, in which case the
	 * default provider is configured with the specified algorithm. The key
	 * manager must implement the javax.net.ssl.X509KeyManager interface.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param keymanager
	 *            An algorithm, provider pair as secified above.
	 */
//	public void setCustomKeyManager(String configID, String keymanager) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, CUSTOMKEYMGR, keymanager);
//	}

	/**
	 * Sets the filename of the truststore object.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param trustStore
	 *            A filename that points to a valid truststore.
	 */
//	public void setTrustStore(String configID, String trustStore) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, TRUSTSTORE, trustStore);
//	}

	/**
	 * Sets the password that is used for the truststore. The password must be
	 * provided in plain text, but it will be stored internally in a scrambled
	 * XOR format.
	 * 
	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#obfuscate
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param password
	 *            The truststore password.
	 */
//	public void setTrustStorePassword(String configID, char[] password) {
//		Properties p = getOrCreate(configID);
//		// convert password, using XOR-based scrambling.
//		String ePasswd = obfuscate(password);
//		for(int i=0;i<password.length;i++) {
//			password[i]=' ';
//		}
//		putOrRemove(p, TRUSTSTOREPWD, ePasswd);
//	}

	/**
	 * Sets the truststore provider. The corresponding provider must be
	 * installed in the system. Example values: "IBMJCE" or "IBMJCEFIPS".
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param provider
	 *            The name of a java cryptography extension.
	 */
//	public void setTrustStoreProvider(String configID, String provider) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, TRUSTSTOREPROVIDER, provider);
//	}

	/**
	 * Sets the truststore type. For example, PKCS12, JKS or JCEKS. The types
	 * that are supported depend on the truststore provider.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param type
	 *            The truststore type.
	 */
//	public void setTrustStoreType(String configID, String type) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, TRUSTSTORETYPE, type);
//	}

	/**
	 * Sets a custom trust managers and the algorithm that it uses. The
	 * trustmanager is specified in the format "algorithm|provider", for example
	 * "IbmX509|IBMJSSE2". The provider might be empty, in which case the
	 * default provider is configured with the specified algorithm. The trust
	 * manager must implement the javax.net.ssl.X509TrustManager interface.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param trustmanager
	 *            An algorithm, provider pair as secified above.
	 */
//	public void setCustomTrustManager(String configID, String trustmanager) {
//		Properties p = getOrCreate(configID);
//		putOrRemove(p, CUSTOMTRUSTMGR, trustmanager);
//	}

	/**
	 * Sets the list of enabled ciphers. For a list of acceptable values, see
	 * the documentation of the underlying JSSE.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param ciphers
	 *            An array of cipher suite names such as
	 *            SSL_RSA_WITH_AES_128_CBC_SHA.
	 */
//	public void setEnabledCipherSuites(String configID, String[] ciphers) {
//		if (ciphers == null)
//			return;
//		Properties p = getOrCreate(configID);
//		String cipherSet = packCipherSuites(ciphers);
//		putOrRemove(p, CIPHERSUITES, cipherSet);
//	}

	/**
	 * Specifies whether the client is required to provide a valid certificate
	 * to the client during SSL negotiation.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param clientAuth
	 *            true, if clients are required to authenticate, false
	 *            otherwise.
	 */
//	public void setClientAuthentication(String configID, boolean clientAuth) {
//		Properties p = getOrCreate(configID);
//		p.put(CLIENTAUTH, Boolean.toString(clientAuth));
//	}

	/**
	 * Returns the property of a given key or null if it doesn't exist. It first
	 * scans the indicated configuration, then the default configuration, then
	 * the system properties.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param ibmKey
	 * @param sysProperty
	 *            The key for the System property.
	 * @return the property of a given key or null if it doesn't exist.
	 */
	private String getProperty(String configID, String ibmKey, String sysProperty) {
		String res = null;
		res = getPropertyFromConfig(configID, ibmKey);
		if ( res != null ) {
			return res;
		}
		// scan system property, if it exists.
		if (sysProperty != null) {
			res = System.getProperty(sysProperty);
		}
		return res;
	}

	/**
	 * Returns the property of a given key or null if it doesn't exist. It first
	 * scans the indicated configuration, then the default configuration
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @param ibmKey
	 * @return the property of a given key or null if it doesn't exist. It first
	 *         scans the indicated configuration, then the default configuration
	 */
	private String getPropertyFromConfig(String configID, String ibmKey) {
		String res = null;
		Properties p =null;
		if(configID!=null) {;
			p = (Properties) configs.get(configID);
		}
		if (p != null) {
			res = p.getProperty(ibmKey);
			if (res != null)
				return res;
		}
		// not found in config. try default properties.
		p = (Properties) this.defaultProperties;
		if (p != null) {
			res = p.getProperty(ibmKey);
			if (res != null)
				return res;
		}
		return res;
	}

	/**
	 * Gets the SSL protocol variant of the indicated configuration or the
	 * default configuration.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The SSL protocol variant.
	 */
	public String getSSLProtocol(String configID) {
		return getProperty(configID, SSLPROTOCOL, null);
	}

	/**
	 * Gets the JSSE provider of the indicated configuration
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The JSSE provider.
	 */
	public String getJSSEProvider(String configID) {
		return getProperty(configID, JSSEPROVIDER, null);
	}

//	/**
//	 * Get the XPD Keystore if running on the XPD platform (otherwise null).
//	 * 
//	 * @return the XPD Keystore if running on the XPD platform (otherwise null).
//	 * @throws MqttDirectException
//	 */
//	private KeyStore getXPDKeystore() throws MqttDirectException {
//		KeyStore keyStore = null;
//		try {
//			Class secPlatClass = Class.forName("com.ibm.rcp.security.auth.SecurePlatform");
//			Method m = secPlatClass.getMethod("getKeyStore", null);
//			Object secPlat = m.invoke(null,null); // getKeyStore is static
//			m = secPlatClass.getMethod("isLoggedIn", null);
//			Boolean b = (Boolean) m.invoke(secPlat, null);
//			if (b.booleanValue()) {
//				// login to secure platform was done.
//				m = secPlatClass.getMethod("getKeyStore", null);
//				keyStore = (KeyStore) m.invoke(secPlat, null);
//			}
//		} catch (ClassNotFoundException e) {
//			/*
//			 * DEVELOPER NOTE: This is not an error. This means that we are not
//			 * running on XPD runtime and therefore we can not get XPD keystore.
//			 * [Next step for the caller, is try to get the keystore from System
//			 * properties (see getKeyStore() method).]
//			 */
//		} catch (IllegalAccessException e) {
//			Object[] inserts = { e.getLocalizedMessage() };
//			throw new MqttSSLInitException(3026, inserts, e);
//		} catch (SecurityException e) {
//			Object[] inserts = { e.getLocalizedMessage() };
//			throw new MqttSSLInitException(3026, inserts, e);
//		} catch (NoSuchMethodException e) {
//			Object[] inserts = { e.getLocalizedMessage() };
//			throw new MqttSSLInitException(3026, inserts, e);
//		} catch (IllegalArgumentException e) {
//			Object[] inserts = { e.getLocalizedMessage() };
//			throw new MqttSSLInitException(3026, inserts, e);
//		} catch (InvocationTargetException e) {
//			Object[] inserts = { e.getLocalizedMessage() };
//			throw new MqttSSLInitException(3026, inserts, e);
//		}
//		return keyStore;
//	}

	/**
	 * Gets the name of the keystore file that is used.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The name of the file that contains the keystore.
	 */
	public String getKeyStore(String configID) { //throws MqttDirectException {
		String ibmKey = KEYSTORE;
		String sysProperty = SYSKEYSTORE;
		
		String res = null;
		res = getPropertyFromConfig(configID, ibmKey);
		if ( res != null ) {
			return res;
		}
		
//		// check for the XPD keystore here
//		if ( ibmKey != null && ibmKey.equals(KEYSTORE) ) {
//			KeyStore keyStore = getXPDKeystore();
//			if (keyStore != null)
//				return res = "Lotus Expeditor";
//		}
		
		// scan system property, if it exists.
		if (sysProperty != null) {
			res = System.getProperty(sysProperty);
		}
		
		return res;
	}

	/**
	 * Gets the plain-text password that is used for the keystore.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The password in plain text.
	 */
	public char[] getKeyStorePassword(String configID) {
		String pw = getProperty(configID, KEYSTOREPWD, SYSKEYSTOREPWD);
		char[] r=null;
		if (pw!=null) {
			if (pw.startsWith(xorTag)) {
				r = deObfuscate(pw);
			} else {
				r = pw.toCharArray();
			}
		}
		return r;
	}

	/**
	 * Gets the type of keystore.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The keystore type.
	 */
	public String getKeyStoreType(String configID) {
		return getProperty(configID, KEYSTORETYPE, SYSKEYSTORETYPE);
	}

	/**
	 * Gets the keystore provider.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The name of the keystore provider.
	 */
	public String getKeyStoreProvider(String configID) {
		return getProperty(configID, KEYSTOREPROVIDER, null);
	}

	/**
	 * Gets the key manager algorithm that is used.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The key manager algorithm.
	 */
	public String getKeyManager(String configID) {
		return getProperty(configID, KEYSTOREMGR, SYSKEYMGRALGO);
	}
	
	/**
	 * Gets the name of the truststore file that is used.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The name of the file that contains the truststore.
	 */
	public String getTrustStore(String configID) {
		return getProperty(configID, TRUSTSTORE, SYSTRUSTSTORE);
	}

	/**
	 * Gets the plain-text password that is used for the truststore.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The password in plain text.
	 */
	public char[] getTrustStorePassword(String configID) {
		String pw = getProperty(configID, TRUSTSTOREPWD, SYSTRUSTSTOREPWD);
		char[] r=null;
		if (pw!=null) {
			if(pw.startsWith(xorTag)) {
				r = deObfuscate(pw);
			} else {
				r = pw.toCharArray();
			}
		}
		return r;
	}

	/**
	 * Gets the type of truststore.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The truststore type.
	 */
	public String getTrustStoreType(String configID) {
		return getProperty(configID, TRUSTSTORETYPE, null);
	}

	/**
	 * Gets the truststore provider.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The name of the truststore provider.
	 */
	public String getTrustStoreProvider(String configID) {
		return getProperty(configID, TRUSTSTOREPROVIDER, null);
	}

	/**
	 * Gets the trust manager algorithm that is used.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return The trust manager algorithm.
	 */
	public String getTrustManager(String configID) {
		return getProperty(configID, TRUSTSTOREMGR, SYSTRUSTMGRALGO);
	}
	
	/**
	 * Returns an array with the enabled ciphers.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return an array with the enabled ciphers
	 */
	public String[] getEnabledCipherSuites(String configID) {
		String ciphers = getProperty(configID, CIPHERSUITES, null);
		String[] res = unpackCipherSuites(ciphers);
		return res;
	}

	/**
	 * Returns whether client authentication is required.
	 * 
	 * @param configID
	 *            The configuration identifier for selecting a configuration or
	 *            null for the default configuration.
	 * @return true, if clients are required to authenticate, false otherwise.
	 */
	public boolean getClientAuthentication(String configID) {
		String auth = getProperty(configID, CLIENTAUTH, null);
		boolean res = false;
		if (auth != null) {
			res = Boolean.valueOf(auth).booleanValue();
		}
		return res;
	}

	/**
	 * Initializes key- and truststore. Returns an SSL context factory. If no
	 * SSLProtocol is already set, uses DEFAULT_PROTOCOL
	 * 
	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#DEFAULT_PROTOCOL
	 * 
	 * @param configID
	 *            The configuration ID
	 * @return An SSL context factory.
	 * @throws MqttDirectException
	 */
	private SSLContext getSSLContext(String configID)
			throws MqttSecurityException{
		final String METHOD_NAME = "getSSLContext";
		SSLContext ctx = null;
		
		String protocol = getSSLProtocol(configID);
		if (protocol == null) {
			protocol = DEFAULT_PROTOCOL;
		}
		if (logger != null) {
			// 12000 "SSL initialization: configID = {0}, protocol = {1}"
			logger.fine(CLASS_NAME, METHOD_NAME, "12000", new Object[] {configID!=null ? configID : "null (broker defaults)", 
					protocol});
		}
		
		String provider = getJSSEProvider(configID);
		try {
			if (provider == null) {
				ctx = SSLContext.getInstance(protocol);
			} else {
				ctx = SSLContext.getInstance(protocol, provider);
			}
			if (logger != null) {
				// 12001 "SSL initialization: configID = {0}, provider = {1}"
				logger.fine(CLASS_NAME, METHOD_NAME, "12001", new Object[] {configID!=null ? configID : "null (broker defaults)", 
						ctx.getProvider().getName()});
			}
			
			String keyStoreName = getProperty(configID, KEYSTORE, null);
			KeyStore keyStore=null;
			KeyManagerFactory keyMgrFact=null;
			KeyManager[] keyMgr=null;
//			if(keyStoreName==null) {
//				// try to instantiate XPD keyStore.
//				keyStore=getXPDKeystore();
//				if (logger != null) {
//					if (keyStore == null) {
//						// 12002 "SSL initialization: configID = {0}, XPD keystore not available"
//						logger.fine(CLASS_NAME, METHOD_NAME, "12002", new Object[]{configID!=null ? configID : "null (broker defaults)"});
//					} else {
//						// 12003 "SSL initialization: configID = {0}, XPD keystore available"
//						logger.fine(CLASS_NAME, METHOD_NAME, "12003", new Object[]{configID!=null ? configID : "null (broker defaults)"});
//					}
//				}
//			}
			
			if(keyStore==null) {
				if(keyStoreName==null) {
					/*
					 * No keystore in config, XPD keystore not available. Try to
					 * get config from system properties.
					 */
					keyStoreName = getProperty(configID, KEYSTORE, SYSKEYSTORE);
				}
				if (logger != null) {
					// 12004 "SSL initialization: configID = {0}, keystore = {1}"
					logger.fine(CLASS_NAME, METHOD_NAME, "12004", new Object[]{configID!=null ? configID : "null (broker defaults)", 
							keyStoreName!=null ? keyStoreName : "null"});
				}
				
				char[] keyStorePwd=getKeyStorePassword(configID);
				if (logger != null) {
					// 12005 "SSL initialization: configID = {0}, keystore password = {1}"
					logger.fine(CLASS_NAME, METHOD_NAME, "12005", new Object[]{configID!=null ? configID : "null (broker defaults)", 
							keyStorePwd!=null ? obfuscate(keyStorePwd) : "null"});
				}
				
				String keyStoreType=getKeyStoreType(configID);
				if(keyStoreType==null) {
					keyStoreType = KeyStore.getDefaultType();
				}
				if (logger != null) {
					// 12006 "SSL initialization: configID = {0}, keystore type = {1}"
					logger.fine(CLASS_NAME, METHOD_NAME, "12006", new Object[]{configID!=null ? configID : "null (broker defaults)", 
							keyStoreType!=null ? keyStoreType : "null"});
				}
				
				String keyMgrAlgo = KeyManagerFactory.getDefaultAlgorithm();
				String keyMgrProvider = getKeyStoreProvider(configID);
				String keyManager = getKeyManager(configID);
				if (keyManager != null) {
					keyMgrAlgo = keyManager;
				}
				
				if(keyStoreName!=null && keyStoreType!=null  && keyMgrAlgo!=null) {
					try {
						keyStore=KeyStore.getInstance(keyStoreType);
						keyStore.load(new FileInputStream(keyStoreName), keyStorePwd);
						if(keyMgrProvider!=null) {
							keyMgrFact = KeyManagerFactory.getInstance(keyMgrAlgo, keyMgrProvider);
						} else {
							keyMgrFact = KeyManagerFactory.getInstance(keyMgrAlgo);
						}
						if (logger != null) {
							// 12010 "SSL initialization: configID = {0}, keystore manager algorithm = {1}"
							logger.fine(CLASS_NAME, METHOD_NAME, "12010", new Object[]{configID!=null ? configID : "null (broker defaults)", 
									keyMgrAlgo!=null ? keyMgrAlgo : "null"});
							// 12009 "SSL initialization: configID = {0}, keystore manager provider = {1}"
							logger.fine(CLASS_NAME, METHOD_NAME, "12009", new Object[]{configID!=null ? configID : "null (broker defaults)", 
									keyMgrFact.getProvider().getName()});				
						}
						keyMgrFact.init(keyStore, keyStorePwd);
						keyMgr=keyMgrFact.getKeyManagers();
					} catch (KeyStoreException e) {
						throw new MqttSecurityException(e);
					} catch (CertificateException e) {
						throw new MqttSecurityException(e);
					} catch (FileNotFoundException e) {
						throw new MqttSecurityException(e);
					} catch (IOException e) {
						throw new MqttSecurityException(e);
					} catch (UnrecoverableKeyException e) {
						throw new MqttSecurityException(e);
					}
				}
			}
			// keystore loaded, keymanagers instantiated if possible
			// now the same for the truststore.
			String trustStoreName = getTrustStore(configID);
			if (logger != null) {
				// 12011 "SSL initialization: configID = {0}, truststore = {1}"
				logger.fine(CLASS_NAME, METHOD_NAME, "12011", new Object[]{configID!=null ? configID : "null (broker defaults)", 
						trustStoreName!=null ? trustStoreName : "null"});
			}
			KeyStore trustStore=null;
			TrustManagerFactory trustMgrFact=null;
			TrustManager[] trustMgr=null;
			char[] trustStorePwd=getTrustStorePassword(configID);
			if (logger != null) {
				// 12012 "SSL initialization: configID = {0}, truststore password = {1}"
				logger.fine(CLASS_NAME, METHOD_NAME, "12012", new Object[]{configID!=null ? configID : "null (broker defaults)", 
						trustStorePwd!=null ? obfuscate(trustStorePwd) : "null"});
			}
			String trustStoreType=getTrustStoreType(configID);
			if(trustStoreType==null) {
				trustStoreType = KeyStore.getDefaultType();
			}
			if (logger != null) {
				// 12013 "SSL initialization: configID = {0}, truststore type = {1}"
				logger.fine(CLASS_NAME, METHOD_NAME, "12013", new Object[]{configID!=null ? configID : "null (broker defaults)", 
						trustStoreType!=null ? trustStoreType : "null"});
			}
			
			String trustMgrAlgo = TrustManagerFactory.getDefaultAlgorithm();
			String trustMgrProvider = getTrustStoreProvider(configID);
			String trustManager = getTrustManager(configID);
			if (trustManager != null) {
				trustMgrAlgo = trustManager;
			}
					
			if(trustStoreName!=null && trustStoreType!=null && trustMgrAlgo!=null) {
				try {
					trustStore=KeyStore.getInstance(trustStoreType);
					trustStore.load(new FileInputStream(trustStoreName), trustStorePwd);
					if(trustMgrProvider!=null) {
						trustMgrFact = TrustManagerFactory.getInstance(trustMgrAlgo, trustMgrProvider);
					} else {
						trustMgrFact = TrustManagerFactory.getInstance(trustMgrAlgo);
					}
					if (logger != null) {
						
						// 12017 "SSL initialization: configID = {0}, truststore manager algorithm = {1}"
						logger.fine(CLASS_NAME, METHOD_NAME, "12017", new Object[]{configID!=null ? configID : "null (broker defaults)", 
								trustMgrAlgo!=null ? trustMgrAlgo : "null"});
						
						// 12016 "SSL initialization: configID = {0}, truststore manager provider = {1}"
						logger.fine(CLASS_NAME, METHOD_NAME, "12016", new Object[]{configID!=null ? configID : "null (broker defaults)", 
								trustMgrFact.getProvider().getName()});		
					}
					trustMgrFact.init(trustStore);
					trustMgr=trustMgrFact.getTrustManagers();
				} catch (KeyStoreException e) {
					throw new MqttSecurityException(e);
				} catch (CertificateException e) {
					throw new MqttSecurityException(e);
				} catch (FileNotFoundException e) {
					throw new MqttSecurityException(e);
				} catch (IOException e) {
					throw new MqttSecurityException(e);
				} 
			}
			// done.
			ctx.init(keyMgr, trustMgr, null);
		} catch (NoSuchAlgorithmException e) {
			throw new MqttSecurityException(e);
		} catch (NoSuchProviderException e) {
			throw new MqttSecurityException(e);
		} catch (KeyManagementException e) {
			throw new MqttSecurityException(e);
		}
		return ctx;
	}

//	/**
//	 * Returns an SSL server socket factory for the given configuration. If no
//	 * SSLProtocol is already set, uses DEFAULT_PROTOCOL. Throws
//	 * IllegalArgumentException if the server socket factory could not be
//	 * created due to underlying configuration problems.
//	 * 
//	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#DEFAULT_PROTOCOL
//	 * 
//	 * @param configID
//	 *            The configuration identifier for selecting a configuration.
//	 * @return An SSLServerSocketFactory
//	 * @throws MqttDirectException
//	 */
//	public SSLServerSocketFactory createServerSocketFactory(String configID)
//			throws MqttDirectException {
//		final String METHOD_NAME = "createServerSocketFactory";
//		SSLContext ctx = getSSLContext(configID);
//		if (logger != null) {
//			// 12018 "SSL initialization: configID = {0}, application-enabled cipher suites = {1}"
//			logger.fine(CLASS_NAME, METHOD_NAME, "12018", new Object[]{configID!=null ? configID : "null (broker defaults)", 
//					getEnabledCipherSuites(configID)!=null ? getProperty(configID, CIPHERSUITES, null) : "null (using platform-enabled cipher suites)"});
//			
//			// 12019 "SSL initialization: configID = {0}, client authentication = {1}"
//			logger.fine(CLASS_NAME, METHOD_NAME, "12019", new Object[]{configID!=null ? configID : "null (broker defaults)", 
//					new Boolean (getClientAuthentication(configID)).toString()});
//		}
//		
//		return ctx.getServerSocketFactory();
//	}

	/**
	 * Returns an SSL socket factory for the given configuration. If no
	 * SSLProtocol is already set, uses DEFAULT_PROTOCOL. Throws
	 * IllegalArgumentException if the socket factory could not be created due
	 * to underlying configuration problems.
	 * 
	 * @see org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory#DEFAULT_PROTOCOL
	 * @param configID
	 *            The configuration identifier for selecting a configuration.
	 * @return An SSLSocketFactory
	 * @throws MqttSecurityException if an error occurs whilst creating the {@link SSLSocketFactory}
	 */
	public SSLSocketFactory createSocketFactory(String configID) 
			throws MqttSecurityException {
		final String METHOD_NAME = "createSocketFactory";
		SSLContext ctx = getSSLContext(configID);
		if (logger != null) {
			// 12020 "SSL initialization: configID = {0}, application-enabled cipher suites = {1}"
			logger.fine(CLASS_NAME, METHOD_NAME, "12020", new Object[]{configID!=null ? configID : "null (broker defaults)", 
					getEnabledCipherSuites(configID)!=null ? getProperty(configID, CIPHERSUITES, null) : "null (using platform-enabled cipher suites)"});
		}
			
		return ctx.getSocketFactory();
	}

}
