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

import net.sf.okapi.common.DefaultLocalePair;
import net.sf.okapi.common.LocaleId;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * A default font mapping test suite.
 */
@RunWith(JUnit4.class)
public class DefaultFontMappingTest {
    private static final String SOURCE_LOCALE_STRING = "en.*";
    private static final String TARGET_LOCALE_STRING = "ru.*";
    private static final String SOURCE_FONT_STRING = "Times.*";
    private static final String TARGET_FONT = "Arial";

    @Test
    public void initialisedWithStrings() {
        new DefaultFontMapping(
            SOURCE_LOCALE_STRING,
            TARGET_LOCALE_STRING,
            SOURCE_FONT_STRING,
            TARGET_FONT
        );
    }

    @Test
    public void applicableToLocalePair() {
        final FontMapping fontMapping = new DefaultFontMapping(
            SOURCE_LOCALE_STRING,
            TARGET_LOCALE_STRING,
            SOURCE_FONT_STRING,
            TARGET_FONT
        );
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.ENGLISH, LocaleId.RUSSIAN))
        ).isTrue();
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(
                LocaleId.fromString("en-GB"),
                LocaleId.fromString("ru-RU"))
            )
        ).isTrue();
    }

    @Test
    public void notApplicableToLocalePair() {
        final FontMapping fontMapping = new DefaultFontMapping(
            SOURCE_LOCALE_STRING,
            TARGET_LOCALE_STRING,
            SOURCE_FONT_STRING,
            TARGET_FONT
        );
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.JAPANESE, LocaleId.RUSSIAN))
        ).isFalse();
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.ENGLISH, LocaleId.JAPANESE))
        ).isFalse();
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.JAPANESE, LocaleId.SPANISH))
        ).isFalse();
    }

    @Test
    public void notApplicableToLocalePairWithEmptyTargetFontProvided() {
        final FontMapping fontMapping = new DefaultFontMapping(
            SOURCE_LOCALE_STRING,
            TARGET_LOCALE_STRING,
            SOURCE_FONT_STRING,
            ""
        );
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.ENGLISH, LocaleId.RUSSIAN))
        ).isFalse();
    }

    @Test
    public void applicableToSourceFont() {
        final FontMapping fontMapping = new DefaultFontMapping(
            SOURCE_LOCALE_STRING,
            TARGET_LOCALE_STRING,
            SOURCE_FONT_STRING,
            TARGET_FONT
        );
        Assertions.assertThat(
            fontMapping.applicableTo("Times New Roman")
        ).isTrue();
    }

    @Test
    public void notApplicableToSourceFontWithEmptyTargetFontProvided() {
        final FontMapping fontMapping = new DefaultFontMapping(
            SOURCE_LOCALE_STRING,
            TARGET_LOCALE_STRING,
            SOURCE_FONT_STRING,
            ""
        );
        Assertions.assertThat(
            fontMapping.applicableTo("Times New Roman")
        ).isFalse();
    }
}
