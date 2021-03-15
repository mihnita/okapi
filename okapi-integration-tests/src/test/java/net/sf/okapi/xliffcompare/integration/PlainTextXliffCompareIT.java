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
import net.sf.okapi.filters.plaintext.PlainTextFilter;

@RunWith(JUnit4.class)
public class PlainTextXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_plaintext";
	private static final String DIR_NAME = "/plaintext/";
	private static final List<String> EXTENSIONS = Arrays.asList("txt");

	public PlainTextXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new PlainTextFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void plainTextXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}