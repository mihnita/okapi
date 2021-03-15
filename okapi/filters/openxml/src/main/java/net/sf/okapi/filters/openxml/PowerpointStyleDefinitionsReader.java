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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Set;

final class PowerpointStyleDefinitionsReader implements StyleDefinitionsReader {
    private static final String UNSUPPORTED_STRUCTURE = "Unsupported structure of %s";

    private final ConditionalParameters conditionalParameters;
    private final XMLEventFactory eventFactory;
    private final XMLEventReader eventReader;
    private final String style;
    private final String paragraphDefault;
    private final Set<String> paragraphLevels;
    private final Cache cache;

    PowerpointStyleDefinitionsReader(
        final ConditionalParameters conditionalParameters,
        final XMLEventFactory eventFactory,
        final XMLEventReader eventReader,
        final StartElement startElement,
        final String style
    ) {
        this(
            conditionalParameters,
            eventFactory,
            eventReader,
            startElement,
            style,
            PowerpointStyleDefinition.DEF_PPR,
            PowerpointStyleDefinition.PARAGRAPH_LEVELS
        );
    }

    PowerpointStyleDefinitionsReader(
        final ConditionalParameters conditionalParameters,
        final XMLEventFactory eventFactory,
        final XMLEventReader eventReader,
        final StartElement startElement,
        final String style,
        final String paragraphDefault,
        final Set<String> paragraphLevels
    ) {
        this.conditionalParameters = conditionalParameters;
        this.eventFactory = eventFactory;
        this.eventReader = eventReader;
        this.style = style;
        this.paragraphDefault = paragraphDefault;
        this.paragraphLevels = paragraphLevels;
        this.cache = new Cache(startElement);
    }

    @Override
    public StartElement startElement() throws XMLStreamException {
        if (this.cache.hasStartElement()) {
            return this.cache.startElement;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartElement() && this.style.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.startElement = event.asStartElement();
                return this.cache.startElement;
            }
        }
        throw new IllegalStateException(String.format(UNSUPPORTED_STRUCTURE, this.style));
    }

    PowerpointStyleDefinition paragraphDefaults() throws XMLStreamException {
        if (!this.cache.hasStartElement()) {
            startElement();
        }
        if (this.cache.hasParagraphDefaults()) {
            return this.cache.paragraphDefaults;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartElement() && this.paragraphDefault.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.paragraphDefaults = new PowerpointStyleDefinition.ParagraphDefault(
                    this.conditionalParameters,
                    this.eventFactory,
                    event.asStartElement()
                );
                this.cache.paragraphDefaults.readWith(this.eventReader);
                return this.cache.paragraphDefaults;
            }
            if (event.isStartElement() && this.paragraphLevels.contains(event.asStartElement().getName().getLocalPart())) {
                this.cache.paragraphStyleStartElement = event.asStartElement();
                break;
            }
            if (event.isEndElement() && this.style.equals(event.asEndElement().getName().getLocalPart())) {
                this.cache.endElement = event.asEndElement();
                break;
            }
        }
        this.cache.paragraphDefaults = new PowerpointStyleDefinition.ParagraphDefault(
            this.conditionalParameters,
            this.eventFactory,
            this.cache.startElement.getName().getNamespaceURI(),
            this.cache.startElement.getName().getPrefix()
        );
        return this.cache.paragraphDefaults;
    }

    boolean nextParagraphLevelAvailable() throws XMLStreamException {
        if (!this.cache.hasParagraphDefaults()) {
            paragraphDefaults();
        }
        if (this.cache.hasEndElement()) {
            return false;
        }
        if (this.cache.hasParagraphStyleStartElement()) {
            return true;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.peek();
            if (event.isStartElement() && this.paragraphLevels.contains(event.asStartElement().getName().getLocalPart())) {
                return true;
            }
            if (event.isEndElement() && this.style.equals(event.asEndElement().getName().getLocalPart())) {
                this.cache.endElement = event.asEndElement();
                this.eventReader.nextEvent();
                return false;
            }
            this.eventReader.nextEvent();
        }
        return false;
    }

    PowerpointStyleDefinition nextParagraphLevel() throws XMLStreamException {
        if (!this.cache.hasParagraphDefaults()) {
            paragraphDefaults();
        }
        if (this.cache.hasParagraphStyleStartElement()) {
            final PowerpointStyleDefinition styleDefinition = powerpointStyleDefinition(this.cache.paragraphStyleStartElement);
            styleDefinition.readWith(this.eventReader);
            this.cache.invalidateParagraphStyleStartElement();
            return styleDefinition;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartElement() && this.paragraphLevels.contains(event.asStartElement().getName().getLocalPart())) {
                final PowerpointStyleDefinition styleDefinition = powerpointStyleDefinition(event.asStartElement());
                styleDefinition.readWith(this.eventReader);
                return styleDefinition;
            }
            if (event.isEndElement() && this.style.equals(event.asEndElement().getName().getLocalPart())) {
                break;
            }
        }
        throw new IllegalStateException(String.format(UNSUPPORTED_STRUCTURE, this.style));
    }

    private PowerpointStyleDefinition powerpointStyleDefinition(final StartElement paragraphStyleStartElement) {
        return new PowerpointStyleDefinition.ParagraphLevel(
            new PowerpointStyleDefinition.ParagraphDefault(
                this.conditionalParameters,
                this.eventFactory,
                paragraphStyleStartElement
            )
        );
    }

    @Override
    public EndElement endElement() throws XMLStreamException {
        if (this.cache.hasEndElement()) {
            return this.cache.endElement;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isEndElement() && this.style.equals(event.asEndElement().getName().getLocalPart())) {
                this.cache.endElement = event.asEndElement();
                return this.cache.endElement;
            }
        }
        throw new IllegalStateException(String.format(UNSUPPORTED_STRUCTURE, this.style));
    }

    private static final class Cache {
        private StartElement startElement;
        private PowerpointStyleDefinition paragraphDefaults;
        private StartElement paragraphStyleStartElement;
        private EndElement endElement;

        Cache(final StartElement startElement) {
            this.startElement = startElement;
        }

        boolean hasStartElement() {
            return null != this.startElement;
        }

        boolean hasParagraphDefaults() {
            return null != this.paragraphDefaults;
        }

        boolean hasParagraphStyleStartElement() {
            return null != this.paragraphStyleStartElement;
        }

        void invalidateParagraphStyleStartElement() {
            this.paragraphStyleStartElement = null;
        }

        boolean hasEndElement() {
            return null != this.endElement;
        }
    }
}
