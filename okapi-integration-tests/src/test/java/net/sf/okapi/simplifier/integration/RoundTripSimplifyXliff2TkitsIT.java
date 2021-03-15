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
import net.sf.okapi.filters.xliff2.XLIFF2Filter;

@RunWith(JUnit4.class)
public class RoundTripSimplifyXliff2TkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_xliff2";
	private static final String DIR_NAME = "/xliff2/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xliff", ".xlf");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".simplify_xliff_extracted";

	public RoundTripSimplifyXliff2TkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
	}

	@Before
	public void setUp() throws Exception {
		filter = new XLIFF2Filter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void debug() throws FileNotFoundException, URISyntaxException {
		// run top level files (without config)
		for (final File file : IntegrationtestUtils.getTestFiles("/xliff2/duplicate_inline_tags", EXTENSIONS)) {
			runTest(true, true, file, CONFIG_ID, null, FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void xliff2Files() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, true, FileComparator::accurateXmlFileCompare);
	}
}
