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

package com.hikvision.push.androidpn.server.xmpp.xml;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * XML DOM utilities.
 */
public final class XMLUtil {
	
	private static final DocumentBuilder	DOCBUILDER;
	private final static TransformerFactory	transformFactory	= TransformerFactory
																		.newInstance();
	static {
		try {
			final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			docBuilderFactory.setIgnoringElementContentWhitespace(true);
			docBuilderFactory.setIgnoringComments(true);
			// docBuilderFactory.setValidating(true);
			
			DOCBUILDER = docBuilderFactory.newDocumentBuilder();
			
		} catch (final ParserConfigurationException e) {
			throw new InternalError("Error creating Document Builder");
		}
	}
	
	/**
	 * Returns a new Document from the current DocumentBuidler.
	 * 
	 * @return a new Document
	 */
	public static final Document newDocument() {
		return DOCBUILDER.newDocument();
	}
	
	/**
	 * Parses a String into a new Element.
	 * 
	 * @param element the String to parse
	 * @return the parsed Element
	 */
	public static final Element fromString(final String element) {
		try {
			final Document doc = newDocument();
			Transformer transform = transformFactory.newTransformer();
			
			// 设置字符编码为UTF-8
			transform.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			
			transform.transform(new StreamSource(new StringReader(element)),
					new DOMResult(doc));
			return doc.getDocumentElement();
		} catch (final TransformerException e) {
			throw new InternalError("Transformer error");
		}
	}
	
	/**
	 * Returns the String representation of an Element.
	 * 
	 * @param element the Element to convert
	 * @return the String representation of a Element
	 */
	public static final String toString(final Element element) {
		
		final StringWriter buffer = new StringWriter();
		try {
			Transformer transform = transformFactory.newTransformer();
			
			transform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			// 写完第一行之后是否换行
			transform.setOutputProperty(OutputKeys.INDENT, "no");
			
			// 设置字符编码为UTF-8
			transform.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			
			transform.transform(new DOMSource(element),
					new StreamResult(buffer));
			
			return buffer.toString();
		} catch (final TransformerException e) {
			throw new InternalError("Transformer error");
		} finally {
			buffer.flush();
			try {
				buffer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private XMLUtil() {
	}
	
}
