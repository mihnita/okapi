/*===========================================================================
  Copyright (C) 2019 by the Okapi Framework contributors
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
============================================================================*/

package net.sf.okapi.filters.xliff2.util;

import java.util.Iterator;

import net.sf.okapi.common.annotation.Note;
import net.sf.okapi.common.annotation.NoteAnnotation;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.annotation.Note.Annotates;
import net.sf.okapi.common.annotation.Note.Priority;
import net.sf.okapi.common.resource.IWithAnnotations;
import net.sf.okapi.filters.xliff2.model.XLIFF2NotesAnnotation;
import net.sf.okapi.lib.xliff2.core.IWithNotes;
import net.sf.okapi.lib.xliff2.core.StartFileData;

/**
 * Saves properties from the XLIFF Toolkit into Okapi Core and back. The
 * properties saved involve primarily attributes, and other data that does not
 * fit neatly within the Okapi Core Pipeline structure.
 * <p>
 * Since the operation to and from should be equivalent, we put both operations
 * in here to make it easier to compare them.
 */
public class NotesMapper {

	private NotesMapper() {
		throw new IllegalStateException("Static Utility class");
	}

	/**
	 * Takes annotations from Okapi Core and stores them as XLIFF 2.0 notes
	 *
	 * @param sourceAnnotationsHolder The source of the annotations.
	 * @param targetXliffNotesHolder  The destination where the notes will be
	 *                                stored.
	 */
	public static void setNotes(IWithAnnotations sourceAnnotationsHolder, IWithNotes targetXliffNotesHolder) {
		for (IAnnotation iAnnotation : sourceAnnotationsHolder.getAnnotations()) {
			if (iAnnotation instanceof XLIFF2NotesAnnotation) {
				XLIFF2NotesAnnotation xliffNoteAnnotation = (XLIFF2NotesAnnotation) iAnnotation;
				for (net.sf.okapi.lib.xliff2.core.Note xliffNote : xliffNoteAnnotation) {
					targetXliffNotesHolder.addNote(xliffNote);
				}
			}
		}
	}

	/**
	 * Takes notes from XLIFF 2.0 and stores them as Okapi Core Annotations
	 *
	 * @param sourceXliffNotesHolder  The source of the notes.
	 * @param targetAnnotationsHolder The destination to store the annotations
	 */
	public static void setAnnotations(IWithNotes sourceXliffNotesHolder, IWithAnnotations targetAnnotationsHolder) {
		final XLIFF2NotesAnnotation xliff2Notes = new XLIFF2NotesAnnotation();
		if (sourceXliffNotesHolder.getNoteCount() > 0) {
			// original xliff2 notes as annotation
			for (net.sf.okapi.lib.xliff2.core.Note note : sourceXliffNotesHolder.getNotes()) {
				xliff2Notes.add(note);
			}
			targetAnnotationsHolder.setAnnotation(xliff2Notes);

			// okapi notes annotation for xliff 1.2
			final NoteAnnotation xliffNotes = new NoteAnnotation();
			for (net.sf.okapi.lib.xliff2.core.Note note : sourceXliffNotesHolder.getNotes()) {
				Note n = new Note();
				n.setNoteText(note.getText());
				n.setPriority(Priority.fromInt(note.getPriority()));
				switch (note.getAppliesTo()) {
				case SOURCE:
					n.setAnnotates(Annotates.SOURCE);
					break;
				case TARGET:
					n.setAnnotates(Annotates.TARGET);
					break;
				default:
					n.setAnnotates(Annotates.GENERAL);
					break;
				}

				xliffNotes.add(n);
			}
			targetAnnotationsHolder.setAnnotation(xliffNotes);
		}
	}

	/**
	 * A placeholder to give a warning if annotations are detected in the
	 * {@link StartSubDocument}. If the {@link StartSubDocument} has some
	 * annotations, they will be lost and a warning will be logged.
	 *
	 * The {@link StartFileData} class from the XLIFF Toolkit does not implement
	 * {@link IWithNotes}, instead the notes in a &lt;file> element are stored in
	 * {@link net.sf.okapi.lib.xliff2.core.MidFileData}. In a future update we could
	 * potentially transfer the annotations from the {@link StartSubDocument} to the
	 * {@link net.sf.okapi.lib.xliff2.core.MidFileData}.
	 *
	 * @param sourceAnnotationsHolder The source of the notes.
	 * @param targetStartFileData     The destination where Notes are attempting to
	 *                                be stored
	 */
	public static void setNotes(IWithAnnotations sourceAnnotationsHolder, StartFileData targetStartFileData) {

		// If user has some annotations, at least provide a warning that they wont be
		// saved
		// TODO: Store these annotations into the MidFileData of the XLIFF Toolkit
		final Iterator<IAnnotation> iterator = sourceAnnotationsHolder.getAnnotations().iterator();
		if (iterator.hasNext()) {
			LoggerFactory.getLogger(NotesMapper.class).warn("Annotations could not be stored as Notes in the "
					+ "StartFileData object. Annotations that you want stored there must go in a DocumentPart.");
		}
	}

}
