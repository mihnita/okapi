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

package net.sf.okapi.connectors.googleautoml.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.sf.okapi.common.StreamUtil;

/**
 * Helper methods for working with the POST /v1beta1/{model_name}:predict endpoint.
 */
public class PredictAPIUtil {
    /**
     * The maximum number of characters that can be translated in a single call to the predict endpoint.
     */
    public static final int CONTENT_CHAR_LIMIT = 250000;

    private final JSONParser parser;

    public PredictAPIUtil() {
        this.parser = new JSONParser();
    }

    /**
     * Given source text of any length, constructs one or more JSONs that can be used as request bodies
     * for the predict endpoint.
     */
    @SuppressWarnings("unchecked")
    public List<JSONObject> getPredictRequests(String sourceText) {
        List<JSONObject> requests = new ArrayList<>();
        List<String> splitTexts = getSplitTexts(sourceText);

        for (String splitText : splitTexts) {
            JSONObject root = new JSONObject();
            JSONObject payload = new JSONObject();
            JSONObject textSnippet = new JSONObject();
            textSnippet.put("content", splitText);
            payload.put("textSnippet", textSnippet);
            root.put("payload", payload);
            requests.add(root);
        }

        return requests;
    }

    /**
     * Splits the source text in smaller segments if the source text length exceeds
     * {@link PredictAPIUtil#CONTENT_CHAR_LIMIT}.
     */
    private List<String> getSplitTexts(String sourceText) {
        List<String> splitTexts = new ArrayList<>();
        for (int i = 0; i < sourceText.length(); i += CONTENT_CHAR_LIMIT) {
            splitTexts.add(sourceText.substring(i, Math.min(sourceText.length(), i + CONTENT_CHAR_LIMIT)));
        }
        return splitTexts;
    }

    /**
     * Extracts the translation from a stream containing the response body data.
     */
    public String extractTranslation(InputStream is) throws ParseException {
        JSONObject response = (JSONObject) parser.parse(StreamUtil.streamUtf8AsString(is));
        JSONArray payload = (JSONArray) response.get("payload");
        JSONObject payloadElement = (JSONObject) payload.get(0);
        JSONObject translation = (JSONObject) payloadElement.get("translation");
        JSONObject translatedContent = (JSONObject) translation.get("translatedContent");
        return (String) translatedContent.get("content");
    }
}
