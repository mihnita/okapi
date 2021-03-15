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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.okapi.common.query.QueryResult;

/**
 * Implementation class of MicrosoftMTAPI based on the Version 3 Translator Text API.
 * <br>
 * This is a preliminary implementation. It does not support of setting of optional API parameters.
 * @see <a href="https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-reference">Translator Text API v3.0 Reference</a>
 */
class MicrosoftMTAPIv3Impl implements MicrosoftMTAPI {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static final int FIXED_FUZZY_SCORE = 95; // Popular value to set for the system that doesn't use a translation memory.
	private Parameters params;
	private HttpClient httpClient;

	private String serviceURL;
	private ObjectMapper objectMapper = new ObjectMapper();
	
	MicrosoftMTAPIv3Impl(Parameters params, HttpClient httpClient) {
		this.params = params;
		this.httpClient = httpClient;
		this.serviceURL =  params.getBaseURL() + "/translate?api-version=3.0";
	}

	HttpClient getHttpClient() {
		return httpClient;
	}

	void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public List<TranslationResponse> getTranslations(String text, String srcLang, String trgLang, int maxHits,
			int threshold) {
		GetTranslationsArrayRequest singleTextRequest = 
				new GetTranslationsArrayRequest(Collections.singletonList(text), srcLang, trgLang, maxHits, params.getCategory());
		List<List<TranslationResponse>> x = getTranslationsArray(singleTextRequest, srcLang, trgLang, maxHits, threshold);
		return x.get(0);
	}

	@Override
	public List<List<TranslationResponse>> getTranslationsArray(GetTranslationsArrayRequest request, String srcLang,
			String trgLang, int maxHits, int threshold) {
		if (params.getAzureKey() == null || params.getAzureKey().isEmpty()) {
			logger.error("Azure Key is not configured.");
					return Collections.emptyList();
		}
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceURL);
			uriBuilder.addParameter("api-version", "3.0");
			uriBuilder.addParameter("from", srcLang);
			uriBuilder.addParameter("to", trgLang);
			uriBuilder.addParameter("textType", "html");
			if (params.getCategory() != null && !params.getCategory().isEmpty()) {
				uriBuilder.addParameter("category", params.getCategory());
			}
			HttpPost httpPost = new HttpPost(uriBuilder.build());

			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("Ocp-Apim-Subscription-Key", params.getAzureKey());
			httpPost.addHeader("X-ClientTraceId", UUID.randomUUID().toString());
			String requestBody = request.toJSON();
			httpPost.setEntity(new StringEntity(requestBody,
					ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), StandardCharsets.UTF_8)));
			
	        List<TranslateResponse> res = execute(httpPost, new TypeReference<List<TranslateResponse>>(){}, requestBody);
	        if (request.getNumRequests()!=res.size()) {
	        	logger.error("{} pieces of text requested but {} pieces of translation returned.", 
	        			request.getNumRequests(), res.size());
	        }

	        List<List<TranslationResponse>> lltr = new ArrayList<>(res.size());
	        int i = 0;
	        for (TranslateResponse tr: res) {
	        	lltr.add(Collections.singletonList(
	        			new TranslationResponse(request.getText(i++), tr.translations.get(0).text, 
	        									QueryResult.QUALITY_UNDEFINED, FIXED_FUZZY_SCORE)));
	        }
	        return lltr;
		} catch (URISyntaxException | IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
			logger.error("translation from {} to {} failed with exception: {}", 
							srcLang, trgLang, sw.toString());
			return Collections.emptyList();
		} 
	}
	

    private <T> T execute(HttpUriRequest request, TypeReference<T> responseType, String requestBody)
            throws IOException {
        HttpResponse response = httpClient.execute(request);

        // Read as a string first so that in an error case, we can log the response in a readable format
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            String message = String.format("Call to %s \"%s\" failed with code \"%d\", reason \"%s\", and response"
                    + " body:\n%s \nfor request body:\n%s",
                    request.getMethod(),
                    request.getURI().toString(),
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase(),
                    responseBody,
                    requestBody);
            throw new IOException(message);
        }

        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException e) {
            logger.error("Unable to deserialize response: {}\n", responseBody);
            throw e;
        }
    }

	@Override
	public String getBaseURL() {
		return params.getBaseURL();
	}

	/**
	 * Limitation imposed by v3 API.
	 * @see <a href="https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-translate?tabs=curl#request-body">v3 API Limitations</a>
	 */
	@Override
	public int maxCharCount() {
		return 5000;
	}

	/**
	 * Limitation imposed by v3 API.
	 * @see <a href="https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-translate?tabs=curl#request-body">v3 API Limitations</a>
	 */
	@Override
	public int maxTextCount() {
		return 100;
	}	
}
