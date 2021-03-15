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
 * Represents the information associated with a file element.
 */
public class StartFileData extends InheritedDataWithExtAttributes {
	
	private String original;

	/**
	 * Creates a {@link StartFileData} object with an optional id.
	 * @param id the id of the file element (should not be null, but we don't throw an 
	 * exception to allow setting it after creation).
	 */
	public StartFileData (String id) {
		setId(id);
	}
	
	/**
	 * Gets the original attribute of this file.
	 * @return the original attribute of this file.
	 */
	public String getOriginal () {
		return original;
	}
	
	/**
	 * Sets the original attribute of this file.
	 * @param original the new original attribute for this file.
	 */
	public void setOriginal (String original) {
		this.original = original;
	}

}
