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
import net.sf.okapi.filters.its.html5.HTML5Filter;

@RunWith(JUnit4.class)
public class HtmlItsXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_itshtml5";
	private static final String DIR_NAME = "/htmlIts/";
	private static final List<String> EXTENSIONS = Arrays.asList(".html", ".html5");

	public HtmlItsXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		setRecursive(true);
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
	public void itsHtmlXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
