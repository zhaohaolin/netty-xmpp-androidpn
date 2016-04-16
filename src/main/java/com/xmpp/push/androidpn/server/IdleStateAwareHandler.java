/*
 * IdleStateAwareHandler.java Creator: joe.zhao Create-Date: 下午11:41:05
 */
package com.xmpp.push.androidpn.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;

/**
 * 超时机制判断, 读超时,写超时,全超时
 * 
 * @author joe.zhao
 * @version $Id: IdleStateAwareHandler, v 0.1 2016年4月9日 下午11:41:05 Exp $
 */
public class IdleStateAwareHandler extends ChannelDuplexHandler {
	
	private final static Logger		LOG	= LoggerFactory
												.getLogger(IdleStateAwareHandler.class);
	private final ConnectManager	connectManager;
	
	public IdleStateAwareHandler(ConnectManager connectManager) {
		this.connectManager = connectManager;
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		Channel channel = ctx.channel();
		if (evt instanceof IdleState) {
			IdleState state = (IdleState) evt;
			
			switch (state) {
				case READER_IDLE: {
					// read timeout
					LOG.warn("channel=[{}] read timeout will close.", channel);
					connectManager.removeConnect(channel.id().asLongText());
					channel.close();
					channel = null;
					break;
				}
				
				case WRITER_IDLE: {
					// write timeout
					LOG.warn("channel=[{}] write timeout will close.", channel);
					connectManager.removeConnect(channel.id().asLongText());
					channel.close();
					channel = null;
					break;
				}
				
				case ALL_IDLE: {
					// all idle
					LOG.warn("channel=[{}] idle timeout will close.", channel);
					connectManager.removeConnect(channel.id().asLongText());
					channel.close();
					channel = null;
					break;
				}
				default:
					break;
			}
		}
		
	}
	
}
