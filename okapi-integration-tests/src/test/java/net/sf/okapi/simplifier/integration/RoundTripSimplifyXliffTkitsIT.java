package net.sf.okapi.simplifier.integration;

import java.io.File;
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
import net.sf.okapi.common.integration.IntegrationtestUtils;
import net.sf.okapi.filters.xliff.XLIFFFilter;

@RunWith(JUnit4.class)
public class RoundTripSimplifyXliffTkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_xliff";
	private static final String DIR_NAME = "/xliff/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xliff", ".xlf", ".sdlxliff");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".simplify_xliff_extracted";

	public RoundTripSimplifyXliffTkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
		addKnownFailingFile("about_the.htm.xlf");
		addKnownFailingFile("test.txt.xlf");
	}

	@Before
	public void setUp() throws Exception {
		filter = new XLIFFFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void xliffSingleFileXliff() throws FileNotFoundException, URISyntaxException {
		for (final File file : IntegrationtestUtils.getTestFiles(DIR_NAME, Arrays.asList(".sdlxliff_single"))) {
			runTest(true, true, file, CONFIG_ID, null, FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void xliffSingleFileSdlXliff() throws FileNotFoundException, URISyntaxException {
		for (final File file : IntegrationtestUtils.getTestFiles(DIR_NAME, Arrays.asList(".sdlxliff_single"))) {
			runTest(true, true, file, "okf_xliff-sdl", null, FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void xliffSingleFileInvalidXml() throws FileNotFoundException, URISyntaxException {
		for (final File file : IntegrationtestUtils.getTestFiles(DIR_NAME, Arrays.asList(".invalid_single"))) {
			runTest(true, true, file, CONFIG_ID, null, FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void xliffSingleFileInvalidFullXml() throws FileNotFoundException, URISyntaxException {
		for (final File file : IntegrationtestUtils.getTestFiles(DIR_NAME, Arrays.asList(".invalid_full_single"))) {
			runTest(true, true, file, CONFIG_ID, null, FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void xliffFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, true, FileComparator::accurateXmlFileCompare);
	}
}
