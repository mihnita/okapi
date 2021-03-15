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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a stand-off group for a given data category that can have multiple
 * instances.
 */
public abstract class DataCategoryGroup<T extends DataCategory> implements IITSItem {

	private List<T> list;
	private String id;

//	@SuppressWarnings("unchecked")
//	public DataCategoryGroup (DataCategoryGroup<T> original) {
//		id = original.id;
//		list = new ArrayList<>();
//		for ( T item : original.list ) {
//			list.add((T)item.createClone());
//		}
//	}
	
	/**
	 * Creates a new {@link DataCategoryGroup} object with a given identifier.
	 * @param id the identifier to use (use null to create one automatically)
	 */
	public DataCategoryGroup (String id) {
		list = new ArrayList<>();
		if ( id == null ) this.id = UUID.randomUUID().toString();
		else this.id = id;
	}
	
	@Override
	public boolean isGroup () {
		return true;
	}

	@Override
	public boolean hasUnresolvedGroup () {
		return false; // Unresolved group do not occur in groups
	}

	/**
	 * Gets the identifier for this group.
	 * @return the identifier for this group.
	 */
	public String getGroupId () {
		return id;
	}

	/**
	 * Sets the identifier for this group. 
	 * @param id the identifier to set.
	 */
	public void setGroupId (String id) {
		this.id = id;
	}

	/**
	 * Gets the list of instances for this group.
	 * @return the list of instances for this group.
	 */
	public List<T> getList () {
		return list;
	}

	@Override
	public void setAnnotatorRef (String annotatorRef) {
		for ( T item : list ) {
			item.setAnnotatorRef(annotatorRef);
		}
	}

	@Override
	public void setAnnotatorRef (AnnotatorsRef ar) {
		for ( T item : list ) {
			item.setAnnotatorRef(ar);
		}
	}

	@Override
	public String getAnnotatorRef () {
		throw new UnsupportedOperationException("Data category groups do not support the getAnnotatorRef() method.");
	}
	
	@Override
	public void validate () {
		for ( T item : list ) {
			item.validate();
		}
	}

}
