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

/**
 * Represents the standalone tag of a protected content.
 * <p>A protected content is a span of content (that may include code or marker tags) that is temporarily
 * folded into a tag reference to prevent the content to be modified.
 * <p>See {@link Unit#hideProtectedContent()} and {@link Unit#showProtectedContent()}.
 */
public class PCont {
	
	protected String codedText;

	/**
	 * Creates an empty {@link PCont} object.
	 */
	public PCont () {
		// Argument-less constructor
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public PCont (PCont original) {
		this(original.codedText);
	}
	
	/**
	 * Creates a new protected content marker with a given coded text.
	 * @param codedText the span of coded text to protect.
	 */
	public PCont (String codedText) {
		setCodedText(codedText);
	}

//	/**
//	 * Indicates if this marker is equal to another.
//	 * <p>Use the <code>=</code> operator to test if two markers are the same.
//	 * @param marker the other marker to compare to this one.
//	 * @return true if both markers are equals.
//	 */
//	//boolean equals (PMarker marker);

	/**
	 * Gets the coded text for this protected content.
	 * The corresponding markers are in the store.
	 * @return the coded text for this protected content.
	 */
	public String getCodedText () {
		return codedText;
	}

	/**
	 * Sets the coded text for this protected content.
	 * @param codedText the new coded text.
	 */
	public void setCodedText (String codedText) {
		this.codedText = codedText;
	}

}
