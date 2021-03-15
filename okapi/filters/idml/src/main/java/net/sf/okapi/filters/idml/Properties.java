/*
 * =============================================================================
 *   Copyright (C) 2010-2017 by the Okapi Framework contributors
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

interface Properties extends Element {
    QName NAME = new QName("Properties");

    List<Property> properties();

    class Default implements Properties {
        private static final String APPLIED_FONT = "AppliedFont";

        private final StartElement startElement;
        private final List<Property> properties;
        private final EndElement endElement;

        Default(StartElement startElement, List<Property> properties, EndElement endElement) {
            this.startElement = startElement;
            this.properties = properties;
            this.endElement = endElement;
        }

        @Override
        public StartElement startElement() {
            return this.startElement;
        }

        @Override
        public List<XMLEvent> innerEvents() {
            return this.properties.stream()
                .map(p -> p.getEvents())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        }

        @Override
        public void updateInnerEventsWith(final List<XMLEvent> events) {
        }

        @Override
        public EndElement endElement() {
            return this.endElement;
        }

        @Override
        public List<Property> properties() {
            return this.properties;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
            this.properties.stream()
                .filter(p -> p.getName().getLocalPart().equals(Properties.Default.APPLIED_FONT))
                .forEach(p -> p.apply(fontMappings));
        }

        @Override
        public List<XMLEvent> getEvents() {
            List<XMLEvent> events = new ArrayList<>();
            events.add(startElement);
            events.addAll(innerEvents());
            events.add(endElement);
            return events;
        }

        @Override
        public QName getName() {
            return this.startElement.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Properties)) return false;
            Properties that = (Properties) o;
            return Objects.equals(properties(), that.properties());
        }

        @Override
        public int hashCode() {
            return Objects.hash(properties());
        }
    }

    class Empty implements Properties {
        private final XMLEventFactory eventFactory;
        private StartElement startElement;
        private EndElement endElement;

        Empty(final XMLEventFactory eventFactory) {
            this.eventFactory = eventFactory;
        }

        @Override
        public StartElement startElement() {
            if (null == this.startElement) {
                this.startElement = this.eventFactory.createStartElement(Properties.NAME, null, null);
            }
            return this.startElement;
        }

        @Override
        public List<XMLEvent> innerEvents() {
            return Collections.emptyList();
        }

        @Override
        public void updateInnerEventsWith(final List<XMLEvent> events) {
        }

        @Override
        public EndElement endElement() {
            if (null == this.endElement) {
                this.endElement = this.eventFactory.createEndElement(Properties.NAME, null);
            }
            return this.endElement;
        }

        @Override
        public List<Property> properties() {
            return Collections.emptyList();
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return Collections.emptyList();
        }

        @Override
        public QName getName() {
            return Properties.NAME;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Properties)) return false;
            Properties that = (Properties) o;
            return Objects.equals(properties(), that.properties());
        }

        @Override
        public int hashCode() {
            return Objects.hash(properties());
        }
    }

    class Builder implements net.sf.okapi.filters.idml.Builder<Properties> {
        private StartElement startElement;
        private List<Property> properties = new ArrayList<>();
        private EndElement endElement;

        Builder setStartElement(StartElement startElement) {
            this.startElement = startElement;
            return this;
        }

        Builder addProperty(Property property) {
            properties.add(property);
            return this;
        }

        Builder addProperties(List<Property> properties) {
            this.properties.addAll(properties);
            return this;
        }

        Builder setEndElement(EndElement endElement) {
            this.endElement = endElement;
            return this;
        }

        @Override
        public Properties build() {
            return new Properties.Default(startElement, properties, endElement);
        }
    }
}
