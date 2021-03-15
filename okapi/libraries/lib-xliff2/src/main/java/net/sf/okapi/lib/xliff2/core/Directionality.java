/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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
 * Represents the types of direction for the bi-directionality information.
 */
public enum Directionality {

	/**
	 * Automatic.
	 */
	AUTO,
	
	/**
	 * Left-to-Right.
	 */
	LTR,
	
	/**
	 * Right-to-Left.
	 */
	RTL,
	
	/**
	 * Inherited from the proper parent.
	 */
	INHERITED;
	
	public String getPrefix () {
		switch ( this ) {
		case AUTO:
			return "a";
		case LTR:
			return "l";
		case RTL:
			return "r";
		default:
			return "i";
		}
	}

	@Override
	public String toString () {
		switch ( this ) {
		case AUTO:
			return "auto";
		case LTR:
			return "ltr";
		case RTL:
			return "rtl";
		case INHERITED:
		default:
			return "NOT ALLOWED";
		}
	}
}
