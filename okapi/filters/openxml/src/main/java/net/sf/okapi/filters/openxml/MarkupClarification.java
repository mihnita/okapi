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
package net.sf.okapi.filters.openxml;

import java.util.ListIterator;

/**
 * Provides a markup clarification.
 */
class MarkupClarification {
    private final MarkupComponentClarification sheetViewClarification;
    private final MarkupComponentClarification alignmentClarification;
    private final MarkupComponentClarification presentationClarification;
    private final BlockPropertiesClarification tablePropertiesClarification;
    private final BlockPropertiesClarification textBodyPropertiesClarification;
    private final BlockPropertiesClarification paragraphPropertiesClarification;
    private final StylesClarification wordStylesClarification;

    /**
     * Constructs the markup clarification.
     * @param sheetViewClarification The sheet view clarification
     * @param alignmentClarification The alignment clarification
     * @param presentationClarification The presentation clarification
     * @param tablePropertiesClarification The table properties clarification
     * @param textBodyPropertiesClarification The text body properties clarification
     * @param paragraphPropertiesClarification The paragraph properties clarification
     * @param wordStylesClarification The word styles clarification
     */
    MarkupClarification(
        final MarkupComponentClarification sheetViewClarification,
        final MarkupComponentClarification alignmentClarification,
        final MarkupComponentClarification presentationClarification,
        final BlockPropertiesClarification tablePropertiesClarification,
        final BlockPropertiesClarification textBodyPropertiesClarification,
        final BlockPropertiesClarification paragraphPropertiesClarification,
        final StylesClarification wordStylesClarification
    ) {
        this.sheetViewClarification = sheetViewClarification;
        this.alignmentClarification = alignmentClarification;
        this.presentationClarification = presentationClarification;
        this.tablePropertiesClarification = tablePropertiesClarification;
        this.textBodyPropertiesClarification = textBodyPropertiesClarification;
        this.paragraphPropertiesClarification = paragraphPropertiesClarification;
        this.wordStylesClarification = wordStylesClarification;
    }

    /**
     * Performs for markup.
     */
    void performFor(Markup markup) {
        ListIterator<MarkupComponent> iterator = markup.components().listIterator();

        while (iterator.hasNext()) {
            final MarkupComponent component = iterator.next();
            if (MarkupComponent.isSheetViewStart(component)) {
                this.sheetViewClarification.performFor(component);
            } else if (MarkupComponent.isAlignmentEmptyElement(component)) {
                this.alignmentClarification.performFor(component);
            } else if (MarkupComponent.isPresentationStart(component)) {
                this.presentationClarification.performFor(component);
            } else if (MarkupComponent.isTableStart(component)) {
                this.tablePropertiesClarification.performWith(iterator);
            } else if (MarkupComponent.isTextBodyStart(component)) {
                this.textBodyPropertiesClarification.performWith(iterator);
            } else if (MarkupComponent.isParagraphStart(component)) {
                this.paragraphPropertiesClarification.performWith(iterator);
            } else if (MarkupComponent.isWordStylesStart(component)) {
                this.wordStylesClarification.performWith(iterator);
            }
        }
    }
}
