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
import java.util.HashMap;
import java.util.Map;

interface ShapeTreeFragments {
    String SP_TREE = "spTree";

    StyleDefinitions listStyleFor(final String shapeId);
    StyleDefinitions listStyleFor(final Placeholder placeholder);
    StyleDefinitions listStyleFor(final String graphicFrameId, final int tableCellNumber);
    void readWith(final XMLEventReader eventReader) throws XMLStreamException;

    final class Empty implements ShapeTreeFragments {
        @Override
        public StyleDefinitions listStyleFor(final String shapeId) {
            return new StyleDefinitions.Empty();
        }

        @Override
        public StyleDefinitions listStyleFor(final Placeholder placeholder) {
            return new StyleDefinitions.Empty();
        }

        @Override
        public StyleDefinitions listStyleFor(final String graphicFrameId, final int tableCellNumber) {
            return new StyleDefinitions.Empty();
        }

        @Override
        public void readWith(final XMLEventReader eventReader) throws XMLStreamException {
        }
    }

    final class Default implements ShapeTreeFragments {
        private final StartElement startElement;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final Map<String, ShapeFragments> shapeFragmentsById;
        private final Map<Placeholder, ShapeFragments> shapeFragmentsByPlaceholder;
        private final Map<String, GraphicFrameFragments> graphicFrameFragmentsById;

        Default(
            final StartElement startElement,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory
        ) {
            this(
                startElement,
                conditionalParameters,
                eventFactory,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
            );
        }

        Default(
            final StartElement startElement,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final Map<String, ShapeFragments> shapeFragmentsById,
            final Map<Placeholder, ShapeFragments> shapeFragmentsByPlaceholder,
            final Map<String, GraphicFrameFragments> graphicFrameFragmentsById
        ) {
            this.startElement = startElement;
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.shapeFragmentsById = shapeFragmentsById;
            this.shapeFragmentsByPlaceholder = shapeFragmentsByPlaceholder;
            this.graphicFrameFragmentsById = graphicFrameFragmentsById;
        }

        @Override
        public StyleDefinitions listStyleFor(final String shapeId) {
            if (!this.shapeFragmentsById.containsKey(shapeId)) {
                throw new IllegalArgumentException("The provided ID is not available");
            }
            return this.shapeFragmentsById.get(shapeId).listStyle();
        }

        @Override
        public StyleDefinitions listStyleFor(final Placeholder placeholder) {
            return this.shapeFragmentsByPlaceholder.containsKey(placeholder)
                ? this.shapeFragmentsByPlaceholder.get(placeholder).listStyle()
                : new StyleDefinitions.Empty();
        }

        @Override
        public StyleDefinitions listStyleFor(final String graphicFrameId, final int tableCellNumber) {
            return this.graphicFrameFragmentsById.containsKey(graphicFrameId)
                ? this.graphicFrameFragmentsById.get(graphicFrameId).listStyleFor(tableCellNumber)
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
                if (ShapeFragments.SP.equals(se.getName().getLocalPart())) {
                    final ShapeFragments shapeFragments = new ShapeFragments.Default(
                        se,
                        this.conditionalParameters,
                        this.eventFactory
                    );
                    shapeFragments.readWith(eventReader);
                    this.shapeFragmentsById.put(
                        shapeFragments.id(),
                        shapeFragments
                    );
                    if (!ShapeFragments.PLACEHOLDER_EMPTY_TYPE.equals(shapeFragments.placeholder().type())
                        && !ShapeFragments.PLACEHOLDER_EMPTY_INDEX.equals(shapeFragments.placeholder().index())) {
                        this.shapeFragmentsByPlaceholder.put(
                            shapeFragments.placeholder(),
                            shapeFragments
                        );
                    }
                    continue;
                }
                if (GraphicFrameFragments.GRAPHIC_FRAME.equals(se.getName().getLocalPart())) {
                    final GraphicFrameFragments graphicFrameFragments = new GraphicFrameFragments.Default(
                        se,
                        this.conditionalParameters,
                        this.eventFactory
                    );
                    graphicFrameFragments.readWith(eventReader);
                    this.graphicFrameFragmentsById.put(
                        graphicFrameFragments.id(),
                        graphicFrameFragments
                    );
                }
            }
        }
    }
}
