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
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;
import com.xmpp.push.androidpn.server.xmpp.ssl.SSLConfig;
import com.xmpp.push.androidpn.server.xmpp.stanza.Packet;
import com.xmpp.push.androidpn.server.xmpp.stanza.XMPPNamespaces;
import com.xmpp.push.androidpn.server.xmpp.xml.XMLElement;

/**
 * XEP-0114 Stream Decoder.
 */
public class XMPPDecodeHandler extends ChannelInboundHandlerAdapter {
	
	private final static Logger	LOG			= LoggerFactory
													.getLogger(XMPPDecodeHandler.class);
	
	private final static QName	STREAM_NAME	= new QName(XMPPNamespaces.STREAM,
													"stream", "stream");
	private final static QName	STARTTLS	= new QName("starttls");
	
	private static enum Status {
		CONNECT, STARTEDTLS, TLSCONNECT, AUTHENTICATE, READY, DISCONNECTED;
	}
	
	private final String	serverName;
	private final SSLConfig	sslConfig;
	private Status			status;
	private TLSPolicy		tlsPolicy		= TLSPolicy.OPTIONAL;
	private boolean			starttls		= true;
	private boolean			authed			= false;
	private SSLEngine		sslEngine;
	private ConnectManager	connectManager	= ConnectManager.getInstance();
	
	public XMPPDecodeHandler(String serverName, SSLConfig sslConfig) {
		super();
		
		this.serverName = checkNotNull(serverName);
		this.sslConfig = checkNotNull(sslConfig);
		
		status = Status.CONNECT;
		
		SSLContext sslContext = sslConfig.getSslContext();
		sslEngine = sslContext.createSSLEngine();
		
		sslEngine.setUseClientMode(false);
		
		// sslEngine.setNeedClientAuth(true);
		sslEngine.setWantClientAuth(true);
		
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		// 这儿采用负载计算
		connectManager.addBalance();
		
		// if (LOG.isDebugEnabled())
		// LOG.debug("channel connected for =[{}]", ctx.channel());
		//
		// String xml =
		// "<stream:stream xmlns='jabber:component:accept' xmlns:stream='http://etherx.jabber.org/streams' to='"
		// + serverName + "'>";
		// channel.writeAndFlush(xml);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("received msg=[{}]", msg);
		}
		
		Channel channel = ctx.channel();
		
