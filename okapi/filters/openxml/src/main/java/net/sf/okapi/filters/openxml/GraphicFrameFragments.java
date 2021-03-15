/*
 * =============================================================================
 * Copyright (C) 2010-2021 by the Okapi Framework contributors
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
import java.util.HashMap;
import java.util.Map;

interface GraphicFrameFragments {
    String GRAPHIC_FRAME = "graphicFrame";
    String C_NV_PR = "cNvPr";
    String TC = "tc";

    String id();
    StyleDefinitions listStyleFor(final int tableCellNumber);
    void readWith(final XMLEventReader eventReader) throws XMLStreamException;

    final class Default implements GraphicFrameFragments {
        private final StartElement startElement;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final Map<Integer, StyleDefinitions> listStylesByTableCellNumber;
        private NonVisualIdentificationPropertyFragments nonVisualIdentificationPropertyFragments;
        private int tableCellNumber;

        Default(
            final StartElement startElement,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory
        ) {
            this(
                startElement,
                conditionalParameters,
                eventFactory,
                new HashMap<>()
            );
        }

        Default(
            final StartElement startElement,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final Map<Integer, StyleDefinitions> listStylesByTableCellNumber
        ) {
            this.startElement = startElement;
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.listStylesByTableCellNumber = listStylesByTableCellNumber;
        }

        @Override
        public String id() {
            if (null == this.nonVisualIdentificationPropertyFragments) {
                throw new IllegalStateException("The non-visual drawing properties are required");
            }
            return this.nonVisualIdentificationPropertyFragments.id();
        }

        @Override
        public StyleDefinitions listStyleFor(final int tableCellNumber) {
            return this.listStylesByTableCellNumber.containsKey(tableCellNumber)
                ? this.listStylesByTableCellNumber.get(tableCellNumber)
                : new StyleDefinitions.Empty();
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
                if (Default.C_NV_PR.equals(se.getName().getLocalPart())) {
                    this.nonVisualIdentificationPropertyFragments = new NonVisualIdentificationPropertyFragments.Default(se);
                } else if (Default.TC.equals(se.getName().getLocalPart())) {
                    this.tableCellNumber++;
                } else if (PowerpointStyleDefinitions.LST_STYLE.equals(se.getName().getLocalPart())) {
                    final StyleDefinitions listStyle = new PowerpointStyleDefinitions(this.eventFactory);
                    listStyle.readWith(
                        new PowerpointStyleDefinitionsReader(
                            this.conditionalParameters,
                            this.eventFactory,
                            eventReader,
                            se,
                            se.getName().getLocalPart()
                        )
                    );
                    this.listStylesByTableCellNumber.put(
                        this.tableCellNumber,
                        listStyle
                    );
                }
            }
        }
    }
}
