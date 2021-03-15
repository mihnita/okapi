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

import javax.xml.stream.events.Attribute;
import java.util.Collections;
import java.util.List;

/**
 * Provides a markup component clarification.
 */
interface MarkupComponentClarification {
    void performFor(final MarkupComponent markupComponent);

    final class Default implements MarkupComponentClarification {
        private final AttributesClarification attributesClarification;
        private final ElementsClarification elementsClarification;

        Default(
            final AttributesClarification attributesClarification,
            final ElementsClarification elementsClarification
        ) {
            this.attributesClarification = attributesClarification;
            this.elementsClarification = elementsClarification;
        }

        @Override
        public void performFor(final MarkupComponent markupComponent) {
            final List<Attribute> attributes;
            if (markupComponent instanceof MarkupComponent.Start) {
                attributes = ((MarkupComponent.Start) markupComponent).getAttributes();
            } else if (markupComponent instanceof MarkupComponent.EmptyElement) {
                attributes = ((MarkupComponent.EmptyElement) markupComponent).getAttributes();
            } else if (markupComponent instanceof BlockProperties) {
                attributes = ((BlockProperties) markupComponent).attributes();
            } else if (markupComponent instanceof RunProperties) {
                attributes = Collections.emptyList();
            } else {
                throw new IllegalArgumentException("Unsupported markup component: ".concat(markupComponent.getClass().getSimpleName()));
            }
            this.attributesClarification.performFor(attributes);
            if (markupComponent instanceof BlockProperties) {
                this.elementsClarification.performFor(((BlockProperties) markupComponent).properties());
            }
            if (markupComponent instanceof RunProperties) {
                this.elementsClarification.performFor(((RunProperties) markupComponent).properties());
            }
        }
    }
}
