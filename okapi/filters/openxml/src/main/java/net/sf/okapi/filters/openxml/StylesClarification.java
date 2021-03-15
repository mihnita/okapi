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

import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

/**
 * Styles clarification.
 */
interface StylesClarification {
    void performWith(final ListIterator<MarkupComponent> iterator);

    final class Word implements StylesClarification {
        private final BlockPropertiesClarification tablePropertiesClarification;
        private final BlockPropertiesClarification paragraphPropertiesClarification;
        private final RunPropertiesClarification runPropertiesClarification;
        private final Set<String> clarifiableWordStyleTypes;

        Word(
            final BlockPropertiesClarification tablePropertiesClarification,
            final BlockPropertiesClarification paragraphPropertiesClarification,
            final RunPropertiesClarification runPropertiesClarification
        ) {
            this(
                tablePropertiesClarification,
                paragraphPropertiesClarification,
                runPropertiesClarification,
                new HashSet<>(Arrays.asList(
                    StyleType.TABLE.toString(),
                    StyleType.PARAGRAPH.toString(),
                    StyleType.CHARACTER.toString()
                ))
            );
        }

        Word (
            final BlockPropertiesClarification tablePropertiesClarification,
            final BlockPropertiesClarification paragraphPropertiesClarification,
            final RunPropertiesClarification runPropertiesClarification,
            final Set<String> clarifiableWordStyleTypes
        ) {
            this.tablePropertiesClarification = tablePropertiesClarification;
            this.paragraphPropertiesClarification = paragraphPropertiesClarification;
            this.runPropertiesClarification = runPropertiesClarification;
            this.clarifiableWordStyleTypes = clarifiableWordStyleTypes;
        }

        @Override
        public void performWith(final ListIterator<MarkupComponent> iterator) {
            while (iterator.hasNext()) {
                final MarkupComponent component = iterator.next();

                if (MarkupComponent.isWordDocumentDefaultsStart(component)) {
                    clarifyWordDocumentDefaultsWith(iterator);
                } else if (MarkupComponent.isWordStyleStart(component)) {
                    clarifyWordStyleWith((MarkupComponent.Start) component, iterator);
                } else if (MarkupComponent.isWordStylesEnd(component)) {
                    break;
                }
            }
        }

        private void clarifyWordDocumentDefaultsWith(final ListIterator<MarkupComponent> iterator) {
            while (iterator.hasNext()) {
                final MarkupComponent component = iterator.next();

                if (MarkupComponent.isWordParagraphPropertiesDefaultStart(component)) {
                    this.paragraphPropertiesClarification.performWith(iterator);
                    this.clarifiableWordStyleTypes.remove(StyleType.PARAGRAPH.toString());
                } else if (MarkupComponent.isWordRunPropertiesDefaultStart(component)) {
                    clarifyDefaultRunPropertiesWith(iterator);
                    this.clarifiableWordStyleTypes.remove(StyleType.CHARACTER.toString());
                } else if (MarkupComponent.isWordDocumentDefaultsEnd(component)) {
                    break;
                }
            }
        }

        private void clarifyDefaultRunPropertiesWith(final ListIterator<MarkupComponent> iterator) {
            while (iterator.hasNext()) {
                final MarkupComponent component = iterator.next();

                if (MarkupComponent.isRunProperties(component)) {
                    this.runPropertiesClarification.performFor(component);
                    iterator.set(component);
                } else if (MarkupComponent.isWordRunPropertiesDefaultEnd(component)) {
                    break;
                }
            }
        }

        private void clarifyWordStyleWith(final MarkupComponent.Start startComponent, final ListIterator<MarkupComponent> iterator) {
            if (
                !startComponent.containsAttributeWithAnyOfValues(
                    WordStyleDefinition.General.TYPE,
                    clarifiableWordStyleTypes
                )
                    || !startComponent.containsAttributeWithAnyOfValues(
                    WordStyleDefinition.General.DEFAULT,
                    XMLEventHelpers.booleanAttributeTrueValues()
                )
            ) {
                return;
            }
            while (iterator.hasNext()) {
                final MarkupComponent component = iterator.next();

                if (MarkupComponent.isParagraphBlockProperties(component)) {
                    iterator.previous();
                    this.paragraphPropertiesClarification.performWith(iterator);
                } else if (MarkupComponent.isRunProperties(component)) {
                    this.runPropertiesClarification.performFor(component);
                    iterator.set(component);
                } else if (MarkupComponent.isTableBlockProperties(component)) {
                    iterator.previous();
                    this.tablePropertiesClarification.performWith(iterator);
                } else if (MarkupComponent.isWordStyleEnd(component)) {
                    break;
                }
            }
        }
    }
}
