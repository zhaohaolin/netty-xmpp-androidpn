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

package com.hikvision.push.androidpn.server.xmpp.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.evt.EventAllocatorImpl;
import com.fasterxml.aalto.evt.IncompleteEvent;
import com.fasterxml.aalto.stax.InputFactoryImpl;

/**
 * Decodes an XML stream into XML Events.
 */
public class XMLFrameDecoder extends FrameDecoder implements XMLStreamConstants {
	
	private static final AsyncXMLInputFactory					FACTORY	= new InputFactoryImpl();
	
	private static final EventAllocatorImpl						allocator;
	private final static Logger									LOG		= LoggerFactory
																				.getLogger(XMLFrameDecoder.class);
	private final AsyncXMLStreamReader<AsyncByteBufferFeeder>	reader;
	
	static {
		FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
				Boolean.FALSE);
		allocator = EventAllocatorImpl.getDefaultInstance();
	}
	
	public XMLFrameDecoder() {
		super(true);
		reader = FACTORY.createAsyncForByteBuffer();
	}
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buffer) throws Exception {
		
		Statistic statistic = (Statistic) channel.getAttachment();
		if (null == statistic) {
			statistic = new Statistic();
			channel.setAttachment(statistic);
		}
		// ++1
		statistic.addCount(1);
		
		if (LOG.isDebugEnabled())
			LOG.debug("decode buffer=[{}], channel=[{}]", buffer, channel);
		
		byte[] chunk = new byte[buffer.readableBytes()];
		buffer.readBytes(chunk);
		
		final List<XMLEventObj> result = new ArrayList<XMLEventObj>();
		
		try {
			reader.getInputFeeder().feedInput(ByteBuffer.wrap(chunk));
			//
			
			final Stack<Integer> stack = new Stack<Integer>();
			XMLEventObj obj = new XMLEventObj();
			while (reader.hasNext()) {
				int token = reader.next();
				XMLEvent event = allocator.allocate(reader);
				
				if (token == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
					
					// System.out.println("=====> result=" + result);
					
					if (obj.getEvents().contains(IncompleteEvent.instance())) {
						return result;
					}
					
					if (obj.getEvents().size() > 0) {
						obj.addEvent(IncompleteEvent.instance());
						result.add(obj);
					}
					return result;
				}
				
				obj.addEvent(event);
				
				if (token == START_ELEMENT) {
					stack.push(1);
				}
				
				if (token == END_ELEMENT) {
					if (!stack.isEmpty()) {
						stack.pop();
					}
				}
				
				// System.out.println("token=" + token + ", stack=" + stack);
				
				// 处理第一次请求
				if (statistic.getCount() == 1 && token == START_ELEMENT) {
					obj.addEvent(IncompleteEvent.instance());
					result.add(obj);
					obj = new XMLEventObj();
					
					if (!stack.isEmpty()) {
						stack.clear();
					}
					continue;
				}
				
				// tls 时，第三个请求处理
				if (statistic.isTls() && statistic.getCount() == 3) {
					obj.addEvent(IncompleteEvent.instance());
					result.add(obj);
					obj = new XMLEventObj();
					
					if (!stack.isEmpty()) {
						stack.clear();
					}
					continue;
				}
				
				if (stack.isEmpty() && statistic.getCount() != 1) {
					obj.addEvent(IncompleteEvent.instance());
					result.add(obj);
					obj = new XMLEventObj();
					
					if (!stack.isEmpty()) {
						stack.clear();
					}
					continue;
				}
			}
		} finally {
			// if (null != reader) {
			// reader.getInputFeeder().endOfInput();
			// }
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("result=[{}]", result);
		
		return result;
	}
}
