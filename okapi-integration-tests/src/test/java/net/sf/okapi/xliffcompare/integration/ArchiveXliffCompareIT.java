package net.sf.okapi.xliffcompare.integration;

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
import net.sf.okapi.common.integration.BaseXliffCompareIT;
import net.sf.okapi.filters.archive.ArchiveFilter;
import net.sf.okapi.filters.archive.Parameters;
import net.sf.okapi.filters.tmx.TmxFilter;

@RunWith(JUnit4.class)
public class ArchiveXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_archive";
	private static final String DIR_NAME = "/archive/";
	private static final List<String> EXTENSIONS = Arrays.asList(".archive", ".zip");

	Parameters params;

	public ArchiveXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		setRecursive(true);
	}

	@Before
	public void setUp() throws Exception {
		FilterConfigurationMapper fcm = new FilterConfigurationMapper();
		// Create configuration for tmx extension (if we need text units from tmx as well)
		try (TmxFilter tmxFilter = new TmxFilter()) {
			for (FilterConfiguration cfg : tmxFilter.getConfigurations()) {
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
	public void archiveXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(params, false);
	}
}
