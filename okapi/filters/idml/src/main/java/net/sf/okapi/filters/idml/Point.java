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

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.util.Deque;
import java.util.Iterator;

interface Point {
    double x();
    double y();
    Point transformedWith(final Deque<OrderingIdioms.TransformationMatrix> transformationMatrices);
    String toString();

    final class Default implements Point {
        private final double x;
        private final double y;

        Default(final StartElement startElement, final QName attributeName) {
            this(startElement.getAttributeByName(attributeName));
        }

        Default(final Attribute attribute) {
            this(attribute.getValue().split(" "));
        }

        Default(final String[] coordinates) {
            this(coordinates[0], coordinates[1]);
        }

        Default(final String x, final String y) {
            this(Double.parseDouble(x), Double.parseDouble(y));
        }

        Default(final double x, final double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public double x() {
            return this.x;
        }

        @Override
        public double y() {
            return this.y;
        }

        @Override
        public Point transformedWith(final Deque<OrderingIdioms.TransformationMatrix> transformationMatrices) {
            final Iterator<OrderingIdioms.TransformationMatrix> transformationMatrixIterator =
                transformationMatrices.descendingIterator();
            Point p = this;
            while (transformationMatrixIterator.hasNext()) {
                final OrderingIdioms.TransformationMatrix tm = transformationMatrixIterator.next();
                p = new Point.Default(
                    tm.getA() * p.x() + tm.getC() * p.y() + tm.getTx(),
                    tm.getB() * p.x() + tm.getD() * p.y() + tm.getTy()
                );
            }
            return p;
        }

        @Override
        public String toString() {
            return String.valueOf(this.x).concat(" ").concat(String.valueOf(this.y));
        }
    }

    final class Comparator implements java.util.Comparator<Point> {
        private final OrderingIdioms.Direction direction;

        Comparator(OrderingIdioms.Direction direction) {
            this.direction = direction;
        }

        @Override
        public int compare(final Point point, final Point anotherPoint) {
            int result;

            if (OrderingIdioms.Direction.RIGHT_TO_LEFT == direction) {
                result = Double.compare(anotherPoint.x(), point.x());

                if (0 != result) {
                    return result;
                }

                return Double.compare(anotherPoint.y(), point.y());
            }

            result = Double.compare(point.x(), anotherPoint.x());

            if (0 != result) {
                return result;
            }

            return Double.compare(point.y(), anotherPoint.y());
        }
    }
}
