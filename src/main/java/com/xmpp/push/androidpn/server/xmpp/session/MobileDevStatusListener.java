package com.xmpp.push.androidpn.server.xmpp.session;

import com.xmpp.push.androidpn.server.SessionStatus;

/**
 * <p>
 * Android手机上线、下线时需要进行的处理<br/>
 * </p>
 * 
 */
public interface MobileDevStatusListener {
	
	void changeStatus(String deviceid, SessionStatus status) throws Exception;
	
}
