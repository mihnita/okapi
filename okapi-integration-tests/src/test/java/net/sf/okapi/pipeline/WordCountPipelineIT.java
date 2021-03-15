package net.sf.okapi.pipeline;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.scopingreport.Parameters;
import net.sf.okapi.steps.scopingreport.ScopingReportStep;
import net.sf.okapi.steps.wordcount.WordCountStep;

public class WordCountPipelineIT
{
	private static FilterConfigurationMapper fcMapper;	
	private static LocaleId locEN = LocaleId.ENGLISH;
	private static LocaleId locES = LocaleId.SPANISH;
	
	public static void setUp() throws Exception {
		// Create the mapper
		fcMapper = new FilterConfigurationMapper();
		// Fill it with the default configurations of several filters
		fcMapper.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
	}

	private static PipelineDriver createPipeline() throws Exception {
		URL rootUrl = WordCountPipelineIT.class.getResource("/test.srx");
		String root = Util.getDirectoryName(rootUrl.toURI().getPath()) + File.separator;
		
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.addStep(new RawDocumentToFilterEventsStep());
		driver.addStep(new WordCountStep());
		//driver.addStep(new SimpleWordCountStep());
		
		ScopingReportStep scope = new ScopingReportStep();
		net.sf.okapi.steps.scopingreport.Parameters p = (Parameters)scope.getParameters();
		p.setOutputPath(root + "scoping_report.html");
		driver.addStep(scope);		
		return driver;
	}
	
	private static URI getUri(String fileName) throws URISyntaxException {
		URL url = WordCountPipelineIT.class.getResource(fileName);
		return url.toURI();
	}
	
	public static void main(String[] args) throws Exception {		
		setUp();
		
		PipelineDriver pd = createPipeline();
		for (int i = 0; i <= 10000000L; i++) {
			// Intentional memory leak for testing
			@SuppressWarnings("resource")
			RawDocument rd = new RawDocument(getUri("/html/ugly_big.htm"), "UTF-8", locEN, locES);
			rd.setFilterConfigId("okf_html");
			pd.addBatchItem(rd, (new File("genericOutput.txt")).toURI(), "UTF-8");
			pd.processBatch();
			pd.clearItems();
		}				
	}
}
