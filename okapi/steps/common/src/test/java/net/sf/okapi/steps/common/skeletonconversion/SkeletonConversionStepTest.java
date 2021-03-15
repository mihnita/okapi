package net.sf.okapi.steps.common.skeletonconversion;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.common.logger.EventListBuilderStep;
import net.sf.okapi.common.logger.EventLogger;
import net.sf.okapi.common.logger.TuDpLogger;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SkeletonConversionStepTest {

private static final LocaleId ENUS = new LocaleId("en", "us");
	private final FileLocation pathBase = FileLocation.fromClass(this.getClass());
	
	@Test
	public void testDoubleExtraction () {
		ArrayList<InputDocument> list = new ArrayList<>();
		
		list.add(new InputDocument(pathBase.in("aa324.html").toString(), null));
		list.add(new InputDocument(pathBase.in("form.html").toString(), null));
		list.add(new InputDocument(pathBase.in("W3CHTMHLTest1.html").toString(), null));
		list.add(new InputDocument(pathBase.in("msg00058.html").toString(), null));
		
		RoundTripComparison rtc = new RoundTripComparison();
		
		SkeletonConversionStep sks = new SkeletonConversionStep();
		
		assertTrue(rtc.executeCompare(new HtmlFilter(), list, "UTF-8", ENUS, ENUS, "skeleton", sks));
	}
	
	@Test
	public void testEvents() {
		EventListBuilderStep elbs1 = new EventListBuilderStep();
		EventListBuilderStep elbs2 = new EventListBuilderStep();
		
		new XPipeline(
				"Test pipeline for SkeletonConversionStepTest",
				new XBatch(
						new XBatchItem(
								pathBase.in("form.html").asUrl(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				elbs1,
				new SkeletonConversionStep(),
				new TuDpLogger(),
				elbs2
		).execute();		
	}
	
	@Test
	public void testEvents2() {
		EventListBuilderStep elbs1 = new EventListBuilderStep();
		EventListBuilderStep elbs2 = new EventListBuilderStep();
		
		new XPipeline(
				"Test pipeline for SkeletonConversionStepTest",
				new XBatch(
						new XBatchItem(
								pathBase.in("msg00058.html").asUrl(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				elbs1,
				new SkeletonConversionStep(),
				new EventLogger(),
				new TuDpLogger(),
				elbs2
		).execute();
	}

}
