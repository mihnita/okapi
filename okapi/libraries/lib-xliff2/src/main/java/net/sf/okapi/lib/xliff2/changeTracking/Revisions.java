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
 * Represents the <code>&lt;revisions&gt;</code> element of the <a href=
 * 'http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#changeTracking_module'>ChangeTracking module</a>.
 * 
 * @author Marta Borriello
 * 
 */
public class Revisions extends BaseList<Revision> implements IWithExtAttributes {

	/** The tag element name constant. */
	public static final String TAG_NAME = "revisions";

	/** <code>appliesTo</code> attribute name. */
	public static final String APPLIES_TO_ATTR_NAME = "appliesTo";

	/** <code>ref</code> attribute name. */
	public static final String REF_ATTR_NAME = "ref";

	/** <code>currentVersion</code> attribute name. */
	public static final String CURRENT_VERSION_ATTR_NAME = "currentVersion";

	/**
	 * Indicates a specific XLIFF element which is a sibling, or a child of a
	 * sibling element, to the change track module within the scope of the
	 * enclosing element. It's a REQUIRED attribute.
	 */
	private String appliesTo;

	/**
	 * Holds a reference to a single instance of an element that has multiple
	 * instances within the enclosing element.
	 */
	private String ref;

	/** Holds a reference to the most current version of a revision. */
	private String currentVersion;

	/** Attributes from other namespaces. */
	private ExtAttributes xattrs;

	
	/**
	 * Default constructor. Creates a new {@link Revisions} object.
	 */
	public Revisions () {
	}

	/**
	 * Creates a new {@link Revisions} object.
	 * 
	 * @param appliesTo
	 *            the value for the REQUIRED attribute <code>appliesTo</code>.
	 * @see #setAppliesTo(String)
	 */
	public Revisions (final String appliesTo) {
		setAppliesTo(appliesTo);
	}

	/**
	 * Creates a copy of an existing {@link Revisions} object
	 * 
	 * @param original
	 *            the existing object to duplicate
	 */
	public Revisions (Revisions original) {
		super(original);
	}

	/**
	 * Sets the value to the <code>appliesTo</code> attribute. It must be the
	 * name of any valid XLIFF element which is a sibling, or a child of a
	 * sibling element, to the change track module within the scope of the
	 * enclosing element. If a null or empty string is provided, an
	 * <code>IllegalArgumentException</code> is thrown.
	 * 
	 * @param appliesTo
	 *            the value for the appliesTo attribute.
	 */
	public final void setAppliesTo (final String appliesTo) {
		if ( Util.isNoE(appliesTo) ) {
			throw new IllegalArgumentException("'appliesTo' is a required attribute and cannot be null.");
		}
		if ( !Util.isValidNmtoken(appliesTo) ) {
			throw new IllegalArgumentException(String.format("The value of '%s' must be an NMTOKEN.",
				Revisions.APPLIES_TO_ATTR_NAME));
		}
		this.appliesTo = appliesTo;
	}

	/**
	 * Gets the value of the appliesTo attribute.
	 * 
	 * @return the value of the appliesTo attribute.
	 */
	public String getAppliesTo () {
		return appliesTo;
	}

	/**
	 * Sets the value for the ref attribute.
	 * 
	 * @param ref
	 *            the value for the ref attribute.
	 */
	public void setRef (String ref) {
		// Null is OK
		if ( (ref != null) && !Util.isValidNmtoken(ref) ) {
			throw new IllegalArgumentException(String.format("The value of '%s' must be an NMTOKEN.",
				Revisions.REF_ATTR_NAME));
		}
		this.ref = ref;
	}

	/**
	 * Gets the value for the ref attribute.
	 * 
	 * @return the value for the ref attribute (can be null).
	 */
	public String getRef () {
		return ref;
	}

	/**
	 * Sets the value of the <code>currentVersion</code> attribute.
	 * 
	 * @param currentVersion
	 *            must be the value of the <code>version</code> attribute of one
	 *            of the <code>revision</code> elements listed in the same
	 *            <code>revisions</code> element.
	 */
	public void setCurrentVersion (String currentVersion) {
		// Null is OK
		if ( (currentVersion != null) && !Util.isValidNmtoken(currentVersion) ) {
			throw new IllegalArgumentException(String.format("The value of '%s' must be an NMTOKEN.",
				Revisions.CURRENT_VERSION_ATTR_NAME));
		}
		this.currentVersion = currentVersion;
	}

	/**
	 * Gets the value of the <code>currentVersion</code> attribute.
	 * 
	 * @return the value of the <code>currentVersion</code> attribute.
	 */
	public String getCurrentVersion () {
		return currentVersion;
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
	 * Gets the opening <code>revisions</code> tag name.
	 * 
	 * @return the opening <code>revisions</code> tag name.
	 */
	public String getOpeningTagName () {
		return Const.PREFIXCOL_TRACKINGSd + TAG_NAME;
	}

	/**
	 * Gets the attributes string.
	 * 
	 * @return the attributes string.
	 */
	public String getAttributesString () {
		StringBuilder attrs = new StringBuilder();
		attrs.append(" ");
		attrs.append(APPLIES_TO_ATTR_NAME);
		attrs.append("=\"");
		attrs.append(appliesTo);
		attrs.append("\"");
		if ( ref != null && !ref.isEmpty() ) {
			attrs.append(" ");
			attrs.append(REF_ATTR_NAME);
			attrs.append("=\"");
			attrs.append(ref);
			attrs.append("\"");
		}
		if ( currentVersion != null && !currentVersion.isEmpty() ) {
			attrs.append(" ");
			attrs.append(CURRENT_VERSION_ATTR_NAME);
			attrs.append("=\"");
			attrs.append(currentVersion);
			attrs.append("\"");
		}
		return attrs.toString();
	}

	/**
	 * Gets the closing <code>changeTrack</code> tag name.
	 * 
	 * @return the closing <code>changeTrack</code> tag name.
	 */
	public String getClosingTagName () {
		return "/" + Const.PREFIXCOL_TRACKINGSd + TAG_NAME;
	}

	/**
	 * Gets the complete <code>revisions</code> opening tag.
	 * 
	 * @return the complete <code>revisions</code> opening tag.
	 */
	public String getCompleteOpeningTag () {
		StringBuilder openingTag = new StringBuilder();
		openingTag.append("<");
		openingTag.append(Const.PREFIXCOL_TRACKINGSd);
		openingTag.append(TAG_NAME);
		openingTag.append(" ");
		openingTag.append(APPLIES_TO_ATTR_NAME);
		openingTag.append("=\"");
		openingTag.append(appliesTo);
		openingTag.append("\"");
		if ( ref != null && !ref.isEmpty() ) {
			openingTag.append(" ");
			openingTag.append(REF_ATTR_NAME);
			openingTag.append("=\"");
			openingTag.append(ref);
			openingTag.append("\"");
		}
		if ( currentVersion != null && !currentVersion.isEmpty() ) {
			openingTag.append(" ");
			openingTag.append(CURRENT_VERSION_ATTR_NAME);
			openingTag.append("=\"");
			openingTag.append(currentVersion);
			openingTag.append("\"");
		}
		openingTag.append(">");
		return openingTag.toString();
	}

	/**
	 * Gets the complete <code>revisions</code> closing tag.
	 * 
	 * @return the complete <code>revisions</code> closing tag.
	 */
	public String getClosingTag () {
		return "</" + Const.PREFIXCOL_TRACKINGSd + TAG_NAME + ">";
	}
}
