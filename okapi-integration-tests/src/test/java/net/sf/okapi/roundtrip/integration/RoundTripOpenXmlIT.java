package net.sf.okapi.roundtrip.integration;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.integration.BaseRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

@RunWith(JUnit4.class)
public class RoundTripOpenXmlIT extends BaseRoundTripIT {
	private static final String CONFIG_ID = "okf_openxml";
	private static final String DIR_NAME = "/openxml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".docx", ".pptx", ".xlsx");

	public RoundTripOpenXmlIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new OpenXMLFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void openXmlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::zipAccurateXMLFileComparator);
	}
}
