/*===========================================================================
  Copyright (C) 2014-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.its;

import java.util.List;

/**
 * Provides a common interface to all objects that can have ITS stand-off elements (groups)
 */
public interface IWithITSGroups {

	/**
	 * Indicates if the object has at least one ITS group.
	 * @return true if the object has at least one ITS group.
	 */
	boolean hasITSGroup();
	
	/**
	 * Gets the ITS groups associated with this object.
	 * @return the ITS groups associated with this object (can be empty, but never null).
	 */
	List<DataCategoryGroup<?>> getITSGroups();
	
	/**
	 * Adds an ITS group to this object.
	 * @param group the group to add.
	 * @return the group added.
	 */
	DataCategoryGroup<?> addITSGroup(DataCategoryGroup<?> group);

}
