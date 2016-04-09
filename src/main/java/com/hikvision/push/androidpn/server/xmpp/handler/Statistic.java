/*
 * 上午10:58:05
 */
package com.hikvision.push.androidpn.server.xmpp.handler;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * TODO
 * 
 * @version $Id: Statistic, v 0.1 2015年5月21日 上午10:58:05 Exp $
 */
public class Statistic {
	
	private int		count	= 0;
	private boolean	tls		= false;
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public void addCount(int count) {
		this.count += count;
	}
	
	public boolean isTls() {
		return tls;
	}
	
	public void setTls(boolean tls) {
		this.tls = tls;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
}
