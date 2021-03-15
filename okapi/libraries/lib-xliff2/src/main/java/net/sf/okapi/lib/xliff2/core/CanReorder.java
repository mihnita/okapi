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
 * Represents the value of the canReorder field.
 * One of: {@link #YES}, {@link #FIRSTNO} or {@link #NO}.
 */
public enum CanReorder {

	YES("yes"),
	
	FIRSTNO("firstNo"),
	
	NO("no");

	private String name;

	/**
	 * Creates a new {@link CanReorder} object with a given name.
	 * @param name the name of the item to create.
	 */
	CanReorder(String name) {
		this.name = name;
	}

	@Override
	public String toString () {
		return name;
	}
	
}
