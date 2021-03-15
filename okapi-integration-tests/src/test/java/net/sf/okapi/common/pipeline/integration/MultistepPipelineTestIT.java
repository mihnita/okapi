package net.sf.okapi.common.pipeline.integration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipeline.IPipelineStep;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.steps.common.FilterEventsToRawDocumentStep;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.common.RawDocumentWriterStep;
import net.sf.okapi.steps.common.createtarget.CreateTargetStep;
import net.sf.okapi.steps.searchandreplace.SearchAndReplaceStep;
import net.sf.okapi.steps.segmentation.Parameters;
import net.sf.okapi.steps.segmentation.SegmentationStep;
import net.sf.okapi.steps.xsltransform.XSLTransformStep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MultistepPipelineTestIT
{

	private FilterConfigurationMapper fcMapper;
	private IPipelineDriver driver;
	private final LocaleId locEN = LocaleId.fromString("EN");
	private final LocaleId locEUES = LocaleId.fromString("eu-ES");
	private final LocaleId locFR = LocaleId.fromString("FR");

	@Before
	public void setUp() throws Exception {
		// Create the mapper
		fcMapper = new FilterConfigurationMapper();
		// Fill it with the default configurations of several filters
		fcMapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.properties.PropertiesFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");

		// Create the driver
		driver = new PipelineDriver();

		// Set the filter configuration mapper
		driver.setFilterConfigurationMapper(fcMapper);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void xsltPipeline() throws URISyntaxException {
		String xsltPath = MultistepPipelineTestIT.class.getResource("identity.xsl").toURI().getPath();
		IPipelineStep step1 = new XSLTransformStep();
		((net.sf.okapi.steps.xsltransform.Parameters) step1.getParameters()).setXsltPath(xsltPath);

		IPipelineStep step2 = new XSLTransformStep();
		((net.sf.okapi.steps.xsltransform.Parameters) step2.getParameters()).setXsltPath(xsltPath);

		IPipelineStep step3 = new XSLTransformStep();
		((net.sf.okapi.steps.xsltransform.Parameters) step3.getParameters()).setXsltPath(xsltPath);

		IPipelineStep step4 = new RawDocumentWriterStep();

		driver.addStep(step1);
		driver.addStep(step2);
		driver.addStep(step3);
		driver.addStep(step4);

		// Set the info for the input and output
		try (RawDocument rawDoc = new RawDocument(getUri("test01.xml"), "UTF-8", locEN, locFR)) {
			rawDoc.setFilterConfigId("okf_xml");
			driver.addBatchItem(rawDoc, getOutputUri("test01.xml"), "UTF-8");
		}

		try (RawDocument rawDoc = new RawDocument(getUri("test02.xml"), "UTF-8", locEN, locFR)) {
			rawDoc.setFilterConfigId("okf_xml");
			driver.addBatchItem(rawDoc, getOutputUri("test02.xml"), "UTF-8");
		}

		try (RawDocument rawDoc = new RawDocument(getUri("test03.xml"), "UTF-8", locEN, locFR)) {
			rawDoc.setFilterConfigId("okf_xml");
			driver.addBatchItem(rawDoc, getOutputUri("test03.xml"), "UTF-8");
		}

		try (RawDocument rawDoc = new RawDocument(getUri("test04.xml"), "UTF-8", locEN, locFR)) {
			rawDoc.setFilterConfigId("okf_xml");
			driver.addBatchItem(rawDoc, getOutputUri("test04.xml"), "UTF-8");
		}

		driver.processBatch();
		
		assertTrue((new File(getOutputUri("test01.xml"))).exists());
		assertTrue((new File(getOutputUri("test02.xml"))).exists());
		assertTrue((new File(getOutputUri("test03.xml"))).exists());
		assertTrue((new File(getOutputUri("test04.xml"))).exists());
				
		driver.clearItems();
		driver.clearSteps();
	}

	@Test
	public void searchAndReplacePipeline() throws URISyntaxException {
		IPipelineStep step1 = new RawDocumentToFilterEventsStep();
		IPipelineStep step2 = new SearchAndReplaceStep();
		((net.sf.okapi.steps.searchandreplace.Parameters) step2.getParameters()).addRule(new String[] { "true",
				"Okapi Framework", "Big Foot" });
		FindStringStep step3 = new FindStringStep("Big Foot");

		driver.addStep(step1);
		driver.addStep(new CreateTargetStep());
		driver.addStep(step2);
		driver.addStep(step3);

		// Set the info for the input and output
		try (RawDocument rawDoc = new RawDocument(getUri("okapi_intro_test.html"), "UTF-8", locEN, locFR)) {
			rawDoc.setFilterConfigId("okf_html");
			driver.addBatchItem(rawDoc);

			driver.processBatch();

			assertTrue(step3.isFound());

			driver.clearItems();
			driver.clearSteps();
		}
	}

	@Test
	public void backAndForthPipeline() throws URISyntaxException {				
		driver.addStep(new RawDocumentToFilterEventsStep());
		driver.addStep(new FilterEventsToRawDocumentStep());
		driver.addStep(new RawDocumentToFilterEventsStep());
		driver.addStep(new FilterEventsToRawDocumentStep());
		driver.addStep(new RawDocumentToFilterEventsStep());
		
		IPipelineStep searchReplaceStep = new SearchAndReplaceStep();
		((net.sf.okapi.steps.searchandreplace.Parameters) searchReplaceStep.getParameters()).addRule(new String[] { "true",
				"Okapi Framework", "Big Foot" });
		driver.addStep(new CreateTargetStep());
		driver.addStep(searchReplaceStep);
		
		FindStringStep findStep = new FindStringStep("Big Foot");
		driver.addStep(findStep);

		// Set the info for the input and output
		try (RawDocument rawDoc = new RawDocument(getUri("okapi_intro_test.html"), "UTF-8", locEN, locFR)) {
			rawDoc.setFilterConfigId("okf_html");
			driver.addBatchItem(rawDoc, getOutputUri("okapi_intro_test.html"), "UTF-8");

			driver.processBatch();		

			assertTrue(findStep.isFound());
		
			driver.clearItems();
			driver.clearSteps();
		}
	}
	
	@Test
	public void copySourceToTargetPipeline() throws URISyntaxException {			
		driver.addStep(new RawDocumentToFilterEventsStep());
		CreateTargetStep copySourceToTargetStep = new CreateTargetStep();
		copySourceToTargetStep.setTargetLocale(locEUES);
		driver.addStep(copySourceToTargetStep);
		driver.addStep(new FilterEventsWriterStep());

		// Set the info for the input and output
		try (RawDocument rawDoc = new RawDocument(getUri("Test01.properties"), "UTF-8", locEN, locEUES)) {
			rawDoc.setFilterConfigId("okf_properties");
			driver.addBatchItem(rawDoc, getOutputUri("Test01.properties"), "UTF-8");
		}

		try (RawDocument rawDoc = new RawDocument(getUri("Test02.properties"), "UTF-8", locEN, locEUES)) {
			rawDoc.setFilterConfigId("okf_properties");
			driver.addBatchItem(rawDoc, getOutputUri("Test02.properties"), "UTF-8");
		}

		try (RawDocument rawDoc = new RawDocument(getUri("Test03.properties"), "UTF-8", locEN, locEUES)) {
			rawDoc.setFilterConfigId("okf_properties");
			driver.addBatchItem(rawDoc, getOutputUri("Test03.properties"), "UTF-8");
		}

		driver.processBatch();	
		
		assertTrue((new File(getOutputUri("Test01.properties"))).exists());
		assertTrue((new File(getOutputUri("Test02.propertiesl"))).exists());
		assertTrue((new File(getOutputUri("Test03.properties"))).exists());

		driver.clearItems();
		driver.clearSteps();
	}

	@Test
	public void segmentationPipeline() throws URISyntaxException {			
		driver.addStep(new RawDocumentToFilterEventsStep());
		SegmentationStep ss = new SegmentationStep();
		Parameters sp = (Parameters)ss.getParameters();
		sp.setSourceSrxPath(new File(getUri("test.srx")).getAbsolutePath());
		sp.setSegmentSource(true);
		driver.addStep(ss);
		driver.addStep(new FilterEventsWriterStep());
						
		// Set the info for the input and output
		try (RawDocument rawDoc = new RawDocument(getUri("Test01.properties"), "UTF-8", locEN)) {
			rawDoc.setFilterConfigId("okf_properties");
			driver.addBatchItem(rawDoc, getOutputUri("Test01.properties"), "UTF-8");
				
			driver.processBatch();	
		
			assertTrue((new File(getOutputUri("Test01.properties"))).exists());
		
			driver.clearItems();
			driver.clearSteps();
		}
	}

	private URI getUri(String fileName) throws URISyntaxException {
		URL url = MultistepPipelineTestIT.class.getResource("/" + fileName);
		return url.toURI();
	}

	private URI getOutputUri(String fileName) {
		File f = new File(Util.getTempDirectory() + "/fileName");
		// FIXME: never use deleteOnExit
		f.deleteOnExit();
		return f.toURI();
	}
}
