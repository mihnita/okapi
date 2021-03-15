package net.sf.okapi.filters.rtf.integration;

//import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.filters.rtf.RtfTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
//import net.sf.okapi.filters.rtf.RTFFilter;

@RunWith(JUnit4.class)
public class ExtractionComparisionTest {
//	private RTFFilter rtfFilter;
	private String[] testFileList;	
	private FileLocation root;
	private String DATA_DIR = "/data/";

	@Before
	public void setUp() throws Exception {
//		rtfFilter = new RTFFilter();		
		testFileList = RtfTestUtils.getTestFiles();
		root = FileLocation.fromClass(getClass());
	}

	@After
	public void tearDown() throws Exception {		
	}

	@Test
	public void testDoubleExtraction () {		
		//RoundTripComparison rtc = new RoundTripComparison();
		ArrayList<InputDocument> list = new ArrayList<>();
		for (String f : testFileList) {
			list.add(new InputDocument(root.in(DATA_DIR + f).toString(), null));
		}
//TODO: implement RTF Filter as a filter		assertTrue(rtc.executeCompare(rtfFilter, list, "UTF-8", "en", "en"));
	}
}
