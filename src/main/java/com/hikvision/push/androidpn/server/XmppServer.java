package com.hikvision.push.androidpn.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hikvision.push.androidpn.server.xmpp.auth.AuthProxy;
import com.hikvision.push.androidpn.server.xmpp.handler.XMLElementDecoder;
import com.hikvision.push.androidpn.server.xmpp.handler.XMLFrameDecoder;
import com.hikvision.push.androidpn.server.xmpp.handler.XMPPDecodeHandler;
import com.hikvision.push.androidpn.server.xmpp.handler.XMPPStreamHandler;
import com.hikvision.push.androidpn.server.xmpp.session.ConnectManager;
import com.hikvision.push.androidpn.server.xmpp.ssl.SSLConfig;

/**
 * TODO
 * 
 * @author joe.zhao
 * @version $Id: XmppServer, v 0.1 2015年4月23日 下午11:23:55 Exp $
 */
public class XmppServer extends AndroidpnConfig {
	
	private final static Logger					LOG						= LoggerFactory
																				.getLogger(XmppServer.class);
	
	private int									maxRetryCount			= 20;
	private long								retryTimeout			= 30 * 1000;							// 30s
																												
	private String								ip						= "0.0.0.0";
	private int									port					= 5222;
	private ServerBootstrap						server					= null;
	
	private AuthProxy							authProxy;
	private SSLConfig							sslConfig;
	private ConnectManager						connectManager			= ConnectManager
																				.getInstance();
	
	private EventLoopGroup						bossGroup				= new NioEventLoopGroup();
	private EventLoopGroup						workerGroup				= new NioEventLoopGroup();
	
	private final static Map<String, Object>	CHANNEL_CONFIGER_MAP	= new HashMap<String, Object>();
	static {
		// CHANNEL_CONFIGER_MAP.put("writeBufferHighWaterMark", 64 * 1024);
		// CHANNEL_CONFIGER_MAP.put("writeBufferLowWaterMark", 32 * 1024);
		// CHANNEL_CONFIGER_MAP.put("writeSpinCount", 16);
		// CHANNEL_CONFIGER_MAP.put("broadcast", true);
		// CHANNEL_CONFIGER_MAP.put("loopbackModeDisabled", true);
		CHANNEL_CONFIGER_MAP.put("reuseAddress", true);
		CHANNEL_CONFIGER_MAP.put("tcpNoDelay", true);
		CHANNEL_CONFIGER_MAP.put("keepAlive", true);
		CHANNEL_CONFIGER_MAP.put("receiveBufferSize", 10240);
		// CHANNEL_CONFIGER_MAP.put("receiveBufferSizePredictor",
		// new FixedReceiveBufferSizePredictor(100000));
		// CHANNEL_CONFIGER_MAP.put("receiveBufferSizePredictorFactory",
		// new FixedReceiveBufferSizePredictorFactory(100000));
		CHANNEL_CONFIGER_MAP.put("sendBufferSize", 10240);
		// CHANNEL_CONFIGER_MAP.put("timeToLive", 30);
		// CHANNEL_CONFIGER_MAP.put("trafficClass", 100);
		// CHANNEL_CONFIGER_MAP.put("bufferFactory",
		// new HeapChannelBufferFactory());
		// CHANNEL_CONFIGER_MAP.put("connectTimeoutMillis", 60000);
		CHANNEL_CONFIGER_MAP.put("soLinger", 1);
	}
	
