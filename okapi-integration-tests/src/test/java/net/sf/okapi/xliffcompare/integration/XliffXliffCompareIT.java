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
import net.sf.okapi.filters.xliff.XLIFFFilter;

@RunWith(JUnit4.class)
public class XliffXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_xliff";
	private static final String DIR_NAME = "/xliff/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xliff", ".xlf", ".sdlxliff");

	public XliffXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, LocaleId.EMPTY);
	}

	@Before
	public void setUp() throws Exception {
		filter = new XLIFFFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void xliffXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, true);
	}
}
