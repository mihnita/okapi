/*===========================================================================
  Copyright (C) 2012-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Represents the context of the namespaces at a specific point during reading or writing of an XLIFF document.
 * The namespaces http://www.w3.org/XML/1998/namespace and http://www.w3.org/2000/xmlns/ are pre-defined.
 * This class implements the {@link NamespaceContext} interface. 
 */
public class NSContext implements NamespaceContext, Cloneable {
	
	private Hashtable<String, String> table;

	/**
	 * Creates a new object.
	 */
	public NSContext () {
		table = new Hashtable<>();
	}
	
	/**
	 * Creates a new context object and add one namespace to it.
	 * @param prefix the prefix of the namespace to add.
	 * @param uri the namespace URI.
	 */
	public NSContext (String prefix,
		String uri)
	{
		this();
		put(prefix, uri);
	}
	
	@Override
	public String toString () {
		return table.toString();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public NSContext clone () {
		NSContext copy = new NSContext();
		copy.table = (Hashtable<String, String>)table.clone();
		return copy;
	}

	@Override
	public String getNamespaceURI (String prefix) {
		if ( table.containsKey(prefix) )
			return table.get(prefix);
		if ( prefix.equals(XMLConstants.XML_NS_PREFIX) )
			return XMLConstants.XML_NS_URI;
		if ( prefix.equals(XMLConstants.XMLNS_ATTRIBUTE) )
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		else
			return XMLConstants.NULL_NS_URI;
	}

	@Override
	public String getPrefix (String uri) {
		Enumeration<String> E = table.keys();
		String key;
		while ( E.hasMoreElements() ) {
			key = E.nextElement();
			if ( table.get(key).equals(uri) )
				return key;
		}
		if ( uri.equals(XMLConstants.XML_NS_URI))
			return XMLConstants.XML_NS_PREFIX;
		if ( uri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI) )
			return XMLConstants.XMLNS_ATTRIBUTE;
		else
			return null;
	}

	@Override
	public Iterator<String> getPrefixes (String uri) {
		if ( uri.equals(XMLConstants.XML_NS_URI) ) {
			return Collections.singletonList(XMLConstants.XML_NS_PREFIX).iterator();
		}
		if ( uri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI) ) {
			return Collections.singletonList(XMLConstants.XMLNS_ATTRIBUTE).iterator();
		}
		List<String> res = new ArrayList<>();
		Enumeration<String> e = table.keys();
		String key;
		while ( e.hasMoreElements() ) {
			key = e.nextElement();
			if ( table.get(key).equals(uri) ) {
				res.add(key);
			}
		}
		return Collections.unmodifiableList(res).iterator();
	} 

	/**
	 * Sets a prefix/uri pair to this context. No checking is done for existing
	 * prefix: If the same is already defined, it will be overwritten.
	 * @param prefix the prefix of the namespace.
	 * @param uri the URI of the namespace.
	 */
	public void put (String prefix,
		String uri)
	{
		table.put(prefix, uri);
	}

	/**
	 * Indicates if this context has a given namespace URI associated with a given prefix.
	 * @param prefix the prefix to look for.
	 * @param uri the URI to look for.
	 * @return treu of the pair is in the context, false otherwise.
	 */
	public boolean containsPair (String prefix,
		String uri)
	{
		Iterator<String> iter = getPrefixes(uri);
		while ( iter.hasNext() ) {
			if ( iter.next().equals(prefix) ) return true;
		}
		return false;
	}

}
