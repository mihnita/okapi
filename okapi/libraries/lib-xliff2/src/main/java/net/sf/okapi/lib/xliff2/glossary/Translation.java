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

package net.sf.okapi.lib.xliff2.glossary;

/**
 * Represents the &lt;translation&gt; element
 * <a href='http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#glossary-module'>Glossary module</a>.
 */
public class Translation extends BaseGlossaryField {

	private String id;
	private String ref;

	/**
	 * Creates a {@link Translation} object with a given text.
	 * @param text the text of the translation.
	 */
	public Translation (String text) {
		setText(text);
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Translation (Translation original) {
		// Create the new object from its base class copy constructor
		super(original);
		// Duplicate additional fields
		this.id = original.id;
		this.ref = original.ref;
	}
	
	/**
	 * Gets the id for this translation.
	 * @return the id for this translation (can be null).
	 */
	public String getId () {
		return id;
	}

	/**
	 * Sets the id for this translation.
	 * @param id the new id for this translation (can be null).
	 */
	public void setId (String id) {
		this.id = id;
	}

	/**
	 * Gets the reference for this translation.
	 * @return the reference for this translation (can be null).
	 */
	public String getRef () {
		return ref;
	}

	/**
	 * Sets the reference for this translation.
	 * @param ref the new reference for this translation.
	 */
	public void setRef (String ref) {
		this.ref = ref;
	}

}
