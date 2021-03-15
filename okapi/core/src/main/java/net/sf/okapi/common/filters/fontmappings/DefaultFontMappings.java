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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default font mappings.
 */
public final class DefaultFontMappings implements FontMappings {

    private final List<FontMapping> mappings;

    public DefaultFontMappings(final FontMapping... mappings) {
        this(new LinkedList<>(Arrays.asList(mappings)));
    }

    public DefaultFontMappings(final List<FontMapping> mappings) {
        this.mappings = mappings;
    }

    /**
     * Returns applicable font mappings to a language pair in the specified
     * order.
     * @param pair The language pair
     * @return The applicable font mappings to the language pair
     */
    @Override
    public FontMappings applicableTo(final LocalePair pair) {
        return new DefaultFontMappings(
            this.mappings.stream()
                .filter(m -> m.applicableTo(pair))
                .collect(Collectors.toList())
        );
    }

    /**
     * Returns a target font for a source font.
     * The final target font value is determined by a sequential
     * substitution of the source font values. I.e.; if there is more than
     * one mapping;
     * 0. Arial -> Times New Roman
     * 1. Times New Roman -> Sans Serif
     * then the first mapping will produce Times New Roman replacement and the
     * second one will be applied to this new value, thus, ending up with the
     * Sans Serif.
     * @param sourceFont The source font
     * @return The target font
     */
    @Override
    public String targetFontFor(final String sourceFont) {
        String font = sourceFont;
        for (final FontMapping fontMapping : this.mappings) {
            if (fontMapping.applicableTo(font)) {
                font = fontMapping.targetFont();
            }
        }
        return font;
    }

    public void addFrom(final FontMappings.Input input) {
        final Iterator<FontMapping> iterator = input.read();
        while (iterator.hasNext()) {
            this.mappings.add(iterator.next());
        }
    }

    @Override
    public <T> T writtenTo(final Output<T> output) {
        return output.writtenWith(this.mappings.iterator());
    }
}
