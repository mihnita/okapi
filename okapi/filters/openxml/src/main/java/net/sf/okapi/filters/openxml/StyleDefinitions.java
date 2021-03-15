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

import javax.xml.stream.XMLStreamException;
import java.util.Collections;

/**
 * Provides the style definitions.
 */
interface StyleDefinitions {
    void readWith(final StyleDefinitionsReader reader) throws XMLStreamException;
    void place(final String parentId, final ParagraphBlockProperties paragraphBlockProperties, final RunProperties runProperties);   String placedId();
    ParagraphBlockProperties combinedParagraphBlockProperties(final ParagraphBlockProperties paragraphBlockProperties);
    RunProperties combinedRunProperties(String paragraphStyle, String runStyle, RunProperties runProperties);
    StyleDefinitions mergedWith(final StyleDefinitions other);
    Markup toMarkup();

    /**
     * Provides an empty style definitions.
     */
    class Empty implements StyleDefinitions {
        @Override
        public void readWith(final StyleDefinitionsReader reader) {
        }

        @Override
        public void place(final String parentId, final ParagraphBlockProperties paragraphBlockProperties, final RunProperties runProperties) {
        }

        @Override
        public String placedId() {
            return null;
        }

        @Override
        public ParagraphBlockProperties combinedParagraphBlockProperties(final ParagraphBlockProperties paragraphBlockProperties) {
            return paragraphBlockProperties;
        }

        @Override
        public RunProperties combinedRunProperties(final String paragraphStyle, final String runStyle, final RunProperties runProperties) {

            // copy run properties with the exclusion of the RunStyleProperty
            return RunProperties.copiedRunProperties(runProperties, false, true, false);
        }

        @Override
        public StyleDefinitions mergedWith(final StyleDefinitions other) {
            return other;
        }

        @Override
        public Markup toMarkup() {
            return new Markup.General(Collections.emptyList());
        }
    }

    /**
     * Provides style definitions traversal stages.
     */
    enum TraversalStage {
        DOCUMENT_DEFAULT(),
        HORIZONTAL(),
        VERTICAL(),
        DIRECT()
    }
}
