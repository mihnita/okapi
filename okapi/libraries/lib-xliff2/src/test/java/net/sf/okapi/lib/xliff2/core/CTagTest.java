package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.sf.okapi.lib.xliff2.InvalidParameterException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CTagTest {

	@Test
	public void testSimple () {
		CTag code = new CTag(null, TagType.OPENING, "1", null);
		assertEquals(TagType.OPENING, code.getTagType());
		assertEquals("1", code.getId());
		assertNull(code.getData());
	}
	
//	@Test
//	public void testAllMarkerTypes () {
//		CTag code = new CTag(null, TagType.OPENING, "1", null);
//		assertEquals(TagType.OPENING, code.getTagType());
//		code.setTagType(TagType.CLOSING);
//		assertEquals(TagType.CLOSING, code.getTagType());
//		code.setTagType(TagType.STANDALONE);
//		assertEquals(TagType.STANDALONE, code.getTagType());
//	}
	
	@Test
	public void testOriginalData () {
		CTag code = new CTag(null, TagType.STANDALONE, "1", null);
		assertNull(code.getData());
		assertFalse(code.hasData());
		code = new CTag(null, TagType.STANDALONE, "1", "");
		assertEquals("", code.getData());
		assertFalse(code.hasData());
		code = new CTag(null, TagType.STANDALONE, "1", "z");
		assertEquals("z", code.getData());
		assertTrue(code.hasData());
	}
	
	@Test
	public void testHintsDefaults () {
		CTag code = new CTag(null, TagType.STANDALONE, "1", null);
		assertTrue(code.getCanDelete());
		assertTrue(code.getCanCopy());
		assertEquals(CanReorder.YES, code.getCanReorder());
	}
	
	@Test
	public void testHintsCanDelete () {
		CTag code = new CTag(null, TagType.STANDALONE, "1", null);
		code.setCanDelete(false);
		assertFalse(code.getCanDelete());
		assertTrue(code.getCanCopy());
		assertEquals(CanReorder.YES, code.getCanReorder());
		code.setCanDelete(true);
		assertTrue(code.getCanDelete());
		assertTrue(code.getCanCopy());
		assertEquals(CanReorder.YES, code.getCanReorder());
	}
	
	@Test
	public void testHintsCanReplicate () {
		CTag code = new CTag(null, TagType.STANDALONE, "1", null);
		code.setCanCopy(false);
		assertTrue(code.getCanDelete());
		assertFalse(code.getCanCopy());
		assertEquals(CanReorder.YES, code.getCanReorder());
		code.setCanCopy(true);
		assertTrue(code.getCanDelete());
		assertTrue(code.getCanCopy());
		assertEquals(CanReorder.YES, code.getCanReorder());
	}
	
	@Test
	public void testHintsCanReorder () {
		CTag code = new CTag(null, TagType.STANDALONE, "1", null);
		assertTrue(code.getCanDelete());
		assertTrue(code.getCanCopy());
		code.setCanReorder(CanReorder.FIRSTNO);
		// Note that canDelete and canCopy are reset automatically based on canReorder in the library
		assertFalse(code.getCanDelete());
		assertFalse(code.getCanCopy());
		assertEquals(CanReorder.FIRSTNO, code.getCanReorder());
		code.setCanReorder(CanReorder.NO);
		assertFalse(code.getCanDelete());
		assertFalse(code.getCanCopy());
		assertEquals(CanReorder.NO, code.getCanReorder());
	}
	
	@Test
	public void testOtherDefaults () {
		CTag code = new CTag(null, TagType.STANDALONE, "1", null);
		assertEquals("", code.getEquiv());
		assertNull(code.getCopyOf());
		assertEquals(Directionality.AUTO, code.getDataDir());
		assertNull(code.getDataRef());
		assertNull(code.getDisp());
	}

	@Test
	public void testEquals () {
		assertEquals(new CTag(TagType.STANDALONE, "1", null), new CTag(TagType.STANDALONE, "1", null));
		assertNotEquals(new CTag(TagType.STANDALONE, "1", null), new CTag(TagType.STANDALONE, "2", null));
		assertNotEquals(new CTag(TagType.STANDALONE, "1", null), new CTag(TagType.OPENING, "1", null));
		assertNotEquals(new CTag(TagType.STANDALONE, "1", null), new CTag(TagType.STANDALONE, "1", "data"));
		
		CTag code1 = new CTag(TagType.STANDALONE, "1", "d1");
		code1.setDisp("di1");
		code1.setEquiv("eq1");
		code1.setSubFlows("sf1");
		code1.setCanDelete(false);
		code1.setType("ui");

		CTag code2 = new CTag(TagType.STANDALONE, "1", "d1");
		code2.setDisp("di1");
		code2.setEquiv("eq1");
		code2.setSubFlows("sf1");
		code2.setCanDelete(false);
		code2.setType("ui");

		assertEquals(code1, code2);
		
		code2.setType("fmt");
		assertNotEquals(code1, code2);
		code2.setType("ui");
		assertEquals(code1, code2);
		
		code2.setEquiv("eq2");
		assertNotEquals(code1, code2);
		code2.setEquiv("eq1");
		assertEquals(code1, code2);
		
		code2.setDisp("di2");
		assertNotEquals(code1, code2);
		code2.setDisp("di1");
		assertEquals(code1, code2);
		
		code2.setSubFlows("sf2");
		assertNotEquals(code1, code2);
		code2.setSubFlows("sf1");
		assertEquals(code1, code2);
		
		code2.setCanCopy(false);
		assertNotEquals(code1, code2);
		code2.setCanCopy(true);
		assertEquals(code1, code2);
	}
	
	@Test
	public void testSubFlows () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setSubFlows(" 1 \t 2\t3  4\n5\t");
		assertEquals("1 2 3 4 5", cm.getSubFlows());
		int i = 1;
		for ( String id : cm.getSubFlowsIds() ) {
			assertEquals(""+i, id);
			i++;
		}
		cm.setSubFlows(null);
		assertNull(cm.getSubFlows());
		assertEquals(0, cm.getSubFlowsIds().length);
	}
	
	@Test
	public void testTypes () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		assertEquals("fmt", cm.getType());
		cm.setType("ui");
		assertEquals("ui", cm.getType());
		cm.setType("quote");
		assertEquals("quote", cm.getType());
		cm.setType("link");
		assertEquals("link", cm.getType());
		cm.setType("image");
		assertEquals("image", cm.getType());
		cm.setType("other");
		assertEquals("other", cm.getType());
	}
	
	@Test
	public void testTypeAndSubType () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		cm.setSubType("scPrefix:scValue");
		assertEquals("fmt", cm.getType());
		assertEquals("scPrefix:scValue", cm.getSubType());
	}

	@Test (expected=InvalidParameterException.class)
	public void testBadSubTypeValues1 () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		cm.setSubType(":value");
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testBadSubTypeValues2 () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		cm.setSubType("my:");
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testBadSubTypeValues3 () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		cm.setSubType(":");
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testBadSubTypeValues4 () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		cm.setSubType("");
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testBadSubTypeValues5 () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		cm.setSubType("xlf:_badvalue_");
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testBadTypeValue () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("fmt");
		cm.setSubType("xlf:var");
		cm.verifyTypeSubTypeValues();
	}
	
	public void testTypeSubTypeValues () {
		CTag cm = new CTag(TagType.STANDALONE, "1", null);
		cm.setType("ui");
		cm.setSubType("xlf:var"); cm.verifyTypeSubTypeValues();
		cm.setType("fmt");
		cm.setSubType("xlf:lb"); cm.verifyTypeSubTypeValues();
		cm.setSubType("xlf:pb"); cm.verifyTypeSubTypeValues();
		cm.setSubType("xlf:b"); cm.verifyTypeSubTypeValues();
		cm.setSubType("xlf:i"); cm.verifyTypeSubTypeValues();
		cm.setSubType("xlf:u"); cm.verifyTypeSubTypeValues();
		cm.setSubType("abc:xyz"); cm.verifyTypeSubTypeValues();
		cm.setType("fmt"); cm.verifyTypeSubTypeValues();
	}
	
	@Test
	public void testCopyConstructor () {
		CTag cm1 = new CTag(TagType.STANDALONE, "1", "data");
		cm1.setCanCopy(false);
		cm1.setCanDelete(false);
		cm1.setCanOverlap(true);
		cm1.setCanReorder(CanReorder.NO);
		cm1.setDisp("disp");
		cm1.setEquiv("equiv");
		cm1.setDataRef("dataRef");
		cm1.setSubFlows("sf1");
		
		// Create a deep clone and compare
		CTag cm2 = new CTag(cm1, (CTag)null);

		assertEquals(cm2, cm1);
		assertNotSame(cm2, cm1);

		Tag bm1 = CloneFactory.create(cm1, null);
		assertEquals(bm1, cm1);
		assertNotSame(bm1, cm1);
	}

	@Test
	public void testClonefactory () {
		Unit unit1 = new Unit("id1");
		Fragment frag1 = unit1.appendSegment().getSource();
		CTag co1 = frag1.openCodeSpan("c1", "[c1]");
		MTag mo1 = frag1.openMarkerSpan("m1", "x:m1");
		frag1.closeCodeSpan("c1", "[/c1]");
		frag1.closeMarkerSpan("m1");

		Unit unit2 = new Unit("id2");
		Fragment frag2 = unit2.appendSegment().getSource();
		frag2.append(frag1); // Cloning occurs here
		assertNotSame(frag1.getTags(), frag2.getTags());
		
		CTag co2 = (CTag)frag2.getTags().get("c1", TagType.OPENING);
		CTag cc2 = (CTag)frag2.getTags().get("c1", TagType.CLOSING);
		assertSame(co2.cc, cc2.cc); // New code opening and closing shared their cc
		assertEquals(co1, co2); // Original and new are equals
		assertNotSame(co1, co2); // But they are not the same
		assertNotSame(co1.cc, co2.cc); // And do not share their cc
		// One more test to check that changing one doesn't update the other
		assertEquals(co1.getCanDelete(), co2.getCanDelete());
		co2.setCanDelete(false);
		assertNotEquals(co1.getCanDelete(), co2.getCanDelete());
		
		MTag mo2 = (MTag)frag2.getTags().get("m1", TagType.OPENING);
		MTag mc2 = (MTag)frag2.getTags().get("m1", TagType.CLOSING);
		assertSame(mo2.mc, mc2.mc); // New marker opening and closing shared their mc
		assertEquals(mo1, mo2); // Original and new are equals
		assertNotSame(mo1, mo2); // But they are not the same
		assertNotSame(mo1.mc, mo2.mc); // And do not share their mc
		// One more test to check that changing one doesn't update the other
		assertSame(mo1.getTranslate(), mo2.getTranslate());
		mo2.setTranslate(false);
		assertNotSame(mo1.getTranslate(), mo2.getTranslate());
	}

}
