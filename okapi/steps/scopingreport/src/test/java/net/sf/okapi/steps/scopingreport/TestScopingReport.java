/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.steps.scopingreport;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XParameter;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.common.pipelinebuilder.XPipelineStep;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.plaintext.PlainTextFilter;
import net.sf.okapi.common.logger.EventLogger;
import net.sf.okapi.common.logger.TextUnitLogger;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.leveraging.LeveragingStep;
import net.sf.okapi.steps.repetitionanalysis.RepetitionAnalysisStep;
import net.sf.okapi.steps.segmentation.SegmentationStep;
import net.sf.okapi.steps.wordcount.CharacterCountStep;
import net.sf.okapi.steps.wordcount.SimpleWordCountStep;
import net.sf.okapi.steps.wordcount.WordCountStep;
import net.sf.okapi.steps.wordcount.categorized.okapi.ExactLocalContextMatchWordCountStep;
import net.sf.okapi.steps.wordcount.common.GMX;
import net.sf.okapi.steps.wordcount.common.Metrics;
import net.sf.okapi.steps.wordcount.common.MetricsAnnotation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TestScopingReport {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final LocaleId EN = new LocaleId("en", "us");
	private static final LocaleId ES = new LocaleId("es", "es");
	private static final LocaleId DE = new LocaleId("de", "de");
	private FileLocation root;

	@Before
	public void setUp () {
		root = FileLocation.fromClass(getClass());
	}

	public static void testPath(String path) {
		Logger localLogger = LoggerFactory.getLogger(TestScopingReport.class); // loggers are cached
		localLogger.debug(new File(path).getAbsolutePath());
	}
	
	@Test
	public void testDefaultTemplate() {
		ScopingReportStep srs = new ScopingReportStep();
		Parameters params = (Parameters) srs.getParameters();
		assertEquals("My Project", params.getProjectName());
		assertEquals("", params.getCustomTemplateURI());
		assertTrue(params.useDefaultTemplate());
		assertTrue(params.useTemplateFile());
		
		new XPipeline(
				"HTML report test",
				new XBatch(
						new XBatchItem(root.in("aa324.html").asUrl(), "UTF-8", EN, ES),
						new XBatchItem(root.in("form.html").asUrl(), "UTF-8", EN, DE)
						),
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				new EventLogger(),
				new WordCountStep(),
				new XPipelineStep(
						srs,
						new XParameter("outputPath", root.out("test_custom_template_report.html").toString())
						)
		).execute();
		logger.debug(root.out("/").toString());
		assertEquals("My Project", params.getProjectName());
		assertEquals("", params.getCustomTemplateURI());
	}
	
	@Test
	public void testDefaultTemplateSWCS() {
		ScopingReportStep srs = new ScopingReportStep();
		Parameters params = (Parameters) srs.getParameters();
		assertEquals("My Project", params.getProjectName());
		assertEquals("", params.getCustomTemplateURI());
		assertTrue(params.useDefaultTemplate());
		assertTrue(params.useTemplateFile());
		
		new XPipeline(
				"HTML report test",
				new XBatch(
						new XBatchItem(root.in("aa324.html").asUri(), "UTF-8", EN, ES),
						new XBatchItem(root.in("form.html").asUri(), "UTF-8", EN, ES)
						),
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				new EventLogger(),
				new SimpleWordCountStep(),
				new XPipelineStep(
						srs,
						new XParameter("outputPath", root.out("test_custom_template_report.html").toString())
						)
		).execute();
		logger.debug(root.out("/").toString());
		assertEquals("My Project", params.getProjectName());
		assertEquals("", params.getCustomTemplateURI());
	}
	
	@Test
	public void testCustomTemplateSWCS() {
		String path = root.in("/totals_report_template.html").toString();
		ScopingReportStep srs = new ScopingReportStep();
		Parameters params = (Parameters) srs.getParameters();
		params.setCustomTemplateURI(path);
		assertTrue(params.useTemplateFile());
		assertEquals("My Project", params.getProjectName());
		assertEquals(path, params.getCustomTemplateURI());
		
		new XPipeline(
				"HTML report test",
				new XBatch(
						new XBatchItem(root.in("test0.txt").asUri(), "UTF-8", EN, ES),
						new XBatchItem(root.in("aa324.html").asUri(), "UTF-8", EN, ES),
						new XBatchItem(root.in("form.html").asUri(), "UTF-8", EN, DE)
						),
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				new EventLogger(),
				new SimpleWordCountStep(),
				new XPipelineStep(
						srs,
						new XParameter("outputPath", root.out("totals_report.html").toString())
						)
		).execute();
		logger.debug(root.out("/").toString());
		assertEquals("My Project", params.getProjectName());
		assertEquals(path, params.getCustomTemplateURI());
	}
	
	@Test
	public void testCustomTemplate() {
		ScopingReportStep srs = new ScopingReportStep();
		Parameters params = (Parameters) srs.getParameters();
		assertEquals("", params.getCustomTemplateURI());
		params.setProjectName("Test scoping report");
		String path = root.in("test_scoping_report.html").toString();
		assertNotNull(path);
		params.setCustomTemplateURI(path);
		assertFalse(params.useDefaultTemplate());
		assertTrue(params.useTemplateFile());
		assertEquals("test_scoping_report.html", Util.getFilename(path, true));
		
		XPipeline pipeline = new XPipeline(
				"HTML report test",
				new XBatch(
						new XBatchItem(root.in("aa324.html").asUrl(), "UTF-8", EN, ES),
						new XBatchItem(root.in("form.html").asUrl(), "UTF-8", EN, ES)
						),
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				new EventLogger(),
				new XPipelineStep(
						srs,
						new XParameter("outputPath", root.out("test_custom_template_report.html").toString())
						)
		);
		assertEquals("Test scoping report", params.getProjectName());
		assertEquals("test_scoping_report.html", Util.getFilename(params.getCustomTemplateURI(), true));
		pipeline.execute();
		logger.debug(root.out("/").toString());
		assertEquals("Test scoping report", params.getProjectName());
		assertEquals("test_scoping_report.html", Util.getFilename(params.getCustomTemplateURI(), true));
	}
	
	@Test
	public void testCustomStringTemplateSWCS() {
		ScopingReportStep srs = new ScopingReportStep();
		Parameters params = (Parameters) srs.getParameters();
		assertEquals("", params.getCustomTemplateString());
		
		String template = "  Total: [" + ScopingReportStep.PROJECT_TOTAL_WORD_COUNT + "]"
				+ "\n  Exact Local Context: [" + ScopingReportStep.PROJECT_EXACT_LOCAL_CONTEXT + "]\n"
				+ "  100% Match: [" + ScopingReportStep.PROJECT_GMX_LEVERAGED_MATCHED_WORD_COUNT + "]\n"
				+ "  Fuzzy Match: [" + ScopingReportStep.PROJECT_GMX_FUZZY_MATCHED_WORD_COUNT + "]\n"
				+ "  Repetitions: [" + ScopingReportStep.PROJECT_GMX_REPETITION_MATCHED_WORD_COUNT + "]\n";
		
		params.setCustomTemplateString(template);
		
		// This should be enough to suppress file output, but the way XPipeline works(?)
		// we also have to pass outputPath="" as an XParameter to get it to work in the test.
		params.setOutputPath(null);
		
		assertFalse(params.useDefaultTemplate());
		assertFalse(params.useTemplateFile());
		
		XPipeline pipeline = new XPipeline(
				"String report test",
				new XBatch(
						new XBatchItem(root.in("aa324.html").asUrl(), "UTF-8", EN, ES),
						new XBatchItem(root.in("form.html").asUrl(), "UTF-8", EN, ES)
						),
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				new EventLogger(),
				new SimpleWordCountStep(),
				new XPipelineStep(
						srs,
						new XParameter("outputPath", "")
						)
		);
		
		pipeline.execute();
		
		assertEquals("  Total: 420\n  Exact Local Context: 0\n  100% Match: 0\n  Fuzzy Match: 0\n  Repetitions: 0\n",
				srs.getReportGenerator().generate());
	}
		
	@Test
	public void htmlReportTest() {
		new XPipeline(
				"HTML report test",
				new XBatch(
						new XBatchItem(root.in("aa324.html").asUrl(), "UTF-8", EN, ES),
						new XBatchItem(root.in("form.html").asUrl(), "UTF-8", EN, ES)
						),
				new RawDocumentToFilterEventsStep(new HtmlFilter()),
				new EventLogger(),
				new XPipelineStep(
						new ScopingReportStep(),
						new XParameter("outputPath", root.out("test_scoping_report.html").toString())
						)
		).execute();
		
		logger.debug(root.out("/").toString());
	}
	
	@Test
	public void htmlReportTest2() {
		StartDocument sd1 = new StartDocument("sd1");
		sd1.setName(root.in("aa324.html").toString());
		StartDocument sd2 = new StartDocument("sd2");
		sd2.setName(root.in("form.html").toString());
		
		ScopingReportStep srs = new ScopingReportStep();
		Parameters params = (Parameters) srs.getParameters();
		params.setProjectName("Test scoping report");
		params.setOutputPath(root.out("test_scoping_report2.html").toString()); 
		
		srs.setSourceLocale(EN); 
		srs.setTargetLocale(ES);
		
		srs.handleEvent(new Event(EventType.START_BATCH));
		srs.handleEvent(new Event(EventType.START_DOCUMENT, sd1));
		srs.handleEvent(new Event(EventType.START_DOCUMENT, sd2));
		
		Ending res = new Ending("end_batch");
		MetricsAnnotation ma = new MetricsAnnotation();
		res.setAnnotation(ma);
		Metrics m = ma.getMetrics();		
		
		m.setMetric(GMX.TotalWordCount, 1273);
		m.setMetric(ExactLocalContextMatchWordCountStep.METRIC, 72);
		m.setMetric(GMX.ExactMatchedWordCount, 120);
		m.setMetric(GMX.LeveragedMatchedWordCount, 132);
		m.setMetric(GMX.FuzzyMatchedWordCount, 781);
		m.setMetric(GMX.RepetitionMatchedWordCount, 112);
		
		srs.handleEvent(new Event(EventType.END_BATCH, res));
		logger.debug(root.out("test_scoping_report2.html").toString());
	}
	
	@Test
	public void testLeveraging() {

		net.sf.okapi.connectors.pensieve.Parameters params = 
			new net.sf.okapi.connectors.pensieve.Parameters();
		params.setDbDirectory(root.in("testtm").toString());
		
		new XPipeline(
				"HTML report test",
				new XBatch(
//						new XBatchItem(root.in("test2.txt").asUrl(), "UTF-8", EN, ES),
						new XBatchItem(root.in("test.txt").asUrl(), "UTF-8", EN, ES)
						),
				//new RawDocumentToFilterEventsStep(new HtmlFilter()),
				new RawDocumentToFilterEventsStep(new PlainTextFilter()),
				new EventLogger(),
				new XPipelineStep(
						new SegmentationStep(),
						//new Parameter("sourceSrxPath", pathBase + "test.srx")
						new XParameter("sourceSrxPath", root.in("default.srx").toString()),
						//new Parameter("sourceSrxPath", pathBase + "myRules.srx")
						new XParameter("trimSrcLeadingWS", net.sf.okapi.steps.segmentation.Parameters.TRIM_YES),
						new XParameter("trimSrcTrailingWS", net.sf.okapi.steps.segmentation.Parameters.TRIM_YES)
				),
				new XPipelineStep(new LeveragingStep(), 
						new XParameter("resourceClassName", net.sf.okapi.connectors.pensieve.PensieveTMConnector.class.getName()),
						new XParameter("resourceParameters", params.toString(), true),
						new XParameter("threshold", 60),
						new XParameter("fillTarget", true)
				),
				new WordCountStep(),
				new TextUnitLogger(),
				new XPipelineStep(
						new ScopingReportStep(),
						new XParameter("projectName", "Test Scoping Report"),
						new XParameter("outputPath", root.out("test_scoping_report3.html").toString())
						)
		).execute();
		
//	No golden file comparison is possible as the output report contains a localized date 
//		FileCompare fc = new FileCompare();
//		String outputFilePath = pathBase + "out/test_scoping_report3.html";
//		String goldFilePath = pathBase + "gold/test_scoping_report3.html";
//		assertTrue(fc.filesExactlyTheSame(outputFilePath, goldFilePath));
		
		logger.debug(root.out("/").toString());
	}

	@Test
	public void test_a_word_is_counted_only_once() {
		ScopingReportStep srs;
		WordCountStep wcs;
		StartDocument sd;
		Ending ed;
		Event sdEvent, edEvent;
		Event sbEvent, ebEvent;
		ITextUnit tu1, tu2, tu3, tu4;
		Event tuEvent1, tuEvent2, tuEvent3, tuEvent4;
		
		net.sf.okapi.connectors.pensieve.Parameters rparams = 
			new net.sf.okapi.connectors.pensieve.Parameters();
		rparams.setDbDirectory(root.in("testtm").toString());
		
		String outputFilePath = root.out("test_scoping_report4.txt").toString();
		String goldFilePath = root.in("gold/test_scoping_report4.txt").toString();
		
		sbEvent = new Event(EventType.START_BATCH);
		ebEvent = new Event(EventType.END_BATCH);
		Event siEvent = new Event(EventType.START_BATCH_ITEM);
		Event eiEvent = new Event(EventType.END_BATCH_ITEM);
		
		sd = new StartDocument("sd");		
		sd.setLocale(LocaleId.ENGLISH);
		sdEvent = new Event(EventType.START_DOCUMENT, sd);
		
		ed = new Ending("ed");
		edEvent = new Event(EventType.END_DOCUMENT, ed);
		
		tu1 = new TextUnit("tu1");
		tu1.setSource(new TextContainer("Elephants cannot fly."));
		tuEvent1 = new Event(EventType.TEXT_UNIT, tu1);
		
		tu2 = new TextUnit("tu2");
		tu2.setSource(new TextContainer("Elephants can't fly."));
		tuEvent2 = new Event(EventType.TEXT_UNIT, tu2);
		
		tu3 = new TextUnit("tu3");
		tu3.setSource(new TextContainer("Elephants can fly."));
		tuEvent3 = new Event(EventType.TEXT_UNIT, tu3);
		
		tu4 = new TextUnit("tu4");
		tu4.setSource(new TextContainer("Airplanes can fly."));
		tuEvent4 = new Event(EventType.TEXT_UNIT, tu4);
		
		LeveragingStep ls = new LeveragingStep();
		ls.setSourceLocale(LocaleId.ENGLISH);
		ls.setTargetLocale(LocaleId.FRENCH);
		net.sf.okapi.steps.leveraging.Parameters params = ls.getParameters();
		params.setResourceParameters(rparams.toString());
		params.setResourceClassName(net.sf.okapi.connectors.pensieve.PensieveTMConnector.class.getName());
		params.setThreshold(60);
		params.setFillTarget(true);
		
		wcs = new WordCountStep();				
		srs = new ScopingReportStep();
		srs.setSourceLocale(LocaleId.ENGLISH);
		srs.setTargetLocale(LocaleId.FRENCH);
		Parameters params2 = (Parameters) srs.getParameters();
		params2.setOutputPath(outputFilePath);
		params2.setCustomTemplateURI(root.in("golden_file_template.txt").toString());
		sd.setName(params2.getCustomTemplateURI());
				
		wcs.handleEvent(sbEvent);
		wcs.handleEvent(siEvent);
		wcs.handleEvent(sdEvent);
		wcs.handleEvent(tuEvent1);
		wcs.handleEvent(tuEvent2);
		wcs.handleEvent(tuEvent3);
		wcs.handleEvent(tuEvent4);
		wcs.handleEvent(edEvent);
		wcs.handleEvent(eiEvent);
		wcs.handleEvent(ebEvent);
		
		ls.handleEvent(sbEvent);
		ls.handleEvent(siEvent);
		ls.handleEvent(sdEvent);
		ls.handleEvent(tuEvent1);
		ls.handleEvent(tuEvent2);
		ls.handleEvent(tuEvent3);
		ls.handleEvent(tuEvent4);
		ls.handleEvent(edEvent);
		ls.handleEvent(eiEvent);
		ls.handleEvent(ebEvent);
		
		srs.handleEvent(sbEvent);
		srs.handleEvent(siEvent);
		srs.handleEvent(sdEvent);
		srs.handleEvent(tuEvent1);
		srs.handleEvent(tuEvent2);
		srs.handleEvent(tuEvent3);
		srs.handleEvent(tuEvent4);
		srs.handleEvent(edEvent);
		srs.handleEvent(eiEvent);
		srs.handleEvent(ebEvent);
				
		logger.debug(TextUnitLogger.getTuInfo(tu1, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu2, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu3, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu4, LocaleId.ENGLISH));
		
		testPath(outputFilePath);
		
		FileCompare fc = new FileCompare();		
		assertTrue(fc.compareFilesPerLines(outputFilePath, goldFilePath, "ISO-8859-1"));
		
	}	
	
	@Test
	public void test_a_word_is_counted_only_once2() {
		ScopingReportStep srs;
		WordCountStep wcs;
		StartDocument sd;
		Ending ed;
		Event sdEvent, edEvent;
		Event sbEvent, ebEvent;
		ITextUnit tu1, tu2, tu3, tu4;
		Event tuEvent1, tuEvent2, tuEvent3, tuEvent4;
		
		net.sf.okapi.connectors.pensieve.Parameters rparams = 
			new net.sf.okapi.connectors.pensieve.Parameters();
		rparams.setDbDirectory(root.in("testtm").toString());
		
		String outputFilePath = root.out("test_scoping_report5.txt").toString();
		String goldFilePath = root.in("gold/test_scoping_report5.txt").toString();
		
		sbEvent = new Event(EventType.START_BATCH);
		ebEvent = new Event(EventType.END_BATCH);
		Event siEvent = new Event(EventType.START_BATCH_ITEM);
		Event eiEvent = new Event(EventType.END_BATCH_ITEM);
		
		sd = new StartDocument("sd");		
		sd.setLocale(LocaleId.ENGLISH);
		sdEvent = new Event(EventType.START_DOCUMENT, sd);
		
		ed = new Ending("ed");
		edEvent = new Event(EventType.END_DOCUMENT, ed);
		
		tu1 = new TextUnit("tu1");
		tu1.setSource(new TextContainer("Elephants cannot fly."));
		tuEvent1 = new Event(EventType.TEXT_UNIT, tu1);
		
		tu2 = new TextUnit("tu2");
		tu2.setSource(new TextContainer("Elephants can't fly."));
		tuEvent2 = new Event(EventType.TEXT_UNIT, tu2);
		
		tu3 = new TextUnit("tu3");
		tu3.setSource(new TextContainer("Elephants can fly."));
		tuEvent3 = new Event(EventType.TEXT_UNIT, tu3);
		
		tu4 = new TextUnit("tu4");
		tu4.setSource(new TextContainer("Airplanes can fly."));
		tuEvent4 = new Event(EventType.TEXT_UNIT, tu4);
		
		LeveragingStep ls = new LeveragingStep();
		ls.setSourceLocale(LocaleId.ENGLISH);
		ls.setTargetLocale(LocaleId.FRENCH);
		net.sf.okapi.steps.leveraging.Parameters params = ls.getParameters();
		params.setResourceParameters(rparams.toString());
		params.setResourceClassName(net.sf.okapi.connectors.pensieve.PensieveTMConnector.class.getName());
		params.setThreshold(60);
		params.setFillTarget(true);
		
		wcs = new WordCountStep();				
		srs = new ScopingReportStep();
		srs.setSourceLocale(LocaleId.ENGLISH);
		srs.setTargetLocale(LocaleId.FRENCH);
		wcs.setTargetLocale(LocaleId.FRENCH);
		Parameters params2 = (Parameters) srs.getParameters();
		params2.setOutputPath(outputFilePath);
		params2.setCustomTemplateURI(root.in("golden_file_template.txt").toString());
		params2.setCountAsNonTranslatable_ExactMatch(true);
		params2.setCountAsNonTranslatable_GMXFuzzyMatch(true);
		sd.setName(params2.getCustomTemplateURI());
				
		wcs.handleEvent(sbEvent);
		wcs.handleEvent(siEvent);
		wcs.handleEvent(sdEvent);
		wcs.handleEvent(tuEvent1);
		wcs.handleEvent(tuEvent2);
		wcs.handleEvent(tuEvent3);
		wcs.handleEvent(tuEvent4);
		wcs.handleEvent(edEvent);
		wcs.handleEvent(eiEvent);
		wcs.handleEvent(ebEvent);
		
		ls.handleEvent(sbEvent);
		ls.handleEvent(siEvent);
		ls.handleEvent(sdEvent);
		ls.handleEvent(tuEvent1);
		ls.handleEvent(tuEvent2);
		ls.handleEvent(tuEvent3);
		ls.handleEvent(tuEvent4);
		ls.handleEvent(edEvent);
		ls.handleEvent(eiEvent);
		ls.handleEvent(ebEvent);
		
		srs.handleEvent(sbEvent);
		srs.handleEvent(siEvent);
		srs.handleEvent(sdEvent);
		srs.handleEvent(tuEvent1);
		srs.handleEvent(tuEvent2);
		srs.handleEvent(tuEvent3);
		srs.handleEvent(tuEvent4);
		srs.handleEvent(edEvent);
		srs.handleEvent(eiEvent);
		srs.handleEvent(ebEvent);
				
		logger.debug(TextUnitLogger.getTuInfo(tu1, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu2, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu3, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu4, LocaleId.ENGLISH));
		
		testPath(outputFilePath);
		
		FileCompare fc = new FileCompare();		
		assertTrue(fc.compareFilesPerLines(outputFilePath, goldFilePath, "ISO-8859-1"));		
	}
	
	@Test
	public void test_repetitions() {
		ScopingReportStep srs;
		WordCountStep wcs;
		CharacterCountStep ccs;
		StartDocument sd;
		RepetitionAnalysisStep ras;
		Ending ed;
		Event sdEvent, edEvent;
		Event sbEvent, ebEvent;
		ITextUnit tu1, tu2, tu3, tu4;
		Event tuEvent1, tuEvent2, tuEvent3, tuEvent4;
		
		String outputFilePath = root.out("test_scoping_report6.txt").toString();
		String goldFilePath = root.in("gold/test_scoping_report6.txt").toString();
		
		sbEvent = new Event(EventType.START_BATCH);
		ebEvent = new Event(EventType.END_BATCH);
		Event siEvent = new Event(EventType.START_BATCH_ITEM);
		Event eiEvent = new Event(EventType.END_BATCH_ITEM);
		
		sd = new StartDocument("sd");		
		sd.setLocale(LocaleId.ENGLISH);
		sdEvent = new Event(EventType.START_DOCUMENT, sd);
		
		ed = new Ending("ed");
		edEvent = new Event(EventType.END_DOCUMENT, ed);
		
		tu1 = new TextUnit("tu1");
		tu1.setSource(new TextContainer("Elephants cannot fly."));
		tuEvent1 = new Event(EventType.TEXT_UNIT, tu1);
		
		tu2 = new TextUnit("tu2");
		//tu2.setSource(new TextContainer("Elephants can't fly."));
		tu2.setSource(new TextContainer("Elephants cannot fly."));
		tuEvent2 = new Event(EventType.TEXT_UNIT, tu2);
		
		tu3 = new TextUnit("tu3");
		tu3.setSource(new TextContainer("Elephants can fly."));
		tuEvent3 = new Event(EventType.TEXT_UNIT, tu3);
		
		tu4 = new TextUnit("tu4");
		tu4.setSource(new TextContainer("Airplanes can fly."));
		tuEvent4 = new Event(EventType.TEXT_UNIT, tu4);
		
		//LeveragingStep ls = new LeveragingStep();
//		ls.setSourceLocale(LocaleId.ENGLISH);
//		ls.setTargetLocale(LocaleId.FRENCH);
//		net.sf.okapi.steps.leveraging.Parameters params = (net.sf.okapi.steps.leveraging.Parameters) ls.getParameters();
//		params.setResourceParameters(rparams.toString());
//		params.setResourceClassName(net.sf.okapi.connectors.pensieve.PensieveTMConnector.class.getName());
//		params.setThreshold(60);
//		params.setFillTarget(true);
		
		ras = new RepetitionAnalysisStep();
		net.sf.okapi.steps.repetitionanalysis.Parameters params = ras.getParameters();
		params.setFuzzyThreshold(10);
		
		ras.setSourceLocale(LocaleId.ENGLISH);
		ras.setTargetLocale(LocaleId.FRENCH);
		wcs = new WordCountStep();
		ccs = new CharacterCountStep();
		srs = new ScopingReportStep();
		srs.setSourceLocale(LocaleId.ENGLISH);
		srs.setTargetLocale(LocaleId.FRENCH);
		Parameters params2 = (Parameters) srs.getParameters();
		params2.setOutputPath(outputFilePath);
		params2.setCustomTemplateURI(root.in("golden_file_template2.txt").toString());
		sd.setName(params2.getCustomTemplateURI());
				
		ras.handleEvent(sbEvent);
		ras.handleEvent(siEvent);
		ras.handleEvent(sdEvent);
		ras.handleEvent(tuEvent1);
		ras.handleEvent(tuEvent2);
		ras.handleEvent(tuEvent3);
		ras.handleEvent(tuEvent4);
		ras.handleEvent(edEvent);
		ras.handleEvent(eiEvent);
		ras.handleEvent(ebEvent);
		
		wcs.handleEvent(sbEvent);
		wcs.handleEvent(siEvent);
		wcs.handleEvent(sdEvent);
		wcs.handleEvent(tuEvent1);
		wcs.handleEvent(tuEvent2);
		wcs.handleEvent(tuEvent3);
		wcs.handleEvent(tuEvent4);
		wcs.handleEvent(edEvent);
		wcs.handleEvent(eiEvent);
		wcs.handleEvent(ebEvent);
		
		ccs.handleEvent(sbEvent);
		ccs.handleEvent(siEvent);
		ccs.handleEvent(sdEvent);
		ccs.handleEvent(tuEvent1);
		ccs.handleEvent(tuEvent2);
		ccs.handleEvent(tuEvent3);
		ccs.handleEvent(tuEvent4);
		ccs.handleEvent(edEvent);
		ccs.handleEvent(eiEvent);
		ccs.handleEvent(ebEvent);
		
//		ls.handleEvent(sbEvent);
//		ls.handleEvent(siEvent);
//		ls.handleEvent(sdEvent);
//		ls.handleEvent(tuEvent1);
//		ls.handleEvent(tuEvent2);
//		ls.handleEvent(tuEvent3);
//		ls.handleEvent(tuEvent4);
//		ls.handleEvent(edEvent);
//		ls.handleEvent(eiEvent);
//		ls.handleEvent(ebEvent);
		
		srs.handleEvent(sbEvent);
		srs.handleEvent(siEvent);
		srs.handleEvent(sdEvent);
		srs.handleEvent(tuEvent1);
		srs.handleEvent(tuEvent2);
		srs.handleEvent(tuEvent3);
		srs.handleEvent(tuEvent4);
		srs.handleEvent(edEvent);
		srs.handleEvent(eiEvent);
		srs.handleEvent(ebEvent);
				
		logger.debug(TextUnitLogger.getTuInfo(tu1, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu2, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu3, LocaleId.ENGLISH));
		logger.debug(TextUnitLogger.getTuInfo(tu4, LocaleId.ENGLISH));
		
		testPath(outputFilePath);
		
		FileCompare fc = new FileCompare();		
		assertTrue(fc.compareFilesPerLines(outputFilePath, goldFilePath, "ISO-8859-1"));		
	}
}

