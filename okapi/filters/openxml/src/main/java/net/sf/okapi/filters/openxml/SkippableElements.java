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

import net.sf.okapi.common.exceptions.OkapiUnexpectedRevisionException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_TABLE_GRID;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.WPML_ID;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.WPML_NAME;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.getAttributeValue;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isEndElement;

/**
 * Privides skippable elements interface.
 */
interface SkippableElements {

    String OPERATION_IS_UNSUPPORTED = "The operation is unsupported.";

    boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement);

    boolean canBeSkipped(final EndElement endElement);

    void skip(final StartElementContext startElementContext) throws XMLStreamException;

    default void skip(final EndElement endElement) {
    }

    static boolean isValuePresentInContextAwareSkippableElements(
        final StartElement startElement,
        final Map<QName, QName> contextAwareSkippableElements
    ) {
        return contextAwareSkippableElements.containsValue(startElement.getName());
    }

    static boolean isEntryPresentInContextAwareSkippableElements(
        final StartElement startElement,
        final StartElement parentStartElement,
        final Map<QName, QName> contextAwareSkippableElements
    ) {
        if (null == parentStartElement) {
            return false;
        }

        for (Map.Entry<QName, QName> entry : contextAwareSkippableElements.entrySet()) {
            if (parentStartElement.getName().equals(entry.getKey())
                    && startElement.getName().equals(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Provides an empty skippable elements implementation.
     */
    final class Empty implements SkippableElements {
        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return false;
        }

        @Override
        public boolean canBeSkipped(EndElement endElement) {
            return false;
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
        }

    }

    /**
     * Provides a default skippable elements implementation.
     */
    final class Default implements SkippableElements {
        private final Set<QName> skippableElementNames;

        Default(final SkippableElement... skippableElements) {
            this.skippableElementNames = Arrays.stream(skippableElements)
                .map(SkippableElement::toName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return this.skippableElementNames.contains(startElement.getName());
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            return this.skippableElementNames.contains(endElement.getName());
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            while (startElementContext.getEventReader().hasNext()) {
                final XMLEvent e = startElementContext.getEventReader().nextEvent();

                if (isEndElement(e, startElementContext.getStartElement())) {
                    return;
                }
            }

            throw new IllegalStateException(ExceptionMessages.UNEXPECTED_STRUCTURE);
        }

        void add(final SkippableElement skippableElement) {
            this.skippableElementNames.add(skippableElement.toName());
        }
    }

    /**
     * Provides an inline skippable element implementation.
     */
    final class Inline implements SkippableElements {
        private final Default defaultSkippableElements;

        Inline(final Default defaultSkippableElements) {
            this.defaultSkippableElements = defaultSkippableElements;
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return this.defaultSkippableElements.canBeSkipped(startElement, parentStartElement);
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            return this.defaultSkippableElements.canBeSkipped(endElement);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            this.defaultSkippableElements.skip(startElementContext);
        }
    }

    final class RevisionInline implements SkippableElements {
        private static final Set<QName> REVISION_SKIPPABLE_ELEMENTS = new HashSet<>();
        static {
            REVISION_SKIPPABLE_ELEMENTS.addAll(SkippableElement.RevisionInline.toNames());
        }

        private final Inline inlineSkippableElements;

        RevisionInline(final Inline inlineSkippableElements) {
            this.inlineSkippableElements = inlineSkippableElements;
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return this.inlineSkippableElements.canBeSkipped(startElement, parentStartElement);
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            return this.inlineSkippableElements.canBeSkipped(endElement);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            if (!startElementContext.getConditionalParameters().getAutomaticallyAcceptRevisions()
                    && REVISION_SKIPPABLE_ELEMENTS.contains(startElementContext.getStartElement().getName())) {
                throw new OkapiUnexpectedRevisionException();
            }
            if (SkippableElement.RevisionInline.RUN_INSERTED_CONTENT.toName().equals(startElementContext.getStartElement().getName())
                    || SkippableElement.RevisionInline.RUN_MOVED_CONTENT_TO.toName().equals(startElementContext.getStartElement().getName())) {
                return;
            }
            this.inlineSkippableElements.skip(startElementContext);
        }
    }

    /**
     * Provides a cross-structure skippable elements implementation.
     */
    final class CrossStructure implements SkippableElements {
        /**
         * The supported number of skippable elements.
         */
        private static final int SUPPORTED_NUMBER_OF_SKIPPABLE_ELEMENTS = 2;
        private static final String START_SUFFIX = "Start";
        private static final String END_SUFFIX = "End";

        private final Default defaultSkippableElements;
        private final QName startSkippableElementName;
        private final QName endSkippableElementName;
        private String identifier;

        /**
         * Constructs the cross-structure skippable elements.
         *
         * @param defaultSkippableElements The default skippable elements.
         *                                 Their size has to be equal to 2 and
         *                                 they have to be in the following order:
         *                                 1. the start skippable element, e.g. "bookmarkStart"
         *                                 2. the end skippable element, e.g. "bookmarkEnd"
         */
        CrossStructure(final Default defaultSkippableElements) {
            if (SUPPORTED_NUMBER_OF_SKIPPABLE_ELEMENTS != defaultSkippableElements.skippableElementNames.size()) {
                throw new IllegalArgumentException(
                    String.format(
                        "The provided number of skippable elements is invalid: '%d'. It must be equal to '%d'.",
                        defaultSkippableElements.skippableElementNames.size(),
                        SUPPORTED_NUMBER_OF_SKIPPABLE_ELEMENTS
                    )
                );
            }
            this.defaultSkippableElements = defaultSkippableElements;
            final Iterator<QName> namesIterator = defaultSkippableElements.skippableElementNames.iterator();
            this.startSkippableElementName = namesIterator.next();
            if (!this.startSkippableElementName.getLocalPart().endsWith(START_SUFFIX)) {
                throw new IllegalArgumentException(
                    String.format(
                        "The start skippable element is invalid: '%s'. It must end with '%s'",
                        this.startSkippableElementName,
                        START_SUFFIX
                    )
                );
            }
            this.endSkippableElementName = namesIterator.next();
            if (!this.endSkippableElementName.getLocalPart().endsWith(END_SUFFIX)) {
                throw new IllegalArgumentException(
                    String.format(
                        "The end skippable element is invalid: '%s'. It must end with '%s'.",
                        this.endSkippableElementName,
                        END_SUFFIX
                    )
                );
            }
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return this.startSkippableElementName.equals(startElement.getName())
                || this.endSkippableElementName.equals(startElement.getName())
                && Objects.equals(this.identifier, getAttributeValue(startElement, WPML_ID));
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            throw new UnsupportedOperationException(OPERATION_IS_UNSUPPORTED);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            this.defaultSkippableElements.skip(startElementContext);

            if (this.startSkippableElementName.equals(startElementContext.getStartElement().getName())) {
                this.identifier = getAttributeValue(startElementContext.getStartElement(), WPML_ID);
            } else {
                this.identifier = null;
            }
        }
    }

    /**
     * Provides a bookmark skippable elements implementation.
     */
    final class BookmarkCrossStructure implements SkippableElements {
        static final String SKIPPABLE_BOOKMARK_NAME = "_GoBack";

        private final CrossStructure crossStructureSkippableElements;
        private final String bookmarkName;

        BookmarkCrossStructure(final CrossStructure crossStructureSkippableElements, final String bookmarkName) {
            this.crossStructureSkippableElements = crossStructureSkippableElements;
            this.bookmarkName = bookmarkName;
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return this.crossStructureSkippableElements.startSkippableElementName.equals(startElement.getName())
                    && this.bookmarkName.equals(getAttributeValue(startElement, WPML_NAME))
                || this.crossStructureSkippableElements.endSkippableElementName.equals(startElement.getName())
                    && (Objects.equals(this.crossStructureSkippableElements.identifier, getAttributeValue(startElement, WPML_ID)));
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            return this.crossStructureSkippableElements.canBeSkipped(endElement);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            this.crossStructureSkippableElements.skip(startElementContext);
        }
    }

    /**
     * Provides revision cross-structure skippable elements implementation.
     */
    final class RevisionCrossStructure implements SkippableElements {
        private static final Set<QName> REVISION_SKIPPABLE_ELEMENTS = new HashSet<>();
        static {
            REVISION_SKIPPABLE_ELEMENTS.addAll(SkippableElement.RevisionCrossStructure.toNames());
        }

        private final CrossStructure crossStructureSkippableElements;

        RevisionCrossStructure(final CrossStructure crossStructureSkippableElements) {
            this.crossStructureSkippableElements = crossStructureSkippableElements;
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return this.crossStructureSkippableElements.canBeSkipped(startElement, parentStartElement);
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            return this.crossStructureSkippableElements.canBeSkipped(endElement);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            if (!startElementContext.getConditionalParameters().getAutomaticallyAcceptRevisions()
                    && REVISION_SKIPPABLE_ELEMENTS.contains(startElementContext.getStartElement().getName())) {
                throw new OkapiUnexpectedRevisionException();
            }
            this.crossStructureSkippableElements.skip(startElementContext);
        }
    }

    /**
     * Provides revision cross-structure skippable elements implementation.
     */
    final class MoveFromRevisionCrossStructure implements SkippableElements {
        private final RevisionCrossStructure revisionCrossStructureSkippableElements;
        private final String structureName;
        private boolean structureCrossed;

        MoveFromRevisionCrossStructure(
            final RevisionCrossStructure revisionCrossStructureSkippableElements,
            final String structureName
        ) {
            this.revisionCrossStructureSkippableElements = revisionCrossStructureSkippableElements;
            this.structureName = structureName;
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            return this.revisionCrossStructureSkippableElements.canBeSkipped(startElement, parentStartElement);
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            return this.revisionCrossStructureSkippableElements.canBeSkipped(endElement);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            this.revisionCrossStructureSkippableElements.skip(startElementContext);

            while (startElementContext.getEventReader().hasNext()) {
                final XMLEvent e = startElementContext.getEventReader().nextEvent();

                if (e.isEndElement() && this.structureName.equals(e.asEndElement().getName().getLocalPart())) {
                    this.structureCrossed = true;
                }
                if (e.isStartElement() && this.structureName.equals(e.asStartElement().getName().getLocalPart())) {
                    this.structureCrossed = false;
                }
                if (e.isStartElement()
                        && this.revisionCrossStructureSkippableElements.crossStructureSkippableElements.endSkippableElementName.equals(e.asStartElement().getName())) {
                    this.revisionCrossStructureSkippableElements.skip(
                        StartElementContextFactory.createStartElementContext(e.asStartElement(), startElementContext)
                    );
                    return;
                }
            }

            throw new IllegalStateException(ExceptionMessages.UNEXPECTED_STRUCTURE);
        }

        boolean isStructureCrossed() {
            return this.structureCrossed;
        }
    }

    /**
     * Provides a property skippable elements implementation.
     */
    final class Property implements SkippableElements {
        private static final Map<QName, QName> CONTEXT_AWARE_PROPERTY_SKIPPABLE_ELEMENTS = new HashMap<>();
        static {
            CONTEXT_AWARE_PROPERTY_SKIPPABLE_ELEMENTS.put(
                Namespaces.WordProcessingML.getQName(RunProperties.RPR),
                SkippableElement.RunProperty.RUN_PROPERTY_CHARACTER_SPACING.toName()
            );
        }

        private final Default defaultSkippableElements;

        Property(final Default defaultSkippableElements, final ConditionalParameters conditionalParameters) {
            this.defaultSkippableElements = defaultSkippableElements;

            if (conditionalParameters.getCleanupAggressively()) {
                this.defaultSkippableElements.add(SkippableElement.RunProperty.RUN_PROPERTY_COMPLEX_SCRIPT_BOLD);
                this.defaultSkippableElements.add(SkippableElement.RunProperty.RUN_PROPERTY_COMPLEX_SCRIPT_ITALICS);
                this.defaultSkippableElements.add(SkippableElement.RunProperty.RUN_PROPERTY_CHARACTER_SPACING);
                this.defaultSkippableElements.add(SkippableElement.RunProperty.RUN_PROPERTY_COMPLEX_SCRIPT_FONT_SIZE);
                this.defaultSkippableElements.add(SkippableElement.RunProperty.RUN_PROPERTY_CHARACTER_WIDTH);
            }
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            if (!this.defaultSkippableElements.canBeSkipped(startElement, parentStartElement)) {
                return false;
            }
            if (isValuePresentInContextAwareSkippableElements(startElement, CONTEXT_AWARE_PROPERTY_SKIPPABLE_ELEMENTS)
                    && !isEntryPresentInContextAwareSkippableElements(startElement, parentStartElement, CONTEXT_AWARE_PROPERTY_SKIPPABLE_ELEMENTS)) {
                return false;
            }
            return true;
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            throw new UnsupportedOperationException(OPERATION_IS_UNSUPPORTED);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            this.defaultSkippableElements.skip(startElementContext);
        }
    }

    /**
     * Provides a revision property skippable elements implementation.
     */
    final class RevisionProperty implements SkippableElements {
        private static final QName LOCAL_NUMBERING_PROPERTIES = Namespaces.WordProcessingML.getQName("numPr");
        private static final Map<QName, QName> CONTEXT_AWARE_REVISION_SKIPPABLE_ELEMENTS = new HashMap<>();
        private static final Set<QName> REVISION_SKIPPABLE_ELEMENTS = new HashSet<>();

        static {
            CONTEXT_AWARE_REVISION_SKIPPABLE_ELEMENTS.put(
                LOCAL_NUMBERING_PROPERTIES,
                SkippableElement.RevisionProperty.RUN_PROPERTY_INSERTED_PARAGRAPH_MARK.toName()
            );
            CONTEXT_AWARE_REVISION_SKIPPABLE_ELEMENTS.put(
                Namespaces.WordProcessingML.getQName(BlockProperties.TBL_PR),
                SkippableElement.RevisionProperty.TABLE_PROPERTIES_CHANGE.toName()
            );
            CONTEXT_AWARE_REVISION_SKIPPABLE_ELEMENTS.put(
                Namespaces.WordProcessingML.getQName(LOCAL_TABLE_GRID),
                SkippableElement.RevisionProperty.TABLE_GRID_CHANGE.toName()
            );
            CONTEXT_AWARE_REVISION_SKIPPABLE_ELEMENTS.put(
                Namespaces.WordProcessingML.getQName(BlockProperties.TR_PR),
                SkippableElement.RevisionProperty.TABLE_ROW_INSERTED.toName()
            );
            CONTEXT_AWARE_REVISION_SKIPPABLE_ELEMENTS.put(
                Namespaces.WordProcessingML.getQName(BlockProperties.TR_PR),
                SkippableElement.RevisionProperty.TABLE_ROW_DELETED.toName()
            );

            REVISION_SKIPPABLE_ELEMENTS.addAll(SkippableElement.RevisionProperty.toNames());
        }

        private final Property propertySkippableElements;
        private final ConditionalParameters conditionalParameters;

        RevisionProperty(final Property propertySkippableElements, final ConditionalParameters conditionalParameters) {
            this.propertySkippableElements = propertySkippableElements;
            this.conditionalParameters = conditionalParameters;
        }

        @Override
        public boolean canBeSkipped(final StartElement startElement, final StartElement parentStartElement) {
            if (!this.propertySkippableElements.canBeSkipped(startElement, parentStartElement)) {
                return false;
            }
            if (!this.conditionalParameters.getAutomaticallyAcceptRevisions()
                    && isEntryPresentInContextAwareSkippableElements(startElement, parentStartElement, CONTEXT_AWARE_REVISION_SKIPPABLE_ELEMENTS)) {
                return false;
            }
            return true;
        }

        @Override
        public boolean canBeSkipped(final EndElement endElement) {
            return this.propertySkippableElements.canBeSkipped(endElement);
        }

        @Override
        public void skip(final StartElementContext startElementContext) throws XMLStreamException {
            if (!startElementContext.getConditionalParameters().getAutomaticallyAcceptRevisions()
                    && REVISION_SKIPPABLE_ELEMENTS.contains(startElementContext.getStartElement().getName())) {
                throw new OkapiUnexpectedRevisionException();
            }
            propertySkippableElements.skip(startElementContext);
        }
    }
}
