package net.sf.okapi.steps.gcaligner;

import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipeline.Pipeline;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.tmx.TmxFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.sentencealigner.Parameters;
import net.sf.okapi.steps.sentencealigner.SentenceAlignerStep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ParagraphToSentenceAlignStepTest {
	private Pipeline pipeline;
	private SentenceAlignerStep aligner;
	private LocaleId sourceLocale = new LocaleId("EN", "US");
	private LocaleId targetLocale = new LocaleId("PT", "BR");
	private FileLocation root;

	@Before
	public void setUp() throws Exception {
		root = FileLocation.fromClass(this.getClass());

		// create pipeline
		pipeline = new Pipeline();

		// add filter step
		TmxFilter filter = new TmxFilter();
		
		pipeline.addStep(new RawDocumentToFilterEventsStep(filter));

		// add aligner step
		aligner = new SentenceAlignerStep();

		Parameters p = new Parameters();
		p.setGenerateTMX(true);
		p.setSegmentTarget(true);
		p.setSegmentSource(true);
		p.setUseCustomTargetRules(true);
		p.setCustomTargetRulesPath(this.getClass().getResource("default.srx").toURI().getPath());
		aligner.setParameters(p);
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations("net.sf.okapi.filters.plaintext.PlainTextFilter");
		pipeline.addStep(aligner);

	}

	@After
	public void tearDown() throws Exception {
		pipeline.destroy();
	}

	@Test
	public void smallTest() {
		splitParagraphAlignment("/smallParagraph.tmx");
	}
	
/*
	@Test
	public void mediumTest() throws URISyntaxException {
		splitParagraphAlignment("/fullParagraph.tmx");
	}
*/
	public void splitParagraphAlignment(String initialTmx) {
		aligner.setSourceLocale(sourceLocale);
		aligner.setTargetLocale(targetLocale);

		Parameters p = aligner.getParameters();
		p.setTmxOutputPath(root.out(initialTmx).toString());
		p.setGenerateTMX(true);
		aligner.setParameters(p);
		
		pipeline.startBatch();

		pipeline.process(new RawDocument(root.in(initialTmx).asInputStream(),
				"UTF-8", sourceLocale, targetLocale));

		pipeline.endBatch();

		// Test we observed the correct events
		// (per line to avoid line-break differences on each platform)
		FileCompare fc = new FileCompare();
		assert(fc.compareFilesPerLines(
				root.out(initialTmx).toString(),
				root.in(initialTmx + ".gold").toString(),
				"UTF-8"));
	}
}
