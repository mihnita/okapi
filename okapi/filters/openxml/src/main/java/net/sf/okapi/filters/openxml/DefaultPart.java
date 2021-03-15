/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.ZipSkeleton;

/**
 * A default part.
 */
class DefaultPart implements Part {
	private final Document.General generalDocument;
	private final ZipEntry entry;
	private final ContentFilter contentFilter;
	private String documentId;
	private String subDocumentId;

	DefaultPart(Document.General generalDocument, ZipEntry entry, ContentFilter contentFilter) {
		this.generalDocument = generalDocument;
		this.entry = entry;
		this.contentFilter = contentFilter;
	}

	@Override
	public Event open() throws IOException {
		this.documentId = this.generalDocument.documentId();
		this.subDocumentId = this.generalDocument.nextSubDocumentId();
		contentFilter.open(
			new RawDocument(
				new BufferedInputStream(generalDocument.inputStreamFor(entry)),
				StandardCharsets.UTF_8.name(),
				this.generalDocument.sourceLocale()
			)
		);
		return convertToStartSubDocument(contentFilter.next());
	}

	/**
	 * Converts a START_DOCUMENT event to a START_SUBDOCUMENT one.
	 * @return the START_SUBDOCUMENT Event
	 */
	private Event convertToStartSubDocument(final Event event) {
		StartSubDocument sd = new StartSubDocument(documentId, subDocumentId);
		sd.setName(entry.getName());
		ConditionalParameters clonedParams = this.generalDocument.conditionalParameters().clone();
		sd.setFilterId(OpenXMLFilter.FILTER_ID);
		clonedParams.nFileType = contentFilter.getParseType();
		sd.setFilterParameters(clonedParams);
		ZipSkeleton skel = new ZipSkeleton(
			(GenericSkeleton) event.getStartDocument().getSkeleton(),
			generalDocument.zipFile(),
			entry
		);
		return new Event(EventType.START_SUBDOCUMENT, sd, skel);
	}

	@Override
	public boolean hasNextEvent() {
		return contentFilter.hasNext();
	}

	@Override
	public Event nextEvent() {
		final Event event = contentFilter.next();
		if (EventType.END_DOCUMENT != event.getEventType()) {
			return event;
		}
		return convertToEndSubDocument(event);
	}

	private Event convertToEndSubDocument(Event event) {
		Ending ending = new Ending(this.subDocumentId);
		ZipSkeleton skel = new ZipSkeleton(
			(GenericSkeleton) event.getResource().getSkeleton(),
			this.generalDocument.zipFile(),
			this.entry
		);
		return new Event(EventType.END_SUBDOCUMENT, ending, skel);
	}

	@Override
	public void close() {
		contentFilter.close();
	}

	@Override
	public void logEvent(Event e) {
		contentFilter.displayOneEvent(e);
	}
}
