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

/**
 * A sanitiser.
 *
 * @param <T> A value type to sanitise
 */
public interface Sanitiser<T> {

    /**
     * Sanitises a value.
     *
     * @param value The value
     * @return A sanitised value
     */
    T sanitise(T value);

    /**
     * Sanitiser's filter.
     *
     * @param <T> A value type to filter
     */
    interface Filter<T> {

        /**
         * Applies a filter to a provided value.
         *
         * @param value The value
         * @return A filtered value
         */
        T apply(T value);
    }
}
