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

package net.sf.okapi.common;

import java.util.List;

/**
 * A string sanitiser.
 */
public final class StringSanitiser implements Sanitiser<String> {

    /**
     * Filters to apply when a string is being sanitised.
     */
    private final List<Filter<String>> filters;

    /**
     * Creates a string sanitiser.
     *
     * @param filters The filters
     */
    public StringSanitiser(final List<Filter<String>> filters) {
        this.filters = filters;
    }

    /**
     * Sanitises a value.
     *
     * @param value The value
     * @return A sanitised value
     */
    @Override
    public String sanitise(final String value) {
        String sanitisedValue = value;

        for (Sanitiser.Filter<String> filter : this.filters) {
            sanitisedValue = filter.apply(sanitisedValue);
        }

        return sanitisedValue;
    }

    /**
     * A trimming filter.
     */
    public static final class TrimmingFilter implements Sanitiser.Filter<String> {

        /**
         * Applies trimming to a value.
         *
         * @param value The value
         * @return A trimmed value
         */
        @Override
        public String apply(final String value) {
            return value.trim();
        }
    }

    /**
     * An integer parsing filter.
     */
    public static final class IntegerParsingFilter implements Sanitiser.Filter<String> {

        /**
         * An error message format.
         *
         * @see java.lang.String#format(java.lang.String, java.lang.Object...)
         */
        private final String errorMessageFormat;

        /**
         * Creates an integer parsing filter.
         *
         * @param errorMessageFormat The error message format
         */
        public IntegerParsingFilter(final String errorMessageFormat) {
            this.errorMessageFormat = errorMessageFormat;
        }

        /**
         * Applies {@link Integer#valueOf(String)} to a provided value.
         *
         * @param value The value
         * @return A parsed integer value as a string
         */
        @Override
        public String apply(final String value) {
            try {
                return Integer.valueOf(value).toString();
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(String.format(this.errorMessageFormat, value), exception);
            }
        }
    }

    /**
     * A double parsing filter.
     */
    public static final class DoubleParsingFilter implements Sanitiser.Filter<String> {

        /**
         * An error message format.
         *
         * @see java.lang.String#format(java.lang.String, java.lang.Object...)
         */
        private final String errorMessageFormat;

        /**
         * Creates a double parsing filter.
         *
         * @param errorMessageFormat The error message format
         */
        public DoubleParsingFilter(final String errorMessageFormat) {
            this.errorMessageFormat = errorMessageFormat;
        }

        /**
         * Applies {@link Double#valueOf(String)} to a provided value.
         *
         * @param value The value
         * @return A parsed double value as a string
         */
        @Override
        public String apply(final String value) {
            try {
                return Double.valueOf(value).toString();
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(String.format(this.errorMessageFormat, value), exception);
            }
        }
    }
}
