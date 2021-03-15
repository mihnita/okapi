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
import net.sf.okapi.common.ParametersString;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * A parameters string font mapping test suite.
 */
@RunWith(JUnit4.class)
public class ParametersStringFontMappingTest {
    @Test
    public void initialisedWithDefaultValues() {
        final FontMapping fontMapping = new ParametersStringFontMapping(
            new ParametersString(
                "sourceLocalePattern=\ntargetLocalePattern=\nsourceFontPattern=\ntargetFont=\n"
            )
        );
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.ENGLISH, LocaleId.FRENCH))
        ).isFalse();
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.SPANISH, LocaleId.JAPANESE))
        ).isFalse();
        Assertions.assertThat(fontMapping.applicableTo("Any source font")).isFalse();
        Assertions.assertThat(fontMapping.targetFont()).isEmpty();
    }

    @Test
    public void initialisedWithDefaultValuesExceptingTargetFont() {
        final FontMapping fontMapping = new ParametersStringFontMapping(
            new ParametersString("targetFont=TargetFont\n")
        );
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.ENGLISH, LocaleId.FRENCH))
        ).isTrue();
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.SPANISH, LocaleId.JAPANESE))
        ).isTrue();
        Assertions.assertThat(fontMapping.applicableTo("Any source font")).isTrue();
        Assertions.assertThat(fontMapping.targetFont()).isEqualTo("TargetFont");
    }

    @Test
    public void initialisedWithNonDefaultValues() {
        final FontMapping fontMapping = new ParametersStringFontMapping(
            new ParametersString(
                "sourceLocalePattern=en-UK\ntargetLocalePattern=ru-RU\n" +
                    "sourceFontPattern=SourceFont\ntargetFont=TargetFont\n"
            )
        );
        Assertions.assertThat(fontMapping.applicableTo(
            new DefaultLocalePair(LocaleId.fromString("en-UK"), LocaleId.fromString("ru-RU"))
        )).isTrue();
        Assertions.assertThat(
            fontMapping.applicableTo(new DefaultLocalePair(LocaleId.ENGLISH, LocaleId.FRENCH))
        ).isFalse();
        Assertions.assertThat(fontMapping.applicableTo("SourceFont")).isTrue();
        Assertions.assertThat(fontMapping.applicableTo("anything")).isFalse();
        Assertions.assertThat(fontMapping.targetFont()).isEqualTo("TargetFont");
    }
}
