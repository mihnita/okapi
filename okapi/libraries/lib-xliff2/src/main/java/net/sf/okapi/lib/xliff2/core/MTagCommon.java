/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
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

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.its.ITSItems;
import net.sf.okapi.lib.xliff2.its.IWithITSAttributes;

/**
 * Represents common data for the opening and closing {@link MTag}.
 */
public class MTagCommon implements IWithITSAttributes {

	private String id;
	private String type;
	private String value;
	private String ref;
	private Boolean translate;
	private ITSItems itsItems;

	public MTagCommon (String id,
		String type)
	{
		setId(id);
		setType(type);
	}
	
	/**
	 * Copy constructor.
	 * @param original the existing {@link MTagCommon} to duplicate.
	 */
	public MTagCommon (MTagCommon original) {
		// Copy the AMarker-specific fields
		id = original.id;
		type = original.type;
		value = original.value;
		ref = original.ref;
		translate = original.translate;
		if ( original.hasITSItem() ) {
			setITSItems(new ITSItems(original.itsItems));
		}
	}
	
	@Override
	public String getId () {
		return id;
	}

	public void setId (String id) {
		if ( id == null ) {
			throw new InvalidParameterException("ID cannot be null.");
		}
		this.id = id;
	}

	public void setType (String type) {
		if ( type == null ) {
			type = MTag.TYPE_DEFAULT; // Use the default
		}
		else {
			// Is it one of the standard values?
			if (!";generic;comment;term;".contains(";" + type + ";")) {
				// If not: check the pattern "prefix:value"
				int n = type.indexOf(':');
				if (( n == -1 ) || ( n == 0 ) || ( n == type.length()-1 )) {
					throw new InvalidParameterException(String.format("Invalid value '%s' for an annotation type.", type));
				}
			}
		}
		this.type = type;
	}

	public String getRef () {
		return ref;
	}
	
	public void setRef (String ref) {
		this.ref = ref;
	}

	public String getValue () {
		return value;
	}
	
	public void setValue (String value) {
		this.value = value;
	}

	public Boolean getTranslate () {
		return translate;
	}

	public void setTranslate (Boolean translate) {
		this.translate = translate;
	}

	/**
	 * Indicates if this object is equals to a given one.
	 * @param other the other object to compare.
	 * @return true if the two objects are identical.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (!(other instanceof MTagCommon)) return false;

		MTagCommon that = (MTagCommon) other;
		return Objects.equals(id, that.id) &&
				Objects.equals(type, that.type) &&
				Objects.equals(value, that.value) &&
				Objects.equals(ref, that.ref) &&
				Objects.equals(translate, that.translate) &&
				Objects.equals(itsItems, that.itsItems);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, value, ref, translate, itsItems);
	}

	@Override
	public boolean hasITSItem () {
		if ( itsItems == null ) return false;
		return !itsItems.isEmpty();
	}

	@Override
	public ITSItems getITSItems () {
		if ( itsItems == null ) {
			itsItems = new ITSItems();
		}
		return itsItems;
	}

	@Override
	public void setITSItems (ITSItems itsItems) {
		this.itsItems = itsItems;
	}

	public String getType () {
		return type;
	}

}
