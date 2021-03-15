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

import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.XMLEvent;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.singletonList;

interface SpecialCharacter extends MarkupRange {
    XMLEvent event();
    SpecialCharacter.Type type();

    class Default implements SpecialCharacter {
        private final MarkupRange.Default defaultRange;

        Default(final XMLEvent event) {
            this(singletonList(event));
        }

        Default(final List<XMLEvent> events) {
            this(new MarkupRange.Default(events));
        }

        Default(final MarkupRange.Default range) {
            this.defaultRange = range;
        }

        @Override
        public XMLEvent event() {
            return getEvents().get(0);
        }

        @Override
        public Type type() {
            return SpecialCharacter.Type.fromDefault(this);
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.defaultRange.getEvents();
        }
    }

    class Instruction implements SpecialCharacter {
        private final SpecialCharacter.Default defaultSpecialCharacter;

        Instruction(final XMLEvent event) {
            this(new SpecialCharacter.Default(event));
        }

        Instruction(final SpecialCharacter.Default defaultSpecialCharacter) {
            this.defaultSpecialCharacter = defaultSpecialCharacter;
        }

        @Override
        public XMLEvent event() {
            return this.defaultSpecialCharacter.event();
        }

        @Override
        public Type type() {
            return SpecialCharacter.Type.fromInstruction(this);
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.defaultSpecialCharacter.getEvents();
        }
    }

    enum Type {
        ALIGNMENT("0"),
        END_NESTED_STYLE("3"),
        FOOTNOTE_MARKER("4"),
        INDENT_HERE_TAB("7"),
        RIGHT_INDENT_TAB("8"),
        AUTO_PAGE_NUMBER("18"),
        SECTION_MARKER("19"),

        FIXED_WIDTH_NON_BREAKING_SPACE("\u202F"),

        HAIR_SPACE("\u200A"),
        THIN_SPACE("\u2009"),
        PUNCTUATION_SPACE("\u2008"),
        FIGURE_SPACE("\u2007"),
        SIXTH_SPACE("\u2006"),
        QUARTER_SPACE("\u2005"),
        THIRD_SPACE("\u2004"),
        FLUSH_SPACE("\u2001"),

        FORCED_LINE_BREAK("\u2028"),
        DISCRETIONARY_LINE_BRAKE("\u200B"),
        ZERO_WIDTH_NON_JOINER("\u200C"),

        DISCRETIONARY_HYPHEN("\u00AD"),
        NON_BREAKING_HYPHEN("\u2011"),

        ZERO_WIDTH_NO_BREAK_SPACE("\uFEFF"),

        UNSUPPORTED("");

        private final String value;

        Type(final String value) {
            this.value = value;
        }

        static Type fromDefault(final SpecialCharacter.Default defaultSpecialCharacter) {
            return fromDefaultString(defaultSpecialCharacter.event().asCharacters().getData());
        }

        static Type fromDefaultString(final String string) {
            for (Type type : EnumSet.range(FIXED_WIDTH_NON_BREAKING_SPACE, ZERO_WIDTH_NO_BREAK_SPACE)) {
                if (type.toString().equals(string)) {
                    return type;
                }
            }
            return UNSUPPORTED;
        }

        static Type fromInstruction(final Instruction instruction) {
            for (Type type : EnumSet.range(ALIGNMENT, SECTION_MARKER)) {
                if (type.toString().equals(((ProcessingInstruction) instruction.event()).getData())) {
                    return type;
                }
            }
            return UNSUPPORTED;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
