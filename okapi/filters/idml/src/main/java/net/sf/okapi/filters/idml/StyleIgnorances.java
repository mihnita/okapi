/*
 * =============================================================================
 *   Copyright (C) 2010-2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.idml;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Style ignorances.
 */
class StyleIgnorances {

    private final Map<AttributeName, Thresholds> attributes;
    private final Map<PropertyName, Thresholds> properties;

    StyleIgnorances(final Map<AttributeName, Thresholds> attributes, final Map<PropertyName, Thresholds> properties) {
        this.attributes = attributes;
        this.properties = properties;
    }

    boolean isAttributeNamePresent(final QName name) {
        return this.attributes.containsKey(AttributeName.fromQName(name));
    }

    void putAttribute(final AttributeName attributeName, final Thresholds thresholds) {
        this.attributes.put(attributeName, thresholds);
    }

    void removeAttribute(final AttributeName attributeName) {
        this.attributes.remove(attributeName);
    }

    boolean isPropertyNamePresent(final QName name) {
        return this.properties.containsKey(PropertyName.fromQName(name));
    }

    void putProperty(final PropertyName propertyName, final Thresholds thresholds) {
        this.properties.put(propertyName, thresholds);
    }

    void removeProperty(final PropertyName propertyName) {
        this.properties.remove(propertyName);
    }

    Thresholds thresholds(final QName name) {
        if (isAttributeNamePresent(name)) {
            return this.attributes.get(AttributeName.fromQName(name));
        } else if (isPropertyNamePresent(name)) {
            return this.properties.get(PropertyName.fromQName(name));
        } else {
            return Thresholds.empty();
        }
    }

    /**
     * Ignorable attribute names.
     */
    enum AttributeName {
        KERNING_METHOD("KerningMethod"),
        KERNING_VALUE("KerningValue"),
        TRACKING("Tracking"),
        BASELINE_SHIFT("BaselineShift"),

        UNSUPPORTED("");

        private final QName value;

        AttributeName(final String value) {
            this.value = Namespaces.getDefaultNamespace().getQName(value);
        }

        static AttributeName fromQName(final QName value) {
            for (AttributeName attributeName : values()) {
                if (attributeName.toQName().equals(value)) {
                    return attributeName;
                }
            }

            return UNSUPPORTED;
        }

        QName toQName() {
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    /**
     * Ignorable property names.
     */
    enum PropertyName {
        LEADING("Leading"),

        UNSUPPORTED("");

        private final QName value;

        PropertyName(final String value) {
            this.value = Namespaces.getDefaultNamespace().getQName(value);
        }

        static PropertyName fromQName(final QName value) {
            for (PropertyName propertyName : values()) {
                if (propertyName.toQName().equals(value)) {
                    return propertyName;
                }
            }

            return UNSUPPORTED;
        }

        QName toQName() {
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    static class Thresholds {

        private static final Type DEFAULT_TYPE = Type.STRING;
        private static final String DEFAULT_MIN = "";
        private static final String DEFAULT_MAX = "";

        private final Type type;
        private final String min;
        private final String max;

        static Thresholds empty() {
            return new Thresholds(DEFAULT_TYPE, DEFAULT_MIN, DEFAULT_MAX);
        }

        Thresholds(Type type, final String min, final String max) {
            this.type = type;
            this.min = min;
            this.max = max;
        }

        Type type() {
            return type;
        }

        String min() {
            return min;
        }

        String max() {
            return max;
        }

        boolean areEmpty() {
            return DEFAULT_TYPE == type
                    && DEFAULT_MIN.equals(min)
                    && DEFAULT_MAX.equals(max);
        }

        enum Type {
            INTEGER("Integer"),
            DOUBLE("Double"),
            STRING("String");

            private final String value;

            Type(final String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return value;
            }
        }
    }
}
