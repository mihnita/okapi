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
import net.sf.okapi.filters.ts.TsFilter;

@RunWith(JUnit4.class)
public class RoundTripTsIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_ts";
	private static final String DIR_NAME = "/ts/";
	private static final List<String> EXTENSIONS = Arrays.asList(".ts");

	public RoundTripTsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("issue531.ts");
	}

	@Before
	public void setUp() throws Exception {
		filter = new TsFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void tsFiles() throws FileNotFoundException, URISyntaxException {		
		realTestFiles(null, true, FileComparator::eventCompare);
	}
}
