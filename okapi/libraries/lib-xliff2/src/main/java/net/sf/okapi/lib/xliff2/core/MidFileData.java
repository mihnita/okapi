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

import net.sf.okapi.lib.xliff2.changeTracking.ChangeTrack;
import net.sf.okapi.lib.xliff2.metadata.Metadata;
import net.sf.okapi.lib.xliff2.validation.Validation;

/**
 * Implements the {@link IWithExtElements} and {@link IWithNotes} interfaces.
 */
public class MidFileData extends DataWithExtElements implements IWithNotes, IWithMetadata, IWithValidation, IWithChangeTrack {

	private Metadata metadata;
	private Validation validation;
	private ChangeTrack changeTrack;
	private Notes notes;

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
