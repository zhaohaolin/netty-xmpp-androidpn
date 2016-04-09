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
package com.hikvision.push.androidpn.server.xmpp.router;

import io.netty.channel.Channel;

import com.hikvision.push.androidpn.server.xmpp.stanza.Message;

/**
 * This class is to route MessageRecord packets to their corresponding handler.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class MessageRouter implements StanzaRouter<Message> {
	
	/**
	 * Constucts a packet router.
	 */
	public MessageRouter() {
		//
	}
	
	/**
	 * Routes the MessageRecord packet.
	 * 
	 * @param packet the packet to route
	 */
	@Override
	public void route(Channel channel, Message packet) {
		throw new RuntimeException("Please implement this!");
	}
	
}
