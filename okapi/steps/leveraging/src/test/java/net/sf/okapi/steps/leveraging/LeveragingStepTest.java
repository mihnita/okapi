package net.sf.okapi.steps.leveraging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.FilterEventsToRawDocumentStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.tm.pensieve.common.TranslationUnit;
import net.sf.okapi.tm.pensieve.common.TranslationUnitVariant;
import net.sf.okapi.tm.pensieve.writer.ITmWriter;
import net.sf.okapi.tm.pensieve.writer.TmWriterFactory;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LeveragingStepTest {
	
	static private FileLocation root;
	static private String tmDir;
	static private String tmDir2;
	static private LocaleId locEN = LocaleId.fromString("en");
	static private LocaleId locFR = LocaleId.fromString("fr");
	
	@BeforeClass
	public static void setupClass () {
		root = FileLocation.fromClass(LeveragingStepTest.class);
		tmDir = Util.ensureSeparator(Util.getTempDirectory(), true) + "levtestTM";
		tmDir2 = Util.ensureSeparator(Util.getTempDirectory(), true) + "levtestTM2";
		createTM();
		createTM2();
	}

	@Test
	public void testSimpleStep () {
		// Ensure output is deleted
		File outFile = root.out("test01.out.html").asFile();
		
		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(HtmlFilter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		
		LeveragingStep levStep = new LeveragingStep();
		Parameters params = levStep.getParameters();
		// Set connector to use
		net.sf.okapi.connectors.pensieve.Parameters tmParams = new net.sf.okapi.connectors.pensieve.Parameters();
		tmParams.fromString(params.getResourceParameters());
		tmParams.setDbDirectory(tmDir);
		params.setResourceParameters(tmParams.toString());
		// Set threshold for fuzzy
		params.setThreshold(90);
		// Set threshold for filling the target
		params.setFillTargetThreshold(90);
		
		pdriver.addStep(levStep);
		
		pdriver.addStep(new FilterEventsToRawDocumentStep());
		
		URI inputURI = root.in("/test01.html").asUri();
		URI outputURI = outFile.toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_html", outputURI, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		assertTrue(outFile.exists());
		
		InputDocument doc = new InputDocument(outFile.getPath(), null);
		// Check exact
		ITextUnit tu = FilterTestDriver.getTextUnit(new HtmlFilter(), doc, "UTF-8", locEN, locFR, 1);
		assertNotNull(tu);
		assertEquals(TRG_1.toText(), tu.getSource().getFirstContent().toText());
		// Check fuzzy
		tu = FilterTestDriver.getTextUnit(new HtmlFilter(), doc, "UTF-8", locEN, locFR, 2);
		assertNotNull(tu);
		assertEquals(TRG_2.toText(), tu.getSource().getFirstContent().toText());
	}
	
	private static final TextFragment TRG_1 = new TextFragment("FR This is an example of text");
	private static final TextFragment TRG_2 = new TextFragment("FR This is an example of TEXT");
	
	static private void createTM () {
		Util.deleteDirectory(tmDir, true);
		Util.createDirectories(tmDir+"/");

		ITmWriter tmWriter = TmWriterFactory.createFileBasedTmWriter(tmDir, true);
		TranslationUnitVariant source = new TranslationUnitVariant(locEN, new TextFragment("This is an example of text"));
		TranslationUnitVariant target = new TranslationUnitVariant(locFR, TRG_1);
		TranslationUnit tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);
		source = new TranslationUnitVariant(locEN, new TextFragment("This is an example of TEXT"));
		target = new TranslationUnitVariant(locFR, TRG_2);
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);
		tmWriter.commit();
		tmWriter.close();
	}

	@Test
	public void testWithTranslations1 () {
		// Leverage only if the target is empty or if the target is the same as the source
		File outFile = processWithTranslations(true, true);
		assertTrue(outFile.exists());

		InputDocument doc = new InputDocument(outFile.getPath(), null);
		IFilter filter = new XLIFFFilter();
		
		ITextUnit tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 1);
		assertEquals("TEXTE UN.", tu.getTarget(locFR).getFirstContent().toText()); // Leverage: target==source
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 2);
		assertEquals("Texte un bis.", tu.getTarget(locFR).getFirstContent().toText());
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 3);
		assertEquals("TEXTE UN TER.", tu.getTarget(locFR).getFirstContent().toText()); // Leverage: empty target

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 4);
		ISegments segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE DEUX. ", segs.get(0).getContent().toText()); // Leverage: target==source
		assertEquals("Texte trois.", segs.get(1).getContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 5);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("Texte quatre. ", segs.get(0).getContent().toText());
		assertEquals("TEXTE CINQ.", segs.get(1).getContent().toText()); // Leverage: target==source

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 6);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE SIX. ", segs.get(0).getContent().toText()); // Leverage: empty target
		assertEquals("TEXTE SEPT.", segs.get(1).getContent().toText()); // Leverage: empty target
	}

	@Test
	public void testWithTranslations2 () {
		// Leverage only if the target is empty (regardless if the target is the same as the source or not)
		File outFile = processWithTranslations(true, false);
		assertTrue(outFile.exists());

		InputDocument doc = new InputDocument(outFile.getPath(), null);
		IFilter filter = new XLIFFFilter();
		
		ITextUnit tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 1);
		assertEquals("Text one.", tu.getTarget(locFR).getFirstContent().toText());
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 2);
		assertEquals("Texte un bis.", tu.getTarget(locFR).getFirstContent().toText());
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 3);
		assertEquals("TEXTE UN TER.", tu.getTarget(locFR).getFirstContent().toText()); // Leverage: empty target

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 4);
		ISegments segs = tu.getTarget(locFR).getSegments();
		assertEquals("Text two. ", segs.get(0).getContent().toText());
		assertEquals("Texte trois.", segs.get(1).getContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 5);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("Texte quatre. ", segs.get(0).getContent().toText());
		assertEquals("Text five.", segs.get(1).getContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 6);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE SIX. ", segs.get(0).getContent().toText()); // Leverage: empty target
		assertEquals("TEXTE SEPT.", segs.get(1).getContent().toText()); // Leverage: empty target
	}

	@Test
	public void testWithTranslations3 () {
		// Leverage even if the target is not empty
		// This leverages everything
		File outFile = processWithTranslations(false, true);
		assertTrue(outFile.exists());

		InputDocument doc = new InputDocument(outFile.getPath(), null);
		IFilter filter = new XLIFFFilter();
		
		ITextUnit tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 1);
		assertEquals("TEXTE UN.", tu.getTarget(locFR).getFirstContent().toText());
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 2);
		assertEquals("TEXTE UN BIS.", tu.getTarget(locFR).getFirstContent().toText());
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 3);
		assertEquals("TEXTE UN TER.", tu.getTarget(locFR).getFirstContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 4);
		ISegments segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE DEUX. ", segs.get(0).getContent().toText());
		assertEquals("TEXTE TROIS.", segs.get(1).getContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 5);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE QUATRE. ", segs.get(0).getContent().toText());
		assertEquals("TEXTE CINQ.", segs.get(1).getContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 6);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE SIX. ", segs.get(0).getContent().toText());
		assertEquals("TEXTE SEPT.", segs.get(1).getContent().toText());
	}

	@Test
	public void testWithTranslations4 () {
		// Leverage even if the target is not empty (second false not used)
		// This leverages everything
		File outFile = processWithTranslations(false, false);
		assertTrue(outFile.exists());

		InputDocument doc = new InputDocument(outFile.getPath(), null);
		IFilter filter = new XLIFFFilter();
		
		ITextUnit tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 1);
		assertEquals("TEXTE UN.", tu.getTarget(locFR).getFirstContent().toText());
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 2);
		assertEquals("TEXTE UN BIS.", tu.getTarget(locFR).getFirstContent().toText());
		
		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 3);
		assertEquals("TEXTE UN TER.", tu.getTarget(locFR).getFirstContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 4);
		ISegments segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE DEUX. ", segs.get(0).getContent().toText());
		assertEquals("TEXTE TROIS.", segs.get(1).getContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 5);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE QUATRE. ", segs.get(0).getContent().toText());
		assertEquals("TEXTE CINQ.", segs.get(1).getContent().toText());

		tu = FilterTestDriver.getTextUnit(filter, doc, "UTF-8", locEN, locFR, 6);
		segs = tu.getTarget(locFR).getSegments();
		assertEquals("TEXTE SIX. ", segs.get(0).getContent().toText());
		assertEquals("TEXTE SEPT.", segs.get(1).getContent().toText());
	}

	public File processWithTranslations (boolean onlyIfTargetIsEmpty,
			boolean onlyIfTargetIsSameAsSource)
	{
		// Ensure output is deleted
		File outFile = root.out("with-trans.out.xlf").asFile();
		if ( outFile.exists() ) {
			outFile.delete();
		}
		assertFalse(outFile.exists());
		
		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(XLIFFFilter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		
		LeveragingStep levStep = new LeveragingStep();
		Parameters params = levStep.getParameters();
		// Set connector to use
		net.sf.okapi.connectors.pensieve.Parameters tmParams = new net.sf.okapi.connectors.pensieve.Parameters();
		tmParams.fromString(params.getResourceParameters());
		tmParams.setDbDirectory(tmDir2);
		params.setResourceParameters(tmParams.toString());
		// Set threshold for fuzzy
		params.setThreshold(90);
		// Set threshold for filling the target
		params.setFillTargetThreshold(90);
		
		params.setFillIfTargetIsEmpty(onlyIfTargetIsEmpty);
		params.setFillIfTargetIsSameAsSource(onlyIfTargetIsSameAsSource);
		
		pdriver.addStep(levStep);
		
		pdriver.addStep(new FilterEventsToRawDocumentStep());
		
		URI inputURI = root.in("/with-trans.xlf").asUri();
		URI outputURI = outFile.toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();
		return outFile;
	}

	static private void createTM2 () {
		Util.deleteDirectory(tmDir2, true);
		Util.createDirectories(tmDir2+"/");

		ITmWriter tmWriter = TmWriterFactory.createFileBasedTmWriter(tmDir2, true);
		
		TranslationUnitVariant source = new TranslationUnitVariant(locEN, new TextFragment("Text one."));
		TranslationUnitVariant target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE UN."));
		TranslationUnit tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);
		
		source = new TranslationUnitVariant(locEN, new TextFragment("Text one bis."));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE UN BIS."));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		source = new TranslationUnitVariant(locEN, new TextFragment("Text one ter."));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE UN TER."));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		source = new TranslationUnitVariant(locEN, new TextFragment("Text two. "));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE DEUX. "));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		source = new TranslationUnitVariant(locEN, new TextFragment("Text three."));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE TROIS."));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		source = new TranslationUnitVariant(locEN, new TextFragment("Text four. "));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE QUATRE. "));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		source = new TranslationUnitVariant(locEN, new TextFragment("Text five."));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE CINQ."));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		source = new TranslationUnitVariant(locEN, new TextFragment("Text six. "));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE SIX. "));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		source = new TranslationUnitVariant(locEN, new TextFragment("Text seven."));
		target = new TranslationUnitVariant(locFR, new TextFragment("TEXTE SEPT."));
		tu = new TranslationUnit(source, target);
		tmWriter.indexTranslationUnit(tu);

		tmWriter.commit();
		tmWriter.close();
	}

}
