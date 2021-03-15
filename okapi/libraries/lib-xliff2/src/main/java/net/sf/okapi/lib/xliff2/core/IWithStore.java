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

package net.sf.okapi.lib.xliff2.core;

/**
 * Represents an object that is the parent of a {@link Store} object, for example a {@link Unit} 
 * holds the store for the inline codes and other related data in that unit. 
 */
public interface IWithStore {

	/**
	 * Indicates if a given id value is already in use in the object (for a {@link Part} or for a {@link Tag}). 
	 * @param id the id value to lookup.
	 * @return true if the value is already used, false otherwise.
	 */
	boolean isIdUsed(String id);

	Directionality getSourceDir();

	void setSourceDir(Directionality dir);

	Directionality getTargetDir();

	void setTargetDir(Directionality dir);

	/**
	 * Gets the {@link Store} object of this object.
	 * @return the store of this object (never null).
	 */
	Store getStore();

}
