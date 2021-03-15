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
import javax.xml.stream.events.StartElement;
import java.util.Objects;

interface Placeholder {
    String type();
    String index();

    final class Default implements Placeholder {
        private static final QName TYPE = new QName("type");
        private static final QName INDEX = new QName("idx");
        private static final String DEFAULT_TYPE = "obj";
        private static final String DEFAULT_INDEX = "0";

        private final String type;
        private final String index;

        Default(final StartElement startElement) {
            this(
                null == startElement.getAttributeByName(Default.TYPE)
                    ? Default.DEFAULT_TYPE
                    : startElement.getAttributeByName(Default.TYPE).getValue(),
                null == startElement.getAttributeByName(Default.INDEX)
                    ? Default.DEFAULT_INDEX
                    : startElement.getAttributeByName(Default.INDEX).getValue()
            );
        }

        Default(final String type, final String index) {
            this.type = type;
            this.index = index;
        }

        @Override
        public String type() {
            return this.type;
        }

        @Override
        public String index() {
            return this.index;
        }

        @Override
        public String toString() {
            return "{type:\"" + this.type + "\", index:\"" + this.index + "\"}";
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Default aDefault = (Default) o;
            return Objects.equals(this.type, aDefault.type) &&
                Objects.equals(this.index, aDefault.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.index);
        }
    }
}
