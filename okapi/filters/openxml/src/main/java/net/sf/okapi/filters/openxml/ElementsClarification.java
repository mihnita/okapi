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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

interface ElementsClarification {
    void performFor(final List<Property> properties);

    final class Bypass implements ElementsClarification {
        @Override
        public void performFor(final List<Property> properties) {
        }
    }

    final class TableBlockPropertyDefault implements ElementsClarification {
        private final ClarificationContext clarificationContext;
        private final String elementName;

        TableBlockPropertyDefault(
            final ClarificationContext clarificationContext,
            final String elementName
        ) {
            this.clarificationContext = clarificationContext;
            this.elementName = elementName;
        }

        @Override
        public void performFor(final List<Property> properties) {
            // todo provide support for table styles hierarchy processing
            final ListIterator<Property> iterator = properties.listIterator();
            while (iterator.hasNext()) {
                final Property blockProperty = iterator.next();
                if (this.elementName.equals(blockProperty.getName().getLocalPart())) {
                    if (!this.clarificationContext.targetRtl()) {
                        iterator.remove();
                        return;
                    }
                    return;
                }
            }
            if (clarificationContext.targetRtl()) {
                iterator.add(
                    new BlockProperty(
                        this.elementName,
                        Collections.emptyMap(),
                        this.clarificationContext.creationalParameters(),
                        this.clarificationContext.conditionalParameters(),
                        new StrippableAttributes.DrawingRunProperties(
                            this.clarificationContext.conditionalParameters(),
                            this.clarificationContext.creationalParameters().getEventFactory()
                        )
                    )
                );
            }
        }
    }

    final class ParagraphBlockPropertyDefault implements ElementsClarification {
        private final ClarificationContext clarificationContext;
        private final String elementName;
        private final String defaultValue;
        private final String defaultValueWhenAbsent;
        private final Set<String> falseValues;
        private final Set<String> trueValues;

        ParagraphBlockPropertyDefault(
            final ClarificationContext clarificationContext,
            final String elementName,
            final String defaultValue,
            final String defaultValueWhenAbsent,
            final Set<String> falseValues,
            final Set<String> trueValues
        ) {
            this.clarificationContext = clarificationContext;
            this.elementName = elementName;
            this.defaultValue = defaultValue;
            this.defaultValueWhenAbsent = defaultValueWhenAbsent;
            this.falseValues = falseValues;
            this.trueValues = trueValues;
        }

