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
import net.sf.okapi.filters.ttx.TTXFilter;

@RunWith(JUnit4.class)
public class TtxXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_ttx";
	private static final String DIR_NAME = "/ttx/";
	private static final List<String> EXTENSIONS = Arrays.asList(".ttx");

	public TtxXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, LocaleId.EMPTY);
	}

	@Before
	public void setUp() throws Exception {
		filter = new TTXFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void ttxXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, true);
	}
}
