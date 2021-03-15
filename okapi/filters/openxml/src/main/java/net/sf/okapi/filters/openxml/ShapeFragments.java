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

interface ShapeFragments {
    String SP = "sp";
    String C_NV_PR = "cNvPr";
    String NV_PR = "nvPr";
    String PLACEHOLDER = "ph";
    String PLACEHOLDER_EMPTY_TYPE = "";
    String PLACEHOLDER_EMPTY_INDEX = PLACEHOLDER_EMPTY_TYPE;

    String id();
    Placeholder placeholder();
    StyleDefinitions listStyle();
    void readWith(final XMLEventReader eventReader) throws XMLStreamException;

    final class Default implements ShapeFragments {
        private final StartElement startElement;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private NonVisualIdentificationPropertyFragments nonVisualIdentificationPropertyFragments;
        private Placeholder placeholder;
        private StyleDefinitions listStyle;

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
        public String id() {
            if (null == this.nonVisualIdentificationPropertyFragments) {
                throw new IllegalStateException("The non-visual drawing properties are required");
            }
            return this.nonVisualIdentificationPropertyFragments.id();
        }

        @Override
        public Placeholder placeholder() {
            return null == this.placeholder
                ? new Placeholder.Default(PLACEHOLDER_EMPTY_TYPE, PLACEHOLDER_EMPTY_INDEX)
                : this.placeholder;
        }

        @Override
        public StyleDefinitions listStyle() {
            return null == this.listStyle
                ? new StyleDefinitions.Empty()
                : this.listStyle;
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
                if (ShapeFragments.C_NV_PR.equals(se.getName().getLocalPart())) {
                    this.nonVisualIdentificationPropertyFragments = new NonVisualIdentificationPropertyFragments.Default(se);
                } else if (ShapeFragments.PLACEHOLDER.equals(se.getName().getLocalPart())) {
                    this.placeholder = new Placeholder.Default(se);
                } else if (PowerpointStyleDefinitions.LST_STYLE.equals(se.getName().getLocalPart())) {
                    this.listStyle =  new PowerpointStyleDefinitions(this.eventFactory);
                    this.listStyle.readWith(
                        new PowerpointStyleDefinitionsReader(
                            this.conditionalParameters,
                            this.eventFactory,
                            eventReader,
                            se,
                            se.getName().getLocalPart()
                        )
                    );
                }
            }
        }
    }
}
