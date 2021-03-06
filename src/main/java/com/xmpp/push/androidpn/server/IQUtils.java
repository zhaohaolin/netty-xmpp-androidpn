/**
 * 
 */
package com.xmpp.push.androidpn.server;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmpp.push.androidpn.server.xmpp.XMPPNamespaces;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ;
import com.xmpp.push.androidpn.server.xmpp.stanza.IQ.Type;
import com.xmpp.push.androidpn.server.xmpp.stanza.Packet;
import com.xmpp.push.androidpn.server.xmpp.xml.XMLElement;

/**
 */
public abstract class IQUtils implements XMPPNamespaces {
	
	private final static Logger	LOG	= LoggerFactory.getLogger(IQUtils.class);
	
	/**
	 * <p>
	 * 生成XMPP格式的推送消息
	 * </p>
	 */
	public final static IQ createIQ(String apiKey, String id, String message,
			String ext, String timestamp) {
		IQ iq = new IQ(IQ.Type.set);
		iq.addExtension("notification", NOTIFICATION);
		XMLElement notification = iq.getExtension("notification", NOTIFICATION);
		if (null == notification) {
			LOG.error("create notification failer.");
			return null;
		}
		
		notification.setChildText("id", id);
		notification.setChildText("t", timestamp);
		notification.setChildText("apiKey", apiKey);
		notification.setChildText("message", message);
		notification.setChildText("ext", ext);
		
		return iq;
	}
	
	public final static boolean send(final Channel channel, final Packet packet) {
		checkNotNull(packet);
		if (channel == null || !channel.isActive()) {
			LOG.warn("Disconnected, can't send stanza: [{}], channel=[{}]",
					packet, channel);
			return false;
		}
		
		String msg = packet.getXML().toString();
		
		if (LOG.isDebugEnabled())
			LOG.debug("sending packet: [{}], channel=[{}]", msg, channel);
		
		if (channel.isWritable()) {
			channel.writeAndFlush(msg);
			return true;
		}
		return false;
	}
	
	public final static IQ createResultIQ(IQ iq) {
		if (!(iq.getType() == Type.get || iq.getType() == Type.set)) {
			throw new IllegalArgumentException(
					"IQ must be of type 'set' or 'get'. Original IQ: "
							+ iq.toString());
		}
		IQ result = new IQ(Type.result);
		result.setId(iq.getId());
		result.setFrom(iq.getTo());
		result.setTo(iq.getFrom());
		return result;
	}
	
}
