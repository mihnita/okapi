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
import net.sf.okapi.common.ParametersString;

/**
 * Parameters string font mapping implementation.
 */
public final class ParametersStringFontMapping implements FontMapping {
    static final String SOURCE_LOCALE_PATTERN = "sourceLocalePattern";
    static final String TARGET_LOCALE_PATTERN = "targetLocalePattern";
    static final String SOURCE_FONT_PATTERN = "sourceFontPattern";
    static final String TARGET_FONT = "targetFont";
    private static final String ANY_STRING_EXPRESSION = ".*";
    private static final String DEFAULT_SOURCE_LOCALE_PATTERN = ANY_STRING_EXPRESSION;
    private static final String DEFAULT_TARGET_LOCALE_PATTERN = ANY_STRING_EXPRESSION;
    private static final String DEFAULT_SOURCE_FONT_PATTERN = ANY_STRING_EXPRESSION;
    static final String DEFAULT_TARGET_FONT = "";

    private final ParametersString parametersString;

    private FontMapping defaultFontMapping;
    private boolean read;

    public ParametersStringFontMapping(final ParametersString parametersString) {
        this.parametersString = parametersString;
    }

    /**
     * Checks whether the font mapping is applicable to a locale pair.
     * <p>Empty target font name values are ignored.</p>
     * @param pair The locale pair
     * @return  {@code true} if the mapping is applicable to the locale pair
     * and {@code false} otherwise
     */
    @Override
    public boolean applicableTo(final LocalePair pair) {
        if (!this.read) {
            fromParametersString();
        }
        return this.defaultFontMapping.applicableTo(pair);
    }

    @Override
    public boolean applicableTo(final String sourceFont) {
        if (!this.read) {
            fromParametersString();
        }
        return this.defaultFontMapping.applicableTo(sourceFont);
    }

    @Override
    public String targetFont() {
        if (!this.read) {
            fromParametersString();
        }
        return this.defaultFontMapping.targetFont();
    }

    /**
     * Reads font mapping values from a parameters string.
     * <p>
     * The following values format is supported:
     * [sourceLocalePattern]
     * [targetLocalePattern]
     * [sourceFontPattern]
     * [targetFont]
     * <p>
     * The source locale pattern can be any supported regular expression.
     * When it is left empty, the font mapping is matched against any source
     * locale.
     * <p>
     * The target locale pattern can be any supported regular expression.
     * When it is left empty, the font mapping is matched against any target
     * locale.
     * <p>
     * The source font name pattern can be any supported regular expression.
     * When it is left empty, the mapping is applied to any available source
     * font.
     * <p>
     * The target font name can be a particular font name or can be left empty.
     * In the latter case the mapping will not be applied (ignored).
     */
    private void fromParametersString() {
        this.defaultFontMapping = new DefaultFontMapping(
            this.parametersString.getString(
                ParametersStringFontMapping.SOURCE_LOCALE_PATTERN,
                ParametersStringFontMapping.DEFAULT_SOURCE_LOCALE_PATTERN
            ),
            this.parametersString.getString(
                ParametersStringFontMapping.TARGET_LOCALE_PATTERN,
                ParametersStringFontMapping.DEFAULT_TARGET_LOCALE_PATTERN
            ),
            this.parametersString.getString(
                ParametersStringFontMapping.SOURCE_FONT_PATTERN,
                ParametersStringFontMapping.DEFAULT_SOURCE_FONT_PATTERN
            ),
            this.parametersString.getString(
                ParametersStringFontMapping.TARGET_FONT,
                ParametersStringFontMapping.DEFAULT_TARGET_FONT
            )
        );
        this.read = true;
    }

    @Override
    public <T> T writtenTo(final Output<T> output) {
        if (!this.read) {
            fromParametersString();
        }
        return this.defaultFontMapping.writtenTo(output);
    }
}
