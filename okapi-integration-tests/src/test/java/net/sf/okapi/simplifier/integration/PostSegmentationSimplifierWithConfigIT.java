package net.sf.okapi.simplifier.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.filters.xml.XMLFilter;
import net.sf.okapi.steps.common.codesimplifier.PostSegmentationCodeSimplifierStep;
import net.sf.okapi.steps.segmentation.Parameters;
import net.sf.okapi.steps.segmentation.SegmentationStep;

@RunWith(JUnit4.class)
public class PostSegmentationSimplifierWithConfigIT {
	private static final LocaleId EN = new LocaleId("en", "us");
	private static final LocaleId ESES = new LocaleId("es", "es");
	private String pathBase;
	private SegmentationStep segmentationStep;

	@Rule
	public ErrorCollector errCol = new ErrorCollector();
	
	@Before
	public void setUp() throws Exception {
		pathBase = Util.getDirectoryName(PostSegmentationSimplifierWithConfigIT.class.getResource("/net/sf/okapi/common/codesimplifier/test1.xlf").getPath()) + "/";
		segmentationStep = new SegmentationStep();
		segmentationStep.setSourceLocale(EN);
		List<LocaleId> tl = new LinkedList<>();
		tl.add(ESES);
		segmentationStep.setTargetLocales(tl);
		Parameters params = (Parameters)segmentationStep.getParameters();
		params.setSegmentSource(true);
		params.setSegmentTarget(true);
		params.setSourceSrxPath(PostSegmentationSimplifierWithConfigIT.class.getClassLoader().getResource("default.srx").getPath());
		params.setTargetSrxPath(PostSegmentationSimplifierWithConfigIT.class.getClassLoader().getResource("default.srx").getPath());
		params.setCopySource(false);
		segmentationStep.handleEvent(Event.START_BATCH_ITEM_EVENT);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testDoubleExtraction() {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();

		list.add(new InputDocument(pathBase + "test1.html", "html_with_simplifier_rules.yml"));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(new HtmlFilter(), list, "UTF-8", EN, ESES, "out",
				segmentationStep,
				new PostSegmentationCodeSimplifierStep()));
	}

	@SuppressWarnings("resource")
	@Test
	public void testDoubleExtraction2() {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();

		list.add(new InputDocument(pathBase + "aa324.html", "html_with_simplifier_rules.yml"));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(new HtmlFilter(), list, "UTF-8", EN, ESES, "out",
				segmentationStep,
				new PostSegmentationCodeSimplifierStep()));
	}

	@SuppressWarnings("resource")
	@Test
	public void testDoubleExtractionReferences() {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();

		list.add(new InputDocument(pathBase + "references_as_codes.html", "html_with_simplifier_rules.yml"));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(new HtmlFilter(), list, "UTF-8", EN, ESES, "out",
				segmentationStep,
				new PostSegmentationCodeSimplifierStep()));
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testDoubleExtractionMergedCodes() {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();

		list.add(new InputDocument(pathBase + "merged_codes.html", "html_with_simplifier_rules.yml"));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(new HtmlFilter(), list, "UTF-8", EN, ESES, "out",
				segmentationStep,
				new PostSegmentationCodeSimplifierStep()));
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testDoubleExtraction3() {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();

		list.add(new InputDocument(pathBase + "form.html", "html_with_simplifier_rules.yml"));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(new HtmlFilter(), list, "UTF-8", EN, ESES, "out",
				segmentationStep,
				new PostSegmentationCodeSimplifierStep()));
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testDoubleExtractionDita() {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();

		list.add(new InputDocument(pathBase + "dita.xml", "okf_xml@with-simplifier-rules.fprm"));
		
		RoundTripComparison rtc = new RoundTripComparison();
		
		assertTrue(rtc.executeCompare(new XMLFilter(), list, "UTF-8", EN, ESES, "out",
				segmentationStep,
				new PostSegmentationCodeSimplifierStep()));
	}

	@SuppressWarnings("resource")
	public void testDoubleExtraction4() {
		ArrayList<InputDocument> list = new ArrayList<InputDocument>();

		list.add(new InputDocument(pathBase + "BinUnitTest01.xlf", "okf_xliff@with-simplifier-rules.fprm"));
		list.add(new InputDocument(pathBase + "JMP-11-Test01.xlf", "okf_xliff@with-simplifier-rules.fprm"));
		list.add(new InputDocument(pathBase + "Manual-12-AltTrans.xlf", "okf_xliff@with-simplifier-rules.fprm"));
		list.add(new InputDocument(pathBase + "test1.xlf", "okf_xliff@with-simplifier-rules.fprm"));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(new XLIFFFilter(), list, "UTF-8", EN, ESES, "out",
				segmentationStep,
				new PostSegmentationCodeSimplifierStep()));
	}
}
