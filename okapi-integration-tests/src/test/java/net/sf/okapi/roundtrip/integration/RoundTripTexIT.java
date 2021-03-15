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

import net.sf.okapi.common.integration.EventRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.filters.tex.TEXFilter;

@RunWith(JUnit4.class)
public class RoundTripTexIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_tex";
	private static final String DIR_NAME = "/tex/";
	private static final List<String> EXTENSIONS = Arrays.asList(".tex");

	public RoundTripTexIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("sample.tex");
		addKnownFailingFile("sample1.tex");
	}

	@Before
	public void setUp() throws Exception {
		filter = new TEXFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void texFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
