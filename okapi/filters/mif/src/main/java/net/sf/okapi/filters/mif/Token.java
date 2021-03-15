/*
 * =============================================================================
 *   Copyright (C) 2010-2020 by the Okapi Framework contributors
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
package net.sf.okapi.filters.mif;

/**
 * Identifies a token.
 */
interface Token {
    Type type();
    String toString();

    /**
     * A default token.
     */
    class Default implements Token {
        private final String value;
        private final Type type;

        Default(final String value, final Type type) {
            this.value = value;
            this.type = type;
        }

        Default(final StringBuilder stringBuilder, final Type type) {
            this(stringBuilder.toString(), type);
        }

        Default(final char value, final Type type) {
            this(String.valueOf(value), type);
        }

        @Override
        public Type type() {
            return this.type;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    /**
     * Token types.
     */
    enum Type {
        WHITESPACE,
        START,
        END,
        IDENTITY,
        LITERAL,
        STATEMENT
    }
}