        @Override
        public void performFor(final List<Property> properties) {
            if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetLtr()
                || this.clarificationContext.sourceRtl() && this.clarificationContext.targetRtl()) {
                // source and target locales are both LTR or RTL - no clarification needed
                return;
            }
            final String combinedValue = this.clarificationContext.combinedParagraphProperties().properties().stream()
                .filter(p -> p.getName().getLocalPart().equals(this.elementName))
                .map(p -> {
                    final String value = p.value();
                    return null == value
                        ? this.defaultValue
                        : value;
                })
                .findFirst()
                .orElse(this.defaultValueWhenAbsent);
            if (this.falseValues.contains(combinedValue)) {
                if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetRtl()) {
                    // add/change to true
                    adjustPropertyValueToTrue(properties);
                }
            } else if (this.trueValues.contains(combinedValue)) {
                if (this.clarificationContext.sourceRtl() && this.clarificationContext.targetLtr()) {
                    // add/change to false
                    adjustPropertyValueToFalse(properties);
                }
            }
        }

        private void adjustPropertyValueToFalse(final List<Property> properties) {
            final Iterator<Property> iterator = properties.iterator();
            while (iterator.hasNext()) {
                final Property property = iterator.next();
                if (this.elementName.equals(property.getName().getLocalPart())) {
                    iterator.remove();
                    return;
                }
            }
            final String falseValue = this.falseValues.stream().findFirst().orElseThrow(
                () -> new IllegalStateException("True values are empty")
            );
            properties.add(
                new BlockProperty(
                    this.elementName,
                    values(falseValue),
                    this.clarificationContext.creationalParameters(),
                    this.clarificationContext.conditionalParameters(),
                    new StrippableAttributes.DrawingRunProperties(
                        this.clarificationContext.conditionalParameters(),
                        this.clarificationContext.creationalParameters().getEventFactory()
                    )
                )
            );
        }

        void adjustPropertyValueToTrue(final List<Property> properties) {
            final String trueValue = this.trueValues.stream().findFirst().orElseThrow(
                () -> new IllegalStateException("True values are empty")
            );
            final ListIterator<Property> iterator = properties.listIterator();
            while (iterator.hasNext()) {
                final Property runProperty = iterator.next();
                if (this.elementName.equals(runProperty.getName().getLocalPart())) {
                    iterator.set(
                        new BlockProperty(
                            this.elementName,
                            values(trueValue),
                            this.clarificationContext.creationalParameters(),
                            this.clarificationContext.conditionalParameters(),
                            new StrippableAttributes.DrawingRunProperties(
                                this.clarificationContext.conditionalParameters(),
                                this.clarificationContext.creationalParameters().getEventFactory()
                            )
                        )
                    );
                    return;
                }
            }
            properties.add(
                new BlockProperty(
                    this.elementName,
                    values(trueValue),
                    this.clarificationContext.creationalParameters(),
                    this.clarificationContext.conditionalParameters(),
                    new StrippableAttributes.DrawingRunProperties(
                        this.clarificationContext.conditionalParameters(),
                        this.clarificationContext.creationalParameters().getEventFactory()
                    )
                )
            );
        }

        private Map<String, String> values(final String trueValue) {
            final Map<String, String> values;
            if (this.defaultValue.contains(trueValue)) {
                values = Collections.emptyMap();
            } else {
                values = Collections.singletonMap("val", trueValue);
            }
            return values;
        }
    }

    final class RunPropertyDefault implements ElementsClarification {
        private final ClarificationContext clarificationContext;
        private final String elementName;
        private final String defaultValue;
        private final String defaultValueWhenAbsent;
        private final Set<String> falseValues;
        private final Set<String> trueValues;

        RunPropertyDefault(
            final ClarificationContext clarificationContext,
            final String elementName,
            final String defaultValue,
            final String defaultValueWhenAbsent,
            final Set<String> falseValues,
            final Set<String> trueValues
        ) {
            this.clarificationContext = clarificationContext;
            this.elementName = elementName;
            this.defaultValue = defaultValue;
            this.defaultValueWhenAbsent = defaultValueWhenAbsent;
            this.falseValues = falseValues;
            this.trueValues = trueValues;
        }

        @Override
        public void performFor(final List<Property> properties) {
            if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetLtr()
                || this.clarificationContext.sourceRtl() && this.clarificationContext.targetRtl()) {
                // source and target locales are both LTR or RTL - no clarification needed
                return;
            }
            final String combinedValue = this.clarificationContext.combinedRunProperties().properties().stream()
                .filter(p -> p.getName().getLocalPart().equals(this.elementName))
                .map(p -> {
                    final String value = p.value();
                    return null == value
                        ? this.defaultValue
                        : value;
                })
                .findFirst()
                .orElse(this.defaultValueWhenAbsent);
            if (this.falseValues.contains(combinedValue)) {
                if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetRtl()) {
                    // add/change to true
                    adjustPropertyValueToTrue(properties);
                }
            } else if (this.trueValues.contains(combinedValue)) {
                if (this.clarificationContext.sourceRtl() && this.clarificationContext.targetLtr()) {
                    // add/change to false
                    adjustPropertyValueToFalse(properties);
                }
            }
        }

        private void adjustPropertyValueToFalse(final List<Property> properties) {
            final Iterator<Property> iterator = properties.iterator();
            while (iterator.hasNext()) {
                final Property runProperty = iterator.next();
                if (this.elementName.equals(runProperty.getName().getLocalPart())) {
                    iterator.remove();
                    return;
                }
            }
            properties.add(
                RunPropertyFactory.createRunProperty(
                    this.clarificationContext.creationalParameters(),
                    this.elementName,
                    values(
                        this.falseValues.stream().findFirst().orElseThrow(
                            () -> new IllegalStateException("False values are empty")
                        )
                    )
                )
            );
        }

        void adjustPropertyValueToTrue(final List<Property> properties) {
            final String trueValue = this.trueValues.stream().findFirst().orElseThrow(
                () -> new IllegalStateException("True values are empty")
            );
            final ListIterator<Property> iterator = properties.listIterator();
            while (iterator.hasNext()) {
                final Property runProperty = iterator.next();
                if (this.elementName.equals(runProperty.getName().getLocalPart())) {
                    iterator.set(
                        RunPropertyFactory.createRunProperty(
                            this.clarificationContext.creationalParameters(),
                            this.elementName,
                            values(trueValue)
                        )
                    );
                    return;
                }
            }
            properties.add(
                RunPropertyFactory.createRunProperty(
                    this.clarificationContext.creationalParameters(),
                    this.elementName,
                    values(trueValue)
                )
            );
        }

        private Map<String, String> values(final String trueValue) {
            final Map<String, String> values;
            if (this.defaultValue.contains(trueValue)) {
                values = Collections.emptyMap();
            } else {
                values = Collections.singletonMap("val", trueValue);
            }
            return values;
        }
    }

    final class RunPropertyLang implements ElementsClarification {
        private final RunPropertyDefault runPropertyDefault;
        private final String elementName;
        private final String attributeName;

        RunPropertyLang(
            final RunPropertyDefault runPropertyDefault,
            final String elementName,
            final String attributeName
        ) {
            this.runPropertyDefault = runPropertyDefault;
            this.elementName = elementName;
            this.attributeName = attributeName;
        }

        @Override
        public void performFor(final List<Property> properties) {
            this.runPropertyDefault.performFor(properties);
            if (this.runPropertyDefault.clarificationContext.sourceLtr() && this.runPropertyDefault.clarificationContext.targetLtr()
                || this.runPropertyDefault.clarificationContext.sourceRtl() && this.runPropertyDefault.clarificationContext.targetRtl()) {
                // source and target locales are both LTR or RTL - no clarification needed
                return;
            }
            if (this.runPropertyDefault.clarificationContext.targetHasCharactersAsNumeralSeparators()) {
                properties.add(
                    RunPropertyFactory.createRunProperty(
                        this.runPropertyDefault.clarificationContext.creationalParameters(),
                        this.elementName,
                        Collections.singletonMap(
                            this.attributeName,
                            this.runPropertyDefault.clarificationContext.targetAsString()
                        )
                    )
                );
            }
        }
    }
}
