/*===========================================================================
  Copyright (C) 2019 by the Okapi Framework contributors
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
package net.sf.okapi.connectors.microsoft;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Microsoft custom translator translation request response JSON object.
 */
public class TranslateResponse {
    public final List<Translation> translations;

    @JsonCreator
    public TranslateResponse(@JsonProperty("translations") List<Translation> translations) {
        this.translations = translations;
    }

    public static class Translation {
        public final String to;
        public final String text;

        @JsonCreator
        public Translation(@JsonProperty("to") String to, @JsonProperty("text") String text) {
            this.to = to;
            this.text = text;
        }
    }
}