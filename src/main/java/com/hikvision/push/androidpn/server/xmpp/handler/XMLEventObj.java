/*
 * 下午9:30:40
 */
package com.hikvision.push.androidpn.server.xmpp.handler;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * TODO
 * 
 * @version $Id: XMLEventObj, v 0.1 2015年5月20日 下午9:30:40 Exp $
 */
public class XMLEventObj {
	
	private final List<XMLEvent>	events	= new ArrayList<XMLEvent>();
	
	public final List<XMLEvent> getEvents() {
		return events;
	}
	
	public final void addEvent(XMLEvent event) {
		this.events.add(event);
	}
	
	public final boolean isEmpty() {
		return this.events.size() == 0 ? true : false;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
}
