/*===========================================================================
  Copyright (C) 2017-2018 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.deepl;

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.BaseConnector;

/**
 * Initial, basic connector for DeepL.
 * See https://stackoverflow.com/questions/45937616/using-deepl-api-to-translate-text for details.
 * This API is deprecated and will not work at some point.
 * Use the DeepLv1Connector instead.
 */
@Deprecated
public class DeepLConnector extends BaseConnector {

	private static String BASEURL = "https://www.deepl.com/jsonrpc";
	
	private Client client;
	private WebTarget wt;
	private List<QueryResult> results;
	private boolean topHitCandidateonly = true; // True to return only the top candidate, false to return all

	@Override
	public void close () {
	}

	@Override
	public String getName () {
		return "DeepL Connector (ALPHA)";
	}

	@Override
	public String getSettingsDisplay () {
		return "DeepL connector - Alpha - URL: "+BASEURL;
	}

	@Override
	public void open () {
		client = ClientBuilder.newClient();
		wt = client.target(BASEURL);
	}

	@Override
	public boolean hasNext () {
		if ( results == null ) {
			return false;
		}
		if ( current >= results.size() ) {
			current = -1;
		}
		return (current > -1);
	}

	@Override
	public QueryResult next () {
		if ( results == null ) {
			return null;
		}
		if (( current > -1 ) && ( current < results.size() )) {
			current++;
			return results.get(current-1);
		}
		current = -1;
		return null;
	}

	@Override
	public int query (String text) {
		current = -1;
		results = new ArrayList<>();
		if ( text.trim().isEmpty() ) {
			return 0;
		}
		String src = toInternalCode(srcLoc);
		String trg = toInternalCode(trgLoc);

		try {
			// Example
			// String json = "{"
			// 	+ "\"jsonrpc\": \"2.0\","
			// 	+ "\"method\": \"LMT_handle_jobs\","
			// 	+ "\"params\": {"
			// 		+ "\"jobs\": [{"
			// 			+ "\"kind\":\"default\","
			// 			+ "\"raw_en_sentence\": \""+jsonEnc.encode(text, EncoderContext.TEXT)+"\""
			// 		+ "}],"
			// 		+"\"lang\": {"
			// 			+ "\"user_preferred_langs\": ["
			// 				+ "\"" + src +"\","
			// 				+ "\""+ trg + "\""
			// 			+ "],"
			// 			+ "\"source_lang_user_selected\": \"" + src + "\","
			// 			+ "\"target_lang\": \"" + trg + "\""
			// 			+"},"
			// 		+ "\"priority\": -1"
			// 	+"}"
			// +"}";

			JsonArray jaJobs = Json.createArrayBuilder()
				.add(Json.createObjectBuilder()
					.add("kind", "default")
					.add("raw_en_sentence", text)
					.build())
				.build();
			JsonObject joLang = Json.createObjectBuilder()
				.add("user_preferred_langs", Json.createArrayBuilder()
					.add(src)
					.add(trg)
					.build())
				.add("source_lang_user_selected", src)
				.add("target_lang", trg)
				.build();
			JsonObject joParams = Json.createObjectBuilder()
				.add("jobs", jaJobs)
				.add("lang", joLang)
				.add("priority", -1)
				.build();
			String json = Json.createObjectBuilder()
				.add("jsonrpc", "2.0")
				.add("method", "LMT_handle_jobs")
				.add("params", joParams)
				.build().toString();

			Invocation.Builder ib = wt.request(MediaType.APPLICATION_JSON_TYPE);
			Response resp = ib.post(Entity.text(json));
			InputStream is = resp.readEntity(InputStream.class);
			JsonReader r = Json.createReader(is);
			JsonObject jo = r.readObject();
			jo = jo.getJsonObject("result");
			if ( jo == null ) return 0; // safety
			JsonArray ja = jo.getJsonArray("translations");
			if ( ja == null ) return 0; // Safety
			JsonArray beams = ja.getJsonObject(0).getJsonArray("beams");
			for ( int i=0; i<beams.size(); i++ ) {
				jo = beams.getJsonObject(i);
				if ( jo == null ) return 0;
				String trans = jo.getString("postprocessed_sentence");
				if ( Util.isEmpty(trans) ) continue;
				QueryResult qr = new QueryResult();
				qr.matchType = MatchType.MT;
				qr.source = new TextFragment(text);
				qr.target = new TextFragment(trans);
				// There is a score, but it is negative and we do not know its meaning
				qr.setFuzzyScore(95); // Arbitrary result for MT
				qr.setCombinedScore(95); // Arbitrary result for MT
				results.add(qr);
				if ( topHitCandidateonly ) break; // Return only the top hit
			}
			if ( results.size() > 0 ) {
				current = 0;
			}
			return results.size();
		}
		catch ( Throwable e ) {
			throw new RuntimeException("Error: "+e.getMessage()
				+ "\nSource text: "+text);
		}
	}

	@Override
	public int query (TextFragment fragment) {
		return query(fragment.getText());
	}

	@Override
	protected String toInternalCode (LocaleId locale) {
		String lang = locale.getLanguage().toUpperCase();
		switch ( lang ) {
		case "EN":
		case "DE":
		case "FR":
		case "ES":
		case "IT":
		case "NL":
		case "PL":
			return lang;
		default:
			throw new InvalidParameterException("Unsupported language: "+lang);
		}
	}

}
