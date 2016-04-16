/**
 * Copyright 2012 José Martínez
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.xmpp.push.androidpn.server.xmpp.handler;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.auth.AuthProxy;
import com.xmpp.push.androidpn.server.xmpp.router.IQRouter;
import com.xmpp.push.androidpn.server.xmpp.router.MessageRouter;
import com.xmpp.push.androidpn.server.xmpp.router.PresenceRouter;
import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;
import com.xmpp.push.androidpn.server.xmpp.ssl.SSLConfig;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ;
import com.xmpp.push.androidpn.server.xmpp.stanza.Message;
import com.xmpp.push.androidpn.server.xmpp.stanza.Packet;
import com.xmpp.push.androidpn.server.xmpp.stanza.Presence;

/**
 * Handles XMPP Stanzas.
 */
public class XMPPHandler extends ChannelInboundHandlerAdapter {
	
	private final String			serverName;
	private final SSLConfig			sslConfig;
	private ConnectManager			connectManager	= ConnectManager
															.getInstance();
	private final MessageRouter		messageRouter;
	private final PresenceRouter	presenceRouter;
	private final IQRouter			iqRouter;
	private final static Logger		LOG				= LoggerFactory
															.getLogger(XMPPHandler.class);
	
	public XMPPHandler(String serverName, SSLConfig sslConfig,
			AuthProxy authProxy) {
		this.serverName = checkNotNull(serverName);
		this.sslConfig = checkNotNull(sslConfig);
		this.messageRouter = new MessageRouter();
		this.presenceRouter = new PresenceRouter();
		this.iqRouter = new IQRouter(checkNotNull(authProxy));
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (!(msg instanceof Packet)) {
			ctx.fireChannelRead(msg);
			return;
		}
		
		// router the xmpp request
		Channel channel = ctx.channel();
		final Packet stanza = (Packet) msg;
		if (stanza instanceof Message) {
			
			Message message = (Message) stanza;
			
			if (LOG.isDebugEnabled())
				LOG.debug("message: [{}]", message);
			
			messageRouter.route(channel, message);
			
		} else if (stanza instanceof Presence) {
			
			// 上下线请求
			Presence presence = (Presence) stanza;
			
			if (LOG.isDebugEnabled())
				LOG.debug("presence: [{}], from: [{}], to: [{}], type: [{}]",
						new Object[] { presence.getId(), presence.getFrom(),
								presence.getType() });
			
			presenceRouter.route(channel, presence);
		} else if (stanza instanceof IQ) {
			
			IQ iq = (IQ) stanza;
			
			if (LOG.isDebugEnabled())
				LOG.debug("iq: [{}]", iq);
			
			iqRouter.route(channel, iq);
		}
	}
	
	// 通道关闭
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (LOG.isDebugEnabled())
			LOG.debug("channel ctx=[{}] disconnected.", ctx);
		connectManager.removeConnect(ctx.channel().id().asLongText());
		ctx.fireChannelInactive();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// LOG.error("exception:" + e);
		if (cause instanceof IOException) {
			connectManager.removeConnect(ctx.channel().id().asLongText());
		}
	}
	
}
