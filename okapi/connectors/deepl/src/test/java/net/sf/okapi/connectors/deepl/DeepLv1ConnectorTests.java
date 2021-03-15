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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class DeepLv1ConnectorTests {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// To run those tests: replace the null by your API key
	private final String DEEPLAPIKEY = null; // "hhhhhhhh-hhhh-hhhh-hhhh-hhhhhhhhhhhh";

	@Test
	public void simpleTest () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);
		conn.query("This is an example in ASCII with the text \"Open the TC_ROOT\\install\\uninstall.xml file\" and some number of characters that should work just fine.");
		assertTrue(conn.hasNext());
		while ( conn.hasNext() ) {
			QueryResult qr = conn.next();
			logger.info("src: {}", qr.source.toText());
			logger.info("trg: {}", qr.target.toText());
			assertTrue(qr.fromMT());
			assertFalse(qr.source.toText().equals(qr.target.toText())); // Translation is not the same at the source
		}
		conn.close();
	}

	@Test
	public void testPlainText () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		params.setPlainText(true);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);
		TextFragment frag = new TextFragment("This is ");
		frag.append(TagType.OPENING, "bold", "<B>");
		frag.append("bold");
		frag.append(TagType.CLOSING, "bold", "</B>");
		frag.append(" with ");
		frag.append(TagType.PLACEHOLDER, "lb", "<BR/>");
		frag.append(" and an ampersand &");
		conn.query(frag);
		assertTrue(conn.hasNext());
		while ( conn.hasNext() ) {
			QueryResult qr = conn.next();
			logQueryResult(qr);
			assertTrue(qr.fromMT());
			assertFalse(qr.target.hasCode());
			assertFalse(qr.source.toText().equals(qr.target.toText())); // Translation is not the same at the source
		}
		conn.close();
	}

	@Test
	public void testWithCodes () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		params.setPlainText(false);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);
		TextFragment frag = new TextFragment("This is ");
		frag.append(TagType.OPENING, "bold", "<B>");
		frag.append("bold");
		frag.append(TagType.CLOSING, "bold", "</B>");
		frag.append(" with ");
		frag.append(TagType.PLACEHOLDER, "lb", "<BR/>");
		frag.append(" and an ampersand &");
		conn.query(frag);
		assertTrue(conn.hasNext());
		while ( conn.hasNext() ) {
			QueryResult qr = conn.next();
			logQueryResult(qr);
			assertTrue(qr.fromMT());
			assertTrue(qr.target.hasCode());
			assertFalse(qr.source.toText().equals(qr.target.toText())); // Translation is not the same at the source
		}
		conn.close();
	}

	@Test
	public void testWithCodesForRSIssue () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		params.setPlainText(false);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.fromBCP47("pl-PL"));
		// &amp;prod-actwksp-long; XRT rendering preferences include 
		// <ph id="1" x="&lt;literalText&#xA; &gt;">{1}</ph>
		// AWC_
		// <ph id="2" x="&lt;/literalText&gt;">{2}</ph>
		//  as a prefix to the preference name, allowing for the assignment of style sheets that are unique to &amp;prod-actwksp-long;.
		TextFragment frag = new TextFragment("&prod-actwksp-long; XRT rendering preferences include ");
		frag.append(TagType.PLACEHOLDER, "ph1", "{1}");
		frag.append("AWC_");
		frag.append(TagType.PLACEHOLDER, "ph2", "{2}");
		frag.append(" as a prefix to the preference name, allowing for the assignment of style sheets that are unique to &prod-actwksp-long;.");
		conn.query(frag);
		assertTrue(conn.hasNext());
		while ( conn.hasNext() ) {
			QueryResult qr = conn.next();
			logQueryResult(qr);
			assertTrue(qr.fromMT());
			assertTrue(qr.target.hasCode());
			assertFalse(qr.source.toText().equals(qr.target.toText())); // Translation is not the same at the source
		}
		conn.close();
	}

	@Test
	public void testWithCodesEnclosingSeveralWords () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		params.setPlainText(false);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);
		TextFragment frag = new TextFragment("This is ");
		frag.append(TagType.OPENING, "bold", "<B>");
		frag.append("quite a bold text");
		frag.append(TagType.CLOSING, "bold", "</B>");
		frag.append("  with ");
		frag.append(TagType.PLACEHOLDER, "lb", "<BR/>");
		frag.append(" and an ampersand &");
		conn.query(frag);
		assertTrue(conn.hasNext());
		while ( conn.hasNext() ) {
			QueryResult qr = conn.next();
			logQueryResult(qr);
			assertTrue(qr.fromMT());
			assertTrue(qr.target.hasCode());
		}
		conn.close();
	}

	@Test
	public void testNegativeCodeIDs () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		params.setPlainText(false);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);
		TextFragment frag = new TextFragment("This is ");
		frag.append(TagType.OPENING, "bold", "<B>", -123);
		frag.append("quite a bold text");
		frag.append(TagType.CLOSING, "bold", "</B>", -123);
		conn.query(frag);
		assertTrue(conn.hasNext());
		while ( conn.hasNext() ) {
			QueryResult qr = conn.next();
			logger.info("src: {}", GenericContent.fromFragmentToLetterCoded(qr.source, true));
			logger.info("trg: {}", GenericContent.fromFragmentToLetterCoded(qr.target, true));
			assertTrue(qr.fromMT());
			assertTrue(qr.target.hasCode());
			assertEquals(-123, qr.target.getCode(0).getId());
		}
		conn.close();
	}

	@Test
	public void testRemoveDuplicateCodes ( ) {
		String dupeCodeString = "<g-1>C'est un</g-1> <e5/><e5/>texte <b123/>assez <b123/>audacieux avec <x2/> et un <g-1>esperluet</g-1> & et un <g2>chien</g2> et un <g2>pomme</g2>.";
		String fixedString = "<g-1>C'est un</g-1> <e5/>texte <b123/>assez audacieux avec <x2/> et un esperluet & et un <g2>chien</g2> et un pomme.";
		try ( DeepLv1Connector conn = new DeepLv1Connector() ) {
			assertEquals(conn.removeDupeCodes(dupeCodeString), fixedString);
		}
	}

	@Test
	public void testBatchQueryGroupSizeLimits () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters deeplParams = conn.getParameters();
		deeplParams.setAuthKey(DEEPLAPIKEY);
		deeplParams.setPlainText(false);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.SPANISH);
		List<TextFragment> frags = new ArrayList<>();
		for ( int i=1 ; i<52 ; i++ ) { // 51 segments
			frags.add(new TextFragment("This is number "+i+"."));
		}
		List<List<QueryResult>> queryResults = conn.batchQuery(frags);
		assertTrue(queryResults.size()==51);
		String str = queryResults.get(50).get(0).source.toText();
		assertEquals(str, "This is number 51.");
		str = queryResults.get(50).get(0).target.toText();
		assertEquals(str, "Este es el n\u00FAmero 51.");
		conn.close();
	}

	@Test
	public void testBatchQueryMaxRequestSize () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters deeplParams = conn.getParameters();
		deeplParams.setAuthKey(DEEPLAPIKEY);
		deeplParams.setPlainText(false);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.SPANISH);
		conn.setMaxRequestSize(200);
		List<TextFragment> frags = new ArrayList<>();
		for ( int i=1 ; i<30 ; i++ ) {
			frags.add(new TextFragment("This is number "+i+"."));
		}
		List<List<QueryResult>> queryResults = conn.batchQuery(frags);
		int allResultsSize = queryResults.size();
		assertTrue(allResultsSize==29);
		List<List<TextFragment>> fragmentGroups = conn.getFragmentGroups();
		int fragGroupSize = fragmentGroups.size();
		assertTrue(fragGroupSize==3);
		conn.close();
	}

	@Test
	public void splitSentencesTest () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		params.setSplitSentences(true);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);

		conn.query("First sentence. Then the second one. Then the third sentence.");
		String tr1;
		assertTrue(conn.hasNext());
		QueryResult qr = conn.next();
		logQueryResult(qr);
		tr1 = qr.target.toText();

		params.setSplitSentences(false);
		conn.query("First sentence. Then the second one. Then the third sentence.");
		String tr2;
		assertTrue(conn.hasNext());
		qr = conn.next();
		logQueryResult(qr);
		tr2 = qr.target.toText();

		// Context makes a difference in the resulting translation candidates
		assertFalse(tr1.equals(tr2));

		conn.close();
	}

	@Test
	public void preserveFormattingTest () {
		if (deeplKeyMissing()) {
			return;
		}

		DeepLv1Connector conn = new DeepLv1Connector();
		DeepLv1ConnectorParameters params = conn.getParameters();
		params.setAuthKey(DEEPLAPIKEY);
		params.setPreserveFormatting(true);
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);

		conn.query("IMPORTANT first word, then some text...  ");
		String tr1;
		assertTrue(conn.hasNext());
		QueryResult qr = conn.next();
		logQueryResult(qr);
		tr1 = qr.target.toText();

		params.setPreserveFormatting(false);
		conn.query("IMPORTANT first word, then some text...  ");
		String tr2;
		assertTrue(conn.hasNext());
		qr = conn.next();
		logQueryResult(qr);
		tr2 = qr.target.toText();

		// Flag makes a difference in the trailing periods
		assertFalse(tr1.equals(tr2));

		conn.close();
	}

	boolean deeplKeyMissing() {
		if (DEEPLAPIKEY == null) {
			logger.warn("Test not run: you need a DeepL API key for it");
			return true;
		}
		return false;
	}

	void logQueryResult(QueryResult qr) {
		logger.info("src: {}", qr.source.toText());
		logger.info("trg: {}", qr.target.toText());
	}
}