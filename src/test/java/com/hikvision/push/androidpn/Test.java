/*
 * CopyRight (c) 2012-2015 Hikvision Co, Ltd. All rights reserved. Filename:
 * Test.java Creator: joe.zhao(zhaohaolin@hikvision.com.cn) Create-Date:
 * 下午6:46:16
 */
package com.hikvision.push.androidpn;

import java.io.IOException;

import com.hikvision.push.androidpn.server.XmppServer;
import com.hikvision.push.androidpn.server.xmpp.auth.AuthProxy;
import com.hikvision.push.androidpn.server.xmpp.session.ConnectManager;

/**
 * TODO
 * 
 * @author joe.zhao(zhaohaolin@hikvision.com.cn)
 * @version $Id: Test, v 0.1 2015年4月9日 下午6:46:16 Exp $
 */
public class Test {
	
	private final static String	ip					= "127.0.0.1";
	private final static int	port				= 8223;
	
	private final static String	apiKey				= "1234567890";
	private final static String	resourceName		= "VideoGo";
	private final static String	xmppSslStoreType	= "JKS";
	private final static String	xmppSslKeystore		= "conf/security/keystore";
	private final static String	xmppSslKeypass		= "changeit";
	private final static String	xmppSslTrustpass	= "changeit";
	
	public static void main(String[] args) throws IOException {
		
		ConnectManager.getInstance().setServerName(ip);
		ConnectManager.getInstance().setResourceName(resourceName);
		
		XmppServer server = new XmppServer();
		server.init(ip, apiKey, resourceName, xmppSslStoreType,
				xmppSslKeystore, xmppSslKeypass, xmppSslTrustpass);
		
		AuthProxy authProxy = new AuthProxyImpl();
		server.setAuthProxy(authProxy);
		
		server.setIp(ip);
		server.setPort(port);
		
		// MobileDevStatusListener mobileDevStatusListener = new
		// MobileDevStatusListenerImpl();
		// SessionManager.getInstance().registMobileDevStatusListener(
		// mobileDevStatusListener);
		
		// SessionManager.getInstance().start();
		
		try {
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@org.junit.Test
	public void test() throws IOException{
		Test.main(null);
	}
	
}
