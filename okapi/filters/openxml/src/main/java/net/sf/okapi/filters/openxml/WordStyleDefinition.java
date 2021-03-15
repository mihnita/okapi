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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.DEFAULT_BOOLEAN_ATTRIBUTE_FALSE_VALUE;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.WPML_VAL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.getAttributeValue;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.getBooleanAttributeValue;

interface WordStyleDefinition extends StyleDefinition {
    String UNEXPECTED_STRUCTURE = "Unexpected structure : ";
    String EMPTY = "";
    String RPR = "rPr";

    StyleType type();
    String id();
    boolean isDefault();
    String parentId();
    void parentId(final String parentId);
    String linkedId();
    void paragraphProperties(final ParagraphBlockProperties paragraphBlockProperties);
    void runProperties(final RunProperties runProperties);

    final class Empty implements WordStyleDefinition {
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final String namespaceUri;
        private final String name;
        private final String prefix;

        Empty(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final String namespaceUri,
            final String name,
            final String prefix
        ) {
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.namespaceUri = namespaceUri;
            this.name = name;
            this.prefix = prefix;
        }

        @Override
        public void readWith(XMLEventReader reader) {
        }

        @Override
        public String id() {
            return EMPTY;
        }

        @Override
        public StyleType type() {
            return StyleType.UNSUPPORTED;
        }

        @Override
        public boolean isDefault() {
            return false;
        }

        @Override
        public String parentId() {
            return null;
        }

        @Override
        public void parentId(final String parentId) {
        }

        @Override
        public String linkedId() {
            return null;
        }

        @Override
        public ParagraphBlockProperties paragraphProperties() {
            return new ParagraphBlockProperties.Word(
                new BlockProperties.Default(
                    this.eventFactory,
                    this.prefix,
                    this.namespaceUri,
                    this.name
                ),
                this.conditionalParameters,
                this.eventFactory,
                new StrippableAttributes.Default(this.eventFactory)
            );
        }

        @Override
        public void paragraphProperties(final ParagraphBlockProperties paragraphBlockProperties) {
        }

        @Override
        public RunProperties runProperties() {
            return new RunProperties.Default(
                this.eventFactory,
                this.prefix,
                this.namespaceUri,
                RunProperties.RPR
            );
        }

        @Override
        public void runProperties(final RunProperties runProperties) {
        }

        @Override
        public Markup toMarkup() {
            return new Markup.Empty();
        }
    }

    final class DocumentDefaults implements WordStyleDefinition {
        static final String PPR_DEFAULT = "pPrDefault";
        static final String RPR_DEFAULT = "rPrDefault";
        private static final String PPR = "pPr";

        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final StartElement startElement;
        private StartElement paragraphPropertiesDefaultStartElement;
        private ParagraphBlockProperties paragraphProperties;
        private EndElement paragraphPropertiesDefaultEndElement;
        private StartElement runPropertiesDefaultStartElement;
        private RunProperties runProperties;
        private EndElement runPropertiesDefaultEndElement;
        private EndElement endElement;

        DocumentDefaults(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final String prefix,
            final String namespaceUri,
            final String name
        ) {
            this(
                conditionalParameters,
                eventFactory,
                eventFactory.createStartElement(
                    prefix,
                    namespaceUri,
                    name
                )
            );
        }

        DocumentDefaults(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final StartElement startElement
        ) {
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.startElement = startElement;
        }

