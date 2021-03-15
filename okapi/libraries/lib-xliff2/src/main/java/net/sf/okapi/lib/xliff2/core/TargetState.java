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

import net.sf.okapi.lib.xliff2.InvalidParameterException;

/**
 * Possible values for the <code>state</code> attribute of a target content:
 * {@link #INITIAL}, {@link #TRANSLATED}, {@link #REVIEWED} and {@link #FINAL}.
 */
public enum TargetState {

	/**
	 * Target state: initial.
	 */
	INITIAL("initial"),
	
	/**
	 * Target state: translated.
	 */
	TRANSLATED("translated"),
	
	/**
	 * Target state: reviewed.
	 */
	REVIEWED("reviewed"),
	
	/**
	 * Target state: final.
	 */
	FINAL("final");
	
	private String name;

	/**
	 * Creates a new targetState object with a given name.
	 * @param name the name of the item to create.
	 */
	TargetState(String name) {
		this.name = name;
	}

	@Override
	public String toString () {
		return name;
	}
	
	/**
	 * Converts a given string representing the name of a target state into a {@link TargetState} object.
	 * @param name the name of the target state.
	 * @return the object for the given name.
	 * @throws InvalidParameterException if the name is invalid.
	 */
	public static TargetState fromString (String name) {
		if ( name == null ) {
			throw new InvalidParameterException("State cannot be null.");
		}
		switch ( name ) {
		case "initial":
			return INITIAL;
		case "translated":
			return TRANSLATED;
		case "reviewed":
			return REVIEWED;
		case "final":
			return FINAL;
		default:
			throw new InvalidParameterException(String.format("Invalid state value: '%s'.", name));
		}
	}

}
