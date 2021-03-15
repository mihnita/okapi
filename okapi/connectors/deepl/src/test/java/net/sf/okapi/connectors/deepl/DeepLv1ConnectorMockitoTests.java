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

package net.sf.okapi.connectors.deepl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

@RunWith(MockitoJUnitRunner.class)
public class DeepLv1ConnectorMockitoTests {

	@Mock
	private CloseableHttpClient httpClient;

	@InjectMocks
	private DeepLv1Connector connector;

	@Before
	public void setup() {
		initConnector();
	}
	
	private void initConnector() {
		connector.getParameters().setAuthKey("fake_token");
	}
	
	@Test
	public void testQuery() throws IOException {
		final CloseableHttpResponse mockResponse = mockHttpResponse(200, deeplJSONResponse(translationDataJSON(
				"EN",
				Arrays.asList("Translated first fragment")
		)));
		when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
		connector.setLanguages(LocaleId.ENGLISH, LocaleId.ENGLISH);
		int i = connector.query("First fragment");
		assertEquals(1, i);
		assertTrue(connector.hasNext());
		assertEquals("Translated first fragment", connector.next().target.toString());
	}

	@Test
	public void testQueryWithCodes() throws IOException {
		final CloseableHttpResponse mockResponse = mockHttpResponse(200, deeplJSONResponse(translationDataJSON(
				"EN",
				Arrays.asList("Text in <g1>bold</g1> and a &")
		)));
		when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
		connector.setLanguages(LocaleId.ENGLISH, LocaleId.ENGLISH);
		
		TextFragment frag = new TextFragment("Text in ");
		frag.append(TagType.OPENING, "b", "<B>");
		frag.append("bold");
		frag.append(TagType.CLOSING, "b", "</B>");
		frag.append(" and a &");
		
		int i = connector.query(frag);
		assertEquals(1, i);
		assertTrue(connector.hasNext());
		// Get the result: it should have the opening/closing codes and & should not be escaped
		TextFragment res = connector.next().target;
		assertEquals("Text in <B>bold</B> and a &", res.toText());
	}

	private CloseableHttpResponse mockHttpResponse(int statusCode, String body) throws IOException {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);

		StatusLine statusLine = mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(statusCode);

		HttpEntity httpEntity = mock(HttpEntity.class);
		when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

		when(response.getStatusLine()).thenReturn(statusLine);
		when(response.getEntity()).thenReturn(httpEntity);

		return response;
	}
	
	private String translationDataJSON(String srcLang, List<String> transStrings) {
		String templateTranslationData = "\"translations\": [%s]";
		String templateTranslationItem = "{\"detected_source_language\": \"%s\", \"text\": \"%s\"}";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < transStrings.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(String.format(templateTranslationItem, srcLang, transStrings.get(i)));
		}
		return String.format(templateTranslationData, sb.toString());
	}

	private String deeplJSONResponse (String body) {
		return String.format("{%s}", body);
	}

}