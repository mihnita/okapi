package net.sf.okapi.common.pipeline.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.AltTranslationsAnnotation;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.connectors.pensieve.PensieveTMConnector;
import net.sf.okapi.filters.tmx.TmxFilter;
import net.sf.okapi.lib.translation.QueryManager;
import net.sf.okapi.tm.pensieve.tmx.OkapiTmxImporter;
import net.sf.okapi.tm.pensieve.writer.ITmWriter;
import net.sf.okapi.tm.pensieve.writer.TmWriterFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class QueryManagerTestIT
{
	private QueryManager qm;
	private PensieveTMConnector pensieveConnector;
	private final LocaleId locENUS = LocaleId.fromString("EN-US");
	private final LocaleId locFRFR = LocaleId.fromString("FR-FR");

	@Before
	public void setUp() throws Exception {
		URL url = QueryManagerTestIT.class.getResource("/mytm.tmx");
		String rootDir = Util.getDirectoryName(url.toURI().getPath());
		
		// create local pensieve TM		
		try (TmxFilter tmxFilter = new TmxFilter()) {
			ITmWriter tmWriter = TmWriterFactory.createFileBasedTmWriter(rootDir, true);
			OkapiTmxImporter tmxHandler = new OkapiTmxImporter(locENUS, tmxFilter);				
			tmxHandler.importTmx(url.toURI(), locFRFR, tmWriter);
			tmWriter.close();
		}

		// load up connectors in QueryManager
		qm = new QueryManager();
		
		qm.setRootDirectory(rootDir);
		qm.setLanguages(locENUS, locFRFR);
		
		pensieveConnector = new PensieveTMConnector();
		net.sf.okapi.connectors.pensieve.Parameters p = new net.sf.okapi.connectors.pensieve.Parameters();
		p.setDbDirectory(Util.ROOT_DIRECTORY_VAR);		
		qm.addAndInitializeResource(
			pensieveConnector, 
			"Pensieve",
			p);
	}
	
	@After
	public void tearDown () {
		if ( qm != null ) {
			qm.close();
			qm = null;
		}
		if (pensieveConnector != null) {
			pensieveConnector.close();
			pensieveConnector = null;
		}
	}
	
	@Test
	public void query() {
		qm.query("Elephants cannot fly.");
		Assert.assertTrue(qm.hasNext());		
		Assert.assertEquals("Elephants cannot fly.", qm.next().source.toText());
		// FIXME: Pensieve should return the match without codes first!!!
		Assert.assertEquals("Les \u00e9l\u00e9phants <b>ne peuvent pas</b> voler.", qm.next().target.toText());
	}

	@Test
	public void leverageNoFill() {
		ITextUnit tu = new TextUnit("1");
		tu.setSourceContent(new TextFragment("Elephants cannot fly."));
		qm.setOptions(999, false, false, false, null, 0, false);
		qm.leverage(tu); //, 999, false, null, 0);
		
		Assert.assertEquals("", tu.getTarget(locFRFR).toString());
		
		AltTranslationsAnnotation a = tu.getTarget(locFRFR).getAnnotation(AltTranslationsAnnotation.class);
		Assert.assertNotNull(a);		
		Assert.assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", a.getFirst().getTarget().toString());
	}
	
	@Test
	public void leverageFill() {
		ITextUnit tu = new TextUnit("1");
		tu.setSourceContent(new TextFragment("Elephants cannot fly."));
		qm.setOptions(1, false, false, false, null, 0, false);
		qm.leverage(tu); //, 1, false, null, 0);
		
		Assert.assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", tu.getTarget(locFRFR).toString());
		
		AltTranslationsAnnotation a = tu.getTarget(locFRFR).getAnnotation(AltTranslationsAnnotation.class);
		Assert.assertNotNull(a);		
		Assert.assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", a.getFirst().getTarget().toString());
	}

	@Test
	public void leverageFillWithPrefix () {
		ITextUnit tu = new TextUnit("1");
		tu.setSourceContent(new TextFragment("Elephants cannot fly."));
		qm.setOptions(1, false, false, false, "PREFIX! ", 100, false);
		qm.leverage(tu); //, 1, false, "PREFIX! ", 100);
		
		Assert.assertEquals("PREFIX! Les \u00e9l\u00e9phants ne peuvent pas voler.", tu.getTarget(locFRFR).toString());
		
		AltTranslationsAnnotation a = tu.getTarget(locFRFR).getAnnotation(AltTranslationsAnnotation.class);
		Assert.assertNotNull(a);		
		Assert.assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", a.getFirst().getTarget().toString());
	}

	@Test
	public void leverageFillWithNoPrefix () {
		ITextUnit tu = new TextUnit("1");
		tu.setSourceContent(new TextFragment("Elephants cannot fly."));
		qm.setOptions(1, false, false, false, "PREFIX! ", 99, false); // Threshold lower than score
		qm.leverage(tu); //, 1, false, "PREFIX! ", 99); 
		
		Assert.assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", tu.getTarget(locFRFR).toString());
		
		AltTranslationsAnnotation a = tu.getTarget(locFRFR).getAnnotation(AltTranslationsAnnotation.class);
		Assert.assertNotNull(a);		
		Assert.assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", a.getFirst().getTarget().toString());
	}

	@Test
	public void leverageFillSeveralSegments () {
		ITextUnit tu = new TextUnit("1");
		TextContainer tc = new TextContainer("Elephants cannot fly.");
		tc.getSegments().append(new Segment("s2", new TextFragment("Except Dumbo!")), " ");
		tu.setSource(tc);
		ISegments segs = tu.getSource().getSegments();
		assertEquals(2, segs.count());
		
		qm.setOptions(1, false, false, false, null, 0, false);
		qm.leverage(tu); //, 1, false, null, 0);
		
		segs = tu.getTarget(locFRFR).getSegments();
		assertEquals(2, segs.count());
		assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", segs.get(0).text.toText());
		assertEquals("", segs.get(1).text.toText());
		
		AltTranslationsAnnotation a = segs.get(0).getAnnotation(AltTranslationsAnnotation.class);
		assertNotNull(a);
		assertEquals("Les \u00e9l\u00e9phants ne peuvent pas voler.", a.getFirst().getTarget().toString());
	}
}
