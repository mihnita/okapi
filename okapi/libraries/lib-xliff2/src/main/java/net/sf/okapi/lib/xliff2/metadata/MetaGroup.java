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

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.core.BaseList;

/**
 * Represents a group of {@link IMetadataItem} objects.
 */
public class MetaGroup extends BaseList<IMetadataItem> implements IWithMetaGroup, IMetadataItem {

	/**
	 * Types of object a meta-group can apply to.
	 */
	public enum AppliesTo {
		/**
		 * The meta-group does not applies to a specific type of object.
		 */
		UNDEFINED(null),
		/**
		 * The meta-group applies to the source.
		 */
		SOURCE("source"),
		/**
		 * The meta-group applies to the target.
		 */
		TARGET("target"),
		/**
		 * The meta-group applies to the ignorable.
		 */
		IGNORABLE("ignorable");

		private String name;

		AppliesTo(String name) {
			this.name = name;
		}

		@Override
		public String toString () {
			return name;
		}
		
		public static AppliesTo fromString (String name) {
			if ( name == null ) {
				return UNDEFINED;
			}
			switch ( name ) {
			case "source":
				return SOURCE;
			case "target":
				return TARGET;
			case "ignorable":
				return IGNORABLE;
			default:
				throw new InvalidParameterException(String.format("Invalid appliesTo value: '%s'.", name));
			}
		}
	}

	private String id;
	private String category;
	private AppliesTo appliesTo = AppliesTo.UNDEFINED;
	
	/**
	 * Creates an empty {@link MetaGroup} object.
	 */
	public MetaGroup () {
		// Nothing to do
	}
	
	/**
	 * Creates a {@link MetaGroup} object with a given category parameter.
	 * @param category the category for this meta-group.
	 */
	public MetaGroup (String category) {
		setCategory(category);
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public MetaGroup (MetaGroup original) {
		super(original);
		id = original.id;
		category = original.category;
		appliesTo = original.appliesTo;
	}

	@Override
	public boolean isGroup () {
		return true;
	}

	public String getId () {
		return id;
	}

	public void setId (String id) {
		this.id = id;
	}

	public String getCategory () {
		return category;
	}

	public void setCategory (String category) {
		this.category = category;
	}

	public AppliesTo getAppliesTo () {
		return appliesTo;
	}

	public void setAppliesTo (AppliesTo appliesTo) {
		this.appliesTo = appliesTo;
	}

	@Override
	public void addGroup (MetaGroup group) {
		add(group);
	}
	
}
