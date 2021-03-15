/*
 * =============================================================================
 * Copyright (C) 2010-2020 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================================
 */
package net.sf.okapi.filters.openxml;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

final class NotesMasterFragments implements SlideTemplateFragments {
    static String NOTES_MASTER = "notesMaster";

    private final StartElement startElement;
    private final ConditionalParameters conditionalParameters;
    private final XMLEventFactory eventFactory;
    private SlideFragments slideFragments;
    private StyleDefinitions notesStyleDefinitions;

    NotesMasterFragments(
        final StartElement startElement,
        final ConditionalParameters conditionalParameters,
        final XMLEventFactory eventFactory
    ) {
        this.startElement = startElement;
        this.conditionalParameters = conditionalParameters;
        this.eventFactory = eventFactory;
    }

    @Override
    public StyleDefinitions listStyleFor(final String shapeId) {
        return slideFragments().listStyleFor(shapeId);
    }

    @Override
    public StyleDefinitions listStyleFor(final Placeholder placeholder) {
        return notesStyleDefinitions().mergedWith(
            slideFragments().listStyleFor(placeholder)
        );
    }

    @Override
    public StyleDefinitions listStyleFor(final String graphicFrameId, final int tableCellNumber) {
        return new StyleDefinitions.Empty();
    }

    private SlideFragments slideFragments() {
        return null == this.slideFragments
            ? new SlideTemplateFragments.Empty()
            : this.slideFragments;
    }

    private StyleDefinitions notesStyleDefinitions() {
        return null == this.notesStyleDefinitions
            ? new StyleDefinitions.Empty()
            : this.notesStyleDefinitions;
    }

    @Override
    public void readWith(final XMLEventReader eventReader) throws XMLStreamException {
        while (eventReader.hasNext()) {
            final XMLEvent e = eventReader.nextEvent();
            if (e.isEndElement() && e.asEndElement().getName().equals(this.startElement.getName())) {
                break;
            }
            if (!e.isStartElement()) {
                continue;
            }
            final StartElement se = e.asStartElement();
            if (NotesMasterFragments.C_SLD.equals(se.getName().getLocalPart())) {
                this.slideFragments = new SlideFragments.Default(
                    se,
                    this.conditionalParameters,
                    this.eventFactory,
                    new SlideTemplateFragments.Empty()
                );
                this.slideFragments.readWith(eventReader);
            } else if (PowerpointStyleDefinitions.NOTES_STYLE.equals(se.getName().getLocalPart())) {
                final PowerpointStyleDefinitionsReader styleDefinitionsReader = new PowerpointStyleDefinitionsReader(
                    this.conditionalParameters,
                    this.eventFactory,
                    eventReader,
                    se,
                    se.getName().getLocalPart()
                );
                this.notesStyleDefinitions = new PowerpointStyleDefinitions(
                    this.eventFactory
                );
                this.notesStyleDefinitions.readWith(styleDefinitionsReader);
            }
        }
    }
}
