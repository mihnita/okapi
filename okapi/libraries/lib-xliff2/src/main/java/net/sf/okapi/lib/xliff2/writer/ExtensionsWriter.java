/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.lib.xliff2.writer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.QName;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.NSContext;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.ExtAttribute;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.ExtChildType;
import net.sf.okapi.lib.xliff2.core.ExtContent;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.IExtChild;
import net.sf.okapi.lib.xliff2.core.IWithExtElements;
import net.sf.okapi.lib.xliff2.core.ProcessingInstruction;

/**
 * Provides the methods to output extended elements and attributes.
 */
public class ExtensionsWriter {

	private StringBuilder sb;
    private String lb;
    private Stack<NSContext> nsStack;

    /**
     * Creates a {@link ExtensionsWriter} that use the current platform line-break.
     */
    public ExtensionsWriter () {
    	lb = System.getProperty("line.separator");
    }

    /**
     * Creates a {@link ExtensionsWriter} and sets the type of line-breaks to use.
     * @param lineBreak the line-break to use.
     */
    public ExtensionsWriter (String lineBreak) {
    	setLineBreak(lineBreak);
    }
    
    /**
     * Sets the line break to use for this document.
     * You must set this before calling any of the <code>create()</code> methods.
     * By default the line-break used is the one of the OS.
     * @param lineBreak the line break to use for this document.
     */
    public void setLineBreak (String lineBreak) {
    	lb = lineBreak;
    }

    /**
     * Builds the XLIFF output for a given list of {@link IExtChild} objects.
     * @param list the list to output.
     * @param nsStack the namespace context stack, or null to use a new one with just XLIFF in context.
     * @return the XLIFF representation of the input list.
     */
    public String buildExtChildren (List<IExtChild> list,
    	Stack<NSContext> nsStack)
    {
    	if (( list == null ) || list.isEmpty() ) {
    		return "";
    	}
    	sb = new StringBuilder();
    	// Create a stack if none is provided
    	if ( nsStack == null ) {
    		this.nsStack = new Stack<>();
    		this.nsStack.push(new NSContext("", Const.NS_XLIFF_CORE20));
//System.out.println("push-buildExtChildren:"+nsStack.peek().toString());
    	}
    	else this.nsStack = nsStack;
    	
    	for ( IExtChild child : list ) {
    		switch ( child.getType() ) {
    		case ELEMENT:
    			ExtElement xe = (ExtElement)child;
    			writeExtElement(xe, gatherNamespaces(null, xe));
    			break;
			case TEXT:
				sb.append(Util.toXML(((ExtContent)child).getText().replace("\n", lb), false));
				break;
			case CDATA:
				sb.append("<![CDATA["+((ExtContent)child).getText().replace("\n", lb)+"]]>");
				break;
			case PI:
				sb.append(((ProcessingInstruction)child).getPI().replace("\n", lb));
				break;
    		}
    	}
    	return sb.toString();
    }

    /**
     * Builds the XLIFF output for a given object that has extension objects.
     * @param parent the object with the extension objects.
     * @param nsStack the namespace context stack, or null to use a new one with just XLIFF in context.
     * @return the XLIFF representation of the extension objects.
     */
	public String buildExtElements (IWithExtElements parent,
		Stack<NSContext> nsStack)
	{
		if ( !parent.hasExtElements() ) {
			return "";
		}
    	sb = new StringBuilder();
    	// Create a stack if none is provided
    	if ( nsStack == null ) {
    		this.nsStack = new Stack<>();
    		this.nsStack.push(new NSContext("", Const.NS_XLIFF_CORE20));
//System.out.println("push-buildExtElements:"+nsStack.peek().toString());
    	}
    	else this.nsStack = nsStack;

    	for ( ExtElement elem : parent.getExtElements() ) {
			writeExtElement(elem, gatherNamespaces(null, elem));
		}
		// Whitespace after extension element are generally not preserved,
		// so we default with one line-break
		sb.append(lb);
		return sb.toString();
	}
	
	/**
	 * Builds the XLIFF output for a giveb {@link ExtAttributes} object.
	 * @param attributes the object to output.
     * @param nsStack the namespace context stack, or null to use a new one with just XLIFF in context.
	 * @return the XLIFF representation of the extension attributes.
	 */
	public String buildExtAttributes (ExtAttributes attributes,
		Stack<NSContext> nsStack)
	{
		if (( attributes == null ) || attributes.isEmpty() ) {
			return "";
		}
    	sb = new StringBuilder();
    	// Create a stack if none is provided
    	if ( nsStack == null ) {
    		this.nsStack = new Stack<>();
    		this.nsStack.push(new NSContext("", Const.NS_XLIFF_CORE20));
//System.out.println("push-buildExtAttributes:"+nsStack.peek().toString());
    	}
    	else this.nsStack = nsStack;

    	writeExtAttributes(attributes);
    	return sb.toString();
	}
	
