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

import com.xmpp.push.androidpn.server.IQUtils;
import com.xmpp.push.androidpn.server.xmpp.exception.UnauthorizedException;
import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ;
import com.xmpp.push.androidpn.server.xmpp.stanza.PacketError;

/**
 * This is an abstract class to handle routed IQ packets.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public abstract class AbstractIQHandler implements IIQHandler {
	
	protected final static Logger	LOG	= LoggerFactory
												.getLogger(AbstractIQHandler.class);
	protected ConnectManager		connectManager;
	
	/**
	 * Constructor.
	 */
	public AbstractIQHandler() {
		connectManager = ConnectManager.getInstance();
	}
	
	/**
	 * Processes the received IQ packet.
	 * 
	 * @param packet the packet
	 */
	public void process(Channel channel, IQ iq) {
		try {
			IQ reply = handleIQ(channel, iq);
			if (reply != null) {
				IQUtils.send(channel, reply);
			}
		} catch (UnauthorizedException e) {
			if (iq != null) {
				try {
					IQ resp = IQUtils.createResultIQ(iq);
					resp.setChildElement(iq.getChildElement());
					resp.setError(PacketError.Condition.not_authorized);
					IQUtils.send(channel, resp);
				} catch (Exception de) {
					LOG.warn("Internal server error", de);
					// connectManager.getSession(iq.getFrom()).close();
				}
			}
		} catch (Exception e) {
			LOG.warn("Internal server error", e);
			try {
				if (null != iq) {
					IQ resp = IQUtils.createResultIQ(iq);
					resp.setChildElement(iq.getChildElement());
					resp.setError(PacketError.Condition.internal_server_error);
					// connectManager.getSession(iq.getFrom()).process(resp);
					IQUtils.send(channel, resp);
				}
			} catch (Exception ex) {
				// Ignore
			}
		}
	}
	
	/**
	 * Handles the received IQ packet.
	 * 
	 * @param packet the packet
	 * @return the response to send back
	 * @throws UnauthorizedException if the user is not authorized
	 */
	@Override
	public abstract IQ handleIQ(Channel channel, IQ packet)
			throws UnauthorizedException;
	
	/**
	 * Returns the namespace of the handler.
	 * 
	 * @return the namespace
	 */
	@Override
	public abstract String getNamespace();
	
}
