/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_ALIGNMENT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_PRESENTATION;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_SHEET_VIEW;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_TABLE;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_TEXT_BODY;

/**
 * Provides a markup component.
 */
interface MarkupComponent extends XMLEvents {

    void apply(FontMappings fontMappings);

    static boolean isSheetViewStart(MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && LOCAL_SHEET_VIEW.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isAlignmentEmptyElement(MarkupComponent markupComponent) {
        return markupComponent instanceof EmptyElement
                && LOCAL_ALIGNMENT.equals(((EmptyElement) markupComponent).getName().getLocalPart());
    }

    static boolean isPresentationStart(MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && LOCAL_PRESENTATION.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isTableStart(MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && LOCAL_TABLE.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isTextBodyStart(MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && LOCAL_TEXT_BODY.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isParagraphStart(MarkupComponent markupComponent) {
        return markupComponent instanceof ParagraphStart;
    }

    static boolean isWordStylesStart(final MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && WordStyleDefinitions.STYLES.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isWordStylesEnd(final MarkupComponent markupComponent) {
        return markupComponent instanceof End
                && WordStyleDefinitions.STYLES.equals(((End) markupComponent).getName().getLocalPart());
    }

    static boolean isWordDocumentDefaultsStart(final MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && WordStyleDefinitions.DOC_DEFAULTS.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isWordDocumentDefaultsEnd(final MarkupComponent markupComponent) {
        return markupComponent instanceof End
                && WordStyleDefinitions.DOC_DEFAULTS.equals(((End) markupComponent).getName().getLocalPart());
    }

    static boolean isWordStyleStart(final MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && WordStyleDefinitions.STYLE.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isWordStyleEnd(final MarkupComponent markupComponent) {
        return markupComponent instanceof End
                && WordStyleDefinitions.STYLE.equals(((End) markupComponent).getName().getLocalPart());
    }

    static boolean isWordParagraphPropertiesDefaultStart(final MarkupComponent markupComponent) {
        return markupComponent instanceof ParagraphStart
                && WordStyleDefinition.DocumentDefaults.PPR_DEFAULT.equals(((ParagraphStart) markupComponent).getName().getLocalPart());
    }

    static boolean isWordRunPropertiesDefaultStart(final MarkupComponent markupComponent) {
        return markupComponent instanceof Start
                && WordStyleDefinition.DocumentDefaults.RPR_DEFAULT.equals(((Start) markupComponent).getName().getLocalPart());
    }

    static boolean isWordRunPropertiesDefaultEnd(final MarkupComponent markupComponent) {
        return markupComponent instanceof End
                && WordStyleDefinition.DocumentDefaults.RPR_DEFAULT.equals(((End) markupComponent).getName().getLocalPart());
    }

    static boolean isParagraphBlockProperties(final MarkupComponent markupComponent) {
        return markupComponent instanceof ParagraphBlockProperties;
    }

    static boolean isRunProperties(final MarkupComponent markupComponent) {
        return markupComponent instanceof RunProperties;
    }

    static boolean isTableBlockProperties(final MarkupComponent markupComponent) {
        return markupComponent instanceof BlockProperties && BlockProperties.TBL_PR.equals(
            ((BlockProperties) markupComponent).startElement().getName().getLocalPart()
        );
    }

    /**
     * Provides a start markup component.
     */
    class Start implements MarkupComponent, Nameable {
        private XMLEventFactory eventFactory;
        private StartElement startElement;
        private List<Attribute> attributes = new ArrayList<>();

        Start(XMLEventFactory eventFactory, StartElement startElement) {
            this.eventFactory = eventFactory;
            this.startElement = startElement;

            Iterator iterator = startElement.getAttributes();

            while (iterator.hasNext()) {
                attributes.add((Attribute) iterator.next());
            }
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return Collections.singletonList(eventFactory.createStartElement(startElement.getName(), getAttributes().iterator(), startElement.getNamespaces()));
        }

        @Override
        public QName getName() {
            return startElement.getName();
        }

        List<Attribute> getAttributes() {
            return attributes;
        }

        boolean containsAttributeWithAnyOfValues(final String name, final Set<String> values) {
            return this.attributes.stream()
                .anyMatch(a -> a.getName().getLocalPart().equals(name)
                                && values.contains(a.getValue()));
        }

        @Override
        public String toString() {
            return "<".concat(this.startElement.getName().getPrefix())
                    .concat(":")
                    .concat(this.startElement.getName().getLocalPart())
                    .concat(">");
        }
    }

    /**
     * Provides a paragraph start markup component.
     * {@link StyleDefinitions} context is available.
     */
    final class ParagraphStart implements MarkupComponent, Nameable {
        private final Start start;
        private final StyleDefinitions styleDefinitions;

        ParagraphStart(
            final XMLEventFactory eventFactory,
            final StyleDefinitions styleDefinitions,
            final StartElement startElement
        ) {
            this(
                new Start(eventFactory, startElement),
                styleDefinitions
            );
        }

        ParagraphStart(final Start start, final StyleDefinitions styleDefinitions) {
            this.start = start;
            this.styleDefinitions = styleDefinitions;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.start.getEvents();
        }

        @Override
        public QName getName() {
            return this.start.getName();
        }

        List<Attribute> getAttributes() {
            return this.start.getAttributes();
        }

        boolean containsAttributeWithAnyOfValues(final String name, final Set<String> values) {
            return this.start.containsAttributeWithAnyOfValues(name, values);
        }

        StyleDefinitions styleDefinitions() {
            return this.styleDefinitions;
        }

        @Override
        public String toString() {
            return this.start.toString();
        }
    }

    /**
     * Provides an end markup component.
     */
    class End implements MarkupComponent, Nameable {
        private EndElement endElement;

        End(EndElement endElement) {
            this.endElement = endElement;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return Collections.singletonList((XMLEvent) endElement);
        }

        @Override
        public QName getName() {
            return this.endElement.getName();
        }

        @Override
        public String toString() {
            return "</".concat(this.endElement.getName().getPrefix())
                    .concat(":")
                    .concat(this.endElement.getName().getLocalPart())
                    .concat(">");
        }
    }

    /**
     * Provides an empty element markup component.
     */
    class EmptyElement implements MarkupComponent, Nameable {
        private static final int EMPTY_ELEMENT_EVENTS_SIZE = 2;

        private XMLEventFactory eventFactory;
        private StartElement startElement;
        private EndElement endElement;
        private List<Attribute> attributes = new ArrayList<>();

        EmptyElement(XMLEventFactory eventFactory, StartElement startElement, EndElement endElement) {
            this.eventFactory = eventFactory;
            this.startElement = startElement;
            this.endElement = endElement;

            Iterator iterator = startElement.getAttributes();

            while (iterator.hasNext()) {
                attributes.add((Attribute) iterator.next());
            }
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            List<XMLEvent> events = new ArrayList<>(EMPTY_ELEMENT_EVENTS_SIZE);

            events.add(eventFactory.createStartElement(startElement.getName(), getAttributes().iterator(), startElement.getNamespaces()));
            events.add(endElement);

            return events;
        }

        @Override
        public QName getName() {
            return startElement.getName();
        }

        List<Attribute> getAttributes() {
            return attributes;
        }
    }

    /**
     * Provides a general markup component.
     */
    final class General implements MarkupComponent {
        private static final Set<Character> WHITESPACES = new HashSet<>(Arrays.asList(' ', '\t', '\r', '\n'));

        private List<XMLEvent> events;

        General(List<XMLEvent> events) {
            this.events = events;
        }

        boolean eventsAreWhitespaces() {
            if (this.events.stream()
                .allMatch(e ->
                    XMLStreamConstants.COMMENT == e.getEventType()
                        || XMLStreamConstants.PROCESSING_INSTRUCTION == e.getEventType()
                        || e.isCharacters()
                )
            ) {
                return this.events.stream()
                    .filter(e -> e.isCharacters())
                    .map(e -> e.asCharacters().getData())
                    .flatMap(s -> s.chars().mapToObj(c -> (char) c))
                    .allMatch(c -> WHITESPACES.contains(c));
            }
            return false;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return events;
        }
    }
}
