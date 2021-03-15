/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.googleautoml;

import java.io.IOException;

import org.json.simple.parser.ParseException;

public interface GoogleAutoMLTranslationAPI {

    /**
     * Performs a prediction using the model with the given resource name and returns the translated text.
     */
    String predict(String sourceText, String modelResourceName, GoogleOAuth2Service service)
            throws IOException, ParseException;

}