	public void start() throws Exception {
		
		// convert config to ssl config
		sslConfig = new SSLConfig();
		convert(sslConfig);
		sslConfig.init();
		
		server = new ServerBootstrap();
		server.group(bossGroup, workerGroup);
		
		// config options
		server.option(ChannelOption.SO_BACKLOG, 128);
		server.option(ChannelOption.TCP_NODELAY, true);
		server.option(ChannelOption.SO_KEEPALIVE, true);
		
		final Timer timer = new HashedWheelTimer();
		
		server.childHandler(new ChannelInitializer<SocketChannel>() {
			
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				final ChannelPipeline pipeline = ch.pipeline();
				
				// 靠近传输层
				InternalLoggerFactory
						.setDefaultFactory(new Slf4JLoggerFactory());
				pipeline.addLast("logger", new LoggingHandler(LogLevel.DEBUG));
				
				// 超时handler 读超时0分钟，写超时0分钟，空闲超时6分钟
				pipeline.addLast("idleHandler", new IdleStateHandler(timer, 0,
						0, 9 * 60, TimeUnit.SECONDS));
				
				pipeline.addLast("idleStateAwareHandler",
						new IdleStateAwareHandler());
				
				pipeline.addLast("xmlFramer", new XMLFrameDecoder());
				pipeline.addLast("xmlDecoder", new XMLElementDecoder());
				
				// encoder
				pipeline.addLast("stringEncoder",
						new StringEncoder(Charset.forName("UTF-8")));
				
				// xmpp decoder handler
				pipeline.addLast("xmppDecoder", new XMPPDecodeHandler(
						getServerName(), sslConfig));
				
				// 业务handler放在线程池中运行
				// pipeline.addLast("executor", executionHandler);
				
				// xmpp 业务逻辑handler
				pipeline.addLast("xmppHandler", new XMPPStreamHandler(
						getServerName(), sslConfig, authProxy));
				
				// 靠近应用层
			}
			
		});
		
		int retryCount = 0;
		boolean binded = false;
		do {
			try {
				// server.bind(new InetSocketAddress(ip, port));
				server.bind(new InetSocketAddress(port));
				binded = true;
			} catch (Exception e) {
				// e.printStackTrace();
				LOG.warn("start failed on host:port=> [{}]:[{}], and retry...",
						ip, port);
				retryCount++;
				if (retryCount >= maxRetryCount) {
					throw e;
				}
				
				try {
					TimeUnit.MILLISECONDS.sleep(retryTimeout);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
			} finally {
				//
			}
		} while (!binded);
		
		if (LOG.isInfoEnabled())
			LOG.info("start succeed in ip:port => [{}]:[{}]", ip, port);
	}
	
	public void stop() {
		if (null != server) {
			this.server = null;
			
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	public int getMaxRetryCount() {
		return maxRetryCount;
	}
	
	public void setMaxRetryCount(int maxRetryCount) {
		this.maxRetryCount = maxRetryCount;
	}
	
	public long getRetryTimeout() {
		return retryTimeout;
	}
	
	public void setRetryTimeout(long retryTimeout) {
		this.retryTimeout = retryTimeout;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public AuthProxy getAuthProxy() {
		return authProxy;
	}
	
	public void setAuthProxy(AuthProxy authProxy) {
		this.authProxy = authProxy;
	}
	
	private class IdleStateAwareHandler extends ChannelDuplexHandler {
		
		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
				throws Exception {
			super.channelIdle(ctx, e);
			Channel channel = ctx.getChannel();
			
			switch (e.getState()) {
				case READER_IDLE: {
					// read timeout
					LOG.warn("channel=[{}] read timeout will close.", channel);
					connectManager.removeConnect(channel.getId());
					channel.close();
					channel = null;
					break;
				}
				
				case WRITER_IDLE: {
					// write timeout
					LOG.warn("channel=[{}] write timeout will close.", channel);
					connectManager.removeConnect(channel.getId());
					channel.close();
					channel = null;
					break;
				}
				
				case ALL_IDLE: {
					// all idle
					LOG.warn("channel=[{}] idle timeout will close.", channel);
					connectManager.removeConnect(channel.getId());
					channel.close();
					channel = null;
					break;
				}
				default:
					break;
			}
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			Throwable throwable = e.getCause();
			Channel channel = ctx.getChannel();
			
			if (throwable instanceof ReadTimeoutException) {
				// read timeout
				LOG.warn("channel=[{}] read timeout will close.", channel);
				connectManager.removeConnect(channel.getId());
				channel.close();
				channel = null;
			} else if (throwable instanceof WriteTimeoutException) {
				// write timeout
				LOG.warn("channel=[{}] write timeout will close.", channel);
				connectManager.removeConnect(channel.getId());
				channel.close();
				channel = null;
			} else if (throwable instanceof TimeoutException) {
				// all timeout
				LOG.warn("channel=[{}] idle timeout will close.", channel);
				connectManager.removeConnect(channel.getId());
				channel.close();
				channel = null;
			} else if (throwable instanceof IOException) {
				// io exception
				LOG.warn("channel=[{}] ioexception will close.", channel);
				connectManager.removeConnect(channel.getId());
				channel.close();
				channel = null;
			} else {
				super.exceptionCaught(ctx, e);
			}
		}
		
	}
	
}
