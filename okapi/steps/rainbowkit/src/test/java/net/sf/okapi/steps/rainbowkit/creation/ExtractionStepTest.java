/*===========================================================================
  Copyright (C) 2010-2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.rainbowkit.creation;

import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipeline.IPipelineStep;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.icml.ICMLFilter;
import net.sf.okapi.filters.its.html5.HTML5Filter;
import net.sf.okapi.filters.openoffice.OpenOfficeFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;
import net.sf.okapi.filters.properties.PropertiesFilter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.filters.xmlstream.XmlStreamFilter;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.common.createtarget.CreateTargetStep;
import net.sf.okapi.steps.rainbowkit.ontram.OntramPackageWriter;
import net.sf.okapi.steps.rainbowkit.xliff.XLIFFPackageWriter;
import net.sf.okapi.steps.xsltransform.XSLTransformStep;

import org.custommonkey.xmlunit.XMLAssert;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(DataProviderRunner.class)
public class ExtractionStepTest {
	
	private FileLocation root;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	private LocaleId locENUS = LocaleId.fromString("en-us");
	private LocaleId locRURU = LocaleId.fromString("ru-ru");
	
	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void stub () {
		assertTrue(true);
	}
	
	@Test
	public void testSimpleStep () {
		// Ensure output is deleted
		assertTrue(Util.deleteDirectory(root.out("/pack1").asFile()));
		
		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(PropertiesFilter.class.getName());
		fcMapper.addConfigurations(OpenOfficeFilter.class.getName());
		fcMapper.addConfigurations(HTML5Filter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		ExtractionStep extStep = new ExtractionStep();
		String outputDir = root.out("/").toString();
		extStep.getParameters().setPackageDirectory(outputDir);
		pdriver.addStep(extStep);


		URI inputURI = root.in("/test01.properties").asUri();
		URI outputURI = new File("/test01.out.properties").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_properties", outputURI, "UTF-8", locEN, locFR));
		
		inputURI = root.in("/test02.html").asUri();
		outputURI = new File("/test02.out.html").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_itshtml5", outputURI, "UTF-8", locEN, locFR));
		
		inputURI = root.in("/sub Dir/test01.odt").asUri();
		outputURI = new File("/sub Dir/test01.out.odt").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_openoffice", outputURI, "UTF-8", locEN, locFR));

		pdriver.processBatch();

		File file = root.out("/pack1/work/test01.properties.xlf").asFile();
		assertTrue(file.exists());
		file = root.out("/pack1/work/test02.html.xlf").asFile();
		assertTrue(file.exists());
		file = root.out("/pack1/work/sub Dir/test01.odt.xlf").asFile();
		assertTrue(file.exists());
	}

// TODO MW: Unit test for issue #534
//	@Test
//	public void testICMLtoXLIFF2() throws URISyntaxException
//	{
//		// Ensure output is deleted
//		assertTrue(Util.deleteDirectory(root.out("/pack1").asFile()));
//
//		IPipelineDriver pdriver = new PipelineDriver();
//		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
//		fcMapper.addConfigurations(ICMLFilter.class.getName());
//		pdriver.setFilterConfigurationMapper(fcMapper);
//		String rootDir = root.in("/").toString();
//		pdriver.setRootDirectories(rootDir, rootDir);
//		pdriver.addStep(new RawDocumentToFilterEventsStep());
//		ExtractionStep extractionStep = new ExtractionStep();
//		Parameters params = new Parameters();
//		String outputDir = root.out("/").toString();
//		params.setPackageDirectory(outputDir);
//		params.setWriterClass("net.sf.okapi.steps.rainbowkit.xliff.XLIFF2PackageWriter");
//		extractionStep.setParameters(params);
//		pdriver.addStep(extractionStep);
//
//		URI inputURI = root.in("/Bullets_Test_EN.icml").asUri();
//		URI outputURI = new File("/Bullets_Test_EN.out.icml").toURI();
//		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_icml", outputURI, "UTF-8", locEN, locFR));
//
//		pdriver.processBatch();
//
//		File file = root.out("/pack1/work/Bullets_Test_EN.icml.xlf").asFile();
//		assertTrue(file.exists());
//
//		XLIFFDocument xlf = new XLIFFDocument();
//		xlf.load(file);
//		assertNotNull("There should be a file element in the generated XLIFF file.", xlf.getFileNode("f1"));
//		assertEquals("The XLIFF file should have one unit.", 1, xlf.getUnits().size());
//		Unit unit = xlf.getUnits().get(0);
//		assertEquals("The unit should have three segments.", 3, unit.getSegmentCount());
//	}

	@Test
	public void testICMLwithCDATAtoXLIFF2() {
		// NOTE MW: Unit test for issue #527

		// Ensure output is deleted
		assertTrue(Util.deleteDirectory(root.out("/pack1").asFile()));

		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(ICMLFilter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		ExtractionStep extractionStep = new ExtractionStep();
		Parameters params = new Parameters();
		String outputDir = root.out("/").toString();
		params.setPackageDirectory(outputDir);
		params.setWriterClass("net.sf.okapi.steps.rainbowkit.xliff.XLIFF2PackageWriter");
		extractionStep.setParameters(params);
		pdriver.addStep(extractionStep);

		URI inputURI = root.in("/CDATA_Test.icml").asUri();
		URI outputURI = new File("/CDATA_Test.out.icml").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_icml", outputURI, "UTF-8", locEN, locFR));

		pdriver.processBatch();

		File file = root.out("/pack1/work/CDATA_Test.icml.xlf").asFile();
		assertTrue(file.exists());

		XLIFFDocument xlf = new XLIFFDocument();
		xlf.load(file);
		assertNotNull("There should be a file element in the generated XLIFF file.", xlf.getFileNode("f1"));
		assertEquals("The XLIFF file should have two units.", 2, xlf.getUnits().size());
	}

	@DataProvider
	public static Object[][] extendedCodeTypesDataProvider() {
		return new Object[][]{
				{"extended-code-type-support.docx", OpenXMLFilter.class.getName(), "okf_openxml"},
				{"extended-code-type-support.docx_en-US_fr-FR.sdlxliff", XLIFFFilter.class.getName(), "okf_xliff"},
		};
	}

	@Test
	@UseDataProvider("extendedCodeTypesDataProvider")
	public void testExtendedCodeTypes(String filename, String filterClassName, String filterConfigurationId) throws Exception {
		Path rootPath = root.in("/").asPath();
		Path packPath = rootPath.resolve("pack1");

		assertTrue(Util.deleteDirectory(packPath.toFile()));

		FilterConfigurationMapper filterConfigurationMapper = new FilterConfigurationMapper();
		filterConfigurationMapper.addConfigurations(filterClassName);

		Parameters parameters = new Parameters();
		parameters.setWriterOptions("#v1\nincludeCodeAttrs.b=true");
		IPipelineStep extractionStep = new ExtractionStep();
		extractionStep.setParameters(parameters);

		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(filterConfigurationMapper);
		pdriver.setRootDirectories(rootPath.toString(), rootPath.toString());
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		pdriver.addStep(extractionStep);

		Path inputPath = rootPath.resolve("code-type").resolve(filename);
		Path outputPath = packPath.resolve("work/code-type").resolve(filename + ".xlf");
		pdriver.addBatchItem(new BatchItemContext(inputPath.toUri(), UTF_8.name(), filterConfigurationId, outputPath.toUri(), UTF_8.name(), locENUS, locFR));

		pdriver.processBatch();
		assertTrue(outputPath.toFile().exists());
		try (Reader out = Files.newBufferedReader(outputPath, UTF_8);
				Reader gold = Files.newBufferedReader(rootPath.resolve("code-type/gold").resolve(filename + ".xlf"), UTF_8)) {
		    XMLAssert.assertXMLEqual(gold, out);
		}
	}

	@Test
	public void testXINICreation ()	throws Exception
	{
		// Ensure output is deleted
		deleteOutputDir("/pack2");
		
		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(XLIFFFilter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		pdriver.addStep(new CreateTargetStep());
		
		ExtractionStep es = new ExtractionStep();
		pdriver.addStep(es);
		Parameters params = es.getParameters();
		String outputDir = root.out("/").toString();
		params.setPackageDirectory(outputDir);
		params.setWriterClass(OntramPackageWriter.class.getName());
		params.setPackageName("pack2");

		URI inputURI = root.in("/xiniPack/original/test1.xlf").asUri();
		URI outputURI  = new File("/pack2/original/test1.out.xlf").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, 
				"UTF-8", locENUS, locRURU));
		
		pdriver.processBatch();

		File file = root.out("/pack2/xini/contents.xini").asFile();
		assertTrue(file.exists());
		
		// Compare with the gold file
		try (Reader goldReader = Files.newBufferedReader(root.in("/xiniPack/xini/contents.xini").asPath(),
		                                                 StandardCharsets.UTF_8)) {
			String actual = new String(Files.readAllBytes(root.out("/pack2/xini/contents.xini").asPath()), StandardCharsets.UTF_8)
					.replaceFirst("xiniPack/original", "");
		    XMLAssert.assertXMLEqual(goldReader, new StringReader(actual));
		}

		deleteOutputDir("/pack2");
	}
	
	@Test
	public void testExtractionAfterXSLTStep()	throws Exception
	{
		// Ensure output is deleted
		deleteOutputDir("/pack3");
		
		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(XmlStreamFilter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.setOutputDirectory(rootDir); 

		XSLTransformStep xslTransformStep = new XSLTransformStep();
		net.sf.okapi.steps.xsltransform.Parameters xsltParams = xslTransformStep.getParameters();
		xsltParams.setXsltPath(root.in("/xsltAndXliffPack/renameTags.xslt").toString());
		xsltParams.setPassOnOutput(true);
		pdriver.addStep(xslTransformStep);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		
		ExtractionStep es = new ExtractionStep();
		Parameters params = es.getParameters();
		params.setWriterClass(XLIFFPackageWriter.class.getName());
		params.setPackageName("pack3");
		pdriver.addStep(es);

		URI inputURI = root.in("/xsltAndXliffPack/original/test.xml").asUri();
		URI outputURI = root.out("/xsltAndXliffPack-Output/test.xml").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xmlstream", outputURI, 
				"UTF-8", locENUS, locRURU));
		
		pdriver.processBatch();

		File xsltOutputFile = root.out("/xsltAndXliffPack-Output/test.xml").asFile();
		assertTrue(xsltOutputFile.exists());
		File rainbowkitOriginalFile = root.in("/pack3/original/xsltAndXliffPack/original/test.xml").asFile();
		assertTrue(rainbowkitOriginalFile.exists());

		// Compare with each other 
		try (Reader xsltOutputFileReader = Files.newBufferedReader(xsltOutputFile.toPath(), StandardCharsets.UTF_8);
				Reader rainbowkitOriginalFileReader = Files.newBufferedReader(rainbowkitOriginalFile.toPath(), StandardCharsets.UTF_8)) {                                    
		    XMLAssert.assertXMLEqual(xsltOutputFileReader, rainbowkitOriginalFileReader);
		}

		deleteOutputDir("/pack3");
		deleteOutputDir("/xsltAndXliffPack-Output");
	}

    public void deleteOutputDir(String dirname) {
		Util.deleteDirectory(root.out(dirname).toString(), false);
    }
}
