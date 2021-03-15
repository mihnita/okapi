/*
 * =============================================================================
 * Copyright (C) 2010-2021 by the Okapi Framework contributors
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

interface RunPropertiesClarification {
    void performFor(final MarkupComponent markupComponent);

    class Default implements RunPropertiesClarification {
        private final ClarificationContext clarificationContext;
        private final MarkupComponentClarification markupComponentClarification;

        Default(
            final ClarificationContext clarificationContext,
            final MarkupComponentClarification markupComponentClarification
        ) {
            this.clarificationContext = clarificationContext;
            this.markupComponentClarification = markupComponentClarification;
        }

        @Override
        public void performFor(final MarkupComponent markupComponent) {
            if (!(markupComponent instanceof RunProperties)) {
                throw new IllegalArgumentException("Unsupported instance given: ".concat(markupComponent.getClass().getSimpleName()));
            }
            this.clarificationContext.adjustCombinedRunPropertiesFor((RunProperties) markupComponent);
            this.markupComponentClarification.performFor(markupComponent);
        }
    }
}
