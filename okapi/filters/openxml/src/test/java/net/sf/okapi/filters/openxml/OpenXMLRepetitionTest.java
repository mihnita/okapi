package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Miscellaneous OOXML tests.
 */
@RunWith(JUnit4.class)
public class OpenXMLRepetitionTest {
	private LocaleId locENUS = LocaleId.fromString("en-us");
	// if it works 15 times then we are good :-)
	private final int HOMINY = 15;
	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

	/**
	 * Test to ensure that the filter can open and close multiple times
	 * @throws Exception
	 */
	@Test
	public void testRepetition() throws Exception {
		OpenXMLFilter filter = new OpenXMLFilter();
		RawDocument doc = new RawDocument(root.in("/HelloWorld.docx").asUri(), "UTF-8", locENUS);
		for(int i=0;i<HOMINY;i++)
			doit(filter, doc);
	}
		
	private void doit(OpenXMLFilter filter, RawDocument doc) {
		try {
            filter.open(doc);
            while (filter.hasNext()) {
                filter.next();
            }
        } finally {
            filter.close();
        }
    }
}
