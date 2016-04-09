/**
 * @ProjectName: 民用软件平台软件
 * @Copyright: 2012 HangZhou Hikvision System Technology Co., Ltd. All Right
 *             Reserved.
 * @address: http://www.hikvision.com
 * @date: 2014-5-17 下午3:57:26
 * @Description: 本内容仅限于杭州海康威视数字技术股份有限公司内部使用，禁止转发.
 */
package com.hikvision.push.androidpn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hikvision.push.androidpn.server.xmpp.auth.AuthProxy;
import com.hikvision.push.androidpn.server.xmpp.auth.AuthToken;
import com.hikvision.push.androidpn.server.xmpp.exception.UnauthenticatedException;

/**
 * <p>
 * </p>
 * 
 * @author hanlifeng 2014-5-17 下午3:57:26
 * @version V1.0
 * @modificationHistory=========================逻辑或功能性重大变更记录
 * @modify by user: {修改人} 2014-5-17
 * @modify by reason:{方法名}:{原因}
 */
public class AuthProxyImpl implements AuthProxy {
	
	private final static Logger	log			= LoggerFactory
													.getLogger(AuthProxyImpl.class);
	
	private static String		servername	= "serverName";
	
	@Override
	public AuthToken authenticate(String username)
			throws UnauthenticatedException, Exception {
		if (username == null) {
			throw new UnauthenticatedException();
		}
		username = username.trim().toLowerCase();
		if (username.contains("@")) {
			int index = username.indexOf("@");
			String domain = username.substring(index + 1);
			if (domain.equals(servername)) {
				username = username.substring(0, index);
			} else {
				throw new UnauthenticatedException();
			}
		}
		if (!isLegal(username)) {
			throw new UnauthenticatedException();
		}
		return new AuthToken(username);
	}
	
	@Override
	public boolean isPlainSupported() {
		return true;
	}
	
	/**
	 * 判断该用户-设备是否为合法的
	 * 
	 * @param username
	 * @return
	 * @throws DBException
	 */
	public boolean isLegal(String username) {
		System.out.println("username=" + username);
		return true;
	}
	
}
