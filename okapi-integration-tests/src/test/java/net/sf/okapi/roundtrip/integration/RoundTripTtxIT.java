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

import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.common.integration.StricterRoundTripIT;
import net.sf.okapi.filters.ttx.TTXFilter;

@RunWith(JUnit4.class)
public class RoundTripTtxIT extends StricterRoundTripIT {
	private static final String CONFIG_ID = "okf_ttx";
	private static final String DIR_NAME = "/ttx/";
	private static final List<String> EXTENSIONS = Arrays.asList(".ttx");

	public RoundTripTtxIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new TTXFilter();
		addKnownFailingFile("Test02_noseg.html.ttx");
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void ttxFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::accurateXmlFileCompare);
	}
}
