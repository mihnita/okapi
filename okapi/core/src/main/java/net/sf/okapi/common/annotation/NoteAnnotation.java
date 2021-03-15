/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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

package net.sf.okapi.common.annotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Annotation used to expose xliff 1.2 like notes
 */
public class NoteAnnotation implements IAnnotation, Iterable<Note> {
	static public final String LOC_NOTE = "developer";
	static public final String TRANS_NOTE = "translator";

	private final List<Note> notes = new ArrayList<>();

	/**
	 * Add a Note to the annotation.
	 * @param note - Note from the xliff document.
	 */
	public void add(Note note) {
		this.notes.add(note);
	}
	
	public Note getNote(int index) {
		return notes.get(index);
	}

	@Override
	public Iterator<Note> iterator() {
		return notes.iterator();
	}
}
