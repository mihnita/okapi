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
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

class WordStyleDefinitionsReader implements StyleDefinitionsReader {
    private static final String UNEXPECTED_STRUCTURE = "Unexpected styles structure: ";

    private final ConditionalParameters conditionalParameters;
    private final XMLEventFactory eventFactory;
    private final XMLEventReader eventReader;
    private final Cache cache;

    WordStyleDefinitionsReader(
        final ConditionalParameters conditionalParameters,
        final XMLEventFactory eventFactory,
        final XMLEventReader eventReader
    ) {
        this.conditionalParameters = conditionalParameters;
        this.eventFactory = eventFactory;
        this.eventReader = eventReader;
        this.cache = new Cache();
    }

    StartDocument startDocument() throws XMLStreamException {
        if (this.cache.hasStartDocument()) {
            return this.cache.startDocument;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartDocument()) {
                this.cache.startDocument = (StartDocument) event;
                return this.cache.startDocument;
            }
        }
        throw new IllegalStateException(UNEXPECTED_STRUCTURE.concat("the start document event is absent"));
    }

    @Override
    public StartElement startElement() throws XMLStreamException {
        if (!this.cache.hasStartDocument()) {
            startDocument();
        }
        if (this.cache.hasStartElement()) {
            return this.cache.startElement;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartElement() && WordStyleDefinitions.STYLES.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.startElement = event.asStartElement();
                return this.cache.startElement;
            }
        }
        throw new IllegalStateException(UNEXPECTED_STRUCTURE.concat("the start element is absent"));
    }

    WordStyleDefinition documentDefaults() throws XMLStreamException {
        if (!this.cache.hasStartElement()) {
            startElement();
        }
        if (this.cache.hasDocumentDefaults()) {
            return this.cache.documentDefaults;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartElement() && WordStyleDefinitions.DOC_DEFAULTS.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.documentDefaults = new WordStyleDefinition.DocumentDefaults(
                    this.conditionalParameters,
                    this.eventFactory,
                    event.asStartElement()
                );
                this.cache.documentDefaults.readWith(this.eventReader);
                return this.cache.documentDefaults;
            }
            if (event.isStartElement() && WordStyleDefinitions.LATENT_STYLES.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.latentStyles = new WordStyleDefinition.Latent(
                    this.eventFactory,
                    event.asStartElement()
                );
                this.cache.latentStyles.readWith(this.eventReader);
                break;
            }
            if (event.isStartElement() && WordStyleDefinitions.STYLE.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.generalStyleStartElement = event.asStartElement();
                break;
            }
            if (event.isEndElement() && WordStyleDefinitions.STYLES.equals(event.asEndElement().getName().getLocalPart())) {
                this.cache.endElement = event.asEndElement();
                break;
            }
        }
        this.cache.documentDefaults = new WordStyleDefinition.DocumentDefaults(
            this.conditionalParameters,
            this.eventFactory,
            this.cache.startElement.getName().getPrefix(),
            this.cache.startElement.getName().getNamespaceURI(),
            WordStyleDefinitions.DOC_DEFAULTS
        );
        return this.cache.documentDefaults;
    }

    WordStyleDefinition latent() throws XMLStreamException {
        if (!this.cache.hasDocumentDefaults()) {
            documentDefaults();
        }
        if (this.cache.hasLatentStyles()) {
            return this.cache.latentStyles;
        }
        if (this.cache.hasGeneralStyleStartElement()) {
            this.cache.latentStyles = new WordStyleDefinition.Latent(
                this.eventFactory,
                this.cache.startElement.getName().getPrefix(),
                this.cache.startElement.getName().getNamespaceURI(),
                WordStyleDefinitions.LATENT_STYLES
            );
            return this.cache.latentStyles;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartElement() && WordStyleDefinitions.LATENT_STYLES.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.latentStyles = new WordStyleDefinition.Latent(
                    this.eventFactory,
                    event.asStartElement()
                );
                this.cache.latentStyles.readWith(this.eventReader);
                return this.cache.latentStyles;
            }
            if (event.isStartElement() && WordStyleDefinitions.STYLE.equals(event.asStartElement().getName().getLocalPart())) {
                this.cache.generalStyleStartElement = event.asStartElement();
                break;
            }
            if (event.isEndElement() && WordStyleDefinitions.STYLES.equals(event.asEndElement().getName().getLocalPart())) {
                this.cache.endElement = event.asEndElement();
                break;
            }
        }
        this.cache.latentStyles = new WordStyleDefinition.Latent(
            this.eventFactory,
            this.cache.startElement.getName().getPrefix(),
            this.cache.startElement.getName().getNamespaceURI(),
            WordStyleDefinitions.LATENT_STYLES
        );
        return this.cache.latentStyles;
    }

    boolean nextGeneralAvailable() throws XMLStreamException {
        if (!this.cache.hasLatentStyles()) {
            latent();
        }
        if (this.cache.hasGeneralStyleStartElement()) {
            return true;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.peek();
            if (event.isStartElement() && WordStyleDefinitions.STYLE.equals(event.asStartElement().getName().getLocalPart())) {
                return true;
            }
            if (event.isEndElement() && WordStyleDefinitions.STYLES.equals(event.asEndElement().getName().getLocalPart())) {
                this.cache.endElement = event.asEndElement();
                this.eventReader.nextEvent();
                return false;
            }
            this.eventReader.nextEvent();
        }
        return false;
    }

    WordStyleDefinition nextGeneral() throws XMLStreamException {
        if (!this.cache.hasLatentStyles()) {
            latent();
        }
        if (this.cache.hasGeneralStyleStartElement()) {
            final WordStyleDefinition styleDefinition = wordStyleDefinition(this.cache.generalStyleStartElement);
            styleDefinition.readWith(this.eventReader);
            this.cache.invalidateGeneralStyleStartElement();
            return styleDefinition;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isStartElement() && WordStyleDefinitions.STYLE.equals(event.asStartElement().getName().getLocalPart())) {
                final WordStyleDefinition styleDefinition = wordStyleDefinition(event.asStartElement());
                styleDefinition.readWith(this.eventReader);
                return styleDefinition;
            }
            if (event.isEndElement() && WordStyleDefinitions.STYLES.equals(event.asEndElement().getName().getLocalPart())) {
                break;
            }
        }
        throw new IllegalStateException(UNEXPECTED_STRUCTURE.concat("the style-start and styles-end elements are absent"));
    }

    private WordStyleDefinition.General wordStyleDefinition(final StartElement startElement) {
        return new WordStyleDefinition.General(
            this.conditionalParameters,
            this.eventFactory,
            startElement
        );
    }

    @Override
    public EndElement endElement() throws XMLStreamException {
        if (!this.cache.hasLatentStyles()) {
            latent();
        }
        if (this.cache.hasEndElement()) {
            return this.cache.endElement;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isEndElement() && WordStyleDefinitions.STYLES.equals(event.asEndElement().getName().getLocalPart())) {
                this.cache.endElement = event.asEndElement();
                return this.cache.endElement;
            }
        }
        throw new IllegalStateException(UNEXPECTED_STRUCTURE.concat("the end element is absent"));
    }

    EndDocument endDocument() throws XMLStreamException {
        if (!this.cache.hasEndElement()) {
            endElement();
        }
        if (this.cache.hasEndDocument()) {
            return this.cache.endDocument;
        }
        while (this.eventReader.hasNext()) {
            final XMLEvent event = this.eventReader.nextEvent();
            if (event.isEndDocument()) {
                this.cache.endDocument = (EndDocument) event;
                return this.cache.endDocument;
            }
        }
        this.cache.endDocument = this.eventFactory.createEndDocument();
        return this.cache.endDocument;
    }

    private static final class Cache {
        private StartDocument startDocument;
        private StartElement startElement;
        private WordStyleDefinition documentDefaults;
        private WordStyleDefinition latentStyles;
        private StartElement generalStyleStartElement;
        private EndElement endElement;
        private EndDocument endDocument;

        Cache() {
        }

        boolean hasStartDocument() {
            return null != this.startDocument;
        }

        boolean hasStartElement() {
            return null != this.startElement;
        }

        boolean hasDocumentDefaults() {
            return null != this.documentDefaults;
        }

        boolean hasLatentStyles() {
            return null != this.latentStyles;
        }

        boolean hasGeneralStyleStartElement() {
            return null != this.generalStyleStartElement;
        }

        void invalidateGeneralStyleStartElement() {
            this.generalStyleStartElement = null;
        }

        boolean hasEndElement() {
            return null != this.endElement;
        }

        boolean hasEndDocument() {
            return null != this.endDocument;
        }
    }
}
