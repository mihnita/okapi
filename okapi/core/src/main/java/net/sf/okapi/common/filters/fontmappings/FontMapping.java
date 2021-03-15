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

import java.util.regex.Pattern;

/**
 * A font mapping.
 */
public interface FontMapping {
    /**
     * Checks whether the font mapping is applicable to a language pair.
     * @param pair The language pair
     * @return {@code true} if the mapping is applicable to the language pair
     * and {@code false} otherwise
     */
    boolean applicableTo(final LocalePair pair);

    /**
     * Checks whether the font mapping is applicable to a source font.
     * @return {@code true} if the mapping is applicable to the source font
     * and {@code false} otherwise
     */
    boolean applicableTo(final String sourceFont);

    /**
     * Obtains a target font.
     * @return The target font name
     */
    String targetFont();

    /**
     * Obtains the font mapping output with the written font mapping to it.
     * @param output The output
     * @return The output with the written font mapping
     */
    <T> T writtenTo(final FontMapping.Output<T> output);

    /**
     * The font mapping output.
     * @param <T> The type of the output
     */
    interface Output<T> {
        /**
         * Obtains a written output with the help of provided source and
         * target languages, source and target fonts.
         * @param sourceLanguagePattern The source language pattern
         * @param targetLanguagePattern The target language pattern
         * @param sourceFontPattern The source font pattern
         * @param targetFont The target font
         * @return The written output
         */
        T writtenWith(
            final Pattern sourceLanguagePattern,
            final Pattern targetLanguagePattern,
            final Pattern sourceFontPattern,
            final String targetFont
        );
    }
}
