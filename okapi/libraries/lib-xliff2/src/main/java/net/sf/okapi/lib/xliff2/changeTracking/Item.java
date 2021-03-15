/*===========================================================================
  Copyright (C) 2015 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.changeTracking;

import java.util.Objects;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.DataWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;

/**
 * Represents the <code>&lt;item&gt;</code> element of the <a href=
 * 'http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#changeTracking_module'>Chang
 * e Tracking module</a>.
 * 
 * @author Marta Borriello
 * 
 */
public class Item extends DataWithExtAttributes implements IWithExtAttributes {

	/** The tag element name constant. */
	public static final String TAG_NAME = "item";

	/** <code>content</code> value for <code>property</code> attribute. */
	public static final String PROPERTY_CONTENT_VALUE = "content";

	/** <code>property</code> attribute name. */
	public static final String PROPERTY_ATTR_NAME = "property";

	/** Indicates the type of revision data. It's a REQUIRED attribute. */
	private String property;

	/** The contained text. */
	private String text;

	/**
	 * Creates a new {@link Item} object.
	 */
	public Item () {
		// Nothing to do
	}

	/**
	 * Copy constructor
	 * @param original the original object to duplicate.
	 */
	public Item (Item original) {
		super(original);
		property = original.property;
		text = original.text;
	}

	/**
	 * Constructor.
	 * 
	 * @param property
	 *            the value for the REQUIRED attribute <code>property</code>.
	 * @see #setProperty(String)
	 */
	public Item (String property) {
		setProperty(property);
	}

	/**
	 * Sets the contained text.
	 * 
	 * @param text
	 *            the contained text.
	 */
	public void setText (String text) {
		this.text = text;
	}

	/**
	 * Gets the contained text.
	 * 
	 * @return the contained text.
	 */
	public String getText () {
		return text;
	}

	/**
	 * Sets the value of the property attribute. The value MUST be either
	 * <code>content</code> to signify the content of an element, or the name of
	 * the attribute relating to the revision data. If either a
	 * <code>null</code> or an empty string is provided, an
	 * <code>IllegalArgumentException</code> is thrown.
	 * 
	 * @param property
	 *            the value of the property attribute.
	 */
	public final void setProperty (final String property) {
		if ( Util.isNoE(property) ) {
			throw new IllegalArgumentException(
				"'property' is a required attribute and cannot be null.");
		}
		this.property = property;
	}

	/**
	 * Gets the value of the property attribute.
	 * 
	 * @return the value of the property attribute.
	 */
	public String getProperty () {
		return property;
	}

	/**
	 * Gets the attributes string.
	 * 
	 * @return the attributes string.
	 */
	public String getAttributesString () {
		StringBuilder attrs = new StringBuilder();
		attrs.append(" ");
		attrs.append(PROPERTY_ATTR_NAME);
		attrs.append("=\"");
		attrs.append(property);
		attrs.append("\"");
		return attrs.toString();
	}

	/**
	 * Gets the opening <code>item</code> tag name.
	 * 
	 * @return the opening <code>item</code> tag name.
	 */
	public String getOpeningTagName () {
		return Const.PREFIXCOL_TRACKINGSd + TAG_NAME;
	}

	/**
	 * Gets the complete <code>item</code> closing tag.
	 * 
	 * @return the complete <code>item</code> closing tag.
	 */
	public String getClosingTag () {
		return "</" + Const.PREFIXCOL_TRACKINGSd + TAG_NAME + ">";
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), property, text);
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
		Item other = (Item) obj;
		if (property == null) {
			if (other.property != null) {
				return false;
			}
		} else if (!property.equals(other.property)) {
			return false;
		}
		if (text == null) {
			if (other.text != null) {
				return false;
			}
		} else if (!text.equals(other.text)) {
			return false;
		}
		return true;
	}

}
