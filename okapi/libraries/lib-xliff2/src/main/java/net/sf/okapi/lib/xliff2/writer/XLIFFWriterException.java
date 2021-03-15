/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.writer;

/**
 * Signals that an XLIFF writer had an error.  
 */
public class XLIFFWriterException extends RuntimeException {

	/**
	 * Serialization version id.
	 */
	private static final long serialVersionUID = -0100;

	/**
	 * Creates an empty new XLIFFWriterException object.
	 */
	public XLIFFWriterException () {
		super();
	}

	/**
	 * Creates a new XLIFFWriterException object with a given message.
	 * @param message text of the message.
	 */
	public XLIFFWriterException (String message) {
		super(message);		
	}

	/**
	 * Creates a new XLIFFWriterException object with a given parent 
	 * exception cause.
	 * @param cause the parent exception cause.
	 */
	public XLIFFWriterException (Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new XLIFFWriterException object with a given message and 
	 * a given parent exception cause.
	 * @param message the message.
	 * @param cause the cause.
	 */
	public XLIFFWriterException (String message, Throwable cause) {
		super(message, cause);
	}

}
