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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.connectors.kantan.KantanMTv21Connector.RequestInfo;

@RunWith(MockitoJUnitRunner.class)
public class KantanMTv21ConnectorMockitoTests {


	@Mock
	private CloseableHttpClient httpClient;

	@InjectMocks
	private KantanMTv21Connector connector;

	@Before
	public void setup () {
		initConnector();
		initMockHttpClient();
	}

	@Test
	public void testBatchQuery ()
		throws IOException
	{
		final CloseableHttpResponse mockResponse = mockHttpResponse(200, kantanJSONResponse("translation", translationDataJSON(
			Arrays.asList("First fragment", "Second fragment", "Third fragment"),
			Arrays.asList("Translated first fragment", "Translated second fragment", "Translated third fragment")
			)));

		when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
		List<List<QueryResult>> r = connector.batchQuery(generateTextFragments("First fragment", "Second fragment", "Third fragment"));
		assertEquals(3, r.size());
		assertEquals("Translated first fragment", r.get(0).get(0).target.toString());
		assertEquals("Translated second fragment", r.get(1).get(0).target.toString());
		assertEquals("Translated third fragment", r.get(2).get(0).target.toString());
	}

	@Test
	public void testQueryTextFragment ()
		throws IOException
	{
		final CloseableHttpResponse mockResponse = mockHttpResponse(200, kantanJSONResponse("translation", translationDataJSON(
			Arrays.asList("First fragment"),
			Arrays.asList("Translated first fragment")
			)));
		when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
		int i = connector.query(new TextFragment("First fragment"));
		assertEquals(1, i);
		assertTrue(connector.hasNext());
		assertEquals("Translated first fragment", connector.next().target.toString());
	}

	@Test
	public void testQuery () throws IOException {
		final CloseableHttpResponse mockResponse = mockHttpResponse(200, kantanJSONResponse("translation", translationDataJSON(
			Arrays.asList("First fragment"),
			Arrays.asList("Translated first fragment")
			)));
		when(httpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
		int i = connector.query("First fragment");
		assertEquals(1, i);
		assertTrue(connector.hasNext());
		assertEquals("Translated first fragment", connector.next().target.toString());
	}

	private void initConnector () {
		(connector.getParameters()).setApiToken("fake_token");
		(connector.getParameters()).setEngine("fake_engine");
	}

	private void initMockHttpClient () {
		//        when(httpClient.getConnectionManager()).thenReturn(mock(ClientConnectionManager.class));
		//when(httpClient.getParams()).thenReturn(mock(HttpParams.class));
	}

	private CloseableHttpResponse mockHttpResponse (int statusCode,
		String body)
		throws IOException
	{
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);

		StatusLine statusLine = mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(statusCode);

		HttpEntity httpEntity = mock(HttpEntity.class);
		when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

		when(response.getStatusLine()).thenReturn(statusLine);
		when(response.getEntity()).thenReturn(httpEntity);

		return response;
	}

	private List<TextFragment> generateTextFragments (String ... src) {
		List<TextFragment> fragments = new ArrayList<>();
		for (String s : src) {
			fragments.add(new TextFragment(s));
		}
		return fragments;
	}

	private String translationDataJSON (List<String> srcStrings,
		List<String> tgtStrings)
	{
		String templateTranslationData = "\"translationData\": [%s]";
		String templateTranslationItem = "{\"src\": \"%s\", \"trg\":\"%s\", \"id\": %d}";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < srcStrings.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(String.format(templateTranslationItem, srcStrings.get(i), tgtStrings.get(i), i));
		}
		return String.format(templateTranslationData, sb.toString());
	}

	private String kantanJSONResponse (String type,
		String body)
	{
		return String.format("{\"response\": {\"type\": \"%s\", \"body\": { %s }}}", type, body);
	}

	@Test
	public void testRoundTrip () {
		TextFragment frag = new TextFragment("This is ");
		frag.append(TagType.OPENING, "bold", "<B>");
		frag.append("bold");
		frag.append(TagType.CLOSING, "bold", "</B>");
		frag.append(" with ");
		frag.append(TagType.PLACEHOLDER, "lb", "<BR/>");

		RequestInfo ri = new RequestInfo(frag);
		String html = ri.getPreparedText();
		TextFragment out = ri.generateResult(html);
		assertEquals(frag.toText(), out.toText());
		assertFalse(out==frag); // Not the same object
	}

}
