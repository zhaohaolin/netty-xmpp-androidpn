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
package com.hikvision.push.androidpn.server.xmpp.handler;

import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hikvision.push.androidpn.server.xmpp.XMPPNamespaces;
import com.hikvision.push.androidpn.server.xmpp.auth.AuthProxy;
import com.hikvision.push.androidpn.server.xmpp.auth.AuthToken;
import com.hikvision.push.androidpn.server.xmpp.exception.UnauthenticatedException;
import com.hikvision.push.androidpn.server.xmpp.exception.UnauthorizedException;
import com.hikvision.push.androidpn.server.xmpp.session.Connection;
import com.hikvision.push.androidpn.server.xmpp.session.IQUtils;
import com.hikvision.push.androidpn.server.xmpp.stanza.IQ;
import com.hikvision.push.androidpn.server.xmpp.stanza.JID;
import com.hikvision.push.androidpn.server.xmpp.stanza.PacketError;
import com.hikvision.push.androidpn.server.xmpp.xml.XMLBuilder;
import com.hikvision.push.androidpn.server.xmpp.xml.XMLElement;

/**
 * This class is to handle the TYPE_IQ jabber:iq:auth protocol.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class IQAuthHandler extends AbstractIQHandler implements XMPPNamespaces {
	
	private XMLElement			probeResponse;
	private AuthProxy			authProxy;
	private final String		resourceName;
	private final String		serverName;
	private final static Logger	LOG		= LoggerFactory
												.getLogger(IQAuthHandler.class);
	private boolean				auth	= false;
	
	/**
	 * Constructor.
	 */
	public IQAuthHandler(String resourceName, String serverName,
			AuthProxy authProxy) {
		this.resourceName = resourceName;
		this.serverName = serverName;
		this.authProxy = authProxy;
		
		probeResponse = XMLBuilder.create("query", AUTH).getXML();
		probeResponse.addChild("username");
		if (authProxy.isPlainSupported()) {
			probeResponse.addChild("password");
		}
		// if (AuthManager.isDigestSupported()) {
		// probeResponse.addElement("digest");
		// }
		probeResponse.addChild("resource");
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
		IQ reply = null;
		
		Connection connection = connectManager.getConnectByChannelId(Integer
				.valueOf(channel.id().asShortText()));
		
		// 当验证通过后
		if (connection == null && auth) {
			LOG.warn("Session not found for key: {} ", iq.getFrom());
			reply = IQUtils.createResultIQ(iq);
			reply.setChildElement(iq.getChildElement());
			reply.setError(PacketError.Condition.internal_server_error);
			return reply;
		}
		
		try {
			XMLElement query = iq.getFirstChild("query");
			
			// get query
			if (IQ.Type.get == iq.getType()) {
				String username = query.getChildText("username");
				if (username != null) {
					probeResponse.setChildText("username", username);
				}
				reply = IQUtils.createResultIQ(iq);
				reply.setChildElement(probeResponse);
				// if (connection.getStatus() != Session.STATUS_AUTHENTICATED) {
				// reply.setTo((JID) null);
				// }
				// set query
			} else {
				String resource = query.getChildText("resource");
				String username = query.getChildText("username");
				String password = query.getChildText("password");
				
				String digest = null;
				if (query.getChildText("digest") != null) {
					digest = query.getChildText("digest").toLowerCase();
				}
				
				// Verify the resource
				if (resource != null && resource.equals(resourceName)) {
					//
				} else {
					throw new IllegalArgumentException(
							"Invalid resource (empty or null).");
				}
				
				// Verify the username
				if (username == null || username.trim().length() == 0) {
					throw new UnauthorizedException(
							"Invalid username (empty or null).");
				}
				
				username = username.toLowerCase();
				
				// Verify that username and password are correct
				AuthToken token = null;
				if (authProxy.isPlainSupported()) {
					token = authProxy.authenticate(username);
				}
				
				if (token == null) {
					throw new UnauthenticatedException();
				}
				
				// System.out.println("username=" + username);
				
				// Set the session authenticated successfully
				connection = new Connection(Integer.valueOf(channel.id()
						.asLongText()), channel, username, resource, channel
						.remoteAddress().toString());
				connectManager.addConnect(connection);
				connection.setAuthToken(token);
				
				auth = true;
				
				if (LOG.isDebugEnabled())
					LOG.debug(
							"create connection success. connection=[{}], channel=[{}], username=[{}]",
							new Object[] { connection, channel, username });
				
				iq.setFrom(iq.getTo());
				// iq.setFrom(connection.getAddress());
				reply = IQUtils.createResultIQ(iq);
				reply.setTo(JID.jid(this.serverName, username, resource));
			}
		} catch (Exception ex) {
			reply = IQUtils.createResultIQ(iq);
			reply.setChildElement(iq.getChildElement());
			if (ex instanceof IllegalArgumentException) {
				reply.setError(PacketError.Condition.not_acceptable);
			} else if (ex instanceof UnauthorizedException) {
				reply.setError(PacketError.Condition.not_authorized);
			} else if (ex instanceof UnauthenticatedException) {
				reply.setError(PacketError.Condition.not_authorized);
			} else {
				reply.setError(PacketError.Condition.internal_server_error);
			}
			LOG.warn("Handles the received IQ packet exception reply getError:"
					+ reply.getError(), ex);
		}
		
		// Send the response directly to the session
		if (reply != null) {
			IQUtils.send(channel, reply);
		}
		return null;
	}
	
	/**
	 * Returns the namespace of the handler.
	 * 
	 * @return the namespace
	 */
	@Override
	public String getNamespace() {
		return AUTH;
	}
	
}
