/*
 * =============================================================================
 *   Copyright (C) 2010-2019 by the Okapi Framework contributors
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
package net.sf.okapi.filters.openxml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;

interface PowerpointStyleDefinition extends StyleDefinition {
    String DEF_PPR = "defPPr";
    Set<String> PARAGRAPH_LEVELS = new HashSet<>(
        Arrays.asList(
            "lvl1pPr",
            "lvl2pPr",
            "lvl3pPr",
            "lvl4pPr",
            "lvl5pPr",
            "lvl6pPr",
            "lvl7pPr",
            "lvl8pPr",
            "lvl9pPr"
        )
    );
    String EMPTY = "";

    String id();
    PowerpointStyleDefinition mergedWith(final PowerpointStyleDefinition other);

    final class ParagraphDefault implements PowerpointStyleDefinition {
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final StartElement startElement;

        private ParagraphBlockProperties paragraphProperties;
        private RunProperties defaultRunProperties;

        ParagraphDefault(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final String namespaceUri,
            final String prefix
        ) {
            this(
                conditionalParameters,
                eventFactory,
                eventFactory.createStartElement(
                    prefix,
                    namespaceUri,
                    DEF_PPR
                )
            );
        }

        ParagraphDefault(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final StartElement startElement
        ) {
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.startElement = startElement;
        }

        @Override
        public String id() {
            return EMPTY;
        }

        @Override
        public ParagraphBlockProperties paragraphProperties() {
            if (null == this.paragraphProperties) {
                this.paragraphProperties = new ParagraphBlockProperties.Drawing(
                    new BlockProperties.Default(
                        this.eventFactory,
                        this.startElement.getName().getPrefix(),
                        this.startElement.getName().getNamespaceURI(),
                        ParagraphBlockProperties.PPR
                    ),
                    this.conditionalParameters,
                    this.eventFactory,
                    new StrippableAttributes.DrawingRunProperties(
                        this.conditionalParameters,
                        this.eventFactory
                    ),
                    new SchemaDefinition.TextParagraphProperties(
                        new QName(
                            this.startElement.getName().getNamespaceURI(),
                            ParagraphBlockProperties.PPR,
                            this.startElement.getName().getPrefix()
                        )
                    )
                );
            }
            return this.paragraphProperties;
        }

        @Override
        public PowerpointStyleDefinition mergedWith(final PowerpointStyleDefinition other) {
            if (!(other instanceof ParagraphDefault)) {
                throw new IllegalArgumentException("The provided argument is illegal: ".concat(other.getClass().getSimpleName()));
            }
            final ParagraphDefault that = (ParagraphDefault) other;
            final ParagraphDefault merged = new ParagraphDefault(
                this.conditionalParameters,
                this.eventFactory,
                this.startElement
            );
            merged.paragraphProperties = paragraphProperties().mergedWith(that.paragraphProperties());
            merged.defaultRunProperties = runProperties().mergedWith(that.runProperties());
            return merged;
        }

        @Override
        public void readWith(XMLEventReader reader) throws XMLStreamException {
            final StrippableAttributes strippableAttributes = new StrippableAttributes.DrawingRunProperties(
                this.conditionalParameters,
                this.eventFactory
            );
            this.paragraphProperties = new MarkupComponentParser().parseParagraphBlockProperties(
                createStartElementContext(
                    this.startElement,
                    reader,
                    this.eventFactory,
                    this.conditionalParameters
                ),
                strippableAttributes,
                new SkippableElements.Empty()
            );
            final ListIterator<Property> iterator =
                this.paragraphProperties.properties().listIterator();
            while (iterator.hasNext()) {
                final Property blockProperty = iterator.next();
                if (RunProperties.DEF_RPR.equals(blockProperty.getName().getLocalPart())) {
                    this.defaultRunProperties = blockProperty.asRunProperties();
                    if (this.defaultRunProperties.properties().isEmpty()) {
                        iterator.remove();
                    } else {
                        // update as some attributes might be stripped
                        iterator.set(
                            new BlockProperty(
                                this.defaultRunProperties.getEvents(),
                                this.conditionalParameters,
                                this.eventFactory,
                                strippableAttributes
                            )
                        );
                    }
                }
            }
        }

        @Override
        public RunProperties runProperties() {
            if (null == this.defaultRunProperties) {
                this.defaultRunProperties = new RunProperties.Default(
                    this.eventFactory,
                    this.startElement.getName().getPrefix(),
                    this.startElement.getName().getNamespaceURI(),
                    RunProperties.DEF_RPR
                );
            }
            return this.defaultRunProperties;
        }

        @Override
        public Markup toMarkup() {
            return new Markup.General(
                Collections.singletonList(
                    paragraphProperties()
                )
            );
        }
    }

    final class ParagraphLevel implements PowerpointStyleDefinition {
        private static final int ID_BEGIN_INDEX = 3;
        private static final int ID_END_INDEX = 4;

        private final ParagraphDefault paragraphDefault;
        private String id;

        ParagraphLevel(final ParagraphDefault paragraphDefault) {
            this.paragraphDefault = paragraphDefault;
        }

        @Override
        public String id() {
            return this.id;
        }

        @Override
        public PowerpointStyleDefinition mergedWith(final PowerpointStyleDefinition other) {
            if (!(other instanceof ParagraphLevel)) {
                throw new IllegalArgumentException("The provided argument is illegal: ".concat(other.getClass().getSimpleName()));
            }
            final ParagraphLevel that = (ParagraphLevel) other;
            if (!id().equals(that.id())) {
                throw new IllegalArgumentException(("The provided paragraph level ID does not match with the available one: ")
                    .concat(that.id()).concat(" instead of ").concat(id()));
            }
            final ParagraphLevel merged = new ParagraphLevel(
                (ParagraphDefault) this.paragraphDefault.mergedWith(that.paragraphDefault)
            );
            merged.id = this.id;
            return merged;
        }

        @Override
        public void readWith(final XMLEventReader reader) throws XMLStreamException {
            this.id = readId();
            this.paragraphDefault.readWith(reader);
        }

        private String readId() {
            return this.paragraphDefault.startElement.getName().getLocalPart()
                .substring(ID_BEGIN_INDEX, ID_END_INDEX);
        }

        @Override
        public ParagraphBlockProperties paragraphProperties() {
            return this.paragraphDefault.paragraphProperties();
        }

        @Override
        public RunProperties runProperties() {
            return this.paragraphDefault.runProperties();
        }

        @Override
        public Markup toMarkup() {
            return this.paragraphDefault.toMarkup();
        }
    }
}
