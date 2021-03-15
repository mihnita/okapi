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

/**
 * Represents an ITS data category or a group of several instances of an ITS data category.
 */
public interface IITSItem {

	/**
	 * Gets the id/name of this data category. 
	 * @return the id/name of this data category.
	 */
	String getDataCategoryName();
	
	/**
	 * Indicates if this item is a group.
	 * @return true if it is a group, false if it is a standalone data category instance.
	 */
	boolean isGroup();
	
	/**
	 * Indicates if this item has currently a reference to a stand-off element that
	 * has not been resolved yet.
	 * <p>This occurs for example when a unit element has a reference to a set of Provenance instances
	 * and the stand-off element has not been read yet (because the reading of the unit's element
	 * is done after the reading of its attributes.
	 * @return true if this item has currently an unresolved reference to a stand-off element.
	 */
	boolean hasUnresolvedGroup();
	
	/**
	 * Sets the annotator reference information for this data category.
	 * @param annotatorRef the reference string to set (can be null).
	 */
	void setAnnotatorRef(String annotatorRef);

	/**
	 * Sets the annotator reference information for this data category.
	 * @param ar the set of references read from <code>its:annotatorsRef</code>.
	 * If it is null, or if there is no reference for the relevant data category: no change is made. 
	 */
	void setAnnotatorRef(AnnotatorsRef ar);
	
	/**
	 * Gets the annotator reference currently set for this data category.
	 * This method is not be supported for items that are data category groups.
	 * @return the annotator reference currently set for this data category.
	 */
	String getAnnotatorRef();

	/**
	 * Validates the data category.
	 * Checks if all required attributes are set properly.
	 * @throws XLIFFException if there is an error.
	 */
	void validate();

	/**
	 * Creates a deep-copy clone of this item.
	 * @return the duplicated item.
	 */
	IITSItem createCopy();

}
