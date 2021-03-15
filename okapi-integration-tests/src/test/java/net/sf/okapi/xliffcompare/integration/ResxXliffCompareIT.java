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
import net.sf.okapi.filters.xml.XMLFilter;

@RunWith(JUnit4.class)
public class ResxXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_xml-resx";
	private static final String DIR_NAME = "/resx/";
	private static final List<String> EXTENSIONS = Arrays.asList(".resx");

	public ResxXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
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
	public void resxFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
