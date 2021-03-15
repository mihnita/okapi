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

package net.sf.okapi.lib.xliff2;

/**
 * Base exception for all XLIFF-library exceptions.
 */
public class XLIFFException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception with a given text.
	 * @param text Text to go with the new exception.
	 */
	public XLIFFException (String text) {
		super(text);
	}
	
	/**
	 * Creates a new exception with a given parent exception.
	 * @param e The parent exception.
	 */
	public XLIFFException (Throwable e) {
		super(e);
	}

	/**
	 * Creates a new exception with a given message and 
	 * a given parent exception cause.
	 * @param message the message.
	 * @param cause the cause.
	 */
	public XLIFFException (String message, Throwable cause) {
		super(message, cause);
	}

}
