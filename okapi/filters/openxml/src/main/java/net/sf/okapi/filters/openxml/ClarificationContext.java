/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.LocaleId;

/**
 * Provides clarification context.
 */
class ClarificationContext {
    private final ConditionalParameters conditionalParameters;
    private final CreationalParameters creationalParameters;
    private final LocaleId sourceLocale;
    private final LocaleId targetLocale;
    private Boolean sourceRtl;
    private Boolean targetRtl;
    private Boolean targetHasCharactersAsNumeralSeparators;
    private StyleDefinitions styleDefinitions;
    private ParagraphBlockProperties combinedParagraphProperties;
    private String paragraphStyle;
    private RunProperties combinedRunProperties;

    ClarificationContext(
        final ConditionalParameters conditionalParameters,
        final CreationalParameters creationalParameters,
        final LocaleId sourceLocale,
        final LocaleId targetLocale
    ) {
        this.conditionalParameters = conditionalParameters;
        this.creationalParameters = creationalParameters;
        this.sourceLocale = sourceLocale;
        this.targetLocale = targetLocale;
    }

    ConditionalParameters conditionalParameters() {
        return this.conditionalParameters;
    }

    CreationalParameters creationalParameters() {
        return this.creationalParameters;
    }

    boolean sourceLtr() {
        return !sourceRtl();
    }

    boolean sourceRtl() {
        if (null == this.sourceRtl) {
            this.sourceRtl = LocaleId.isBidirectional(this.sourceLocale);
        }
        return this.sourceRtl;
    }

    boolean targetLtr() {
        return !targetRtl();
    }

    boolean targetRtl() {
        if (null == this.targetRtl) {
            this.targetRtl = LocaleId.isBidirectional(this.targetLocale);
        }
        return this.targetRtl;
    }

    boolean targetHasCharactersAsNumeralSeparators() {
        if (null == this.targetHasCharactersAsNumeralSeparators) {
            this.targetHasCharactersAsNumeralSeparators =
                LocaleId.hasCharactersAsNumeralSeparators(this.targetLocale);
        }
        return this.targetHasCharactersAsNumeralSeparators;
    }

    String targetAsString() {
        return this.targetLocale.toString();
    }

    void adjust(final StyleDefinitions styleDefinitions) {
        this.styleDefinitions = styleDefinitions;
    }

    private StyleDefinitions styleDefinitions() {
        if (null == this.styleDefinitions) {
            this.styleDefinitions = new StyleDefinitions.Empty();
        }
        return this.styleDefinitions;
    }

    void adjustCombinedParagraphPropertiesAndParagraphStyleFor(final ParagraphBlockProperties paragraphProperties) {
        this.combinedParagraphProperties = this.styleDefinitions.combinedParagraphBlockProperties(
            paragraphProperties
        );
        this.paragraphStyle = paragraphProperties.paragraphStyle();
    }

    ParagraphBlockProperties combinedParagraphProperties() {
        return this.combinedParagraphProperties;
    }

    void adjustCombinedRunPropertiesFor(final RunProperties runProperties) {
        final RunProperty runStyleProperty = runProperties.getRunStyleProperty();
        this.combinedRunProperties = styleDefinitions().combinedRunProperties(
            this.paragraphStyle,
            null == runStyleProperty ? null : runStyleProperty.value(),
            runProperties
        );
    }

    RunProperties combinedRunProperties() {
        return this.combinedRunProperties;
    }
}
