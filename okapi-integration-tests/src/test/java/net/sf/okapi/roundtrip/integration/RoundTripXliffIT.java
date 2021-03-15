package net.sf.okapi.roundtrip.integration;

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

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.integration.EventRoundTripIT;
import net.sf.okapi.common.integration.FileComparator;
import net.sf.okapi.common.integration.IntegrationtestUtils;
import net.sf.okapi.filters.xliff.XLIFFFilter;

@RunWith(JUnit4.class)
public class RoundTripXliffIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_xliff";
	private static final String DIR_NAME = "/xliff/";
	private static final List<String> EXTENSIONS = Arrays.asList(".xliff", ".xlf", ".sdlxliff");

	final static FileLocation root = FileLocation.fromClass(RoundTripXliffIT.class);

	public RoundTripXliffIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("non-segment-with-target.xlf");
		addKnownFailingFile("non-segment-without-target.xlf");
		addKnownFailingFile("tag_merge_error_src_diff.sdlxliff");
		addKnownFailingFile("lqiTest.xlf");
		addKnownFailingFile("about_the.htm.xlf");
		addKnownFailingFile("test.txt.xlf");
		addKnownFailingFile("sampe_sch.xliff");
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
	public void tag_merge_error() throws FileNotFoundException, URISyntaxException {
		final File d = root.in("/xliff/sdlxliff/tag").asFile();
		for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), Arrays.asList(".sdlxliff"), true)) {
			runTest(true, true, file, "okf_xliff-sdl", null, FileComparator::eventCompare);
		}
	}

	@Test
	public void fail() throws FileNotFoundException, URISyntaxException {
		final File d = root.in("/xliff/fail").asFile();
		for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), EXTENSIONS, true)) {
			runTest(true, true, file, "okf_xliff", null, FileComparator::eventCompare);
		}
	}

	@Test
	public void xliffFiles() throws FileNotFoundException, URISyntaxException {		
		realTestFiles(null, true, FileComparator::eventCompare);
	}
}
