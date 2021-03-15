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

package net.sf.okapi.lib.xliff2.core;

import net.sf.okapi.lib.xliff2.metadata.Metadata;

/**
 * Provides the methods to add and retrieve metadata for an object. 
 */
public interface IWithMetadata {

	/**
	 * Indicates if the object has metadata.
	 * @return true if the object has metadata, false otherwise.
	 */
	boolean hasMetadata();
	
	/**
	 * Gets the {@link Metadata} object for the parent, creates an empty of if there is none.
	 * @return the {@link Metadata} object for the parent (can be empty, but never null).
	 */
	Metadata getMetadata();

	/**
	 * sets the {@link Metadata} object for the parent.
	 * @param metadata the new {@link Metadata} object for the parent.
	 */
	void setMetadata(Metadata metadata);

}
