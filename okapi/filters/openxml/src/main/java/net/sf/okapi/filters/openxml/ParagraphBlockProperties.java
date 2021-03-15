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

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.WPML_VAL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.getAttributeValue;

interface ParagraphBlockProperties extends BlockProperties {
    String PPR = "pPr";

    boolean containsRunPropertyDeletedParagraphMark();
    String paragraphStyle();
    String highlightColor();
    String textColor();
    void refine(final QName innerBlockPropertyName, final String styleId, final List<Property> commonRunProperties) throws XMLStreamException;
    ParagraphBlockProperties withoutParagraphStyle();
    boolean mergeableWith(final ParagraphBlockProperties other);
    ParagraphBlockProperties mergedWith(final ParagraphBlockProperties other);

    final class Drawing implements ParagraphBlockProperties {
        private static final String LOCAL_LEVEL = "lvl";

        private final Default defaultBlockProperties;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final StrippableAttributes strippableAttributes;
        private final SchemaDefinition.Component schemaDefinition;

        Drawing(
            final BlockProperties.Default defaultBlockProperties,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final StrippableAttributes strippableAttributes,
            final SchemaDefinition.Component schemaDefinition
        ) {
            this.defaultBlockProperties = defaultBlockProperties;
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.strippableAttributes = strippableAttributes;
            this.schemaDefinition = schemaDefinition;
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.defaultBlockProperties.getEvents();
        }

        @Override
        public QName getName() {
            return this.defaultBlockProperties.getName();
        }

        @Override
        public void apply(final FontMappings fontMappings) {
            this.defaultBlockProperties.properties()
                .forEach(p -> p.apply(fontMappings));
        }

        @Override
        public StartElement startElement() {
            return this.defaultBlockProperties.startElement();
        }

        @Override
        public List<Attribute> attributes() {
            return this.defaultBlockProperties.attributes();
        }

        @Override
        public List<Property> properties() {
            return this.defaultBlockProperties.properties();
        }

        @Override
        public EndElement endElement() {
            return this.defaultBlockProperties.endElement();
        }

        @Override
        public boolean isEmpty() {
            return this.defaultBlockProperties.isEmpty();
        }

        @Override
        public boolean containsRunPropertyDeletedParagraphMark() {
            return false;
        }

        @Override
        public String paragraphStyle() {
            final Attribute paragraphLevelAttribute = paragraphLevelAttribute();

            if (null != paragraphLevelAttribute) {
                return paragraphLevelAttribute.getValue();
            }
            return null;
        }

        private Attribute paragraphLevelAttribute() {
            for (Attribute attribute : this.defaultBlockProperties.attributes()) {
                if (LOCAL_LEVEL.equals(attribute.getName().getLocalPart())) {
                    return attribute;
                }
            }
            return null;
        }

        @Override
        public String highlightColor() {
            return null;
        }

        @Override
        public String textColor() {
            return null;
        }

        @Override
        public void refine(final QName innerBlockPropertyName, final String styleId, final List<Property> commonRunProperties) throws XMLStreamException {
            final ListIterator<Property> propertiesIterator = this.defaultBlockProperties.properties().listIterator();
            while (propertiesIterator.hasNext()) {
                final Property blockProperty = propertiesIterator.next();
                if (blockProperty.getName().equals(innerBlockPropertyName)) {
                    updateProperty(propertiesIterator, blockProperty, commonRunProperties);
                    return;
                }
            }
            addProperty(propertiesIterator, innerBlockPropertyName, commonRunProperties);
        }

        private void updateProperty(
            final ListIterator<Property> propertiesIterator,
            final Property blockProperty,
            final List<Property> commonRunProperties
        ) throws XMLStreamException {
            final RunProperties runProperties = blockProperty.asRunProperties();
            runProperties.refine(commonRunProperties);
            runProperties.alignWith(this.schemaDefinition);
            propertiesIterator.set(
                new BlockProperty(
                    runProperties.getEvents(),
                    this.conditionalParameters,
                    this.eventFactory,
                    this.strippableAttributes
                )
            );
        }

        private void addProperty(
            final ListIterator<Property> propertiesIterator,
            final QName innerBlockPropertyName,
            final List<Property> commonRunProperties
        ) {
            final List<Attribute> attributes = asAttributes(commonRunProperties);
            final List<Property> properties = asProperties(commonRunProperties);
            final List<XMLEvent> events = new ArrayList<>();

            events.add(this.eventFactory.createStartElement(innerBlockPropertyName, attributes.iterator(), null));
            events.addAll(asXMLEvents(properties));
            events.add(this.eventFactory.createEndElement(innerBlockPropertyName, null));

            rewindToSchemaDefinedPlace(propertiesIterator, innerBlockPropertyName).add(
                new BlockProperty(
                    events,
                    this.conditionalParameters,
                    this.eventFactory,
                    this.strippableAttributes
                )
            );
        }

