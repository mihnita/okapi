package net.sf.okapi.roundtrip.integration;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.integration.BaseRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.filters.xmlstream.XmlStreamFilter;

@RunWith(JUnit4.class)
public class RoundTripDitaIT extends BaseRoundTripIT {
	private static final String CONFIG_ID = "okf_xmlstream-dita";
	private static final String DIR_NAME = "/dita/";
	private static final List<String> EXTENSIONS = Arrays.asList(".dita", ".ditamap");

	public RoundTripDitaIT() {
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
	public void ditaFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::accurateXmlFileCompare);
	}
}