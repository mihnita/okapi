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
import net.sf.okapi.filters.doxygen.DoxygenFilter;

@RunWith(JUnit4.class)
public class DoxygenXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_doxygen";
	private static final String DIR_NAME = "/doxygen/";
	private static final List<String> EXTENSIONS = Arrays.asList(".h", ".py");

	public DoxygenXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		setRecursive(true);
	}

	@Before
	public void setUp() throws Exception {
		filter = new DoxygenFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void doxygenXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
