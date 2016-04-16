/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.xmpp.push.androidpn.server.xmpp.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for SSL settings.
 * 
 * @author Sehwan Noh (sehnoh@gmail.com)
 */
public class SSLConfig {
	
	private static final Logger	LOG			= LoggerFactory
													.getLogger(SSLConfig.class);
	
	private SSLContext			sslContext;
	private String				storeType;
	private KeyStore			keyStore;
	private String				keyStoreLocation;
	private String				keyPass;
	private KeyStore			trustStore;
	private String				trustStoreLocation;
	private String				trustPass;
	private URL					classPath	= SSLConfig.class.getResource("/");
	
	public void init() throws IOException {
		// Load keystore
		if (null == keyStore) {
			try {
				keyStore = KeyStore.getInstance(storeType);
				keyStore.load(new FileInputStream(keyStoreLocation),
						keyPass.toCharArray());
			} catch (Exception e) {
				LOG.warn("SSLConfig startup problem.\n" + "  storeType: ["
						+ storeType + "]\n" + "  keyStoreLocation: ["
						+ keyStoreLocation + "]\n" + "  keyPass: [" + keyPass
						+ "]", e);
				keyStore = null;
			}
			
			if (keyStore == null) {
				throw new IOException();
			}
		}
		// Load truststore
		if (null == trustStore) {
			try {
				trustStore = KeyStore.getInstance(storeType);
				trustStore.load(new FileInputStream(trustStoreLocation),
						trustPass.toCharArray());
				
			} catch (Exception e) {
				try {
					trustStore = KeyStore.getInstance(storeType);
					trustStore.load(null, trustPass.toCharArray());
				} catch (Exception ex) {
					LOG.warn("SSLConfig startup problem.\n" + "  storeType: ["
							+ storeType + "]\n" + "  trustStoreLocation: ["
							+ trustStoreLocation + "]\n" + "  trustPass: ["
							+ trustPass + "]", e);
					trustStore = null;
				}
			}
		}
		
		// Init factory
		if (null == sslContext) {
			try {
				sslContext = SSLContext.getInstance("TLS");
				
				KeyManagerFactory keyFactory = KeyManagerFactory
						.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				keyFactory.init(keyStore, getKeyPassword().toCharArray());
				
				TrustManagerFactory c2sTrustFactory = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				c2sTrustFactory.init(trustStore);
				
				sslContext.init(keyFactory.getKeyManagers(),
						c2sTrustFactory.getTrustManagers(),
						new java.security.SecureRandom());
				
			} catch (Exception e) {
				LOG.warn("SSLConfig factory setup problem." + "  storeType: ["
						+ storeType + "]\n" + "  keyStoreLocation: ["
						+ keyStoreLocation + "]\n" + "  keyPass: [" + keyPass
						+ "]\n" + "  trustStoreLocation: ["
						+ trustStoreLocation + "]\n" + "  trustPass: ["
						+ trustPass + "]", e);
				keyStore = null;
				trustStore = null;
			}
		}
	}
	
	public SSLContext getSslContext() {
		return sslContext;
	}
	
	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}
	
	public String getKeyStoreLocation() {
		return keyStoreLocation;
	}
	
	public void setKeyStoreLocation(String keyStoreLocation) {
		this.keyStoreLocation = classPath.getPath() + File.separator
				+ keyStoreLocation;
	}
	
	public String getKeyPass() {
		return keyPass;
	}
	
	public void setKeyPass(String keyPass) {
		this.keyPass = keyPass;
	}
	
	public KeyStore getTrustStore() {
		return trustStore;
	}
	
	public void setTrustStore(KeyStore trustStore) {
		this.trustStore = trustStore;
	}
	
	public String getTrustStoreLocation() {
		return trustStoreLocation;
	}
	
	public void setTrustStoreLocation(String trustStoreLocation) {
		this.trustStoreLocation = classPath.getPath() + File.separator
				+ trustStoreLocation;
	}
	
	public String getTrustPass() {
		return trustPass;
	}
	
	public void setTrustPass(String trustPass) {
		this.trustPass = trustPass;
	}
	
	public URL getClassPath() {
		return classPath;
	}
	
	public void setClassPath(URL classPath) {
		this.classPath = classPath;
	}
	
	public void setStoreType(String storeType) {
		this.storeType = storeType;
	}
	
	public void setKeyStore(KeyStore keyStore) {
		this.keyStore = keyStore;
	}
	
	public SSLContext getc2sSSLContext() {
		return sslContext;
	}
	
	public String getKeystoreLocation() {
		return keyStoreLocation;
	}
	
	public String getc2sTruststoreLocation() {
		return trustStoreLocation;
	}
	
	public String getStoreType() {
		return storeType;
	}
	
	public KeyStore getKeyStore() {
		return keyStore;
	}
	
	public String getKeyPassword() {
		return keyPass;
	}
	
	public KeyStore getc2sTrustStore() throws IOException {
		if (trustStore == null) {
			throw new IOException();
		}
		return trustStore;
	}
	
	public String getc2sTrustPassword() {
		return trustPass;
	}
	
}
