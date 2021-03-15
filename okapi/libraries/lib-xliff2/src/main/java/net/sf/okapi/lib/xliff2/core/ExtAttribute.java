/*===========================================================================
  Copyright (C) 2011-2013 by the Okapi Framework contributors
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

import java.util.Objects;

import javax.xml.namespace.QName;

import net.sf.okapi.lib.xliff2.Const;

/**
 * Represents an extension (or unsupported module) attribute.
 */
public class ExtAttribute {

	private final QName qName;
	private String value;

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ExtAttribute (ExtAttribute original) {
		this(original.qName, original.value);
	}
	
	/**
	 * Creates a new {@link ExtAttribute} object.
	 * @param qName the qualified name of this attribute.
	 * @param value the value of this attribute.
	 */
	public ExtAttribute (QName qName,
		String value)
	{
		this.qName = qName;
		this.value = value;
	}

	/**
	 * Gets the QName of this attribute.
	 * @return the QName of this attribute.
	 */
	public QName getQName () {
		return qName;
	}
	
	/**
	 * Gets the value of this attribute.
	 * @return the value of this attribute (can be null).
	 */
	public String getValue () {
		return value;
	}
	
	/**
	 * Sets the value of this attribute.
	 * @param value the new value of this attribute (can be null).
	 */
	public void setValue (String value) {
		this.value = value;
	}
	
	/**
	 * Gets the local name of this attribute.
	 * @return the local name of this attribute.
	 */
	public String getLocalPart () {
		return qName.getLocalPart();
	}
	
	/**
	 * Gets the namespace URI of this attribute.
	 * @return the namespace URI of this attribute.
	 */
	public String getNamespaceURI () {
		return qName.getNamespaceURI();
	}
	
	/**
	 * Gets the prefix of this attribute.
	 * @return the prefix of this attribute.
	 */
	public String getPrefix () {
		return qName.getPrefix();
	}

	/**
	 * Indicates if this attribute is part of a module or not.
	 * @return true if this attribute is part of a module.
	 */
	public boolean isModule () {
		return qName.getNamespaceURI().startsWith(Const.NS_XLIFF_MODSTART);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (!(other instanceof ExtAttribute)) return false;

		ExtAttribute that = (ExtAttribute) other;
		return Objects.equals(qName, that.qName) &&
				Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(qName, value);
	}
}
