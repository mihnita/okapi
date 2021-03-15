/*===========================================================================
  Copyright (C) 2014-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.validation;

import java.text.Normalizer;
import java.text.Normalizer.Form;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;

/**
 * Represents the &lt;rule> element of the 
 * <a href='http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#validation_module'>Validation module</a>.
 */
public class Rule implements IWithExtAttributes {

	/**
	 * Types of validation rule.
	 */
	public enum Type {
		ISPRESENT("isPresent"),
		ISNOTPRESENT("isNotPresent"),
		STARTSWITH("startsWith"),
		ENDSWITH("endsWith"),
		CUSTOM("custom");

		private String name;

		Type(String name) {
			this.name = name;
		}

		@Override
		public String toString () {
			return name;
		}
		
		public static Type fromString (String name) {
			if ( name == null ) {
				throw new InvalidParameterException("A rule type must not be null");
			}
			switch ( name ) {
			case "isPresent":
				return ISPRESENT;
			case "isNotPresent":
				return ISNOTPRESENT;
			case "startsWith":
				return STARTSWITH;
			case "endsWith":
				return ENDSWITH;
			// "custom" is not a valid value
			// Other values are invalid
			default:
				throw new InvalidParameterException(String.format("Invalid rule type value: '%s'.", name));
			}
		}
	}

	/**
	 * Form of normalization possible.
	 */
	public enum Normalization {
		NONE("none"),
		NFC("nfc"),
		NFD("nfd");

		private String name;

		Normalization(String name) {
			this.name = name;
		}

		@Override
		public String toString () {
			return name;
		}
		
		public static Normalization fromString (String name) {
			if ( name == null ) {
				throw new InvalidParameterException("A normalization value must not be null");
			}
			switch ( name ) {
			case "none":
				return NONE;
			case "nfc":
				return NFC;
			case "nfd":
				return NFD;
			default:
				throw new InvalidParameterException(String.format("Invalid normalization value: '%s'.", name));
			}
		}
	}

	private Type type = null;
	private String data;
	private String effectiveData;
	private int occurs;
	private boolean existsInSource = false;
	private boolean caseSensitive = true;
	private boolean enabled = true;
	private Normalization normalization = Normalization.NFC;
	private ExtAttributes xattrs;
	private boolean inherited = false;

