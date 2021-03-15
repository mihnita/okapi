package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.core.Note.AppliesTo;
import net.sf.okapi.lib.xliff2.matches.Match;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.reader.XLIFFReaderException;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnitTest {

	@Test
	public void testDefaults () {
		Unit unit = new Unit("id1");
		assertEquals("id1", unit.getId());
		assertTrue(unit.getCanResegment());
		assertTrue(unit.getTranslate());
		assertEquals(Directionality.AUTO, unit.getSourceDir());
		assertEquals(Directionality.AUTO, unit.getTargetDir());
		assertTrue(unit.getExtAttributes().isEmpty());
		assertTrue(unit.getExtElements().isEmpty());
		assertEquals(0, unit.getNoteCount());
		// This object has no parts initially
		assertEquals(0, unit.getPartCount());
		assertNull(unit.getType());
		assertNull(unit.getName());
	}

	@Test
	public void testGetPartAndSegment () {
		Unit unit = new Unit("u1");
		unit.appendSegment().getSource().append("p0s0");
		unit.appendIgnorable().getSource().append("p1");
		unit.appendIgnorable().getSource().append("p2");
		unit.appendSegment().getSource().append("p3s1");
		unit.appendSegment().getSource().append("p4s2");
		unit.appendIgnorable().getSource().append("p5");
		unit.appendSegment().getSource().append("p6s3");
		assertEquals(7, unit.getPartCount());
		assertEquals(4, unit.getSegmentCount());
		assertEquals("p0s0", unit.getPart(0).getSource().toString());
		assertEquals("p2", unit.getPart(2).getSource().toString());
		assertEquals("p0s0", unit.getSegment(0).getSource().toString());
		assertEquals("p6s3", unit.getPart(6).getSource().toString());
		assertEquals("p6s3", unit.getSegment(3).getSource().toString());
		assertEquals("p3s1", unit.getSegment(1).getSource().toString());
		assertEquals("p4s2", unit.getSegment(2).getSource().toString());
	}
	
	@Test
	public void testTargetOrderedList () {
		Unit unit = new Unit("u1");
		Segment seg = unit.appendSegment();
		seg.setSource("seg1-s1");
		seg.setTarget("seg1-t7");
		seg.setTargetOrder(7);
		unit.appendIgnorable().setSource(" .2. ");
		seg = unit.appendSegment();
		seg.setSource("seg2-s3");
		seg.setTarget("seg2-t1");
		seg.setTargetOrder(1);
		unit.appendIgnorable().setSource(" .4. ");
		seg = unit.appendSegment();
		seg.setSource("seg3-s5");
		seg.setTarget("seg3-t5");
		// Use default seg.setTargetOrder(5);
		unit.appendIgnorable().setSource(" .6. ");
		seg = unit.appendSegment();
		seg.setSource("seg4-s7");
		seg.setTarget("seg4-t3");
		seg.setTargetOrder(3);
		
		StringBuilder src = new StringBuilder();
		StringBuilder trg = new StringBuilder();
		for ( Part part : unit.getTargetOrderedParts() ) {
			src.append(part.getSource().toString());
			trg.append(U.getTargetOrSource(part).toString());
		}
		assertEquals("seg2-s3 .2. seg4-s7 .4. seg3-s5 .6. seg1-s1", src.toString());
		assertEquals("seg2-t1 .2. seg4-t3 .4. seg3-t5 .6. seg1-t7", trg.toString());
	}
	
	@Test
	public void testInlineTranslate1 () {
		Unit unit = new Unit("u1");
		Segment seg = unit.appendSegment();
		seg.setSource("toTrans");
		Part part = unit.appendIgnorable();
		MTag am = part.getSource().openMarkerSpan("m1", null);
		am.setTranslate(false);
		unit.appendSegment().setSource("noTrans");
		part = unit.appendIgnorable();
		part.getSource().closeMarkerSpan("m1");
		
		List<Boolean> res = unit.getTranslateStateEndings(true);
		assertEquals(true, res.get(0)); // at the end of segment 1
		assertEquals(false, res.get(1)); // at the end of ignorable 1
		assertEquals(false, res.get(2)); // at the end of segment 2
		assertEquals(true, res.get(3)); // at the end of ignorable 2
	}

	@Test
	public void testProtectedCodedText () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1'>"
			+ "<source>tt <mrk id='m1' translate='no'>nt</mrk> tt.</source>"
			+ "<target>TT <mrk id='m1' translate='no'>nt</mrk> TT.</target>"
			+ "</segment>"
			+ "<ignorable>"
			+ "<source> <sm id='m2' translate='no'/></source>"
			+ "<target> <sm id='m2' translate='no'/></target>"
			+ "</ignorable>"
			+ "<segment id='s2'>"
			+ "<source>nt 2 <mrk id='m3' translate='yes'>tt</mrk> nt.</source>"
			+ "<target>nt 2 <mrk id='m3' translate='yes'>TT</mrk> nt.</target>"
			+ "</segment>"
			+ "<ignorable>"
			+ "<source> <sm id='m4' translate='yes'/></source>"
			+ "<target> <sm id='m4' translate='yes'/></target>"
			+ "</ignorable>"
			+ "<segment id='s3'>"
			+ "<source>tt 3.</source>"
			+ "<target>TT 3.</target>"
			+ "</segment>"
			+ "<ignorable>"
			+ "<source><em startRef='m4'/> </source>"
			+ "<target><em startRef='m4'/> </target>"
			+ "</ignorable>"
			+ "<segment id='s4'>"
			+ "<source>nt 4.</source>"
			+ "<target>nt 4.</target>"
			+ "</segment>"
			+ "<ignorable>"
			+ "<source><em startRef='m2'/> </source>"
			+ "<target><em startRef='m2'/> </target>"
			+ "</ignorable>"
			+ "</unit></file></xliff>";
		
		Unit unit = getUnit(snippet, 1);
		unit.hideProtectedContent();
		// Segment s1, part index 0
		Part part = unit.getPart(0);
		assertTrue(part.isSegment());
		//assertEquals(true, pct.getSourceEndingTranslate());
		assertEquals("{oA}nt{cA}", U.fmtMarkers(part.getSourceTags().getPCont(U.kSP(0)).getCodedText()));
		assertEquals("tt {$} tt.", U.fmtMarkers(part.getSource().getCodedText()));
		// Part index 1
		part = unit.getPart(1);
		assertFalse(part.isSegment());
		//assertEquals(false, pct.getSourceEndingTranslate());
		assertEquals("{oA}", U.fmtMarkers(part.getSourceTags().getPCont(U.kSP(1)).getCodedText()));
		assertEquals(" {$}", U.fmtMarkers(part.getSource().getCodedText()));
		// Segment s2, part index 2
		part = unit.getPart(2);
		//assertEquals(false, pct.getSourceEndingTranslate());
		assertEquals("nt 2 ", part.getSourceTags().getPCont(U.kSP(2)).getCodedText());
		assertEquals(" nt.", part.getSourceTags().getPCont(U.kSP(3)).getCodedText());
		assertEquals("{$}{oA}tt{cA}{$}", U.fmtMarkers(part.getSource().getCodedText()));
		// Part index 3
		part = unit.getPart(3);
		//assertEquals(true, pct.getSourceEndingTranslate());
		assertEquals(" ", part.getSourceTags().getPCont(U.kSP(4)).getCodedText());
		assertEquals("{$}{oA}", U.fmtMarkers(part.getSource().getCodedText()));
		// Segment s3, part index 4
		part = unit.getPart(4);
		//assertEquals(true, pct.getSourceEndingTranslate());
		assertEquals("tt 3.", U.fmtMarkers(part.getSource().getCodedText()));
		// Part index 5
		part = unit.getPart(5);
		//assertEquals(false, pct.getSourceEndingTranslate());
		assertEquals(" ", part.getSourceTags().getPCont(U.kSP(5)).getCodedText());
		assertEquals("{cA}{$}", U.fmtMarkers(part.getSource().getCodedText()));
		// Segment s4, part index 6
		part = unit.getPart(6);
		//assertEquals(false, pct.getSourceEndingTranslate());
		assertEquals("nt 4.", part.getSourceTags().getPCont(U.kSP(6)).getCodedText());
		assertEquals("{$}", U.fmtMarkers(part.getSource().getCodedText()));
		// Part index 7
		part = unit.getPart(7);
		//assertEquals(true, pct.getSourceEndingTranslate());
		assertEquals("{cA}", U.fmtMarkers(part.getSourceTags().getPCont(U.kSP(7)).getCodedText()));
		assertEquals("{$} ", U.fmtMarkers(part.getSource().getCodedText()));
		
		// Make changes to the translatable text
		pseudoTranslateSource(unit);
		// Expand back
		unit.showProtectedContent();
		// Check the result after expanding back
		assertEquals("ZZZ{oA}nt{cA}ZZZZ", U.fmtMarkers(unit.getPart(0).getSource().getCodedText()));
		assertEquals("Z{oA}", U.fmtMarkers(unit.getPart(1).getSource().getCodedText()));
		assertEquals("nt 2 {oA}ZZ{cA} nt.", U.fmtMarkers(unit.getPart(2).getSource().getCodedText()));
		assertEquals(" {oA}", U.fmtMarkers(unit.getPart(3).getSource().getCodedText()));
		assertEquals("ZZZZZ", U.fmtMarkers(unit.getPart(4).getSource().getCodedText()));
		assertEquals("{cA} ", U.fmtMarkers(unit.getPart(5).getSource().getCodedText()));
		assertEquals("nt 4.", U.fmtMarkers(unit.getPart(6).getSource().getCodedText()));
		assertEquals("{cA}Z", U.fmtMarkers(unit.getPart(7).getSource().getCodedText()));
	}
	
	@Test
	public void testInlineTranslate2 () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1' translate='no'>"
			+ "<segment id='s1'>"
			+ "<source>t1 <sm id='1' translate='yes'/>t2 <sm id='2' translate='no'/></source>"
			+ "</segment>"
			+ "<segment id='s2'>"
			+ "<source>t3 <em startRef='2'/>t4 </source>"
			+ "</segment>"
			+ "<segment id='s3'>"
			+ "<source><em startRef='1'/>t5</source>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		
		Unit unit = getUnit(snippet, 1);
		unit.hideProtectedContent();
		assertEquals("{$}{oA}t2 {$}", U.fmtMarkers(unit.getPart(0).getSource().toString()));
		assertEquals("{$}t4 ", U.fmtMarkers(unit.getPart(1).getSource().toString()));
		assertEquals("{cA}{$}", U.fmtMarkers(unit.getPart(2).getSource().toString()));
		//assertEquals("{$}{oA}t2 {$}", U.fmtMarkers(unit.getPart(0).getTarget().toString()));
		//assertEquals("{$}t4 ", U.fmtMarkers(unit.getPart(1).getTarget().toString()));
		//assertEquals("{cA}{$}", U.fmtMarkers(unit.getPart(2).getTarget().toString()));
		assertNull(unit.getPart(0).getTarget());
		assertNull(unit.getPart(1).getTarget());
		assertNull(unit.getPart(2).getTarget());
		pseudoTranslateSource(unit);
		
		unit.showProtectedContent();
		assertEquals("t1 {oA}ZZZ{oA}", U.fmtMarkers(unit.getPart(0).getSource().toString()));
		assertEquals("t3 {cA}ZZZ", U.fmtMarkers(unit.getPart(1).getSource().toString()));
		//TODO: Fix this: assertEquals("{cA}t5", Utilities.fmtMarkers(unit.getPart(2).getSource().toString()));
