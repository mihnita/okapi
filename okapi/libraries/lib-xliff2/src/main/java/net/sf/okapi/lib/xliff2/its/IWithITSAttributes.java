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

package net.sf.okapi.lib.xliff2.its;

/**
 * Provides a common interface to all objects that can have ITS attributes.
 */
public interface IWithITSAttributes {

	/**
	 * Indicates if the object has at least one ITS item.
	 * @return true if the object has at least one ITS item.
	 */
	boolean hasITSItem();
	
	/**
	 * Gets the ITS items associated with this object.
	 * @return the ITS items associated with this object (can be empty, but never null).
	 */
	ITSItems getITSItems();
	
	/**
	 * Sets the list of the ITS items associated with this object.
	 * @param items the new items to set.
	 */
	void setITSItems(ITSItems items);

	/**
	 * Gets the id of the object that holds the ITS items.
	 * @return the id of the object.
	 */
	String getId();
	
}
