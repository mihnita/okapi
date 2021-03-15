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
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.StreamUtil;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.connectors.googleautoml.util.PredictAPIUtil;

public class GoogleAutoMLTranslationAPIImpl implements GoogleAutoMLTranslationAPI {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleAutoMLTranslationAPIImpl.class);

    private final String baseUrl;
    private final PredictAPIUtil predictApiUtil;

    public GoogleAutoMLTranslationAPIImpl(String baseUrl) {
        this.baseUrl = baseUrl;
        this.predictApiUtil = new PredictAPIUtil();
    }

    @Override
    public String predict(String sourceText, String modelResourceName, GoogleOAuth2Service service)
            throws IOException, ParseException {
        List<String> translations = new ArrayList<>();
        List<JSONObject> requests = predictApiUtil.getPredictRequests(sourceText);

        for (JSONObject request : requests) {
            URL url = new URL(String.format("%s/%s:predict", baseUrl, modelResourceName));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + service.getAccessToken());
            conn.setDoOutput(true);

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(request.toJSONString());
            }

            if (conn.getResponseCode() == 200) {
                translations.add(predictApiUtil.extractTranslation(conn.getInputStream()));
            } else {
                String msg = StreamUtil.streamUtf8AsString(conn.getErrorStream());
                LOG.info("Error during AutoML predict call: {}", msg);
                throw new OkapiException(msg);
            }
        }

        StringBuilder translationBuilder = new StringBuilder();
        for (String translation : translations) {
            translationBuilder.append(translation);
        }
        return translationBuilder.toString();
    }
}