//		assertEquals("t1 {oA}t2 {oA}", Utilities.fmtMarkers(unit.getPart(0).getTarget().toString()));
//		assertEquals("t3 {cA}t4 ", Utilities.fmtMarkers(unit.getPart(1).getTarget().toString()));
//		assertEquals("{cA}t5", Utilities.fmtMarkers(unit.getPart(2).getTarget().toString()));
	}	

	@Test
	public void testSplit1 () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1'>"
			+ "<source>source1. source2.</source>"
			+ "<target>translation1. translation2.</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		
		// Test simple split
		unit.split(0, 9, 9, 14, 14, true);
		assertEquals(2, unit.getPartCount());
		assertEquals("source1. ", unit.getPart(0).getSource().toString());
		assertEquals("source2.", unit.getPart(1).getSource().toString());
		assertEquals("translation1. ", unit.getPart(0).getTarget().toString());
		assertEquals("translation2.", unit.getPart(1).getTarget().toString());
		assertEquals("s1", unit.getPart(0).getId());
		assertEquals("s2", unit.getPart(1).getId());

		// Test split with empty source
		// source = "source2."
		// target = "translation2."
		unit.split(1, 0, 0, 0, 5, true);
		assertEquals(3, unit.getPartCount());
		assertEquals("trans", unit.getPart(1).getTarget().toString());
		assertEquals("lation2.", unit.getPart(2).getTarget().toString());
		assertEquals("source2.", unit.getPart(1).getSource().toString());
		assertEquals("", unit.getPart(2).getSource().toString());
		assertEquals("s1", unit.getPart(0).getId());
		assertEquals("s2", unit.getPart(1).getId());
		assertEquals("s3", unit.getPart(2).getId());

		// Test no split on each side
		// source = "source2."
		// target = "trans"
		unit.split(1, 8, 8, 0, 0, true);
		// Same as before: nothing has changed
		assertEquals(3, unit.getPartCount());
		assertEquals("trans", unit.getPart(1).getTarget().toString());
		assertEquals("lation2.", unit.getPart(2).getTarget().toString());
		assertEquals("source2.", unit.getPart(1).getSource().toString());
		assertEquals("", unit.getPart(2).getSource().toString());
		assertEquals("s1", unit.getPart(0).getId());
		assertEquals("s2", unit.getPart(1).getId());
		assertEquals("s3", unit.getPart(2).getId());
		
		// Test with middle part
		// source = "source1. "
		// target = "translation1. "
		unit.split(0, 7, 9, 5, 8, true); // target: "lat"=middle
		// Same as before: nothing has changed
		assertEquals(5, unit.getPartCount());
		assertEquals("source1", unit.getPart(0).getSource().toString());
		assertEquals("trans", unit.getPart(0).getTarget().toString());
		assertEquals(". ", unit.getPart(1).getSource().toString());
		assertEquals("lat", unit.getPart(1).getTarget().toString());
		assertEquals("", unit.getPart(2).getSource().toString());
		assertEquals("ion1. ", unit.getPart(2).getTarget().toString());
		assertEquals("source2.", unit.getPart(3).getSource().toString());
		assertEquals("trans", unit.getPart(3).getTarget().toString());
		assertEquals("", unit.getPart(4).getSource().toString());
		assertEquals("lation2.", unit.getPart(4).getTarget().toString());
		assertEquals("s1", unit.getPart(0).getId());
		assertEquals("s4", unit.getPart(1).getId());
		assertEquals("s5", unit.getPart(2).getId());
		assertEquals("s2", unit.getPart(3).getId());
		assertEquals("s3", unit.getPart(4).getId());
	}
	
	@Test
	public void testSplit2 () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1' state='translated' subState='my:info'>"
			+ "<source xml:lang='en' xml:space='preserve'>source1.  source2.</source>"
			+ "<target xml:lang='fr' xml:space='preserve'>translation1.  translation2.</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		
		// Test simple split
		unit.split(0, 8, 10, 13, 15, true);
		
		assertEquals(3, unit.getPartCount());
		Segment seg = (Segment)unit.getPart(0);
		assertEquals("source1.", seg.getSource().toString());
		assertEquals("translation1.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.TRANSLATED, seg.getState());
		assertEquals("my:info", seg.getSubState());

		Part part = unit.getPart(1);
		assertEquals("  ", part.getSource().toString());
		assertEquals("  ", part.getTarget().toString());
		assertTrue(part.getPreserveWS());
		
		seg = (Segment)unit.getPart(2);
		assertEquals("source2.", seg.getSource().toString());
		assertEquals("translation2.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.TRANSLATED, seg.getState());
		assertEquals("my:info", seg.getSubState());
	}
	
	@Test
	public void testSplit3 () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1' state='reviewed' subState='my:info'>"
			+ "<source xml:lang='en' xml:space='preserve'>source1.  source2.</source>"
			+ "<target xml:lang='fr' xml:space='preserve'>translation1.  translation2.</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		
		// Test simple split
		unit.split(0, 8, 10, 13, 15, true);
		
		assertEquals(3, unit.getPartCount());
		Segment seg = (Segment)unit.getPart(0);
		assertEquals("source1.", seg.getSource().toString());
		assertEquals("translation1.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.TRANSLATED, seg.getState());
		assertNull(seg.getSubState());

		Part part = unit.getPart(1);
		assertEquals("  ", part.getSource().toString());
		assertEquals("  ", part.getTarget().toString());
		assertTrue(part.getPreserveWS());
		
		seg = (Segment)unit.getPart(2);
		assertEquals("source2.", seg.getSource().toString());
		assertEquals("translation2.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.TRANSLATED, seg.getState());
		assertNull(seg.getSubState());
	}
	
	@Test
	public void testSplit4 () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1' state='reviewed' subState='my:info'>"
			+ "<source xml:lang='en' xml:space='preserve'>source1.  source2.</source>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		
		// Test simple split
		unit.split(0, 8, 10, -1, -1, true);
		
		assertEquals(3, unit.getPartCount());
		Segment seg = (Segment)unit.getPart(0);
		assertEquals("source1.", seg.getSource().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.REVIEWED, seg.getState()); // No change because no target
		assertEquals("my:info", seg.getSubState()); // No change because no target

		Part part = unit.getPart(1);
		assertEquals("  ", part.getSource().toString());
		assertTrue(part.getPreserveWS());
		
		seg = (Segment)unit.getPart(2);
		assertEquals("source2.", seg.getSource().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.REVIEWED, seg.getState()); // No change because no target
		assertEquals("my:info", seg.getSubState()); // No change because no target
	}
	
	@Test
	public void testSplit5 () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1' state='reviewed' subState='my:info'>"
			+ "<source xml:lang='en' xml:space='preserve'>source1.  source2.</source>"
			+ "<target xml:lang='fr' xml:space='preserve'>translation1.  translation2.</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		
		// Test simple split
		unit.split(0, 8, 10, 13, 15, false); // No state/subState change requested
		
		assertEquals(3, unit.getPartCount());
		Segment seg = (Segment)unit.getPart(0);
		assertEquals("source1.", seg.getSource().toString());
		assertEquals("translation1.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.REVIEWED, seg.getState());
		assertEquals("my:info", seg.getSubState());

		Part part = unit.getPart(1);
		assertEquals("  ", part.getSource().toString());
		assertEquals("  ", part.getTarget().toString());
		assertTrue(part.getPreserveWS());
		
		seg = (Segment)unit.getPart(2);
		assertEquals("source2.", seg.getSource().toString());
		assertEquals("translation2.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.REVIEWED, seg.getState());
		assertEquals("my:info", seg.getSubState());
	}
	
	@Test
	public void testSplit6 () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1' state='reviewed' subState='my:info'>"
			+ "<source xml:space='preserve'>source1.  source2.</source>"
			+ "<target xml:space='preserve'>trans1.  trans2.</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		
		// Test simple split using -1
		unit.split(0, 8, -1, 7, -1, false); // No state/subState change requested
		
		assertEquals(2, unit.getPartCount());
		Segment seg = (Segment)unit.getPart(0);
		assertEquals("source1.", seg.getSource().toString());
		assertEquals("trans1.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.REVIEWED, seg.getState());
		assertEquals("my:info", seg.getSubState());

		seg = (Segment)unit.getPart(1);
		assertEquals("  source2.", seg.getSource().toString());
		assertEquals("  trans2.", seg.getTarget().toString());
		assertTrue(seg.getPreserveWS());
		assertEquals(TargetState.REVIEWED, seg.getState());
		assertEquals("my:info", seg.getSubState());
	}
	
	@Test
	public void testSplitWithTargetOrder () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1'>"
			+ "<source>s1. s2.</source>"
			+ "<target order='4'>t1-5. t2-6.</target>"
			+ "</segment>"
			+ "<ignorable>"
			+ "<source> </source>"
			+ "<target></target>"
			+ "</ignorable>"
			+ "<segment id='s2'>"
			+ "<source>s3. s4. </source>"
			+ "<target order='1'>t3-1. t4-2. </target>"
			+ "</segment>"
			+ "<segment id='s3'>"
			+ "<source>s5. s6.</source>"
			+ "<target order='3'>t5-3. t6-4. </target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		// Check before splitting
		assertEquals("s1. s2. s3. s4. s5. s6.", getAllText(unit, false));
		assertEquals("t3-1. t4-2. t5-3. t6-4. t1-5. t2-6.", getAllText(unit, true));
		// Split
		unit.split(0, 4, 4, 6, 6, true);
		assertEquals("s1. ", unit.getPart(0).getSource().getCodedText());
		assertEquals("t1-5. ", unit.getPart(0).getTarget().getCodedText());
		unit.split(3, 4, 4, 6, 6, true);
		assertEquals("s4. ", unit.getPart(4).getSource().getCodedText());
		assertEquals("t4-2. ", unit.getPart(4).getTarget().getCodedText());
		unit.split(5, 4, 4, 6, 6, true);
		assertEquals("s6.", unit.getPart(6).getSource().getCodedText());
		assertEquals("t6-4. ", unit.getPart(6).getTarget().getCodedText());
		// check after
		assertEquals("s1. s2. s3. s4. s5. s6.", getAllText(unit, false));
		assertEquals("t3-1. t4-2. t5-3. t6-4. t1-5. t2-6.", getAllText(unit, true));
	}
	
	@Test
	public void testSplitWithSourceTargetInfo () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment id='s1'>"
			+ "<source xml:space='preserve'>s1. s2.</source>"
			+ "<target xml:space='preserve'>t1. t2.</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(snippet, 1);
		// Split
		unit.split(0, 4, 4, 4, 4, true);
		assertEquals("s1. ", unit.getPart(0).getSource().getCodedText());
		assertEquals("t1. ", unit.getPart(0).getTarget().getCodedText());
		assertTrue(unit.getPart(1).getPreserveWS());
		assertEquals(Directionality.INHERITED, unit.getPart(1).getSource().getDir(false));
		assertEquals(Directionality.INHERITED, unit.getPart(1).getTarget().getDir(false));
	}

	@Test
	public void testJoin1 () {
		String snippet = "<?xml version='1.0'?>"
				+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
				+ "<file id=\"f1\"><unit id='1'>"
				+ "<segment id='s1'>"
				+ "<source>s1. </source>"
				+ "<target>t1. </target>"
				+ "</segment>"
				+ "<segment id='s2'>"
				+ "<source xml:space='preserve'>s2.</source>"
				+ "<target xml:space='preserve'>t2.</target>"
				+ "</segment>"
				+ "</unit></file></xliff>";
			Unit unit = getUnit(snippet, 1);
			unit.join(0, 1, true, true);
			assertEquals("s1. s2.", unit.getPart(0).getSource().getCodedText());
			assertEquals("t1. t2.", unit.getPart(0).getTarget().getCodedText());
		assertTrue(unit.getPart(0).getPreserveWS());
			unit = getUnit(snippet, 1);

			unit.join(0, -1, false, false);
			assertEquals(1, unit.getPartCount());
			assertEquals("s1. s2.", unit.getPart(0).getSource().getCodedText());
	}
	
	@Test
	public void testJoin2 () {
		String snippet = "<?xml version='1.0'?>"
				+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
				+ "<file id=\"f1\"><unit id='1'>"
				+ "<segment id='s1'>"
				+ "<source>s1. </source>"
				+ "<target>t1. </target>"
				+ "</segment>"
				+ "<segment id='s2'>"
				+ "<source>s2. </source>"
				+ "<target>t2. </target>"
				+ "</segment>"
				+ "<segment id='s3' canResegment='no'>"
				+ "<source>s3. </source>"
				+ "<target>t3. </target>"
				+ "</segment>"
				+ "<segment id='s4'>"
				+ "<source>s4. </source>"
				+ "<target>t4. </target>"
				+ "</segment>"
				+ "<segment id='s5'>"
				+ "<source>s5.</source>"
				+ "<target>t5.</target>"
				+ "</segment>"
				+ "</unit></file></xliff>";
			Unit unit = getUnit(snippet, 1);
			unit.joinAll(true);
			assertEquals(3, unit.getPartCount());
			assertEquals("s1. s2. ", unit.getPart(0).getSource().getCodedText());
			assertEquals("s3. ", unit.getPart(1).getSource().getCodedText());
			assertEquals("s4. s5.", unit.getPart(2).getSource().getCodedText());

			unit = getUnit(snippet, 1);
			unit.join(0, -1, false, false);
			assertEquals(1, unit.getPartCount());
			assertEquals("s1. s2. s3. s4. s5.", unit.getPart(0).getSource().getCodedText());
	}
	
	@Test
	public void testJoin3 () {
		String snippet = "<?xml version='1.0'?>"
				+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
				+ "<file id=\"f1\"><unit id='1'>"
				+ "<segment id='s1' canResegment='no'>"
				+ "<source>s1. </source>"
				+ "<target>t1. </target>"
				+ "</segment>"
				+ "<segment id='s2'>"
				+ "<source>s2. </source>"
				+ "<target>t2. </target>"
				+ "</segment>"
				+ "<segment id='s3'>"
				+ "<source>s3. </source>"
				+ "<target>t3. </target>"
				+ "</segment>"
				+ "<segment id='s4' canResegment='no'>"
				+ "<source>s4. </source>"
				+ "<target>t4. </target>"
				+ "</segment>"
				+ "<segment id='s5'>"
				+ "<source>s5. </source>"
				+ "<target>t5. </target>"
				+ "</segment>"
				+ "<segment id='s6' canResegment='no'>"
				+ "<source>s6.</source>"
				+ "<target>t6.</target>"
				+ "</segment>"
				+ "</unit></file></xliff>";
			Unit unit = getUnit(snippet, 1);
			unit.joinAll(true);
			assertEquals(5, unit.getPartCount());
			assertEquals("s1. ", unit.getPart(0).getSource().getCodedText());
			assertEquals("s2. s3. ", unit.getPart(1).getSource().getCodedText());
			assertEquals("s4. ", unit.getPart(2).getSource().getCodedText());
			assertEquals("s5. ", unit.getPart(3).getSource().getCodedText());
			assertEquals("s6.", unit.getPart(4).getSource().getCodedText());
			
			unit = getUnit(snippet, 1);
			unit.join(0, -1, false, false);
			assertEquals(1, unit.getPartCount());
			assertEquals("s1. s2. s3. s4. s5. s6.", unit.getPart(0).getSource().getCodedText());
	}

