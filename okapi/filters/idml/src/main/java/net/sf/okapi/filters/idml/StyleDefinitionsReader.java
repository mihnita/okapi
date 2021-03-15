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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedList;
import java.util.List;

interface StyleDefinitionsReader {
    StartDocument startDocument() throws XMLStreamException;
    StartElement startElement() throws XMLStreamException;
    StyleDefinition.Group rootCharacterStyleGroup() throws XMLStreamException;
    StyleDefinition.Group rootParagraphStyleGroup() throws XMLStreamException;
    List<StyleDefinition.Style> tablesOfContents() throws XMLStreamException;
    StyleDefinition.Group rootCellStyleGroup() throws  XMLStreamException;
    StyleDefinition.Group rootTableStyleGroup() throws XMLStreamException;
    StyleDefinition.Group rootObjectStyleGroup() throws XMLStreamException;
    List<StyleDefinition.Style> trapPresets() throws XMLStreamException;
    EndElement endElement() throws XMLStreamException;
    EndDocument endDocument() throws  XMLStreamException;

    class Default implements StyleDefinitionsReader {
        private static final String UNEXPECTED_STRUCTURE = "Unexpected styles structure: %s";
        private static final String STYLES = "Styles";
        private static final String CHARACTER_STYLE = "CharacterStyle";
        private static final String CHARACTER_STYLE_GROUP = CHARACTER_STYLE.concat("Group");
        private static final String ROOT_CHARACTER_STYLE_GROUP = "Root".concat(CHARACTER_STYLE_GROUP);
        private static final String PARAGRAPH_STYLE = "ParagraphStyle";
        private static final String PARAGRAPH_STYLE_GROUP = PARAGRAPH_STYLE.concat("Group");
        private static final String ROOT_PARAGRAPH_STYLE_GROUP = "Root".concat(PARAGRAPH_STYLE_GROUP);
        private static final String TABLE_OF_CONTENT_STYLE = "TOCStyle";
        private static final String TABLE_OF_CONTENT_STYLE_ENTRY = TABLE_OF_CONTENT_STYLE.concat("Entry");
        private static final String CELL_STYLE = "CellStyle";
        private static final String CELL_STYLE_GROUP = CELL_STYLE.concat("Group");
        private static final String ROOT_CELL_STYLE_GROUP = "Root".concat(CELL_STYLE_GROUP);
        private static final String TABLE_STYLE = "TableStyle";
        private static final String TABLE_STYLE_GROUP = TABLE_STYLE.concat("Group");
        private static final String ROOT_TABLE_STYLE_GROUP = "Root".concat(TABLE_STYLE_GROUP);
        private static final String OBJECT_STYLE = "ObjectStyle";
        private static final String OBJECT_STYLE_GROUP = OBJECT_STYLE.concat("Group");
        private static final String ROOT_OBJECT_STYLE_GROUP = "Root".concat(OBJECT_STYLE_GROUP);
        private static final String TRAP_PRESET = "TrapPreset";

        private final XMLEventReader eventReader;
        private final XMLEventFactory eventFactory;

        private StartElement rootCellStyleGroupStartElement;
        private EndElement endElement;

        Default(final XMLEventReader eventReader, final XMLEventFactory eventFactory) {
            this.eventReader = eventReader;
            this.eventFactory = eventFactory;
        }

        @Override
        public StartDocument startDocument() throws XMLStreamException {
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartDocument()) {
                    return (StartDocument) event;
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the start document event is absent")
            );
        }

