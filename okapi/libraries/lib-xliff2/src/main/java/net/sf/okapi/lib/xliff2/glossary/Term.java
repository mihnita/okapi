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
 * Represents the &lt;term&gt; element
 * <a href='http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#glossary-module'>Glossary module</a>.
 */
public class Term extends BaseGlossaryField {

	/**
	 * Creates a {@link Term} object with a given text.
	 * @param text the text of the term.
	 */
	public Term (String text) {
		setText(text);
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Term (Term original) {
		// Create the new object from its base class copy constructor
		super(original);
	}
	
}
