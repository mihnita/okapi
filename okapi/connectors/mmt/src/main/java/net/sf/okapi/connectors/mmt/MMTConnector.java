/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.mmt;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.BaseConnector;
import net.sf.okapi.lib.translation.QueryUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Connector for the <a href="http://www.modernmt.eu/">ModernMT Systems MT Engine API</a>.
 */
public class MMTConnector extends BaseConnector {

    private static final String TRANSLATE_METHOD = "/translate";
    private static final String QPARAM = "?q=";
    private static final String CONTEXTPARAM = "&context=";

    private Parameters params;
    private JSONParser parser;
    private QueryUtil util;

    public MMTConnector () {
        params = new Parameters();
        util = new QueryUtil();
        parser = new JSONParser();
    }

    @Override
    public String getName () {
        return "ModernMT API Connector";
    }

    @Override
    public String getSettingsDisplay () {
    	return "ModernMT URL: " + params.getUrl() +
        	(params.getContext().isEmpty() ? "" : " - Context: " + params.getContext());
    }

    @Override
    public void close () {
        // Nothing to do
    }

    @Override
    public void open () {
        // Nothing to do
    }

    @Override
    public int query (String plainText) {
        return query(new TextFragment(plainText));
    }

    /**
     * Queries the ModernMT API fro the configured server.
     * See <a href="https://github.com/ModernMT/MMT/wiki/API-Translate">MMT Translate API</a> for details.
     * @param fragment the fragment to query.
     * @return the number of translations (1 or 0).
     */
    @Override
    public int query (TextFragment fragment) {
        current = -1;
        try {
            if (!fragment.hasText(false)) {
                return 0;
            }
            if (Util.isEmpty(params.getUrl())) {
                throw new OkapiException("You must have a URL configured for your ModernMT Engine.");
            }
            String qtext = util.toCodedHTML(fragment);
            String contextPart = !params.getContext().isEmpty() ? CONTEXTPARAM + URLEncoder.encode(params.getContext(), "UTF-8") : "";
            StringBuilder sb = new StringBuilder(params.getUrl()).append(TRANSLATE_METHOD)
                    .append(QPARAM).append(URLEncoder.encode(qtext, "UTF-8"))
                    .append("&source=").append(srcLoc.getLanguage())
                    .append("&target=").append(trgLoc.getLanguage())
                    .append(contextPart);

            URL url = new URL(sb.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int code = conn.getResponseCode();
            if (code != 200) {
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append(String.format("Response Code: \"%d\", Body: \"%s\"", code, conn.getResponseMessage()));

                JSONObject resp = (JSONObject) parser.parse(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                if (resp.containsKey("error")) {
                    JSONObject error = (JSONObject) resp.get("error");
                    String type = (String) error.get("type");
                    String message = (String) error.get("message");
                    errorMessage.append(String.format(", Error Type: \"%s\", Message: \"%s\"", type, message));
                }
                throw new OkapiException(errorMessage.toString());
            }

            JSONObject object = (JSONObject) parser.parse(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            if (object.containsKey("data")) {
                JSONObject data = (JSONObject) object.get("data");
                String translation = (String) data.get("translation");

                result = new QueryResult();
                result.weight = getWeight();
                result.source = fragment;
                if (fragment.hasCode()) {
                    result.target = new TextFragment(util.fromCodedHTML(translation, fragment, true),
                            fragment.getClonedCodes());
                } else {
                    result.target = new TextFragment(util.fromCodedHTML(translation, fragment, true));
                }
                result.setFuzzyScore(95);
                result.origin = getName();
                result.matchType = MatchType.MT;
                current = 0;
            }
        }
        catch (Throwable e) {
            throw new OkapiException("Error querying the server.\n" + e.getMessage(), e);
        }
        return ((current==0) ? 1 : 0);
    }

    @Override
    public Parameters getParameters () {
        return params;
    }

    @Override
    public void setParameters (IParameters params) {
        this.params = (Parameters) params;
    }

}
