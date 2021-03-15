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

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

interface NonVisualIdentificationPropertyFragments {
    String id();
    boolean hidden();

    final class Default implements NonVisualIdentificationPropertyFragments {
        private static final String ID = "id";
        private static final String HIDDEN = "hidden";

        private final StartElement startElement;
        private String id;
        private boolean hidden;

        Default(final StartElement startElement) {
            this.startElement = startElement;
        }

        @Override
        public String id() {
            if (null == this.id) {
                final Attribute attribute = this.startElement.getAttributeByName(new QName(Default.ID));
                if (null == attribute) {
                    throw new IllegalStateException("The non-visual drawing properties ID is required");
                }
                this.id = attribute.getValue();
            }
            return this.id;
        }

        @Override
        public boolean hidden() {
            final Attribute attribute = this.startElement.getAttributeByName(new QName(Default.HIDDEN));
            if (null != attribute) {
                this.hidden = Boolean.valueOf(attribute.getValue());
            }
            return this.hidden;
        }
    }
}
