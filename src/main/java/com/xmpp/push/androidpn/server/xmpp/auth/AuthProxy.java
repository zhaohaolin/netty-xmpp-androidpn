package com.xmpp.push.androidpn.server.xmpp.auth;

import com.xmpp.push.androidpn.server.xmpp.exception.UnauthenticatedException;

/**
 * <p>
 */
public interface AuthProxy {
	
	public AuthToken authenticate(String username)
			throws UnauthenticatedException, Exception;
	
	public boolean isPlainSupported();
	
}
