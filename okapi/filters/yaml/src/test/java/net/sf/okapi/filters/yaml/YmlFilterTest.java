/*===========================================================================
  Copyright (C) 2010-2019 by the Okapi Framework contributors
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

package net.sf.okapi.filters.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

import net.sf.okapi.filters.html.HtmlFilter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class YmlFilterTest {
	
	private YamlFilter filter;
	private FileLocation root;

	@Before
	public void setUp() {
		filter = new YamlFilter();
		root = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void testDefaultInfo() {
		assertNotNull(filter.getName());
		assertNotNull(filter.getDisplayName());
		assertNotNull(filter.getParameters());
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertTrue(list.size() > 0);
	}

	@Test
	public void testStartDocument () {
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(filter,
			new InputDocument(root.in("/yaml/Test01.yml").toString(), null),
			"UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH));
	}

	@Test
	public void testSimpleYaml() {
		String snippet = "config:\n  title: \"My Rails Website\"";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("My Rails Website", tu.getSource().toString());
		assertEquals("config/title", tu.getName());
	}

	@Test
	public void testSimplePlaceholders() {
		String snippet = "config:\n  title: \"My {{count}} Rails Website\"";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("{{count}}", tu.getSource().getFirstContent().getCode(0).toString());
		assertEquals("My {{count}} Rails Website", tu.getSource().toString());
		assertEquals("config/title", tu.getName());
	}
	
	@Test
	public void emptyKey() {
		String snippet = "- test";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test", tu.getSource().toString());
		assertEquals("", tu.getName());
	}
	
	@Test
	public void nonEmptyKey() {
		String snippet = "test: test";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test", tu.getSource().toString());
		assertEquals("test", tu.getName());
		
		snippet = "test: 'test'";
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test", tu.getSource().toString());
		assertEquals("test", tu.getName());
		
		snippet = "test: \"test\"";
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test", tu.getSource().toString());
		assertEquals("test", tu.getName());
	}
	
	@Test
	public void list() {
		String snippet = "test: [\"test1\", \"test2\", \"test3\"]";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test1", tu.getSource().toString());
		assertEquals("test", tu.getName());
		
		snippet = "test: [test1, test2, test3]";
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test1", tu.getSource().toString());
		assertEquals("test", tu.getName());
		
		snippet = "- [test1, test2, test3]";
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test1", tu.getSource().toString());
		assertEquals("", tu.getName());
	}
	
	@Test
	public void listSingleQuote() {
		String snippet = "test: ['test1','test2','test3']";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test1", tu.getSource().toString());
		assertEquals("test", tu.getName());			
	}

	@Test
	public void map() {
		String snippet = "- {1: test1, 2: test2, 3: test3}";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test1", tu.getSource().toString());
		assertEquals("1", tu.getName());
		
		snippet = "test: {1: test1, 2: test2, 3: test3}";
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test1", tu.getSource().toString());
		assertEquals("test/1", tu.getName());
		
		snippet = "test: {1: \"test1\", 2:\"test2\", 3:\"test3\"}";
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("test1", tu.getSource().toString());
		assertEquals("test/1", tu.getName());
	}
	
	@Test
	public void mapWithEmptyKeys() {
		String snippet = "order: [ :day, :month, :year ]";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals(":day", tu.getSource().toString());
		assertEquals("order", tu.getName());
	}
	
	@Test
	public void mapWithEmptyKeysQuoted() {
		String snippet = "order: [ \":day\", \":month\", \":year\" ]";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals(":day", tu.getSource().toString());
		assertEquals("order", tu.getName());
	}
	
	@Test
	public void issue555() {
		String snippet = "test: \"'s house\"";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("'s house", tu.getSource().toString());
		
		snippet = "test: \"'hello'\"";
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertNotNull(tu);
		assertEquals("'hello'", tu.getSource().toString());
	}
	
	@Test
	public void issue556() {
		String snippet = "html: \"Visit <a href=\\\"http://www.google.com\\\">Google</a>\"";
		FilterConfigurationMapper mapper = new FilterConfigurationMapper();
		mapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		Parameters params = filter.getParameters();
		params.setSubfilter("okf_html");
		params.setUseCodeFinder(false);
		filter.setParameters(params);
		filter.setFilterConfigurationMapper(mapper);
		RawDocument rd = new RawDocument(snippet, LocaleId.ENGLISH);
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, rd, null));
		assertEquals(1, tus.size());
		assertEquals("Visit [#$sg1_sf1_dp1]Google</a>", tus.get(0).getSource().toString());
	}
	
	@Test
	public void testDoubleExtraction() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/Test02.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/en.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/Test01.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/Test03.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/big_config.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/comment_issue.yml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	@Test
	public void testDoubleExtractionWithEscapes() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/escapes.yml").toString(), null));
		// FIXME: see snakeyaml issue https://code.google.com/p/snakeyaml/issues/detail?id=205
		// Generally surrogates are not allowed per YAML 1.1 spec in some cases
		//list.add(new InputDocument(root.in("/yaml/issues/ios_emoji_surrogate.yaml").toString(), null));
		//list.add(new InputDocument(root.in("/yaml/emoji1.yaml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	@Test
	public void testFlow() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/flow_sample.yml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	@Test
	public void testScalars() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/line_continuation.yml").toString(), null));		
		list.add(new InputDocument(root.in("/yaml/literal.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/plain_wrapped_lf.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/scalar_sample.yml").toString(), null));


		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	@Test
	public void testDoubleExtractionWithMultilines() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/folded_indented.yml").toString(), null));		
		list.add(new InputDocument(root.in("/yaml/single_wrapped.yml").toString(), null));		
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	@Test
	public void testDoublePlainWithQuotes() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/plain_with_single_quotes.yaml").toString(), null));				
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	
	@Test
	public void testDoubleExtractionLongLine() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/Test01.yml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/long_line.yml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}

	@Test
	public void testMultilineValue() throws Exception {
		String snippet = "long_line: |-\n    This is a\n   very long line.";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, null), 1);
		assertEquals("This is a\n   very long line.", tu.getSource().toString());
	}

	@Test
	public void testDoubleExtractionNonStrings() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/non_strings.yaml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	@Test
	public void testRoundtripFailures() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/ruby/ruby1_lf.yaml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/issues/issue56-1_lf.yaml").toString(), null));
		list.add(new InputDocument(root.in("/yaml/spec_test/example2_18_lf.yaml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}

	@Test
	public void commentsAfterPlainScalarsPreserved() {
		String snippet = "a plain scalar with # a true comment\n" + "";
		List<Event> events = getEvents(snippet, new Parameters());
		assertEquals(6, events.size());
		assertSame(EventType.TEXT_UNIT, events.get(2).getEventType());
		assertEquals("a plain scalar with", events.get(2).getTextUnit().getSource().toString());
		assertSame(EventType.DOCUMENT_PART, events.get(4).getEventType());
		assertEquals(" # a true comment\n", events.get(4).getDocumentPart().toString());

		snippet = "a plain scalar with\n" +
			"# a comment on the next line\n" + "";
		events = getEvents(snippet, new Parameters());
		assertEquals(6, events.size());
		assertSame(EventType.TEXT_UNIT, events.get(2).getEventType());
		assertEquals("a plain scalar with", events.get(2).getTextUnit().getSource().toString());
		assertSame(EventType.DOCUMENT_PART, events.get(4).getEventType());
		assertEquals("\n# a comment on the next line\n", events.get(4).getDocumentPart().toString());
	}

	@Test
	public void commentsAfterPlainScalarMappingValuesPreserved() {
		String snippet = "map:\n" +
			"  - key1: \"value1\"\n" +
			"    key2: value2 # a comment after a plain scalar mapping value\n";
		List<Event> events = getEvents(snippet, new Parameters());
		assertEquals(18, events.size());
		assertSame(EventType.TEXT_UNIT, events.get(12).getEventType());
		assertEquals("value2", events.get(12).getTextUnit().getSource().toString());
		assertSame(EventType.END_GROUP, events.get(14).getEventType());
		assertEquals(
			" # a comment after a plain scalar mapping value\n",
			events.get(14).getEnding().toString()
		);

		snippet = "map:\n" +
			"  - key1: \"value1\"\n" +
			"    key2: value2\n" +
			"# a comment on the next line after a plain scalar mapping value\n";
		events = getEvents(snippet, new Parameters());
		assertEquals(18, events.size());
		assertSame(EventType.TEXT_UNIT, events.get(12).getEventType());
		assertEquals("value2", events.get(12).getTextUnit().getSource().toString());
		assertSame(EventType.END_GROUP, events.get(14).getEventType());
		assertEquals(
			"\n# a comment on the next line after a plain scalar mapping value\n",
			events.get(14).getEnding().toString()
		);
	}

	@Test
	public void commentsAfterScalarsRoundTripped() {
		Arrays.asList("/yaml/967-1.yaml", "/yaml/967-2.yaml").forEach(
			p -> {
				try {
					assertEquals(
						TestUtil.inputStreamAsString(root.in(p).asInputStream()),
						FilterTestDriver.generateOutput(
							FilterTestDriver.getEvents(
								filter,
								new RawDocument(
									root.in(p).asInputStream(),
									StandardCharsets.UTF_8.name(),
									LocaleId.ENGLISH
								),
								new Parameters()
							),
							new EncoderManager(),
							LocaleId.ENGLISH
						)
					);
				} catch (IOException e) {
					fail("I/O error: ".concat(e.toString()));
				}
			}
		);
	}

	@Ignore("very strange even pyyaml doesn't like it")
	public void testAnchorsAndAlias() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/recursive/with-children-pretty.yaml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}
	
	@Test
	public void testOpenTwiceWithString() {
		RawDocument rawDoc = new RawDocument("config:\n  title: \"My Rails Website\"", LocaleId.ENGLISH);
		filter.open(rawDoc);
		filter.open(rawDoc);
		filter.close();
	}

	@Test
	public void testDoubleSubfilter() {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/yaml/Issue556.yml").toString(), null));				
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
				LocaleId.ENGLISH));
	}

	@Test
	public void testSubfiltering() throws Exception {
		FilterConfigurationMapper mapper = new FilterConfigurationMapper();
		mapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		Parameters params = filter.getParameters();
		params.setSubfilter("okf_html");
		params.setUseCodeFinder(false);
		filter.setParameters(params);
		filter.setFilterConfigurationMapper(mapper);
		RawDocument rd = new RawDocument(root.in("/yaml/subfilter.yml").asInputStream(),
							"UTF-8", LocaleId.ENGLISH);
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, rd, null));
		assertEquals(2, tus.size());
		assertEquals("Hello world.", tus.get(0).getSource().toString());
		assertEquals("Hello again, world.", tus.get(1).getSource().toString());
		assertNotEquals(tus.get(0).getId(), tus.get(1).getId());
	}

	@Test
	public void testSubFilterProcessLiteralAsBlock() throws Exception {
		FilterConfigurationMapper mapper = new FilterConfigurationMapper();
		mapper.addConfigurations(HtmlFilter.class.getName());
		YamlFilter filter = new YamlFilter();
		Parameters params = filter.getParameters();
		params.setSubfilter("okf_html");
		params.setSubFilterProcessLiteralAsBlock(true);
		params.setUseCodeFinder(false);
		filter.setParameters(params);
		filter.setFilterConfigurationMapper(mapper);
		RawDocument rd = new RawDocument(root.in("/yaml/literal_html.yml").asInputStream(),
			"UTF-8", LocaleId.ENGLISH);
		assertEquals(9, TestUtil.inputStreamAsString(rd.getStream()).split("\n").length);
		List<Event> events = FilterTestDriver.getEvents(filter, rd, params);
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
		assertEquals(5, tus.size());
		assertEquals("Some literal HTML text that should be processed as a block", tus.get(0).getSource().toString());
		assertEquals("item 1", tus.get(1).getSource().toString());
		assertEquals("item 2", tus.get(2).getSource().toString());
		assertEquals("item 3", tus.get(3).getSource().toString());
		assertEquals("not literal", tus.get(4).getSource().toString());
		assertNotEquals(tus.get(0).getId(), tus.get(1).getId());

        EncoderManager em = new EncoderManager();
        em.setAllKnownMappings();
        String output = FilterTestDriver.generateChangedOutput(events, em, LocaleId.ENGLISH);
		assertEquals(3, output.split("\n").length);
        assertEquals(
			TestUtil.inputStreamAsString(root.in("/yaml/literal_html_expected.yml").asInputStream()),
            output);
	}

    @Test
    public void testRoundTripSubFilterProcessLiteralAsBlock() {
        ArrayList<InputDocument> list = new ArrayList<>();
        list.add(new InputDocument(root.in("/yaml/literal_html.yml").toString(), "okf_yaml@literal_html.fprm"));
        YamlFilter filter = new YamlFilter();
        FilterConfigurationMapper mapper = new FilterConfigurationMapper();
        mapper.addConfigurations(HtmlFilter.class.getName());
        Parameters params = filter.getParameters();
        params.setSubfilter("okf_html");
        params.setSubFilterProcessLiteralAsBlock(true);
        params.setUseCodeFinder(false);
        filter.setParameters(params);
        filter.setFilterConfigurationMapper(mapper);

        RoundTripComparison rtc = new RoundTripComparison();
        assertTrue(rtc.executeCompare(filter, list, "UTF-8", LocaleId.ENGLISH,
            LocaleId.ENGLISH));
    }

	private ArrayList<Event> getEvents(String snippet, IParameters params) {
		ArrayList<Event> list = new ArrayList<>();
		filter.open(new RawDocument(snippet, LocaleId.ENGLISH));
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}
}
