package net.sf.okapi.roundtrip.integration;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import net.sf.okapi.common.integration.StricterRoundTripIT;
import net.sf.okapi.filters.mif.MIFFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.integration.EventRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.filters.markdown.MarkdownFilter;

@RunWith(JUnit4.class)
public class RoundTripMifIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_mif";
	private static final String DIR_NAME = "/mif/";
	private static final List<String> EXTENSIONS = Arrays.asList(".mif");

	public RoundTripMifIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new MIFFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void mifFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}