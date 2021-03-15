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

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.BaseList;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;

/**
 * Represents the <code>&lt;revision&gt;</code> element of the <a href=
 * 'http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#changeTracking_module'>Chan
 * g e Tracking module</a>.
 * 
 * @author Marta Borriello
 * 
 */
public class Revision extends BaseList<Item> implements IWithExtAttributes {

	/** The tag element name constant. */
	public static final String TAG_NAME = "revision";

	/** <code>author</code> attribute name. */
	public static final String AUTHOR_ATTR_NAME = "author";

	/** <code>datetime</code> attribute name. */
	public static final String DATETIME_ATTR_NAME = "datetime";

	/** <code>version</code> attribute name. */
	public static final String VERSION_ATTR_NAME = "version";

	/**
	 * Indicates the user or agent that created or modified the referenced
	 * element or its attributes.
	 */
	private String author;

	/**
	 * Indicates the date and time the referenced element or its attributes were
	 * created or modified.
	 */
	private String datetime;

	/**
	 * Indicates the version of the referenced element or its attributes that
	 * were created or modified.
	 */
	private String version;

	/** Attributes from other namespaces. */
	private ExtAttributes xattrs;

	/**
	 * Creates a new {@link Revision} object.
	 */
	public Revision () {
		// Nothing to do
	}
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Revision (Revision original) {
		super(original);
	}

	/**
	 * Gets the author attribute value.
	 * 
	 * @return the author attribute value.
	 */
	public String getAuthor () {
		return author;
	}

	/**
	 * Sets the value for the author attribute.
	 * 
	 * @param author
	 *            the value for the author attribute.
	 */
	public void setAuthor (String author) {
		this.author = author;
	}

	/**
	 * Gets the value of the datetime attribute.
	 * 
	 * @return the value of the datetime attribute.
	 */
	public String getDatetime () {
		return datetime;
	}

	/**
	 * Sets the value of the datetime attribute.
	 * 
	 * @param datetime
	 *            the value of the datetime attribute.
	 */
	public void setDatetime (String datetime) {
		//TODO: validate the date/time format?
		this.datetime = datetime;
	}

	/**
	 * Gets the value of the version attribute.
	 * 
	 * @return the value of the version attribute.
	 */
	public String getVersion () {
		return version;
	}

	/**
	 * Sets the value of the version attribute.
	 * 
	 * @param version
	 *            the value of the version attribute.
	 */
	public void setVersion (String version) {
		// Null is OK
		if ( (version != null) && !Util.isValidNmtoken(version) ) {
			throw new IllegalArgumentException(String.format("The value of '%s' must be an NMTOKEN.",
				Revision.VERSION_ATTR_NAME));
		}
		this.version = version;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.lib.xliff2.core.IWithExtAttributes#getExtAttributes()
	 */
	@Override
	public ExtAttributes getExtAttributes () {
		if ( xattrs == null ) {
			xattrs = new ExtAttributes();
		}
		return xattrs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.lib.xliff2.core.IWithExtAttributes#setExtAttributes(net.
	 * sf.okapi.lib.xliff2.core.ExtAttributes)
	 */
	@Override
	public void setExtAttributes (ExtAttributes attributes) {

		this.xattrs = attributes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.okapi.lib.xliff2.core.IWithExtAttributes#hasExtAttribute()
	 */
	@Override
	public boolean hasExtAttribute () {
		return xattrs != null && !xattrs.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.sf.okapi.lib.xliff2.core.IWithExtAttributes#getExtAttributeValue(
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String getExtAttributeValue (String namespaceURI,
		String localName)
	{
		String xattrValue = null;
		if ( hasExtAttribute() ) {
			xattrValue = xattrs.getAttributeValue(namespaceURI, localName);
		}
		return xattrValue;
	}

	/**
	 * Gets the attributes string.
	 * 
	 * @return the attributes string.
	 */
	public String getAttributesString () {
		StringBuilder attrs = new StringBuilder();
		if ( author != null && !author.isEmpty() ) {
			attrs.append(" ");
			attrs.append(AUTHOR_ATTR_NAME);
			attrs.append("=\"");
			attrs.append(author);
			attrs.append("\"");
		}
		if ( datetime != null && !datetime.isEmpty() ) {
			attrs.append(" ");
			attrs.append(DATETIME_ATTR_NAME);
			attrs.append("=\"");
			attrs.append(datetime);
			attrs.append("\"");
		}
		if ( version != null && !version.isEmpty() ) {
			attrs.append(" ");
			attrs.append(VERSION_ATTR_NAME);
			attrs.append("=\"");
			attrs.append(version);
			attrs.append("\"");
		}
		return attrs.toString();
	}
	
	/**
	 * Gets the opening <code>revision</code> tag name.
	 * 
	 * @return the opening <code>revision</code> tag name.
	 */
	public String getOpeningTagName () {
		return Const.PREFIXCOL_TRACKINGSd + TAG_NAME;
	}
	
	/**
	 * Gets the complete <code>revision</code> closing tag.
	 * 
	 * @return the complete <code>revision</code> closing tag.
	 */
	public String getClosingTag () {
		return "</" + Const.PREFIXCOL_TRACKINGSd + TAG_NAME + ">";
	}

}
