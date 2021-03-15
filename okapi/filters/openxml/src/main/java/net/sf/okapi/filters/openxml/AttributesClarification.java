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

import javax.xml.stream.events.Attribute;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

interface AttributesClarification {
    void performFor(final List<Attribute> attributes);

    final class Bypass implements AttributesClarification {
        @Override
        public void performFor(final List<Attribute> attributes) {
        }
    }

    final class Default implements AttributesClarification {
        private final ClarificationContext clarificationContext;
        private final ClarifiableAttribute clarifiableAttribute;

        Default(
            final ClarificationContext clarificationContext,
            final ClarifiableAttribute clarifiableAttribute
        ) {
            this.clarificationContext = clarificationContext;
            this.clarifiableAttribute = clarifiableAttribute;
        }

        @Override
        public void performFor(final List<Attribute> attributes) {
            final ListIterator<Attribute> iterator = attributes.listIterator();
            while (iterator.hasNext()) {
                final Attribute attribute = iterator.next();
                if (this.clarifiableAttribute.getName().equals(attribute.getName().getLocalPart())) {
                    if (!this.clarificationContext.targetRtl()) {
                        iterator.remove();
                        return;
                    }
                    if (this.clarifiableAttribute.getValues().contains(attribute.getValue())) {
                        return;
                    }
                    iterator.set(requiredAttribute());
                    return;
                }
            }
            if (this.clarificationContext.targetRtl()) {
                iterator.add(requiredAttribute());
            }
        }

        private Attribute requiredAttribute() {
            return this.clarificationContext.creationalParameters().getEventFactory().createAttribute(
                this.clarifiableAttribute.getPrefix(),
                this.clarificationContext.creationalParameters().getNamespaceUri(),
                this.clarifiableAttribute.getName(),
                this.clarifiableAttribute.getValues().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Clarifiable values are empty"))
            );
        }
    }

    final class Straightforward implements AttributesClarification {
        private final ClarificationContext clarificationContext;
        private final List<ClarifiableAttribute> clarifiableAttributes;

        Straightforward(
            final ClarificationContext clarificationContext,
            final List<ClarifiableAttribute> clarifiableAttributes
        ) {
            this.clarificationContext = clarificationContext;
            this.clarifiableAttributes = clarifiableAttributes;
        }

        @Override
        public void performFor(final List<Attribute> attributes) {
            final List<ClarifiableAttribute> tempAttributes = new LinkedList<>(clarifiableAttributes);
            final ListIterator<Attribute> attributesIterator = attributes.listIterator();
            while (attributesIterator.hasNext()) {
                final Attribute attribute = attributesIterator.next();
                final Iterator<ClarifiableAttribute> tempAttrsIterator = tempAttributes.iterator();
                while (tempAttrsIterator.hasNext()) {
                    final ClarifiableAttribute clarifiableAttribute = tempAttrsIterator.next();
                    if (attribute.getName().getLocalPart().equals(clarifiableAttribute.getName())) {
                        attributesIterator.set(attributeFrom(clarifiableAttribute));
                        tempAttrsIterator.remove();
                    }
                }
            }
            tempAttributes.forEach(ca -> attributes.add(attributeFrom(ca)));
        }

        private Attribute attributeFrom(final ClarifiableAttribute clarifiableAttribute) {
            return this.clarificationContext.creationalParameters().getEventFactory().createAttribute(
                clarifiableAttribute.getPrefix(),
                this.clarificationContext.creationalParameters().getNamespaceUri(),
                clarifiableAttribute.getName(),
                clarifiableAttribute.getValues().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Clarifiable values are empty"))
            );
        }
    }

    final class AlignmentAndRtl implements AttributesClarification {
        private final ClarificationContext clarificationContext;
        private final String prefix;
        private final String alignmentName;
        private final String alignmentLeft;
        private final String alignmentRight;
        private final String rtlName;
        private final Set<String> falseValues;
        private final Set<String> trueValues;

        AlignmentAndRtl(
            final ClarificationContext clarificationContext,
            final String prefix,
            final String alignmentName,
            final String alignmentLeft,
            final String alignmentRight,
            final String rtlName,
            final Set<String> falseValues,
            final Set<String> trueValues
        ) {
            this.clarificationContext = clarificationContext;
            this.prefix = prefix;
            this.alignmentName = alignmentName;
            this.alignmentLeft = alignmentLeft;
            this.alignmentRight = alignmentRight;
            this.rtlName = rtlName;
            this.falseValues = falseValues;
            this.trueValues = trueValues;
        }

