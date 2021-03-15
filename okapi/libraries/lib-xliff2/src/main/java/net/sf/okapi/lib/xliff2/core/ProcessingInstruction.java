/*===========================================================================
  Copyright (C) 2013-2014 by the Okapi Framework contributors
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
 * Represents a processing instruction as an extension content object.
 */
public class ProcessingInstruction implements IExtChild {

	private String text;

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ProcessingInstruction (ProcessingInstruction original) {
		this(original.text);
	}
	
	/**
	 * Creates a new {@link ProcessingInstruction} object.
	 * @param text the text of the content (can be null).
	 */
	public ProcessingInstruction (String text) {
		this.text = text;
	}
	
	@Override
	public ExtChildType getType () {
		return ExtChildType.PI;
	}

	/**
	 * Gets the processing instruction text.
	 * @return the processing instruction text (can be null).
	 */
	public String getPI () {
		return text;
	}

	/**
	 * Sets the processing instruction text.
	 * @param text the new processing instruction text (can be null).
	 */
	public void setPI (String text) {
		this.text = text;
	}
	
}
