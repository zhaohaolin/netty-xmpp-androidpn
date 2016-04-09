package com.hikvision.push.androidpn.server;

/**
 * 
 * TODO
 * 
 * @version $Id: SessionStatus, v 0.1 2016年4月9日 上午11:29:56 Exp $
 */
public enum SessionStatus {
	
	ANDROID_UP(1), ANDROID_DOWN(2);
	
	private int	code;
	
	private SessionStatus(int code) {
		this.code = code;
	}
	
	public SessionStatus getByValue(int code) {
		for (SessionStatus each : values()) {
			if (each.code == code) {
				return each;
			}
		}
		return null;
	}
}
