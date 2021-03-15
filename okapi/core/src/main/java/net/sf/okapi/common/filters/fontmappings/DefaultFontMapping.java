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
 * A default font mapping implementation.
 */
public final class DefaultFontMapping implements FontMapping {
    private final Pattern sourceLocalePattern;
    private final Pattern targetLocalePattern;
    private final Pattern sourceFontPattern;
    private final String targetFont;

    public DefaultFontMapping(
        final String sourceLocaleString,
        final String targetLocaleString,
        final String sourceFontString,
        final String targetFont
    ) {
        this(
            Pattern.compile(sourceLocaleString),
            Pattern.compile(targetLocaleString),
            Pattern.compile(sourceFontString),
            targetFont
        );
    }

    public DefaultFontMapping(
        final Pattern sourceLocalePattern,
        final Pattern targetLocalePattern,
        final Pattern sourceFontPattern,
        final String targetFont
    ) {
        this.sourceLocalePattern = sourceLocalePattern;
        this.targetLocalePattern = targetLocalePattern;
        this.sourceFontPattern = sourceFontPattern;
        this.targetFont = targetFont;
    }

    @Override
    public boolean applicableTo(final LocalePair pair) {
        return !this.targetFont.isEmpty()
            && this.sourceLocalePattern.matcher(pair.source().toString()).matches()
            && this.targetLocalePattern.matcher(pair.target().toString()).matches();
    }

    @Override
    public boolean applicableTo(final String sourceFont) {
        return !this.targetFont.isEmpty()
            && this.sourceFontPattern.matcher(sourceFont).matches();
    }

    @Override
    public String targetFont() {
        return this.targetFont;
    }

    @Override
    public <T> T writtenTo(final Output<T> output) {
        return output.writtenWith(
            this.sourceLocalePattern,
            this.targetLocalePattern,
            this.sourceFontPattern,
            this.targetFont
        );
    }
}
