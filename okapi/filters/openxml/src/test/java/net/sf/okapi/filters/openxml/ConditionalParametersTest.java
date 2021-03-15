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
package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.filters.fontmappings.DefaultFontMapping;
import net.sf.okapi.common.filters.fontmappings.DefaultFontMappings;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConditionalParametersTest {
    @Test
    public void defaultValuesExposedAsString() {
        Assert.assertEquals(
            "#v1\n" +
            "maxAttributeSize.i=4194304\n" +
            "bPreferenceTranslateDocProperties.b=true\n" +
            "bPreferenceTranslateComments.b=true\n" +
            "bPreferenceTranslatePowerpointNotes.b=true\n" +
            "bPreferenceTranslatePowerpointMasters.b=true\n" +
            "bPreferenceIgnorePlaceholdersInPowerpointMasters.b=false\n" +
            "bPreferenceTranslateWordHeadersFooters.b=true\n" +
            "bPreferenceTranslateWordHidden.b=false\n" +
            "bPreferenceTranslateWordExcludeGraphicMetaData.b=false\n" +
            "bPreferenceTranslatePowerpointHidden.b=false\n" +
            "bPreferenceTranslateExcelHidden.b=false\n" +
            "bPreferenceTranslateExcelExcludeColors.b=false\n" +
            "bPreferenceTranslateExcelExcludeColumns.b=false\n" +
            "bPreferenceTranslateExcelSheetNames.b=false\n" +
            "bPreferenceAddLineSeparatorAsCharacter.b=false\n" +
            "sPreferenceLineSeparatorReplacement=$0a$\n" +
            "bPreferenceReplaceNoBreakHyphenTag.b=false\n" +
            "bPreferenceIgnoreSoftHyphenTag.b=false\n" +
            "bPreferenceAddTabAsCharacter.b=false\n" +
            "bPreferenceAggressiveCleanup.b=false\n" +
            "bPreferenceAutomaticallyAcceptRevisions.b=true\n" +
            "bPreferencePowerpointIncludedSlideNumbersOnly.b=false\n" +
            "bPreferenceTranslateExcelDiagramData.b=false\n" +
            "bPreferenceTranslateExcelDrawings.b=false\n" +
            "subfilter=\n" +
            "bInExcludeMode.b=true\n" +
            "bInExcludeHighlightMode.b=true\n" +
            "bPreferenceTranslateWordExcludeColors.b=false\n" +
            "bReorderPowerpointNotesAndComments.b=false\n" +
            "tsComplexFieldDefinitionsToExtract.i=1\n" +
            "cfd0=HYPERLINK\n" +
            "tsExcelExcludedColors.i=0\n" +
            "tsExcelExcludedColumns.i=0\n" +
            "tsExcludeWordStyles.i=0\n" +
            "tsWordHighlightColors.i=0\n" +
            "tsWordExcludedColors.i=0\n" +
            "tsPowerpointIncludedSlideNumbers.i=0",
            new ConditionalParameters().toString()
        );
    }

    @Test
    public void fontMappingsExposedAsString() {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.fontMappings(
            new DefaultFontMappings(
                new DefaultFontMapping(".*", ".*", "Times.*", "Arial")
            )
        );
        Assert.assertEquals(
            "#v1\n" +
            "maxAttributeSize.i=4194304\n" +
            "bPreferenceTranslateDocProperties.b=true\n" +
            "bPreferenceTranslateComments.b=true\n" +
            "bPreferenceTranslatePowerpointNotes.b=true\n" +
            "bPreferenceTranslatePowerpointMasters.b=true\n" +
            "bPreferenceIgnorePlaceholdersInPowerpointMasters.b=false\n" +
            "bPreferenceTranslateWordHeadersFooters.b=true\n" +
            "bPreferenceTranslateWordHidden.b=false\n" +
            "bPreferenceTranslateWordExcludeGraphicMetaData.b=false\n" +
            "bPreferenceTranslatePowerpointHidden.b=false\n" +
            "bPreferenceTranslateExcelHidden.b=false\n" +
            "bPreferenceTranslateExcelExcludeColors.b=false\n" +
            "bPreferenceTranslateExcelExcludeColumns.b=false\n" +
            "bPreferenceTranslateExcelSheetNames.b=false\n" +
            "bPreferenceAddLineSeparatorAsCharacter.b=false\n" +
            "sPreferenceLineSeparatorReplacement=$0a$\n" +
            "bPreferenceReplaceNoBreakHyphenTag.b=false\n" +
            "bPreferenceIgnoreSoftHyphenTag.b=false\n" +
            "bPreferenceAddTabAsCharacter.b=false\n" +
            "bPreferenceAggressiveCleanup.b=false\n" +
            "bPreferenceAutomaticallyAcceptRevisions.b=true\n" +
            "bPreferencePowerpointIncludedSlideNumbersOnly.b=false\n" +
            "bPreferenceTranslateExcelDiagramData.b=false\n" +
            "bPreferenceTranslateExcelDrawings.b=false\n" +
            "subfilter=\n" +
            "bInExcludeMode.b=true\n" +
            "bInExcludeHighlightMode.b=true\n" +
            "bPreferenceTranslateWordExcludeColors.b=false\n" +
            "bReorderPowerpointNotesAndComments.b=false\n" +
            "tsComplexFieldDefinitionsToExtract.i=1\n" +
            "cfd0=HYPERLINK\n" +
            "tsExcelExcludedColors.i=0\n" +
            "tsExcelExcludedColumns.i=0\n" +
            "tsExcludeWordStyles.i=0\n" +
            "tsWordHighlightColors.i=0\n" +
            "tsWordExcludedColors.i=0\n" +
            "tsPowerpointIncludedSlideNumbers.i=0\n" +
            "fontMappings.0.sourceLocalePattern=.*\n" +
            "fontMappings.0.targetLocalePattern=.*\n" +
            "fontMappings.0.sourceFontPattern=Times.*\n" +
            "fontMappings.0.targetFont=Arial\n" +
            "fontMappings.number.i=1",
            conditionalParameters.toString()
        );
    }
}
