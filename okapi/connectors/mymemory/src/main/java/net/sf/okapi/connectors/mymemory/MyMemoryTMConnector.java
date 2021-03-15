/*===========================================================================
  Copyright (C) 2009-2021 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.mymemory;

import com.ibm.icu.util.ULocale;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.exceptions.OkapiNotImplementedException;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.BaseConnector;
import net.sf.okapi.lib.translation.ITMQuery;
import net.sf.okapi.lib.translation.QueryUtil;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Connector for MyMemory collaborative translation memory system.
 *
 * More info at https://mymemory.translated.net/
 */
public class MyMemoryTMConnector extends BaseConnector implements ITMQuery {

	private static final String BASE_URL = "https://api.mymemory.translated.net";
	private static final String BASE_QUERY = "/get?q=%s&langpair=%s|%s";
	public static final SimpleDateFormat sdfShort=new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat sdfLong=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final List<QueryResult> results;
	private final QueryUtil qutil;
	private final JSONParser parser;
	private int current = -1;
	private int maxHits = 25;
	private int threshold = 75;
	private Parameters params;

	public MyMemoryTMConnector () {
		params = new Parameters();
		qutil = new QueryUtil();
		parser = new JSONParser();
		results = new ArrayList<>();
	}

	@Override
	public String getName () {
		return "MyMemory";
	}

	@Override
	public String getSettingsDisplay () {
		return "Server: " + BASE_URL + '\n' +
				"Allow MT: " + (params.getUseMT() ? "Yes" : "No") + '\n' +
				"Key: " + (Util.isEmpty(params.getKey()) ? "None" : params.getKey()) + '\n' +
				"Email: " + (Util.isEmpty(params.getEmail()) ? "None" : params.getEmail());
	}

	@Override
	public void close () {
		// Nothing to do
	}

	@Override
	public boolean hasNext () {
		if ( current >= results.size() ) {
			current = -1;
		}
		return (current > -1);
	}

	@Override
	public QueryResult next () {
		if (( current > -1 ) && ( current < results.size() )) {
			current++;
			return results.get(current-1);
		}
		current = -1;
		return null;
	}

	@Override
	public void open () {
		// do nothing
	}

	@Override
	public int query (TextFragment frag) {
		results.clear();
		current = -1;
		if ( !frag.hasText(false) ) return 0;
		try {
			// Build the query URL
			StringBuilder urlBuilder = new StringBuilder(BASE_URL);
			String text = qutil.separateCodesFromText(frag);
			urlBuilder.append(String.format(BASE_QUERY, URLEncoder.encode(text, StandardCharsets.UTF_8.name()), srcCode, trgCode));
			urlBuilder.append("&numres=").append(maxHits);
			if (!params.getUseMT()) {
				urlBuilder.append("&mt=0");
			}
			if (!Util.isEmpty(params.getKey())) {
				urlBuilder.append("&key=").append(URLEncoder.encode(params.getKey(), StandardCharsets.UTF_8.name()));
			}
			if (!Util.isEmpty(params.getEmail())) {
				urlBuilder.append("&de=").append(URLEncoder.encode(params.getEmail(), StandardCharsets.UTF_8.name()));
			}
			URL url = new URL(urlBuilder.toString());

			// Get the response
			JSONObject objResponse;
			try (InputStreamReader reader = new InputStreamReader(url.openConnection().getInputStream(), StandardCharsets.UTF_8)) {
				objResponse = (JSONObject) parser.parse(reader);
			}

			// Check response status code is 200 and there is a result array otherwise fail immediately
			if (!objResponse.get("responseStatus").equals(200L)) {
				throw new OkapiException(objResponse.get("responseDetails").toString());
			}
			if (!(objResponse.get("matches") instanceof JSONArray)) {
				throw new OkapiException("Unexpected response (no results array)");
			}

			// Process returned matches
			JSONArray matches = (JSONArray)objResponse.get("matches");
			for (Object m : matches) {
				JSONObject match = (JSONObject) m;
				QueryResult res = new QueryResult();

				// Check the score (Cast to Number because the value can be 1 (Long) or 0.N (Double))
				int score = (int)(((Number)match.get("match")).doubleValue()*100.0);
				// 1% penalty in case of presence of codes (unsupported)
				if (qutil.hasCode()) score--;

				// Stop if we cross the threshold (matched are sorted by decreasing score)
				if (score < getThreshold()) break;

				// Detect machine translated matches
				String from = (String) match.get("last-updated-by");
				if (from == null) {
					from = (String) match.get("created-by");
					if (from == null)
						from = "";
				}
				if (from.equals(Util.MTFLAG)) {
					res.matchType = MatchType.MT;
				}else{
					res.matchType = MatchType.FUZZY;
				}

				// Read creation date
				final String dateString = match.get("create-date").toString();
				try {
					res.creationDate = sdfLong.parse(dateString);
				} catch (ParseException e1) {
					try {
						res.creationDate = sdfShort.parse(dateString);
					} catch (ParseException e2) {
						// continue without setting creation date
					}
				}

				// Set other result attributes
				res.weight = getWeight();
				res.origin = getName();
				res.setFuzzyScore(score);

				// Set source and target text
				if (qutil.hasCode()) {
					res.source = qutil.createNewFragmentWithCodes((String) match.get("segment"));
					res.target = qutil.createNewFragmentWithCodes((String) match.get("translation"));
				} else {
					res.source = new TextFragment((String) match.get("segment"));
					res.target = new TextFragment((String) match.get("translation"));
				}

				results.add(res);
			}
			current = 0;
		} catch (Throwable e) {
			if (!(e instanceof OkapiException)) {
				throw new OkapiException("Error querying the server: " + e.getMessage(), e);
			} else
				throw (OkapiException) e;
		}
		if ( results.size() > 0 ) current = 0;
		return results.size();
	}

	@Override
	public int query (String plainText) {
		return query(new TextFragment(plainText));
	}

	@Override
	public List<List<QueryResult>> batchQuery (List<TextFragment> fragments) {
		throw new OkapiNotImplementedException();
	}

	@Override
	protected String toInternalCode (LocaleId locale) {
		// The expected language code is language-Region with region mandatory
		String lang = locale.getLanguage();
		String reg = locale.getRegion();
		if (reg == null || reg.isEmpty()) {
			ULocale fullLocale = ULocale.addLikelySubtags(locale.toIcuLocale());
			reg = fullLocale.getCountry().toLowerCase(Locale.US);
		}
		return lang + "-" +reg;
	}

	/**
	 * Sets the maximum number of hits to return.
	 */
	@Override
	public void setMaximumHits (int max) {
		if ( max < 1 ) maxHits = 1;
		else maxHits = max;
	}

	@Override
	public void setThreshold (int threshold) {
		this.threshold = threshold;
	}

	@Override
	public int getMaximumHits () {
		return maxHits;
	}

	@Override
	public int getThreshold () {
		return threshold;
	}

	@Override
	public Parameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	@Override
	public void setRootDirectory (String rootDir) {
		// Not used
	}

}
