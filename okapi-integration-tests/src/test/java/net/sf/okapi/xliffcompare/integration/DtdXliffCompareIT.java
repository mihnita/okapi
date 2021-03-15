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
import net.sf.okapi.filters.dtd.DTDFilter;

@RunWith(JUnit4.class)
public class DtdXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_dtd";
	private static final String DIR_NAME = "/dtd/";
	private static final List<String> EXTENSIONS = Arrays.asList(".dtd");

	public DtdXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		setRecursive(true);
	}

	@Before
	public void setUp() throws Exception {
		filter = new DTDFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void dtdXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
