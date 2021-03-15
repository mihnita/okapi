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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

interface PathPoint extends Element {
    QName NAME = Namespaces.getDefaultNamespace().getQName("PathPointType");
    Point anchor();
    Point leftDirection();
    Point rightDirection();

    final class Default implements PathPoint {
        private static final QName ANCHOR = Namespaces.getDefaultNamespace().getQName("Anchor");
        private static final QName LEFT_DIRECTION = Namespaces.getDefaultNamespace().getQName(
            "LeftDirection"
        );
        private static final QName RIGHT_DIRECTION = Namespaces.getDefaultNamespace().getQName(
            "RightDirection"
        );

        private final Point anchor;
        private final Point leftDirection;
        private final Point rightDirection;
        private final XMLEventFactory eventFactory;

        private StartElement startElement;
        private EndElement endElement;

        Default(
            final StartElement startElement,
            final XMLEventFactory eventFactory
        ) {
            this(
                new Point.Default(startElement, ANCHOR),
                new Point.Default(startElement, LEFT_DIRECTION),
                new Point.Default(startElement, RIGHT_DIRECTION),
                eventFactory
            );
        }

        Default(
            final Point anchor,
            final Point leftDirection,
            final Point rightDirection,
            final XMLEventFactory eventFactory
        ) {
            this.anchor = anchor;
            this.leftDirection = leftDirection;
            this.rightDirection = rightDirection;
            this.eventFactory = eventFactory;
        }

        @Override
        public Point anchor() {
            return this.anchor;
        }

        @Override
        public Point leftDirection() {
            return this.leftDirection;
        }

        @Override
        public Point rightDirection() {
            return this.rightDirection;
        }

        @Override
        public StartElement startElement() {
            if (null == this.startElement) {
                this.startElement = this.eventFactory.createStartElement(
                    NAME,
                    Arrays.asList(
                        this.eventFactory.createAttribute(ANCHOR, this.anchor.toString()),
                        this.eventFactory.createAttribute(LEFT_DIRECTION, this.leftDirection.toString()),
                        this.eventFactory.createAttribute(RIGHT_DIRECTION, this.rightDirection.toString())
                    ).iterator(),
                    null
                );
            }
            return this.startElement;
        }

        @Override
        public List<XMLEvent> innerEvents() {
            return Collections.emptyList();
        }

        @Override
        public void updateInnerEventsWith(final List<XMLEvent> events) {
        }

        @Override
        public EndElement endElement() {
            if (null == this.endElement) {
                this.endElement = this.eventFactory.createEndElement(NAME, null);
            }
            return this.endElement;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public QName getName() {
            return this.startElement().getName();
        }

        @Override
        public List<XMLEvent> getEvents() {
            return Arrays.asList(startElement(), endElement());
        }
    }
}
