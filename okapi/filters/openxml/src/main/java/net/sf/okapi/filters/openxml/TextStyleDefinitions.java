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

interface TextStyleDefinitions {
    String TX_STYLES = "txStyles";
    String OTHER = "other";

    StyleDefinitions styleFor(final String type);
    void readWith(final XMLEventReader eventReader) throws XMLStreamException;

    final class Empty implements TextStyleDefinitions {
        @Override
        public StyleDefinitions styleFor(final String type) {
            return new StyleDefinitions.Empty();
        }

        @Override
        public void readWith(final XMLEventReader eventReader) throws XMLStreamException {
        }
    }

    final class Default implements TextStyleDefinitions {
        private final StartElement startElement;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;

        private StyleDefinitions title;
        private StyleDefinitions body;
        private StyleDefinitions other;

        Default(
            final StartElement startElement,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory
        ) {
            this.startElement = startElement;
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
        }

        @Override
        public StyleDefinitions styleFor(final String type) {
            switch (type) {
                case "title":
                case "ctrTitle":
                    return null == this.title ? new StyleDefinitions.Empty() : this.title;
                case "subTitle":
                case "body":
                    return null == this.body ? new StyleDefinitions.Empty() : this.body;
                default:
                    return null == this.other ? new StyleDefinitions.Empty() : this.other;
            }
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
                if (PowerpointStyleDefinitions.TITLE_STYLE.equals(se.getName().getLocalPart())) {
                    this.title = textStyleDefinitions(se, eventReader);
                } else if (PowerpointStyleDefinitions.BODY_STYLE.equals(se.getName().getLocalPart())) {
                    this.body = textStyleDefinitions(se, eventReader);
                } else if (PowerpointStyleDefinitions.OTHER_STYLE.equals(se.getName().getLocalPart())) {
                    this.other = textStyleDefinitions(se, eventReader);
                }
            }
        }

        private StyleDefinitions textStyleDefinitions(final StartElement startElement, final XMLEventReader eventReader) throws XMLStreamException {
            final StyleDefinitions styleDefinitions = new PowerpointStyleDefinitions(this.eventFactory);
            styleDefinitions.readWith(
                new PowerpointStyleDefinitionsReader(
                    this.conditionalParameters,
                    this.eventFactory,
                    eventReader,
                    startElement,
                    startElement.getName().getLocalPart()
                )
            );
            return styleDefinitions;
        }
    }
}
