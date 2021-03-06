/*
 * =============================================================================
 *   Copyright (C) 2010-2013 by the Okapi Framework contributors
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

package net.sf.okapi.filters.idml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import static net.sf.okapi.filters.idml.ParsingIdioms.UNEXPECTED_STRUCTURE;

class GeometryPathParser {

    private static final QName PATH_POINT_ARRAY = Namespaces.getDefaultNamespace().getQName("PathPointArray");

    private final StartElement startElement;
    private final XMLEventReader eventReader;
    private final XMLEventFactory eventFactory;

    GeometryPathParser(
        final StartElement startElement,
        final XMLEventReader eventReader,
        final XMLEventFactory eventFactory
    ) {
        this.startElement = startElement;
        this.eventReader = eventReader;
        this.eventFactory = eventFactory;
    }

    GeometryPath parse() throws XMLStreamException {
        GeometryPath.Builder geometryPathBuilder = new GeometryPath.Builder();

        geometryPathBuilder.setStartElement(startElement);

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                geometryPathBuilder.setEndElement(event.asEndElement());
                return geometryPathBuilder.build();
            }

            if (!event.isStartElement() || !PATH_POINT_ARRAY.equals(event.asStartElement().getName())) {
                continue;
            }

            parsePathPointArray(event.asStartElement(), geometryPathBuilder);
        }

        throw new IllegalStateException(UNEXPECTED_STRUCTURE);
    }

    private void parsePathPointArray(StartElement startElement, GeometryPath.Builder geometryPathBuilder) throws XMLStreamException {
        geometryPathBuilder.setPathPointArrayStartElement(startElement);

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                geometryPathBuilder.setPathPointArrayEndElement(event.asEndElement());
                return;
            }

            if (!event.isStartElement() || !PathPoint.NAME.equals(event.asStartElement().getName())) {
                continue;
            }

            geometryPathBuilder.addPathPoint(parsePathPoint(event.asStartElement()));
        }

        throw new IllegalStateException(UNEXPECTED_STRUCTURE);
    }

    private PathPoint parsePathPoint(StartElement startElement) throws XMLStreamException {
        XMLEvent event = eventReader.nextTag();

        if (!event.isEndElement() || !PathPoint.NAME.equals(event.asEndElement().getName())) {
            throw new IllegalStateException(UNEXPECTED_STRUCTURE);
        }

        return new PathPoint.Default(startElement, this.eventFactory);
    }
}