        @Override
        public void performFor(final List<Attribute> attributes) {
            if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetLtr()
                || this.clarificationContext.sourceRtl() && this.clarificationContext.targetRtl()) {
                // source and target locales are both LTR or RTL - no clarification needed
                return;
            }
            final String combinedAlignment = this.clarificationContext.combinedParagraphProperties().attributes().stream()
                .filter(a -> a.getName().getLocalPart().equals(this.alignmentName))
                .map(a -> a.getValue())
                .findFirst()
                .orElse(this.alignmentLeft);
            final String combinedRtl = this.clarificationContext.combinedParagraphProperties().attributes().stream()
                .filter(a -> a.getName().getLocalPart().equals(this.rtlName))
                .map(a -> a.getValue())
                .findFirst()
                .orElse(
                    this.falseValues.stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("False values are empty"))
                );
            if (combinedAlignment.equals(this.alignmentLeft) && this.falseValues.contains(combinedRtl)) {
                if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetRtl()) {
                    // add/change alignment - right, add/change rtl - true
                    new Straightforward(
                        this.clarificationContext,
                        Arrays.asList(
                            new ClarifiableAttribute(
                                this.prefix,
                                this.alignmentName,
                                Collections.singleton(this.alignmentRight)
                            ),
                            new ClarifiableAttribute(
                                this.prefix,
                                this.rtlName,
                                this.trueValues
                            )
                        )
                    ).performFor(attributes);
                }
            } else if (combinedAlignment.equals(this.alignmentLeft) && this.trueValues.contains(combinedRtl)) {
                if (this.clarificationContext.sourceRtl() && this.clarificationContext.targetLtr()) {
                    // add/change alignment - right, add/change rtl - false
                    new Straightforward(
                        this.clarificationContext,
                        Arrays.asList(
                            new ClarifiableAttribute(
                                this.prefix,
                                this.alignmentName,
                                Collections.singleton(this.alignmentRight)
                            ),
                            new ClarifiableAttribute(
                                this.prefix,
                                this.rtlName,
                                this.falseValues
                            )
                        )
                    ).performFor(attributes);
                }
            } else if (combinedAlignment.equals(this.alignmentRight) && this.falseValues.contains(combinedRtl)) {
                if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetRtl()) {
                    // add/change alignment - left, add/change rtl - true
                    new Straightforward(
                        this.clarificationContext,
                        Arrays.asList(
                            new ClarifiableAttribute(
                                this.prefix,
                                this.alignmentName,
                                Collections.singleton(this.alignmentLeft)
                            ),
                            new ClarifiableAttribute(
                                this.prefix,
                                this.rtlName,
                                this.trueValues
                            )
                        )
                    ).performFor(attributes);
                }
            } else if (combinedAlignment.equals(this.alignmentRight) && this.trueValues.contains(combinedRtl)) {
                if (this.clarificationContext.sourceRtl() && this.clarificationContext.targetLtr()) {
                    // add/change alignment - left, add/change rtl - false
                    new Straightforward(
                        this.clarificationContext,
                        Arrays.asList(
                            new ClarifiableAttribute(
                                this.prefix,
                                this.alignmentName,
                                Collections.singleton(this.alignmentLeft)
                            ),
                            new ClarifiableAttribute(
                                this.prefix,
                                this.rtlName,
                                this.falseValues
                            )
                        )
                    ).performFor(attributes);
                }
            } else if (!combinedAlignment.equals(this.alignmentLeft) && !combinedAlignment.equals(this.alignmentRight)
                && this.falseValues.contains(combinedRtl)) {
                if (this.clarificationContext.sourceLtr() && this.clarificationContext.targetRtl()) {
                    // keep alignment, add/change rtl - true
                    new Straightforward(
                        this.clarificationContext,
                        Collections.singletonList(
                            new ClarifiableAttribute(
                                this.prefix,
                                this.rtlName,
                                this.trueValues
                            )
                        )
                    ).performFor(attributes);
                }
            } else if (!combinedAlignment.equals(this.alignmentLeft) && !combinedAlignment.equals(this.alignmentRight)
                && this.trueValues.contains(combinedRtl)) {
                if (this.clarificationContext.sourceRtl() && this.clarificationContext.targetLtr()) {
                    // keep alignment, add/change rtl - false
                    new Straightforward(
                        this.clarificationContext,
                        Collections.singletonList(
                            new ClarifiableAttribute(
                                this.prefix,
                                this.rtlName,
                                this.falseValues
                            )
                        )
                    ).performFor(attributes);
                }
            }
        }
    }
}
