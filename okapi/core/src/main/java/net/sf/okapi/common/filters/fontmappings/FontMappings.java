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
package net.sf.okapi.common.filters.fontmappings;

import net.sf.okapi.common.LocalePair;

import java.util.Iterator;

/**
 * Font mappings.
 */
public interface FontMappings {
    /**
     * Returns applicable font mappings to a language pair.
     * @param pair The language pair
     * @return The applicable font mappings to the language pair
     */
    FontMappings applicableTo(final LocalePair pair);

    /**
     * Returns a target font for a source font.
     * @param sourceFont The source font
     * @return The target font
     */
    String targetFontFor(final String sourceFont);

    /**
     * Adds font mappings from input.
     * @param input The font mappings input to add from
     */
    void addFrom(final FontMappings.Input input);

    /**
     * Obtains the font mappings output with the written font mappings to it.
     * @param output The output
     * @return The output with the written font mappings
     */
    <T> T writtenTo(final FontMappings.Output<T> output);

    /**
     * The font mappings input.
     */
    interface Input {
        /**
         * Obtains an iterator of the font mappings, which have been read
         * from the input.
         * @return The font mappings iterator
         */
        Iterator<FontMapping> read();
    }

    /**
     * The font mappings output.
     * @param <T> The type of the output
     */
    interface Output<T> {
        /**
         * Obtains a written output with the help of a provided font
         * mappings iterator.
         * @param fontMappingsIterator The font mappings iterator
         * @return The written output
         */
        T writtenWith(final Iterator<FontMapping> fontMappingsIterator);
    }
}
