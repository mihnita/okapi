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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.lib.translation.BaseConnector;

/**
 * MT connector for DeepL.
 * See https://www.deepl.com/docs/api-reference.html for details.
 */
public class DeepLv1Connector extends BaseConnector {

	private int maxTextParameters = 50;
	private int maxRequestSize = 28000;
	
	private List<List<TextFragment>> fragmentGroups;
	
	private static String BASEURL = "https://api.deepl.com/v1/translate";

	private Logger logger = LoggerFactory.getLogger(DeepLv1Connector.class);

	private final Pattern[] patterns;
	
	private CloseableHttpClient httpClient;
	private List<QueryResult> results;
	private boolean topHitCandidateonly = true; // Should be true for now, however not being used for batch mode
	private DeepLv1ConnectorParameters params;

	public DeepLv1Connector () {
		this(null);
	}

	/**
	 * Constructor for tests
	 * @param httpClient the client to use
	 */
	protected DeepLv1Connector (CloseableHttpClient httpClient) {
		this.params = new DeepLv1ConnectorParameters();
		this.httpClient = ( httpClient==null ? HttpClients.createDefault() : httpClient );

		// Create the pattern matchers for duplicates removal
		String[] regexs = { "\\<g-?\\d+?\\>", "\\</g-?\\d+?\\>", "\\<x-?\\d+?/\\>", "\\<b-?\\d+?/\\>", "\\<e-?\\d+?/\\>" };
		patterns = new Pattern[regexs.length];
		for ( int i=0; i<regexs.length; i++ ) {
			patterns[i] = Pattern.compile(regexs[i]);
		}
	}

	public int getMaxRequestSize () {
		return maxRequestSize;
	}

