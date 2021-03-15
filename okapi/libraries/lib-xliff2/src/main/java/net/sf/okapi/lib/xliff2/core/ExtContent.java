/*===========================================================================
  Copyright (C) 2012-2014 by the Okapi Framework contributors
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
 * Represents an extension content object: a {@link IExtChild} object of 
 * type {@link ExtChildType#TEXT} or {@link ExtChildType#CDATA}. 
 */
public class ExtContent implements IExtChild {

	private boolean cdata;
	private String text;

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ExtContent (ExtContent original) {
		this(original.text, original.cdata);
	}

	/**
	 * Creates a new {@link ExtContent} object.
	 * @param text the text of the content.
	 */
	public ExtContent (String text) {
		setText(text);
	}
	
	/**
	 * Creates a new {@link ExtContent} object in CDATA mode or not.
	 * @param text the text of the content.
	 * @param cdata true if the content should be written in a CDATA section, false otherwise.
	 */
	public ExtContent (String text,
		boolean cdata)
	{
		setText(text);
		setCData(cdata);
	}
	
	@Override
	public ExtChildType getType () {
		if ( cdata ) return ExtChildType.CDATA;
		else return ExtChildType.TEXT;
	}

	/**
	 * Indicates if the content is to be represented as CDATA.
	 * @return true if the content is to be represented as CDATA, false otherwise.
	 */
	public boolean getCData () {
		return cdata;
	}

	/**
	 * Sets the flag indicating if the content is to be represented as CDATA.
	 * @param cdata true to represent the content as CDATA.
	 */
	public void setCData (boolean cdata) {
		this.cdata = cdata;
	}

	/**
	 * Gets the content.
	 * @return the content.
	 */
	public String getText () {
		return text;
	}

	/**
	 * Sets the content.
	 * @param text the new content.
	 */
	public void setText (String text) {
		this.text = text;
	}
	
}
