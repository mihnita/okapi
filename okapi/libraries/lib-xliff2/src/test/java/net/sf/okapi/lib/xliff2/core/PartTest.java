package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import net.sf.okapi.lib.xliff2.core.Part.GetTarget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PartTest {

	@Test
	public void testRemoveAnnotations () {
		Unit unit = new Unit("u1");
		Part part = unit.appendIgnorable();
		Fragment srcFrag = part.getSource();
		srcFrag.openMarkerSpan("m1", "comment").setValue("sc1");
		srcFrag.append("s1");
		srcFrag.closeMarkerSpan("m1");
		// Target
		Fragment trgFrag = part.getTarget(GetTarget.CREATE_EMPTY);
		trgFrag.openMarkerSpan("m2", "comment").setValue("tc1");
		trgFrag.append("s1");
		trgFrag.closeMarkerSpan("m2");
		trgFrag.annotate(0, -1, "my:type", "value", null);

		part.removeMarkers(false, null);
		assertFalse(part.getSource().hasTag());
		part.removeMarkers(true, "my:type");
		assertEquals(2, part.getSource().getStore().getTargetTags().size());
	}

	@Test
	public void testCreateTargetClone () {
		Unit unit = new Unit("u1");
		Part part = unit.appendSegment();
		Fragment frag = part.getSource();
		frag.append("t1");
		frag.append(TagType.OPENING, "c1", "[z]", true);
		frag.append("t2");
		frag.append(TagType.CLOSING, "c1", "[/z]", true);
		part = unit.appendSegment();
		frag = part.getSource();
		frag.append("t3");
		frag.append(TagType.OPENING, "c2", "[x]", false);
		frag.append("t4");
		frag.append(TagType.CLOSING, "c2", "[/x]", false);
		
		Fragment trgFrag = part.getTarget(GetTarget.CLONE_SOURCE);
		assertEquals("t3<pc id=\"c2\">t4</pc>", trgFrag.toXLIFF());
	}
	
	@Test
	public void testGetId () {
		Unit unit = new Unit("u1");
		unit.appendSegment().setId("s1");
		unit.appendSegment().setId("s3");
		Part part = unit.appendSegment();
		assertNull(part.getId());
		assertNull(part.getId(false));
		assertEquals("s2", part.getId(true));
		part = unit.appendSegment();
		assertEquals("s4", part.getId(true));
	}
}
