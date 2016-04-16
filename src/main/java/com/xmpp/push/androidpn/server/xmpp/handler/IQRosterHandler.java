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
package com.xmpp.push.androidpn.server.xmpp.handler;

import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.exception.UnauthorizedException;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ;

/**
 * This class is to handle the TYPE_IQ jabber:iq:roster protocol.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class IQRosterHandler extends AbstractIQHandler {
	
	private final static Logger	LOG	= LoggerFactory
											.getLogger(IQRosterHandler.class);
	
	/**
	 * Constructor.
	 */
	public IQRosterHandler() {
		//
	}
	
	/**
	 * Handles the received IQ packet.
	 * 
	 * @param iq the packet
	 * @return the response to send back
	 * @throws UnauthorizedException if the user is not authorized
	 */
	@Override
	public IQ handleIQ(Channel channel, IQ iq) throws UnauthorizedException {
		
		if (LOG.isDebugEnabled())
			LOG.debug("channel=[{}], roster iq=[{}]", channel, iq);
		return null;
	}
	
	/**
	 * Returns the namespace of the handler.
	 * 
	 * @return the namespace
	 */
	@Override
	public String getNamespace() {
		return ROSTER;
	}
	
}
