package com.xmpp.push.androidpn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.SessionStatus;
import com.xmpp.push.androidpn.server.xmpp.session.MobileDevStatusListener;

/**
 * 
 * TODO
 * 
 * @author joe.zhao
 * @version $Id: MobileDevStatusListenerImpl, v 0.1 2016年4月17日 上午7:48:10 Exp $
 */
public class MobileDevStatusListenerImpl implements MobileDevStatusListener {
	
	private static final Logger	log	= LoggerFactory
											.getLogger(MobileDevStatusListenerImpl.class);
	
	@Override
	public void changeStatus(String deviceid, SessionStatus type)
			throws Exception {
		// 触发消息重传
		System.out.println(", deviceId=" + deviceid + ", status=" + type);
	}
	
}
