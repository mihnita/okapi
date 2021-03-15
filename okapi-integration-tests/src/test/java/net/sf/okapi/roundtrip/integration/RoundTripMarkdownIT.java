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
import net.sf.okapi.filters.markdown.MarkdownFilter;

@RunWith(JUnit4.class)
public class RoundTripMarkdownIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_markdown";
	private static final String DIR_NAME = "/markdown/";
	private static final List<String> EXTENSIONS = Arrays.asList(".md");

	public RoundTripMarkdownIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("html-table-w-empty-lines.md");
		addKnownFailingFile("example4.md");
		if ("\r\n".equals(System.lineSeparator())) {
			addKnownFailingFile("Inline HTML (Advanced).md");
			addKnownFailingFile("Inline HTML (Simple).md");
			addKnownFailingFile("Markdown Documentation - Basics.md");
			addKnownFailingFile("Markdown Documentation - Syntax.md");
			addKnownFailingFile("example3.md");
			addKnownFailingFile("html_list_changed.md");
			addKnownFailingFile("html_list_original.md");
			addKnownFailingFile("html_table1_original.md");
			addKnownFailingFile("html_table_changed.md");
			addKnownFailingFile("min_math_original.md");
			addKnownFailingFile("regressing_test_single_page.md");
			addKnownFailingFile("sample_html_combo.md");
		}
	}

	@Before
	public void setUp() throws Exception {
		filter = new MarkdownFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void markdownFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
