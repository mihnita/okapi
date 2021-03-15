/*
 * =============================================================================
 *   Copyright (C) 2010-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.idml;

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

interface Markup extends Iterable<MarkupRange>, Eventive {
    void add(final MarkupRange markupRange);
    void apply(final FontMappings fontMappings);

    class Default implements Markup {
        private final List<MarkupRange> ranges;

        Default(List<MarkupRange> ranges) {
            this.ranges = ranges;
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.ranges.stream()
                .map(r -> r.getEvents())
                .flatMap(events -> events.stream())
                .collect(Collectors.toList());
        }

        @Override
        public Iterator<MarkupRange> iterator() {
            return this.ranges.iterator();
        }

        @Override
        public void add(final MarkupRange markupRange) {
            this.ranges.add(markupRange);
        }

        @Override
        public void apply(final FontMappings fontMappings) {
            this.ranges.forEach(r -> r.apply(fontMappings));
        }
    }
}
