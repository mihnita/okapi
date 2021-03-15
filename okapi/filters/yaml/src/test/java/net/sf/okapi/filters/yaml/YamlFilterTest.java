package net.sf.okapi.filters.yaml;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class YamlFilterTest {
	private YamlFilter filter;
	private FileLocation root;
	private LocaleId locEN = LocaleId.fromString("en");

	@Before
	public void setUp() {
		filter = new YamlFilter();
		root = FileLocation.fromClass(this.getClass());
	}

	@After
	public void tearDown() {
		filter.close();
	}

	@Test
	public void testInlineCodeFinderNewLineCharacterDoubleQuotedString() {
		Parameters params = new Parameters();
		params.setUseCodeFinder(true);
		params.getCodeFinder().addRule("</?([A-Z0-9a-z]*)\\b[^>]*>");
		params.getCodeFinder().addRule("\\n");

		String snippet = "\"This is \\n code test\"";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
				FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		TextContainer textContainer = tus.get(0).getSource();
		assertEquals("\n", textContainer.getFirstContent().getCode(0).getDisplayText());
		assertEquals("This is \uE103\uE110 code test", textContainer.getCodedText());
		//assertEquals(snippet, eventWriter(snippet, params));
	}

	@Test
	public void testInlineCodeFinderNewLineCharacterSingleQuotedString() throws IOException {
		Parameters params = new Parameters();
		params.setUseCodeFinder(true);
		params.getCodeFinder().addRule("</?([A-Z0-9a-z]*)\\b[^>]*>");
		params.getCodeFinder().addRule("\\n");

		String snippet = "\'This is \\n code test\'";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
				FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		TextContainer textContainer = tus.get(0).getSource();
		assertEquals("\\n", textContainer.getFirstContent().getCode(0).getDisplayText());
		assertEquals("This is \uE103\uE110 code test", textContainer.getCodedText());
		assertEquals(snippet, eventWriter(snippet, params));
	}

	@Test
	public void testInlineCodeFinderNewLineCharacterStringWithoutQuotes() throws IOException {
		Parameters params = new Parameters();
		params.setUseCodeFinder(true);
		params.getCodeFinder().addRule("</?([A-Z0-9a-z]*)\\b[^>]*>");
		params.getCodeFinder().addRule("\\n");

		String snippet = "This is \\n code test";
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(
				FilterTestDriver.getEvents(filter, snippet, params, locEN, null));
		TextContainer textContainer = tus.get(0).getSource();
		assertEquals("\\n", textContainer.getFirstContent().getCode(0).getDisplayText());
		assertEquals("This is \uE103\uE110 code test", textContainer.getCodedText());
		assertEquals(snippet, eventWriter(snippet, params));
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