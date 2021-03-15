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

interface SlideFragments {
    String C_SLD = "cSld";

    StyleDefinitions listStyleFor(final String shapeId);
    StyleDefinitions listStyleFor(final Placeholder placeholder);
    StyleDefinitions listStyleFor(final String graphicFrameId, final int tableCellNumber);
    void readWith(final XMLEventReader eventReader) throws XMLStreamException;

    final class Empty implements SlideFragments {
        private final SlideFragments templateFragments;

        Empty(final SlideFragments templateFragments) {
            this.templateFragments = templateFragments;
        }

        @Override
        public StyleDefinitions listStyleFor(final String shapeId) {
            return new StyleDefinitions.Empty();
        }

        @Override
        public StyleDefinitions listStyleFor(final Placeholder placeholder) {
            return this.templateFragments.listStyleFor(placeholder);
        }

        @Override
        public StyleDefinitions listStyleFor(final String graphicFrameId, final int tableCellNumber) {
            return this.templateFragments.listStyleFor(graphicFrameId, tableCellNumber);
        }

        @Override
        public void readWith(final XMLEventReader eventReader) throws XMLStreamException {
        }
    }

    final class Default implements SlideFragments {
        private final StartElement startElement;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final SlideFragments templateFragments;
        private ShapeTreeFragments shapeTreeFragments;

        Default(
            final StartElement startElement,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final SlideFragments templateFragments
        ) {
            this.startElement = startElement;
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.templateFragments = templateFragments;
        }

        @Override
        public StyleDefinitions listStyleFor(final String shapeId) {
            return null == this.shapeTreeFragments
                ? new StyleDefinitions.Empty()
                : this.shapeTreeFragments.listStyleFor(shapeId);
        }

        @Override
        public StyleDefinitions listStyleFor(final Placeholder placeholder) {
            return null == this.shapeTreeFragments
                ? this.templateFragments.listStyleFor(placeholder)
                : this.templateFragments.listStyleFor(placeholder).mergedWith(
                    this.shapeTreeFragments.listStyleFor(placeholder)
                );
        }

        @Override
        public StyleDefinitions listStyleFor(final String graphicFrameId, final int tableCellNumber) {
            return null == this.shapeTreeFragments
                ? this.templateFragments.listStyleFor(graphicFrameId, tableCellNumber)
                : this.templateFragments.listStyleFor(graphicFrameId, tableCellNumber).mergedWith(
                    this.shapeTreeFragments.listStyleFor(graphicFrameId, tableCellNumber)
                );
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
                if (ShapeTreeFragments.SP_TREE.equals(se.getName().getLocalPart())) {
                    this.shapeTreeFragments = new ShapeTreeFragments.Default(
                        se,
                        this.conditionalParameters,
                        this.eventFactory
                    );
                    this.shapeTreeFragments.readWith(eventReader);
                }
            }
        }
    }
}
