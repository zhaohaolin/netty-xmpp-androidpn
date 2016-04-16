package com.xmpp.push.androidpn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.auth.AuthProxy;
import com.xmpp.push.androidpn.server.xmpp.auth.AuthToken;
import com.xmpp.push.androidpn.server.xmpp.exception.UnauthenticatedException;

/**
 * 
 * TODO
 * 
 * @author joe.zhao
 * @version $Id: AuthProxyImpl, v 0.1 2016年4月17日 上午7:48:44 Exp $
 */
public class AuthProxyImpl implements AuthProxy {
	
	private final static Logger	log			= LoggerFactory
													.getLogger(AuthProxyImpl.class);
	
	private static String		servername	= "serverName";
	
	@Override
	public AuthToken authenticate(String username)
			throws UnauthenticatedException, Exception {
		if (username == null) {
			throw new UnauthenticatedException();
		}
		username = username.trim().toLowerCase();
		if (username.contains("@")) {
			int index = username.indexOf("@");
			String domain = username.substring(index + 1);
			if (domain.equals(servername)) {
				username = username.substring(0, index);
			} else {
				throw new UnauthenticatedException();
			}
		}
		if (!isLegal(username)) {
			throw new UnauthenticatedException();
		}
		return new AuthToken(username);
	}
	
	@Override
	public boolean isPlainSupported() {
		return true;
	}
	
	/**
	 * 判断该用户-设备是否为合法的
	 * 
	 * @param username
	 * @return
	 * @throws DBException
	 */
	public boolean isLegal(String username) {
		System.out.println("username=" + username);
		return true;
	}
	
}
