package net.sf.okapi.simplifier.integration;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.integration.BaseRoundTripSimplifyTkitsIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.filters.yaml.YamlFilter;

@RunWith(JUnit4.class)
public class RoundTripSimplifyYamlTkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_yaml";
	private static final String DIR_NAME = "/yaml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".yml", ".yaml");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".simplify_xliff";

	public RoundTripSimplifyYamlTkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
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
	public void yamlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::utf8FilePerLineComparator);
	}
}
