package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;

import net.sf.okapi.lib.xliff2.core.Part.GetTarget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DirectionalityTest {

	@Test
	public void testDefaultValues () {
		Unit unit = new Unit("id");
		assertEquals(Directionality.AUTO, unit.getSourceDir());
		assertEquals(Directionality.AUTO, unit.getTargetDir());
		
		Segment seg = unit.appendSegment();
		assertEquals(Directionality.INHERITED, seg.getSource().getDir(false));
		assertEquals(Directionality.INHERITED, seg.getTarget(GetTarget.CREATE_EMPTY).getDir(false));
		assertEquals(Directionality.AUTO, seg.getSource().getDir(true));
		assertEquals(Directionality.AUTO, seg.getTarget(GetTarget.CREATE_EMPTY).getDir(true));

		Part ign = unit.appendIgnorable();
		assertEquals(Directionality.INHERITED, ign.getSource().getDir(false));
		assertEquals(Directionality.INHERITED, ign.getTarget(GetTarget.CREATE_EMPTY).getDir(false));
		assertEquals(Directionality.AUTO, ign.getSource().getDir(true));
		assertEquals(Directionality.AUTO, ign.getTarget(GetTarget.CREATE_EMPTY).getDir(true));
	}
	
	@Test
	public void testAccess () {
		Unit unit = new Unit("id");
		unit.setSourceDir(Directionality.RTL);
		unit.setTargetDir(unit.getSourceDir());
		assertEquals(Directionality.RTL, unit.getSourceDir());
		assertEquals(Directionality.RTL, unit.getTargetDir());
		
		Segment seg = unit.appendSegment();
		// Default is inherited from unit
		assertEquals(Directionality.RTL, seg.getSource().getDir(true));
		assertEquals(Directionality.RTL, seg.getTarget(GetTarget.CREATE_EMPTY).getDir(true));
		// Override
		seg.getSource().setDir(Directionality.LTR);
		seg.getTarget(GetTarget.CREATE_EMPTY).setDir(seg.getSource().getDir(true));
		assertEquals(Directionality.LTR, seg.getSource().getDir(true));
		assertEquals(Directionality.LTR, seg.getTarget().getDir(true));
		
		Part ign = unit.appendIgnorable();
		// Default is inherited from unit
		assertEquals(Directionality.RTL, ign.getSource().getDir(true));
		assertEquals(Directionality.RTL, ign.getTarget(GetTarget.CREATE_EMPTY).getDir(true));
		// Override
		ign.getSource().setDir(Directionality.LTR);
		ign.getTarget(GetTarget.CREATE_EMPTY).setDir(seg.getSource().getDir(true));
		assertEquals(Directionality.LTR, ign.getSource().getDir(true));
		assertEquals(Directionality.LTR, ign.getTarget().getDir(true));
	}
	
	@Test
	public void testNesting () {
		Unit unit = new Unit("id");
		unit.setSourceDir(Directionality.RTL);

		Segment seg = unit.appendSegment();
		// "a[rtl]B [ltr]c [/ltr]D [/rtl]e"
		Fragment srcFrag = seg.getSource();
		srcFrag.append("a ");
		CTag cm1 = srcFrag.append(TagType.OPENING, "1", "[rtl]", false);
		cm1.setDir(Directionality.RTL);
		srcFrag.append("B ");
		CTag cm2 = srcFrag.append(TagType.OPENING, "2", "[ltr]", false);
		cm2.setDir(Directionality.LTR);
		srcFrag.append("c ");
		srcFrag.append(TagType.CLOSING, "2", "[/ltr]", false);
		srcFrag.append("D ");
		srcFrag.append(TagType.CLOSING, "1", "[/rtl]", false);
		srcFrag.append("e");
//TODO: fix dir output
		assertEquals("a <pc id=\"1\">B <pc id=\"2\">c </pc>D </pc>e", srcFrag.toXLIFF());
	}

}
