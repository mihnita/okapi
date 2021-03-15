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

import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Signals that there is a marker not properly located in a fragment or unit.  
 */
public class InvalidMarkerOrderException extends XLIFFException {

	/**
	 * Serialization version id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new InvalidMarkerOrder object with a given message.
	 * @param message text of the message.
	 */
	public InvalidMarkerOrderException (String message) {
		super(message);		
	}

	/**
	 * Creates a new InvalidMarkerOrder object with a given parent 
	 * exception cause.
	 * @param cause the parent exception cause.
	 */
	public InvalidMarkerOrderException (Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new InvalidMarkerOrder object with a given message and 
	 * a given parent exception cause.
	 * @param message the message.
	 * @param cause the cause.
	 */
	public InvalidMarkerOrderException (String message, Throwable cause) {
		super(message, cause);
	}

}