        @Override
        public void readWith(final XMLEventReader reader) throws XMLStreamException {
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement()) {
                    if (event.asEndElement().getName().equals(this.startElement.getName())) {
                        this.endElement = event.asEndElement();
                        return;
                    }
                    if (event.asEndElement().getName().getLocalPart().equals(PPR_DEFAULT)) {
                        this.paragraphPropertiesDefaultEndElement = event.asEndElement();
                    } else if (event.asEndElement().getName().getLocalPart().equals(RPR_DEFAULT)) {
                        this.runPropertiesDefaultEndElement = event.asEndElement();
                    }
                } else if (event.isStartElement()) {
                    switch (event.asStartElement().getName().getLocalPart()) {
                        case PPR_DEFAULT:
                            this.paragraphPropertiesDefaultStartElement = event.asStartElement();
                            break;
                        case RPR_DEFAULT:
                            this.runPropertiesDefaultStartElement = event.asStartElement();
                            break;
                        case PPR:
                            this.paragraphProperties = paragraphProperties(reader, event);
                            break;
                        case RPR:
                            this.runProperties = runProperties(reader, event);
                            break;
                    }
                }
            }
            throw new IllegalStateException(
                UNEXPECTED_STRUCTURE
                .concat(this.startElement.getName().getLocalPart())
                .concat(" end element is absent")
            );
        }

        private ParagraphBlockProperties paragraphProperties(XMLEventReader reader, XMLEvent event) throws XMLStreamException {
            return new MarkupComponentParser().parseParagraphBlockProperties(
                StartElementContextFactory.createStartElementContext(
                    event.asStartElement(),
                    null,
                    reader,
                    this.eventFactory,
                    this.conditionalParameters
                ),
                new StrippableAttributes.DrawingRunProperties(
                    this.conditionalParameters,
                    this.eventFactory
                ),
                new SkippableElements.Property(
                    new SkippableElements.Default(
                        SkippableElement.BlockProperty.BLOCK_PROPERTY_BIDI_VISUAL,
                        SkippableElement.RunProperty.RUN_PROPERTY_LANGUAGE
                    ),
                    this.conditionalParameters
                )
            );
        }

        private RunProperties runProperties(XMLEventReader reader, XMLEvent event) throws XMLStreamException {
            final StartElementContext startElementContext = StartElementContextFactory.createStartElementContext(
                event.asStartElement(),
                null,
                reader,
                this.eventFactory,
                this.conditionalParameters
            );
            return new RunPropertiesParser(
                startElementContext,
                new RunSkippableElements(startElementContext)
            )
            .parse();
        }

        @Override
        public String id() {
            throw new UnsupportedOperationException();
        }

        @Override
        public StyleType type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDefault() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String parentId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void parentId(final String parentId) {
        }

        @Override
        public String linkedId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ParagraphBlockProperties paragraphProperties() {
            if (null == this.paragraphProperties) {
                this.paragraphProperties = new ParagraphBlockProperties.Word(
                    new BlockProperties.Default(
                        this.eventFactory,
                        this.startElement.getName().getPrefix(),
                        this.startElement.getName().getNamespaceURI(),
                        PPR
                    ),
                    this.conditionalParameters,
                    this.eventFactory,
                    new StrippableAttributes.Default(this.eventFactory)
                );
            }
            return this.paragraphProperties;
        }

        @Override
        public void paragraphProperties(final ParagraphBlockProperties paragraphBlockProperties) {
            this.paragraphProperties = paragraphBlockProperties;
        }

        @Override
        public RunProperties runProperties() {
            if (null == this.runProperties) {
                this.runProperties = new RunProperties.Default(
                    this.eventFactory,
                    this.startElement.getName().getPrefix(),
                    this.startElement.getName().getNamespaceURI(),
                    RunProperties.RPR
                );
            }
            return this.runProperties;
        }

        @Override
        public void runProperties(final RunProperties runProperties) {
            this.runProperties = runProperties;
        }

        @Override
        public Markup toMarkup() {
            final Markup markup = new Markup.General(new LinkedList<>());
            if (null == this.endElement) {
                return markup;
            }
            markup.addComponent(new MarkupComponent.Start(this.eventFactory, this.startElement));
            if (null != this.paragraphPropertiesDefaultStartElement
                    && null != this.paragraphPropertiesDefaultEndElement) {
                // Ack! MS products need this element to be present, even if it is empty,
                // in order to properly assign some default paragraph styles correctly!
                markup.addComponent(
                    new MarkupComponent.ParagraphStart(
                        new MarkupComponent.Start(
                            this.eventFactory,
                            this.paragraphPropertiesDefaultStartElement
                        ),
                        new StyleDefinitions.Empty()
                    )
                );
                markup.addComponent(paragraphProperties());
                markup.addComponent(new MarkupComponent.End(this.paragraphPropertiesDefaultEndElement));
            }
            if (null != this.runPropertiesDefaultStartElement
                    && null != this.runProperties
                    && null != this.runPropertiesDefaultEndElement) {
                markup.addComponent(new MarkupComponent.Start(this.eventFactory, this.runPropertiesDefaultStartElement));
                markup.addComponent(this.runProperties);
                markup.addComponent(new MarkupComponent.End(this.runPropertiesDefaultEndElement));
            }
            markup.addComponent(new MarkupComponent.End(this.endElement));
            return markup;
        }
    }

    final class Latent implements WordStyleDefinition {
        private final XMLEventFactory eventFactory;
        private final StartElement startElement;
        private final List<XMLEvent> innerEvents;
        private EndElement endElement;

        Latent(
            final XMLEventFactory eventFactory,
            final String prefix,
            final String namespaceUri,
            final String name
        ) {
            this(
                eventFactory,
                eventFactory.createStartElement(
                    prefix,
                    namespaceUri,
                    name
                )
            );
        }

        Latent(final XMLEventFactory eventFactory, final StartElement startElement) {
            this(
                eventFactory,
                startElement,
                new LinkedList<>()
            );
        }

        Latent(
            final XMLEventFactory eventFactory,
            final StartElement startElement,
            final List<XMLEvent> innerEvents
        ) {
            this.eventFactory = eventFactory;
            this.startElement = startElement;
            this.innerEvents = innerEvents;
        }

        @Override
        public void readWith(final XMLEventReader reader) throws XMLStreamException {
            this.innerEvents.clear();
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement() && event.asEndElement().getName().equals(this.startElement.getName())) {
                    this.endElement = event.asEndElement();
                    return;
                }
                this.innerEvents.add(event);
            }
            throw new IllegalStateException(
                UNEXPECTED_STRUCTURE
                .concat(this.startElement.getName().getLocalPart())
                .concat(" end element is absent")
            );
        }

        @Override
        public String id() {
            throw new UnsupportedOperationException();
        }

        @Override
        public StyleType type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDefault() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String parentId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void parentId(final String parentId) {
        }

        @Override
        public String linkedId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ParagraphBlockProperties paragraphProperties() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void paragraphProperties(final ParagraphBlockProperties paragraphBlockProperties) {
        }

        @Override
        public RunProperties runProperties() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void runProperties(final RunProperties runProperties) {
        }

        @Override
        public Markup toMarkup() {
            if (this.innerEvents.isEmpty()) {
                return new Markup.Empty();
            }
            if (null == this.endElement) {
                this.endElement = this.eventFactory.createEndElement(
                    this.startElement.getName(),
                    this.startElement.getNamespaces()
                );
            }
            final Markup markup = new Markup.General(new LinkedList<>());
            markup.addComponent(new MarkupComponent.Start(this.eventFactory, this.startElement));
            markup.addComponent(new MarkupComponent.General(this.innerEvents));
            markup.addComponent(new MarkupComponent.End(this.endElement));
            return markup;
        }
    }

    final class General implements WordStyleDefinition {
        static final String TYPE = "type";
        static final String STYLE_ID = "styleId";
        static final String DEFAULT = "default";
        private static final String NAME = "name";
        private static final String BASED_ON = "basedOn";
        private static final String LINK = "link";
        private static final String VAL = "val";

        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final StartElement startElement;
        private final StrippableAttributes strippableAttributes;
        private final SkippableElements skippableElements;

        private StyleType type;
        private String id;
        private boolean shouldBeDefault;
        private String parentId;
        private String linkedId;
        private List<XMLEvent> eventsBeforeParagraphProperties;
        private ParagraphBlockProperties paragraphProperties;
        private List<XMLEvent> eventsBeforeRunProperties;
        private RunProperties runProperties;
        private List<XMLEvent> eventsBeforeTableProperties;
        private BlockProperties tableProperties;
        private List<XMLEvent> eventsBeforeEndElement;
        private EndElement endElement;

        private boolean attributesRead;

        General(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final StartElement startElement
        ) {
            this(
                conditionalParameters,
                eventFactory,
                startElement,
                new StrippableAttributes.DrawingRunProperties(
                    conditionalParameters,
                    eventFactory
                ),
                new SkippableElements.Property(
                    new SkippableElements.Default(
                        SkippableElement.BlockProperty.BLOCK_PROPERTY_BIDI_VISUAL,
                        SkippableElement.RunProperty.RUN_PROPERTY_LANGUAGE
                    ),
                    conditionalParameters
                )
            );
        }

        General(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final StartElement startElement,
            final StrippableAttributes strippableAttributes,
            final SkippableElements skippableElements
        ) {
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.startElement = startElement;
            this.strippableAttributes = strippableAttributes;
            this.skippableElements = skippableElements;
        }

        @Override
        public StyleType type() {
            if (!this.attributesRead) {
                readAttributes();
            }
            return this.type;
        }

        @Override
        public String id() {
            if (!this.attributesRead) {
                readAttributes();
            }
            return this.id;
        }

        @Override
        public boolean isDefault() {
            if (!this.attributesRead) {
                readAttributes();
            }
            return this.shouldBeDefault;
        }

        @Override
        public String parentId() {
            return this.parentId;
        }

        @Override
        public void parentId(final String parentId) {
            this.parentId = parentId;
        }

        @Override
        public String linkedId() {
            return this.linkedId;
        }

        @Override
        public void readWith(final XMLEventReader reader) throws XMLStreamException {
            readAttributes();
            readElements(reader);
        }

        private void readAttributes() {
            readType();
            readStyleId();
            readDefault();
            this.attributesRead = true;
        }

        private void readType() {
            this.type = StyleType.fromString(
                getAttributeValue(
                    this.startElement,
                    new QName(this.startElement.getName().getNamespaceURI(), TYPE, this.startElement.getName().getPrefix())
                )
            );
            if (this.type == StyleType.UNSUPPORTED) {
                this.type = StyleType.PARAGRAPH;
            }
        }

        private void readStyleId() {
            this.id = getAttributeValue(
                this.startElement,
                new QName(this.startElement.getName().getNamespaceURI(), STYLE_ID, this.startElement.getName().getPrefix())
            );
        }

        private void readDefault() {
            this.shouldBeDefault = getBooleanAttributeValue(
                startElement,
                new QName(this.startElement.getName().getNamespaceURI(), DEFAULT, this.startElement.getName().getPrefix()),
                DEFAULT_BOOLEAN_ATTRIBUTE_FALSE_VALUE
            );
        }

        private void readElements(final XMLEventReader reader) throws XMLStreamException {
            this.eventsBeforeParagraphProperties = new LinkedList<>();
            List<XMLEvent> currentEvents = this.eventsBeforeParagraphProperties;
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isEndElement() && event.asEndElement().getName().equals(this.startElement.getName())) {
                    if (null == this.paragraphProperties) {
                        this.paragraphProperties = new ParagraphBlockProperties.Word(
                            new BlockProperties.Default(
                                this.eventFactory,
                                this.startElement.getName().getPrefix(),
                                this.startElement.getName().getNamespaceURI(),
                                ParagraphBlockProperties.PPR
                            ),
                            this.conditionalParameters,
                            this.eventFactory,
                            new StrippableAttributes.Default(this.eventFactory)
                        );
                    }
                    if (null == this.runProperties || 0 == this.runProperties.count()) {
                        this.runProperties = new RunProperties.Default(
                            this.eventFactory,
                            this.startElement.getName().getPrefix(),
                            this.startElement.getName().getNamespaceURI(),
                            RunProperties.RPR
                        );
                    }
                    if (StyleType.TABLE == this.type && null == this.tableProperties) {
                        this.tableProperties = new BlockProperties.Default(
                            this.eventFactory,
                            this.startElement.getName().getPrefix(),
                            this.startElement.getName().getNamespaceURI(),
                            BlockProperties.TBL_PR
                        );
                    }
                    this.endElement = event.asEndElement();
                    return;
                } else if (event.isStartElement()) {
                    final String localPart = event.asStartElement().getName().getLocalPart();
                    if (BASED_ON.equals(localPart)) {
                        this.parentId = getAttributeValue(event.asStartElement(), WPML_VAL);
                    } else if (LINK.equals(localPart)) {
                        this.linkedId = getAttributeValue(event.asStartElement(), WPML_VAL);
                    } else if (ParagraphBlockProperties.PPR.equals(localPart)) {
                        this.paragraphProperties = paragraphProperties(event.asStartElement(), reader);
                        this.eventsBeforeRunProperties = new LinkedList<>();
                        currentEvents = this.eventsBeforeRunProperties;
                        continue;
                    } else if (RunProperties.RPR.equals(localPart)) {
                        this.runProperties = runProperties(event.asStartElement(), reader);
                        this.eventsBeforeTableProperties = new LinkedList<>();
                        currentEvents = this.eventsBeforeTableProperties;
                        continue;
                    } else if (BlockProperties.TBL_PR.equals(localPart)) {
                        this.tableProperties = blockProperties(event.asStartElement(), reader);
                        this.eventsBeforeEndElement = new LinkedList<>();
                        currentEvents = this.eventsBeforeEndElement;
                        continue;
                    } else if (BlockProperties.TR_PR.equals(localPart)
                            || BlockProperties.TC_PR.equals(localPart)) {
                        if (null == this.eventsBeforeEndElement) {
                            this.eventsBeforeEndElement = new LinkedList<>();
                            currentEvents = this.eventsBeforeEndElement;
                        }
                    } else if (BlockProperties.TBL_STYLE_PR.equals(localPart)) {
                        if (null == this.eventsBeforeEndElement) {
                            this.eventsBeforeEndElement = new LinkedList<>();
                            currentEvents = this.eventsBeforeEndElement;
                        }
                        currentEvents.add(event);
                        currentEvents.addAll(eventsToEndElement(reader, BlockProperties.TBL_STYLE_PR));
                        continue;
                    }
                }
                currentEvents.add(event);
            }
            throw new IllegalStateException(
                UNEXPECTED_STRUCTURE
                .concat(this.startElement.getName().getLocalPart())
                .concat(" end element is absent")
            );
        }

        private ParagraphBlockProperties paragraphProperties(final StartElement startElement, final XMLEventReader reader) throws XMLStreamException {
            return new MarkupComponentParser().parseParagraphBlockProperties(
                StartElementContextFactory.createStartElementContext(
                    startElement,
                    null,
                    reader,
                    this.eventFactory,
                    this.conditionalParameters
                ),
                this.strippableAttributes,
                this.skippableElements
            );
        }

        private BlockProperties blockProperties(final StartElement startElement, final XMLEventReader reader) throws XMLStreamException {
            return new MarkupComponentParser().parseBlockProperties(
                StartElementContextFactory.createStartElementContext(
                    startElement,
                    null,
                    reader,
                    this.eventFactory,
                    this.conditionalParameters
                ),
                this.strippableAttributes,
                this.skippableElements
            );
        }

        private RunProperties runProperties(final StartElement startElement, final XMLEventReader reader) throws XMLStreamException {
            final StartElementContext startElementContext = StartElementContextFactory.createStartElementContext(
                startElement,
                null,
                reader,
                this.eventFactory,
                this.conditionalParameters
            );
            return new RunPropertiesParser(
                startElementContext,
                new RunSkippableElements(startElementContext)
            )
            .parse();
        }

        private List<XMLEvent> eventsToEndElement(final XMLEventReader reader, final String name) throws XMLStreamException {
            final List<XMLEvent> events = new LinkedList<>();
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                events.add(event);
                if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(name)) {
                    return events;
                }
            }
            throw new IllegalStateException(
                UNEXPECTED_STRUCTURE
                .concat(name)
                .concat(" end element is absent")
            );
        }

        @Override
        public ParagraphBlockProperties paragraphProperties() {
            if (null == this.paragraphProperties) {
                this.paragraphProperties = new ParagraphBlockProperties.Word(
                    new BlockProperties.Default(
                        this.eventFactory,
                        this.startElement.getName().getPrefix(),
                        this.startElement.getName().getNamespaceURI(),
                        ParagraphBlockProperties.PPR
                    ),
                    this.conditionalParameters,
                    this.eventFactory,
                    new StrippableAttributes.Default(this.eventFactory)
                );
            }
            return this.paragraphProperties;
        }

        @Override
        public void paragraphProperties(final ParagraphBlockProperties paragraphBlockProperties) {
            this.paragraphProperties = paragraphBlockProperties;
        }

        @Override
        public RunProperties runProperties() {
            return this.runProperties;
        }

        @Override
        public void runProperties(final RunProperties runProperties) {
            this.runProperties = runProperties;
        }

        @Override
        public Markup toMarkup() {
            final Markup markup = new Markup.General(new LinkedList<>());
            markup.addComponent(new MarkupComponent.Start(this.eventFactory, this.startElement));
            if (null == this.eventsBeforeParagraphProperties) {
                readElements();
            }
            markup.addComponent(new MarkupComponent.General(this.eventsBeforeParagraphProperties));
            if (null != this.paragraphProperties) {
                markup.addComponent(this.paragraphProperties);
            }
            if (null != this.eventsBeforeRunProperties) {
                markup.addComponent(new MarkupComponent.General(this.eventsBeforeRunProperties));
            }
            markup.addComponent(this.runProperties);
            if (null != this.eventsBeforeTableProperties) {
                markup.addComponent(new MarkupComponent.General(this.eventsBeforeTableProperties));
            }
            if (null != this.tableProperties) {
                markup.addComponent(this.tableProperties);
            }
            if (null != this.eventsBeforeEndElement) {
                markup.addComponent(new MarkupComponent.General(this.eventsBeforeEndElement));
            }
            markup.addComponent(new MarkupComponent.End(this.endElement));

            return markup;
        }

        private void readElements() {
            this.eventsBeforeParagraphProperties = new LinkedList<>();
            readNameElement();
            readBasedOnElement();
            readEndElement();
        }

        private void readNameElement() {
            readElementWith(General.NAME, id());
        }

        private void readBasedOnElement() {
            readElementWith(General.BASED_ON, this.parentId);
        }

        private void readElementWith(final String name, final String value) {
            if (null == value) {
                return;
            }
            final List<Attribute> attributes = Collections.singletonList(
                this.eventFactory.createAttribute(
                    new QName(
                        this.startElement.getName().getNamespaceURI(),
                        General.VAL,
                        this.startElement.getName().getPrefix()
                    ),
                    value
                )
            );
            this.eventsBeforeParagraphProperties.add(
                this.eventFactory.createStartElement(
                    new QName(
                        this.startElement.getName().getNamespaceURI(),
                        name,
                        this.startElement.getName().getPrefix()
                    ),
                    attributes.iterator(),
                    null
                )
            );
            this.eventsBeforeParagraphProperties.add(
                this.eventFactory.createEndElement(
                    new QName(
                        this.startElement.getName().getNamespaceURI(),
                        name,
                        this.startElement.getName().getPrefix()
                    ),
                    null
                )
            );
        }

        private void readEndElement() {
            this.endElement = this.eventFactory.createEndElement(
                this.startElement.getName(),
                null
            );
        }
    }
}
