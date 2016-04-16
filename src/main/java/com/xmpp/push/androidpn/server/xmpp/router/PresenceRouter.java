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
package com.xmpp.push.androidpn.server.xmpp.router;

import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.handler.PresenceUpdateHandler;
import com.xmpp.push.androidpn.server.xmpp.stanza.Presence;

/**
 * This class is to route Presence packets to their corresponding handler.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class PresenceRouter implements StanzaRouter<Presence> {
	
	private final static Logger		LOG	= LoggerFactory
												.getLogger(PresenceRouter.class);
	
	private PresenceUpdateHandler	presenceUpdateHandler;
	
	/**
	 * Constucts a packet router.
	 */
	public PresenceRouter() {
		presenceUpdateHandler = new PresenceUpdateHandler();
	}
	
	/**
	 * Routes the Presence packet.
	 * 
	 * @param presence the packet to route
	 */
	@Override
	public void route(Channel channel, Presence presence) {
		if (presence == null) {
			throw new NullPointerException();
		}
		handle(channel, presence);
	}
	
	private void handle(Channel channel, Presence packet) {
		try {
			Presence.Type type = packet.getType();
			// Presence updates (null == 'available')
			if (type == null || Presence.Type.unavailable == type) {
				presenceUpdateHandler.process(channel, packet);
			} else {
				LOG.warn("Unknown presence type");
			}
			
		} catch (Exception e) {
			LOG.warn("Could not route packet", e);
			// TODO close channel
		}
	}
	
}
