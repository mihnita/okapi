package net.sf.okapi.common.pipeline.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.steps.common.FilterEventsToRawDocumentStep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FilterEventsToRawDocumentStepTestIT
{

	private FilterEventsToRawDocumentStep eventToDoc;
	private String htmlSnippet;
	private HtmlFilter htmlFilter;
	private final LocaleId locEN = LocaleId.fromString("EN");

    @Before
	public void setUp() throws Exception {
		htmlFilter = new HtmlFilter();
		htmlSnippet = "<p>This is a <i>test</i> snippet</p>";		
	}

	@After
	public void tearDown() throws Exception {
		htmlFilter.close();
	}

	@Test
	public void htmlEventsToRawDocumentWithUserURI() throws IOException {
		Event event = null;
		eventToDoc = new FilterEventsToRawDocumentStep();
		
		try (RawDocument rawDoc = new RawDocument(htmlSnippet, locEN)) {
			File tmpFile = File.createTempFile("~okapi-1_", ".tmp");
			eventToDoc.setOutputURI(tmpFile.toURI());
			eventToDoc.setOutputEncoding("UTF-8");
		
			htmlFilter.open(rawDoc);
			while (htmlFilter.hasNext()) {
				event = eventToDoc.handleEvent(htmlFilter.next());
			}
			htmlFilter.close();

			assertNotNull(event);
			// last event should be RawDocument
			assertTrue(event.getEventType() == EventType.RAW_DOCUMENT);
			// Get the EventsToRawDocumentStep output and compare it to our input
			assertEquals(htmlSnippet, convertRawDocumentToString((RawDocument)event.getResource()));
			eventToDoc.destroy();
		}
	}

	@Test
	public void htmlEventsToRawDocument() throws IOException {
		Event event = null;		
		eventToDoc = new FilterEventsToRawDocumentStep();
		try (RawDocument rawDoc = new RawDocument(htmlSnippet, locEN)) {
			eventToDoc.setOutputEncoding("UTF-8");

			htmlFilter.open(rawDoc);
			while ( htmlFilter.hasNext() ) {
				event = eventToDoc.handleEvent(htmlFilter.next());
			}
			htmlFilter.close();

			assertNotNull(event);
			// last event should be RawDocument
			assertTrue(event.getEventType() == EventType.RAW_DOCUMENT);
			// Get the EventsToRawDocumentStep output and compare it to our input
			assertEquals(htmlSnippet, convertRawDocumentToString((RawDocument)event.getResource()));
			eventToDoc.destroy();
		}
	}

	private String convertRawDocumentToString(RawDocument d) throws IOException {		
		int c;
		StringWriter sw = new StringWriter();
		try (Reader r = d.getReader()) { 
			while ( true ) {
				c = r.read();			
				if (c == -1) break;
				sw.append((char) c);
			}
		}
		return sw.toString();
	}
}
