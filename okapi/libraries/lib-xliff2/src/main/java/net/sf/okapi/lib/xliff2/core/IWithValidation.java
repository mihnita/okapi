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

import net.sf.okapi.lib.xliff2.validation.Validation;

/**
 * Provides the methods to add and retrieve validation rules for an object. 
 */
public interface IWithValidation {

	/**
	 * Indicates if the object has validation data.
	 * @return true if the object has validataion data, false otherwise.
	 */
	boolean hasValidation();
	
	/**
	 * Gets the {@link Validation} object for the parent, creates an empty of if there is none.
	 * @return the {@link Validation} object for the parent (can be empty, but never null).
	 */
	Validation getValidation();

	/**
	 * sets the {@link Validation} object for the parent.
	 * @param validation the new {@link Validation} object for the parent.
	 */
	void setValidation(Validation validation);

}
