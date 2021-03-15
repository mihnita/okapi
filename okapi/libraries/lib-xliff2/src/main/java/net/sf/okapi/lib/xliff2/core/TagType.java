/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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
 * Possible types for tags:
 * {@link #OPENING}, {@link #CLOSING} and {@link #STANDALONE}.
 */
public enum TagType {

	/**
	 * Opening tag of a spanning code or opening tag for a marker.
	 * <p>For example, the tag representing the start tag of an HTML bold element.
	 */
	OPENING,
	
	/**
	 * Closing tag of a spanning code or closing tag for a marker.
	 * <p>For example, the tag representing the end tag of an HTML bold element.
	 */
	CLOSING,
	
	/**
	 * Standalone marker for a standalone code.
	 * <p>For example, the tag for the code representing an HTML line break.
	 * <p>This type is not used for markers ({@link MTag}).
	 */
	STANDALONE

}
