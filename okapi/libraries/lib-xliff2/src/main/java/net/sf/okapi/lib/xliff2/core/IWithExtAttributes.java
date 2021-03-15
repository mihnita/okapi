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
 * Provides a common interface to all objects that can have extension attributes.
 * @see ExtAttributes
 * @see ExtAttribute
 */
public interface IWithExtAttributes {

	/**
	 * Gets the {@link ExtAttributes} object for the parent object.
	 * If none exists, one is created.
	 * @return the {@link ExtAttributes} object for the parent object, never null.
	 */
	ExtAttributes getExtAttributes();
	
	/**
	 * Sets the {@link ExtAttributes} object associated with this object.
	 * @param attributes the {@link ExtAttributes} object associated with this object.
	 * If null, a new {@link ExtAttributes} object is created.
	 */
	void setExtAttributes(ExtAttributes attributes);

	/**
	 * Indicates if at least one extension attribute is present.
	 * @return true if at least one extension attribute is present; false otherwise.
	 */
	boolean hasExtAttribute();

	/**
	 * Gets the value for a given extension attribute.
	 * @param namespaceURI the URI of the namespace for the attribute.
	 * @param localName the name of the attribute.
	 * @return the value of the extension attribute, or null if it does not exist.
	 */
	String getExtAttributeValue(String namespaceURI,
								String localName);

}