		if (msg instanceof XMLElement) {
			final XMLElement element = (XMLElement) msg;
			
			switch (status) {
			
			// 1. 连接请求判断
				case CONNECT:
					
					if ("stream:stream".equals(element.getTagName())) {
						
						// // to=""
						// if (!serverName.equals(element.getAttributeByName(
						// new QName("to")).getValue())) {
						// throw new Exception("server name mismatch");
						// }
						
						status = Status.STARTEDTLS;
						// status = Status.READY;
						
						// 返回client端，支持tls,以及哪些权限认证方式
						channel.writeAndFlush(startResp());
						
						if (LOG.isDebugEnabled())
							LOG.debug("client channel=[{}] connect ok", channel);
					}
					break;
				
				// 2. 返回客户端要求starttls
				case STARTEDTLS:
					
					if ("starttls".equals(element.getTagName())) {
						
						status = Status.TLSCONNECT;
						starttls = false;
						
						// 响应返回客户端可以继续
						String xml = "<proceed xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>";
						channel.writeAndFlush(xml);
						
						// add tls handler
						channel.pipeline().addFirst("tls",
								new SslHandler(sslEngine));
						
						if (LOG.isDebugEnabled()) {
							LOG.debug("client start tls ok. channel=[{}]",
									channel);
						}
						
						// ctx.sendUpstream(e);
						ctx.fireChannelRead(msg);
						break;
					}
					
					// 如果不是tls与下面的连接情况一样
					{
						if (LOG.isDebugEnabled()) {
							LOG.debug("client request msg by channel=[{}]",
									channel);
						}
						
						status = Status.READY;
						
						final Packet stanza = Packet.fromElement(element);
						if (stanza == null)
							throw new Exception("Unknown stanza");
						
						ctx.fireChannelRead(stanza);
						break;
					}
					
					// 3.当客户端要求采用tls传输时，返回tls响应
				case TLSCONNECT:
					
					if ("stream:stream".equals(element.getTagName())) {
						
						status = Status.READY;
						
						// 返回client端
						channel.writeAndFlush(tlsNegotiated());
						
						if (LOG.isDebugEnabled()) {
							LOG.debug("client tls connect ok. channel=[{}]",
									channel);
						}
					}
					break;
				
				// 4.当客户端采用tls后，正常的iq请求
				case READY:
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("client request msg by channel=[{}]", channel);
					}
					
					final Packet stanza = Packet.fromElement(element);
					if (stanza == null)
						throw new Exception("Unknown stanza");
					
					// 弹出到上一层的handler继续处理
					ctx.fireChannelRead(stanza);
					break;
				
				default:
					throw new Exception("unexpected handleElement");
			}
		} else {
			ctx.fireChannelRead(msg);
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// ctx.channel().writeAndFlush("</stream:stream>");
		connectManager.removeConnect(ctx.channel().id().asLongText());
	}
	
	public static final int		MAJOR_VERSION	= 1;
	public static final int		MINOR_VERSION	= 0;
	private static final String	language		= "en";
	
	// Build the start packet response
	private final String startResp() {
		StringBuilder builder = new StringBuilder(200);
		
		String streamId = Long.toHexString(Double.doubleToLongBits(Math
				.random()));
		
		builder.append("<?xml version='1.0' encoding='UTF-8'?>");
		builder.append("<stream:stream ");
		builder.append("xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\" from=\"");
		builder.append(serverName);
		builder.append("\" id=\"");
		builder.append(streamId);
		builder.append("\" xml:lang=\"");
		builder.append(language);
		builder.append("\" version=\"");
		builder.append(MAJOR_VERSION).append(".").append(MINOR_VERSION);
		builder.append("\">");
		
		builder.append(features());
		
		return builder.toString();
	}
	
	// XMPP 1.0 needs stream features
	private final String features() {
		StringBuilder builder = new StringBuilder();
		builder.append("<stream:features>");
		
		if (starttls && tlsPolicy != TLSPolicy.DISABLED) {
			builder.append("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\">");
			if (tlsPolicy == TLSPolicy.REQUIRED) {
				builder.append("<required/>");
			}
			builder.append("</starttls>");
		}
		
		// specificFeatures
		if (!authed) {
			// Supports Non-SASL Authentication
			builder.append("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>");
			// Supports In-Band Registration
			builder.append("<register xmlns=\"http://jabber.org/features/iq-register\"/>");
		} else {
			// If the session has been authenticated
			builder.append("<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"/>");
			builder.append("<session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\"/>");
		}
		builder.append("</stream:features>");
		return builder.toString();
	}
	
	private final String tlsNegotiated() {
		// Offer stream features including SASL Mechanisms
		StringBuilder builder = new StringBuilder(620);
		builder.append("<?xml version='1.0' encoding='UTF-8'?>");
		builder.append("<stream:stream ");
		builder.append("xmlns:stream=\"http://etherx.jabber.org/streams\" ");
		builder.append("xmlns=\"jabber:client\" from=\"");
		builder.append(serverName);
		builder.append("\" id=\"");
		
		String streamId = Long.toHexString(Double.doubleToLongBits(Math
				.random()));
		
		builder.append(streamId);
		builder.append("\" xml:lang=\"");
		builder.append(language);
		builder.append("\" version=\"");
		builder.append(MAJOR_VERSION).append(".").append(MINOR_VERSION);
		builder.append("\">");
		builder.append("<stream:features>");
		// Include specific features such as auth and register for client
		// sessions
		
		if (starttls && tlsPolicy != TLSPolicy.DISABLED) {
			builder.append("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\">");
			if (tlsPolicy == TLSPolicy.REQUIRED) {
				builder.append("<required/>");
			}
			builder.append("</starttls>");
		}
		
		if (!authed) {
			// Supports Non-SASL Authentication
			builder.append("<auth xmlns=\"http://jabber.org/features/iq-auth\"/>");
			// Supports In-Band Registration
			builder.append("<register xmlns=\"http://jabber.org/features/iq-register\"/>");
		} else {
			// If the session has been authenticated
			builder.append("<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"/>");
			builder.append("<session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\"/>");
		}
		builder.append("</stream:features>");
		return builder.toString();
	}
	
	// TLS安全策略
	public enum TLSPolicy {
		REQUIRED, OPTIONAL, DISABLED
	}
	
}