	/**
	 * Used for 'batchQuery' mode, sets a value (bytes) to use when grouping text fragments. 
	 * For each request made to DeepL, the total size of the group of text fragments will not exceed this size.
	 * @param maxRequestSize the maximum size in bytes to use for each DeepL request (otherwise default is 28KB)
	 */
	public void setMaxRequestSize (int maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	public int getMaxTextParameters () {
		return maxTextParameters;
	}

	public void setMaxTextParameters (int maxTextParameters) {
		this.maxTextParameters = maxTextParameters;
	}

	public List<List<TextFragment>> getFragmentGroups() {
		return fragmentGroups;
	}

	@Override
	public void close () {
		if ( httpClient != null ) {
			try {
				httpClient.close();
			}
			catch ( IOException e ) {
				logger.warn("Error when closing the HTTP client: {}", e.getMessage(), e);
			}
		}
	}

	@Override
	public String getName () {
		return "DeepL Connector v1";
	}

	@Override
	public String getSettingsDisplay () {
		return "DeepL connector v1 - URL: " + BASEURL +
			" Plain-text: " + (params.getPlainText() ? "Yes" : "No");
	}

	@Override
	public DeepLv1ConnectorParameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (DeepLv1ConnectorParameters)params;
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
	public List<List<QueryResult>> batchQuery (List<TextFragment> originalFragmentList) {

		List<List<QueryResult>> masterResults = new ArrayList<>();

		// Reset the results
		current = -1;
		results = new ArrayList<>();
		String text = "";
		String src = toInternalCode(srcLoc);
		String trg = toInternalCode(trgLoc);
		String out = null;
		
		// Check if we should use plain text (whether to set 'tag_handling: xml' in request)
		boolean usePlainText = params.getPlainText();
		
		// Iterate the list of fragments and break it up into groups of fragments ;
		// each group will be a translation request and adhere to DeepL limitations per-request
		fragmentGroups = new ArrayList<>();
		List<TextFragment> fragGroup = new ArrayList<>();
		int sizeTracker = 0;
		for ( int i=0; i < originalFragmentList.size(); i++ ) {
			
			TextFragment frag = originalFragmentList.get(i);

			if ( Util.isEmpty(frag.getText()) ) {
				logger.warn("Found empty fragment at index {} in the list of text fragments", i);
				continue;
			}
			
			int fragSize = frag.getText().getBytes().length;
			if ( fragSize > maxRequestSize ) {
				logger.warn("Found text fragment at index {} that is too large to be processed by DeepL", i);
				continue;
			}
			
			// Check to see if adding this frag would take us over the size limit and check if we are still under the limit for total 'text' request parameters
			if ( (sizeTracker + fragSize) <= maxRequestSize && (fragGroup.size() + 1) <= maxTextParameters ) {
				fragGroup.add(frag);
				sizeTracker += fragSize;
			}
			else {
				// Cannot add more to this fragment group, so, add it to the list of groups, then reinitialize everything
				fragmentGroups.add(fragGroup);
				fragGroup = new ArrayList<>();
				sizeTracker = 0;
				// Need to add the fragment from this current iteration to the new list
				fragGroup.add(frag);
				sizeTracker += fragSize;
			}
			
			if ( i == ( originalFragmentList.size() - 1 ) ) {
				// This is the final iteration, so we just add whatever we have in this group
				fragmentGroups.add(fragGroup);
			}
			
		}
		
		// We now should have a list containing groups of fragments. Go through the list and for each group we issue a request to DeepL
		for ( int i=0; i < fragmentGroups.size(); i++ ) {
			
			try {
				
				List<TextFragment> fragmentGroup = fragmentGroups.get(i);
				
				// Setup POST body as url-encoded (DeepL does not allow 'application/json' but does allow 'application/x-www-form-urlencoded')
				List<NameValuePair> reqParams = new ArrayList<>();
				
				for ( TextFragment frag : fragmentGroup ) {
					if ( usePlainText ) {
						text = frag.getText();
					}
					else {
						// Convert the Okapi codes to generic XML-like codes
						text = GenericContent.fromFragmentToLetterCoded(frag, true);
					}
					// Set the text
					reqParams.add(new BasicNameValuePair("text", text));
				}

				reqParams.add(new BasicNameValuePair("auth_key", params.getAuthKey()));
				reqParams.add( new BasicNameValuePair("split_sentences", params.getSplitSentences() ? "1" : "0"));
				reqParams.add( new BasicNameValuePair("preserve_formatting", params.getPreserveFormatting() ? "1" : "0"));
				reqParams.add(new BasicNameValuePair("source_lang", src));
				reqParams.add(new BasicNameValuePair("target_lang", trg));
				if ( !usePlainText ) {
					reqParams.add(new BasicNameValuePair("tag_handling", "xml"));
				}
				UrlEncodedFormEntity encodedParams = new UrlEncodedFormEntity(reqParams, "UTF-8");

				// Setup the POST
				HttpPost post = new HttpPost(BASEURL);
				post.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
				post.setHeader("Content-Type",ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
				post.setEntity(encodedParams);

				try ( CloseableHttpResponse response = httpClient.execute(post) ) {
					// Example of DeepL response
					// { "translations": [{"detected_source_language": "EN", "text": "Translated text"}, {"detected_source_language": "EN", "text": "Translated 2"}] }

					// Handle response
					if ( response.getStatusLine().getStatusCode() != 200 ) {
						throw new RuntimeException("HTTP response="+response.getStatusLine().getStatusCode()
							+ ": " +response.getStatusLine().getReasonPhrase());
					}
					// Else: parse the result
					out = readContent(response);
					InputStream is = new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8));
					JsonReader r = Json.createReaderFactory(null).createReader(is, StandardCharsets.UTF_8);
					JsonObject jo = r.readObject();
					JsonArray ja = jo.getJsonArray("translations");

					if ( ja == null ) {
						logger.warn("DeepL result does not have 'translations'. Response was: {}", jo.toString());
						masterResults.add(results);
						continue;
					}

					for ( int j=0; j < ja.size(); j++ ) {
						// DeepL documentation says the order of the translations is the same as the order of the input 'text' values
						// So it should be OK to use the index here to lookup the original fragment
						TextFragment origFrag = fragmentGroup.get(j);
						jo = ja.getJsonObject(j);
						if ( jo == null ) { continue; }

						String trans = jo.getString("text");
						if ( Util.isEmpty(trans) ) {
							logger.warn("Empty MT result for source '{}'. Using source.", origFrag.getText());
							trans = origFrag.getText(); // Use the source
						}
						QueryResult qr = new QueryResult();
						qr.matchType = MatchType.MT;
						qr.source = origFrag;
						// Deal with inline codes if needed
						if ( usePlainText ) {
							qr.target = new TextFragment(trans);
						}
						else {
							// Workaround for duplicated codes coming from DeepL
							trans = removeDupeCodes(trans);
							// Convert back to fragment
							qr.target = GenericContent.fromLetterCodedToFragment(trans, origFrag.clone(), true, true);
						}
						qr.setFuzzyScore(95); // Arbitrary result for MT
						qr.setCombinedScore(95); // Arbitrary result for MT

						// Attempt to mimic the MS batch step/connector, where each source/TU appears to have a list of QR for it
						// DeepL seems to only provide one translation per text fragment
						// So here we are making a list of lists of QR's, however each sub-list only has one QR
						results = new ArrayList<>(); 
						results.add(qr);
						masterResults.add(results);
					}
				}
				if ( results.size() > 0 ) {
					current = 0;
				}
			}
			catch ( Throwable e ) {
				throw new RuntimeException("Error: "+e.getMessage()
					+ "\nSource text: "+text
					+ "\nOutput: "+out);
			}
		}
		return masterResults;
	}

