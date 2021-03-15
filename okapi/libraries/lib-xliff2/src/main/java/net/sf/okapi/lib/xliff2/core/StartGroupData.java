/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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
 * Represents the information associated with the start of a group.
 */
public class StartGroupData extends CompleteData {

	/**
	 * Creates a new {@link StartGroupData} object with a given id.
	 * @param id the id to use for this group (should not be null, but allow it to set it later).
	 */
	public StartGroupData (String id) {
		setId(id);
	}

}
