package com.hikvision.push.androidpn.server;

import com.hikvision.push.androidpn.server.xmpp.ssl.SSLConfig;

/**
 * 
 * 
 * @version $Id: AndroidpnConfig, v 0.1 2016年4月9日 上午11:30:17 Exp $
 */
public class AndroidpnConfig {
	
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setResourceName(String resourceName) {
		ResourceName = resourceName;
	}

	public void setXmppSslStoreType(String xmppSslStoreType) {
		this.xmppSslStoreType = xmppSslStoreType;
	}

	public void setXmppSslKeystore(String xmppSslKeystore) {
		this.xmppSslKeystore = xmppSslKeystore;
	}

	public void setXmppSslKeypass(String xmppSslKeypass) {
		this.xmppSslKeypass = xmppSslKeypass;
	}

	public void setXmppSslTrustpass(String xmppSslTrustpass) {
		this.xmppSslTrustpass = xmppSslTrustpass;
	}

	private String	serverName;
	private String	apiKey;
	private String	ResourceName;
	private String	xmppSslStoreType;
	private String	xmppSslKeystore;
	private String	xmppSslKeypass;
	private String	xmppSslTrustpass;
	
	public void init(String serName, String api, String resName,
			String sslStoreType, String sslKeystore, String sslKeypass,
			String sslTrustpass) {
		serverName = serName;
		apiKey = api;
		ResourceName = resName;
		xmppSslStoreType = sslStoreType;
		xmppSslKeystore = sslKeystore;
		xmppSslKeypass = sslKeypass;
		xmppSslTrustpass = sslTrustpass;
	}
	
	public void convert(SSLConfig conf) {
		conf.setStoreType(xmppSslStoreType);
		
		conf.setKeyStoreLocation(xmppSslKeystore);
		conf.setKeyPass(xmppSslKeypass);
		
		conf.setTrustStoreLocation(xmppSslTrustpass);
		conf.setTrustPass(xmppSslTrustpass);
	}
	
	public String getServerName() {
		return serverName;
	}
	
	public String getApiKey() {
		return apiKey;
	}
	
	public String getResourceName() {
		return ResourceName;
	}
	
	public String getXmppSslStoreType() {
		return xmppSslStoreType;
	}
	
	public String getXmppSslKeystore() {
		return xmppSslKeystore;
	}
	
	public String getXmppSslKeypass() {
		return xmppSslKeypass;
	}
	
	public String getXmppSslTrustpass() {
		return xmppSslTrustpass;
	}
	
}
