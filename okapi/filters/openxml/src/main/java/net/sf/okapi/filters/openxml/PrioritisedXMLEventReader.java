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
 * Provides a prioritised XML event reader.
 *
 * It accepts two XML event readers, the first of which is given a priority for
 * its XML source reading.
 */
final class PrioritisedXMLEventReader implements XMLEventReader {

    /**
     * A prioritised reader.
     */
    private final XMLEventReader prioritisedReader;

    /**
     * A default reader.
     */
    private final XMLEventReader defaultReader;

    PrioritisedXMLEventReader(final XMLEventReader prioritisedReader, final XMLEventReader defaultReader) {
        this.prioritisedReader = prioritisedReader;
        this.defaultReader = defaultReader;
    }

    @Override
    public boolean hasNext() {
        return this.prioritisedReader.hasNext() || this.defaultReader.hasNext();
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        return this.prioritisedReader.hasNext()
            ? this.prioritisedReader.nextEvent()
            : this.defaultReader.nextEvent();
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        return this.prioritisedReader.hasNext()
            ? this.prioritisedReader.nextTag()
            : this.defaultReader.nextTag();
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        return this.prioritisedReader.hasNext()
            ? this.prioritisedReader.peek()
            : this.defaultReader.peek();
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
        this.prioritisedReader.close();
        this.defaultReader.close();
    }

    @Override
    public Object next() {
        return this.prioritisedReader.hasNext()
            ? this.prioritisedReader.next()
            : this.defaultReader.next();
    }
}
