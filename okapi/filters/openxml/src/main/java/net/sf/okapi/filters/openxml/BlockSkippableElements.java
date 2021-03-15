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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;

/**
 * Represents block skippable elements and provides a way to skip them.
 */
final class BlockSkippableElements {
    private final StartElementContext startElementContext;
    private final SkippableElements insertedAndMovedToRunContentSkippableElements;
    private final SkippableElements deletedAndMovedFromRunContentAndProofingErrorSkippableElements;
    private final SkippableElements bookmarkSkippableElements;
    private final SkippableElements moveToRangeSkippableElements;
    private final SkippableElements.MoveFromRevisionCrossStructure moveFromRangeSkippableElements;

    BlockSkippableElements(final StartElementContext startElementContext) {
        this.startElementContext = startElementContext;

        this.insertedAndMovedToRunContentSkippableElements = new SkippableElements.RevisionInline(
            new SkippableElements.Inline(
                new SkippableElements.Default(
                    SkippableElement.RevisionInline.RUN_INSERTED_CONTENT,
                    SkippableElement.RevisionInline.RUN_MOVED_CONTENT_TO
                )
            )
        );
        this.deletedAndMovedFromRunContentAndProofingErrorSkippableElements = new SkippableElements.RevisionInline(
            new SkippableElements.Inline(
                new SkippableElements.Default(
                    SkippableElement.RevisionInline.RUN_DELETED_CONTENT,
                    SkippableElement.RevisionInline.RUN_MOVED_CONTENT_FROM,
                    SkippableElement.GeneralInline.PROOFING_ERROR_ANCHOR
                )
            )
        );
        this.bookmarkSkippableElements = new SkippableElements.BookmarkCrossStructure(
            new SkippableElements.CrossStructure(
                new SkippableElements.Default(
                    SkippableElement.GeneralCrossStructure.BOOKMARK_START,
                    SkippableElement.GeneralCrossStructure.BOOKMARK_END
                )
            ),
            SkippableElements.BookmarkCrossStructure.SKIPPABLE_BOOKMARK_NAME
        );
        this.moveToRangeSkippableElements = new SkippableElements.RevisionCrossStructure(
            new SkippableElements.CrossStructure(
                new SkippableElements.Default(
                    SkippableElement.RevisionCrossStructure.MOVE_TO_RANGE_START,
                    SkippableElement.RevisionCrossStructure.MOVE_TO_RANGE_END
                )
            )
        );
        this.moveFromRangeSkippableElements = new SkippableElements.MoveFromRevisionCrossStructure(
            new SkippableElements.RevisionCrossStructure(
                new SkippableElements.CrossStructure(
                    new SkippableElements.Default(
                        SkippableElement.RevisionCrossStructure.MOVE_FROM_RANGE_START,
                        SkippableElement.RevisionCrossStructure.MOVE_FROM_RANGE_END
                    )
                )
            ),
            startElementContext.getStartElement().getName().getLocalPart()
        );
    }

    /**
     * Skips events according to the configured skippable elements.
     *
     * @param event An XML event
     *
     * @return {@code true}  - if an element has been skipped
     *         {@code false} - otherwise
     *
     * @throws XMLStreamException
     */
    boolean skip(final XMLEvent event) throws XMLStreamException {
        if (event.isStartElement()
                && this.insertedAndMovedToRunContentSkippableElements.canBeSkipped(event.asStartElement(), this.startElementContext.getStartElement())) {
            this.insertedAndMovedToRunContentSkippableElements.skip(
                createStartElementContext(event.asStartElement(), this.startElementContext)
            );
            return true;
        }
        if (event.isEndElement() && this.insertedAndMovedToRunContentSkippableElements.canBeSkipped(event.asEndElement())) {
            this.insertedAndMovedToRunContentSkippableElements.skip(event.asEndElement());
            return true;
        }
        if (event.isStartElement()
                && this.deletedAndMovedFromRunContentAndProofingErrorSkippableElements.canBeSkipped(event.asStartElement(), this.startElementContext.getStartElement())) {
            this.deletedAndMovedFromRunContentAndProofingErrorSkippableElements.skip(
                createStartElementContext(event.asStartElement(), this.startElementContext)
            );
            return true;
        }
        if (event.isStartElement() && this.bookmarkSkippableElements.canBeSkipped(event.asStartElement(), null)) {
            this.bookmarkSkippableElements.skip(
                createStartElementContext(event.asStartElement(), this.startElementContext)
            );
            return true;
        }
        if (event.isStartElement() && this.moveToRangeSkippableElements.canBeSkipped(event.asStartElement(), null)) {
            this.moveToRangeSkippableElements.skip(
                createStartElementContext(event.asStartElement(), this.startElementContext)
            );
            return true;
        }
        if (event.isStartElement() && this.moveFromRangeSkippableElements.canBeSkipped(event.asStartElement(), null)) {
            this.moveFromRangeSkippableElements.skip(
                createStartElementContext(event.asStartElement(), this.startElementContext)
            );
            return true;
        }
        return false;
    }

    boolean isBorderCrossed() {
        return this.moveFromRangeSkippableElements.isStructureCrossed();
    }
}
