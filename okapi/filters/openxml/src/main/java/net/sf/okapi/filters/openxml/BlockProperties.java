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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides a block properties markup component.
 */
interface BlockProperties extends MarkupComponent, Nameable {
    String TBL_PR = "tblPr";
    String TR_PR = "trPr";
    String TC_PR = "tcPr";
    String TBL_STYLE_PR = "tblStylePr";
    String BODY_PR = "bodyPr";
    String SMART_TAG_PROPERTIES = "smartTagPr";
    String STRUCTURAL_DOCUMENT_TAG_PROPERTIES = "sdtPr";
    String STRUCTURAL_DOCUMENT_TAG_END_PROPERTIES = "sdtEndPr";

    StartElement startElement();
    List<Attribute> attributes();
    List<Property> properties();
    EndElement endElement();
    boolean isEmpty();

    final class Default implements BlockProperties {
        private final XMLEventFactory eventFactory;
        private final StartElement startElement;
        private final EndElement endElement;

        private final List<Attribute> attributes;
        private final List<Property> properties;

        Default(
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
                    name,
                    null,
                    null
                ),
                eventFactory.createEndElement(
                    prefix,
                    namespaceUri,
                    name,
                    null
                )
            );
        }

        Default(
            final XMLEventFactory eventFactory,
            final StartElement startElement,
            final EndElement endElement
        ) {
            this(
                eventFactory,
                startElement,
                endElement,
                new ArrayList<>()
            );
        }

        Default(
            final XMLEventFactory eventFactory,
            final StartElement startElement,
            final EndElement endElement,
            final List<Property> properties
        ) {
            this.eventFactory = eventFactory;
            this.startElement = startElement;
            this.endElement = endElement;

            this.attributes = new ArrayList<>();
            Iterator iterator = startElement.getAttributes();

            while (iterator.hasNext()) {
                this.attributes.add((Attribute) iterator.next());
            }

            this.properties = new ArrayList<>(properties);
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public QName getName() {
            return startElement.getName();
        }

        @Override
        public List<XMLEvent> getEvents() {
            if (isEmpty() && !BODY_PR.equals(this.startElement.getName().getLocalPart())) {
                return Collections.emptyList();
            }
            final List<XMLEvent> events = new ArrayList<>();
            events.add(eventFactory.createStartElement(startElement.getName(), attributes().iterator(), startElement.getNamespaces()));
            for (final Property property : properties) {
                events.addAll(property.getEvents());
            }
            events.add(endElement);
            return events;
        }

        @Override
        public StartElement startElement() {
            return this.startElement;
        }

        @Override
        public List<Attribute> attributes() {
            return attributes;
        }

        @Override
        public List<Property> properties() {
            return properties;
        }

        @Override
        public EndElement endElement() {
            return this.endElement;
        }

        @Override
        public boolean isEmpty() {
            return this.attributes.isEmpty() && this.properties.isEmpty();
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            final String prefix = startElement.getName().getPrefix();
            if (!prefix.isEmpty()) {
                builder.append(prefix).append(":");
            }
            builder.append(startElement.getName().getLocalPart()).append(" ")
                .append("(").append(attributes.size()).append(")")
                .append(toString(attributes)).append(" ")
                .append("(").append(properties.size()).append(")")
                .append(properties);
            return builder.toString();
        }

        private String toString(final List<Attribute> attributes) {
            final StringBuilder builder = new StringBuilder();
            builder.append("[");
            for (final Attribute attribute : attributes) {
                final String prefix = attribute.getName().getPrefix();
                if (!prefix.isEmpty()) {
                    builder.append(prefix).append(":");
                }
                builder.append(attribute.getName().getLocalPart()).append("=\"");
                builder.append(attribute.getValue()).append("\"");
            }
            builder.append("]");
            return builder.toString();
        }
    }
}
