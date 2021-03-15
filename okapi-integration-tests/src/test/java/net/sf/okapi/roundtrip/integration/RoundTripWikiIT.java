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

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.integration.EventRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.filters.wiki.WikiFilter;

@RunWith(JUnit4.class)
public class RoundTripWikiIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_wiki";
	private static final String DIR_NAME = "/wikitext/";
	private static final List<String> EXTENSIONS = Arrays.asList(".wiki", ".txt");

	public RoundTripWikiIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, LocaleId.EMPTY);
	}

	@Before
	public void setUp() throws Exception {
		filter = new WikiFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void wikiFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
