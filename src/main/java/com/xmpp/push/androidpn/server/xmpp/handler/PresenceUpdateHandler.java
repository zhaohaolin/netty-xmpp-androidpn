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

import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;
import com.xmpp.push.androidpn.server.xmpp.session.Connection;
import com.xmpp.push.androidpn.server.xmpp.stanza.Presence;

/**
 * This class is to handle the presence protocol.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class PresenceUpdateHandler {
	
	protected final static Logger	LOG	= LoggerFactory
												.getLogger(PresenceUpdateHandler.class);
	protected ConnectManager		connectManager;
	
	/**
	 * Constructor.
	 */
	public PresenceUpdateHandler() {
		connectManager = ConnectManager.getInstance();
	}
	
	/**
	 * Processes the presence packet.
	 * 
	 * @param packet the packet
	 */
	public void process(Channel channel, Presence presence) {
		
		if (LOG.isDebugEnabled())
			LOG.debug("channel=[{}], presence=[{}]", channel, presence);
		
		Connection connect = connectManager.getConnectByChannelId(channel.id()
				.asLongText());
		
		if (LOG.isDebugEnabled())
			LOG.debug("channel=[{}], connection=[{}]", channel, connect);
		try {
			Presence.Type type = presence.getType();
			
			// 上线
			if (type == null) { // null == available
			
				if (LOG.isDebugEnabled())
					LOG.debug(
							"Rejected available presence: [{}], channel: [{}]",
							presence, channel);
				
				// 下线
			} else if (Presence.Type.unavailable == type) {
				//
				if (LOG.isDebugEnabled())
					LOG.debug(
							"Rejected unavailable presence: [{}], channel: [{}]",
							presence, channel);
			} else {
				// Connection connection =
				// connectManager.getConnectByChannelId(channel.getId());
				// if (connection!=null) {
				// presence.setFrom(new JID(null, connection.getServerName(),
				// null, true));
				// // presence.setTo(session.getAddress());
				// presence.setTo(JID.jid(null, null, null));
				// } else {
				// JID sender = presence.getFrom();
				// presence.setFrom(presence.getTo());
				// presence.setTo(sender);
				// }
				// presence.setError(PacketError.Condition.bad_request);
				// IQUtils.send(channel, presence);
			}
			
		} catch (Exception e) {
			LOG.warn("Internal server error. Triggered by packet: {}",
					presence, e);
		}
	}
	
}
