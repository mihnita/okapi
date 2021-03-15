/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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

import java.util.Objects;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.Util;

/**
 * Represents a note object.
 */
public class Note extends DataWithExtAttributes {

	/**
	 * Types of object a note can apply to.
	 */
	public enum AppliesTo {
		/**
		 * The note does not applies to a specific type of object.
		 */
		UNDEFINED,
		/**
		 * The note applies to the source.
		 */
		SOURCE,
		/**
		 * the note applies to the target.
		 */
		TARGET
	}

	private String id;
	private String content;
	private AppliesTo appliesTo = AppliesTo.UNDEFINED;
	private int priority = 1;
	private String category;

	/**
	 * Creates a new empty {@link Note} object.
	 */
	public Note () {
		// Nothing to do
	}

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Note (Note original) {
		// Create the new object from the base class copy constructor
		super(original);
		// Copy the Note-specific fields
		id = original.id;
		content = original.content;
		appliesTo = original.appliesTo;
		priority = original.priority;
		category = original.category;
	}
	
	/**
	 * Creates a new {@link Note} object with a content with a scope set to {@link AppliesTo#UNDEFINED}.
	 * @param content the content of the note.
	 */
	public Note (String content) {
		this(content, AppliesTo.UNDEFINED);
	}
	
	/**
	 * Creates a new {@link Note} object with a content and a scope.
	 * @param content the content of this note.
	 * @param appliesTo the scope of this note.
	 */
	public Note (String content,
		AppliesTo appliesTo)
	{
		this.content = content;
		this.appliesTo = appliesTo;
	}

	/**
	 * Gets a representation for this note: its content.
	 * @return the text content of this note.
	 */
	@Override
	public String toString () {
		return content;
	}
	
	/**
	 * Gets the text content of this note.
	 * @return the text content of this note.
	 */
	public String getText () {
		return content;
	}
	
	/**
	 * Sets the content of this note.
	 * @param content the new content to set.
	 */
	public void setText (String content) {
		this.content = content;
	}
	
	/**
	 * Indicates if this note is empty, that is if the content is null or empty.
	 * @return true if the content is null or empty.
	 */
	public boolean isEmpty () {
		return Util.isNoE(content);
	}

	/**
	 * Gets the type of object this note applies to.
	 * @return the type of object this note applies to.
	 */
	public AppliesTo getAppliesTo () {
		return appliesTo;
	}

	/**
	 * Sets the type of object this note applies to.
	 * @param appliesTo the type of object this note applies to.
	 */
	public void setAppliesTo (AppliesTo appliesTo) {
		this.appliesTo = appliesTo;
	}

	/**
	 * Gets the id for this note.
	 * @return the id of this note (can be null).
	 */
	public String getId () {
		return id;
	}

	/**
	 * Sets the id for this note.
	 * @param id the new id to set.
	 */
	public void setId (String id) {
		this.id = id;
	}

	/**
	 * Gets the category for this note.
	 * @return the category for this note (can be null).
	 */
	public String getCategory () {
		return category;
	}

	/**
	 * Sets the category for this note.
	 * @param category the new category to set (can be null).
	 */
	public void setCategory (String category) {
		this.category = category;
	}
	
	/**
	 * Gets the priority for this note.
	 * @return the priority for this note. 1 indicates the highest priority, 10 the lowest.
	 */
	public int getPriority () {
		return priority;
	}
	
	/**
	 * Sets the priority for this note.
	 * @param priority the new priority to set.
	 * The value must be between 1 (inclusive) and 10 (inclusive).
	 * 1 indicates the highest priority, 10 the lowest.
	 */
	public void setPriority (int priority) {
		if (( priority < 1 ) || ( priority > 10 )) {
			throw new InvalidParameterException("Invalid priority value. It must be between 1 and 10.");
		}
		this.priority = priority;
	}

	@Override
	public int hashCode () {
		return Objects.hash(super.hashCode(), appliesTo, category, content, id, priority);
	}

	@Override
	public boolean equals (Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Note other = (Note)obj;
		if (appliesTo != other.appliesTo) {
			return false;
		}
		if (category == null) {
			if (other.category != null) {
				return false;
			}
		} else if (!category.equals(other.category)) {
			return false;
		}
		if (content == null) {
			if (other.content != null) {
				return false;
			}
		} else if (!content.equals(other.content)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (priority != other.priority) {
			return false;
		}
		return true;
	}

}
