package net.sf.okapi.lib.xliff2.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReaderException;
import net.sf.okapi.lib.xliff2.test.U;

@RunWith(JUnit4.class)
public class TermMarkerTest {

	@Test (expected = XLIFFException.class)
	public void testMissingAnnotatorRef () {
		TermTag tm = new TermTag("1");
		tm.setTermConfidence(0.5);
		tm.validate();
	}

	@Test (expected = InvalidParameterException.class)
	public void testInvalidType () {
		TermTag tm = new TermTag("1");
		tm.setType("abc");
	}

	@Test
	public void testValidType () {
		TermTag tm = new TermTag("1");
		tm.setType("its:term-no");
		assertEquals("its:term-no", tm.getType());
		tm.setType("term");
		assertEquals("term", tm.getType());
	}

	@Test (expected = InvalidParameterException.class)
	public void testInvalidConfidence () {
		TermTag tm = new TermTag("1");
		tm.setTermConfidence(1.234);
	}

	@Test
	public void testMTConfidenceCanNullifyConfidence () {
		TermTag tm = new TermTag("1");
		tm.setTermConfidence(0.5);
		tm.setTermConfidence(null);
		tm.validate();
	}
	
	@Test
	public void testNormalCreation () {
		TermTag tm = new TermTag("1");
		assertEquals("term", tm.getType());
	}
	
	@Test
	public void testTermInfoAndTermInfoRef () {
		TermTag tm = new TermTag("1");
		assertNull(tm.getTermInfo());
		assertNull(tm.getTermInfoRef());
		
		tm.setRef("tref");
		assertEquals("tref", tm.getTermInfoRef());
		assertEquals("tref", tm.getRef());
		assertNull(tm.getTermInfo());
		assertNull(tm.getValue());
		
		tm.setTermInfo("tinfo");
		assertNull(tm.getTermInfoRef());
		assertNull(tm.getRef());
		assertEquals("tinfo", tm.getTermInfo());
		assertEquals("tinfo", tm.getValue());

		tm.setTermInfoRef("trefagain");
		assertEquals("trefagain", tm.getTermInfoRef());
		assertEquals("trefagain", tm.getRef());
		assertNull(tm.getTermInfo());
		assertNull(tm.getValue());

		// setRef() and setValue() don not update value/ref to null
		tm.setValue("value");
		assertEquals("trefagain", tm.getTermInfoRef());
		assertEquals("trefagain", tm.getRef());
		assertEquals("value", tm.getTermInfo());
		assertEquals("value", tm.getValue());
		tm.setRef("ref");
		assertEquals("ref", tm.getTermInfoRef());
		assertEquals("ref", tm.getRef());
		assertEquals("value", tm.getTermInfo());
		assertEquals("value", tm.getValue());
	}
	
	@Test
	public void testNormalTermMarker () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"term\" value=\"val\" ref=\"ref\">source</mrk></source>\n"
			+ "</segment>\n</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertEquals(TermTag.TYPE_TERM, am.getType());
		assertEquals("val", am.getValue());
		assertEquals("ref", am.getRef());
		assertTrue(am instanceof TermTag);
		assertEquals(text, U.writeEvents(events));
	}

	@Test
	public void testITSTermMarker () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\" its:annotatorsRef=\"terminology|abc\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"term\" itsxlf:termConfidence=\"0.123\" value=\"val\" ref=\"ref\">source</mrk></source>\n"
			+ "</segment>\n</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertEquals(TermTag.TYPE_TERM, am.getType());
		assertEquals("val", am.getValue());
		assertEquals("ref", am.getRef());
		assertTrue(am instanceof TermTag);
		assertEquals(0.123, ((TermTag)am).getTermConfidence(), 0.0);
		assertEquals("abc", ((TermTag)am).getAnnotatorRef());
		// Add ITS specific metadata
		TermTag tm = (TermTag)am;
		tm.setTermConfidence(0.987);
		tm.setAnnotatorRef("acme");
		// Test output
		String expected= U.STARTDOCWITHITS
			+ "<file id=\"f1\" its:annotatorsRef=\"terminology|abc\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"term\" value=\"val\" ref=\"ref\" "
			+ "itsxlf:termConfidence=\"0.987\" its:annotatorsRef=\"terminology|acme\">source</mrk></source>\n"
			+ "</segment>\n</unit>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeEvents(events));
		U.getEvents(expected); // Validate
	}

	@Test (expected = XLIFFReaderException.class)
	public void testHalfTermMarker () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source>text and <sm id=\"1\" type=\"term\" value=\"val\" ref=\"ref\"/>term</source>\n</segment>\n"
			+ "<segment>\n<source> after.</source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		U.getEvents(text);
	}

	@Test (expected = XLIFFException.class)
	public void testLoneEndTermMarker () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source>text <em startRef=\"1\"/></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		U.getEvents(text);
	}

}
