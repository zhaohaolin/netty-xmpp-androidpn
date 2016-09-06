package com.xmpp.push.androidpn.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.auth.AuthProxy;
import com.xmpp.push.androidpn.server.xmpp.handler.XMLElementDecoder;
import com.xmpp.push.androidpn.server.xmpp.handler.XMLFrameDecoder;
import com.xmpp.push.androidpn.server.xmpp.handler.XMPPDecodeHandler;
import com.xmpp.push.androidpn.server.xmpp.handler.XMPPHandler;
import com.xmpp.push.androidpn.server.xmpp.session.ConnectManager;
import com.xmpp.push.androidpn.server.xmpp.ssl.SSLConfig;

/**
 * TODO
 * 
 * @author joe.zhao
 * @version $Id: XmppServer, v 0.1 2015年4月23日 下午11:23:55 Exp $
 */
public class XmppServer extends AndroidpnConfig {
	
	private final static Logger				LOG				= LoggerFactory
																	.getLogger(XmppServer.class);
	
	private int								maxRetryCount	= Integer.MAX_VALUE;
	private long							retryTimeout	= 30 * 1000;							// 30s
																									
	private String							ip				= "0.0.0.0";
	private int								port			= 5222;
	private ServerBootstrap					bootstrap		= null;
	
	private AuthProxy						authProxy		= null;
	private SSLConfig						sslConfig		= null;
	private ConnectManager					connectManager	= ConnectManager
																	.getInstance();
	
	private EventLoopGroup					bossGroup		= null;
	private EventLoopGroup					workerGroup		= null;
	private Class<? extends ServerChannel>	channelClass	= null;
	
	public synchronized void serverStart() throws Exception {
		
		// convert config to ssl config
		sslConfig = new SSLConfig();
		convert(sslConfig);
		sslConfig.init();
		
		bootstrap = new ServerBootstrap();
		
		// init groups and channelClass
		if (isUseLinuxNativeEpoll()) {
			bossGroup = new EpollEventLoopGroup(getBossThreads(),
					new DefaultThreadFactory("TCPServerBoss"));
			workerGroup = new EpollEventLoopGroup(getWorkerThreads(),
					new DefaultThreadFactory("TCPServerWorker"));
			channelClass = EpollServerSocketChannel.class;
		} else {
			bossGroup = new NioEventLoopGroup(getBossThreads(),
					new DefaultThreadFactory("TCPServerBoss"));
			workerGroup = new NioEventLoopGroup(getWorkerThreads(),
					new DefaultThreadFactory("TCPServerWorker"));
			channelClass = NioServerSocketChannel.class;
		}
		bootstrap.group(bossGroup, workerGroup);
		bootstrap.channel(channelClass);
		
		// config options
		bootstrap.option(ChannelOption.SO_BACKLOG, 128);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		
		bootstrap.option(ChannelOption.ALLOCATOR,
				PooledByteBufAllocator.DEFAULT);
		
		// child option
		bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.childOption(ChannelOption.ALLOCATOR,
				PooledByteBufAllocator.DEFAULT);
		
		// initializer
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				final ChannelPipeline pipeline = ch.pipeline();
				
				// -------------------
				// 靠近传输层
				// -------------------
				// InternalLoggerFactory
				// .setDefaultFactory(new Slf4JLoggerFactory());
				pipeline.addLast("logger", new LoggingHandler(LogLevel.DEBUG));
				
				// 超时handler 读超时0分钟，写超时0分钟，空闲超时9分钟
				pipeline.addLast("idleHandler", new IdleStateHandler(0, 0,
						9 * 60, TimeUnit.SECONDS));
				
				pipeline.addLast("idleStateAwareHandler",
						new IdleStateAwareHandler(connectManager));
				
				// encoder
				pipeline.addLast("stringEncoder",
						new StringEncoder(Charset.forName("UTF-8")));
				
				// xml decoder
				pipeline.addLast("xmlFrameDecoder", new XMLFrameDecoder());
				pipeline.addLast("xmlElementDecoder", new XMLElementDecoder());
				
				// xmpp decoder
				pipeline.addLast("xmppDecoder", new XMPPDecodeHandler(
						getServerName(), sslConfig));
				
				// xmpp 业务逻辑handler
				pipeline.addLast("xmppHandler", new XMPPHandler(
						getServerName(), sslConfig, authProxy));
				// -------------------
				// 靠近应用层
				// -------------------
			}
			
		});
		
		int retryCount = 0;
		boolean binded = false;
		do {
			try {
				bootstrap.bind(new InetSocketAddress(port)).addListener(
						new FutureListener<Void>() {
							
							@Override
							public void operationComplete(Future<Void> future)
									throws Exception {
								if (future.isSuccess()) {
									if (LOG.isInfoEnabled()) {
										LOG.info(
												"start succeed in ip:port => [{}]:[{}]",
												ip, port);
									}
								}
							}
						});
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
		
		// Bind a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				serverStop();
			}
		});
	}
	
	public synchronized void serverStop() {
		if (null != bootstrap) {
			this.bootstrap = null;
		}
		
		if (null != bossGroup) {
			bossGroup.shutdownGracefully();
		}
		
		if (null != workerGroup) {
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
	
}
