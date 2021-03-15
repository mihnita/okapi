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
import java.util.HashSet;
import java.util.Set;

/**
 * Provides a skippable element.
 */
interface SkippableElement {

    QName toName();

    /**
     * Provides a general inline skippable element enumeration.
     */
    enum GeneralInline implements SkippableElement {

        PROOFING_ERROR_ANCHOR(Namespaces.WordProcessingML.getQName("proofErr")),
        SOFT_HYPHEN(Namespaces.WordProcessingML.getQName("softHyphen")),
        ALTERNATE_CONTENT_FALLBACK(Namespaces.MarkupCompatibility.getQName("Fallback")),
        LAST_RENDERED_PAGE_BREAK(Namespaces.WordProcessingML.getQName("lastRenderedPageBreak"));

        private final QName name;

        GeneralInline(final QName name) {
            this.name = name;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }

    /**
     * Provides a phonetic run and phonetic property skippable element enumeration.
     */
    enum PhoneticInline implements SkippableElement {

        PHONETIC_RUN(Namespaces.SpreadsheetML.getQName("rPh")),
        PHONETIC_PROPERTY(Namespaces.SpreadsheetML.getQName("phoneticPr"));

        private final QName name;

        PhoneticInline(final QName name) {
            this.name = name;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }

    /**
     * Provides a revision inline skippable element enumeration.
     */
    enum RevisionInline implements SkippableElement {

        RUN_INSERTED_CONTENT(Namespaces.WordProcessingML.getQName("ins")),
        RUN_DELETED_CONTENT(Namespaces.WordProcessingML.getQName("del")),
        RUN_MOVED_CONTENT_TO(Namespaces.WordProcessingML.getQName("moveTo")), // a:moveTo is not considered
        RUN_MOVED_CONTENT_FROM(Namespaces.WordProcessingML.getQName("moveFrom"));

        private final QName name;

        RevisionInline(final QName name) {
            this.name = name;
        }

        static Set<QName> toNames() {
            final Set<QName> names = new HashSet<>(values().length);

            for (final RevisionInline revisionInline : values()) {
                names.add(revisionInline.toName());
            }

            return names;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }

    /**
     * Provides a general cross-structure skippable element enumeration.
     */
    enum GeneralCrossStructure implements SkippableElement {

        BOOKMARK_START(Namespaces.WordProcessingML.getQName("bookmarkStart")),
        BOOKMARK_END(Namespaces.WordProcessingML.getQName("bookmarkEnd"));

        private final QName name;

        GeneralCrossStructure(final QName name) {
            this.name = name;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }

    /**
     * Provides a revision cross-structure skippable element enumeration.
     */
    enum RevisionCrossStructure implements SkippableElement {

        MOVE_TO_RANGE_START(Namespaces.WordProcessingML.getQName("moveToRangeStart")),
        MOVE_TO_RANGE_END(Namespaces.WordProcessingML.getQName("moveToRangeEnd")),
        MOVE_FROM_RANGE_START(Namespaces.WordProcessingML.getQName("moveFromRangeStart")),
        MOVE_FROM_RANGE_END(Namespaces.WordProcessingML.getQName("moveFromRangeEnd"));

        private final QName name;

        RevisionCrossStructure(final QName name) {
            this.name = name;
        }

        static Set<QName> toNames() {
            final Set<QName> names = new HashSet<>(values().length);

            for (final RevisionCrossStructure revisionCrossStructure : values()) {
                names.add(revisionCrossStructure.toName());
            }

            return names;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }

    /**
     * Provides a block property skippable element enumeration.
     */
    enum BlockProperty implements SkippableElement {

        BLOCK_PROPERTY_BIDI_VISUAL(Namespaces.WordProcessingML.getQName(XMLEventHelpers.LOCAL_BIDI_VISUAL));

        private final QName name;

        BlockProperty(final QName name) {
            this.name = name;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }

