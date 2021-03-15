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
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

class GeometryPath implements Element {
    private final StartElement startElement;
    private final StartElement pathPointArrayStartElement;
    private final List<PathPoint> pathPoints;
    private final EndElement pathPointArrayEndElement;
    private final EndElement endElement;

    GeometryPath(StartElement startElement, StartElement pathPointArrayStartElement, List<PathPoint> pathPoints, EndElement pathPointArrayEndElement, EndElement endElement) {
        this.startElement = startElement;
        this.pathPointArrayStartElement = pathPointArrayStartElement;
        this.pathPoints = pathPoints;
        this.pathPointArrayEndElement = pathPointArrayEndElement;
        this.endElement = endElement;
    }

    @Override
    public StartElement startElement() {
        return this.startElement;
    }

    @Override
    public List<XMLEvent> innerEvents() {
        final List<XMLEvent> events = new ArrayList<>();
        events.add(this.pathPointArrayStartElement);
        this.pathPoints.forEach(p -> events.addAll(p.getEvents()));
        events.add(this.pathPointArrayEndElement);
        return events;
    }

    @Override
    public void updateInnerEventsWith(final List<XMLEvent> events) {
    }

    List<PathPoint> pathPoints() {
        return pathPoints;
    }

    @Override
    public EndElement endElement() {
        return this.endElement;
    }

    @Override
    public void apply(final FontMappings fontMappings) {
    }

    @Override
    public List<XMLEvent> getEvents() {
        final List<XMLEvent> events = new ArrayList<>();
        events.add(this.startElement);
        events.addAll(innerEvents());
        events.add(this.endElement);
        return events;
    }

    @Override
    public QName getName() {
        return this.startElement.getName();
    }

    static class Builder implements net.sf.okapi.filters.idml.Builder<GeometryPath> {
        private StartElement startElement;
        private StartElement pathPointArrayStartElement;
        private List<PathPoint> pathPoints = new ArrayList<>();
        private EndElement pathPointArrayEndElement;
        private EndElement endElement;

        Builder setStartElement(StartElement startElement) {
            this.startElement = startElement;
            return this;
        }

        Builder setPathPointArrayStartElement(StartElement pathPointArrayStartElement) {
            this.pathPointArrayStartElement = pathPointArrayStartElement;
            return this;
        }

        Builder addPathPoint(PathPoint pathPoint) {
            pathPoints.add(pathPoint);
            return this;
        }

        Builder setPathPointArrayEndElement(EndElement pathPointArrayEndElement) {
            this.pathPointArrayEndElement = pathPointArrayEndElement;
            return this;
        }

        Builder setEndElement(EndElement endElement) {
            this.endElement = endElement;
            return this;
        }

        @Override
        public GeometryPath build() {
            return new GeometryPath(startElement, pathPointArrayStartElement, pathPoints, pathPointArrayEndElement, endElement);
        }
    }
}
