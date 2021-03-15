/*===========================================================================
  Copyright (C) 2013-2017 by the Okapi Framework contributors
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
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import net.sf.okapi.lib.xliff2.Const;

/**
 * Represent an extension (or un-supported module) element.
 */
public class ExtElement extends DataWithExtAttributes implements IExtChild {

	private final QName qName;
	private ArrayList<IExtChild> children;

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ExtElement (ExtElement original) {
		super(original);
		this.qName = original.qName;
		if ( original.children != null ) {
			if ( children == null ) children = new ArrayList<>();
			for ( IExtChild child : original.children ) {
				children.add(CloneFactory.create(child));
			}
		}
	}
	
	/**
	 * Creates a new {@link ExtElement} object.
	 * @param qName the qualified name of the element.
	 */
	public ExtElement (QName qName) {
		this.qName = qName;
	}
	
	/**
	 * Gets the QName of this element. 
	 * @return the QName of this element.
	 */
	public QName getQName () {
		return qName;
	}

	/**
	 * Gets the {@link ExtAttributes} object for this element.
	 * This is kept for backward compatibility, use {@link #getExtAttributes()} instead. 
	 * @return the {@link ExtAttributes} object for this element.
	 */
	@Deprecated
	public ExtAttributes getAttributes () {
		return getExtAttributes();
	}

	/**
	 * Gets the {@link ExtChildType} type for this element.
	 * @return the {@link ExtChildType} type for this element.
	 */
	@Override
	public ExtChildType getType () {
		return ExtChildType.ELEMENT;
	}

	/**
	 * Indicates if this element has at least one child.
	 * @return true if this element has at least one child, false otherwise.
	 */
	public boolean hasChild () {
		if ( children == null ) return false;
		return (children.size() > 0);
	}
	
	/**
	 * Gets the list of the children of this element.
	 * An empty list is created if needed. 
	 * @return list of the children of this element.
	 * @see #hasChild()
	 */
	public List<IExtChild> getChildren () {
		if ( children == null ) children = new ArrayList<>();
		return children;
	}

	/**
	 * Gets the first element child for a given namespace and name of this element.
	 * @param namespaceURI the namespace URI of the element to retrieve.
	 * @param localName the name of the element to retrieve.
	 * @return the first child element of the given namespace and name if it exists,
	 * or null if it does not.
	 */
	public ExtElement getFirstElement (String namespaceURI,
		String localName)
	{
		if ( children == null ) return null;
		QName qn = new QName(namespaceURI, localName);
		for ( IExtChild child : children ) {
			if ( child.getType() != ExtChildType.ELEMENT ) continue;
			if ( ((ExtElement)child).getQName().equals(qn) ) {
				return (ExtElement)child;
			}
		}
		return null;
	}

	/**
	 * Gets the first child element of this element.
	 * @return the first child element of this element, or null if there is no child element.
	 */
	public ExtElement getFirstElement () {
		if ( children == null ) return null;
		for ( IExtChild child : children ) {
			if ( child.getType() == ExtChildType.ELEMENT ) {
				return (ExtElement)child;
			}
		}
		return null; // No element found
	}

	/**
	 * Gets the first child content of this element.
	 * @return the first child content of this element, or null if there is no child content.
	 */
	public ExtContent getFirstContent () {
		if ( children == null ) return null;
		for ( IExtChild child : children ) {
			if ( child.getType() != ExtChildType.ELEMENT ){
				return (ExtContent)child;
			}
		}
		return null; // no content found
	}

	/**
	 * Adds a {@link IExtChild} object to this element.
	 * @param child the object to add.
	 * @return the added object.
	 */
	public IExtChild addChild (IExtChild child) {
		if ( children == null ) children = new ArrayList<>();
		children.add(child);
		return child;
	}
	
	/**
	 * Adds a child element to this element.
	 * @param namespaceURI the namespace URI of the element to add.
	 * @param localPart the local part of the name of the element to add.
	 * @param prefix the prefix of the name of the element to add.
	 * @return the added element.
	 */
	public ExtElement addElement (String namespaceURI,
		String localPart,
		String prefix)
	{
		return (ExtElement)addChild(new ExtElement(
			new QName(namespaceURI, localPart, prefix)));
	}
	
	/**
	 * Adds a child element to this element, both of the same namespace.
	 * @param localPart the local part of the name of the element to add.
	 * @return the added element.
	 */
	public ExtElement addElement (String localPart) {
		return addElement(qName.getNamespaceURI(), localPart, qName.getPrefix());
	}
	
	/**
	 * Adds a text content to this element.
	 * @param text the text to add.
	 * @return the added {@link ExtContent} object.
	 */
	public ExtContent addContent (String text) {
		return (ExtContent)addChild(new ExtContent(text));
	}

	/**
	 * Indicates if this extension element is part of a module or not.
	 * @return true if this extension element is part of a module.
	 */
	public boolean isModule () {
		return qName.getNamespaceURI().startsWith(Const.NS_XLIFF_MODSTART);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), children, qName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ExtElement other = (ExtElement) obj;
		if (children == null) {
			if (other.children != null) {
				return false;
			}
		} else if (!children.equals(other.children)) {
			return false;
		}
		if (qName == null) {
			if (other.qName != null) {
				return false;
			}
		} else if (!qName.equals(other.qName)) {
			return false;
		}
		return true;
	}

}
