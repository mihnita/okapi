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

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.integration.BaseXliffCompareIT;
import net.sf.okapi.filters.xliff2.XLIFF2Filter;

@RunWith(JUnit4.class)
public class Xliff2XliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_xliff2";
	private static final String DIR_NAME = "/xliff2/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xliff", ".xlf");

	public Xliff2XliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, LocaleId.EMPTY);
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
	public void xliff2XliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, true);
	}
}
