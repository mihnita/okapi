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

import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.integration.BaseRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.filters.archive.ArchiveFilter;
import net.sf.okapi.filters.archive.Parameters;
import net.sf.okapi.filters.tmx.TmxFilter;

@RunWith(JUnit4.class)
public class RoundTripArchiveIT extends BaseRoundTripIT {
	private static final String CONFIG_ID = "okf_archive";
	private static final String DIR_NAME = "/archive/";
	private static final List<String> EXTENSIONS = Arrays.asList(".archive", ".zip");

	private Parameters params;

	public RoundTripArchiveIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
	}

	@Before
	public void setUp() throws Exception {
		final FilterConfigurationMapper fcm = new FilterConfigurationMapper();
		// Create configuration for tmx extension (if we need text units from tmx as well)
		try (TmxFilter tmxFilter = new TmxFilter()) {
			for (final FilterConfiguration cfg : tmxFilter.getConfigurations()) {
				fcm.addConfiguration(cfg);
			}
		}

		filter = new ArchiveFilter();
		filter.setFilterConfigurationMapper(fcm);

		params = new Parameters();
		params.setFileNames("*.xliff, *.tmx, *.xlf");
		params.setConfigIds("okf_xliff, okf_tmx, okf_xliff");
		filter.setParameters(params);
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void archiveFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(params, false, FileComparator::zipAccurateXMLFileComparator);
	}
}
