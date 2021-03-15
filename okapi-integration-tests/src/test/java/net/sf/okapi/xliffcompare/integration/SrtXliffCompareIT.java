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
import net.sf.okapi.filters.regex.RegexFilter;

@RunWith(JUnit4.class)
public class SrtXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_regex-srt";
	private static final String DIR_NAME = "/srt/";
	private static final List<String> EXTENSIONS = Arrays.asList(".srt");

	public SrtXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new RegexFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void srtXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