	private void writeExtAttributes (ExtAttributes attributes) {
		if ( attributes == null ) return;
		
		NSContext nsCtx = nsStack.peek();
		for ( String namespaceURI : attributes.getNamespaces() ) {
			// Skip empty namespace and namespaces in scope
			if ( !namespaceURI.isEmpty() && ( nsStack.peek().getPrefix(namespaceURI) == null )) {
				String prefix = attributes.getNamespacePrefix(namespaceURI);
				if ( prefix != null ) {
					sb.append(" xmlns" + (prefix.isEmpty() ? "" : ":"+prefix) + "=\"" + namespaceURI + "\"");
					nsCtx.put(prefix, namespaceURI);
				}
			}
		}
		for ( ExtAttribute att : attributes ) {
			String prefix = nsCtx.getPrefix(att.getNamespaceURI());
			sb.append(" " + (Util.isNoE(prefix) ? "" : prefix+":") + att.getLocalPart()
				+ "=\"" + Util.toXML(att.getValue(), true) + "\"");
		}
	}

	private void writeExtElement (ExtElement element,
		Map<String, String> childrenNS)
	{
		QName qn = element.getQName();
		String prefix = "";
		if ( !qn.getPrefix().isEmpty() ) prefix = qn.getPrefix()+":";
		// Start tag
		sb.append("<"+prefix+qn.getLocalPart());
		
		// Namespaces for this element
		NSContext nsCtx = nsStack.push(nsStack.peek().clone());
//System.out.println("push<extElement>:"+nsStack.peek().toString());
		String ctxPrfx = nsCtx.getPrefix(qn.getNamespaceURI());
		if (( ctxPrfx == null ) || !ctxPrfx.equals(qn.getPrefix()) ) {
			// Not in scope so we write it
			sb.append(" xmlns" + (qn.getPrefix().isEmpty() ? "" : ":"+qn.getPrefix()) + "=\"" + qn.getNamespaceURI() + "\"");
			nsCtx.put(qn.getPrefix(), qn.getNamespaceURI());
		}
		// Extra namespaces used inside (to declare all on the first one)
		if ( childrenNS != null ) {
			for ( String uri : childrenNS.keySet() ) {
				ctxPrfx = nsCtx.getPrefix(uri);
				String locPrfx = childrenNS.get(uri);
				if (( ctxPrfx == null ) || !nsCtx.containsPair(ctxPrfx, uri) ) { //!ctxPrfx.equals(locPrfx) ) {
					// Not in scope so we write it
					sb.append(" xmlns" + (locPrfx.isEmpty() ? "" : ":"+locPrfx) + "=\"" + uri + "\"");
					nsCtx.put(locPrfx, uri); // And add it to the context
				}
			}
		}
		
		// Attributes
		writeExtAttributes(element.getExtAttributes());
		sb.append(">");
		// Content
		for ( IExtChild child : element.getChildren() ) {
			switch ( child.getType() ) {
			case ELEMENT:
				writeExtElement((ExtElement)child, null);
				break;
			case TEXT:
				sb.append(Util.toXML(((ExtContent)child).getText().replace("\n", lb), false));
				break;
			case CDATA:
				sb.append("<![CDATA["+((ExtContent)child).getText().replace("\n", lb)+"]]>");
				break;
			case PI:
				sb.append(((ProcessingInstruction)child).getPI().replace("\n", lb));
				break;
			}
		}
		// End tag
		sb.append("</"+prefix+qn.getLocalPart()+">");
		nsStack.pop();
//System.out.println("pop</extElement>:"+nsStack.peek().toString());
	}

	private Map<String, String> gatherNamespaces (Map<String, String> map,
		ExtElement element)
	{
		if ( map == null ) {
			map = new LinkedHashMap<>();
		}
		// Add this q-name if not in the list yet
		String prefix = element.getQName().getPrefix();
		String uri = element.getQName().getNamespaceURI();
		if ( !map.containsValue(prefix) ) {
			if ( prefix.isEmpty() && uri.equals(Const.NS_XLIFF_CORE20) ) {
				// Do not add: it's the default
			}
			else 
				map.put(uri, prefix);
		}
		// Check the children
		for ( IExtChild child : element.getChildren() ) {
			if ( child.getType() == ExtChildType.ELEMENT ) {
				gatherNamespaces(map, (ExtElement)child);
			}
		}
		return map;
	}

}
