/*===========================================================================
  Copyright (C) 2013-2014 by the Okapi Framework contributors
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

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.changeTracking.ChangeTrack;
import net.sf.okapi.lib.xliff2.metadata.Metadata;
import net.sf.okapi.lib.xliff2.validation.Validation;

/**
 * Implements the {@link IWithInheritedData}, {@link IWithExtAttributes}, {@link IWithExtElements},
 * {@link IWithNotes}, {@link IWithMetadata}, {@link IWithChangeTrack} and {@link IWithValidation} interfaces.
 * Also provide methods for type and name access.
 */
class CompleteData extends InheritedDataWithExtAttributes
	implements IWithNotes, IWithMetadata, IWithValidation, IWithExtElements, IWithChangeTrack {

	private Notes notes;
	private Metadata metadata;
	private Validation validation;
	private ChangeTrack changeTrack;
	private ExtElements xelems;
	private String name;
	private String type;

	/**
	 * Creates an empty {@link CompleteData} object.
	 */
	protected CompleteData () {
		// Nothing to do
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	protected CompleteData (CompleteData original) {
		super(original);
		if ( original.getNoteCount() > 0 ) {
			notes = new Notes(original.notes);
		}
		if ( original.hasMetadata() ) {
			metadata = new Metadata(original.metadata);
		}
		if ( original.hasValidation() ) {
			validation = new Validation(original.validation, false);
		}
		if ( original.hasExtElements() ) {
			xelems = new ExtElements(original.xelems);
		}
		name = original.name;
		type = original.type;
	}
	
	@Override
	public ExtElements getExtElements () {
		if ( xelems == null ) xelems = new ExtElements();
		return xelems;
	}

	@Override
	public boolean hasExtElements () {
		if ( xelems == null ) return false;
		return (xelems.size() > 0);
	}

	@Override
	public ExtElements setExtElements (ExtElements elements) {
		this.xelems = elements;
		return getExtElements();
	}

	@Override
	public void addNote (Note note) {
		if ( notes == null ) notes = new Notes();
		notes.add(note);
	}

	@Override
	public Notes getNotes () {
		if ( notes == null ) notes = new Notes();
		return notes;
	}
	
	@Override
	public int getNoteCount () {
		if ( notes == null ) return 0;
		return notes.size();
	}

	/**
	 * Gets the name for this group.
	 * @return the name for this group (can be null).
	 */
	public String getName () {
		return name;
	}
	
	/**
	 * Sets the name for this group.
	 * @param name the new name to set (can be null).
	 */
	public void setName (String name) {
		this.name = name;
	}

	/**
	 * Gets the type for this group.
	 * @return the type for this group (can be null).
	 */
	public String getType () {
		return type;
	}
	
	/**
	 * Sets the type for this group.
	 * The value must have a prefix (for both unit and group elements)
	 * @param type the new type to set (can be null).
	 */
	public void setType (String type) {
		if ( type != null ) {
			int n = type.indexOf(':');
			if (( n == -1 ) || ( n == 0 ) || ( n == type.length()-1 )) {
				throw new InvalidParameterException(String.format("Invalid value '%s' for type.", type));
			}
			if ( type.startsWith("xlf:") ) {
				// No values define for 2.0
				throw new InvalidParameterException("The prefix 'xlf' is reserved.");
			}
		}
		this.type = type;
	}

	@Override
	public boolean hasMetadata () {
		if ( metadata == null ) return false;
		return !metadata.isEmpty();
	}

	@Override
	public Metadata getMetadata () {
		if ( metadata == null ) metadata = new Metadata();
		return metadata;
	}

	@Override
	public void setMetadata (Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public boolean hasValidation () {
		if ( validation == null ) return false;
		return !validation.isEmpty();
	}

	@Override
	public Validation getValidation () {
		if ( validation == null ) validation = new Validation();
		return validation;
	}

	@Override
	public void setValidation (Validation validation) {
		this.validation = validation;
	}

	@Override
	public ChangeTrack getChangeTrack () {
		if ( changeTrack == null ) changeTrack = new ChangeTrack();
		return changeTrack;
	}

	@Override
	public void setChangeTrack (ChangeTrack changeTrack) {
		this.changeTrack = changeTrack;
	}

	@Override
	public boolean hasChangeTrack () {
		if ( changeTrack == null ) return false;
		return !changeTrack.isEmpty();
	}
	
}
