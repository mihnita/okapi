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

package net.sf.okapi.lib.xliff2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Represents a set of {@link ExtElement} objects.
 */
public class ExtElements implements Iterable<ExtElement> {

	private ArrayList<ExtElement> list;

	/**
	 * Creates an empty {@link ExtElements} object.
	 */
	public ExtElements () {
		// Nothing to do
	}

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ExtElements (ExtElements original) {
		if ( !original.isEmpty() ) {
			for ( ExtElement elem : original.list ) {
				add(new ExtElement(elem));
			}
		}
	}
	
	@Override
	public Iterator<ExtElement> iterator () {
		if ( list == null ) {
			list = new ArrayList<>(2);
		}
		return list.iterator();	}

	/**
	 * Adds an  element to this set.
	 * @param element the element to add.
	 * @return the added element (same as the parameter).
	 */
	public ExtElement add (ExtElement element) {
		if ( list == null ) {
			list = new ArrayList<>(2);
		}
		list.add(element);
		return element;
	}

	/**
	 * Adds a new {@link ExtElement} object to this set.
	 * @param namespaceURI the namespace URI of the element.
	 * @param localPart the locale part of the element name.
	 * @param prefix the prefix.
	 * @return the element created.
	 */
	public ExtElement add (String namespaceURI,
		String localPart,
		String prefix)
	{
		return add(new ExtElement(new QName(namespaceURI, localPart, prefix)));
	}

	/**
	 * Gets the number of elements in this set.
	 * @return the number of elements in this set.
	 */
	public int size () {
		if ( list == null ) return 0;
		return list.size();
	}

	/**
	 * Indicates if this set has at least one element.
	 * @return true if this set as at least one element, false if it has none.
	 */
	public boolean isEmpty () {
		return (( list == null ) || ( list.size() == 0 ));
	}
	
	/**
	 * Gets the element at a given position.
	 * @param index the position of the element to retrieve.
	 * @return the element at the given position.
	 * @throws IndexOutOfBoundsException if the position is invalid.
	 */
	public ExtElement get (int index) {
		if ( list == null ) throw new IndexOutOfBoundsException();
		return list.get(index);
	}
	
	/**
	 * Gets a list of all child elements for a given namespace and name
	 * for this element (not recursively)
	 * @param namespaceURI the namespace of the elements to list.
	 * @param localName the name of the elements to list.
	 * @return the list (it may be empty, but never null)
	 */
	public List<ExtElement> find (String namespaceURI,
		String localName)
	{
		if ( list == null ) return Collections.emptyList();
		QName qn = new QName(namespaceURI, localName);
		ArrayList<ExtElement> res = new ArrayList<>();
		for ( ExtElement elem : list ) {
			if ( elem.getQName().equals(qn) ) {
				res.add(elem);
			}
		}
		return res;
	}

	/**
	 * Get and if needed, create before, a given element from this set.
	 * @param namespaceURI the namespace of the element.
	 * @param localName the name of the element.
	 * @param prefix the prefix to use if it needs to be created.
	 * @return the element searched for: the first existing one, or one just created.
	 */
	public ExtElement getOrCreate (String namespaceURI,
		String localName,
		String prefix)
	{
		if ( list != null ) {
			QName qn = new QName(namespaceURI, localName);
			for ( ExtElement elem : list ) {
				if ( elem.getQName().equals(qn) ) {
					return elem;
				}
			}
		}
		// If we get here, it's because the element does not exists
		// So we add it
		return add(namespaceURI, localName, prefix);
	}
}
