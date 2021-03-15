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
import net.sf.okapi.filters.txml.TXMLFilter;

@RunWith(JUnit4.class)
public class TxmlXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_txml";
	private static final String DIR_NAME = "/txml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".txml");

	public TxmlXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, LocaleId.EMPTY);
	}

	@Before
	public void setUp() throws Exception {
		filter = new TXMLFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	//@Ignore("Test fail: gtmt attribute difference, but new merge seems correct")
	public void txmlXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, true);
	}
}
