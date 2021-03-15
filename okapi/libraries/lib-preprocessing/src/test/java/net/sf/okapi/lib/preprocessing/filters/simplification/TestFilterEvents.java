package net.sf.okapi.lib.preprocessing.filters.simplification;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.common.logger.EventListBuilderStep;
import net.sf.okapi.common.logger.TuDpLogger;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TestFilterEvents {

	private static final LocaleId ENUS = new LocaleId("en", "us");
	private FileLocation root;

	@Before
	public void startUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void listInternalFilterEvents() {
		IFilter filter = new HtmlFilter();
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter),
				new TuDpLogger()
		).execute();
	}
	
	@Test
	public void listTransformedEvents() {
		SimplificationFilter filter = new SimplificationFilter();
		Parameters params = filter.getParameters();
		params.setFilterConfigId("okf_html");
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter),
				new TuDpLogger()
		).execute();
	}
	
	@Test
	public void testInternalFilterEvents() {
		IFilter filter = new HtmlFilter();
		EventListBuilderStep elbs1 = new EventListBuilderStep();		
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter),
				elbs1
		).execute();
		
		List<Event> list = elbs1.getList();
		DocumentPart dp;
		ITextUnit tu;
		
		assertEquals(109, list.size());
		
		dp = list.get(1).getDocumentPart();
		assertEquals("dp2", dp.getId());
		assertEquals("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=[#$$self$@%encoding]\">", dp.getSkeleton().toString());
		
		dp = list.get(91).getDocumentPart();
		assertEquals("dp53", dp.getId());
		assertEquals("<option value=\"BB\">", dp.getSkeleton().toString());
		
		dp = list.get(93).getDocumentPart();
		assertEquals("dp54", dp.getId());
		assertEquals("<option value=\"CR\">", dp.getSkeleton().toString());
		
		dp = list.get(95).getDocumentPart();
		assertEquals("dp55", dp.getId());
		assertEquals("<option value=\"AFL\">", dp.getSkeleton().toString());
		
		dp = list.get(97).getDocumentPart();
		assertEquals("dp56", dp.getId());
		assertEquals("<option value=\"SOC\">", dp.getSkeleton().toString());
		
		tu = list.get(84).getTextUnit();
		assertEquals("tu33", tu.getId());
		assertEquals("<input type=\"radio\" name=\"FavouriteFare\" [#$tu34] checked=\"checked\" /> Spam <input " +
				"type=\"radio\" name=\"FavouriteFare\" [#$tu35] /> Rhubarb <input type=\"radio\" name=\"FavouriteFare\" " +
				"[#$tu36] /> Honey <input type=\"radio\" name=\"FavouriteFare\" [#$tu37] /> Rum", tu.getSource().toString());
		
		tu = list.get(105).getTextUnit();
		assertEquals("tu45", tu.getId());
		assertEquals("<input type=\"submit\" [#$tu46] name=\"button1\"/>", tu.getSource().toString());
	}
	
	@Test
	public void testTransformedEvents() {
		SimplificationFilter filter = new SimplificationFilter();
		Parameters params = filter.getParameters();
		params.setFilterConfigId("okf_html");
		EventListBuilderStep elbs1 = new EventListBuilderStep();
		
		new XPipeline(
				null,
				new XBatch(
						new XBatchItem(
								root.in("/form.html").asUri(),
								"UTF-8",
								ENUS)
						),
						
				new RawDocumentToFilterEventsStep(filter),
				elbs1
		).execute();
		
		List<Event> list = elbs1.getList();
		DocumentPart dp;
		ITextUnit tu;
		
		assertEquals(121, list.size());
		
		dp = list.get(1).getDocumentPart();
		assertEquals("dp2", dp.getId());
		assertEquals("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">", dp.getSkeleton().toString());
		
		dp = list.get(103).getDocumentPart();
		assertEquals("dp53", dp.getId());
		assertEquals("<option value=\"BB\">", dp.getSkeleton().toString());
		
		dp = list.get(105).getDocumentPart();
		assertEquals("dp54", dp.getId());
		assertEquals("<option value=\"CR\">", dp.getSkeleton().toString());
		
		dp = list.get(107).getDocumentPart();
		assertEquals("dp55", dp.getId());
		assertEquals("<option value=\"AFL\">", dp.getSkeleton().toString());
		
		dp = list.get(109).getDocumentPart();
		assertEquals("dp56", dp.getId());
		assertEquals("<option value=\"SOC\">", dp.getSkeleton().toString());
		
		tu = list.get(94).getTextUnit();
		assertEquals("tu33", tu.getId());
		assertEquals("<input type=\"radio\" name=\"FavouriteFare\" " +
				"value=\"spam\" checked=\"checked\" /> Spam <input " +
				"type=\"radio\" name=\"FavouriteFare\" value=\"rhubarb\" " +
				"/> Rhubarb <input type=\"radio\" name=\"FavouriteFare\" " +
				"value=\"honey\" /> Honey <input type=\"radio\" name=\"" +
				"FavouriteFare\" value=\"rum\" /> Rum", tu.getSource().toString());
		
		tu = list.get(117).getTextUnit();
		assertEquals("tu45", tu.getId());
		assertEquals("<input type=\"submit\" value=\"Submit Form\" name=\"button1\"/>", tu.getSource().toString());
	}
}
