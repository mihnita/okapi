package net.sf.okapi.simplifier.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.integration.BaseRoundTripSimplifyTkitsIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.common.integration.IntegrationtestUtils;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

@RunWith(JUnit4.class)
public class RoundTripSimplifyOpenXmlTkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_openxml";
	private static final String DIR_NAME = "/openxml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".docx", ".pptx", ".xlsx");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".simplify_xliff";

	public RoundTripSimplifyOpenXmlTkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
		// skip these for now
		addKnownFailingFile("offce_2013_Hangs.docx"); 
		addKnownFailingFile("big.docx");
		addKnownFailingFile("offce_2013_big.docx");
	}

	@Before
	public void setUp() throws Exception {
		filter = new OpenXMLFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Ignore("placeholder to test single file")
	public void singleFile() throws FileNotFoundException, URISyntaxException {
		for (final File file : IntegrationtestUtils.getTestFiles(DIR_NAME, Arrays.asList(".docx_FAILURE"))) {
			runTest(true, false, file, CONFIG_ID, null, FileComparator::zipAccurateXMLFileComparator);
		}
	}

	@Test
	public void openXmlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::zipAccurateXMLFileComparator);
	}
}
