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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

interface StyleDefinition extends MarkupRange {
    void from(final XMLEventReader eventReader) throws XMLStreamException;

    interface Style extends StyleDefinition {
        Iterator<Element> innerElements();

        class Default implements StyleDefinition.Style {
            private final StartElement startElement;
            private final List<Element> innerElements;
            private final XMLEventFactory eventFactory;

            private Properties properties;
            private EndElement endElement;

            Default(
                final StartElement startElement,
                final List<Element> innerElements,
                final XMLEventFactory eventFactory
            ) {
                this.startElement = startElement;
                this.properties = new Properties.Empty(eventFactory);
                this.innerElements = innerElements;
                this.eventFactory = eventFactory;
            }

            @Override
            public Iterator<Element> innerElements() {
                return this.innerElements.iterator();
            }

            @Override
            public void from(final XMLEventReader eventReader) throws XMLStreamException {
                while (eventReader.hasNext()) {
                    final XMLEvent event = eventReader.nextEvent();
                    if (event.isEndElement() && event.asEndElement().getName().equals(this.startElement.getName())) {
                        this.endElement = event.asEndElement();
                        return;
                    }
                    if (event.isStartElement() && Properties.NAME.equals(event.asStartElement().getName())) {
                        this.properties = new PropertiesParser(event.asStartElement(), eventReader, this.eventFactory).parse();
                        continue;
                    }
                    if (event.isStartElement()) {
                        this.innerElements.add(
                            new ElementParser(
                                event.asStartElement(),
                                eventReader,
                                eventFactory
                            )
                            .parse(new Element.Builder())
                        );
                    }
                }
            }

            @Override
            public void apply(final FontMappings fontMappings) {
                this.properties.apply(fontMappings);
                this.innerElements.forEach(e -> e.apply(fontMappings));
            }

            @Override
            public List<XMLEvent> getEvents() {
                final List<XMLEvent> events = new ArrayList<>();
                events.add(this.startElement);
                events.addAll(this.properties.getEvents());
                this.innerElements.forEach(e -> events.addAll(e.getEvents()));
                events.add(this.endElement);
                return events;
            }
        }
    }

    interface Group extends StyleDefinition {
        Iterator<StyleDefinition.Style> styles();
        Iterator<StyleDefinition.Group> styleGroups();

        class Default implements Group {
            private final StartElement startElement;
            private final String styleName;
            private final String styleGroupName;
            private final List<StyleDefinition.Style> styles;
            private final List<StyleDefinition.Group> styleGroups;
            private final XMLEventFactory eventFactory;

            private Properties properties;
            private EndElement endElement;

            Default(
                final StartElement startElement,
                final String styleName,
                final String styleGroupName,
                final List<StyleDefinition.Style> styles,
                final List<StyleDefinition.Group> styleGroups,
                final XMLEventFactory eventFactory
            ) {
                this.startElement = startElement;
                this.properties = new Properties.Empty(eventFactory);
                this.styleName = styleName;
                this.styleGroupName = styleGroupName;
                this.styles = styles;
                this.styleGroups = styleGroups;
                this.eventFactory = eventFactory;
            }

            @Override
            public Iterator<StyleDefinition.Style> styles() {
                return this.styles.iterator();
            }

            @Override
            public Iterator<StyleDefinition.Group> styleGroups() {
                return this.styleGroups.iterator();
            }

            @Override
            public void from(final XMLEventReader eventReader) throws XMLStreamException {
                while (eventReader.hasNext()) {
                    final XMLEvent event = eventReader.nextEvent();
                    if (event.isEndElement() && event.asEndElement().getName().equals(this.startElement.getName())) {
                        this.endElement = event.asEndElement();
                        return;
                    }
                    if (event.isStartElement() && Properties.NAME.equals(event.asStartElement().getName())) {
                        this.properties = new PropertiesParser(event.asStartElement(), eventReader, this.eventFactory).parse();
                        continue;
                    }
                    if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(this.styleName)) {
                        final StyleDefinition.Style style = new StyleDefinition.Style.Default(
                            event.asStartElement(),
                            new LinkedList<>(),
                            this.eventFactory
                        );
                        style.from(eventReader);
                        this.styles.add(style);
                        continue;
                    }
                    if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals(this.styleGroupName)) {
                        final StyleDefinition.Group group = new Group.Default(
                            event.asStartElement(),
                            this.styleName,
                            this.styleGroupName,
                            new LinkedList<>(),
                            new LinkedList<>(),
                            this.eventFactory
                        );
                        group.from(eventReader);
                        this.styleGroups.add(group);
                    }
                }
            }

            @Override
            public void apply(final FontMappings fontMappings) {
                this.properties.apply(fontMappings);
                this.styles.forEach(s -> s.apply(fontMappings));
                this.styleGroups.forEach(sg -> sg.apply(fontMappings));
            }

            @Override
            public List<XMLEvent> getEvents() {
                final List<XMLEvent> events = new LinkedList<>();
                events.add(this.startElement);
                events.addAll(this.properties.getEvents());
                this.styles.forEach(s -> events.addAll(s.getEvents()));
                this.styleGroups.forEach(sg -> events.addAll(sg.getEvents()));
                events.add(this.endElement);
                return events;
            }
        }
    }
}
