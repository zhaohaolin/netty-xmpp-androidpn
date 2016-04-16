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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.IQUtils;
import com.xmpp.push.androidpn.server.xmpp.XMPPNamespaces;
import com.xmpp.push.androidpn.server.xmpp.auth.AuthProxy;
import com.xmpp.push.androidpn.server.xmpp.handler.AbstractIQHandler;
import com.xmpp.push.androidpn.server.xmpp.handler.IQAuthHandler;
import com.xmpp.push.androidpn.server.xmpp.handler.IQRosterHandler;
import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;
import com.xmpp.push.androidpn.server.xmpp.session.Connection;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ;
import com.xmpp.push.androidpn.server.xmpp.stanza.PacketError;
import com.xmpp.push.androidpn.server.xmpp.xml.XMLElement;

/**
 * This class is to route IQ packets to their corresponding handler.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class IQRouter implements StanzaRouter<IQ>, XMPPNamespaces {
	
	private final static Logger				LOG					= LoggerFactory
																		.getLogger(IQRouter.class);
	
	private ConnectManager					connectionManager;
	private Map<String, AbstractIQHandler>	namespace2Handlers	= new HashMap<String, AbstractIQHandler>();
	
	/**
	 * Constucts a packet router registering new IQ handlers.
	 */
	public IQRouter(AuthProxy authProxy) {
		connectionManager = ConnectManager.getInstance();
		
		// auth
		IQAuthHandler iqAuthHandler = new IQAuthHandler(
				connectionManager.getResourceName(),
				connectionManager.getServerName(), authProxy);
		namespace2Handlers.put(iqAuthHandler.getNamespace(), iqAuthHandler);
		
		// roster
		IQRosterHandler iqRosterHandler = new IQRosterHandler();
		namespace2Handlers.put(iqRosterHandler.getNamespace(), iqRosterHandler);
	}
	
	/**
	 * Routes the IQ packet based on its namespace.
	 * 
	 * @param iq the packet to route
	 */
	@Override
	public void route(Channel channel, IQ iq) {
		if (iq == null) {
			throw new NullPointerException();
		}
		
		// 心跳处理
		XMLElement heartElement = iq.getFirstChild("ping");
		if (null != heartElement) {
			IQ pong = IQUtils.createResultIQ(iq);
			IQUtils.send(channel, pong);
			return;
		}
		
		XMLElement childElement = iq.getFirstChild("query");
		if (null == childElement) {
			LOG.warn("childElement is null for channel=[{}], iq=[{}]", channel,
					iq);
			return;
		}
		
		String namespace = childElement.getNamespace();
		
		if (StringUtils.isEmpty(namespace)) {
			LOG.warn("namespace is empty for channel=[{}], iq=[{}]", channel,
					iq);
			return;
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("namespace=[{}], channel=[{}], iq=[{}]", new Object[] {
					namespace, channel, iq });
		}
		
		if (namespace.equals(AUTH) || namespace.equals(REGISTER)
				|| namespace.equals(BIND)) {
			handle(channel, iq, childElement, namespace);
		} else {
			// 返回错误
			// IQ reply = IQUtils.createResultIQ(iq);
			// reply.setChildElement(iq.getChildElement());
			// reply.setError(PacketError.Condition.not_authorized);
			//
			// // send resp err to client
			// IQUtils.send(channel, reply);
		}
	}
	
	private void handle(Channel channel, IQ iq, XMLElement childElement,
			String namespace) {
		try {
			if (namespace == null) {
				if (iq.getType() != IQ.Type.result
						&& iq.getType() != IQ.Type.error) {
					LOG.warn("Unknown packet " + iq);
				}
			} else {
				AbstractIQHandler handler = namespace2Handlers.get(namespace);
				if (handler == null) {
					sendErrorPacket(channel, iq,
							PacketError.Condition.service_unavailable);
				} else {
					handler.process(channel, iq);
				}
			}
			
		} catch (Exception e) {
			LOG.warn("Could not route packet", e);
			Connection connect = connectionManager
					.getConnectByChannelId(channel.id().asLongText());
			if (connect != null) {
				IQ reply = IQUtils.createResultIQ(iq);
				reply.setError(PacketError.Condition.internal_server_error);
				// send resp err to client
				IQUtils.send(channel, reply);
			}
		}
	}
	
	/**
	 * Senda the error packet to the original sender
	 */
	private void sendErrorPacket(Channel channel, IQ iq,
			PacketError.Condition condition) {
		if (IQ.Type.error == iq.getType()) {
			LOG.warn("Cannot reply an IQ error to another IQ error: " + iq);
			return;
		}
		IQ reply = IQUtils.createResultIQ(iq);
		reply.setChildElement(iq.getChildElement());
		reply.setError(condition);
		// send resp err to client
		IQUtils.send(channel, reply);
	}
	
}