	@Override
	public int query (String text) {
		return query(new TextFragment(text));
	}

	@Override
	public int query (TextFragment fragment) {

		String text;
		// Check if we should use plain text (whether to set 'tag_handling: xml' in request)
		boolean usePlainText = params.getPlainText();
		if ( usePlainText ) {
			text = fragment.getText();
		}
		else {
			// Convert the Okapi codes to generic XML-like codes
			//String text = qu.toCodedHTML(fragment);
			text = GenericContent.fromFragmentToLetterCoded(fragment, true);
		}
		
		// Reset the results
		current = -1;
		results = new ArrayList<>();
		if ( text.trim().isEmpty() ) {
			return 0;
		}
		String src = toInternalCode(srcLoc);
		String trg = toInternalCode(trgLoc);
		String out = null;
		
		try {
			// Setup POST body as URL-encoded (DeepL does not allow 'application/json' but does allow 'application/x-www-form-urlencoded')
			List<NameValuePair> reqParams = new ArrayList<>();
			reqParams.add(new BasicNameValuePair("auth_key", params.getAuthKey()));
			reqParams.add( new BasicNameValuePair("split_sentences", params.getSplitSentences() ? "1" : "0"));
			reqParams.add( new BasicNameValuePair("preserve_formatting", params.getPreserveFormatting() ? "1" : "0"));
			reqParams.add(new BasicNameValuePair("source_lang", src));
			reqParams.add(new BasicNameValuePair("target_lang", trg));
			reqParams.add(new BasicNameValuePair("text", text));
			if ( !usePlainText ) {
				reqParams.add(new BasicNameValuePair("tag_handling", "xml"));
			}
			UrlEncodedFormEntity encodedParams = new UrlEncodedFormEntity(reqParams, "UTF-8");

			// Setup the POST
			HttpPost post = new HttpPost(BASEURL);
			post.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
			post.setHeader("Content-Type",ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
			post.setEntity(encodedParams);

			try ( CloseableHttpResponse response = httpClient.execute(post) ) {
				// Example of DeepL response
				// { "translations": [{"detected_source_language": "EN", "text": "Translated text"}] }

				// Handle response
				if ( response.getStatusLine().getStatusCode() != 200 ) {
					throw new RuntimeException("HTTP response="+response.getStatusLine().getStatusCode()
						+ ": " +response.getStatusLine().getReasonPhrase());
				}
				// Else: parse the result
				out = readContent(response);
				InputStream is = new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8));
				JsonReader r = Json.createReaderFactory(null).createReader(is, StandardCharsets.UTF_8);
				JsonObject jo = r.readObject();
				JsonArray ja = jo.getJsonArray("translations");
				if ( ja == null ) {
					logger.warn("DeepL result does not have 'translations'. Query was: {}", text);
					return 0; // Safety
				}
				
				for ( int i=0; i<ja.size(); i++ ) {
					jo = ja.getJsonObject(i);
					if ( jo == null ) return 0;
					String trans = jo.getString("text");
					if ( Util.isEmpty(trans) ) {
						logger.warn("Empty MT result for source '{}'. Using source.", fragment.getText());
						trans = fragment.toText(); // Use the source
					}
					QueryResult qr = new QueryResult();
					qr.matchType = MatchType.MT;
					qr.source = fragment;
					if ( usePlainText ) {
						qr.target = new TextFragment(trans);
					}
					else {
						// Workaround for duplicated codes coming from DeepL
						trans = removeDupeCodes(trans);
						// Convert back to fragment
						qr.target = GenericContent.fromLetterCodedToFragment(trans, fragment.clone(), true, true);
					}
					qr.setFuzzyScore(95); // Arbitrary result for MT
					qr.setCombinedScore(95); // Arbitrary result for MT
					results.add(qr);
					if ( topHitCandidateonly ) break; // Return only the top hit
				}
			}
			if ( results.size() > 0 ) {
				current = 0;
			}
			return results.size();
		}
		catch ( Throwable e ) {
			throw new RuntimeException("Error: "+e.getMessage()
				+ "\nSource text: "+text
				+ "\nOutput: "+out);
		}
	}
	
	private String readContent (HttpResponse response)
		throws IOException
	{
		String body = null;
		HttpEntity entity = response.getEntity();
		if ( entity == null )
			return "";
		try (InputStream stream = entity.getContent()) {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(stream, StandardCharsets.UTF_8));
			body = br.readLine();
		}
		EntityUtils.consume(entity);
		return body;
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

	@Override
	public void open() {
	}
	
	protected String removeDupeCodes (String dupeCodeText) {
		// Identify all codes in the string
		List<String> foundCodes = new ArrayList<>();
		for ( Pattern pattern : patterns ) {
			Matcher mat = pattern.matcher(dupeCodeText);
			while ( mat.find() ) { foundCodes.add(mat.group()); }
		}
		
		// If no codes found at all
		if ( foundCodes.size() == 0 ) { return dupeCodeText; }

		// Identify duplicate codes
		Map<String, String> seenCodes = new HashMap<>();
		List<String> dupeCodes = new ArrayList<>();
		for ( String codeVal : foundCodes ) {
			// If we've never seen this code, add to the group of seen codes
			if ( !seenCodes.containsKey(codeVal) ) {
				seenCodes.put(codeVal, codeVal);
				continue;
			}
			else {
				// We have seen this code before so add it to the dupe list
				dupeCodes.add(codeVal);
			}
		}
		
		// If no duplicate codes found
		if ( dupeCodes.size() == 0 ) { return dupeCodeText; }

		// Now attempt to remove the dupe codes from the string
		for ( String dupeCode : dupeCodes ) {
			String[] textParts = dupeCodeText.split(dupeCode);
			String tmpString = "";
			for ( int i=0 ; i < textParts.length ; i++ ) {
				String textPart = textParts[i];
				// Iterate and rebuild the whole string but keep only the first appearance of the code
				if ( i==0 ) {
					if ( dupeCodeText.startsWith(dupeCode) ) {
						tmpString = dupeCode+textPart;
					}
					else {
						tmpString = textPart+dupeCode;
					}
				}
				else {
					tmpString += textPart;
				}
			}
			dupeCodeText = tmpString;
		}
		
		return dupeCodeText;
	}
	
}