        @Override
        public StartElement startElement() throws XMLStreamException {
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && STYLES.equals(event.asStartElement().getName().getLocalPart())) {
                    return event.asStartElement();
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the start element event is absent")
            );
        }

        @Override
        public StyleDefinition.Group rootCharacterStyleGroup() throws XMLStreamException {
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && ROOT_CHARACTER_STYLE_GROUP.equals(event.asStartElement().getName().getLocalPart())) {
                    final StyleDefinition.Group group = new StyleDefinition.Group.Default(
                        event.asStartElement(),
                        CHARACTER_STYLE,
                        CHARACTER_STYLE_GROUP,
                        new LinkedList<>(),
                        new LinkedList<>(),
                        this.eventFactory
                    );
                    group.from(this.eventReader);
                    return group;
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the root character style group is absent")
            );
        }

        @Override
        public StyleDefinition.Group rootParagraphStyleGroup() throws XMLStreamException {
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && ROOT_PARAGRAPH_STYLE_GROUP.equals(event.asStartElement().getName().getLocalPart())) {
                    final StyleDefinition.Group group = new StyleDefinition.Group.Default(
                        event.asStartElement(),
                        PARAGRAPH_STYLE,
                        PARAGRAPH_STYLE_GROUP,
                        new LinkedList<>(),
                        new LinkedList<>(),
                        this.eventFactory
                    );
                    group.from(this.eventReader);
                    return group;
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the root paragraph style group is absent")
            );
        }

        @Override
        public List<StyleDefinition.Style> tablesOfContents() throws XMLStreamException {
            final List<StyleDefinition.Style> tablesOfContents = new LinkedList<>();
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && TABLE_OF_CONTENT_STYLE.equals(event.asStartElement().getName().getLocalPart())) {
                    final StyleDefinition.Style group = new StyleDefinition.Style.Default(
                        event.asStartElement(),
                        new LinkedList<>(),
                        this.eventFactory
                    );
                    group.from(this.eventReader);
                    tablesOfContents.add(group);
                }
                if (event.isStartElement() && ROOT_CELL_STYLE_GROUP.equals(event.asStartElement().getName().getLocalPart())) {
                    this.rootCellStyleGroupStartElement = event.asStartElement();
                    break;
                }
            }
            return tablesOfContents;
        }

        @Override
        public StyleDefinition.Group rootCellStyleGroup() throws XMLStreamException {
            if (null != this.rootCellStyleGroupStartElement) {
                final StyleDefinition.Group group = new StyleDefinition.Group.Default(
                    this.rootCellStyleGroupStartElement,
                    CELL_STYLE,
                    CELL_STYLE_GROUP,
                    new LinkedList<>(),
                    new LinkedList<>(),
                    this.eventFactory
                );
                group.from(this.eventReader);
                return group;
            }
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && ROOT_CELL_STYLE_GROUP.equals(event.asStartElement().getName().getLocalPart())) {
                    final StyleDefinition.Group group = new StyleDefinition.Group.Default(
                        event.asStartElement(),
                        CELL_STYLE,
                        CELL_STYLE_GROUP,
                        new LinkedList<>(),
                        new LinkedList<>(),
                        this.eventFactory
                    );
                    group.from(this.eventReader);
                    return group;
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the root cell style group is absent")
            );
        }

        @Override
        public StyleDefinition.Group rootTableStyleGroup() throws XMLStreamException {
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && ROOT_TABLE_STYLE_GROUP.equals(event.asStartElement().getName().getLocalPart())) {
                    final StyleDefinition.Group group = new StyleDefinition.Group.Default(
                        event.asStartElement(),
                        TABLE_STYLE,
                        TABLE_STYLE_GROUP,
                        new LinkedList<>(),
                        new LinkedList<>(),
                        this.eventFactory
                    );
                    group.from(this.eventReader);
                    return group;
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the root table style group is absent")
            );
        }

        @Override
        public StyleDefinition.Group rootObjectStyleGroup() throws XMLStreamException {
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && ROOT_OBJECT_STYLE_GROUP.equals(event.asStartElement().getName().getLocalPart())) {
                    final StyleDefinition.Group group = new StyleDefinition.Group.Default(
                        event.asStartElement(),
                        OBJECT_STYLE,
                        OBJECT_STYLE_GROUP,
                        new LinkedList<>(),
                        new LinkedList<>(),
                        this.eventFactory
                    );
                    group.from(this.eventReader);
                    return group;
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the root object style group is absent")
            );
        }

        @Override
        public List<StyleDefinition.Style> trapPresets() throws XMLStreamException {
            final List<StyleDefinition.Style> trapPresets = new LinkedList<>();
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isStartElement() && TRAP_PRESET.equals(event.asStartElement().getName().getLocalPart())) {
                    final StyleDefinition.Style style = new StyleDefinition.Style.Default(
                        event.asStartElement(),
                        new LinkedList<>(),
                        this.eventFactory
                    );
                    style.from(this.eventReader);
                    trapPresets.add(style);
                }
                if (event.isEndElement() && STYLES.equals(event.asEndElement().getName().getLocalPart())) {
                    this.endElement = event.asEndElement();
                    break;
                }
            }
            return trapPresets;
        }

        @Override
        public EndElement endElement() throws XMLStreamException {
            if (null != this.endElement) {
                return this.endElement;
            }
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isEndElement() && STYLES.equals(event.asEndElement().getName().getLocalPart())) {
                    return event.asEndElement();
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the end element event is absent")
            );
        }

        @Override
        public EndDocument endDocument() throws XMLStreamException {
            while (this.eventReader.hasNext()) {
                final XMLEvent event = this.eventReader.nextEvent();
                if (event.isEndDocument()) {
                    return (EndDocument) event;
                }
            }
            throw new IllegalStateException(
                String.format(UNEXPECTED_STRUCTURE, "the end document event is absent")
            );
        }
    }

    class Cached implements StyleDefinitionsReader {
        private final Default defaultReader;

        private StartDocument startDocument;
        private StartElement startElement;
        private StyleDefinition.Group rootCharacterStyleGroup;
        private StyleDefinition.Group rootParagraphStyleGroup;
        private List<StyleDefinition.Style> tablesOfContents;
        private StyleDefinition.Group rootCellStyleGroup;
        private StyleDefinition.Group rootTableStyleGroup;
        private StyleDefinition.Group rootObjectStyleGroup;
        private List<StyleDefinition.Style> trapPresets;
        private EndElement endElement;
        private EndDocument endDocument;

        Cached(final Default defaultReader) {
            this.defaultReader = defaultReader;
        }

        @Override
        public StartDocument startDocument() throws XMLStreamException {
            if (null == this.startDocument) {
                this.startDocument = this.defaultReader.startDocument();
            }
            return this.startDocument;
        }

        @Override
        public StartElement startElement() throws XMLStreamException {
            if (null == this.startDocument) {
                startDocument();
            }
            if (null == this.startElement) {
                this.startElement = this.defaultReader.startElement();
            }
            return this.startElement;
        }

        @Override
        public StyleDefinition.Group rootCharacterStyleGroup() throws XMLStreamException {
            if (null == this.startElement) {
                startElement();
            }
            if (null == this.rootCharacterStyleGroup) {
                this.rootCharacterStyleGroup = this.defaultReader.rootCharacterStyleGroup();
            }
            return this.rootCharacterStyleGroup;
        }

        @Override
        public StyleDefinition.Group rootParagraphStyleGroup() throws XMLStreamException {
            if (null == this.rootCharacterStyleGroup) {
                rootCharacterStyleGroup();
            }
            if (null == this.rootParagraphStyleGroup) {
                this.rootParagraphStyleGroup = this.defaultReader.rootParagraphStyleGroup();
            }
            return this.rootParagraphStyleGroup;
        }

        @Override
        public List<StyleDefinition.Style> tablesOfContents() throws XMLStreamException {
            if (null == this.rootParagraphStyleGroup) {
                rootParagraphStyleGroup();
            }
            if (null == this.tablesOfContents) {
                this.tablesOfContents = this.defaultReader.tablesOfContents();
            }
            return this.tablesOfContents;
        }

        @Override
        public StyleDefinition.Group rootCellStyleGroup() throws XMLStreamException {
            if (null == this.tablesOfContents) {
                tablesOfContents();
            }
            if (null == this.rootCellStyleGroup) {
                this.rootCellStyleGroup = this.defaultReader.rootCellStyleGroup();
            }
            return this.rootCellStyleGroup;
        }

        @Override
        public StyleDefinition.Group rootTableStyleGroup() throws XMLStreamException {
            if (null == this.rootCellStyleGroup) {
                rootCellStyleGroup();
            }
            if (null == this.rootTableStyleGroup) {
                this.rootTableStyleGroup = this.defaultReader.rootTableStyleGroup();
            }
            return this.rootTableStyleGroup;
        }

        @Override
        public StyleDefinition.Group rootObjectStyleGroup() throws XMLStreamException {
            if (null == this.rootTableStyleGroup) {
                rootTableStyleGroup();
            }
            if (null == this.rootObjectStyleGroup) {
                this.rootObjectStyleGroup = this.defaultReader.rootObjectStyleGroup();
            }
            return this.rootObjectStyleGroup;
        }

        @Override
        public List<StyleDefinition.Style> trapPresets() throws XMLStreamException {
            if (null == this.rootObjectStyleGroup) {
                rootObjectStyleGroup();
            }
            if (null == this.trapPresets) {
                this.trapPresets = this.defaultReader.trapPresets();
            }
            return this.trapPresets;
        }

        @Override
        public EndElement endElement() throws XMLStreamException {
            if (null == this.trapPresets) {
                trapPresets();
            }
            if (null == this.endElement) {
                this.endElement = this.defaultReader.endElement();
            }
            return this.endElement;
        }

        @Override
        public EndDocument endDocument() throws XMLStreamException {
            if (null == this.endElement) {
                endElement();
            }
            if (null == this.endDocument) {
                this.endDocument = this.defaultReader.endDocument();
            }
            return this.endDocument;
        }
    }
}
