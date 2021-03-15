/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.common.resource;

import net.sf.okapi.common.IResource;

/**
 * Provides the methods common to all resources that can be named.
 */
public interface INameable extends IResource {
	/**
	 * Gets the name of this resource. The resource name corresponds to different things depending
	 * on the type of resource. For a StartDocument the name is the URI of the document. Otherwise,
	 * in most cases the name is the identifier of the resource (This is the equivalent of the XLIFF 
	 * resname attribute).
	 * @return This resource name, or null if there is none.
	 */
	String getName();

	/**
	 * Sets the name of this resource. The resource name is the equivalent of the XLIFF resname attribute.
	 * @param name New name to set.
	 */
	void setName(String name);

	/**
	 * Gets the type information associated with this resource. For example "button".
	 * @return The type information associated with this resource.
	 */
	String getType();

	/**
	 * Sets the type information associated with this resource. For example "button".
	 * @param value The new type information.
	 */
	void setType(String value);

	/**
	 * Gets the type of content of this resource. For example "text/xml".
	 * @return The type of content of this resource.
	 */
	String getMimeType();

	/**
	 * Sets the type of content of this resource. For example "text/xml".
	 * @param value The new type of content of this resource.
	 */
	void setMimeType(String value);

	/**
	 * Indicates if the content of this resource is translatable.
	 * By default this indicator is set to true for all resources. 
	 * @return True if the content of this resource is translatable. False if
	 * it is not translatable.
	 */
	boolean isTranslatable();

	/**
	 * Sets the flag indicating if the content of this resource is translatable.
	 * @param value True to indicate that the content of this resource is translatable.
	 */
	void setIsTranslatable(boolean value);

	/**
	 * Indicates if the white-spaces in the content of this resource should be preserved.
	 * By default this indicator is set to false for all resources. 
	 * @return True if the white-spaces in the content of this resource should be preserved.
	 */
	boolean preserveWhitespaces();

	/**
	 * sets the flag indicating if the white-spaces in the content of this resource should be preserved.
	 * @param value True to indicate that the white-spaces in the content of this resource should be preserved.
	 */
	void setPreserveWhitespaces(boolean value);
}
