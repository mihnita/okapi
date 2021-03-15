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
import net.sf.okapi.filters.json.JSONFilter;

@RunWith(JUnit4.class)
public class RoundTripJsonIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_json";
	private static final String DIR_NAME = "/json/";
	private static final List<String> EXTENSIONS = Arrays.asList(".json");

	public RoundTripJsonIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("1 Edward Parallax.json");
	}

	@Before
	public void setUp() throws Exception {
		filter = new JSONFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void jsonFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
