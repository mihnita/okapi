package net.sf.okapi.simplifier.integration;

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
import net.sf.okapi.filters.its.html5.HTML5Filter;

@RunWith(JUnit4.class)
public class RoundTripSimplifierHtmlItsTkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_itshtml5";
	private static final String DIR_NAME = "/htmlIts/";
	private static final List<String> EXTENSIONS = Arrays.asList(".html", ".html5");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".xliff";

	public RoundTripSimplifierHtmlItsTkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
	}

	@Before
	public void setUp() throws Exception {
		filter = new HTML5Filter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void itsHtmlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::accurateXmlFileCompare);
	}
}
