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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.DocumentPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createEndMarkupComponent;
import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createGeneralMarkupComponent;
import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createStartMarkupComponent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isAlignmentStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isPresentationEndEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isPresentationStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isSheetViewEndEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isSheetViewStartEvent;

/**
 * Provides a modifiable part.
 */
class ModifiablePart extends NonTranslatablePart {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final Document.General generalDocument;
    private final ZipEntry entry;
    private final InputStream inputStream;
    private StrippableAttributes drawingDirectionStrippableAttributes;

    ModifiablePart(final Document.General generalDocument, final ZipEntry entry, final InputStream inputStream) {
        this.generalDocument = generalDocument;
        this.entry = entry;
        this.inputStream = inputStream;
    }

    @Override
    public Event open() throws IOException, XMLStreamException {
        this.drawingDirectionStrippableAttributes = new StrippableAttributes.DrawingDirection(
            this.generalDocument.eventFactory()
        );
        XMLEventReader xmlEventReader = this.generalDocument.inputFactory().createXMLEventReader(
                new InputStreamReader(new BufferedInputStream(this.inputStream), StandardCharsets.UTF_8));

        return open(xmlEventReader);
    }

    private Event open(XMLEventReader xmlEventReader) throws XMLStreamException {
        DocumentPart documentPart;
        try {
            documentPart = handlePart(xmlEventReader);
        } finally {
            if (null != xmlEventReader) {
                xmlEventReader.close();
            }
        }

        return new Event(EventType.DOCUMENT_PART, documentPart);
    }

    private DocumentPart handlePart(XMLEventReader xmlEventReader) throws XMLStreamException {
        ModifiablePart.MarkupBuilder markupBuilder = new ModifiablePart.MarkupBuilder();

        while (xmlEventReader.hasNext()) {
            XMLEvent event = xmlEventReader.nextEvent();

            if (isPresentationStartEvent(event)
                    || isSheetViewStartEvent(event)) {
                markupBuilder.addMarkupComponent(
                    new MarkupComponent.Start(
                        this.generalDocument.eventFactory(),
                        this.drawingDirectionStrippableAttributes.strip(event.asStartElement())
                    )
                );
            } else if (isPresentationEndEvent(event)
                    || isSheetViewEndEvent(event)) {
                markupBuilder.addMarkupComponent(createEndMarkupComponent(event.asEndElement()));
            } else if (isAlignmentStartEvent(event)) {
                markupBuilder.addMarkupComponent(new MarkupComponentParser().parseEmptyElementMarkupComponent(xmlEventReader, this.generalDocument.eventFactory(), event.asStartElement()));
            } else if (event.isStartElement()
                && PowerpointStyleDefinitions.NAMES.contains(event.asStartElement().getName().getLocalPart())) {
                final PowerpointStyleDefinitionsReader reader = new PowerpointStyleDefinitionsReader(
                    this.generalDocument.conditionalParameters(),
                    this.generalDocument.eventFactory(),
                    xmlEventReader,
                    event.asStartElement(),
                    event.asStartElement().getName().getLocalPart()
                );
                final PowerpointStyleDefinitions powerpointStyleDefinitions =
                    new PowerpointStyleDefinitions(this.generalDocument.eventFactory());
                powerpointStyleDefinitions.readWith(reader);
                markupBuilder.addMarkup(powerpointStyleDefinitions.toMarkup());
            } else {
                markupBuilder.addEvent(event);
            }
        }

        DocumentPart documentPart = new DocumentPart(this.entry.getName(), false);
        documentPart.setSkeleton(
            new MarkupZipSkeleton(
                this.generalDocument.zipFile(),
                this.entry,
                this.generalDocument.outputFactory(),
                this.generalDocument.encoding(),
                markupBuilder.build()
            )
        );
        return documentPart;
    }

    @Override
    public void logEvent(Event e) {
        LOGGER.trace("[[ {}: {} ]]", getClass().getSimpleName(), entry.getName());
    }

    private static class MarkupBuilder {
        private List<XMLEvent> events = new ArrayList<>();
        private Markup markup = new Block.Markup(new Markup.General(new ArrayList<>()));

        void addEvent(XMLEvent event) {
            events.add(event);
        }

        void addMarkupComponent(MarkupComponent markupComponent) {
            flushEvents();
            this.markup.addComponent(markupComponent);
        }

        void addMarkup(final Markup markup) {
            flushEvents();
            this.markup.addMarkup(markup);
        }

        private void flushEvents() {
            if (!events.isEmpty()) {
                this.markup.addComponent(createGeneralMarkupComponent(events));
                events = new ArrayList<>();
            }
        }

        Markup build() {
            flushEvents();
            return this.markup;
        }
    }
}
