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
import net.sf.okapi.filters.table.TableFilter;

@RunWith(JUnit4.class)
public class RoundTripTableIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_table_csv";
	private static final String DIR_NAME = "/table/";
	private static final List<String> EXTENSIONS = Arrays.asList(".csv", ".tab");

	public RoundTripTableIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		filter = new TableFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void tableFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
