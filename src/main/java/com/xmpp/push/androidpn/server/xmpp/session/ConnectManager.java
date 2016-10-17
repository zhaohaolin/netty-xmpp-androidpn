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
package com.xmpp.push.androidpn.server.xmpp.session;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.IQUtils;
import com.xmpp.push.androidpn.server.SessionStatus;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ;
import com.xmpp.push.androidpn.server.xmpp.stanza.JID;

/**
 * This class manages the sessions connected to the server.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class ConnectManager {
	
	private static final Logger				LOG			= LoggerFactory
																.getLogger(ConnectManager.class);
	private static final ConnectManager		INSTANCE	= new ConnectManager();
	
	private String							resourceName;
	private String							serverName;
	private String							apiKey;
	private Map<String, Connection>			connectMap	= new ConcurrentHashMap<String, Connection>();
	private Map<String, String>				channelMap	= new ConcurrentHashMap<String, String>();
	private ExecutorService					exec		= Executors
																.newSingleThreadExecutor();
	
	private AtomicLong						balance		= new AtomicLong(0);
	private BlockingQueue<MobileDevStatus>	queue		= new LinkedBlockingQueue<MobileDevStatus>();
	private MobileDevStatusListener			mobileDevStatusListener;
	
	private ConnectManager() {
		//
	}
	
	public Map<String, Connection> getConnectmap() {
		return connectMap;
	}
	
	public Map<String, String> getChannelmap() {
		return channelMap;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	
	public String getServerName() {
		return serverName;
	}
	
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	
	public String getApiKey() {
		return apiKey;
	}
	
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public final static ConnectManager getInstance() {
		return INSTANCE;
	}
	
	public void registMobileDevStatusListener(MobileDevStatusListener listener) {
		this.mobileDevStatusListener = listener;
	}
	
	public final synchronized void start() {
		// 手机在线状态修改任务
		exec.execute(new Runnable() {
			
			@Override
			public void run() {
				LOG.info("开始Android设备状态维护任务......");
				try {
					while (!Thread.currentThread().isInterrupted()) {
						MobileDevStatus mobileDevStatus = queue.take();
						String deviceid = mobileDevStatus.getClientId();
						SessionStatus type = mobileDevStatus.getStatus();
						if (mobileDevStatusListener != null) {
							mobileDevStatusListener
									.changeStatus(deviceid, type);
						}
					}
				} catch (Exception e) {
					LOG.warn("Android设备状态维护任务遇到中断异常: " + e);
				}
				LOG.info("结束Android设备状态维护任务......");
			}
			
		});
	}
	
	public final synchronized void stop() {
		if (null != exec) {
			exec.shutdownNow();
		}
	}
	
	public final void addConnect(final Connection connect) {
		if (serverName == null) {
			throw new IllegalStateException("Server not initialized");
		}
		
		String clientId = connect.getDeviceid();
		
		// WARN 这儿要检查是否有连接存在，如果存在就先断开以前的连接，以保证客户端只能有一个连接
		Connection exist = connectMap.get(clientId);
		if (null != exist) {
			LOG.warn(
					"deviceId=[{}] connect exist for connection=[{}]  deivceId=[{}] will closed.",
					new Object[] { exist.getDeviceid(), exist,
							exist.getDeviceid() });
			Channel channel = exist.getChannel();
			if (channel != null) {
				LOG.warn(
						"channel=[{}] will closed. channelId=[{}], deviceId=[{}]",
						new Object[] { channel, exist.getId(),
								exist.getDeviceid() });
				connectMap.remove(exist.getDeviceid());
				channel.close();
				exist = null;
			}
		}
		
		// 添加连接到hashmap中
		connectMap.put(clientId, connect);
		
		if (LOG.isInfoEnabled()) {
			LOG.info("deviceId=[{}], connect=[{}], add connectMap", clientId,
					connect);
		}
		
		channelMap.put(connect.getId(), clientId);
		if (LOG.isDebugEnabled()) {
			LOG.debug("channelId=[{}] deviceId=[{}] put to channelMap.",
					connect.getId(), clientId);
		}
		
		// 修改手机上线状态
		final MobileDevStatus mobileDevStatus = new MobileDevStatus(
				connect.getDeviceid(), SessionStatus.ANDROID_UP);
		
		queue.offer(mobileDevStatus);
		
		if (LOG.isDebugEnabled())
			LOG.debug("ClientSession created.");
	}
	
	/**
	 * 
	 * 判断这个deviceid的客户端是否存在
	 * 
	 * @author joe.zhao(zhaohaolin@hikvision.com.cn) 2015年5月15日 下午4:23:28
	 * @param deviceid
	 * @return
	 */
	public final boolean contains(String deviceid) {
		return connectMap.containsKey(deviceid);
	}
	
	/**
	 * 
	 * 根据deviceid直接发送消息到客户端手机上
	 * 
	 * @param clientId
	 * @param message
	 * @param ext
	 * 
	 */
	public final boolean sendMessage(String clientId, String id,
			String message, String ext, String timestamp) {
		if (!contains(clientId)) {
			LOG.error(
					"deviceid=[{}] has no channel to send message=[{}], ext=[{}].",
					new Object[] { clientId, message, ext });
			return false;
		}
		
		Connection connect = connectMap.get(clientId);
		if (null == connect) {
			LOG.error(
					"deviceid=[{}] has no channel to send message=[{}], ext=[{}].",
					new Object[] { clientId, message, ext });
			return false;
		}
		
		Channel channel = connect.getChannel();
		
		// convert message to xmpp protocol
		IQ iq = IQUtils.createIQ(apiKey, id, message, ext, timestamp);
		if (null == iq) {
			LOG.error("create iq failer. apiKey=[{}], message=[{}], ext=[{}]",
					new Object[] { apiKey, message, ext });
			return false;
		}
		
		iq.setFrom(JID.jid(connect.getDeviceid(), serverName, resourceName));
		iq.setTo(JID.jid(connect.getDeviceid(), connect.getTarget(),
				resourceName));
		
		// send message to channel
		boolean ret = IQUtils.send(channel, iq);
		return ret;
	}
	
	public final int getOnlineSessionSize() {
		// return balance.intValue();
		return channelMap.size();
	}
	
	public final Connection getConnectByChannelId(final String channelId) {
		if (channelMap.containsKey(channelId)) {
			String deviceid = channelMap.get(channelId);
			return connectMap.get(deviceid);
		}
		return null;
	}
	
	/**
	 * 
	 * 根据channelId删除客户端连接
	 * 
	 * @param channelId
	 * @return
	 */
	public final boolean removeConnect(final String channelId) {
		String deviceid = channelMap.get(channelId);
		if (StringUtils.isEmpty(deviceid)) {
			
			if (LOG.isDebugEnabled())
				LOG.debug("channelId=[{}] get deviceid is null or empty.",
						channelId);
			return false;
		}
		
		if (LOG.isInfoEnabled())
			LOG.info("close connect deviceid=[{}], channelId=[{}]", deviceid,
					channelId);
		
		channelMap.remove(channelId);
		return removeConnect(channelId, deviceid);
	}
	
	/**
	 * 
	 * 根据deviceid删除客户端连接
	 * 
	 * @param clientId
	 * @return
	 */
	public final boolean removeConnect(final String channelId,
			final String clientId) {
		delBalance();
		
		Connection connect = connectMap.get(clientId);
		if (null == connect) {
			LOG.warn("deviceId=[{}] channelId=[{}] get connect is null.",
					clientId, channelId);
			return false;
		}
		
		// WARN 如果不是本channelId的，也不应该删除
		if (channelId != connect.getChannel().id().asLongText()) {
			LOG.warn(
					"deviceId=[{}] channelId=[{}] is not this channelId=[{}].",
					clientId, channelId, connect.getChannel().id());
			return false;
		}
		
		connectMap.remove(clientId);
		
		if (LOG.isInfoEnabled())
			LOG.info("设备ID: [{}] 手机设备离线接收到事件通知", clientId);
		
		final MobileDevStatus mobileDevStatus = new MobileDevStatus(clientId,
				SessionStatus.ANDROID_DOWN);
		queue.offer(mobileDevStatus);
		
		if (LOG.isDebugEnabled())
			LOG.debug(
					"设备ID: [{}] 手机设备离线, 删除在SessionManager map 中的session, 放置到队列中",
					clientId);
		
		return true;
	}
	
	public synchronized void addBalance() {
		balance.incrementAndGet();
	}
	
	public synchronized void delBalance() {
		if (balance.get() > 0)
			balance.decrementAndGet();
	}
	
	/**
	 * Closes the all sessions.
	 */
	public void closeAllSessions() {
		try {
			// FIXME
		} catch (Exception e) {
			LOG.warn("closeAllSessions：关闭所有连接失败", e);
		}
	}
	
	private class MobileDevStatus {
		
		private String			clientId;
		private SessionStatus	status;
		
		/**
		 * 创建一个新的实例MobileDevStatus.
		 * 
		 * @param clientId
		 * @param status
		 * @param clientSession
		 */
		public MobileDevStatus(String clientId, SessionStatus status) {
			this.clientId = clientId;
			this.status = status;
		}
		
		public String getClientId() {
			return clientId;
		}
		
		public SessionStatus getStatus() {
			return status;
		}
		
	}
	
}
