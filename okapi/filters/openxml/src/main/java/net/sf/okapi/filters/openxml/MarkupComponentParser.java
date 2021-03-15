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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

import static net.sf.okapi.filters.openxml.BlockPropertiesFactory.createBlockProperties;
import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isEndElement;

/**
 * Provides a markup component parser.
 */
class MarkupComponentParser {

    MarkupComponent parseEmptyElementMarkupComponent(XMLEventReader eventReader, XMLEventFactory eventFactory, StartElement startElement) throws XMLStreamException {
        if (!eventReader.hasNext()) {
            throw new IllegalStateException(ExceptionMessages.UNEXPECTED_STRUCTURE);
        }

        XMLEvent nextEvent = eventReader.nextEvent();

        if (!isEndElement(nextEvent, startElement)) {
            throw new IllegalStateException(ExceptionMessages.UNEXPECTED_STRUCTURE);
        }


        return MarkupComponentFactory.createEmptyElementMarkupComponent(eventFactory, startElement, nextEvent.asEndElement());
    }

    BlockProperties parseBlockProperties(
        final StartElementContext startElementContext,
        final StrippableAttributes strippableAttributes,
        final SkippableElements skippableElements
    ) throws XMLStreamException {
        final List<Property> properties = new ArrayList<>();

        while (startElementContext.getEventReader().hasNext()) {
            XMLEvent event = startElementContext.getEventReader().nextEvent();

            if (isEndElement(event, startElementContext.getStartElement())) {
                return createBlockProperties(
                    startElementContext.getConditionalParameters(),
                    startElementContext.getEventFactory(),
                    startElementContext.getStartElement(),
                    event.asEndElement(),
                    properties
                );
            }

            if (!event.isStartElement()) {
                continue;
            }

            if (skippableElements.canBeSkipped(event.asStartElement(), startElementContext.getStartElement())) {
                // skip the first level skippable properties
                skippableElements.skip(createStartElementContext(event.asStartElement(), startElementContext));
                continue;
            }
            final BlockProperty property = new BlockProperty(
                eventsWithSkippedRevisionProperties(
                    createStartElementContext(event.asStartElement(), startElementContext),
                    skippableElements
                ),
                startElementContext.getConditionalParameters(),
                startElementContext.getEventFactory(),
                strippableAttributes
            );
            if (isEmptyBlockRunPropertiesElement(property)) {
                continue;
            }
            properties.add(property);
        }

        throw new IllegalStateException(ExceptionMessages.UNEXPECTED_STRUCTURE);
    }

    private static boolean isEmptyBlockRunPropertiesElement(BlockProperty blockProperty) {
        if (!RunProperties.RPR.equals(blockProperty.getName().getLocalPart())
                && !RunProperties.DEF_RPR.equals(blockProperty.getName().getLocalPart())) {
            return false; // do not care about others
        }
        if (blockProperty.getEvents().get(0).asStartElement().getAttributes().hasNext()) {
            return false;
        }
        if (2 == blockProperty.getEvents().size()) {
            return true;
        }
        return blockProperty.getEvents().subList(1, blockProperty.getEvents().size() - 1).stream()
            .allMatch(e -> XMLEventHelpers.isWhitespace(e));
    }

    private static List<XMLEvent> eventsWithSkippedRevisionProperties(
        final StartElementContext startElementContext,
        final SkippableElements skippableElements
    ) throws XMLStreamException {
        List<XMLEvent> events = new ArrayList<>();

        events.add(startElementContext.getStartElement());

        while (startElementContext.getEventReader().hasNext()) {
            XMLEvent event = startElementContext.getEventReader().nextEvent();

            if (event.isStartElement() && skippableElements.canBeSkipped(event.asStartElement(), startElementContext.getStartElement())) {
                // skip the second level skippable properties
                skippableElements.skip(createStartElementContext(event.asStartElement(), startElementContext));
                continue;
            }
            events.add(event);

            if (isEndElement(event, startElementContext.getStartElement())) {
                return events;
            }
        }

        throw new IllegalStateException(ExceptionMessages.UNEXPECTED_STRUCTURE);
    }

    ParagraphBlockProperties parseParagraphBlockProperties(
        final StartElementContext startElementContext,
        final StrippableAttributes strippableAttributes,
        final SkippableElements skippableElements
    ) throws XMLStreamException {
        return (ParagraphBlockProperties) parseBlockProperties(
            startElementContext,
            strippableAttributes,
            skippableElements
        );
    }
}
