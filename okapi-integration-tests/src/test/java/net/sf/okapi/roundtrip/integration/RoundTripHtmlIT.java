package net.sf.okapi.roundtrip.integration;

import net.sf.okapi.common.integration.EventRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.common.integration.IntegrationtestUtils;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.html.Parameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class RoundTripHtmlIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_html";
	private static final String DIR_NAME = "/html/";
	private static final List<String> EXTENSIONS = Arrays.asList(".html", ".htm");

	private Parameters params;

	public RoundTripHtmlIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		// The files listed in the "if" fail only because of newline differences.
		// For HTML technically still the same.
		addKnownFailingFile("ugly_big.htm");
		addKnownFailingFile("111.zip.html");
		addKnownFailingFile("98959751.html");
	}

	@Before
	public void setUp() throws Exception {
		filter = new HtmlFilter();
		params = new Parameters(HtmlFilter.class.getResource("nonwellformedConfiguration.yml"));
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void htmlSingleFile() throws FileNotFoundException, URISyntaxException {		
		for (final File file : IntegrationtestUtils.getTestFiles("/html/issue1004/", EXTENSIONS)) {
			runTest(true, false, file, CONFIG_ID, null, FileComparator::eventCompare);
		}
	}

	@Test
	public void htmlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(params, false, FileComparator::eventCompare);
	}
}
