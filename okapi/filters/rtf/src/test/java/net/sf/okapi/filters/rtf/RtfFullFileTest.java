package net.sf.okapi.filters.rtf;

import java.io.InputStream;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RtfFullFileTest {

	private RTFFilter filter;
	private String[] testFileList;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	private FileLocation root;

	@Before
	public void setUp() throws Exception {
		filter = new RTFFilter();		
		testFileList = RtfTestUtils.getTestFiles();
		root = FileLocation.fromClass(getClass());
	}

	@After
	public void tearDown() {
		filter.close();
	}

	@Test
	public void testAllExternalFiles() {
		@SuppressWarnings("unused")
		Event event = null;

		for (String f : testFileList) {		
			InputStream stream = root.in("/" + f).asInputStream();
			filter.open(new RawDocument(stream, "windows-1252", locEN, locFR));
			while (filter.hasNext()) {
				event = filter.next();
			}
		}
	}	
}
