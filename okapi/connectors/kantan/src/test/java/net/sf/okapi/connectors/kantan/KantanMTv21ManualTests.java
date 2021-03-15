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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

@RunWith(JUnit4.class)
public class KantanMTv21ManualTests {

	private LocaleId srcLoc = LocaleId.fromBCP47("en-US");
	private LocaleId trgLoc = LocaleId.fromBCP47("fr-FR");
	private String apiToken = ""; // Your API Token
	private String engine = "French_JD_20180303"; // Your engine name

	@Test
	public void testQuery () {
		if ( Util.isEmpty(apiToken) ) return;
		try (KantanMTv21Connector conn = new KantanMTv21Connector()) {
			KantanMTv21ConnectorParameters params = conn.getParameters();
			params.setApiToken(apiToken);
			params.setEngine(engine);
			conn.open();
			conn.setLanguages(srcLoc, trgLoc);

			int count = conn.query("This is a test for the API.");
			assertEquals(1, count);
			QueryResult qr = conn.next();
			assertNotNull(qr);
			System.out.println("src: "+qr.source.toText());
			System.out.println("trg: "+qr.target.toText());
		}
	}

	@Test
	public void testGetEngines () {
		if ( Util.isEmpty(apiToken) ) return;
		try (KantanMTv21Connector conn = new KantanMTv21Connector()) {
			KantanMTv21ConnectorParameters params = conn.getParameters();
			params.setApiToken(apiToken);
			conn.open();
			System.out.println(conn.getEngines());
		}
	}

	@Test
	public void testQueryWithTags () {
		if ( Util.isEmpty(apiToken) ) return;
		KantanMTv21Connector conn = null;
		try {
			TextFragment frag = new TextFragment("This is ");
			frag.append(TagType.OPENING, "bold", "<B>");
			frag.append("bold + < and > and the characters @#$");
			frag.append(TagType.CLOSING, "bold", "</B>");
			frag.append(" with ");
			frag.append(TagType.PLACEHOLDER, "lb", "<BR/>");
			frag.append(" and &.");

			conn = new KantanMTv21Connector();
			KantanMTv21ConnectorParameters params = conn.getParameters();
			params.setApiToken(apiToken);
			params.setEngine(engine);
			conn.setLanguages(LocaleId.fromBCP47("en-US"), LocaleId.fromBCP47("fr-FR"));
			conn.open();
			int count = conn.query(frag);
			assertEquals(1, count);
			QueryResult qr = conn.next();
			System.out.println("src: "+qr.source.toText());
			System.out.println("trg: "+qr.target.toText());
		}
		finally {
			if ( conn != null ) conn.close();
		}
	}

}
