package com.xmpp.push.androidpn.server.xmpp.router;

import io.netty.channel.Channel;

import com.xmpp.push.androidpn.server.xmpp.stanza.Packet;

public interface StanzaRouter<T extends Packet> {
	
	public void route(Channel channel, T packet);
	
}
