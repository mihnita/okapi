package net.sf.okapi.steps.idbasedcopy;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.po.POFilter;
import net.sf.okapi.filters.properties.PropertiesFilter;
import net.sf.okapi.steps.common.FilterEventsToRawDocumentStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IdBasedCopyStepTest {
	
	private FileLocation root;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	
	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void stub () {
		assertTrue(true);
	}
	
	@Test
	public void testCopy () {
		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(PropertiesFilter.class.getName());
		fcMapper.addConfigurations(POFilter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		pdriver.addStep(new IdBasedCopyStep());
		pdriver.addStep(new FilterEventsToRawDocumentStep());

		// Add the properties files
		URI input1URI = root.in("/destination1.properties").asUri();
		URI input2URI = root.in("/reference1.properties").asUri();
		String propOutputPath = "/destination1.out.properties";
		URI output1URI = root.out(propOutputPath).asUri();
		BatchItemContext bic1 = new BatchItemContext(input1URI, "UTF-8", "okf_properties", output1URI, "UTF-8", locEN, locFR);
		RawDocument rd2 = new RawDocument(input2URI, "UTF-8", locFR);
		rd2.setFilterConfigId("okf_properties");
		bic1.add(rd2, null, null);
		pdriver.addBatchItem(bic1);
		
		// Add the PO files
		input1URI = root.in("/destination1.po").asUri();
		input2URI = root.in("/reference1.po").asUri();
		String poOutputPath = "/destination1.out.po";
		output1URI = root.out(poOutputPath).asUri();
		bic1 = new BatchItemContext(input1URI, "UTF-8", "okf_po", output1URI, "UTF-8", locEN, locFR);
		rd2 = new RawDocument(input2URI, "UTF-8", locEN, locFR);
		rd2.setFilterConfigId("okf_po");
		bic1.add(rd2, null, null);
		pdriver.addBatchItem(bic1);

		// Make sure the output is deleted
		root.out(propOutputPath).asFile().delete();
		root.out(poOutputPath).asFile().delete();
		
		pdriver.processBatch();

		//TODO: need to test content too
		assertTrue(root.out(propOutputPath).asFile().exists());
		assertTrue(root.out(poOutputPath).asFile().exists());
	}

}
