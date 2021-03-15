package net.sf.okapi.filters;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

public class BaseMemoryLeakTestIT {
	private FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();	
	private LocaleId locEN = LocaleId.fromString("EN");

	public void addConfigurations(String string) {
		fcMapper.addConfigurations(string);
	}

	private PipelineDriver simplePipeline() throws Exception {
		// Create the driver
		PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.addStep(new RawDocumentToFilterEventsStep());	
		return driver;
	}
	
	private URI getUri(String fileName) throws URISyntaxException {
		URL url = getClass().getResource(fileName);
		return url.toURI();
	}

	public void runIt(String configId, String docPath) throws Exception {
		PipelineDriver pd = simplePipeline();
		for (int i = 0; i <= 10_000_000L; i++) {
			// Intentional memory leak for testing
			RawDocument rd = new RawDocument(getUri(docPath), "UTF-8", locEN);
			rd.setFilterConfigId(configId);
			pd.addBatchItem(rd, (new File("genericOutput.txt")).toURI(), "UTF-8");
			pd.processBatch();
			pd.clearItems();			
		}				
	}
}
