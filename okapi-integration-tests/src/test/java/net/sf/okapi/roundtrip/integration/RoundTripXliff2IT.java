package net.sf.okapi.roundtrip.integration;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.integration.BaseRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.common.integration.IntegrationtestUtils;
import net.sf.okapi.filters.xliff2.XLIFF2Filter;
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
public class RoundTripXliff2IT extends BaseRoundTripIT {
	private static final String CONFIG_ID = "okf_xliff2";
	private static final String DIR_NAME = "/xliff2/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xliff", ".xlf", "xlf2");

	final static FileLocation root = FileLocation.fromClass(RoundTripXliff2IT.class);

	public RoundTripXliff2IT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("test01.xlf");
		addKnownFailingFile("test3.xlf");
	}

	@Before
	public void setUp() throws Exception {
		filter = new XLIFF2Filter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void xliff2SingleFile() throws FileNotFoundException, URISyntaxException {
		for (final File file : IntegrationtestUtils.getTestFiles("/xliff2/debug/", EXTENSIONS)) {
			runTest(true, true, file, CONFIG_ID, null, FileComparator::accurateXmlFileCompare);
		}
	}

	@Test
	public void deepenSegmentation() throws FileNotFoundException, URISyntaxException {
		final File d = root.in("/xliff2/deepenSegmentation").asFile();
		for (final File c : IntegrationtestUtils.getConfigFile(d.getPath())) {
			for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), EXTENSIONS, true)) {
				final String configName = Util.getFilename(c.getAbsolutePath(), false);
				final String customConfigPath = c.getParent();
				runTest(true, true, file, configName, customConfigPath, FileComparator::accurateXmlFileCompare);
			}
		}
	}

	@Test
	public void mergeAsParagrah() throws FileNotFoundException, URISyntaxException {
		final File d = root.in("/xliff2/mergeIntoParagraph").asFile();
		for (final File c : IntegrationtestUtils.getConfigFile(d.getPath())) {
			for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), EXTENSIONS, true)) {
				final String configName = Util.getFilename(c.getAbsolutePath(), false);
				final String customConfigPath = c.getParent();
				runTest(true, true, file, configName, customConfigPath, FileComparator::accurateXmlFileCompare);
			}
		}
	}

	@Test
	public void duplicate_ids() throws FileNotFoundException, URISyntaxException {
		final File d = root.in("/xliff2/duplicate_inline_tags").asFile();
		for (final File c : IntegrationtestUtils.getConfigFile(d.getPath())) {
			for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), EXTENSIONS, true)) {
				final String configName = Util.getFilename(c.getAbsolutePath(), false);
				final String customConfigPath = c.getParent();
				runTest(true, true, file, configName, customConfigPath, FileComparator::accurateXmlFileCompare);
			}
		}
	}

	@Test
	public void xliff2Files() throws FileNotFoundException, URISyntaxException {		
		realTestFiles(null, true, FileComparator::accurateXmlFileCompare);
	}
}
