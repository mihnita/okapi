/*===========================================================================
  Copyright (C) 2008-2013 by the Okapi Framework contributors
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

package net.sf.okapi.filters.regex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.annotation.Note;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RegexFilterTest {
	
	private RegexFilter filter;
	private FileLocation location;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	private LocaleId locFRCA = LocaleId.fromString("fr-ca");
	
	@Before
	public void setUp() {
		filter = new RegexFilter();
		location = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void testStartDocument () {
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(filter,
			new InputDocument(location.in("/Test01_stringinfo_en.info").toString(), null),
			"UTF-8", locEN, locEN));
	}
	
	@Test
	public void testDoubleExtraction () {
		// Read all files in the data directory
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(location.in("/Test01_srt_en.srt").toString(), "okf_regex@SRT.fprm"));
		list.add(new InputDocument(location.in("/Test01_stringinfo_en.info").toString(), "okf_regex@StringInfo.fprm"));
		list.add(new InputDocument(location.in("/TestRules01.txt").toString(), "okf_regex@TestRules01.fprm"));
		list.add(new InputDocument(location.in("/TestRules02.txt").toString(), "okf_regex@TestRules02.fprm"));
		list.add(new InputDocument(location.in("/TestRules03.txt").toString(), "okf_regex@TestRules03.fprm"));
		list.add(new InputDocument(location.in("/TestRules04.txt").toString(), "okf_regex@TestRules04.fprm"));
		list.add(new InputDocument(location.in("/TestRules05.txt").toString(), "okf_regex@TestRules05.fprm"));
		list.add(new InputDocument(location.in("/TestRules06.txt").toString(), "okf_regex@TestRules06.fprm")); 
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locFR));

		list.clear();
		list.add(new InputDocument(location.in("/TestFrenchISL.isl").toString(), "okf_regex@INI.fprm")); 
		assertTrue(rtc.executeCompare(filter, list, "Windows-1252", locFR, locFRCA));
	}

	@Test
	public void testConfigurations () {
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertEquals(5, list.size());
		IParameters params = filter.getParameters();
		assertNotNull(params);
		for ( FilterConfiguration config : list ) {
			if ( config.parametersLocation == null ) continue; // Default
			URL url = filter.getClass().getResource(config.parametersLocation);
			params.load(url, false);
			assertNotNull(params.toString());
		}
	}

	@Test
	public void testNameExtraction() {
		String snippet = "A\r\n" +
				"00:00:12,000 --> 00:00:15,123\n" +
				"This is the first subtitle\n" +
				"\n" +
				"00:00:20,000 --> 00:00:22,000\r\n" +
				"Another subtitle\n";
		Rule rule = new Rule();
		rule.setUseCodeFinder(false);
		rule.setRuleType(Rule.RULETYPE_CONTENT);
		rule.setExpression("^(([^\\r\\n]*)\\r?\\n)?(\\d\\d:\\d\\d:\\d\\d.*?)\\r?\\n(.*?)(\\n\\n+|\\z)");
		rule.setNameGroup(2);
		rule.setNoteGroup(3);
		rule.setSourceGroup(4);
		Parameters params = new Parameters();
		params.getRules().add(rule);
		filter.setParameters(params);

		ArrayList<Event> list = getEvents(snippet);
		assertEquals(4, list.size());

		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("This is the first subtitle", tu.getSource().toString());
		assertEquals("A", tu.getName());
		NoteAnnotation notes = tu.getAnnotation(NoteAnnotation.class);
		Note n = notes.getNote(0);
		assertEquals("00:00:12,000 --> 00:00:15,123", n.getNoteText());

		tu = FilterTestDriver.getTextUnit(list, 2);
		assertNotNull(tu);
		assertNull(tu.getName());
		assertEquals("Another subtitle\n", tu.getSource().toString());
		notes = tu.getAnnotation(NoteAnnotation.class);
		n = notes.getNote(0);
		assertEquals("00:00:20,000 --> 00:00:22,000", n.getNoteText());
	}

	@Test
	public void testNoteExtraction() {
		String snippet = "A\r\n" +
				"00:00:12,000 --> 00:00:15,123\n" +
				"This is the first subtitle\n" +
				"\n" +
				"00:00:20,000 --> 00:00:22,000\r\n" +
				"Another subtitle\n";
		Rule rule = new Rule();
		rule.setUseCodeFinder(false);
		rule.setRuleType(Rule.RULETYPE_CONTENT);
		rule.setExpression("^(([^\\r\\n]*)\\r?\\n)?(\\d\\d:\\d\\d:\\d\\d.*?)\\r?\\n(.*?)(\\n\\n+|\\z)");
		rule.setNameGroup(3);
		rule.setNoteGroup(2);
		rule.setSourceGroup(4);
		Parameters params = new Parameters();
		params.getRules().add(rule);
		filter.setParameters(params);

		ArrayList<Event> list = getEvents(snippet);
		assertEquals(4, list.size());

		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("This is the first subtitle", tu.getSource().toString());
		assertEquals("00:00:12,000 --> 00:00:15,123", tu.getName());
		NoteAnnotation notes = tu.getAnnotation(NoteAnnotation.class);
		Note n = notes.getNote(0);
		assertEquals("A", n.getNoteText());

		tu = FilterTestDriver.getTextUnit(list, 2);
		assertNotNull(tu);
		assertEquals("00:00:20,000 --> 00:00:22,000", tu.getName());
		assertEquals("Another subtitle\n", tu.getSource().toString());
		assertNull(tu.getAnnotation(NoteAnnotation.class));
	}

	@Test
	public void testMeta() {
		String snippet = "1\n" +
				"00:00:12,000 --> 00:00:15,123\n" + 
				"This is the first subtitle\n" + 
				"\n" +  
				"2\n" + 
				"00:00:20,000 --> 00:00:22,000  X1:40 X2:600 Y1:20 Y2:50\n" + 
				"Another subtitle demonstrating position.\n";
		Parameters params = new Parameters();
		Rule rule = new Rule();
		rule.setUseCodeFinder(false);
		rule.setRuleType(Rule.RULETYPE_CONTENT);
		rule.setExpression("^(\\d\\d:\\d\\d:\\d\\d.*?)\\n(.*?)(\\n\\n+|\\z)");
		rule.setSourceGroup(2);
		rule.setNoteGroup(1);
		params.getRules().add(rule);
		MetaRule metaRule = new MetaRule("meta", "[0-9]+");
		params.getMetaRules().add(metaRule);
		filter.setParameters(params);
		// Process
		ArrayList<Event> list = getEvents(snippet);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("This is the first subtitle", tu.getSource().toString());
		NoteAnnotation notes = tu.getAnnotation(NoteAnnotation.class);
		Note n = notes.getNote(0);
		assertEquals("00:00:12,000 --> 00:00:15,123", n.getNoteText());
		GenericAnnotations annos = tu.getAnnotation(GenericAnnotations.class);
		assertEquals("00", annos.getAllAnnotations().get(0).getString("meta"));
		
		tu = FilterTestDriver.getTextUnit(list, 2);
		assertNotNull(tu);
		assertEquals("Another subtitle demonstrating position.\n", tu.getSource().toString());
		notes = tu.getAnnotation(NoteAnnotation.class);
		n = notes.getNote(0);
		assertEquals("00:00:20,000 --> 00:00:22,000  X1:40 X2:600 Y1:20 Y2:50", n.getNoteText());
		annos = tu.getAnnotation(GenericAnnotations.class);
		assertEquals("00", annos.getAllAnnotations().get(0).getString("meta"));
	}
	
	@Test
	public void testSimpleRule () {
		String snippet = "test1=\"text1\"\ntest2=\"text2\"\n";
		Parameters params = new Parameters();
		Rule rule = new Rule();
		rule.setRuleType(Rule.RULETYPE_STRING);
		rule.setExpression("=(.+)$");
		rule.setSourceGroup(1);
		params.getRules().add(rule);
		filter.setParameters(params);
		// Process
		ArrayList<Event> list = getEvents(snippet);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("text1", tu.getSource().toString());
		tu = FilterTestDriver.getTextUnit(list, 2);
		assertNotNull(tu);
		assertEquals("text2", tu.getSource().toString());
	}

	@Test
	public void testIDAndText () {
		String snippet = "[Id1]\tText1\r\n[Id2]\tText2";
		Parameters params = new Parameters();
		Rule rule = new Rule();
		rule.setRuleType(Rule.RULETYPE_CONTENT);
		rule.setExpression("^\\[(.*?)]\\s*(.*?)(\\n|\\Z)");
		rule.setSourceGroup(2);
		rule.setNameGroup(1);
		rule.setPreserveWS(true);
		params.getRules().add(rule);
		filter.setParameters(params);
		// Process
		ArrayList<Event> list = getEvents(snippet);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("Text1", tu.getSource().toString());
		assertEquals("Id1", tu.getName());
		tu = FilterTestDriver.getTextUnit(list, 2);
		assertNotNull(tu);
		assertEquals("Text2", tu.getSource().toString());
		assertEquals("Id2", tu.getName());
	}

	@Test
	public void testEscapeDoubleChar () {
		String snippet = "id = [\"\"\"a\"\"b\"\"c\"\"\"]";
		Parameters params = new Parameters();
		Rule rule = new Rule();
		rule.setRuleType(Rule.RULETYPE_CONTENT);
		rule.setExpression("^.*?\\[(.*?)]");
		rule.setSourceGroup(1);
		rule.setRuleType(Rule.RULETYPE_STRING);
		params.getRules().add(rule);
		params.setUseDoubleCharEscape(true);
		filter.setParameters(params);
		// Process
		ArrayList<Event> list = getEvents(snippet);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("\"\"a\"\"b\"\"c\"\"", tu.getSource().toString());
	}

	@Test
	public void testEscapeDoubleCharNoEscape () {
		String snippet = "id = [\"a\" and \"b\"]";
		Parameters params = new Parameters();
		Rule rule = new Rule();
		rule.setRuleType(Rule.RULETYPE_CONTENT);
		rule.setExpression("^.*?\\[(.*?)]");
		rule.setSourceGroup(1);
		rule.setRuleType(Rule.RULETYPE_STRING);
		params.getRules().add(rule);
		params.setUseDoubleCharEscape(true);
		filter.setParameters(params);
		// Process
		ArrayList<Event> list = getEvents(snippet);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("a", tu.getSource().toString());
		tu = FilterTestDriver.getTextUnit(list, 2);
		assertNotNull(tu);
		assertEquals("b", tu.getSource().toString());
	}

	private ArrayList<Event> getEvents(String snippet) {
		return FilterTestDriver.getEvents(filter, snippet,  locEN);
	}
	
	@Test
	public void testEmptyLines() {
		Parameters params = new Parameters();
		Rule rule = new Rule();
		rule.setRuleType(Rule.RULETYPE_CONTENT);
		params.setRegexOptions(Pattern.MULTILINE);
		
		rule.setExpression("^(.*?)$");
		rule.setSourceGroup(1);
		
		//rule.setExpression("(^(?=.+))(.*?)$");
		//rule.setSourceGroup(2);
		
		params.getRules().add(rule);
		filter.setParameters(params);
						
		String inputText = "Line 1\n\nLine 2\n\n\n\n\nLine 3\n\n\nLine 4";
		//                  0123456 7 8901234 5 6 7 8 9012345 6 7 890123 
		//                  0           1              2            3
				
		listEvents(inputText);
		
		// Test individual events
		filter.open(new RawDocument(inputText, locEN));
		
		testEvent(EventType.START_DOCUMENT, "");
		testEvent(EventType.TEXT_UNIT, "Line 1");
		testEvent(EventType.DOCUMENT_PART, "\n\n");
		testEvent(EventType.TEXT_UNIT, "Line 2");
		testEvent(EventType.DOCUMENT_PART, "\n\n\n\n\n");
		testEvent(EventType.TEXT_UNIT, "Line 3");
		testEvent(EventType.DOCUMENT_PART, "\n\n\n");
		testEvent(EventType.TEXT_UNIT, "Line 4");
		testEvent(EventType.END_DOCUMENT, "");
		
		
		String inputText2 = "Line 1\nLine 2\n\nLine 3\n\n\nLine 4\n\n\n\n\n\n";
		//                   0123456 7890123 4 5678901 2 3 4567890 1 2 3 4 5  
		//                   0          1           2            3
		
		filter.open(new RawDocument(inputText2, locEN));
		
		testEvent(EventType.START_DOCUMENT, "");
		testEvent(EventType.TEXT_UNIT, "Line 1");
		testEvent(EventType.DOCUMENT_PART, "\n");
		testEvent(EventType.TEXT_UNIT, "Line 2");
		testEvent(EventType.DOCUMENT_PART, "\n\n");
		testEvent(EventType.TEXT_UNIT, "Line 3");
		testEvent(EventType.DOCUMENT_PART, "\n\n\n");
		testEvent(EventType.TEXT_UNIT, "Line 4");
		testEvent(EventType.DOCUMENT_PART, "\n\n\n\n\n\n");
		testEvent(EventType.END_DOCUMENT, "");
	}

	@Test
	public void testSemicolonInData() {
		
		Parameters params = filter.getParameters();
		params.load(location.in("/okf_regex@macStrings.fprm").asUrl(), false);
		params.setRemoveBSlashEscape(false);
						
		filter.open(new RawDocument(location.in("/TestRules07.strings").asUri(), "Windows-1252", locEN));
		
		testEvent(EventType.START_DOCUMENT, "");
		testEvent(EventType.DOCUMENT_PART, "/* Comment 1 */\n\"Item_Without_semicolon\" = \"");
		testEvent(EventType.TEXT_UNIT, "Text1");
		testEvent(EventType.DOCUMENT_PART, "\";");
		testEvent(EventType.DOCUMENT_PART, "\n\n/* Comment 2 */\n\"Item_With_semicolon\" = \"");
		testEvent(EventType.TEXT_UNIT, "Text2;Text3");
		testEvent(EventType.DOCUMENT_PART, "\";");
		testEvent(EventType.DOCUMENT_PART, "\n\n/* Comment 3 */\n\"Item_With_colon\" = \"");
		testEvent(EventType.TEXT_UNIT, "Text4:Text5");
		testEvent(EventType.DOCUMENT_PART, "\";");
		testEvent(EventType.DOCUMENT_PART, "\n\n/* Comment 4 */\n\"Item_With_trailing_comment\" = \"");
		testEvent(EventType.TEXT_UNIT, "Text6");
		testEvent(EventType.DOCUMENT_PART, "\";");
		testEvent(EventType.DOCUMENT_PART, " // Comment 5\n\n/* Comment 6 */\n\"Item_With_fake_statement_end\" = \"");
		testEvent(EventType.TEXT_UNIT, "Text7\\\";");
		testEvent(EventType.DOCUMENT_PART, "\";");
		testEvent(EventType.DOCUMENT_PART, "\n");
		testEvent(EventType.END_DOCUMENT, "");
	}

	@Test
	public void testBackslashEscapeHandling() {

		Parameters params = filter.getParameters();
		params.load(location.in("/okf_regex@macStrings.fprm").asUrl(), false);

		filter.open(new RawDocument(location.in("/test.strings").asUri(), "Windows-1252", locEN));

		testEvent(EventType.START_DOCUMENT, "");
		testEvent(EventType.DOCUMENT_PART, "/* test string */\n\"test \\\"string\\\"\" = \"");
		testEvent(EventType.TEXT_UNIT, "test \"string\"");
		testEvent(EventType.DOCUMENT_PART, "\";");
		testEvent(EventType.DOCUMENT_PART, "\n");
		testEvent(EventType.END_DOCUMENT, "");
	}
	
	private void testEvent(EventType expectedType, String expectedText) {
		assertNotNull(filter);
		Event event = filter.next();		
		assertNotNull(event);
		assertTrue(event.getEventType() == expectedType);

		switch (event.getEventType()) {
		case TEXT_UNIT:
			IResource res = event.getResource();
			assertTrue(res instanceof ITextUnit);
			assertEquals(expectedText, Util.normalizeNewlines(res.toString()));
			break;

		case DOCUMENT_PART:
			res = event.getResource();
			assertTrue(res instanceof DocumentPart);
			ISkeleton skel = res.getSkeleton();
			if ( skel != null ) {
				assertEquals(expectedText,
					Util.normalizeNewlines(skel.toString()));
			}
			break;
		default:
			break;
		}
	}
		
	private void listEvents(String inputText) { 
		// List all events in Console
		FilterTestDriver testDriver = new FilterTestDriver();
		testDriver.setDisplayLevel(0);
		testDriver.setShowSkeleton(true);
		
		filter.open(new RawDocument(inputText, locEN));
		if (!testDriver.process(filter)) Assert.fail();
		filter.close();
	}
	
}

