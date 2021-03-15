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

package net.sf.okapi.connectors.kantan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.BaseConnector;

/**
 * Connector for the <a href="https://www.kantanmt.com/">KantanMT</a> API v2.1.
 * The engine must be started separately (through the KantanMT web interface, or via API)
 * and is assumed to be running by this connector. As the engine initialization process
 * takes several minutes, the connector does not perform initialization on its own in
 * the {@link #open} method.
 * @see <a href="http://docs.kantanmt.apiary.io">http://docs.kantanmt.apiary.io</a>
 * Note: KantanMT uses TLS 1.2, which is supported by default by Java 8 (but not Java 7)
 */
public class KantanMTv21Connector extends BaseConnector {

	public static final String STATE_OFFLINE = "offline";
	public static final String STATE_RUNNING = "running";
	public static final String STATE_INITIALISING = "initialising";
	public static final String STATE_TERMINATING = "terminating";

	private Logger logger = LoggerFactory.getLogger(KantanMTv21Connector.class);

	private static final String API_SERVER_URL = "https://app.kantanmt.com/api/";
	private static final String CONNECTOR_ERROR = "KantanMT Connector Error: %s";

	private static final int DEFAULT_FUZZY_SCORE = 95;
	private static final int MAX_SEGMENTS = 900;

	private KantanMTv21ConnectorParameters parameters;
	private JSONParser parser;
	private CloseableHttpClient httpClient = null;
	private List<QueryResult> results = new ArrayList<>();

	public KantanMTv21Connector () {
		parameters = new KantanMTv21ConnectorParameters();
		parser = new JSONParser();
		httpClient = HttpClientBuilder.create().build();
	}

	@Override
	public String getName () {
		return "KantanMT v2.1";
	}

	@Override
	public String getSettingsDisplay () {
		String engine = (parameters != null) ? parameters.getEngine() : "";
		String alias = (parameters != null) ? parameters.getAlias() : "";
		StringBuilder sb = new StringBuilder("URL: "+API_SERVER_URL+"\n");
		if ( !Util.isEmpty(engine) ) {
			sb.append("Using engine \"" + engine + "\"");
		}
		else if ( !Util.isEmpty(alias) ) {
			sb.append("Using alias \"" + alias + "\"");
		}
		else {
			sb.append("No engine or alias selected");
		}
		return sb.toString();
	}

	@Override
	public void open () {
		// Nothing to do
	}

	@Override
	public void close () {
		if ( httpClient != null ) {
			try {
				httpClient.close();
			}
			catch ( IOException e ) {
				logger.warn("Cannot close HttpClient: {}", e.getMessage());
			}
		}
	}

	@Override
	public KantanMTv21ConnectorParameters getParameters () {
		return parameters;
	}

	@Override
	protected String toInternalCode (LocaleId locId) {
		// Get the string output, but in lowercase
		return locId.toString().toLowerCase();
	}

	@Override
	public int query (String plainText) {
		return query(new TextFragment(plainText));
	}

	@Override
	public int query (TextFragment tf) {
		results = batchQuery(Collections.singletonList(tf)).get(0);
		if ( results.size() > 0 ) current = 0;
		return results.size();
	}

	@Override
	public boolean hasNext () {
		if ( results == null ) return false;
		if ( current >= results.size() ) {
			current = -1;
		}
		return (current > -1);
	}

	@Override
	public QueryResult next () {
		if ( results == null ) return null;
		if (( current > -1 ) && ( current < results.size() )) {
			current++;
			return results.get(current-1);
		}
		current = -1;
		return null;
	}

	/**
	 * Wrapper to hold the original fragment along with its extracted codes, so
	 * that we can reinsert them into the translated target.
	 */
	protected static class RequestInfo {

		private TextFragment originalFragment;
		private String preparedText;

		RequestInfo(TextFragment fragment) {
			this.originalFragment = fragment;
			this.preparedText = GenericContent.fromFragmentToLetterCoded(originalFragment, true);
		}

		String getPreparedText () {
			return preparedText;
		}

		TextFragment generateResult(String translated) {
			return GenericContent.fromLetterCodedToFragment(translated, originalFragment.clone(), true, true);
		}
	}

	private void checkParameters () {
		if ( Util.isEmpty(parameters.getApiToken()) ) {
			throw new OkapiException("You must specify an authorization token to use this connector.");
		}
		if ( Util.isEmpty(parameters.getEngine()) ) {
			if ( Util.isEmpty(parameters.getAlias()) ) {
				throw new OkapiException("You must specify either an engine name or an alias name to use this connector.");
			}
		}
	}

