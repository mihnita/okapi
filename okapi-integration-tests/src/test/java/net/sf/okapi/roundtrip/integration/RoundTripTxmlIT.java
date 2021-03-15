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
import net.sf.okapi.filters.txml.TXMLFilter;

@RunWith(JUnit4.class)
public class RoundTripTxmlIT extends EventRoundTripIT {
	private static final String CONFIG_ID = "okf_txml";
	private static final String DIR_NAME = "/txml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".txml");

	public RoundTripTxmlIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS);
		addKnownFailingFile("Test01.docx.txml");
		addKnownFailingFile("Test03.mif.txml");
		addKnownFailingFile("Test02.html.txml");
	}

	@Before
	public void setUp() throws Exception {
		filter = new TXMLFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	/**
	 * <b>WARNING: all current tests fail!!!
	 * @throws FileNotFoundException
	 * @throws URISyntaxException
	 */
	public void txmlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::eventCompare);
	}
}
