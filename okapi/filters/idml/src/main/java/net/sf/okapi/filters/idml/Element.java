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
package net.sf.okapi.filters.idml;

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

interface Element extends MarkupRange, Nameable {
    StartElement startElement();
    List<XMLEvent> innerEvents();
    void updateInnerEventsWith(final List<XMLEvent> events);
    EndElement endElement();

    class Default implements Element {
        private final StartElement startElement;
        private final List<XMLEvent> innerEvents;
        private final EndElement endElement;
        private final XMLEventFactory eventFactory;

        Default(
            final StartElement startElement,
            final List<XMLEvent> innerEvents,
            final EndElement endElement,
            final XMLEventFactory eventFactory
        ) {
            this.startElement = startElement;
            this.innerEvents = innerEvents;
            this.endElement = endElement;
            this.eventFactory = eventFactory;
        }

        @Override
        public StartElement startElement() {
            return this.startElement;
        }

        @Override
        public List<XMLEvent> innerEvents() {
            return this.innerEvents;
        }

        @Override
        public void updateInnerEventsWith(final List<XMLEvent> events) {
            final ListIterator<XMLEvent> iterator = this.innerEvents.listIterator();
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
            events.forEach(e -> iterator.add(e));
        }

        @Override
        public EndElement endElement() {
            return this.endElement;
        }

        XMLEventFactory eventFactory() {
            return eventFactory;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            final List<XMLEvent> events = new ArrayList<>(this.innerEvents.size() + 2);
            events.add(this.startElement);
            events.addAll(this.innerEvents);
            events.add(this.endElement);
            return events;
        }

        @Override
        public QName getName() {
            return this.startElement.getName();
        }
    }

    class Builder implements net.sf.okapi.filters.idml.Builder<Element> {
        protected StartElement startElement;
        protected List<XMLEvent> innerEvents = new ArrayList<>();
        protected EndElement endElement;
        protected XMLEventFactory eventFactory;

        Builder setStartElement(StartElement startElement) {
            this.startElement = startElement;
            return this;
        }

        Builder addInnerEvent(XMLEvent innerEvent) {
            innerEvents.add(innerEvent);
            return this;
        }

        Builder addInnerEvents(List<XMLEvent> innerEvents) {
            this.innerEvents.addAll(innerEvents);
            return this;
        }

        Builder setEndElement(EndElement endElement) {
            this.endElement = endElement;
            return this;
        }

        void setEventFactory(XMLEventFactory eventFactory) {
            this.eventFactory = eventFactory;
        }

        @Override
        public Element build() {
            return new Element.Default(startElement, innerEvents, endElement, eventFactory);
        }
    }
}
