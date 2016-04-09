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

package com.hikvision.push.androidpn.server.xmpp.handler;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.nio.channels.Channels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hikvision.push.androidpn.server.xmpp.auth.AuthProxy;
import com.hikvision.push.androidpn.server.xmpp.router.IQRouter;
import com.hikvision.push.androidpn.server.xmpp.router.MessageRouter;
import com.hikvision.push.androidpn.server.xmpp.router.PresenceRouter;
import com.hikvision.push.androidpn.server.xmpp.session.ConnectManager;
import com.hikvision.push.androidpn.server.xmpp.ssl.SSLConfig;
import com.hikvision.push.androidpn.server.xmpp.stanza.IQ;
import com.hikvision.push.androidpn.server.xmpp.stanza.Message;
import com.hikvision.push.androidpn.server.xmpp.stanza.Packet;
import com.hikvision.push.androidpn.server.xmpp.stanza.Presence;

/**
 * Handles XMPP Stanzas.
 */
public class XMPPStreamHandler extends SimpleChannelHandler {
	
	private final String			serverName;
	private final SSLConfig			sslConfig;
	private ConnectManager			connectManager	= ConnectManager
															.getInstance();
	private final MessageRouter		messageRouter;
	private final PresenceRouter	presenceRouter;
	private final IQRouter			iqRouter;
	private final static Logger		LOG				= LoggerFactory
															.getLogger(XMPPStreamHandler.class);
	
	public XMPPStreamHandler(String serverName, SSLConfig sslConfig,
			AuthProxy authProxy) {
		this.serverName = checkNotNull(serverName);
		this.sslConfig = checkNotNull(sslConfig);
		this.messageRouter = new MessageRouter();
		this.presenceRouter = new PresenceRouter();
		this.iqRouter = new IQRouter(checkNotNull(authProxy));
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (!(e.getMessage() instanceof Packet)) {
			ctx.sendUpstream(e);
			return;
		}
		
		// router the xmpp request
		Channel channel = ctx.getChannel();
		final Packet stanza = (Packet) e.getMessage();
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
	
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (!(e.getMessage() instanceof Packet)) {
			ctx.sendDownstream(e);
			return;
		}
		
		Channels.write(ctx, e.getFuture(), ChannelBuffers.copiedBuffer(e
				.getMessage().toString(), CharsetUtil.UTF_8));
	}
	
	@Override
	public void disconnectRequested(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		// callback.willDisconnect();
		ctx.sendDownstream(e);
	}
	
	// 通道关闭
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		// callback.disconnected();
		if (LOG.isDebugEnabled())
			LOG.debug("channel ctx=[{}] disconnected.", ctx);
		connectManager.removeConnect(ctx.getChannel().getId());
		ctx.sendUpstream(e);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		// super.exceptionCaught(ctx, e);
		// e.getCause().printStackTrace();
		// LOG.error("exception:" + e);
		if (e.getCause() instanceof IOException) {
			connectManager.removeConnect(ctx.getChannel().getId());
		}
	}
	
}
