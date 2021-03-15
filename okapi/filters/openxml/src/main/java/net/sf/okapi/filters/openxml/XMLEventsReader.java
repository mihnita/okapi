/*
 * =============================================================================
 *   Copyright (C) 2010-2019 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.isWhitespace;

/**
 * Provides an XML events reader.
 */
final class XMLEventsReader implements XMLEventReader {
    /**
     * An XML events iterator.
     */
    private final Iterator<XMLEvent> eventsIterator;

    XMLEventsReader(final Iterable<XMLEvent> events) {
        this.eventsIterator = events.iterator();
    }

    @Override
    public boolean hasNext() {
        return eventsIterator.hasNext();
    }

    @Override
    public XMLEvent nextEvent() {
        return eventsIterator.next();
    }

    @Override
    public void remove() {
        eventsIterator.remove();
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        for (XMLEvent e = nextEvent(); e != null; e = nextEvent()) {
            if (e.isStartElement() || e.isEndElement()) {
                return e;
            } else if (!isWhitespace(e)) {
                throw new IllegalStateException("Unexpected event: " + e);
            }
        }
        return null;
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws XMLStreamException {
    }

    @Override
    public Object next() {
        return eventsIterator.next();
    }
}
