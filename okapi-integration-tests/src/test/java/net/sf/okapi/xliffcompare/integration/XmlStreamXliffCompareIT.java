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
import net.sf.okapi.filters.xmlstream.XmlStreamFilter;

@RunWith(JUnit4.class)
public class XmlStreamXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_xmlstream";
	private static final String DIR_NAME = "/xmlstream/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xml");

	public XmlStreamXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new XmlStreamFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void xmlStreamXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
