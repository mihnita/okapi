package net.sf.okapi.lib.xliff2.its;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MTConfidenceTest {

	@Test (expected = XLIFFException.class)
	public void testMTConfidenceMissingAnnotatorRef () {
		// An annotator reference must be set if mtConfidence is defined
		MTConfidence mtc = new MTConfidence();
		mtc.setMtConfidence(0.5);
		mtc.validate();
	}

	@Test (expected = InvalidParameterException.class)
	public void testMTConfidenceInvalidConfidence () {
		// Bad confidence value
		MTConfidence mtc = new MTConfidence();
		mtc.setMtConfidence(1.234);
	}

	@Test
	public void testMTConfidenceCanNullifyConfidence () {
		// Nullifying confidence is OK
		MTConfidence mtc = new MTConfidence();
		mtc.setMtConfidence(0.5);
		mtc.setMtConfidence(null);
	}
	
	@Test
	public void testNormalCreation () {
		MTConfidence mtc = new MTConfidence("anno", 0.5);
		assertEquals("anno", mtc.getAnnotatorRef());
		assertEquals(0.5, mtc.getMtConfidence(), 0.0);
	}
	
	@Test
	public void testMTConfidence () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\" its:annotatorsRef=\"mt-confidence|myTool\">\n"
			+ "<segment>\n<source>source</source>\n"
			+ "<target><mrk id=\"1\" type=\"its:any\" its:mtConfidence=\"0.321\">source</mrk></target>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getTargetTags().get(U.kOA(0));
		assertEquals(0.321, ((MTConfidence)am.getITSItems().get(MTConfidence.class)).getMtConfidence(), 0.0);
		assertEquals(text, U.writeEvents(events));
	}

}
