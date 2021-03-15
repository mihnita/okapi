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

import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.eventEquals;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.getAttributeValue;

/**
 * Provides a block property.
 */
final class BlockProperty implements Property {
    private static final QName DEFAULT_NAME = new QName("");
    private static final String BU_FONT = "buFont";

    private final List<XMLEvent> events;
    private final XMLEventFactory eventFactory;
    private final ConditionalParameters conditionalParameters;
    private final StrippableAttributes strippableAttributes;

    BlockProperty(
        final String localName,
        final Map<String, String> attributes,
        final CreationalParameters creationalParameters,
        final ConditionalParameters conditionalParameters,
        final StrippableAttributes strippableAttributes
    ) {
        this(
            Stream.of(
                creationalParameters.getEventFactory().createStartElement(
                    creationalParameters.getPrefix(),
                    creationalParameters.getNamespaceUri(),
                    localName,
                    attributes.entrySet().stream()
                        .map(e -> creationalParameters.getEventFactory().createAttribute(
                                creationalParameters.getPrefix(),
                                creationalParameters.getNamespaceUri(),
                                e.getKey(),
                                e.getValue()
                            )
                        ).collect(Collectors.toList()).iterator(),
                    null
                ),
                creationalParameters.getEventFactory().createEndElement(
                    creationalParameters.getPrefix(),
                    creationalParameters.getNamespaceUri(),
                    localName
                )
            ).collect(Collectors.toList()),
            conditionalParameters,
            creationalParameters.getEventFactory(),
            strippableAttributes
        );
    }

    BlockProperty(
        final List<XMLEvent> events,
        final ConditionalParameters conditionalParameters,
        final XMLEventFactory eventFactory,
        final StrippableAttributes strippableAttributes
    ) {
        this.events = events;
        this.conditionalParameters = conditionalParameters;
        this.eventFactory = eventFactory;
        this.strippableAttributes = strippableAttributes;
    }

    @Override
    public List<XMLEvent> getEvents() {
        return events;
    }

    @Override
    public QName getName() {
        return null == events.get(0)
                ? DEFAULT_NAME
                : events.get(0).asStartElement().getName();
    }

    @Override
    public RunProperties asRunProperties() throws XMLStreamException {
        if (!RunProperties.RPR.equals(getName().getLocalPart())
            && !RunProperties.DEF_RPR.equals(getName().getLocalPart())) {
            throw new IllegalStateException(
                String.format(
                    "The %s block property can't become run properties",
                    getName().toString()
                )
            );
        }
        final XMLEventReader eventReader = new XMLEventsReader(this.events);
        final StartElementContext startElementContext = StartElementContextFactory.createStartElementContext(
            this.strippableAttributes.strip(eventReader.nextEvent().asStartElement()),
            null,
            eventReader,
            this.eventFactory,
            this.conditionalParameters
        );
        final RunProperties runProperties = new RunPropertiesParser(
            startElementContext,
            new RunSkippableElements(startElementContext)
        ).parse();
        if (runProperties.properties().isEmpty()) {
            return new RunProperties.Default(
                this.eventFactory,
                this.events.get(0).asStartElement(),
                this.events.get(this.events.size() - 1).asEndElement(),
                new ArrayList<>()
            );
        }
        return runProperties;
    }

    @Override
    public void apply(final FontMappings fontMappings) {
        if (this.events.isEmpty()) {
            return;
        }
        final StartElement startElement = this.events.get(0).asStartElement();
        if (BlockProperty.BU_FONT.equals(startElement.getName().getLocalPart())) {
            final String font = getAttributeValue(startElement, RunProperty.FontRunProperty.TYPEFACE);
            final String newFont = fontMappings.targetFontFor(font);
            if (font.equals(newFont)) {
                return; // no adjustment needed
            }
            final List<Attribute> attributes = new LinkedList<>();
            final Iterator iterator = startElement.getAttributes();
            while (iterator.hasNext()) {
                final Attribute attribute = (Attribute) iterator.next();
                if (RunProperty.FontRunProperty.TYPEFACE.equals(attribute.getName())) {
                    attributes.add(
                        this.eventFactory.createAttribute(
                            attribute.getName(),
                            newFont
                        )
                    );
                    continue;
                }
                attributes.add(attribute);
            }
            this.events.set(
                0,
                this.eventFactory.createStartElement(
                    startElement.getName(),
                    attributes.iterator(),
                    null
                )
            );
        }
        if (RunProperties.RPR.equals(startElement.getName().getLocalPart())
            || RunProperties.DEF_RPR.equals(startElement.getName().getLocalPart())) {
            final RunProperties runProperties;
            try {
                runProperties = asRunProperties();
            } catch (XMLStreamException e) {
                throw new OkapiBadFilterInputException("Error with reading the XML");
            }
            if (0 == runProperties.count()) {
                return; // no adjustment needed
            }
            runProperties.apply(fontMappings);
            if (this.events.equals(runProperties.getEvents())) {
                return; // no adjustment needed
            }
            this.events.clear();
            this.events.addAll(runProperties.getEvents());
        }
    }

    @Override
    public String value() {
        final StartElement startElement = this.events.get(0).asStartElement();
        final String value;
        if (Namespace.PREFIX_W.equals(startElement.getName().getPrefix())) {
            value = XMLEventHelpers.getAttributeValue(startElement, XMLEventHelpers.WPML_VAL);
        } else if (Namespace.PREFIX_A.equals(startElement.getName().getPrefix())) {
            value = XMLEventHelpers.getAttributeValue(startElement, XMLEventHelpers.DML_VAL);
        } else {
            throw new IllegalStateException("The operation is not supported");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockProperty that = (BlockProperty) o;

        return eventEquals(events, that.events);
    }

    @Override
    public int hashCode() {
        return events.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
    }
}
