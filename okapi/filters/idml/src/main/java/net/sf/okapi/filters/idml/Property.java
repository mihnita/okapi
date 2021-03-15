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

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class Property implements Element {
    private final Element.Default defaultElement;

    Property(StartElement startElement, List<XMLEvent> innerEvents, EndElement endElement, XMLEventFactory eventFactory) {
        this(new Element.Default(startElement, innerEvents, endElement, eventFactory));
    }

    Property(final Element.Default defaultElement) {
        this.defaultElement = defaultElement;
    }

    @Override
    public StartElement startElement() {
        return this.defaultElement.startElement();
    }

    @Override
    public List<XMLEvent> innerEvents() {
        return this.defaultElement.innerEvents();
    }

    @Override
    public void updateInnerEventsWith(final List<XMLEvent> events) {
        this.defaultElement.updateInnerEventsWith(events);
    }

    @Override
    public EndElement endElement() {
        return this.defaultElement.endElement();
    }

    @Override
    public void apply(final FontMappings fontMappings) {
        final StringBuilder sb = new StringBuilder();
        innerEvents().forEach(e -> sb.append(e.asCharacters().getData()));
        final String sourceFont = sb.toString();
        final String targetFont = fontMappings.targetFontFor(sourceFont);
        if (!targetFont.equals(sourceFont)) {
            updateInnerEventsWith(
                Collections.singletonList(
                    this.defaultElement.eventFactory().createCharacters(
                        targetFont
                    )
                )
            );
        }
    }

    @Override
    public List<XMLEvent> getEvents() {
        return this.defaultElement.getEvents();
    }

    @Override
    public QName getName() {
        return this.defaultElement.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;

        Property that = (Property) o;

        return Objects.equals(startElement(), that.startElement())
                && Objects.equals(innerEvents(), that.innerEvents())
                && Objects.equals(endElement(), that.endElement());
    }

    @Override
    public int hashCode() {
        return Objects.hash(startElement(), innerEvents(), endElement());
    }

    static class Builder extends Element.Builder {

        @Override
        public Property build() {
            return new Property(startElement, innerEvents, endElement, eventFactory);
        }
    }

    static class PathGeometryProperty extends Property {

        private final List<GeometryPath> geometryPaths;

        PathGeometryProperty(StartElement startElement, List<GeometryPath> geometryPaths, EndElement endElement, XMLEventFactory eventFactory) {
            super(startElement, Collections.<XMLEvent>emptyList(), endElement, eventFactory);
            this.geometryPaths = geometryPaths;
        }

        @Override
        public List<XMLEvent> innerEvents() {
            List<XMLEvent> events = new ArrayList<>();

            for (GeometryPath geometryPath : geometryPaths) {
                events.addAll(geometryPath.getEvents());
            }

            return events;
        }

        List<GeometryPath> getGeometryPaths() {
            return geometryPaths;
        }

        static class Builder extends Property.Builder {

            private List<GeometryPath> geometryPaths = new ArrayList<>();

            Builder addGeometryPath(GeometryPath geometryPath) {
                geometryPaths.add(geometryPath);
                return this;
            }

            @Override
            public PathGeometryProperty build() {
                return new PathGeometryProperty(startElement, geometryPaths, endElement, eventFactory);
            }
        }
    }
}
