package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AnnotatedSpanTest {

	@Test
	public void testSimpleCase () {
		Unit unit = new Unit("u1");
		Part part = unit.appendSegment();
		Fragment srcFrag = part.getSource();
		MTag am1 = srcFrag.openMarkerSpan("m1", "comment");
		am1.setValue("sc1");
		srcFrag.append("s1");
		srcFrag.closeMarkerSpan("m1");
		
		List<AnnotatedSpan> list = unit.getAnnotatedSpans(false);
		assertEquals(1, list.size());
		AnnotatedSpan aspan = list.get(0);
		assertSame(am1, aspan.getMarker());
		assertEquals(2, aspan.getStart());
		assertSame(part, aspan.getStartPart());
		assertEquals(4, aspan.getEnd());
		assertSame(part, aspan.getEndPart());
		assertEquals("s1", aspan.getCodedText());
		assertTrue(aspan.isFullContent());
		assertEquals(1, aspan.getPartCount());
		assertEquals("m1", aspan.getId());
	}

	@Test
	public void testMultiParts () {
		Unit unit = new Unit("u1");
		Part part = unit.appendSegment();
		Fragment srcFrag = part.getSource();
		MTag am1 = srcFrag.openMarkerSpan("m1", "comment");
		am1.setValue("sc1");
		MTag am2 = srcFrag.openMarkerSpan("m2", "my:info");
		am2.setValue("info");
		srcFrag.append("Text1.");
		srcFrag.appendCode("c1", "[BR/]");
		unit.appendIgnorable().getSource().append(' ');
		srcFrag = unit.appendSegment().getSource();
		srcFrag.append("More.");
		srcFrag.closeMarkerSpan("m2");
		srcFrag.closeMarkerSpan("m1");
		List<AnnotatedSpan> list = unit.getAnnotatedSpans(false);
		
		assertEquals(2, list.size());
		AnnotatedSpan aspan = list.get(0);
		assertSame(am1, aspan.getMarker());
		assertEquals(2, aspan.getStart());
		assertSame(unit.getPart(0), aspan.getStartPart());
		assertEquals(7, aspan.getEnd());
		assertSame(unit.getPart(2), aspan.getEndPart());
		assertEquals("{oA}Text1.{hC} More.{cA}", U.fmtMarkers(aspan.getCodedText()));
		assertEquals("Text1. More.", aspan.getPlainText());
		assertTrue(aspan.isFullContent());
		assertEquals(3, aspan.getPartCount());
		assertEquals("m1", aspan.getId());

		aspan = list.get(1);
		assertSame(am2, aspan.getMarker());
		assertEquals(4, aspan.getStart());
		assertSame(unit.getPart(0), aspan.getStartPart());
		assertEquals(5, aspan.getEnd());
		assertSame(unit.getPart(2), aspan.getEndPart());
		assertEquals("Text1.{hC} More.", U.fmtMarkers(aspan.getCodedText()));
		assertEquals("Text1. More.", aspan.getPlainText());
		assertTrue(aspan.isFullContent());
		assertEquals(3, aspan.getPartCount());
		assertEquals("m2", aspan.getId());
	}

}