	@Override
	public List<List<QueryResult>> batchQuery (List<TextFragment> fragments) {
		List<List<QueryResult>> results = new ArrayList<>();
		try {
			checkParameters();
			HttpPost post = new HttpPost(API_SERVER_URL+"translate");
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");

			List<RequestInfo> requests = new ArrayList<>();
			for (TextFragment fragment : fragments) {
				requests.add(new RequestInfo(fragment));
			}
			List<UrlEncodedFormEntity> forms = fragmentsToPostForms(requests);
			for (UrlEncodedFormEntity form : forms) {
				post.setEntity(form);
				try ( CloseableHttpResponse response = httpClient.execute(post) ) {
					final StatusLine status = response.getStatusLine();
					if (status == null) {
						logger.error("Unable to get response status code from Kantan API");
						throw new OkapiException(String.format(CONNECTOR_ERROR, "unable to get response status code"));
					}

					int code = status.getStatusCode();
					String content = readContent(response);

					if ( content == null || content.length() == 0 ) {
						logger.error("Unable to get response content from Kantan API. Response code {}", code);
						throw new OkapiException(String.format(CONNECTOR_ERROR, "missing response content, status code: " + code));
					}

					if ( code != 200 ) {
						logger.error("Error in communication with a remote server, status code => {}, response body => {}",
							code, content);
						String e = code == 401
								? "Translation request is not authorized. Please, verify your Kantan profile name and authorization token"
								: "Remote server responded with " + code + " status code";
						throw new OkapiException(e);
					}
					processResponse(requests, results, content);
				}
			}
		}
		catch ( ParseException e ) {
			logger.error("Cannot parse json response from KantanMT", e);
			throw new OkapiException("Cannot parse json response from Kantan MT. " + e.getMessage(), e);
		}
		catch ( IOException e ) {
			logger.error("Error in communication with Kantan MT server", e);
			throw new OkapiException("Error in communication with Kantan MT server: " + e.getMessage(), e);
		}

		return results;
	}

	private void processResponse (List<RequestInfo> requests,
		List<List<QueryResult>> results,
		String content)
		throws ParseException
	{
		JSONObject object = (JSONObject)parser.parse(content);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>)object.get("response");
		String type = (String)map.get("type");

