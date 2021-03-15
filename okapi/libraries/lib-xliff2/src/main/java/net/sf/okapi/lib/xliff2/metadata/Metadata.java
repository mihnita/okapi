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

package net.sf.okapi.lib.xliff2.metadata;

import net.sf.okapi.lib.xliff2.core.BaseList;

/**
 * Represents a list of {@link IMetadataItem} objects.
 */
public class Metadata extends BaseList<MetaGroup> implements IWithMetaGroup {

	private String id;
	
	/**
	 * Creates an empty {@link Metadata} object.
	 */
	public Metadata () {
		// Nothing to do
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Metadata (Metadata original) {
		super(original);
		this.id = original.id;
	}

	public String getId () {
		return id;
	}

	public void setId (String id) {
		this.id = id;
	}

	@Override
	public void addGroup (MetaGroup group) {
		add(group);
	}
	
}
