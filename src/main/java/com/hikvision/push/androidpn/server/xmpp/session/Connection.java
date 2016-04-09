/*
 * Connection.java Creator: joe.zhao Create-Date:
 * 下午4:11:18
 */
package com.hikvision.push.androidpn.server.xmpp.session;

import io.netty.channel.Channel;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.hikvision.push.androidpn.server.xmpp.auth.AuthToken;

/**
 * TODO
 * 
 * @version $Id: Connection, v 0.1 2015年5月15日 下午4:11:18 Exp $
 */
public class Connection implements Serializable {
	
	/**
	 * TODO
	 */
	private static final long	serialVersionUID	= 1L;
	private final int			id;
	private final Channel		channel;
	private final String		deviceid;
	private final String		resource;
	private final String		target;
	private AuthToken			authToken;
	
	public Connection(int id, Channel channel, String deviceid,
			String resource, String target) {
		this.id = id;
		this.channel = channel;
		this.deviceid = deviceid;
		this.resource = resource;
		this.target = target;
	}
	
	public int getId() {
		return id;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	public String getDeviceid() {
		return deviceid;
	}
	
	public String getResource() {
		return resource;
	}
	
	public String getTarget() {
		return target;
	}
	
	public AuthToken getAuthToken() {
		return authToken;
	}
	
	public void setAuthToken(AuthToken authToken) {
		this.authToken = authToken;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
}
