package net.sf.okapi.lib.preprocessing.filters.simplification;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterUtil;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.common.logger.EventLogger;
import net.sf.okapi.common.logger.TuDpLogger;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TestSubfilter {

	private IFilter filter;
	private LocaleId locEN = LocaleId.ENGLISH;
	private FileLocation root;

	@Before
	public void startUp() {
		root = FileLocation.fromClass(getClass());
		filter = FilterUtil.createFilter("okf_simplification@xmlcustom",
				root.in("/subfilters/okf_xmlstream@microcustom2.fprm").asUrl());
	}
	
	@Test
	public void testEvents() {
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/subfilters/import8971089963360986920.xml").asUri(),
								"UTF-8",
								locEN)
						),
						
				new RawDocumentToFilterEventsStep(filter),
				new EventLogger()
		).execute();
	}
	
	@Test
	public void testTuDpEvents() {
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/subfilters/import8971089963360986920.xml").asUri(),
								"UTF-8",
								locEN)
						),
						
				new RawDocumentToFilterEventsStep(filter),
				new TuDpLogger()
		).execute();
	}
	
	@Test
	public void testDoubleExtraction() {
		ArrayList<InputDocument> list = new ArrayList<>();
		
		list.add(new InputDocument(root.in("/subfilters/import8971089963360986920.xml").toString(), null));
		
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN, "out"));
	}
	
}
