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
import net.sf.okapi.filters.po.POFilter;

@RunWith(JUnit4.class)
public class RoundTripSimplifyPoTkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_po";
	private static final String DIR_NAME = "/po/";
	private static final List<String> EXTENSIONS = Arrays.asList(".po");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".xliff";

	public RoundTripSimplifyPoTkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
	}

	@Before
	public void setUp() throws Exception {
		filter = new POFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void poFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::utf8FilePerLineComparator);
	}
}
