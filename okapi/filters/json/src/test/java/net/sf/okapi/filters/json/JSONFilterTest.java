/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.json;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.annotation.Note;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.filters.*;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class JSONFilterTest {

	private JSONFilter filter;
	private FileLocation root;
	private LocaleId locEN = LocaleId.fromString("en");

	@Before
	public void setUp() {
		filter = new JSONFilter();
		root = FileLocation.fromClass(this.getClass());
	}

	@After
	public void tearDown() {
		filter.close();
	}

	@Test
	public void testList () {
		Parameters params = new Parameters();
		params.setExtractStandalone(true);
		String snippet = "{\"key\" : [ \"Text1\" ] }";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertTrue(tu!=null);
	}

	@Test
	public void testEscapedForwardSlashDecoding() {
		Parameters params = new Parameters();
		params.setEscapeForwardSlashes(false);
		String snippet = "{\"key\" : \"https:\\/\\/okapiframework.org/wiki\" }";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertEquals("https://okapiframework.org/wiki", tu.getSource().toString());
		params.setEscapeForwardSlashes(true);
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertEquals("https://okapiframework.org/wiki", tu.getSource().toString());
	}

	@Test
	public void testObject () {
		String snippet = "{\"key\" : { \"key2\" : \"Text1\" } }";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertTrue(tu!=null);
	}

	@Test
	public void testValue () {
		String snippet = "{\"key\" : \"Text1\"}";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertTrue(tu!=null);
	}

	@Test
	public void testAllWithKeyNoException () {
		String snippet = "{ \"key1\" : \"Text1\" }";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("Text1", tu.getSource().toString());
		assertEquals("key1", tu.getName());
	}

	@Test
	public void testAllWithKeywithException () {
		String snippet = "{ \"key1\" : \"Text1\" }";
		Parameters params = new Parameters(); // Default: all with keys
		params.setExceptions("key?"); // Except those
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertTrue(tu==null);
	}

	@Test
	public void testNoneWithKeywithException () {
		String snippet = "{ \"key1\" : \"Text1\" }";
		Parameters params = new Parameters();
		params.setExtractAllPairs(false); // None with key
		params.setExceptions("key?"); // Except those
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertNotNull(tu);
		assertEquals("Text1", tu.getSource().toString());
		assertEquals("key1", tu.getName());
	}

	@Test
	public void testPath () {
		String snippet = "{\"key\" : [{ \"key1\" : \"Text1\"}, {\"key2\" : \"Text1\"}], \"key3\": \"Text3\" }";
		Parameters params = new Parameters();
		params.setUseKeyAsName(true);
		params.setUseFullKeyPath(true);
		List<Event> events = getEvents(snippet, params);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		assertEquals("/key/key1", tu.getName());
		tu = FilterTestDriver.getTextUnit(events, 2);
		assertNotNull(tu);
		assertEquals("/key/key2", tu.getName());
		tu = FilterTestDriver.getTextUnit(events, 3);
		assertNotNull(tu);
		assertEquals("/key3", tu.getName());
	}

	@Test
	public void testLeadingSlash () {
		String snippet = "{\"key\" : [{ \"key1\" : \"Text1\"}, {\"key2\" : \"Text1\"}], \"key3\": \"Text3\" }";
		Parameters params = new Parameters();
		params.setUseKeyAsName(true);
		params.setUseFullKeyPath(true);

		// Without the slash
		params.setUseLeadingSlashOnKeyPath(false);
		List<Event> events = getEvents(snippet, params);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		assertEquals("key/key1", tu.getName());
		tu = FilterTestDriver.getTextUnit(events, 2);
		assertNotNull(tu);
		assertEquals("key/key2", tu.getName());
		tu = FilterTestDriver.getTextUnit(events, 3);
		assertNotNull(tu);
		assertEquals("key3", tu.getName());

		// With the slash
		params.setUseLeadingSlashOnKeyPath(true);
		events = getEvents(snippet, params);
		tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		assertEquals("/key/key1", tu.getName());
		tu = FilterTestDriver.getTextUnit(events, 2);
		assertNotNull(tu);
		assertEquals("/key/key2", tu.getName());
		tu = FilterTestDriver.getTextUnit(events, 3);
		assertNotNull(tu);
		assertEquals("/key3", tu.getName());
	}

	@Test
	public void testStandaloneYes () {
		String snippet = "{ \"key\" : [ \"Text1\", \"Text2\" ] }";
		Parameters params = new Parameters();
		params.setExtractStandalone(true);
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertNotNull(tu);
		assertEquals("Text1", tu.getSource().toString());
		assertEquals("key", tu.getName());
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 2);
		assertNotNull(tu);
		assertEquals("Text2", tu.getSource().toString());
	}

	@Test
	public void testStandaloneDefaultWhichIsNo () {
		String snippet = "{ \"key\" : [[ \"Text1\" ], [ \"Text2\" ]] }";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertTrue(tu==null);
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 2);
		assertTrue(tu==null);
	}

	@Test
	public void testSmartQuotes() {
		String snippet = "{ \"key1\" : \"What is (and 'isn’t) special about science\" }";
		Parameters p = new Parameters();
		p.setSubfilter("okf_html");
		List<Event> events = getEvents(snippet, p);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		assertEquals("What is (and 'isn’t) special about science", tu.getSource().toString());
		assertEquals("key1_1", tu.getName());
		String expected = "{ \"key1\" : \"What is (and &#39;isn’t) special about science\" }";
		// Output is raw character because it's UTF-8
		assertEquals(expected, FilterTestDriver.generateOutput(events,
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testEscape () {
		String snippet = "{ \"key1\" : \"agrave=\\u00E0\" }";
		List<Event> events = getEvents(snippet, null);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		assertEquals("agrave=\u00e0", tu.getSource().toString());
		assertEquals("key1", tu.getName());
		String expected = "{ \"key1\" : \"agrave=\u00e0\" }";
		// Output is raw character because it's UTF-8
		assertEquals(expected, FilterTestDriver.generateOutput(events,
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testEscapes () {
		String snippet = "  {\n  \"key1\" :  \"abc\\n\\\"\\\\ \\b\\f\\t\\r\\/\"  } \r ";
		List<Event> events = getEvents(snippet, null);
		// Content = '\"'
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertEquals("abc\n\"\\ \b\f\t\r/", tu.getSource().toString());
		assertEquals(snippet, FilterTestDriver.generateOutput(events,
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testWhiteSpaceAndComments () {
		String snippet = "/*comment*/<!--comment-->#comment\n //comment\r\n { #comment\n    \"key\"     :     //comment\n \n\n\n \n  { \"key2\" : /*comment*/ \"<!--html in value comment-->value\"}  } ";
		List<Event> events = getEvents(snippet, null);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertEquals("<!--html in value comment-->value", tu.getSource().toString());
		assertEquals(snippet, FilterTestDriver.generateOutput(events,
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testMultilineComment() {
		String snippet = "/*comment*/<!--comment-->{\"key\" :  { \"key2\" : \"value\"}  } ";
		List<Event> events = getEvents(snippet, null);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertEquals("value", tu.getSource().toString());
		assertEquals(snippet, FilterTestDriver.generateOutput(events,
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testNestedComments () {
		String snippet = "/*c1/*c2/*c3*/*/*/{\"key\" : { \"key2\" : \"value\"}} ";
		List<Event> events = getEvents(snippet, null);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertEquals("value", tu.getSource().toString());
		assertEquals(snippet, FilterTestDriver.generateOutput(events,
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testEmptyValue() {
		String snippet = "{\"templateOverridePath\": \"\"}";
		List<Event> events = getEvents(snippet, null);
		assertEquals(snippet, FilterTestDriver.generateOutput(events,
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testDecimalNumber() {
		String snippet = "{\"northeast\":{\"lat\":37.8302885,\"lng\":-122.4766272}}";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertTrue(tu==null);
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 2);
		assertTrue(tu==null);
	}

	@Test
	public void testDoubleExtraction () {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/test01.json").toString(), null));
		list.add(new InputDocument(root.in("/test02.json").toString(), null));
		list.add(new InputDocument(root.in("/test03.json").toString(), null));
		list.add(new InputDocument(root.in("/test04.json").toString(), null));
		list.add(new InputDocument(root.in("/test05.json").toString(), null));
		list.add(new InputDocument(root.in("/test06.json").toString(), null));
		list.add(new InputDocument(root.in("/test08.json").toString(), null));
		list.add(new InputDocument(root.in("/test09.json").toString(), null));
		list.add(new InputDocument(root.in("/books.json").toString(), null));
		list.add(new InputDocument(root.in("/geo.json").toString(), null));
		list.add(new InputDocument(root.in("/array-test.json").toString(), null));
		list.add(new InputDocument(root.in("/twitter.json").toString(), null));
		list.add(new InputDocument(root.in("/metadata.json").toString(), null));
		list.add(new InputDocument(root.in("/1EdwardParallax.json").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN));
	}

	@Test
	public void testDoubleExtractionOnPreviousFailure () {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/customer_form.json").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN));
	}

	@Test
	public void testDoubleExtractionOnInvalid () {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/invalid_by_most_processors.json").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN));
	}

	@Test
	public void testDefaultInfo () {
		assertNotNull(filter.getParameters());
		assertNotNull(filter.getName());
		assertNotNull(filter.getDisplayName());
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertTrue(list.size()>0);
	}

	@Test
	public void testSimpleEntrySkeleton () {
		String snippet = "  {\r  \"key1\" :  \"Text1\"  } \r ";
		assertEquals(snippet, FilterTestDriver.generateOutput(getEvents(snippet, null),
				filter.getEncoderManager(), locEN));
	}

	@Test
	public void testLineBreaks () {
		String snippet = "{ \"key1\" : \"Text1\" }\r";
		StartDocument sd = FilterTestDriver.getStartDocument(getEvents(snippet, null));
		assertNotNull(sd);
		assertEquals("\r", sd.getLineBreak());
	}

	@Test
	public void testSubfilter() throws IOException {
		// This is testing:
		//  - JSON unescaping (\\b to \b, \\n to \n, \" to ")
		//  - JSON output escaping (/ to \/, \b to \\b)
		//  - Subfilter whitespace folding (\t, \n disappear) and output escaping (& to &amp;, " to &quot;)
		//  - Subfilter parsing of block-level (<p>) and inline tags (<i>)
		String snippet = "{ \"key1\" : \"<p>Hello, \\t \\\"<i>crazy</i>\\\" \\b world & friends!\\n\\n<\\/p> \\n!\" }";
		Parameters params = new Parameters();
		params.setSubfilter("okf_html");
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertNotNull(tu);
		assertEquals("key1_1", tu.getName());
		assertEquals("sg1_sf1_tu1", tu.getId());
		assertEquals("Hello, \"<i>crazy</i>\" \b world & friends!", tu.getSource().toString());
		assertEquals("Hello, \"crazy\" \b world & friends!", tu.getSource().getCodedText());

		String expected = "{ \"key1\" : \"<p>Hello, &quot;<i>crazy<\\/i>&quot; \\b world &amp; friends!<\\/p>!\" }";
		assertEquals(expected, eventWriter(snippet, null));
	}

	@Test
	public void testSubfilterEasyToDebug() throws IOException {
		// This is testing:
		//  - JSON unescaping (\\b to \b, \\n to \n, \" to ")
		//  - JSON output escaping (/ to \/, \b to \\b)
		//  - Subfilter whitespace folding (\t, \n disappear) and output escaping (& to &amp;, " to &quot;)
		//  - Subfilter parsing of block-level (<p>) and inline tags (<i>)
		String snippet = "{ \"key1\" : \"H\\t\\bH\" }";
		Parameters params = new Parameters();
		params.setSubfilter("okf_html");
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, params), 1);
		assertNotNull(tu);
		assertEquals("key1_1", tu.getName());
		assertEquals("sg1_sf1_tu1", tu.getId());
		assertEquals("H \bH", tu.getSource().toString());
		assertEquals("H \bH", tu.getSource().getCodedText());

		String expected = "{ \"key1\" : \"H \\bH\" }";
		assertEquals(expected, eventWriter(snippet, null));
	}

	@Test
	public void testSubFilterDoubleExtraction () {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/test07-subfilter.json").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();

		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN));

		setSubfilterMapping("net.sf.okapi.filters.html.HtmlFilter");
		Parameters params = new Parameters();
		params.setSubfilter("okf_html");
		filter.setParameters(params);

		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN));
	}

	@Test
	public void testSubfiltersProduceDistinctTextUnitIds() throws Exception {
		setSubfilterMapping("net.sf.okapi.filters.html.HtmlFilter");
		InputStream input = FileLocation.fromClass(getClass()).in("/test07-subfilter.json").asInputStream();
		RawDocument rd = new RawDocument(input, "UTF-8", LocaleId.ENGLISH);
		Parameters params = new Parameters();
		params.setSubfilter("okf_html");
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, rd, params));
		HashSet<String> ids = new HashSet<>();
		for (ITextUnit tu : tus) {
			ids.add(tu.getId());
		}
		assertEquals(tus.size(), ids.size());
	}

	@Test
	public void testEscapeForwardSlashes() throws IOException{
		// Default behavior to maintain backwards compatibility
		String snippet = "{ \"key1\" : \"http://www.google.com\" }";
		assertEquals("{ \"key1\" : \"http:\\/\\/www.google.com\" }", eventWriter(snippet, null));
	}

	@Test
	public void testNoEscapeForwardSlashes() throws IOException{
		// Set to not escape forward slashes
		Parameters params = new Parameters();
		params.setEscapeForwardSlashes(false);
		String snippet = "{ \"key1\" : \"http://www.google.com\" }";
		assertEquals("{ \"key1\" : \"http://www.google.com\" }", eventWriter(snippet, params));
	}


	@Test
	public void testEscapeForwardSlashesSubfilter() throws IOException{
		setSubfilterMapping("net.sf.okapi.filters.html.HtmlFilter");
		Parameters params = new Parameters();
		params.setSubfilter("okf_html");
		params.setEscapeForwardSlashes(false);
		String snippet = "{\"key1\": \"<a href=\\\"http://www.google.com\\\">http://www.google.com</a>\"}";
		assertEquals("{\"key1\": \"<a href=\\\"http://www.google.com\\\">http://www.google.com</a>\"}", eventWriter(snippet, params));
	}

	@Test
	public void testInlineCodeFinderEscaping() throws IOException {
		Parameters params = new Parameters();
		params.setUseCodeFinder(true);
		params.setEscapeForwardSlashes(false);
		String snippet = "{ \"foo\": \"<a href=\\\"http://localhost\\\" title=\\\"hello world\\\">translate me</a>\" }";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
				FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals("\uE103\uE110translate me\uE103\uE111", tus.get(0).getSource().getCodedText());
		assertEquals(snippet, eventWriter(snippet, params));
	}

	@Test
	public void testInlineCodeFinderNewLineCharacter() throws IOException {
		Parameters params = new Parameters();
		params.setUseCodeFinder(true);
		params.getCodeFinder().addRule("</?([A-Z0-9a-z]*)\\b[^>]*>");
		params.getCodeFinder().addRule("\n");

		String snippet = "{ \"foo\": \"This is \\n code test\" }";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
				FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		TextContainer textContainer = tus.get(0).getSource();
		assertEquals("\\n", textContainer.getFirstContent().getCode(0).getDisplayText());
		assertEquals("This is \uE103\uE110 code test", textContainer.getCodedText());
		assertEquals(snippet, eventWriter(snippet, params));
	}

	@Test
	public void testNoteRules() {
		Parameters params = new Parameters();
		params.setNoteRules("date|description");
		
		String snippet = "{ \"description\": \"blah blah blah\", \"foo\": \"Extract me\" }";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(1, tus.size());
		ITextUnit tu = tus.get(0);
		TextContainer so = tu.getSource();
		assertEquals("Extract me", so.getCodedText());
		NoteAnnotation notes = tu.getAnnotation(NoteAnnotation.class);
		Note n = notes.getNote(0);
		assertEquals("blah blah blah", n.getNoteText());
		assertEquals("description", n.getFrom());
	}
	
	@Test
	public void testIdRules() {
		Parameters params = new Parameters();
		params.setIdRules("id");
		
		// ID rule matches
		String snippet = "{ \"id\": \"1234567890\", \"foo\": \"Extract me\" }";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(1, tus.size());
		ITextUnit tu = tus.get(0);
		TextContainer so = tu.getSource();
		assertEquals("Extract me", so.getCodedText());
		assertEquals("1234567890", tu.getName());
		
		// ID rule does not match
		snippet = "{ \"foo\": \"Extract me\" }";
		tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(1, tus.size());
		tu = tus.get(0);
		so = tu.getSource();
		assertEquals("Extract me", so.getCodedText());
		assertEquals("foo", tu.getName());
	}
	
	@Test
	public void testGenericMetaRules() {
		Parameters params = new Parameters();
		params.setGenericMetaRules("foo");
		
		// GenericMeta rule matches
		String snippet = "{ \"name\": \"value\", \"foo\": \"generic meta\" }";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(1, tus.size());
		ITextUnit tu = tus.get(0);
		GenericAnnotations annos = tu.getAnnotation(GenericAnnotations.class);
		TextContainer so = tu.getSource();
		assertEquals("value", so.getCodedText());
		assertEquals("name", tu.getName());
		assertEquals("generic meta", annos.getAllAnnotations().get(0).getString("foo"));
		
		// No GenericMeta rule matches
		params = new Parameters();
		snippet = "{ \"name\": \"value\", \"foo\": \"generic meta\" }";
		tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(2, tus.size());
		tu = tus.get(0);
		so = tu.getSource();
		assertEquals("value", so.getCodedText());
		assertEquals("name", tu.getName());
		
		tu = tus.get(1);
		so = tu.getSource();
		assertEquals("generic meta", so.getCodedText());
		assertEquals("foo", tu.getName());
		assertNull(tu.getProperty("foo"));
	}
	
	@Test
	public void testExtractionRules() {
		Parameters params = new Parameters();
		params.setExtractionRules("foo");
		
		// GenericMeta rule matches
		String snippet = "{ \"name\": \"value\", \"foo\": \"extract me\" }";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(1, tus.size());
		ITextUnit tu = tus.get(0);
		TextContainer so = tu.getSource();
		assertEquals("extract me", so.getCodedText());
		assertEquals("foo", tu.getName());
		
		// No new extraction rules (old extraction logic)
		params = new Parameters();
		tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(2, tus.size());
		tu = tus.get(0);
		so = tu.getSource();
		assertEquals("value", so.getCodedText());
		assertEquals("name", tu.getName());
		
		tu = tus.get(1);
		so = tu.getSource();
		assertEquals("extract me", so.getCodedText());
		assertEquals("foo", tu.getName());
		
		// no new extraction rules with exceptions
		params = new Parameters();
		params.setExceptions("foo");
		tus = FilterTestDriver.filterTextUnits(
			FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		assertEquals(1, tus.size());
		tu = tus.get(0);
		so = tu.getSource();
		assertEquals("value", so.getCodedText());
		assertEquals("name", tu.getName());
				
	}
	
	@Test
	public void metaDataAndExtractionRulesWithSubfilter() throws IOException {
		FileLocation location = FileLocation.fromClass(getClass());

		Parameters params = new Parameters();
		params.load(location.in("/metadata.fprm").asUrl(), false);
		filter.setParameters(params);
		setSubfilterMapping("net.sf.okapi.filters.html.HtmlFilter");
		
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
				FilterTestDriver.getEvents(filter, TestUtil.getFileAsString(location.in("/metadata.json").asFile()), params, locEN, null));
		assertEquals(3, tus.size());
		
		ITextUnit tu = tus.get(0);
		GenericAnnotations annos = tu.getAnnotation(GenericAnnotations.class);

		TextContainer so = tu.getSource();
		assertEquals("blurb on the year of the Tiger...", so.getCodedText());
		assertEquals("115013866768", tu.getName());
		assertEquals("http://foo/bar/tiger.png", annos.getAllAnnotations().get(0).getString("/widgets/image"));
		NoteAnnotation notes = tu.getAnnotation(NoteAnnotation.class);
		Note n = notes.getNote(0);
		assertEquals("The Year of the Tiger", n.getNoteText());
		assertEquals("name", n.getFrom());
		
		tu = tus.get(1);
		annos = tu.getAnnotation(GenericAnnotations.class);
		so = tu.getSource();
		assertEquals("blurb on the year of the Rabbit...", so.getCodedText());
		assertEquals("115013866769", tu.getName());
		assertEquals("http://foo/bar/rabbit.png", annos.getAllAnnotations().get(0).getString("/widgets/image"));
		notes = tu.getAnnotation(NoteAnnotation.class);
		n = notes.getNote(0);
		assertEquals("The Year of the Rabbit", n.getNoteText());
		assertEquals("name", n.getFrom());
		
		tu = tus.get(2);
		annos = tu.getAnnotation(GenericAnnotations.class);
		so = tu.getSource();
		assertEquals("blurb on the year of the Snake...", so.getCodedText());
		assertEquals("115013866770", tu.getName());
		assertEquals("http://foo/bar/snake.png", annos.getAllAnnotations().get(0).getString("/widgets/image"));
		notes = tu.getAnnotation(NoteAnnotation.class);
		n = notes.getNote(0);
		assertEquals("The Year of the Snake", n.getNoteText());
		assertEquals("name", n.getFrom());
	}
	
	private ArrayList<Event> getEvents(String snippet, IParameters params) {
		setSubfilterMapping("net.sf.okapi.filters.html.HtmlFilter");
		return FilterTestDriver.getEvents(filter, snippet, params, locEN, null);
	}

	private void setSubfilterMapping(String filterClass) {
		FilterConfigurationMapper mapper = new FilterConfigurationMapper();
		mapper.addConfigurations(filterClass);
		filter.setFilterConfigurationMapper(mapper);
	}

	private String eventWriter(String input, Parameters parameters) throws IOException {
		if (parameters != null) {
			filter.setParameters(parameters);
		}

		try (IFilterWriter writer = filter.createFilterWriter()) {
			// Open the input
			filter.open(new RawDocument(input, LocaleId.ENGLISH, LocaleId.SPANISH));

			// Prepare the output
			writer.setOptions(LocaleId.SPANISH, "UTF-8");
			ByteArrayOutputStream writerBuffer = new ByteArrayOutputStream();
			writer.setOutput(writerBuffer);

			// Process the document
			Event event;
			while (filter.hasNext()) {
				event = filter.next();
				writer.handleEvent(event);
			}
			writerBuffer.close();
			return new String(writerBuffer.toByteArray(), StandardCharsets.UTF_8);
		}
	}
}
