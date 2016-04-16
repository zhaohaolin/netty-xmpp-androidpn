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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSL Trust Manager Factory class.
 * 
 * @author Sehwan Noh (sehnoh@gmail.com)
 */
public abstract class SSLTrustManagerFactory {
	
	private static final Logger	LOG	= LoggerFactory
											.getLogger(SSLTrustManagerFactory.class);
	
	public final static TrustManager[] getTrustManagers(String storeType,
			String truststore, String trustpass)
			throws NoSuchAlgorithmException, KeyStoreException, IOException,
			CertificateException {
		TrustManager[] trustManagers;
		if (truststore == null) {
			trustManagers = null;
		} else {
			TrustManagerFactory trustFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			if (trustpass == null) {
				trustpass = "";
			}
			KeyStore keyStore = KeyStore.getInstance(storeType);
			keyStore.load(new FileInputStream(truststore),
					trustpass.toCharArray());
			trustFactory.init(keyStore);
			trustManagers = trustFactory.getTrustManagers();
		}
		return trustManagers;
	}
	
	public final static TrustManager[] getTrustManagers(KeyStore truststore,
			String trustpass) {
		TrustManager[] trustManagers;
		try {
			if (truststore == null) {
				trustManagers = null;
			} else {
				TrustManagerFactory trustFactory = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				
				trustFactory.init(truststore);
				
				trustManagers = trustFactory.getTrustManagers();
			}
		} catch (KeyStoreException e) {
			trustManagers = null;
			LOG.error("SSLTrustManagerFactory startup problem.", e);
		} catch (NoSuchAlgorithmException e) {
			trustManagers = null;
			LOG.error("SSLTrustManagerFactory startup problem.", e);
		}
		return trustManagers;
	}
	
}
