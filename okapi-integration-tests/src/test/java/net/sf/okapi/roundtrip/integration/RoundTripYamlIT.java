package net.sf.okapi.roundtrip.integration;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.integration.EventRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.common.integration.IntegrationtestUtils;
import net.sf.okapi.filters.yaml.YamlFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class RoundTripYamlIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_yaml";
	private static final String DIR_NAME = "/yaml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".yml", ".yaml");

	public RoundTripYamlIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("unknown-tags-example.yaml");
		addKnownFailingFile("no-children-1-pretty.yaml");
		if ("\r\n".equals(System.lineSeparator())) {
			addKnownFailingFile("example2_14.yaml");
			addKnownFailingFile("example2_18.yaml");
			addKnownFailingFile("example2_27.yaml");
			addKnownFailingFile("example2_28.yaml");
			addKnownFailingFile("plain_wrapped.yml");
		}
	}

	@Before
	public void setUp() throws Exception {
		filter = new YamlFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void yamlWithHtmlSubfilter() throws FileNotFoundException, URISyntaxException {		
		// run each subdirectory where we assume there is a custom config)
		for(final File d : IntegrationtestUtils.getSubDirs("/yaml/subfilter/")) {
			for(final File c : IntegrationtestUtils.getConfigFile(d.getPath())) {
				for(final File file : IntegrationtestUtils.getTestFiles(d.getPath(), EXTENSIONS, true)) {					
					final String configName = Util.getFilename(c.getAbsolutePath(), false);
					final String customConfigPath = c.getParent();
					runTest(true, false, file, configName, customConfigPath, FileComparator::eventCompare);
				}
			}
		}
	}

	@Test
	public void yamlSingleFile() throws FileNotFoundException, URISyntaxException {		
		for (final File file : IntegrationtestUtils.getTestFiles("/yaml/examples/", Arrays.asList(".yaml"))) {
			runTest(true, false, file, CONFIG_ID, null, FileComparator::eventCompare);
		}
	}

	@Test
	public void yamlFiles() throws FileNotFoundException, URISyntaxException {		
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