        private List<Attribute> asAttributes(final List<Property> commonRunProperties) {
            return commonRunProperties
                .stream()
                .filter(runProperty -> runProperty instanceof RunProperty.AttributeRunProperty)
                .map(runProperty -> this.eventFactory.createAttribute(runProperty.getName(), runProperty.value()))
                .collect(Collectors.toList());
        }

        private List<Property> asProperties(final List<Property> commonRunProperties) {
            return commonRunProperties
                .stream()
                .filter(runProperty -> !(runProperty instanceof RunProperty.AttributeRunProperty))
                .collect(Collectors.toList());
        }

        private static List<XMLEvent> asXMLEvents(final List<Property> commonRunProperties) {
            return commonRunProperties
                .stream()
                .map(Property::getEvents)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        }

        /**
         * Rewinds the block properties iterator to a schema defined place.
         *
         * It is assumed that the iterator does not have next element
         * (we are at the end of the properties list).
         *
         * @param propertiesIterator     The properties iterator
         * @param innerBlockPropertyName The inner block property name
         *
         * @return The block properties iterator
         */
        private ListIterator<Property> rewindToSchemaDefinedPlace(
            final ListIterator<Property> propertiesIterator,
            final QName innerBlockPropertyName
        ) {
            if (!propertiesIterator.hasPrevious()) {
                // empty properties, just returning
                return propertiesIterator;
            }
            final Iterator<SchemaDefinition.Component> iterator =
                    this.schemaDefinition.listIteratorAfter(innerBlockPropertyName);
            if (!iterator.hasNext()) {
                // the inner block property is the last component in the schema definition
                return propertiesIterator;
            }

            while (iterator.hasNext()) {
                final SchemaDefinition.Component component = iterator.next();
                while (propertiesIterator.hasPrevious()) {
                    final Property blockProperty = propertiesIterator.previous();
                    if (blockProperty.getName().equals(component.name())) {
                        return propertiesIterator;
                    }
                }
                rewindToEndOfProperties(propertiesIterator);
            }

            // no block properties present after the inner block property
            return propertiesIterator;
        }

        private void rewindToEndOfProperties(final ListIterator<Property> propertiesIterator) {
            while (propertiesIterator.hasNext()) {
                propertiesIterator.next();
            }
        }

        @Override
        public ParagraphBlockProperties withoutParagraphStyle() {
            return this;
        }

        @Override
        public boolean mergeableWith(final ParagraphBlockProperties paragraphProperties) {
            final Set<Property> otherProperties = new HashSet<>(paragraphProperties.properties());
            otherProperties.removeAll(this.properties());
            if (!otherProperties.isEmpty()) {
                return false;
            }
            final Set<Attribute> otherAttributes = new HashSet<>(paragraphProperties.attributes());
            otherAttributes.removeAll(this.attributes());
            if (!otherAttributes.isEmpty()) {
                return false;
            }
            return true;
        }

        @Override
        public ParagraphBlockProperties mergedWith(final ParagraphBlockProperties paragraphProperties) {
            final List<Property> mergedProperties = properties().stream()
                .filter(p -> !paragraphProperties.properties().contains(p))
                .collect(Collectors.toList());
            mergedProperties.addAll(paragraphProperties.properties());
            final List<Attribute> mergedAttributes = attributes().stream()
                .filter(a -> !paragraphProperties.attributes().contains(a))
                .collect(Collectors.toList());
            mergedAttributes.addAll(paragraphProperties.attributes());
            return new ParagraphBlockProperties.Drawing(
                new Default(
                    this.eventFactory,
                    this.eventFactory.createStartElement(
                        startElement().getName(),
                        mergedAttributes.iterator(),
                        null
                    ),
                    endElement(),
                    mergedProperties
                ),
                this.conditionalParameters,
                this.eventFactory,
                this.strippableAttributes,
                this.schemaDefinition
            );
        }
    }

    final class Word implements ParagraphBlockProperties {
        private static final String PARAGRAPH_STYLE = "pStyle";
        private static final String VAL = "val";

        private final BlockProperties.Default defaultBlockProperties;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final StrippableAttributes strippableAttributes;

        Word(
            final Default defaultBlockProperties,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final StrippableAttributes strippableAttributes) {
            this.defaultBlockProperties = defaultBlockProperties;
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.strippableAttributes = strippableAttributes;
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.defaultBlockProperties.getEvents();
        }

        @Override
        public QName getName() {
            return this.defaultBlockProperties.getName();
        }

        @Override
        public void apply(final FontMappings fontMappings) {
            this.defaultBlockProperties.properties()
                .forEach(p -> p.apply(fontMappings));
        }

        @Override
        public StartElement startElement() {
            return this.defaultBlockProperties.startElement();
        }

        @Override
        public List<Attribute> attributes() {
            return this.defaultBlockProperties.attributes();
        }

        @Override
        public List<Property> properties() {
            return this.defaultBlockProperties.properties();
        }

        @Override
        public EndElement endElement() {
            return this.defaultBlockProperties.endElement();
        }

        @Override
        public boolean isEmpty() {
            return this.defaultBlockProperties.isEmpty();
        }

