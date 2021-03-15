package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SegmentTest {

	@Test
	public void testSimple () {
		Segment seg = new Segment(new Unit("u1").getStore());
		Fragment srcFrag = seg.getSource();
		srcFrag.append(TagType.OPENING, "1", "[1]", true);
		srcFrag.append("text with \u0305 and \u0001");
		srcFrag.append(TagType.CLOSING, "1", "[/1]", true);
		srcFrag.appendCode("2", "[2and\u0001/]");
		seg.setId("s1");
		seg.setPreserveWS(true);
		// Target
		Fragment trgFrag = seg.getTarget(GetTarget.CREATE_EMPTY);
		trgFrag.append(TagType.OPENING, "1", "{1}", true);
		trgFrag.appendCode("2", "{2AND\u0001/}");
		trgFrag.append("TEXT WITH \u0305 AND \u0001");
		trgFrag.append(TagType.CLOSING, "1", "{/1}", true);

		assertTrue(seg.getPreserveWS());
		assertTrue(seg.getCanResegment());
		assertEquals(Segment.STATE_DEFAULT, seg.getState());
		assertNull(seg.getSubState());
		assertEquals("s1", seg.getId());
	}
	
	@Test
	public void testStateAndSubStateValues () {
		Segment seg = new Segment(new Unit("u1").getStore());
		seg.setState("initial");
		seg.setState("translated");
		seg.setState("reviewed");
		seg.setState("final");
		seg.setSubState("a:bc");
	}

	@Test(expected=InvalidParameterException.class)
	public void testStateBadStateValue () {
		Segment seg = new Segment(new Unit("u1").getStore());
		seg.setState("_badvalue_for_test_");
	}

	@Test(expected=InvalidParameterException.class)
	public void testStateBadSubStateValue1 () {
		Segment seg = new Segment(new Unit("u1").getStore());
		seg.setSubState(":value");
	}

	@Test(expected=InvalidParameterException.class)
	public void testStateBadSubStateValue2 () {
		Segment seg = new Segment(new Unit("u1").getStore());
		seg.setSubState("abc:");
	}

	@Test(expected=InvalidParameterException.class)
	public void testStateBadSubStateValue3 () {
		Segment seg = new Segment(new Unit("u1").getStore());
		seg.setSubState("abc-value");
	}

	@Test(expected=InvalidParameterException.class)
	public void testStateBadSubStateValue4 () {
		Segment seg = new Segment(new Unit("u1").getStore());
		seg.setSubState("");
	}

	@Test
	public void testCloneSource () {
		Segment seg = new Segment(new Unit("u1").getStore());
		Fragment f1 = seg.getSource();
		f1.append(TagType.OPENING, "1", "o1", false);
		f1.append(TagType.CLOSING, "1", "c1", false);
		f1.openMarkerSpan("m1", "comment").setValue("data");
		f1.append(TagType.STANDALONE, "2", "p2", false);
		f1.closeMarkerSpan("m1");
		Fragment f2 = seg.getTarget(GetTarget.CLONE_SOURCE);
		Tags m1 = seg.getSourceTags();
		Tags m2 = seg.getTargetTags();

		assertEquals(f2.toString(), f1.toString());
		assertEquals(f2.toXLIFF(), f1.toXLIFF());

		assertFalse(m2.get(U.kOC(0)) == m1.get(U.kOC(0)));
		assertEquals(m2.get(U.kOC(0)).getId(), m1.get(U.kOC(0)).getId());
		assertEquals(m2.get(U.kOC(0)).getTagType(), m1.get(U.kOC(0)).getTagType());

		assertFalse(m2.get(U.kCA(0)) == m1.get(U.kCA(0)));
		assertEquals(m2.get(U.kCA(0)).getId(), m1.get(U.kCA(0)).getId());
		assertEquals(m2.get(U.kCA(0)).getTagType(), m1.get(U.kCA(0)).getTagType());
	}
}
