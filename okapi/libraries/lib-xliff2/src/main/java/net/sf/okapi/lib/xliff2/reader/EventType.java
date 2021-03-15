/*===========================================================================
  Copyright (C) 2012-2013 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.reader;

/**
 * List of the different types of {@link Event} objects the {@link XLIFFReader} can generate.
 */
public enum EventType {
	/**
	 * Start of the input document.
	 * This event has no associated resource.
	 * There are no events before this one.
	 */
	START_DOCUMENT,
	
	/**
	 * End of the input document.
	 * This event has no associated resource.
	 * There are no events after this one.
	 */
	END_DOCUMENT,
	
	/**
	 * Start of the XLIFF element of the document.
	 * This event comes with an {@link net.sf.okapi.lib.xliff2.core.StartXliffData StartXliffData} resource.
	 */
	START_XLIFF,
		
	/**
	 * End of the XLIFF element in the document.
	 * This event has no associated resource.
	 */
	END_XLIFF,
		
	/**
	 * Start of a file in an XLIFF document.
	 * This event comes with a {@link net.sf.okapi.lib.xliff2.core.StartFileData StartFileData} resource.
	 */
	START_FILE,
	
	/**
	 * Part of the file-level data after the skeleton and before the first unit or group.
	 * This event comes with a {@link net.sf.okapi.lib.xliff2.core.MidFileData MidFileData} resource.
	 */
	MID_FILE,
	
	/**
	 * End of a file in an XLIFF document.
	 * This event has no associated resource.
	 */
	END_FILE,
		
	/**
	 * Skeleton element.
	 * This event comes with a {@link net.sf.okapi.lib.xliff2.core.StartXliffData StartXliffData} resource.
	 */
	SKELETON,
		
	/**
	 * Start of a group.
	 * This event comes with a {@link net.sf.okapi.lib.xliff2.core.StartGroupData StartGroupData} resource.
	 */
	START_GROUP,
		
	/**
	 * End of a group.
	 * This event has no associated resource.
	 */
	END_GROUP,
		
	/**
	 * A full unit element (start to end)
	 * This event comes with a {@link net.sf.okapi.lib.xliff2.core.Unit Unit} resource.
	 */
	TEXT_UNIT,
		
	/**
	 * Non-significant parts of the document (white-spaces between elements
	 * outside the content).
	 * this event comes with a {@link net.sf.okapi.lib.xliff2.core.InsingnificantPartData InsingnificantPartData} resource.
	 */
	INSIGNIFICANT_PART
}
