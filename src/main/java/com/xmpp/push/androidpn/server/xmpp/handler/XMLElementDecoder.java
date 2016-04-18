/**
 * Copyright 2012 José Martínez
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.xmpp.push.androidpn.server.xmpp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import com.xmpp.push.androidpn.server.xmpp.xml.XMLElement;
import com.xmpp.push.androidpn.server.xmpp.xml.XMLElementImpl;
import com.xmpp.push.androidpn.server.xmpp.xml.XMLUtil;

/**
 * Processes XML Events into XML Elements.
 */
public class XMLElementDecoder extends MessageToMessageDecoder<Object>
		implements XMLStreamConstants {
	
	private final static Logger				LOG					= LoggerFactory
																		.getLogger(XMLElementDecoder.class);
	
	private static final XMLOutputFactory	XMLOUTPUTFACTORY	= new OutputFactoryImpl();
	
	public XMLElementDecoder() {
		//
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, Object msg,
			List<Object> out) throws Exception {
		
		if (LOG.isTraceEnabled()) {
			LOG.trace("receive msg=[{}], channel=[{}]",
					new Object[] { msg, ctx.channel() });
		}
		
		if (msg instanceof XMLEventObj) {
			
			XMLEventObj obj = (XMLEventObj) msg;
			if (obj.isEmpty()) {
				LOG.warn("xmlEventObj is empty.");
				return;
			}
			
			List<XMLEvent> events = obj.getEvents();
			
			Document document = XMLUtil.newDocument();
			DOMResult result = new DOMResult(document);
			XMLEventWriter writer = XMLOUTPUTFACTORY
					.createXMLEventWriter(result);
			if (null == writer || null == document) {
				LOG.warn("writer is null or document is null.");
				return;
			}
			
			for (XMLEvent event : events) {
				
				try {
					if (null != writer)
						writer.add(event);
				} catch (Exception ex) {
					//
				}
				
				if (event.getEventType() == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
					try {
						if (null != writer && null != document) {
							writer.flush();
							Element element = document.getDocumentElement();
							XMLElement xelement = XMLElementImpl
									.fromElement(element);
							ctx.fireChannelRead(xelement);
						}
					} catch (Exception ex) {
						// LOG.error("xml element decoder has execptor: " + ex);
					} finally {
						if (null != writer) {
							writer.close();
							writer = null;
						}
						result = null;
						document = null;
					}
				}
				
			}
			
			return;
		}
		
	}
	
}
