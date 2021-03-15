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

class PropertyParser extends ElementParser {

    private static final QName GEOMETRY_PATH_TYPE = Namespaces.getDefaultNamespace().getQName("GeometryPathType");

    PropertyParser(StartElement startElement, XMLEventReader eventReader, XMLEventFactory eventFactory) {
        super(startElement, eventReader, eventFactory);
    }

    Property parse() throws XMLStreamException {
        return (Property) super.parse(new Property.Builder());
    }

    static class PathGeometryPropertyParserRange extends PropertyParser {

        PathGeometryPropertyParserRange(StartElement startElement, XMLEventReader eventReader, XMLEventFactory eventFactory) {
            super(startElement, eventReader, eventFactory);
        }

        Property.PathGeometryProperty parse() throws XMLStreamException {
            Property.PathGeometryProperty.Builder builder = new Property.PathGeometryProperty.Builder();

            builder.setStartElement(startElement);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                    builder.setEndElement(event.asEndElement());
                    return builder.build();
                }

                if (!event.isStartElement() || !GEOMETRY_PATH_TYPE.equals(event.asStartElement().getName())) {
                    continue;
                }

                builder.addGeometryPath(
                    new GeometryPathParser(event.asStartElement(), eventReader, eventFactory).parse()
                );
            }

            throw new IllegalStateException(UNEXPECTED_STRUCTURE);
        }
    }
}
