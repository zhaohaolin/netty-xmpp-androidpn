/*
 * CopyRight (c) 2012-2015 Hikvision Co, Ltd. All rights reserved. Filename:
 * Test.java Creator: joe.zhao(zhaohaolin@hikvision.com.cn) Create-Date:
 * 下午6:46:16
 */
package com.xmpp.push.androidpn;

import io.netty.channel.Channel;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.xmpp.push.androidpn.server.IQUtils;
import com.xmpp.push.androidpn.server.XmppServer;
import com.xmpp.push.androidpn.server.xmpp.auth.AuthProxy;
import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;

/**
 * TODO
 * 
 * @author joe.zhao(zhaohaolin@hikvision.com.cn)
 * @version $Id: Test, v 0.1 2015年4月9日 下午6:46:16 Exp $
 */
public class Test {
	
	private final static String	ip					= "127.0.0.1";
	private final static int	port				= 8222;
	
	private final static String	apiKey				= "1234567890";
	private final static String	resourceName		= "VideoGo";
	private final static String	xmppSslStoreType	= "JKS";
	private final static String	xmppSslKeystore		= "conf/security/keystore";
	private final static String	xmppSslKeypass		= "changeit";
	private final static String	xmppSslTrustpass	= "changeit";
	
	public static void main(String[] args) throws Exception {
		
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
		
		server.serverStart();
		
		// test push
		final ConnectManager connectManager = ConnectManager.getInstance();
		Thread.sleep(1000 * 30);
		
		System.out.println("开始测试消息推送");
		
		Map<String, String> map = connectManager.getChannelmap();
		Iterator<Entry<String, String>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			String channelId = entry.getKey();
			
			Channel channel = connectManager.getConnectByChannelId(channelId)
					.getChannel();
			
			String content = "我是消息内容";
			String ext = "i am ext content.";
			String id = Long
					.toHexString(Double.doubleToLongBits(Math.random()));
			
			IQUtils.send(channel, IQUtils.createIQ(apiKey, id, content, ext));
			System.out.println("向channel=" + channel + ", 推送消息完成.");
		}
	}
	
	@org.junit.Test
	public void test() throws Exception {
		Test.main(null);
	}
	
}
