/**
 * @ProjectName: 民用软件平台软件
 * @Copyright: 2012 HangZhou Hikvision System Technology Co., Ltd. All Right
 *             Reserved.
 * @address: http://www.hikvision.com
 * @date: 2014-5-17 下午3:58:19
 * @Description: 本内容仅限于杭州海康威视数字技术股份有限公司内部使用，禁止转发.
 */
package com.hikvision.push.androidpn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hikvision.push.androidpn.server.SessionStatus;
import com.hikvision.push.androidpn.server.xmpp.session.MobileDevStatusListener;

/**
 * <p>
 * </p>
 * 
 * @author hanlifeng 2014-5-17 下午3:58:19
 * @version V1.0
 * @modificationHistory=========================逻辑或功能性重大变更记录
 * @modify by user: {修改人} 2014-5-17
 * @modify by reason:{方法名}:{原因}
 */
public class MobileDevStatusListenerImpl implements MobileDevStatusListener {
	
	private static final Logger	log	= LoggerFactory
											.getLogger(MobileDevStatusListenerImpl.class);
	
	@Override
	public void changeStatus(String deviceid, SessionStatus type)
			throws Exception {
		if (type == SessionStatus.ANDROID_UP) {
			// 触发消息重传
			System.out.println(", deviceId=" + deviceid + ", status=" + type);
		}
	}
	
}
