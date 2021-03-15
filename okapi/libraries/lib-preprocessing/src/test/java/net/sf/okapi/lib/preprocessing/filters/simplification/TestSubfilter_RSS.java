package net.sf.okapi.lib.preprocessing.filters.simplification;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterUtil;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.common.logger.EventLogger;
import net.sf.okapi.common.logger.TuDpLogger;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.common.ResourceSimplifierStep;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSubfilter_RSS {

	private IFilter filter;
	private LocaleId locEN = LocaleId.ENGLISH;
	private FileLocation root;

	@Before
	public void startUp() {
		root = FileLocation.fromClass(getClass());
		filter = FilterUtil.createFilter(
				root.in("/subfilters/okf_xmlstream@microcustom2.fprm").asUrl());
	}
	
	@Test
	public void testEvents() {
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/subfilters/import8971089963360986920.xml").asUrl(),
								"UTF-8",
								locEN)
						),
						
				new RawDocumentToFilterEventsStep(filter),
				new ResourceSimplifierStep(),
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
				new ResourceSimplifierStep(),
				new TuDpLogger()
		).execute();
	}	
}
