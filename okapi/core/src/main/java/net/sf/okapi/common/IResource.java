/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
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

package net.sf.okapi.common;

import net.sf.okapi.common.resource.IWithAnnotations;
import net.sf.okapi.common.resource.IWithProperties;
import net.sf.okapi.common.resource.IWithSkeleton;

/**
 * Common set of features all the types of resources associated with events have.  
 */
public interface IResource extends IWithProperties, IWithAnnotations, IWithSkeleton, Cloneable {
	
	int CREATE_EMPTY = 0;
	int COPY_CONTENT = 0x01;
	int COPY_PROPERTIES = 0x02;
	int COPY_SEGMENTATION = 0x04;
	int COPY_SEGMENTED_CONTENT = (COPY_SEGMENTATION | COPY_CONTENT);
	int COPY_ALL = (COPY_SEGMENTED_CONTENT | COPY_PROPERTIES);

	/**
	 * Gets the identifier of the resource. This identifier is unique per extracted document and by type of resource.
	 * This value is filter-specific. It and may be different from one extraction 
	 * of the same document to the next. It can a sequential number or not, incremental 
	 * or not, and it can be not a number.
	 * It has no correspondence in the source document ("IDs" coming from the source document
	 * are "names" and not available for all resources).
	 * @return the identifier of this resource.
	 */
	String getId ();
	
	/**
	 * Sets the identifier of this resource.
	 * @param id the new identifier value.
	 * @see #getId()
	 */
	void setId (String id);
}
