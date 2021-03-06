package net.sf.okapi.filters.xmlstream.integration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.filters.xmlstream.XmlStreamFilter;

@RunWith(JUnit4.class)
public class DitaExtractionComparisionTest {

	private XmlStreamFilter xmlStreamFilter;
	private String[] ditaFileList;
	private LocaleId locEN = LocaleId.fromString("en");
	private final FileLocation root = FileLocation.fromClass(getClass());

	@Before
	public void setUp() throws Exception {
		xmlStreamFilter = new XmlStreamFilter();	
		xmlStreamFilter.setParametersFromURL(XmlStreamFilter.class.getResource("dita.yml"));
		ditaFileList = XmlStreamTestUtils.getTestFiles("/", ".dita");		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testStartDocument() {
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(xmlStreamFilter,
				new InputDocument(root.in("/bookmap-readme.dita").toString(), null),
				"UTF-8", locEN, locEN));
	}
	

	@Test
	public void testOpenTwice () {
		File file = root.in("/bookmap-readme.dita").asFile();
		RawDocument rawDoc = new RawDocument(file.toURI(), "UTF-8", locEN);
		xmlStreamFilter.open(rawDoc);
		xmlStreamFilter.close();
		xmlStreamFilter.open(rawDoc);
		xmlStreamFilter.close();
	}
	
	@Test
	public void testDoubleExtractionSingle() {
		RoundTripComparison rtc = new RoundTripComparison();
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/bookmap-readme.dita").toString(), null));
		assertTrue(rtc.executeCompare(xmlStreamFilter, list, "UTF-8", locEN, locEN));
	}
	
	@Test
	public void testDoubleExtraction() {
		RoundTripComparison rtc = new RoundTripComparison();
		ArrayList<InputDocument> list = new ArrayList<>();
		for (String f : ditaFileList) {			
			list.add(new InputDocument(root.in("/" + f).toString(), null));			
		}
		assertTrue(rtc.executeCompare(xmlStreamFilter, list, "UTF-8", locEN, locEN));
	}
	
	@Test @Ignore
	public void testReconstructFile() {
		GenericSkeletonWriter writer = new GenericSkeletonWriter();
		StringBuilder tmp = new StringBuilder();

		// Open the document to process
		xmlStreamFilter.open(new RawDocument(root.in("/bookmap-readme.dita").asUri(),
				"UTF-8", new LocaleId("en")));

		// process the input document
		while (xmlStreamFilter.hasNext()) {
			Event event = xmlStreamFilter.next();
			switch (event.getEventType()) {
			case START_DOCUMENT:
				writer.processStartDocument(LocaleId.SPANISH, "utf-8", null,
						xmlStreamFilter.getEncoderManager(), (StartDocument) event.getResource());
				break;
			case TEXT_UNIT:
				ITextUnit tu = event.getTextUnit();
				tmp.append(writer.processTextUnit(tu));
				break;
			case DOCUMENT_PART:
				DocumentPart dp = (DocumentPart) event.getResource();
				tmp.append(writer.processDocumentPart(dp));
				break;
			case START_GROUP:
			case START_SUBFILTER:
				StartGroup startGroup = (StartGroup) event.getResource();
				tmp.append(writer.processStartGroup(startGroup));
				break;
			case END_GROUP:
			case END_SUBFILTER:
				Ending ending = (Ending) event.getResource();
				tmp.append(writer.processEndGroup(ending));
				break;
			default:
				break;
			}
		}
		writer.close();
	}
}