	/**
	 * Creates a rule of a given type.
	 * @param type the name of the rule type.
	 * @param data the text data for the rule (can be null).
	 */
	public Rule (String type,
		String data)
	{
		this.type = Type.fromString(type);
		this.data = data;
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Rule (Rule original) {
		type = original.type;
		caseSensitive = original.caseSensitive;
		normalization = original.normalization;
		data = original.data;
		effectiveData = original.effectiveData;
		enabled = original.enabled;
		existsInSource = original.existsInSource;
		occurs = original.occurs;
		inherited = original.inherited;
		if ( original.hasExtAttribute() ) {
			xattrs = new ExtAttributes(original.xattrs);
		}
	}

	/**
	 * Gets a human readable representation of the rule.
	 * @return the text display of this rule.
	 */
	public String getDisplay () {
		StringBuilder tmp = new StringBuilder(type.toString() + "='"+data+"'"
			+ (" caseSensitive="+(caseSensitive ? "yes" : "no"))
			+ (" normalization="+normalization.toString()));
		// type-specific info
		switch ( type ) {
		case ISPRESENT:
			tmp.append(occurs>1 ? " occurs="+occurs : " occurs=once-or-more");
			// Fall thru
		case ENDSWITH:
		case STARTSWITH:
			tmp.append(" existsInSource=" + (existsInSource ? "yes" : "no"));
			break;
		case CUSTOM:
		case ISNOTPRESENT:
			break;
		}
		tmp.append(inherited ? " (inherited-rule)" : "");
		return tmp.toString();
	}
	
	public Type getType () {
		return type;
	}

	public void setType (Type type) {
		this.type = type;
	}

	public String getData () {
		return data;
	}

	public void setData (String data) {
		this.data = data;
	}

	/**
	 * Gets the effective text to use when applying the rule.
	 * For example if the rule is not case sensitive or need to be normalized, the original
	 * data string needs to be modified for applying the rule. The effective data is the
	 * result of those changes.
	 * @return the effective data for this rule.
	 */
	public String getEffectiveData () {
		return effectiveData;
	}

	public int getOccurs () {
		return occurs;
	}

	public void setOccurs (int occurs) {
		this.occurs = occurs;
	}

	/**
	 * Gets the flag 'existsInSource'.
	 * @return the flag 'existsinSource'
	 */
	public boolean getExistsInSource () {
		return existsInSource;
	}

	/**
	 * Sets the flag 'existsInSource'
	 * @param existsInSource the flag 'existsinSource' to set
	 */
	public void setExistsInSource (boolean existsInSource) {
		this.existsInSource = existsInSource;
	}

	public boolean isCaseSensitive () {
		return caseSensitive;
	}

	public void setCaseSensitive (boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Indicates if this rule is enabled or not.
	 * @return true if this rule is enabled, false if it is not.
	 */
	public boolean isEnabled () {
		return enabled;
	}

	/**
	 * Sets the flag indicating if this rule is enabled or not.
	 * @param enabled true to enable this rule, false to disable it.
	 */
	public void setEnabled (boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Use {@link #isEnabled()} instead of this method.
	 * @return true if this rule is enabled, false if it is not.
	 */
	@Deprecated
	public boolean isEnable () {
		return isEnabled();
	}

	/**
	 * Use {@link #setEnabled(boolean)} instead of this method.
	 * @param enable true to enable this rule, false to disable it.
	 */
	@Deprecated
	public void setEnable (boolean enable) {
		setEnabled(enable);
	}

	public Normalization getNormalization () {
		return normalization;
	}

	public void setNormalization (Normalization normalization) {
		this.normalization = normalization;
	}

	@Override
	public void setExtAttributes (ExtAttributes attributes) {
		this.xattrs = attributes;
	}

	/**
	 * Indicates if this rule is inherited from a parent object.
	 * @return true if this rule is inherited from a parent object, false otherwise.
	 */
	public boolean isInherited () {
		return inherited;
	}

	/**
	 * Sets the flag indicating if this rule is inherited from a parent object.
	 * @param inherited true if this rule is inherited from a parent object, false otherwise.
	 */
	public void setInherited (boolean inherited) {
		this.inherited = inherited;
	}

	@Override
	public ExtAttributes getExtAttributes () {
		if ( xattrs == null ) {
			xattrs = new ExtAttributes();
		}
		return xattrs;
	}

	@Override
	public boolean hasExtAttribute () {
		if ( xattrs == null ) return false;
		return !xattrs.isEmpty();
	}

	@Override
	public String getExtAttributeValue (String namespaceURI,
		String localName)
	{
		if ( xattrs == null ) return null;
		return xattrs.getAttributeValue(namespaceURI, localName);
	}
	
	/**
	 * Creates the effective data from the data and flags for this rule.
	 * For example make the data lower-case or normalize it as needed.
	 */
	public void prepare () {
		verify();
		if ( type == Type.CUSTOM ) return;
		effectiveData = data;
		if ( !isCaseSensitive() ) {
			effectiveData = effectiveData.toLowerCase();
		}
		switch ( normalization ) {
		case NFC:
			effectiveData = Normalizer.normalize(effectiveData, Form.NFC);
			break;
		case NFD:
			effectiveData = Normalizer.normalize(effectiveData, Form.NFD);
			break;
		case NONE:
			// Do nothing
			break;
		}
	}

	/**
	 * Verifies if this rule has valid parameters.
	 * @throws XLIFFException if there is a problem with the rule's parameters.
	 */
	public void verify () {
		if ( type == null ) {
			throw new XLIFFException("You must specify a type of rule (e.g. isPresent, startsWith, etc.)");
		}
		switch ( type ) {
		case CUSTOM:
			// TBD
//			if ( getExtAttributes().size() != 1 ) {
//				// Rule must have one and only one extended attribute
//				throw new RuntimeException("A custom rule must have a single extension attribute.");
//			}
			break;
		case ENDSWITH:
			break;
		case ISNOTPRESENT:
			break;
		case ISPRESENT:
			break;
		case STARTSWITH:
			break;
		}

		// Note: existsInSource must be used only with
		// isPresent, startsWith and endsWith

		// Note: occurs is used only with isPresent, but there is no restriction in the specification
		// to have it forbidden with other type of rules
	}

}
