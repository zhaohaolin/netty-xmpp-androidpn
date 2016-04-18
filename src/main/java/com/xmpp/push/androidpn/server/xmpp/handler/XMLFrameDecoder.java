/*
 * XMLStreamFrameDecoder.java Creator: joe.zhao Create-Date: 下午2:22:01
 */
package com.xmpp.push.androidpn.server.xmpp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteBuffer;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.evt.EventAllocatorImpl;
import com.fasterxml.aalto.stax.InputFactoryImpl;

/**
 * TODO
 * 
 * @author joe.zhao
 * @version $Id: XMLStreamFrameDecoder, v 0.1 2016年4月11日 下午2:22:01 Exp $
 */
public class XMLFrameDecoder extends ByteToMessageDecoder implements
		XMLStreamConstants {
	
	private static final AsyncXMLInputFactory					FACTORY	= new InputFactoryImpl();
	
	private static final EventAllocatorImpl						ALLOCATOR;
	private final static Logger									LOG		= LoggerFactory
																				.getLogger(XMLFrameDecoder.class);
	private final AsyncXMLStreamReader<AsyncByteBufferFeeder>	streamReader;
	
	static {
		FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
				Boolean.FALSE);
		ALLOCATOR = EventAllocatorImpl.getDefaultInstance();
	}
	
	public XMLFrameDecoder() {
		streamReader = FACTORY.createAsyncForByteBuffer();
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer,
			List<Object> out) throws Exception {
		if (LOG.isTraceEnabled()) {
			LOG.trace("decode buffer=[{}], channel=[{}]", buffer, ctx.channel());
		}
		
		if (buffer.isReadable()) {
			byte[] chunk = new byte[buffer.readableBytes()];
			buffer.readBytes(chunk);
			
			try {
				streamReader.getInputFeeder().feedInput(ByteBuffer.wrap(chunk));
				
				XMLEventObj xmlEventObj = new XMLEventObj();
				while (streamReader.hasNext()) {
					int token = streamReader.next();
					XMLEvent event = ALLOCATOR.allocate(streamReader);
					xmlEventObj.addEvent(event);
					
					if (token == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
						out.add(xmlEventObj);
						return;
					}
					
				}
			} finally {
				// if (null != reader) {
				// reader.getInputFeeder().endOfInput();
				// }
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("result=[{}]", out);
			}
			return;
		}
		
	}
}