        @Override
        public boolean containsRunPropertyDeletedParagraphMark() {
            return this.defaultBlockProperties.properties()
                .stream()
                .filter(bp -> RunProperties.RPR.equals(bp.getName().getLocalPart()))
                .map(rp -> rp.getEvents())
                .flatMap(events -> events.stream())
                .filter(e -> e.isStartElement())
                .map(e -> e.asStartElement().getName())
                .anyMatch(n -> SkippableElement.RevisionProperty.RUN_PROPERTY_DELETED_PARAGRAPH_MARK.toName().equals(n));
        }

        @Override
        public String paragraphStyle() {
            final Property paragraphStyleProperty = paragraphStyleProperty();

            if (null != paragraphStyleProperty) {
                return getAttributeValue(paragraphStyleProperty.getEvents().get(0).asStartElement(), WPML_VAL);
            }

            return null;
        }

        private Property paragraphStyleProperty() {
            return blockProperty(PARAGRAPH_STYLE);
        }

        private Property blockProperty(final String localPart) {
            final QName name = new QName(
                this.defaultBlockProperties.getName().getNamespaceURI(),
                localPart
            );
            for (final Property property : this.defaultBlockProperties.properties()) {
                if (property.getName().equals(name)) {
                    return property;
                }
            }
            return null;
        }

        @Override
        public String highlightColor() {
            final Property highlightColorProperty = highlightColorProperty();

            if (null != highlightColorProperty) {
                return getAttributeValue(highlightColorProperty.getEvents().get(0).asStartElement(), WPML_VAL);
            }
            return null;
        }

        private Property highlightColorProperty() {
            return blockProperty(RunProperty.HighlightRunProperty.NAME);
        }

        @Override
        public String textColor() {
            final Property colorProperty = textColorProperty();

            if (null != colorProperty) {
                return getAttributeValue(colorProperty.getEvents().get(0).asStartElement(), WPML_VAL);
            }
            return null;
        }

        private Property textColorProperty() {
            return blockProperty(RunProperty.ColorRunProperty.NAME);
        }

        @Override
        public void refine(final QName innerBlockPropertyName, final String styleId, final List<Property> commonRunProperties) throws XMLStreamException {
            final ListIterator<Property> propertiesIterator = this.defaultBlockProperties.properties().listIterator();
            while (propertiesIterator.hasNext()) {
                final Property blockProperty = propertiesIterator.next();
                if (Word.PARAGRAPH_STYLE.equals(blockProperty.getName().getLocalPart())) {
                    updateProperty(propertiesIterator, innerBlockPropertyName, styleId);
                    return;
                }
            }
            addProperty(propertiesIterator, innerBlockPropertyName, styleId);
        }

        private void updateProperty(
            final ListIterator<Property> propertiesIterator,
            final QName innerBlockPropertyName,
            final String styleId
        ) {
            propertiesIterator.set(
                blockProperty(innerBlockPropertyName, styleId)
            );
        }

        private void addProperty(
            final ListIterator<Property> propertiesIterator,
            final QName innerBlockPropertyName,
            final String styleId
        ) {
            propertiesIterator.add(
                blockProperty(innerBlockPropertyName, styleId)
            );
        }

        private BlockProperty blockProperty(final QName innerBlockPropertyName, final String styleId) {
            return new BlockProperty(
                Word.PARAGRAPH_STYLE,
                Collections.singletonMap(Word.VAL, styleId),
                new CreationalParameters(
                    this.eventFactory,
                    innerBlockPropertyName.getPrefix(),
                    innerBlockPropertyName.getNamespaceURI()
                ),
                this.conditionalParameters,
                new StrippableAttributes.DrawingRunProperties(
                    this.conditionalParameters,
                    this.eventFactory
                )
            );
        }

        @Override
        public ParagraphBlockProperties withoutParagraphStyle() {
            return new Word(
                new Default(
                    this.eventFactory,
                    this.startElement(),
                    this.endElement(),
                    this.properties().stream()
                        .filter(p -> !Word.PARAGRAPH_STYLE.equals(p.getName().getLocalPart()))
                        .collect(Collectors.toList())
                ),
                this.conditionalParameters,
                this.eventFactory,
                this.strippableAttributes
            );
        }

        @Override
        public boolean mergeableWith(final ParagraphBlockProperties other) {
            final Set<Property> otherProperties = new HashSet<>(other.properties());
            otherProperties.removeAll(this.properties());
            if (!otherProperties.isEmpty()) {
                return false;
            }
            return true;
        }

        @Override
        public ParagraphBlockProperties mergedWith(final ParagraphBlockProperties other) {
            final List<Property> mergedProperties = properties().stream()
                .filter(p -> !other.properties().contains(p))
                .collect(Collectors.toList());
            mergedProperties.addAll(other.properties());
            return new ParagraphBlockProperties.Word(
                new Default(
                    this.eventFactory,
                    startElement(),
                    endElement(),
                    mergedProperties
                ),
                this.conditionalParameters,
                this.eventFactory,
                this.strippableAttributes
            );
        }
    }
}
