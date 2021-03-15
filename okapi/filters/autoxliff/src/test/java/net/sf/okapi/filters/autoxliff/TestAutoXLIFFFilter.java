package net.sf.okapi.filters.autoxliff;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.SAXException;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.filters.xliff.SdlXliffSkeletonWriter;

@RunWith(JUnit4.class)
public class TestAutoXLIFFFilter {
	private FileLocation fl = FileLocation.fromClass(getClass());
	private AutoXLIFFFilter filter = null;

	@Before
	public void setup() {
		filter = new AutoXLIFFFilter();
	}

	@Test
	public void testDelegateXLIFF20() throws Exception {
		RawDocument rd = new RawDocument(fl.in("/xliff2.xlf").asInputStream(), "UTF-8", LocaleId.ENGLISH);
		rd.setTargetLocale(LocaleId.FRENCH);
		List<Event> events = FilterTestDriver.getEvents(filter, rd, null);
		// Make sure we save the correct filter config
		StartDocument sd = events.get(0).getStartDocument();
		assertEquals(filter.getParameters().toString(), sd.getFilterParameters().toString());
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
		assertEquals(1, tus.size());
		assertEquals("Sample segment.", tus.get(0).getSource().toString());
		roundtrip("/gold/xliff2.xlf", rd, events);
	}

	@Test
	public void testDelegateXLIFF12() throws Exception {
		RawDocument rd = new RawDocument(fl.in("/xliff12.xlf").asInputStream(), "UTF-8", LocaleId.ENGLISH);
		rd.setTargetLocale(LocaleId.GERMAN);
		List<Event> events = FilterTestDriver.getEvents(filter, rd, null);
		// Make sure we save the correct filter config
		StartDocument sd = events.get(0).getStartDocument();
		assertEquals(filter.getParameters().toString(), sd.getFilterParameters().toString());
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
		assertEquals(1, tus.size());
		assertEquals("Segment one.", tus.get(0).getSource().toString());
		roundtrip("/gold/xliff12.xlf", rd, events);
	}

	@Test
	public void testDelegateSDLXLIFF() throws Exception {
		// Assume XLIFF 1.2 files are SDLXLIFF
		filter.getParameters().setXLIFF12Config("okf_xliff-sdl");
		RawDocument rd = new RawDocument(fl.in("/sdlxliff.xlf").asInputStream(), "UTF-8", LocaleId.ENGLISH);
		rd.setTargetLocale(LocaleId.FRENCH);
		List<Event> events = FilterTestDriver.getEvents(filter, rd, null);
		// Make sure we save the correct filter config
		StartDocument sd = events.get(0).getStartDocument();
		assertEquals(filter.getParameters().toString(), sd.getFilterParameters().toString());
		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
		assertEquals(1, tus.size());
		assertEquals("First sentence. Second longer sentence. Followed by a third one.", tus.get(0).getSource().toString());
		// Verify that we extracted SDLXLIFF-specific metadata
		ISegments segs = tus.get(0).getTargetSegments(LocaleId.FRENCH);
		Segment seg = segs.get(0);
		assertEquals("Première phrase", seg.text.toString());
		assertEquals("Translated", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF).getValue());
	}

	private void roundtrip(String goldFileName, RawDocument rd, List<Event> events) throws IOException, SAXException {
		Path temp = Files.createTempFile("okapi~autoxliff~1", ".xlf");
		try (IFilterWriter w = filter.createFilterWriter();
			 OutputStream os = Files.newOutputStream(temp, StandardOpenOption.CREATE)) {
			w.setOptions(rd.getTargetLocale(), "UTF-8");
			w.setOutput(os);
			events.forEach(w::handleEvent);
		}
		try (Reader g = new InputStreamReader(fl.in(goldFileName).asInputStream(), StandardCharsets.UTF_8);
				 Reader o = Files.newBufferedReader(temp, StandardCharsets.UTF_8)){
			assertXMLEqual(g, o);
			Files.delete(temp);
		}
	}
}