//	@Test
//	public void testChangeOfMarkers () {
//		Unit unit = new Unit("1");
//		Segment seg1 = unit.appendNewSegment();
//		Fragment frag1 = seg1.getSource();
//		frag1.append(TagType.OPENING, "1", "<b>", false);
//		Segment seg2 = unit.appendNewSegment();
//		Fragment frag2 = seg2.getSource();
//		frag2.append(TagType.CLOSING, "1", "</b>", false);
//		assertEquals("<sc id=\"1\" canOverlap=\"no\"/>", frag1.toXLIFF(null, false));
//		assertEquals("<ec startRef=\"1\" canOverlap=\"no\"/>", frag2.toXLIFF(null, false));
//
//		Markers newM = new Markers(seg2.getStore());
//		newM.add(new CMarker(TagType.OPENING, "1", "<b>"));
//		newM.add(new CMarker(TagType.CLOSING, "1", "</b>"));
//		
//		seg1.getTarget(GetTarget.CLONE_SOURCE);
//		
//		Fragment tf = seg2.getTarget(GetTarget.CLONE_SOURCE);
//		tf.setCodedText(tf.getCodedText());
	//Not easy to do with new coded text
//		tf.getStore().getTargetMarkers().reset(newM);
//
//		assertEquals("<sc id=\"1\" canOverlap=\"no\"/>", seg1.getTarget().toXLIFF(null, false));
//		assertEquals("<ec startRef=\"1\" canOverlap=\"no\"/>", seg2.getTarget().toXLIFF(null, false));
//		
//	}
	
	@Test
	public void testNonRemovableVerification () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment>"
			+ "<source><ph id='1'/><pc id='2' canDelete='no'>text</pc> </source>"
			+ "<target><pc id='2' canDelete='no'>text</pc> <ph id='3' canDelete='no'/></target>"
			+ "</segment>"
			+ "<segment>"
			+ "<source><ph id='3' canDelete='no'/>text </source>"
			+ "<target>text. </target>"
			+ "</segment>"
			+ "<segment>"
			+ "<source><ph id='4' canDelete='no'/>text </source>"
			+ "</segment>" // No target for this segment -> OK to not have the code
			+ "</unit></file></xliff>";
		assertTrue(getUnit(snippet, 1) != null);
	}
	
	@Test (expected=XLIFFException.class)
	public void testNonRemovableVerificationWithError () {
		String snippet = "<?xml version='1.0'?>"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='1'>"
			+ "<segment>"
			+ "<source><ph id='1'/><pc id='2'>text</pc> </source>"
			+ "<target><pc id='2' canDelete='no'>text</pc> </target>"
			+ "</segment>"
			+ "<segment>" // ph id='3' is missing
			+ "<source><ph id='3' canDelete='no'/>text </source>"
			+ "<target>text. </target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		assertTrue(getUnit(snippet, 1) != null);
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testInvalidTypeValue () {
		Unit unit = new Unit("id");
		unit.setType("badValue");
	}
	
	@Test
	public void testAttributes () {
		Unit unit = new Unit("id");
		unit.setType("good:value");
		assertEquals("good:value", unit.getType());
		unit.setId("newId");
		assertEquals("newId", unit.getId());
		unit.setCanResegment(false);
		assertFalse(unit.getCanResegment());
		unit.setName("name");
		assertEquals("name", unit.getName());
		unit.setTranslate(false);
		assertFalse(unit.getTranslate());
	}

	@Test
	public void testCopyConstructor () {
		Unit u1 = new Unit("u1");
		u1.setCanResegment(false);
		Note note = u1.getNotes().add(new Note("note1", AppliesTo.TARGET));
		note.setId("n1");
		note.setCategory("cat");
		note.setPriority(5);
		note.getExtAttributes().setAttribute("ns", "attr", "valnote");
		u1.setSourceDir(Directionality.RTL);
		u1.setTargetDir(Directionality.RTL);
		u1.setTranslate(false);
		u1.setType("x:utype");
		Segment seg = u1.appendSegment();
		seg.setCanResegment(false);
		seg.setId("s1");
		seg.setPreserveWS(true);
		seg.setState("translated");
		seg.setSubState("x:mine");
		Fragment frag = seg.getSource();
		frag.setDir(Directionality.RTL);
		frag.append("src");
		frag.append(TagType.OPENING, "1", "[c1]", true);
		frag.appendCode("2", "[c2/]");
		frag.append(TagType.CLOSING, "1", "[/c1]", true);

		// Clone and compare
		Unit u2 = new Unit(u1);
		assertEquals(u1.getAnnotatorsRef(), u2.getAnnotatorsRef());
		assertEquals(u1.getCanResegment(), u2.getCanResegment());
		assertTrue(sameExtAttributes(u1, u2));
		assertEquals(u1.getId(), u2.getId());
		assertEquals(u1.getName(), u2.getName());
		
		assertTrue(sameNotes(u1, u2));
		assertEquals(u1.getSourceDir(), u2.getSourceDir());
		assertEquals(u1.getTargetDir(), u2.getTargetDir());
		assertEquals(u1.getTranslate(), u2.getTranslate());
		assertEquals(u1.getType(), u2.getType());

		Segment s1 = u1.getSegment(0);
		Segment s2 = u2.getSegment(0);
		assertEquals(s1.getCanResegment(), s2.getCanResegment());
		assertEquals(s1.getId(), s2.getId());
		assertEquals(s1.getPreserveWS(), s2.getPreserveWS());
		assertEquals(s1.getState(), s2.getState());
		assertEquals(s1.getSubState(), s2.getSubState());
		assertTrue(sameFragments(s1.getSource(), s2.getSource()));
	}
	
	@Test
	public void testMatchWithIsolatedInMatch () {
		String xlfStr = getTestXlfWithIsolatedInMatch();
		Unit unit = getUnit(xlfStr, 1);
		assertNotNull("There seems to be no unit in this xlf doc. ", unit);
	}
	
	@Test
	public void testMissingIsolatedAttribute () {
		try {
			String xlfStr = getTestXlfWithMissingIsolatedAttribute();
			getUnit(xlfStr, 1);
		}
		catch ( XLIFFReaderException e ) {
			assertTrue(e.getMessage().contains("Missing isolated='yes' for opening code id='1'"));
		}
	}
	
	@Test
	public void testGetMatches() {
		String xlfStr = getTestXlf();
		Unit unit = getUnit(xlfStr, 1);
		assertNotNull("There seems to be no unit in this xlf doc. ", unit);
		
		List<Match> exactMatches = unit.getAllExactMatches();
		assertNotNull("There should be some exact matches returned. ", exactMatches);
		assertEquals("There should be one exact match.", 1, exactMatches.size());
		
		double minSim = 85.0;
		List<Match> simMatches = unit.getMatchesByMinimumSimilarity(minSim);
		assertNotNull("There should be some matches returned. ", simMatches);
		assertEquals("There should be five matches.", 5, simMatches.size());
		
		double maxSim = 90.0;
		List<Match> simRangeMatches = unit.getMatchesBySimilarityRange(minSim, maxSim);
		assertNotNull("There should be some matches returned. ", simRangeMatches);
		assertEquals("There should be four matches.", 4, simRangeMatches.size());
		
		int segIdx = 2; 
		List<Match> segmentMatches = unit.getMatchesForSegment(segIdx);
		assertNotNull("There should be some matches returned for segment " + segIdx, segmentMatches);
		assertEquals("There should be six matches returned for segment with index " + segIdx, 6, segmentMatches.size());
		
		segIdx = 3; 
		segmentMatches = unit.getMatchesForSegment(segIdx);
		assertNotNull("There should be some matches returned for segment " + segIdx, segmentMatches);
		assertEquals("There should be one match returned for segment with index " + segIdx, 1, segmentMatches.size());
		
		String ref = "#2";
		List<Match> refMatches = unit.getMatchesByRef(ref);
		assertNotNull("There should be some matches returned for match ref " + ref, refMatches);
		assertEquals("There should be one match for ref " + ref, 1, refMatches.size());
		
		ref = "2";
		refMatches = unit.getMatchesByRef(ref);
		assertNotNull("There should be some matches returned for match ref " + ref, refMatches);
		assertEquals("There should be one match for ref " + ref, 1, refMatches.size());
		
		ref = "#3";
		refMatches = unit.getMatchesByRef(ref);
		assertNotNull("There should be some matches returned for match ref " + ref, refMatches);
		assertEquals("There should be three matches for ref " + ref, 3, refMatches.size());
		
		ref = "3";
		refMatches = unit.getMatchesByRef(ref);
		assertNotNull("There should be some matches returned for match ref " + ref, refMatches);
		assertEquals("There should be three matches for ref " + ref, 3, refMatches.size());
	}
	
	@Test
	public void testNoMatches () {
		Unit unit = new Unit("id1");
		unit.appendSegment().setSource("text");
		assertEquals(0, unit.getAllExactMatches().size());
		assertEquals(0, unit.getMatchesByMinimumSimilarity(10.0).size());
		assertEquals(0, unit.getMatchesByRef("someRef").size());
		assertEquals(0, unit.getMatchesBySimilarityRange(0.0, 75.0).size());
		assertEquals(0, unit.getMatchesForSegment(0).size());
		assertEquals(0, unit.getMatches().size());
	}

	private String getTestXlfWithIsolatedInMatch () {
		String xlString = "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0'\r\n" + 
				" srcLang='en' trgLang='fr'>\r\n" + 
				" <file id='1'>\r\n" + 
				"  <unit id='1'>\r\n" + 
				"   <mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\r\n" + 
				"    <mtc:match ref=\"#m1\" similarity=\"86.0\" matchSuitability=\"86.0\">\r\n" + 
				"     <source>text <sc id='1' isolated=\"yes\"/></source>\r\n" + 
				"     <target>texte <sc id='1' isolated=\"yes\"/></target>\r\n" + 
				"    </mtc:match>\r\n" + 
				"   </mtc:matches>\r\n" + 
				"   <segment>\r\n" + 
				"    <source><sm id=\"m1\" type=\"mtc:match\"/>Text <sc id='1'/><em startRef=\"m1\"/>stuff<ec startRef=\"1\"/></source>\r\n" + 
				"   </segment>\r\n" + 
				"  </unit>\r\n" + 
				" </file>\r\n" + 
				"</xliff>";
		return xlString;
	}
	
	private String getTestXlfWithMissingIsolatedAttribute () {
		String xlString = "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0'\r\n" + 
				" srcLang='en' trgLang='fr'>\r\n" + 
				" <file id='1'>\r\n" + 
				"  <unit id='1'>\r\n" + 
				"   <mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\r\n" + 
				"    <mtc:match ref=\"#m1\" similarity=\"86.0\" matchSuitability=\"86.0\">\r\n" + 
				"     <source>text <sc id='1' isolated=\"yes\"/></source>\r\n" + 
				"     <target>texte <sc id='1' isolated=\"yes\"/></target>\r\n" + 
				"    </mtc:match>\r\n" + 
				"   </mtc:matches>\r\n" + 
				"   <segment>\r\n" +                             // <sc id='1'/> should have isolated="yes"
				"    <source><sm id=\"m1\" type=\"mtc:match\"/>Text <sc id='1'/><em startRef=\"m1\"/></source>\r\n" + 
				"   </segment>\r\n" + 
				"  </unit>\r\n" + 
				" </file>\r\n" + 
				"</xliff>";
		return xlString;
	}
	
	private String getTestXlf() {
		String xlfStr = "<?xml version=\"1.0\"?>\n" +
                "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"de\"\n" +
                "    xmlns:its=\"http://www.w3.org/2005/11/its\" xmlns:itsxlf=\"http://www.w3.org/ns/its-xliff/\"\n" +
                "    its:version=\"2.0\">\n" +
                "    <file id=\"f1\"\n" +
                "        original=\"Alice_Chapter1 EN.1.icml\">\n" +
                "        <group id=\"g1\">\n" +
                "            <unit id=\"ud7-4\">\n" +
                "                    <mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n" +
                "                        <mtc:match ref=\"#1\" type=\"other\" similarity=\"86.0\" matchSuitability=\"86.0\"\n" +
                "                            >\n" +
                "                            <originalData>\n" +
                "                                <data id=\"d1\">&lt;Content></data>\n" +
                "                                <data id=\"d2\">&lt;/Content></data>\n" +
                "                                <data id=\"d3\">&lt;/Content>&lt;Br/>&lt;Br/></data>\n" +
                "                            </originalData>\n" +
                "                            <source>So she was considering, in her own mind (as well as she could,\n" +
                "                                for the hot day made her feel very sleepy and stupid), whether the\n" +
                "                                pleasure of making a daisy-chain would be worth the trouble of\n" +
                "                                getting up and picking the daisies, when suddenly a White Rabbit\n" +
                "                                with pink eyes ran close by her.</source>\n" +
                "                            <target>Sie überlegte sich eben, (so gut es ging, denn sie war schläfrig\n" +
                "                                und dumm von der Hitze,) ob es der Mühe wert sei aufzustehen und\n" +
                "                                Gänseblümchen zu pflücken, um eine Kette damit zu machen, als\n" +
                "                                plötzlich ein weißes Kaninchen mit roten Augen dicht an ihr\n" +
                "                                    vorbeirannte.<pc id=\"11\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"12\" canCopy=\"no\"\n" +
                "                                    canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"13\"\n" +
                "                                    canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"\n" +
                "                                    /><pc id=\"14\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\"\n" +
                "                                    dataRefStart=\"d1\"/><pc id=\"15\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d3\" dataRefStart=\"d1\"/></target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#1\" type=\"other\" similarity=\"86.0\" matchSuitability=\"86.0\"\n" +
                "                            >\n" +
                "                            <originalData>\n" +
                "                                <data id=\"d1\">&lt;Content></data>\n" +
                "                                <data id=\"d2\">&lt;/Content></data>\n" +
                "                                <data id=\"d3\">&lt;/Content>&lt;Br/>&lt;Br/></data>\n" +
                "                            </originalData>\n" +
                "                            <source>So she was considering, in her own mind (as well as she could,\n" +
                "                                for the hot day made her feel very sleepy and stupid), whether the\n" +
                "                                pleasure of making a daisy-chain would be worth the trouble of\n" +
                "                                getting up and picking the daisies, when suddenly a White Rabbit\n" +
                "                                with pink eyes ran close by her.</source>\n" +
                "                            <target>Sie überlegte sich eben, (so gut es ging, denn sie war schläfrig\n" +
                "                                und dumm von der Hitze,) ob es der Mühe werth sei aufzustehen und\n" +
                "                                Gänseblümchen zu pflücken, um eine Kette damit zu machen, als\n" +
                "                                plötzlich ein weißes Kaninchen mit rothen Augen dicht an ihr\n" +
                "                                    vorbeirannte.<pc id=\"11\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"12\" canCopy=\"no\"\n" +
                "                                    canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"13\"\n" +
                "                                    canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"\n" +
                "                                    /><pc id=\"14\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\"\n" +
                "                                    dataRefStart=\"d1\"/><pc id=\"15\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d3\" dataRefStart=\"d1\"/></target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#1\" type=\"other\" similarity=\"85.0\" matchSuitability=\"85.0\"\n" +
                "                            >\n" +
                "                            <originalData>\n" +
                "                                <data id=\"d1\">&lt;Content></data>\n" +
                "                                <data id=\"d2\">&lt;/Content></data>\n" +
                "                                <data id=\"d3\">&lt;/Content>&lt;Br/>&lt;Br/></data>\n" +
                "                            </originalData>\n" +
                "                            <source>So she was considering, in her own mind (as well as she could,\n" +
                "                                for the hot day made her feel very sleepy and stupid), whether the\n" +
                "                                pleasure of making a daisy-chain would be worth the trouble of\n" +
                "                                getting up and picking the daisies, when suddenly a Blue Rabbit with\n" +
                "                                pink eyes ran close by her.</source>\n" +
                "                            <target>Sie überlegte sich eben, (so gut es ging, denn sie war schläfrig\n" +
                "                                und dumm von der Hitze,) ob es der Mühe werth sei aufzustehen und\n" +
                "                                Gänseblümchen zu pflücken, um eine Kette damit zu machen, als\n" +
                "                                plötzlich ein blaues Kaninchen mit rothen Augen dicht an ihr\n" +
                "                                    vorbeirannte.<pc id=\"11\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"12\" canCopy=\"no\"\n" +
                "                                    canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"13\"\n" +
                "                                    canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"\n" +
                "                                    /><pc id=\"14\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\"\n" +
                "                                    dataRefStart=\"d1\"/><pc id=\"15\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d3\" dataRefStart=\"d1\"/></target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#1\" type=\"other\" similarity=\"84.0\" matchSuitability=\"84.0\"\n" +
                "                            >\n" +
                "                            <originalData>\n" +
                "                                <data id=\"d1\">&lt;Content></data>\n" +
                "                                <data id=\"d2\">&lt;/Content></data>\n" +
                "                                <data id=\"d3\">&lt;/Content>&lt;Br/>&lt;Br/></data>\n" +
                "                            </originalData>\n" +
                "                            <source>So she was considering, in her own mind (as well as she could,\n" +
                "                                for the hot day made her feel very sleepy and stupid), whether the\n" +
                "                                pleasure of making a daisy-chain would be worth the trouble of\n" +
                "                                getting up and picking the daisies, when suddenly a White Bunny with\n" +
                "                                pink eyes ran close by her.</source>\n" +
                "                            <target>Sie überlegte sich eben, (so gut es ging, denn sie war schläfrig\n" +
                "                                und dumm von der Hitze,) ob es der Mühe werth sei aufzustehen und\n" +
                "                                Gänseblümchen zu pflücken, um eine Kette damit zu machen, als\n" +
                "                                plötzlich ein weißer Hase mit rothen Augen dicht an ihr\n" +
                "                                    vorbeirannte.<pc id=\"11\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"12\" canCopy=\"no\"\n" +
                "                                    canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"13\"\n" +
                "                                    canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"\n" +
                "                                    /><pc id=\"14\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\"\n" +
                "                                    dataRefStart=\"d1\"/><pc id=\"15\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d3\" dataRefStart=\"d1\"/></target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#1\" type=\"other\" similarity=\"80.0\" matchSuitability=\"80.0\"\n" +
                "                            >\n" +
                "                            <originalData>\n" +
                "                                <data id=\"d1\">&lt;Content></data>\n" +
                "                                <data id=\"d2\">&lt;/Content></data>\n" +
                "                                <data id=\"d3\">&lt;/Content>&lt;Br/>&lt;Br/></data>\n" +
                "                            </originalData>\n" +
                "                            <source> &lt;it/>&lt;it/> So she was considering, in her own mind (as\n" +
                "                                well as she could, for the hot day made her feel very sleepy and\n" +
                "                                stupid), whether the pleasure of making a daisy-chain would be worth\n" +
                "                                the trouble of getting up and picking the daisies, when suddenly a\n" +
                "                                White Rabbit with pink eyes ran close by her. </source>\n" +
                "                            <target>\n" +
                "                                <pc id=\"11\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\"\n" +
                "                                    dataRefStart=\"d1\"/><pc id=\"12\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d2\" dataRefStart=\"d1\"/> Sie überlegte sich eben, (so\n" +
                "                                gut es ging, denn sie war schläfrig und dumm von der Hitze,) ob es\n" +
                "                                der Mühe wert sei aufzustehen und Gänseblümchen zu pflücken, um eine\n" +
                "                                Kette damit zu machen, als plötzlich ein weißes Kaninchen mit roten\n" +
                "                                Augen dicht an ihr vorbeirannte. <pc id=\"13\" canCopy=\"no\"\n" +
                "                                    canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"14\"\n" +
                "                                    canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"\n" +
                "                                    /><pc id=\"15\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d3\"\n" +
                "                                    dataRefStart=\"d1\"/></target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#1\" type=\"other\" similarity=\"80.0\" matchSuitability=\"80.0\"\n" +
                "                            >\n" +
                "                            <originalData>\n" +
                "                                <data id=\"d1\">&lt;Content></data>\n" +
                "                                <data id=\"d2\">&lt;/Content></data>\n" +
                "                                <data id=\"d3\">&lt;/Content>&lt;Br/>&lt;Br/></data>\n" +
                "                            </originalData>\n" +
                "                            <source>So she was considering, in her own mind (as well as she could,\n" +
                "                                for the hot day made her feel very sleepy and stupid), whether the\n" +
                "                                pleasure of making a daisy-chain would be worth the trouble of\n" +
                "                                getting up and picking the daisies, when suddenly a Blue Bunny with\n" +
                "                                orange spies ran close by her.</source>\n" +
                "                            <target>Sie überlegte sich eben, (so gut es ging, denn sie war schläfrig\n" +
                "                                und dumm von der Hitze,) ob es der Mühe werth sei aufzustehen und\n" +
                "                                Gänseblümchen zu pflücken, um eine Kette damit zu machen, als\n" +
                "                                plötzlich ein blauer Hase mit orangenen Spionen dicht an ihr\n" +
                "                                    vorbeirannte.<pc id=\"11\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"12\" canCopy=\"no\"\n" +
                "                                    canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"13\"\n" +
                "                                    canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"\n" +
                "                                    /><pc id=\"14\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\"\n" +
                "                                    dataRefStart=\"d1\"/><pc id=\"15\" canCopy=\"no\" canDelete=\"no\"\n" +
                "                                    dataRefEnd=\"d3\" dataRefStart=\"d1\"/></target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#2\" type=\"other\" similarity=\"89.0\" matchSuitability=\"89.0\"\n" +
                "                            >\n" +
                "                            <originalData>\n" +
                "                                <data id=\"d1\">&lt;Content></data>\n" +
                "                                <data id=\"d2\">&lt;/Content></data>\n" +
                "                            </originalData>\n" +
                "                            <source>There was nothing so very remarkable in that; nor did Alice\n" +
                "                                think it so very much out of the way to hear the Rabbit say to\n" +
                "                                itself \"Oh dear! </source>\n" +
                "                            <target>Dies war grade nicht sehr; merkwürdig; Alice fand es auch nicht\n" +
                "                                sehr außerordentlich, daß sie das Kaninchen sagen hörte: » Oh\n" +
                "                                Jemine! <pc id=\"18\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\"\n" +
                "                                    dataRefStart=\"d1\"/><sc id=\"19\" canOverlap=\"no\" isolated=\"yes\"\n" +
                "                                    canCopy=\"no\" canDelete=\"no\" dataRef=\"d1\"/></target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#3\" type=\"other\" similarity=\"100.0\" matchSuitability=\"100.0\"\n" +
                "                            >\n" +
                "                            <source>Would the fall never come to an end? </source>\n" +
                "                            <target>Wollte denn der Fall nie endigen? </target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#3\" type=\"other\" similarity=\"84.0\" matchSuitability=\"84.0\"\n" +
                "                            >\n" +
                "                            <source>Would the autumn never come to an end? </source>\n" +
                "                            <target>Wollte denn der Herbst nie endigen? </target>\n" +
                "                        </mtc:match>\n" +
                "                        <mtc:match ref=\"#3\" type=\"other\" similarity=\"84.0\" matchSuitability=\"84.0\"\n" +
                "                            >\n" +
                "                            <source>Would the winter never come to an end? </source>\n" +
                "                            <target>Wollte denn der Winter nie endigen? </target>\n" +
                "                        </mtc:match>\n" +
                "                    </mtc:matches>\n" +
                "                    \n" +
                "                    <originalData>\n" +
                "                        <data id=\"d1\">&lt;Content></data>\n" +
                "                        <data id=\"d2\">&lt;/Content></data>\n" +
                "                        <data id=\"d3\">&lt;/Content>&lt;Br/>&lt;Br/></data>\n" +
                "                    </originalData>\n" +
                "                    <segment>\n" +
                "                        <source xml:space=\"preserve\"><pc id=\"4\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">Alice was beginning to get very tired of sitting by her sister on the </pc><sc id=\"5\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d1\"/>bank, and of having nothing to do. </source>\n" +
                "                        <target xml:space=\"preserve\"><pc id=\"4\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">Alice was beginning to get very tired of sitting by her sister on the </pc><sc id=\"5\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d1\"/>bank, and of having nothing to do. </target>\n" +
                "                    </segment>\n" +
                "                    <segment>\n" +
                "                        <source xml:space=\"preserve\">Once or twice she had peeped into the <ec startRef=\"5\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d2\"/><pc id=\"6\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">book her sister was reading, but it had no pictures or conversations in </pc><pc id=\"7\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">it, “and what is the use of a book,” thought Alice, “without pictures or </pc><pc id=\"8\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d3\" dataRefStart=\"d1\">conversations?”</pc></source>\n" +
                "                        <target xml:space=\"preserve\">Once or twice she had peeped into the <ec startRef=\"5\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d2\"/><pc id=\"6\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">book her sister was reading, but it had no pictures or conversations in </pc><pc id=\"7\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">it, “and what is the use of a book,” thought Alice, “without pictures or </pc><pc id=\"8\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d3\" dataRefStart=\"d1\">conversations?”</pc></target>\n" +
                "                    </segment>\n" +
                "                    <segment state=\"initial\">\n" +
                "                        <source xml:space=\"preserve\"><mrk id=\"1\" type=\"mtc:match\"><pc id=\"11\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">So she was considering in her own mind (as well as she could, for the </pc><pc id=\"12\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">day made her feel very sleepy and stupid), whether the pleasure of </pc><pc id=\"13\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">making a daisy-chain would be worth the trouble of getting up and </pc><pc id=\"14\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">picking the daisies, when suddenly a White Rabbit with pink eyes ran </pc><pc id=\"15\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d3\" dataRefStart=\"d1\">close by her.</pc></mrk></source>\n" +
                "                        <target xml:space=\"preserve\">Sie überlegte sich eben, (so gut es ging, denn sie war schläfrig und dumm von der Hitze,) ob es der Mühe wert sei aufzustehen und Gänseblümchen zu pflücken, um eine Kette damit zu machen, als plötzlich ein weißes Kaninchen mit roten Augen dicht an ihr vorbeirannte.<pc id=\"11\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"12\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"13\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"14\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><pc id=\"15\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d3\" dataRefStart=\"d1\"/></target>\n" +
                "                    </segment>\n" +
                "                    <segment state=\"initial\">\n" +
                "                        <source xml:space=\"preserve\"><sm id=\"2\" type=\"mtc:match\"/><pc id=\"18\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\">There was nothing so very remarkable in that, nor did Alice think it so </pc><sc id=\"19\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d1\"/>very much out of the way to hear the Rabbit say to itself, “Oh dear! <em startRef=\"2\"/></source>\n" +
                "                        <target xml:space=\"preserve\">Dies war grade nicht sehr; merkwürdig; Alice fand es auch nicht sehr außerordentlich, daß sie das Kaninchen sagen hörte: » Oh Jemine! <pc id=\"18\" canCopy=\"no\" canDelete=\"no\" dataRefEnd=\"d2\" dataRefStart=\"d1\"/><sc id=\"19\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d1\"/></target>\n" +
                "                    </segment>\n" +
                "                    <segment>\n" +
                "                        <source xml:space=\"preserve\">Oh <ec startRef=\"19\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d2\"/>dear! </source>\n" +
                "                        <target xml:space=\"preserve\">Oh <ec startRef=\"19\" canOverlap=\"no\" canCopy=\"no\" canDelete=\"no\" dataRef=\"d2\"/>dear! </target>\n" +
                "                    </segment>\n" +
                "                    <segment>\n" +
                "                        <source xml:space=\"preserve\">I shall be too late!” </source>\n" +
                "                        <target xml:space=\"preserve\">I shall be too late!” </target>\n" +
                "                    </segment>\n" +
                "                    <!-- snip -->\n" +
                "                    <segment>\n" +
                "                        <source xml:space=\"preserve\">Down, down, down! </source>\n" +
                "                        <target xml:space=\"preserve\">Down, down, down! </target>\n" +
                "                    </segment>\n" +
                "                    <segment state=\"initial\">\n" +
                "                        <source xml:space=\"preserve\"><mrk id=\"3\" type=\"mtc:match\">Would the fall never come to an end? </mrk></source>\n" +
                "                        <target xml:space=\"preserve\">Wollte denn der Fall nie endigen? </target>\n" +
                "                    </segment>\n" +
                "                </unit>\n" +
                "        </group>\n" +
                "    </file>\n" +
                "</xliff>\n";
		
		return xlfStr;
	}

	private boolean sameFragments (Fragment f1,
		Fragment f2)
	{
		String ct1 = f1.getCodedText();
		String ct2 = f2.getCodedText();
		for ( int i=0; i<ct1.length(); i++ ) {
			char c1 = ct1.charAt(i);
			char c2 = ct2.charAt(i);
			assertEquals(c1, c2);
			if ( Fragment.isChar1(c1) ) {
				int key1 = Fragment.toKey(c1, ct1.charAt(i+1));
				int key2 = Fragment.toKey(c2, ct2.charAt(i+1));
				i++;
				Tag bm1 = f1.getTag(key1);
				Tag bm2 = f2.getTag(key2);
				assertEquals(bm1.getId(), bm2.getId());
				assertEquals(bm1.getTagType(), bm2.getTagType());
				assertEquals(bm1.getType(), bm2.getType());
				assertTrue(sameExtAttributes(bm1, bm2));
				if ( bm1 instanceof CTag ) {
					CTag cm1 = (CTag)bm1;
					CTag cm2 = (CTag)bm2;
					assertEquals(cm1.getCanCopy(), cm2.getCanCopy());
					assertEquals(cm1.getCanDelete(), cm2.getCanDelete());
					assertEquals(cm1.getCanOverlap(), cm2.getCanOverlap());
					assertEquals(cm1.getCanReorder(), cm2.getCanReorder());
					assertEquals(cm1.getCopyOf(), cm2.getCopyOf());
					assertEquals(cm1.getDataDir(), cm2.getDataDir());
					assertEquals(cm1.getDataRef(), cm2.getDataRef());
					assertEquals(cm1.getDir(), cm2.getDir());
					assertEquals(cm1.getDisp(), cm2.getDisp());
					assertEquals(cm1.getEquiv(), cm2.getEquiv());
					assertEquals(cm1.isInitialWithData(), cm2.isInitialWithData());
					assertEquals(cm1.getData(), cm2.getData());
					assertEquals(cm1.getSubFlows(), cm2.getSubFlows());
					assertEquals(cm1.getSubType(), cm2.getSubType());
				}
				else {
					MTag am1 = (MTag)bm1;
					MTag am2 = (MTag)bm2;
					assertEquals(am1.getRef(), am2.getRef());
					assertEquals(am1.getTranslate(), am2.getTranslate());
					assertEquals(am1.getValue(), am2.getValue());
				}
			}
		}
		return true;
	}
	
	private boolean sameExtAttributes (IWithExtAttributes o1,
		IWithExtAttributes o2)
	{
		for ( ExtAttribute  a1 : o1.getExtAttributes() ) {
			ExtAttribute a2 = o2.getExtAttributes().getAttribute(a1.getNamespaceURI(), a1.getLocalPart());
			assertEquals(a1.getValue(), a2.getValue());
			assertEquals(a1.getPrefix(), a2.getPrefix());
		}
		return true;
	}

	private boolean sameNotes (IWithNotes o1,
		IWithNotes o2)
	{
		assertEquals(o1.getNoteCount(), o2.getNoteCount());
		Iterator<Note> iter1 = o1.getNotes().iterator();
		Iterator<Note> iter2 = o2.getNotes().iterator();
		while ( iter1.hasNext() ) {
			assertTrue(iter2.hasNext());
			Note n1 = iter1.next();
			Note n2 = iter2.next();
			assertEquals(n1.getAppliesTo(), n2.getAppliesTo());
			assertEquals(n1.getCategory(), n2.getCategory());
			assertEquals(n1.getId(), n2.getId());
			assertEquals(n1.getPriority(), n2.getPriority());
			assertEquals(n1.getText(), n2.getText());
			assertTrue(sameExtAttributes(n1, n2));
		}
		return true;
	}

	private void pseudoTranslateSource (Unit unit) {
		for ( Part part : unit ) {
			Fragment frag = part.getSource();
			StringBuilder tmp = new StringBuilder(frag.getCodedText());
			for (int i=0; i<tmp.length(); i++ ) {
				if ( Fragment.isChar1(tmp.charAt(i)) ) {
					i++; // Skip
				}
				else {
					tmp.setCharAt(i, 'Z');
				}
			}
			frag.setCodedText(tmp.toString());
		}
	}
	
	private String getAllText (Unit unit,
		boolean target)
	{
		StringBuilder tmp = new StringBuilder();
		if ( target ) {
			List<Part> list = unit.getTargetOrderedParts();
			for ( Part part : list ) {
				tmp.append(U.getTargetOrSource(part).toXLIFF());
			}
		}
		else {
			for ( Part part : unit ) {
				tmp.append(part.getSource().toXLIFF());
			}
		}
		return tmp.toString();
	}
	
	private Unit getUnit (String snippet, int pos)
	{
		int count = 0;
		try ( XLIFFReader reader = new XLIFFReader() ) {
			reader.open(snippet);
			while ( reader.hasNext() ) {
				Event event = reader.next();
				if ( event.isUnit() ) {
					count++;
					if ( count == pos ) return event.getUnit();
				}
			}
			return null;
		}
	}
	
}
