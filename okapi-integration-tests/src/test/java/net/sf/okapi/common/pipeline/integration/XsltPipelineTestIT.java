package net.sf.okapi.common.pipeline.integration;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XsltPipelineTestIT
{

    private IPipelineDriver driver;
	private final LocaleId locEN = LocaleId.fromString("EN");
	static FileLocation root;

    @Before
    public void setUp() throws Exception {
    	IFilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
    	fcMapper.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
    	driver = new PipelineDriver();
    	driver.setFilterConfigurationMapper(fcMapper);
    	root = FileLocation.fromClass(this.getClass());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void runXsltPipeline() throws URISyntaxException,
            IOException {
        driver.clearItems();

        // Input resource
		URI inputXml = root.in("test.xml").asUri();

		try (InputStream in1 = root.in("identity.xsl").asInputStream();
				InputStream in2 = root.in("remove_b_tags.xsl").asInputStream();
				RawDocument rd = new RawDocument(inputXml, "UTF-8", locEN)) {
			// Make copy of input
	        driver.addStep(new XsltTransformStep(in1));

	        // Remove b tags from input
	        driver.addStep(new XsltTransformStep(in2));

	        // Filtering step - converts resource to events
	        driver.addStep(new RawDocumentToFilterEventsStep());

	        // Writer step - converts events to a resource
	        driver.addStep(new FilterEventsWriterStep());

	        rd.setFilterConfigId("okf_xml");
	        File outFile = root.out("output.xml").asFile();
	        driver.addBatchItem(rd, outFile.toURI(), "UTF-8");
	        driver.processBatch();

        // Read the result and compare
        StringBuilder tmp = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(outFile), "UTF-8"))) {
        	char[] buf = new char[2048];
        	int count;
        	while ((count = reader.read(buf)) != -1) {
        		tmp.append(buf, 0, count);
        	}
        }
        // Remove new lines so this test will pass on all OSes
        String tmpStr = tmp.toString().replaceAll("\n", "").replaceAll("\r", "");
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<start fileID=\"02286_000_000\">"
                +     "<para id=\"1\">This is a test with .</para>"
                + "</start>",
                tmpStr);
		}
    }

}
