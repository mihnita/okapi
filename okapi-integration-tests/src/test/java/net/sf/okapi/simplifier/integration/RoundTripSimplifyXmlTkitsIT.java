package net.sf.okapi.simplifier.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.integration.BaseRoundTripSimplifyTkitsIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.common.integration.IntegrationtestUtils;
import net.sf.okapi.filters.xml.XMLFilter;

@RunWith(JUnit4.class)
public class RoundTripSimplifyXmlTkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_xml";
	private static final String DIR_NAME = "/xml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xml");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".xliff";

	public RoundTripSimplifyXmlTkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
	}

	@Before
	public void setUp() throws Exception {
		filter = new XMLFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void xmlSingleFileTranslate1() throws FileNotFoundException, URISyntaxException {
		for (final File file : IntegrationtestUtils.getTestFiles(DIR_NAME, Arrays.asList(".xml.single"))) {
			runTest(true, false, file, CONFIG_ID, null, FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void xmlIssue591() throws FileNotFoundException, URISyntaxException {
		// %23 for #, throws an exception otherwise. Should fix FileLocation...
		for (final File file : IntegrationtestUtils.getTestFiles("/xml/Issue%23591/", EXTENSIONS)) {
			runTest(true, false, file, "okf_xml@ibxlf1", file.getParent(), FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void xmlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::accurateXmlFileCompare);
	}
}
