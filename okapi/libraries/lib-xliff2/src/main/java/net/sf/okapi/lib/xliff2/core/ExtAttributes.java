/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * Represents a set of {@link ExtAttribute} objects and associated namespaces.
 */
public class ExtAttributes implements Iterable<ExtAttribute> {

	private ArrayList<ExtAttribute> attrs;
	private Map<String, String> namespaces;
	private int autoPrefixCount = 1;

	/**
	 * Creates a new empty {@link ExtAttributes} object.
	 */
	public ExtAttributes () {
		// Argument-less constructor
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ExtAttributes (ExtAttributes original) {
		for ( ExtAttribute attr : original ) {
			setAttribute(new ExtAttribute(attr));
		}
		if ( original.hasNamespace() ) {
			for ( String key : original.getNamespaces() ) {
				setNamespace(original.getNamespacePrefix(key), key);
			}
		}
		autoPrefixCount = original.autoPrefixCount;
	}
	
	/**
	 * Gets the value for a given attribute name of a given namespace.
	 * @param namespaceURI the namespace URI of the attribute.
	 * @param localName the name of the attribute.
	 * @return the value (can be null), or null if not found.
	 */
	public String getAttributeValue (String namespaceURI,
		String localName)
	{
		if ( attrs == null ) return null;
		for ( ExtAttribute att : attrs ) {
			if ( att.getLocalPart().equals(localName)
				&& att.getNamespaceURI().equals(namespaceURI) ) {
				return att.getValue();
			}
		}
		return null;
	}
	
	/**
	 * Gets an attribute if it exists.
	 * @param namespaceURI the namespace URI of the attribute QName.
	 * @param localName the local name of the attribute.
	 * @return the attribute or null.
	 */
	public ExtAttribute getAttribute (String namespaceURI,
		String localName)
	{
		if ( attrs == null ) return null;
		for ( ExtAttribute att : attrs ) {
			if ( att.getLocalPart().equals(localName)
				&& att.getNamespaceURI().equals(namespaceURI) ) {
				return att;
			}
		}
		return null;
	}
	
	/**
	 * Sets a given attribute in this set.
	 * If an attribute with the same name and namespace URI already exists, the value of the
	 * existing one is replaced by the new one, but the attribute object itself is not changed.
	 * @param attribute the attribute to add/set.
	 * @return the set/added attribute (may be the attribute passed, or the existing one).
	 */
	public ExtAttribute setAttribute (ExtAttribute attribute) {
		if ( attrs == null ) {
			attrs = new ArrayList<>(2);
		}
		ensureNamespaceAndPrefix(attribute.getNamespaceURI(), attribute.getPrefix());
		int pos = 0;
		for ( ExtAttribute att : attrs ) {
			if ( att.getLocalPart().equals(attribute.getLocalPart())
				&& att.getNamespaceURI().equals(attribute.getNamespaceURI()) ) {
				attrs.set(pos, attribute);
				return attribute;
			}
			pos++;
		}
		attrs.add(attribute);
		return attribute;
	}

	/**
	 * Sets an attribute in this set. 
	 * If an attribute with the same name and namespace URI already exists, the value of the
	 * existing one is replaced by the new one, but the attribute object itself is not changed.
	 * @param namespaceURI the namespace URI of the attribute.
	 * @param localName the name of the attribute.
	 * @param value the value of the attribute.
	 * @return the set/added attribute.
	 */
	public ExtAttribute setAttribute (String namespaceURI,
		String localName,
		String value)
	{
		if ( attrs == null ) {
			attrs = new ArrayList<>(2);
		}
		ensureNamespaceAndPrefix(namespaceURI, null);
		ExtAttribute att = getAttribute(namespaceURI, localName);
		if ( att == null ) {
			att = new ExtAttribute(new QName(namespaceURI, localName, getNamespacePrefix(namespaceURI)), value);
			attrs.add(att);
		}
		att.setValue(value);
		return att;
	}

	/**
	 * Sets an attribute without namespace for this set.
	 * (this means the attribute will be part of the namespace 
	 * of the element where it is defined).
	 * @param localName the name of the attribute.
	 * @param value the value of the attribute.
	 * @return the new attribute.
	 */
	public ExtAttribute setAttribute (String localName,
		String value)
	{
		return setAttribute("", localName, value);
	}
	
	/**
	 * Makes sure there is a prefix associated with each namespace.
	 * If the given URI has no associated prefix, one is create automatically for it.
	 * @param namespaceURI the namespace URI to check.
	 * @param oriPrefix the prefix of the namespace (or null).
	 */
	private void ensureNamespaceAndPrefix (String namespaceURI,
		String oriPrefix)
	{
		if ( getNamespacePrefix(namespaceURI) == null ) {
			if ( namespaces == null ) {
				namespaces = new LinkedHashMap<>();
			}
			String prefix;
			// Try to re-use the prefix first
			if (( oriPrefix != null ) && !namespaces.containsValue(oriPrefix) ) {
				prefix = oriPrefix;
			}
			else {
				while ( true ) {
					prefix = "x"+autoPrefixCount;
					if ( namespaces.containsValue(prefix) ) autoPrefixCount++;
					else break;
				}
			}
			setNamespace(prefix, namespaceURI);
		}
	}

	/**
	 * Removes an attribute from this set.
	 * If the attribute is not found, nothing is done.
	 * @param namespaceURI the namespace URI of the attribute.
	 * @param localName the name of the attribute.
	 */
	public void deleteAttribute (String namespaceURI,
		String localName)
	{
		if ( attrs == null ) return;
		ExtAttribute att = getAttribute(namespaceURI, localName);
		if ( att == null ) return;
		attrs.remove(att);
	}

	/**
	 * Indicates if this set is empty or not.
	 * @return true if this set is empty, false if not.
	 */
	public boolean isEmpty () {
		if (( attrs != null ) && ( attrs.size() > 0 )) return false;
		if (( namespaces != null ) && ( namespaces.size() > 0 )) return false;
		return true;
	}
	
	/**
	 * Gets the number of attributes in this set.
	 * @return the number of attributes in this set.
	 */
	public int size () {
		if ( attrs == null ) return 0;
		return attrs.size();
	}

	/**
	 * Creates an iterator for the attributes in this set.
	 * @return a new iterator for the attributes in this set.
	 */
	@Override
	public Iterator<ExtAttribute> iterator () {
		if ( attrs == null ) {
			attrs = new ArrayList<>(2);
		}
		return attrs.iterator();
	}

	/**
	 * Sets a namespace in this set.
	 * @param prefix the namespace prefix.
	 * @param namespaceURI the namsepace URI.
	 */
	public void setNamespace (String prefix,
		String namespaceURI)
	{
		if ( namespaces == null ) {
			namespaces = new LinkedHashMap<>();
		}
		namespaces.put(namespaceURI, prefix);
	}

	/**
	 * Gets the prefix for a given namespace URI.
	 * @param namespaceURI the namespace URI.
	 * @return the prefix for the given namespace URI, or null if not found.
	 */
	public String getNamespacePrefix (String namespaceURI) {
		if ( namespaces == null ) return null;
		return namespaces.get(namespaceURI);
	}
	
	/**
	 * Indicates if this set has at least one namespace defined.
	 * @return true if this set has at least one namespace defined, false otherwise.
	 */
	public boolean hasNamespace () {
		if ( namespaces == null ) return false;
		return (namespaces.size() > 0);
	}

	/**
	 * Gets the set of keys for the namespaces in this object.
	 * @return the set of keys for the namespaces in this object.
	 */
	public Set<String> getNamespaces () {
		if ( namespaces == null ) return Collections.emptySet();
		return namespaces.keySet();
	}

}