    /**
     * Provides a run property skippable element enumeration.
     */
    enum RunProperty implements SkippableElement {

        RUN_PROPERTY_COMPLEX_SCRIPT_BOLD(Namespaces.WordProcessingML.getQName("bCs")),
        RUN_PROPERTY_COMPLEX_SCRIPT_ITALICS(Namespaces.WordProcessingML.getQName("iCs")),
        RUN_PROPERTY_RTL_WPML(Namespaces.WordProcessingML.getQName(XMLEventHelpers.LOCAL_RTL)),
        RUN_PROPERTY_RTL_DML(Namespaces.DrawingML.getQName(XMLEventHelpers.LOCAL_RTL)),
        RUN_PROPERTY_LANGUAGE(Namespaces.WordProcessingML.getQName(XMLEventHelpers.LOCAL_PROPERTY_LANGUAGE)),
        RUN_PROPERTY_NO_SPELLING_OR_GRAMMAR(Namespaces.WordProcessingML.getQName("noProof")),
        RUN_PROPERTY_CHARACTER_SPACING(Namespaces.WordProcessingML.getQName("spacing")),
        RUN_PROPERTY_COMPLEX_SCRIPT_FONT_SIZE(Namespaces.WordProcessingML.getQName("szCs")),
        RUN_PROPERTY_CHARACTER_WIDTH(Namespaces.WordProcessingML.getQName("w")),
        RUN_PROPERTY_VERTICAL_ALIGNMENT_WPML(Namespaces.WordProcessingML.getQName("vertAlign")),
        RUN_PROPERTY_VERTICAL_ALIGNMENT_SML(Namespaces.SpreadsheetML.getQName("vertAlign"));

        private final QName name;

        RunProperty(final QName name) {
            this.name = name;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }

    /**
     * Provides a revision property skippable element enumeration.
     */
    enum RevisionProperty implements SkippableElement {

        RUN_PROPERTY_INSERTED_PARAGRAPH_MARK(Namespaces.WordProcessingML.getQName("ins")),
        RUN_PROPERTY_DELETED_PARAGRAPH_MARK(Namespaces.WordProcessingML.getQName("del")),
        RUN_PROPERTY_MOVED_PARAGRAPH_TO(Namespaces.WordProcessingML.getQName("moveTo")),
        RUN_PROPERTY_MOVED_PARAGRAPH_FROM(Namespaces.WordProcessingML.getQName("moveFrom")),

        PARAGRAPH_PROPERTIES_CHANGE(Namespaces.WordProcessingML.getQName("pPrChange")),
        RUN_PROPERTIES_CHANGE(Namespaces.WordProcessingML.getQName("rPrChange")),
        SECTION_PROPERTIES_CHANGE(Namespaces.WordProcessingML.getQName("sectPrChange")),
        TABLE_GRID_CHANGE(Namespaces.WordProcessingML.getQName("tblGridChange")),
        TABLE_PROPERTIES_CHANGE(Namespaces.WordProcessingML.getQName("tblPrChange")),
        TABLE_PROPERTIES_EXCEPTIONS_CHANGE(Namespaces.WordProcessingML.getQName("tblPrExChange")),
        TABLE_CELL_PROPERTIES_CHANGE(Namespaces.WordProcessingML.getQName("tcPrChange")),
        TABLE_ROW_PROPERTIES_CHANGE(Namespaces.WordProcessingML.getQName("trPrChange")),
        TABLE_ROW_INSERTED(Namespaces.WordProcessingML.getQName("ins")),
        TABLE_ROW_DELETED(Namespaces.WordProcessingML.getQName("del"));

        private final QName name;

        RevisionProperty(final QName name) {
            this.name = name;
        }

        static Set<QName> toNames() {
            final Set<QName> names = new HashSet<>(values().length);

            for (final RevisionProperty revisionProperty : values()) {
                names.add(revisionProperty.toName());
            }

            return names;
        }

        @Override
        public QName toName() {
            return this.name;
        }
    }
}
