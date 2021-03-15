/*
 * =============================================================================
 *   Copyright (C) 2010-2013 by the Okapi Framework contributors
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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.idml.ParsingIdioms.ITEM_TRANSFORM;
import static net.sf.okapi.filters.idml.ParsingIdioms.SELF;
import static net.sf.okapi.filters.idml.ParsingIdioms.parseSpreadItems;

class SpreadParser {
    private final ZipInput<XMLEventReader> zipInputReader;
    private final QName spreadName;
    private final XMLEventFactory eventFactory;
    private final String activeLayerId;

    SpreadParser(
        final ZipInput<XMLEventReader> zipInputReader,
        final QName spreadName,
        final XMLEventFactory eventFactory,
        final String activeLayerId
    ) {
        this.zipInputReader = zipInputReader;
        this.spreadName = spreadName;
        this.eventFactory = eventFactory;
        this.activeLayerId = activeLayerId;
    }

    Spread parse(final ZipEntry entry) throws XMLStreamException, IOException {
        XMLEventReader eventReader = null;
        Spread.SpreadBuilder spreadBuilder = new Spread.SpreadBuilder()
            .setActiveLayerId(activeLayerId);

        try {
            eventReader = this.zipInputReader.of(entry);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (!event.isStartElement()) {
                    continue;
                }

                StartElement element = event.asStartElement();

                if (this.spreadName.equals(element.getName())) {
                    spreadBuilder = parseSpread(element, eventReader, spreadBuilder);
                    break;
                }
            }
        } finally {
            if (null != eventReader) {
                eventReader.close();
            }
        }

        return spreadBuilder.build();
    }

    private Spread.SpreadBuilder parseSpread(StartElement startElement, XMLEventReader eventReader, Spread.SpreadBuilder spreadBuilder) throws XMLStreamException {

        spreadBuilder.setId(startElement.getAttributeByName(SELF).getValue());
        spreadBuilder.setTransformation(startElement.getAttributeByName(ITEM_TRANSFORM).getValue());

        parseSpreadItems(startElement, eventReader, eventFactory, spreadBuilder);

        return spreadBuilder;
    }
}
