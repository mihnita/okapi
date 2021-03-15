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
import net.sf.okapi.filters.idml.IDMLFilter;

@RunWith(JUnit4.class)
public class RoundTripSimplifyIdmlTkitsIT extends BaseRoundTripSimplifyTkitsIT {
	private static final String CONFIG_ID = "okf_idml";
	private static final String DIR_NAME = "/idml/";
	private static final List<String> EXTENSIONS = Arrays.asList(".idml");
	private static final String XLIFF_EXTRACTED_EXTENSION = ".simplify_xliff";

	public RoundTripSimplifyIdmlTkitsIT() {
		super(CONFIG_ID, DIR_NAME, EXTENSIONS, XLIFF_EXTRACTED_EXTENSION);
		// skip these for now    
		addKnownFailingFile("sample_brochure2.idml"); 
		addKnownFailingFile("large_sample_newspaper1.idml");
		addKnownFailingFile("3_levels_of_translation_consistency.idml");
		addKnownFailingFile("7_elements_of_localizability_201104.idml");
		addKnownFailingFile("BookMynePlus.idml");
		addKnownFailingFile("4_story_pointers.idml");
	}

	@Before
	public void setUp() throws Exception {
		filter = new IDMLFilter();
	}

	@After
	public void tearDown() throws Exception {
		filter.close();
	}

	@Test
	public void idmlFiles() throws FileNotFoundException, URISyntaxException {
		realTestFiles(null, false, FileComparator::zipAccurateXMLFileComparator);
	}
}
