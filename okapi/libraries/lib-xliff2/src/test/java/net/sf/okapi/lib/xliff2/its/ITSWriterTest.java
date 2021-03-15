package net.sf.okapi.lib.xliff2.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.test.U;

@RunWith(JUnit4.class)
public class ITSWriterTest {

	@Test
	public void testAnnotate () {
		Unit unit = new Unit("id");
		unit.appendSegment();
		Fragment frag = unit.getPart(0).getSource();
		frag.append("Some text");
		IITSItem item = ITSWriter.annotate(frag, 0, -1, new LocQualityIssue("comment"));
		assertNotNull(item);
		MTag am = (MTag)frag.getStore().getSourceTags().get(U.kOA(0));
		assertNotNull(am);
		IITSItem res = am.getITSItems().get(LocQualityIssue.class);
		assertNotNull(res);
		assertEquals("comment", ((LocQualityIssue)res).getComment());
		assertEquals("{oA}Some text{cA}", U.fmtMarkers(frag.getCodedText()));
	}

	@Test
	public void testAnnotateTwice () {
		Unit unit = new Unit("id");
		unit.appendSegment().setSource("some text");
		Fragment frag = unit.getPart(0).getTarget(GetTarget.CLONE_SOURCE);
		Provenance prov = new Provenance();
		prov.setPerson("John Dow");
		prov.setRevPerson("John Smith");
		ITSWriter.annotate(frag, 0, -1, prov);
		ITSWriter.annotate(frag, 0, -1, new LocQualityIssue("bad spelling"), "its:any");
		String xliff = frag.toXLIFF();
		assertTrue(xliff.contains("type=\"its:any\""));
		assertTrue(xliff.contains("its:locQualityIssueComment=\"bad spelling\""));
		assertTrue(xliff.contains("its:person=\"John Dow\""));
		assertTrue(xliff.contains("its:revPerson=\"John Smith\""));
		assertTrue(xliff.contains(">some text</"));
	}

	@Test
	public void testAnnotateOverlapping () {
		Unit unit = new Unit("id");
		unit.appendSegment().setSource("some shared text");
		Fragment frag = unit.getPart(0).getTarget(GetTarget.CLONE_SOURCE);
		ITSWriter.annotate(frag, 0, 11, new LocQualityIssue("Issue 1"));
		ITSWriter.annotate(frag, 7, -1, new LocQualityIssue("Issue 2"), "its:any");
		assertEquals("<sm id=\"1\" type=\"its:any\" its:locQualityIssueComment=\"Issue 1\"/>some "
			+ "<sm id=\"2\" type=\"its:any\" its:locQualityIssueComment=\"Issue 2\"/>shared<em startRef=\"1\"/>"
			+ " text<em startRef=\"2\"/>", frag.toXLIFF());
	}

	@Test
	public void testAnnotateAll () {
		Unit unit = new Unit("id");
		unit.appendSegment();
		Fragment frag = unit.getPart(0).getSource();
		frag.append("Some text");
		for ( Segment seg : unit.getSegments() ) {
			seg.setTarget("DU TEXTE");
			ITSWriter.annotate(seg.getTarget(), 0, -1, new MTConfidence("myAnnotator", 0.5), "its:any");
			assertEquals("{oA}DU TEXTE{cA}", U.fmtMarkers(seg.getTarget().getCodedText()));
		}
	}
	
	@Test
	public void testWriteVariousDC () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\" its:annotatorsRef=\"mt-confidence|myTool\" itsxlf:domains=\"dom1\">\n"
			+ "<segment>\n<source>source</source>\n"
			+ "<target><mrk id=\"1\" type=\"its:any\" its:mtConfidence=\"0.321\">source</mrk></target>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		Domain dom = (Domain)unit.getITSItems().get(Domain.class);
		assertEquals("dom1", dom.getDomain());
		dom.setDomain("newDom");

		MTag am = (MTag)unit.getPart(0).getTargetTags().get(U.kOA(0));
		assertEquals(0.321, ((MTConfidence)am.getITSItems().get(MTConfidence.class)).getMtConfidence(), 0.0);
		
		String expected = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\" itsxlf:domains=\"newDom\" its:annotatorsRef=\"mt-confidence|myTool\">\n"
			+ "<segment>\n<source>source</source>\n"
			+ "<target><mrk id=\"1\" type=\"its:any\" its:mtConfidence=\"0.321\">source</mrk></target>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeEvents(events));
	}

}
