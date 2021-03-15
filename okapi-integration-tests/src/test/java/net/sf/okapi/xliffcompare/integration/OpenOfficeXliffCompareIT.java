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
import net.sf.okapi.filters.openoffice.OpenOfficeFilter;

@RunWith(JUnit4.class)
public class OpenOfficeXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_openoffice";
	private static final String DIR_NAME = "/openoffice/";
	private static final List<String> EXTENSIONS =
			Arrays.asList(".odt", ".ods", ".odg", ".odp", ".ott", ".ots", ".otp", ".otg");

	public OpenOfficeXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new OpenOfficeFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void openOfficeXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
