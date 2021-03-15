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

package net.sf.okapi.lib.xliff2.glossary;

import java.util.Objects;

import net.sf.okapi.lib.xliff2.core.DataWithExtAttributes;

/**
 * Represents the base class for the fields of the {@link GlossEntry} object.
 */
abstract class BaseGlossaryField extends DataWithExtAttributes {

	private String text;
	private String source;
	
	/**
	 * Creates an empty {@link BaseGlossaryField} object.
	 */
	public BaseGlossaryField () {
		// Nothing to do
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public BaseGlossaryField (BaseGlossaryField original) {
		// Create the new object from its base class copy constructor
		super(original);
		this.text = original.text;
		this.source = original.source;
	}

	/**
	 * Get the text of this field.
	 * @return the text of this field (can be null).
	 */
	public String getText () {
		return text;
	}
	
	/**
	 * Sets the text of this field.
	 * @param text the new text of this field (can be null).
	 */
	public void setText (String text) {
		this.text = text;
	}
	
	/**
	 * Gets the source of this field.
	 * @return the source of this field (can be null).
	 */
	public String getSource () {
		return source;
	}
	
	/**
	 * sets the source of this field.
	 * @param source the new source of this field (can be null).
	 */
	public void setSource (String source) {
		this.source = source;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), source, text);
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
		BaseGlossaryField other = (BaseGlossaryField) obj;
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
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
