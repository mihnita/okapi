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

/**
 * Provides a consumable XML events reader.
 *
 * When the {@link ConsumableXMLEventsReader#nextEvent()} method is called
 * the returned XML event is also removed from the underling source of events.
 */
final class ConsumableXMLEventsReader implements XMLEventReader {
    /**
     * The XML events reader traversing through the XML events.
     */
    final private XMLEventReader eventReader;

    ConsumableXMLEventsReader(final XMLEventReader eventReader) {
        this.eventReader = eventReader;
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        final XMLEvent event = this.eventReader.nextEvent();
        if (null != event) {
            this.eventReader.remove();
        }
        return event;
    }

    @Override
    public boolean hasNext() {
        return this.eventReader.hasNext();
    }

    @Override
    public Object next() {
        throw new UnsupportedOperationException();
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
    public XMLEvent nextTag() throws XMLStreamException {
        XMLEvent event = nextEvent();

        while (null != event) {
            if (event.isStartElement() || event.isEndElement()) {
                break;
            }
            event = nextEvent();
        }

        return event;
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws XMLStreamException {
        this.eventReader.close();
    }
}
