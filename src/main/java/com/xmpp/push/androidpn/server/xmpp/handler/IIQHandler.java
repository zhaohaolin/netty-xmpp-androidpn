/*
 * IIQHandler.java Creator: joe.zhao Create-Date: 下午11:57:23
 */
package com.xmpp.push.androidpn.server.xmpp.handler;

import io.netty.channel.Channel;

import com.xmpp.push.androidpn.server.xmpp.XMPPNamespaces;
import com.xmpp.push.androidpn.server.xmpp.exception.UnauthorizedException;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ;

/**
 * TODO
 * 
 * @author joe.zhao
 * @version $Id: IIQHandler, v 0.1 2015年5月18日 下午11:57:23 Exp $
 */
public interface IIQHandler extends XMPPNamespaces {
	
	String getNamespace();
	
	IQ handleIQ(Channel channel, IQ packet) throws UnauthorizedException;
	
}
