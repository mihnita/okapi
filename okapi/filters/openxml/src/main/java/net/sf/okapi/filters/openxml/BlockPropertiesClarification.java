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

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Provides a block properties clarification.
 */
interface BlockPropertiesClarification {
    void performWith(final ListIterator<MarkupComponent> markupComponentIterator);

    final class Default implements BlockPropertiesClarification {
        private final ClarificationContext clarificationContext;
        private final String blockPropertiesName;
        private final MarkupComponentClarification markupComponentClarification;

        Default(
            final ClarificationContext clarificationContext,
            final String blockPropertiesName,
            final MarkupComponentClarification markupComponentClarification
        ) {
            this.clarificationContext = clarificationContext;
            this.blockPropertiesName = blockPropertiesName;
            this.markupComponentClarification = markupComponentClarification;
        }

        @Override
        public void performWith(final ListIterator<MarkupComponent> markupComponentIterator) {
            MarkupComponent markupComponent;
            if (!markupComponentIterator.hasNext()) {
                // the block is the very last component
                markupComponent = blockProperties();
                markupComponentIterator.add(markupComponent);
            } else {
                markupComponent = markupComponentIterator.next();
                if (markupComponent instanceof MarkupComponent.General
                    && ((MarkupComponent.General) markupComponent).eventsAreWhitespaces()) {
                    if (!markupComponentIterator.hasNext()) {
                        markupComponent = blockProperties();
                        markupComponentIterator.add(markupComponent);
                    } else {
                        markupComponent = markupComponentIterator.next();
                    }
                }
                if (!(markupComponent instanceof BlockProperties)
                    || !blockPropertiesName.equals(((BlockProperties) markupComponent).getName().getLocalPart())) {
                    // block properties must be the very first after the start of a block
                    markupComponentIterator.previous();
                    markupComponent = blockProperties();
                    markupComponentIterator.add(markupComponent);
                }
            }
            if (markupComponent instanceof ParagraphBlockProperties) {
                this.clarificationContext.adjustCombinedParagraphPropertiesAndParagraphStyleFor((ParagraphBlockProperties) markupComponent);
            }
            this.markupComponentClarification.performFor(markupComponent);
        }

        private BlockProperties blockProperties() {
            return BlockPropertiesFactory.createBlockProperties(
                this.clarificationContext.conditionalParameters(),
                this.clarificationContext.creationalParameters(),
                this.blockPropertiesName,
                new ArrayList<>(),
                new ArrayList<>()
            );
        }
    }

    final class Paragraph implements BlockPropertiesClarification {
        private final BlockPropertiesClarification.Default defaultBlockPropertiesClarification;

        Paragraph(final BlockPropertiesClarification.Default defaultBlockPropertiesClarification) {
            this.defaultBlockPropertiesClarification = defaultBlockPropertiesClarification;
        }

        @Override
        public void performWith(final ListIterator<MarkupComponent> markupComponentIterator) {
            final MarkupComponent markupComponent = markupComponentIterator.previous();
            if (markupComponent instanceof MarkupComponent.ParagraphStart) {
                this.defaultBlockPropertiesClarification.clarificationContext.adjust(
                    ((MarkupComponent.ParagraphStart) markupComponent).styleDefinitions()
                );
            } else {
                this.defaultBlockPropertiesClarification.clarificationContext.adjust(
                    new StyleDefinitions.Empty()
                );
            }
            markupComponentIterator.next();
            this.defaultBlockPropertiesClarification.performWith(markupComponentIterator);
        }
    }
}
