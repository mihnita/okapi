package net.sf.okapi.xliffcompare.integration;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.integration.BaseXliffCompareIT;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.html.Parameters;

@RunWith(JUnit4.class)
public class HtmlXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_html";
	private static final String DIR_NAME = "/html/";
	private static final List<String> EXTENSIONS = Arrays.asList(".html", ".htm");

	private Parameters params;

	public HtmlXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("98959751.html");
		setRecursive(true);
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
	public void htmlXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(params, false);
	}
}
