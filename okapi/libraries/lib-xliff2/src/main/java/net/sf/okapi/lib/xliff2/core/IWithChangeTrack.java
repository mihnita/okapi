/*===========================================================================
  Copyright (C) 2015 by the Okapi Framework contributors
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

import net.sf.okapi.lib.xliff2.changeTracking.ChangeTrack;

/**
 * Represents an object that can contain a {@link ChangeTrack} object. Each
 * class representing a XLIFF element where the change tracking module is
 * allowed, should implement this interface.
 * 
 * @author Marta Borriello
 * 
 */
public interface IWithChangeTrack {

	/**
	 * Gets the {@link ChangeTrack} object for this element, creates an empty of if there is none.
	 * @return the {@link ChangeTrack} object for this element (can be empty, never null).
	 */
	ChangeTrack getChangeTrack();

	/**
	 * Sets the {@link ChangeTrack} object for this element.
	 * @param changeTrack the {@link ChangeTrack} object for this element.
	 */
	void setChangeTrack(ChangeTrack changeTrack);

	/**
	 * Check if there is at least a revision for this element.
	 * @return <code>true</code> if there is at least a revision for this element; <code>false</code> otherwise.
	 */
	boolean hasChangeTrack();
}
