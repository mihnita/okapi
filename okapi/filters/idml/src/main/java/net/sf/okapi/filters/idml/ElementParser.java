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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import static net.sf.okapi.filters.idml.ParsingIdioms.UNEXPECTED_STRUCTURE;

class ElementParser {

    protected final StartElement startElement;
    protected final XMLEventReader eventReader;
    protected final XMLEventFactory eventFactory;

    ElementParser(StartElement startElement, XMLEventReader eventReader, XMLEventFactory eventFactory) {
        this.startElement = startElement;
        this.eventReader = eventReader;
        this.eventFactory = eventFactory;
    }

    Element parse(Element.Builder builder) throws XMLStreamException {
        int inlineNestingDepth = 0;

        builder.setStartElement(startElement);

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement() && event.asStartElement().getName().equals(startElement.getName())) {
                inlineNestingDepth++;
            }

            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {

                if (0 < inlineNestingDepth) {
                    builder.addInnerEvent(event);
                    inlineNestingDepth--;
                    continue;
                }

                builder.setEndElement(event.asEndElement());
                builder.setEventFactory(eventFactory);

                return builder.build();
            }

            builder.addInnerEvent(event);
        }

        throw new IllegalStateException(UNEXPECTED_STRUCTURE);
    }
}
