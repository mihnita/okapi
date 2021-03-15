/*
 * =============================================================================
 *   Copyright (C) 2010-2017 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =============================================================================
 */

package net.sf.okapi.filters.idml;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.skeleton.ZipSkeleton;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class StorySubDocument implements SubDocument {
    private static final String MIME_TYPE = "text/xml";

    private final Parameters parameters;
    private final ZipInput<XMLEventReader> zipInputReader;
    private final XMLEventFactory eventFactory;
    private final ZipFile zipFile;
    private final ZipEntry zipEntry;
    private final String parentId;
    private final String id;

    private Iterator<Event> eventsIterator;

    StorySubDocument(
        final Parameters parameters,
        final ZipInput<XMLEventReader> zipInputReader,
        final XMLEventFactory eventFactory,
        final ZipFile zipFile,
        final ZipEntry zipEntry,
        final String parentId,
        final String id
    ) {
        this.parameters = parameters;
        this.zipInputReader = zipInputReader;
        this.eventFactory = eventFactory;
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
        this.parentId = parentId;
        this.id = id;
    }

    /**
     * Opens this sub-document and performs any initial processing.
     *
     * @return Event The first event for this sub-document (START_SUBDOCUMENT)
     * @throws IOException
     * @throws XMLStreamException
     */
    public Event open() throws IOException, XMLStreamException {
        XMLEventReader eventReader = null;
        try {
            eventReader = this.zipInputReader.of(this.zipEntry);

            Story story = new StoryParser(parameters, eventFactory, eventReader).parse();
            IdGenerator documentPartIdGenerator = new IdGenerator(zipEntry.getName(), IdGenerator.DOCUMENT_PART);
            IdGenerator textUnitIdGenerator = new IdGenerator(zipEntry.getName(), IdGenerator.TEXT_UNIT);

            List<Event> events = new StoryEventsAccumulator(story, parameters, eventFactory, documentPartIdGenerator, textUnitIdGenerator).accumulate();
            events.add(
                new Event(
                    EventType.END_SUBDOCUMENT,
                    new Ending(id),
                    new ZipSkeleton(
                        this.zipFile,
                        this.zipEntry
                    )
                )
            );
            eventsIterator = events.iterator();
        } finally {
            if (eventReader != null) {
                eventReader.close();
            }
        }

        return createStartSubDocumentEvent();
    }

    protected Event createStartSubDocumentEvent() {
        StartSubDocument sd = new StartSubDocument(parentId, id);
        sd.setName(zipEntry.getName());
        sd.setMimeType(MIME_TYPE);
        ZipSkeleton zs = new ZipSkeleton(zipFile, zipEntry);
        sd.setSkeleton(zs);
        sd.setFilterId(IDMLFilter.FILTER_ID);
        sd.setFilterParameters(parameters);

        return new Event(EventType.START_SUBDOCUMENT, sd);
    }

    @Override
    public boolean hasNextEvent() {
        return eventsIterator.hasNext();
    }

    @Override
    public Event nextEvent() {
        return eventsIterator.next();
    }

    @Override
    public void close() {
    }

    @Override
    public void logEvent(Event e) {
    }
}
