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

import java.util.LinkedList;
import java.util.List;

/**
 * Default font mappings test suite.
 */
@RunWith(JUnit4.class)
public class DefaultFontMappingsTest {
    @Test
    public void initialisedAndWrittenToParametersStringFontMappingsOutput() {
        final FontMappings fontMappings = new DefaultFontMappings(
            new DefaultFontMapping(
                "en.*",
                "ru.*",
                "SourceFont.*",
                "TargetFont"
            ),
            new DefaultFontMapping(
                "ru",
                "fr",
                "Source font 2",
                "Target font 2"
            )
        );
        Assertions.assertThat(fontMappings.writtenTo(new ParametersStringFontMappingsOutput()))
            .extracting(ParametersString::toString)
            .isEqualTo(
                "#v1\n" +
                "fontMappings.0.sourceLocalePattern=en.*\n" +
                "fontMappings.0.targetLocalePattern=ru.*\n" +
                "fontMappings.0.sourceFontPattern=SourceFont.*\n" +
                "fontMappings.0.targetFont=TargetFont\n" +
                "fontMappings.1.sourceLocalePattern=ru\n" +
                "fontMappings.1.targetLocalePattern=fr\n" +
                "fontMappings.1.sourceFontPattern=Source font 2\n" +
                "fontMappings.1.targetFont=Target font 2\n" +
                "fontMappings.number.i=2"
            );
    }

    @Test
    public void notAddedFromIncompleteParametersStringFontMappingsInput() {
        final List<FontMapping> fontMappingList = new LinkedList<>();
        final FontMappings fontMappings = new DefaultFontMappings(fontMappingList);
        fontMappings.addFrom(
            new ParametersStringFontMappingsInput(
                new ParametersString(
                    "fontMappings.number.i=1\nfontMappings.0\n"
                )
            )
        );
        Assertions.assertThat(fontMappingList.size()).isEqualTo(0);
    }

    @Test
    public void addedFromParametersStringFontMappingsInputAndWrittenToParametersStringFontMappingsOutput() {
        final ParametersString ps = new ParametersString(
            "fontMappings.0.sourceLocalePattern=en\n" +
            "fontMappings.0.targetLocalePattern=ru\n" +
            "fontMappings.0.sourceFontPattern=Times.*\n" +
            "fontMappings.0.targetFont=Arial\n" +
            "fontMappings.number.i=1\n"
        );
        final FontMappings fontMappings = new DefaultFontMappings(new LinkedList<>());
        fontMappings.addFrom(new ParametersStringFontMappingsInput(ps));
        Assertions.assertThat(fontMappings.writtenTo(new ParametersStringFontMappingsOutput()))
            .extracting(ParametersString::toString)
            .isEqualTo(ps.toString());
    }

    @Test
    public void addedFromParametersStringFontMappingsInputAndApplicableToSpecificLanguagePairWrittenToParametersStringFontMappingsOutput() {
        final ParametersString ps = new ParametersString(
            "fontMappings.number.i=6\n"
            + "fontMappings.0.sourceFontPattern=Times.*\n"
            + "fontMappings.0.targetFont=:Arial Unicode MS\n"
            + "fontMappings.1.targetLocalePattern=ru"
            + "fontMappings.1.sourceFontPattern=The Sims Sans\n"
            + "fontMappings.1.targetFont=Arial Unicode MS\n"
            + "fontMappings.2.sourceLocalePattern=en\n"
            + "fontMappings.2.targetLocalePattern=ru\n"
            + "fontMappings.2.sourceFontPattern=Arial Unicode MS\n"
            + "fontMappings.2.targetFont=Times New Roman\n"
            + "fontMappings.3.sourceLocalePattern=en\n"
            + "fontMappings.3.targetLocalePattern=ru\n"
            + "fontMappings.3.sourceFontPattern=The Sims Sans:\n"
            + "fontMappings.4.sourceLocalePattern=ru\n"
            + "fontMappings.4.targetLocalePattern=en\n"
            + "fontMappings.4.sourceFontPattern=Times.*\n"
            + "fontMappings.4.targetFont=Times New Roman\n"
            + "fontMappings.5.sourceLocalePattern=ru\n"
            + "fontMappings.5.targetLocalePattern=en\n"
            + "fontMappings.5.sourceFontPattern=The Sims Sans"
        );
        final FontMappings fontMappings = new DefaultFontMappings(new LinkedList<>());
        fontMappings.addFrom(new ParametersStringFontMappingsInput(ps));
        final FontMappings applicableToPair = fontMappings.applicableTo(
            new DefaultLocalePair(LocaleId.ENGLISH, LocaleId.RUSSIAN)
        );
        Assertions.assertThat(applicableToPair.writtenTo(new ParametersStringFontMappingsOutput()))
            .extracting(ParametersString::toString)
            .isEqualTo(
                "#v1\n"
                + "fontMappings.0.sourceLocalePattern=.*\n"
                + "fontMappings.0.targetLocalePattern=.*\n"
                + "fontMappings.0.sourceFontPattern=Times.*\n"
                + "fontMappings.0.targetFont=:Arial Unicode MS\n"
                + "fontMappings.1.sourceLocalePattern=en\n"
                + "fontMappings.1.targetLocalePattern=ru\n"
                + "fontMappings.1.sourceFontPattern=Arial Unicode MS\n"
                + "fontMappings.1.targetFont=Times New Roman\n"
                + "fontMappings.number.i=2"
            );
    }
}
