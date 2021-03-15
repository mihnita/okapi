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
import net.sf.okapi.filters.doxygen.DoxygenFilter;

@RunWith(JUnit4.class)
public class RoundTripDoxygenIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_doxygen";
	private static final String DIR_NAME = "/doxygen/";
	private static final List<String> EXTENSIONS = Arrays.asList(".h", ".py");

	public RoundTripDoxygenIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("python.py");
		if ("\r\n".equals(System.lineSeparator())) {
			addKnownFailingFile("javadoc-style.h");
			addKnownFailingFile("sample.h");
		}
	}

	@Before
	public void setUp() throws Exception {
		filter = new DoxygenFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void doxygenFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
