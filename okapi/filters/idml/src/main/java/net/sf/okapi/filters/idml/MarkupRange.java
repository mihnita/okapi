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
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.XMLEvent;
import java.util.List;

import static java.util.Collections.singletonList;

interface MarkupRange extends Eventive {
    void apply(final FontMappings fontMappings);

    class Default implements MarkupRange {
        private final List<XMLEvent> events;

        Default(final List<XMLEvent> events) {
            this.events = events;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.events;
        }
    }

    class Start implements MarkupRange, Nameable {
        private static final int START_ELEMENT_INDEX = 0;

        private final Default defaultRange;

        Start(final javax.xml.stream.events.StartDocument startDocument) {
            this(singletonList(startDocument));
        }

        Start(final javax.xml.stream.events.StartElement startElement) {
            this(singletonList(startElement));
        }

        Start(final List<XMLEvent> events) {
            this(new Default(events));
        }

        Start(final Default range) {
            this.defaultRange = range;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.defaultRange.getEvents();
        }

        @Override
        public QName getName() {
            return getEvents().get(START_ELEMENT_INDEX).asStartElement().getName();
        }
    }

    class End implements MarkupRange, Nameable {
        private static final int END_ELEMENT_INDEX = 0;

        private final Default defaultRange;

        End(final javax.xml.stream.events.EndDocument endDocument) {
            this(singletonList(endDocument));
        }

        End(EndElement endElement) {
            this(singletonList(endElement));
        }

        End(final List<XMLEvent> events) {
            this(new Default(events));
        }

        End(final Default range) {
            this.defaultRange = range;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.defaultRange.getEvents();
        }

        @Override
        public QName getName() {
            return getEvents().get(END_ELEMENT_INDEX).asEndElement().getName();
        }
    }
}