		if (type.equals("translation")) {
			JSONObject bodyObj = (JSONObject)map.get("body");
			JSONArray translations = (JSONArray)bodyObj.get("translationData");

			List<QueryResult> qrList;
			for (Object translation : translations) {
				JSONObject transObj = (JSONObject) translation;
				long id = (Long) transObj.get("id");

				if (( id >= requests.size() ) || ( id < 0 )) {
					String m = "source fragment for translation with id '" + id + "' was not found";
					logger.error(m);
					throw new OkapiException(m);
				}
				RequestInfo request = requests.get((int)id);
				qrList = new ArrayList<>();
				result = new QueryResult();
				result.weight = getWeight();
				result.origin = getName();
				result.source = request.originalFragment;
				result.matchType = MatchType.MT;
				result.setFuzzyScore(DEFAULT_FUZZY_SCORE);

				if ( !request.originalFragment.hasText(false) ) {
					result.target = request.originalFragment.clone();
				}
				else {
					String translatedText = (String)transObj.get("trg");
					translatedText = translatedText.replaceAll("&amp;", "&");
					result.target = request.generateResult(translatedText);
				}
				qrList.add(result);
				results.add(qrList);
			}
		}
		else if ( type.equals("status") ) {
			JSONObject bodyObj = (JSONObject)map.get("body");
			String state = (String)bodyObj.get("state");
			String m = String.format(CONNECTOR_ERROR, "server is in '" + state + "' state");
			throw new OkapiException(m);
		}
		else {
			String m = String.format("Server returned JSON response with unprocessible type value '%s'", type);
			logger.error(m);
			throw new OkapiException(m);
		}
	}

	private List<UrlEncodedFormEntity> fragmentsToPostForms (List<RequestInfo> requests) {
		List<UrlEncodedFormEntity> forms = new ArrayList<>();

		List<NameValuePair> pairs = new ArrayList<>();
		pairs.add(new BasicNameValuePair("auth", parameters.getApiToken()));
		if ( !Util.isEmpty(parameters.getEngine()) ) {
			pairs.add(new BasicNameValuePair("engine", parameters.getEngine()));
		}
		else {
			pairs.add(new BasicNameValuePair("alias", parameters.getAlias()));
			pairs.add(new BasicNameValuePair("src", srcCode));
			pairs.add(new BasicNameValuePair("trg", trgCode));
		}

		for (int i = 0; i < requests.size(); i++) {
			final RequestInfo request = requests.get(i);
			if (!request.originalFragment.hasText(false)) {
				continue;
			}
			final String text = request.preparedText.replaceAll("&", "%26");

			if (pairs.size() >= MAX_SEGMENTS) {
				forms.add(new UrlEncodedFormEntity(pairs, Consts.UTF_8));
				pairs = new ArrayList<>();
				pairs.add(new BasicNameValuePair("auth", parameters.getApiToken()));
				//pairs.add(new BasicNameValuePair("profile", parameters.getProfileName()));
			}
			pairs.add(new BasicNameValuePair(Integer.toString(i), text));
		}
		forms.add(new UrlEncodedFormEntity(pairs, Consts.UTF_8));

		return forms;
	}

	private String readContent (HttpResponse response)
		throws IOException
	{
		String body = null;
		HttpEntity entity = response.getEntity();
		if ( entity == null ) return "";
		try ( InputStream stream = entity.getContent() ) {
			BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			body = br.readLine();
		}
		EntityUtils.consume(entity);
		return body;
	}

	/**
	 * Gets the information about the available engines.
	 * @return A JSON string with the engine information.
	 */
	public String getEngines () {
		try {
			HttpGet httpget = new HttpGet(API_SERVER_URL+"query/"+parameters.getApiToken()+"/engines");
			try ( CloseableHttpResponse resp = httpClient.execute(httpget) ) {
				String json = readContent(resp);
				return json;
			}
		}
		catch ( Exception e ) {
			throw new RuntimeException("Error getting the engines: "+e.getMessage(), e);
		}
	}

	/**
	 * Get the status of an engine.
	 * @param engine the name of the engine to check.
	 * @return the status information ('running' for when it is online and ready, etc.)
	 */
	public String getEngineStatus (String engine) {
		try {
			HttpGet httpget = new HttpGet(API_SERVER_URL+"query/"+parameters.getApiToken()+"/status/"+engine);
			try ( CloseableHttpResponse resp = httpClient.execute(httpget) ) {
				String json = readContent(resp);
				return processGetEngineStatusResponse(json);
			}
		}
		catch ( Exception e ) {
			throw new RuntimeException("Error getting engine status: "+e.getMessage(), e);
		}
	}

	/**
	 * Starts an engine (do nothing if it's already started).
	 * @param engine the name of the engine to start.
	 * @return the status of the engine after the call (e.g. 'running' if it is running already).
	 */
	public String startEngine (String engine) {
		try {
			HttpGet httpget = new HttpGet(API_SERVER_URL+"init/"+parameters.getApiToken()+"/"+engine);
			try ( CloseableHttpResponse resp = httpClient.execute(httpget) ) {
				String json = readContent(resp);
				return processStartEngineResponse(json);
			}
		}
		catch ( Exception e ) {
			throw new RuntimeException("Error starting engine: "+e.getMessage(), e);
		}
	}

	/**
	 * Stops an engine (do nothing if it's already stopped).
	 * @param engine the name of the engine to start.
	 * @return the status of the engine after the call (e.g. 'terminating' if it is being stopped).
	 */
	public String stopEngine (String engine) {
		try {
			HttpGet httpget = new HttpGet(API_SERVER_URL+"shutdown/"+parameters.getApiToken()+"/"+engine);
			try ( CloseableHttpResponse resp = httpClient.execute(httpget) ) {
				String json = readContent(resp);
				return processStopEngineResponse(json);
			}
		}
		catch ( Exception e ) {
			throw new RuntimeException("Error stopping engine: "+e.getMessage(), e);
		}
	}

	private String processGetEngineStatusResponse (String content)
		throws ParseException
	{
		JSONObject object = (JSONObject)parser.parse(content);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>)object.get("response");
		String type = (String)map.get("type");
		JSONObject bodyObj = (JSONObject)map.get("body");

		if ( type.equals("status") ) {
			return (String)bodyObj.get("state");
		}
		else {
			String m = String.format("Server returned JSON response with unprocessible type value '%s'", type);
			logger.error(m);
			throw new OkapiException(m);
		}
	}

	private String processStartEngineResponse (String content)
		throws ParseException
	{
		JSONObject object = (JSONObject)parser.parse(content);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>)object.get("response");
		String type = (String)map.get("type");
		JSONObject bodyObj = (JSONObject)map.get("body");

		if ( type.equals("status") ) {
			return (String)bodyObj.get("state");
		}
		else if ( type.equals("error") ) {
			throw new OkapiException("Error starting engine: "+bodyObj.toString());
		}
		else {
			String m = String.format("Server returned JSON response with unprocessible type value '%s'", type);
			logger.error(m);
			throw new OkapiException(m);
		}
	}

	private String processStopEngineResponse (String content)
		throws ParseException
	{
		JSONObject object = (JSONObject)parser.parse(content);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>)object.get("response");
		String type = (String)map.get("type");
		JSONObject bodyObj = (JSONObject)map.get("body");

		if ( type.equals("status") ) {
			return (String)bodyObj.get("state");
		}
		else if ( type.equals("error") ) {
			throw new OkapiException("Error starting engine: "+(String)bodyObj.get("error"));
		}
		else {
			String m = String.format("Server returned JSON response with unprocessible type value '%s'", type);
			logger.error(m);
			throw new OkapiException(m);
		}
	}

}
