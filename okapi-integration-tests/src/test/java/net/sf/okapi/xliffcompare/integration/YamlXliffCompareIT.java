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

import net.sf.okapi.common.integration.BaseXliffCompareIT;
import net.sf.okapi.filters.yaml.YamlFilter;

@RunWith(JUnit4.class)
public class YamlXliffCompareIT extends BaseXliffCompareIT {
	private static final String CONFIG_ID = "okf_yaml";
	private static final String DIR_NAME = "/yaml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".yml", ".yaml");

	public YamlXliffCompareIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("unknown-tags-example.yaml");
		addKnownFailingFile("no-children-1-pretty.yaml");
		if ("\r\n".equals(System.lineSeparator())) {
			addKnownFailingFile("error8.yaml");
			addKnownFailingFile("error9.yaml");
			addKnownFailingFile("everything.yaml");
			addKnownFailingFile("example12.yaml");
			addKnownFailingFile("example2_14.yaml");
			addKnownFailingFile("example2_16.yaml");
			addKnownFailingFile("example2_18.yaml");
			addKnownFailingFile("example2_27.yaml");
			addKnownFailingFile("example2_28.yaml");
			addKnownFailingFile("folded_indented.yml");
			addKnownFailingFile("folded_literal_examples.yml");
			addKnownFailingFile("plain_wrapped.yml");
			addKnownFailingFile("scalar_sample.yml");
			addKnownFailingFile("single_wrapped.yml");		
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
	public void yamlXliffCompareFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false);
	}
}
