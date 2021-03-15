package net.sf.okapi.lib.preprocessing.filters.simplification;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiBadFilterParametersException;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestParameters {

	private static final LocaleId ENUS = new LocaleId("en", "us");
	private FileLocation root;

	@Before
	public void startUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testParameters() {
		SimplificationFilter filter = new SimplificationFilter();
		
		Parameters params =	filter.getParameters();
		params.setSimplifyResources(true);
		params.setSimplifyCodes(false);
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter)
		).execute();
	}
	
	@Test(expected = OkapiBadFilterParametersException.class)
	public void testParameters_NullParameters() {
		SimplificationFilter filter = new SimplificationFilter();
		filter.setParameters(null);		
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter)
		).execute();
	}
	
	@Test(expected = OkapiBadFilterParametersException.class)
	public void testParameters_NullConfigId() {
		SimplificationFilter filter = new SimplificationFilter();
		Parameters params =	filter.getParameters();
		params.setFilterConfigId(null);		
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter)
		).execute();
	}
	
	@Test(expected = OkapiBadFilterParametersException.class)
	public void testParameters_EmptyConfigId() {
		SimplificationFilter filter = new SimplificationFilter();
		Parameters params =	filter.getParameters();
		params.setFilterConfigId("");		
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter)
		).execute();
	}
	
	@Test(expected = OkapiBadFilterParametersException.class)
	public void testParameters_NonexistentConfigId() {
		SimplificationFilter filter = new SimplificationFilter();
		Parameters params =	filter.getParameters();
		params.setFilterConfigId("okf_bogus");		
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter)
		).execute();
	}
}
