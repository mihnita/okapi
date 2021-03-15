package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.its.AnnotatorsRef;
import net.sf.okapi.lib.xliff2.its.DataCategories;
import net.sf.okapi.lib.xliff2.its.ITSWriter;
import net.sf.okapi.lib.xliff2.its.MTConfidence;
import net.sf.okapi.lib.xliff2.its.TermTag;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class FragmentTest {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testSimpleFragment () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "text");
		assertEquals("text", frag.toString());
		assertFalse(frag.isTarget());
	}

	@Test
	public void testSimpleTargetFragment () {
		Fragment frag = new Fragment(new Unit("id").getStore(), true);
		assertEquals("", frag.toString());
		assertTrue(frag.isTarget());
	}

	@Test
	public void testInterFragmentCopy () {
		Fragment frag1 = new Fragment(new Unit("u1").getStore(), false);
		frag1.appendCode("c1", "[A1/]");
		frag1.append(" aaa");
		Unit unit = new Unit("u2");
		Fragment frag2 = new Fragment(unit.getStore(), false);
		frag2.appendCode("c2", "[B1/]");
		frag2.append(" bbb ");
		
		assertEquals(1, frag2.getStore().getSourceTags().size());
		frag2.append(frag1);
		assertEquals("<ph id=\"c2\"/> bbb <ph id=\"c1\"/> aaa", frag2.toXLIFF());
		assertEquals(2, frag2.getStore().getSourceTags().size());
		
// TEST id resolution		
	}

	@Test
	public void testAppend () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "text1");
		assertEquals("text1", frag.toString());
		frag.append("text2");
		assertEquals("text1text2", frag.toString());
		frag.append('3');
		assertEquals("text1text23", frag.toString());
	}

	@Test
	public void testCloseAnnotation () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "text1");
		MTag am = frag.openMarkerSpan("m1", "abc:ann");
		am.setRef("ref");
		am.setTranslate(false);
		am.setValue("val");
		frag.append("tex");
		am = frag.closeMarkerSpan("m1");
		assertEquals("abc:ann", am.getType());
		assertEquals("ref", am.getRef());
		assertEquals("val", am.getValue());
		assertFalse(am.getTranslate());
	}

	@Test
	public void testCodesNoData () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "<elem atrr='&amp;'>", false);
		frag.append("text");
		frag.append(TagType.CLOSING, "1", "</elem>", false);
		frag.appendCode("2", "<br/>");
		assertEquals("<pc id=\"1\">text</pc><ph id=\"2\"/>", frag.toXLIFF());
	}
	
	@Test
	public void testCodesNoDataNotWellFormed1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "[o1]", true);
		frag.append("t1");
		frag.append(TagType.OPENING, "2", "[o2]", true);
		frag.append("t2");
		frag.append(TagType.CLOSING, "1", "[c1]", true);
		frag.appendCode("3", "<br/>");
		frag.append("t3");
		frag.append(TagType.CLOSING, "2", "[c2]", true);
		assertEquals("<sc id=\"1\"/>t1<sc id=\"2\"/>t2<ec startRef=\"1\"/><ph id=\"3\"/>t3<ec startRef=\"2\"/>",
			frag.toXLIFF());
	}

	@Test
	public void testCodesNoDataNotWellFormed2 () {
		// Allow non-well-formed inside well-formed
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "[o1]", true);
		frag.append("t1");
		frag.append(TagType.OPENING, "2", "[o2]", true);
		frag.append("t2");
		frag.append(TagType.OPENING, "3", "[o3]", true);
		frag.append("t3");
		frag.append(TagType.CLOSING, "2", "[c2]", true);
		frag.append("t4");
		frag.append(TagType.CLOSING, "3", "[c3]", true);
		frag.append("t5");
		frag.append(TagType.CLOSING, "1", "[c1]", true);
		assertEquals("<pc id=\"1\" canOverlap=\"yes\">t1<sc id=\"2\"/>t2<sc id=\"3\"/>t3<ec startRef=\"2\"/>t4<ec startRef=\"3\"/>t5</pc>",
			frag.toXLIFF());
	}

	@Test
	public void testSimpleMarkers () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		MTag ann = frag.openMarkerSpan("a1", "comment");
		ann.setValue("my comment");
		frag.append("t1 ");
		frag.closeMarkerSpan(ann.getId());
		frag.append("t2.");
		assertEquals("<mrk id=\"a1\" type=\"comment\" value=\"my comment\">t1 </mrk>t2.", frag.toXLIFF());
	}
	
	@Test
	public void testAnnotate () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "Word word.");
		frag.annotate(5, 9, "abc:type", "myValue", "myRef");
		assertEquals("Word <mrk id=\"1\" type=\"abc:type\" value=\"myValue\" ref=\"myRef\">word</mrk>.", frag.toXLIFF());
	}
	
	@Test
	public void testAnnotateUsingANote () {
		Unit unit = new Unit("id");
		Fragment frag = new Fragment(unit.getStore(), false, "Word ");
		frag.append(TagType.OPENING, "1", "<b>", false);
		frag.append("word");
		frag.append(TagType.CLOSING, "1", "</b>", false);
		frag.append(".");
		Note note = frag.annotateWithNote(0, -1, "Text of the note");
		assertEquals("Text of the note", note.getText());
		assertEquals("<mrk id=\"2\" type=\"comment\" ref=\"#n="+note.getId()+"\">Word <pc id=\"1\">word</pc>.</mrk>",
			frag.toXLIFF());
		assertSame(unit.getNotes().get(0), note);
	}
	
	@Test
	public void testAnnotateTerm () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "Word word.");
		// test 'term'
		frag.annotate(5, 9, "term", "myValue", "myRef");
		MTag ann = (MTag)frag.getStore().getSourceTags().get(U.kOA(0));
		assertTrue(ann instanceof TermTag); // Check if we changed the class
		// test 'its:term-no'
		frag.annotate(5+2, 9+2, "its:term-no", null, null);
		ann = (MTag)frag.getStore().getSourceTags().get(U.kOA(1));
		assertEquals("2", ann.getId());
		assertTrue(ann instanceof TermTag); // Check if we changed the class
		// Output
		assertEquals("Word <mrk id=\"1\" type=\"term\" value=\"myValue\" ref=\"myRef\">"
			+ "<mrk id=\"2\" type=\"its:term-no\">word</mrk></mrk>.", frag.toXLIFF());
	}
	
	@Test
	public void testAnnotateFindExisting () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "Word word.");
		//                                                              0123456789
		MTag first = new MTag("m1", "my:first");
		frag.annotate(5, 9, first);
		// "Word ##word##."
		//  01234567890123
		int len = frag.getCodedText().length();
		
		MTag am1 = frag.getOrCreateMarker(7, 11, null, "my:type"); // start inside, end inside
		am1.getExtAttributes().setAttribute("myNS", "a1", "v1");
		assertSame(first, am1);
		assertEquals(len, frag.getCodedText().length());
		
		MTag am2 = frag.getOrCreateMarker(7, 13, null, "my:type"); // start inside, end outside
		am2.getExtAttributes().setAttribute("myNS", "a2", "v2");
		assertSame(first, am2);

		MTag am3 = frag.getOrCreateMarker(5, 11, null, "my:type"); // start outside, end inside
		am3.getExtAttributes().setAttribute("myNS", "a3", "v3");
		assertSame(first, am3);
		
		MTag am4 = frag.getOrCreateMarker(5, 13, null, "my:type"); // start outside, end outside
		am4.getExtAttributes().setAttribute("myNS", "a4", "v4");
		assertSame(first, am4);
		
		assertEquals("Word <mrk id=\"m1\" type=\"my:first\" xmlns:x1=\"myNS\" "
			+ "x1:a1=\"v1\" x1:a2=\"v2\" x1:a3=\"v3\" x1:a4=\"v4\">word</mrk>.", frag.toXLIFF());
	}
	
	@Test
	public void testAnnotateAllFindExisting () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "Word word.");
		//                                                              0123456789
		MTag first = new MTag("m1", "my:first");
		frag.annotate(0, -1, first);
		int len = frag.getCodedText().length();
		
		MTag am1 = frag.getOrCreateMarker(0, -1, null, "my:type"); // Same range
		am1.getExtAttributes().setAttribute("myNS", "a1", "v1");
		assertSame(first, am1);
		assertEquals(len, frag.getCodedText().length());

		assertEquals("<mrk id=\"m1\" type=\"my:first\" xmlns:x1=\"myNS\" x1:a1=\"v1\">Word word.</mrk>",
			frag.toXLIFF());
	}
	
	@Test
	public void testAnnotateNoExisting () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "Word word.");
		//                                                              0123456789
		MTag first = new MTag("m1", "my:first");
		frag.annotate(5, 9, first);
		// "Word ##word##."
		//  01234567890123
		int len = frag.getCodedText().length();
		
		MTag am1 = frag.getOrCreateMarker(7, 10, null, "my:type"); // start inside, end short
		am1.getExtAttributes().setAttribute("myNS", "a1", "v1");
		assertNotSame(first, am1);
		assertEquals(len+4, frag.getCodedText().length());
		
		assertEquals("Word <mrk id=\"m1\" type=\"my:first\">"
			+ "<mrk id=\"1\" type=\"my:type\" xmlns:x1=\"myNS\" x1:a1=\"v1\">wor</mrk>d</mrk>.", frag.toXLIFF());
	}
	
	@Test
	public void testAnnotateWithInlineCodes () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false, "Word ");
		frag.append(TagType.OPENING, "1", "<b>", false);
		frag.append("word");
		frag.append(TagType.CLOSING, "1", "</b>", false);
		frag.append(".");
		// "Word ##word##."
		//  01234567890123
		frag.annotate(7, 7+"word".length(), "abc:type", "myValue", "myRef");
		assertEquals("Word <pc id=\"1\"><mrk id=\"2\" type=\"abc:type\" value=\"myValue\" ref=\"myRef\">word</mrk></pc>.",
			frag.toXLIFF());
	}
	
	@Test
	public void testMarkersNotWellFormed1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.openMarkerSpan("1", "term");
		frag.append("t1");
		frag.append(TagType.OPENING, "2", "[c2]", false);
		frag.closeMarkerSpan("1");
		frag.append(TagType.CLOSING, "2", "[/c2]", false);
		assertEquals("<sm id=\"1\" type=\"term\"/>t1<sc id=\"2\" canOverlap=\"no\"/><em startRef=\"1\"/><ec startRef=\"2\" canOverlap=\"no\"/>",
			frag.toXLIFF());
	}

	@Test
	public void testCodesDataOutside () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "<elem atrr='&amp;'>", false);
		frag.append("text");
		frag.append(TagType.CLOSING, "1", "</elem>", false);
		frag.appendCode("2", "<br/>");
		
		assertEquals("<pc id=\"1\" dataRefEnd=\"d2\" dataRefStart=\"d1\">text</pc><ph id=\"2\" dataRef=\"d3\"/>",
			frag.toXLIFF(null, null, true));
	}

	@Test
	public void testOpeningIsolatedCodes () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "[o1]", true);
		frag.append("t1");
		assertEquals("<sc id=\"1\" isolated=\"yes\"/>t1", frag.toXLIFF());
		assertEquals("<sc id=\"1\" isolated=\"yes\" dataRef=\"d1\"/>t1", frag.toXLIFF(null, null, true));
	}

	@Test
	public void testclosingIsolatedCodes () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.CLOSING, "1", "[c1]", true);
		frag.append("t1");
		assertEquals("<ec id=\"1\" isolated=\"yes\"/>t1", frag.toXLIFF());
		assertEquals("<ec id=\"1\" isolated=\"yes\" dataRef=\"d1\"/>t1", frag.toXLIFF(null, null, true));
	}

	@Test
	public void testInvalidChars () {
		char[] chars = Character.toChars(0x10001);
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "[\u0002"+chars[0]+chars[1]+"\uFFFF]", true);
		frag.append("\u001a\u0002\t\n\u0020\uD7FF\u0019"+chars[0]+chars[1]+"\uFFFF");
		frag.append(TagType.CLOSING, "1", "[/\u0002"+chars[0]+chars[1]+"\uFFFF]", true);
		
		assertEquals("<pc id=\"1\" canOverlap=\"yes\" dataRefEnd=\"d2\" dataRefStart=\"d1\">"
			+ "<cp hex=\"001A\"/><cp hex=\"0002\"/>\t\n\u0020\uD7FF<cp hex=\"0019\"/>"+chars[0]+chars[1]+"<cp hex=\"FFFF\"/>"
			+ "</pc>",
			frag.toXLIFF(null, null, true));
	}

	@Test
	public void testCodesDataOutsideWithReuse () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "<elem atrr='&amp;'>", false);
		frag.append("t1");
		frag.append(TagType.CLOSING, "1", "</elem>", false);
		frag.appendCode("2", "<br/>");
		frag.append(TagType.OPENING, "3", "<elem atrr='&amp;'>", false);
		frag.append("t2");
		frag.append(TagType.CLOSING, "3", "</elem>", false);
		frag.appendCode("4", "<br/>");
		
		assertEquals("<pc id=\"1\" dataRefEnd=\"d2\" dataRefStart=\"d1\">t1</pc><ph id=\"2\" dataRef=\"d3\"/>"
			+ "<pc id=\"3\" dataRefEnd=\"d2\" dataRefStart=\"d1\">t2</pc><ph id=\"4\" dataRef=\"d3\"/>",
			frag.toXLIFF(null, null, true));
	}

	@Test
	public void testEquals1 () {
		Fragment frag1 = new Fragment(new Unit("id1").getStore(), false); // Source
		frag1.append(TagType.OPENING, "1", "[1]", true);
		frag1.append("text with \u0305");
		frag1.append(TagType.CLOSING, "1", "[/1]", true);
		frag1.appendCode("2", "[2/]");

		Fragment frag2 = new Fragment(new Unit("id2").getStore(), true); // Target
		frag2.append(TagType.OPENING, "1", "[1]", true);
		frag2.append("text with \u0305");
		frag2.append(TagType.CLOSING, "1", "[/1]", true);
		frag2.appendCode("2", "[2/]");

		assertTrue(frag1.equals(frag2));
	}
	
	@Test
	public void testEquals2 () {
		Fragment frag1 = new Fragment(new Unit("id1").getStore(), false);
		Fragment frag2 = new Fragment(new Unit("id2").getStore(), false);
		assertTrue(frag1.equals(frag2));
	}
	
	@Test
	public void testEquals3 () {
		Fragment frag1 = new Fragment(new Unit("id1").getStore(), false, "text");
		Fragment frag2 = new Fragment(new Unit("id2").getStore(), false, "text");
		assertTrue(frag1.equals(frag2));
	}

	@Test
	public void testEquals4 () {
		Fragment frag1 = new Fragment(new Unit("id1").getStore(), false);
		frag1.append(TagType.OPENING, "c1", "<b>", true);
		Fragment frag2 = new Fragment(new Unit("id2").getStore(), false);
		frag2.append(TagType.OPENING, "c2", "<b>", true);
		assertFalse(frag1.equals(frag2)); // Marker IDs are different
	}
	
	@Test
	public void testEquals5 () {
		Fragment frag1 = new Fragment(new Unit("id1").getStore(), false);
		frag1.append(TagType.OPENING, "c1", "<b>", true);
		Fragment frag2 = new Fragment(new Unit("id2").getStore(), false);
		frag2.openMarkerSpan("c1", null); // Try with same ID (even if it's not valid)
		assertFalse(frag1.equals(frag2)); // Code vs annotation
	}
	
	@Test
	public void testIterator1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append("abc");
		frag.append(TagType.OPENING, "1", "[1]", true);
		frag.append("d");
		frag.append(TagType.CLOSING, "1", "[/1]", true);
		frag.appendCode("2", "[2/]");
		frag.append("ef");
		frag.annotate(11, 12, MTag.TYPE_DEFAULT, "val", null);
		// "abc##d####e##f##"

		int i=0;
		for ( Object obj : frag ) {
			switch ( i ) {
			case 0: // abc
				assertEquals("abc", obj);
				break;
			case 1: // [1]
				assertEquals("[1]", ((CTag)obj).getData());
				break;
			case 2: // d
				assertEquals("d", obj);
				break;
			case 3: // [/1]
				assertEquals("[/1]", ((CTag)obj).getData());
				break;
			case 4: // [2/]
				assertEquals("[2/]", ((CTag)obj).getData());
				break;
			case 5: // e
				assertEquals("e", obj);
				break;
			case 6: // marker id=3
				assertEquals("val", ((MTag)obj).getValue());
				break;
			case 7: // f
				assertEquals("f", obj);
				break;
			case 8: // end marker
				assertEquals(TagType.CLOSING, ((MTag)obj).getTagType());
				assertEquals("val", ((MTag)obj).getValue());
				break;
			default: // We should not get here
				fail();
			}
			i++;
		}
	}

	@Test
	public void testIterator2 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append(TagType.OPENING, "1", "[1]", true);
		frag.append(TagType.CLOSING, "1", "[/1]", true);
		//[][]

		int i=0;
		CTag cm;
		for ( Object obj : frag ) {
			switch ( i ) {
			case 0: // [1]
				cm = (CTag)obj;
				assertEquals("[1]", cm.getData());
				break;
			case 1: // [/1]
				cm = (CTag)obj;
				assertEquals("[/1]", cm.getData());
				break;
			default: // We should not get here
				assertNull("done");
			}
			i++;
		}
	}

	@Test
	public void testIterator3 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append("");
		// Nothing
		for ( Object obj : frag ) {
			assertNull(obj); // We should not get here
		}
	}

	@Test
	public void testIteratorForTag1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		Iterator<Tag> iter = frag.getIterable(Tag.class).iterator();
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void testIteratorForTag2 () {
		Fragment frag = createFragment();
		// "Text ##in ##bold#### ######."
		int i = 0;
		for ( Tag tag : frag.getIterable(Tag.class) ) {
			switch ( i ) {
			case 0: // <b>
				assertEquals("1", tag.getId());
				assertEquals("<b>", ((CTag)tag).getData());
				break;
			case 1: // start annotation 
				assertEquals("3", tag.getId());
				assertEquals("comment", tag.getType());
				break;
			case 2: // end annotation
				assertEquals("3", tag.getId());
				assertEquals("comment", tag.getType());
				break;
			case 3: // </b>
				assertEquals("1", tag.getId());
				assertEquals("</b>", ((CTag)tag).getData());
				break;
			case 4: // start m1
				assertEquals("m1", tag.getId());
				assertEquals(false, ((MTag)tag).getTranslate());
				break;
			case 5: // standalone
				assertEquals("2", tag.getId());
				assertEquals("<br>", ((CTag)tag).getData());
				break;
			case 6: // end m1
				assertEquals("m1", tag.getId());
				assertEquals(false, ((MTag)tag).getTranslate());
				break;
			}
			i++;
		}
		assertEquals(7, i);
	}
	
	@Test
	public void testIteratorForCTag1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		Iterator<CTag> iter = frag.getIterable(CTag.class).iterator();
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void testIteratorForCTag2 () {
		Fragment frag = createFragment();
		// "Text ##in ##bold#### ######."
		int i = 0;
		for ( CTag tag : frag.getIterable(CTag.class) ) {
			switch ( i ) {
			case 0: // <b>
				assertEquals("1", tag.getId());
				assertEquals("<b>", tag.getData());
				break;
			case 1: // </b>
				assertEquals("1", tag.getId());
				assertEquals("</b>", tag.getData());
				break;
			case 2: // standalone
				assertEquals("2", tag.getId());
				assertEquals("<br>", tag.getData());
				break;
			}
			i++;
		}
		assertEquals(3, i);
	}
	
	@Test
	public void testIteratorForMTag1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		Iterator<MTag> iter = frag.getIterable(MTag.class).iterator();
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void testIteratorForMTag2 () {
		Fragment frag = createFragment();
		// "Text ##in ##bold#### ######."
		int i = 0;
		for ( MTag tag : frag.getIterable(MTag.class) ) {
			switch ( i ) {
			case 0: // start annotation 
				assertEquals("3", tag.getId());
				assertEquals("comment", tag.getType());
				break;
			case 1: // end annotation
				assertEquals("3", tag.getId());
				assertEquals("comment", tag.getType());
				break;
			case 2: // start m1
				assertEquals("m1", tag.getId());
				assertEquals(false, tag.getTranslate());
				break;
			case 3: // end m1
				assertEquals("m1", tag.getId());
				assertEquals(false, tag.getTranslate());
				break;
			}
			i++;
		}
		assertEquals(4, i);
	}
	
	@Test
	public void testIteratorForObject1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		Iterator<Object> iter = frag.getIterable(Object.class).iterator();
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void testIteratorForObject2 () {
		Fragment frag = createFragment();
		// "Text ##in ##bold#### ######."
		int i = 0;
		for ( Object obj : frag.getIterable(Object.class) ) {
			switch ( i ) {
			case 0: // "Text "
				assertEquals("Text ", obj);
				break;
			case 1: // <b>
				assertEquals("<b>", ((CTag)obj).getData());
				break;
			case 2: // "in "
				assertEquals("in ", obj);
				break;
			case 3: // start annotation 
				assertEquals("comment", ((MTag)obj).getType());
				break;
			case 4: // "bold"
				assertEquals("bold", obj);
				break;
			case 5: // end annotation
				assertEquals("comment", ((MTag)obj).getType());
				break;
			case 6: // </b>
				assertEquals("</b>", ((CTag)obj).getData());
				break;
			case 7: // " "
				assertEquals(" ", obj);
				break;
			case 8: // start m1
				assertEquals(false, ((MTag)obj).getTranslate());
				break;
			case 9: // standalone
				assertEquals("<br>", ((CTag)obj).getData());
				break;
			case 10: // end m1
				assertEquals(false, ((MTag)obj).getTranslate());
				break;
			case 11: // "."
				assertEquals(".", obj);
				break;
			}
			i++;
		}
		assertEquals(12, i);
	}

	@Test
	public void testIteratorForString1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		Iterator<String> iter = frag.getIterable(String.class).iterator();
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void testIteratorForString2 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		frag.append("abc");
		int i = 0;
		for ( String str : frag.getIterable(String.class) ) {
			assertEquals("abc", str);
			i++;
		}
		assertEquals(1, i);
	}
	
	@Test
	public void testIteratorForString3 () {
		Fragment frag = createFragment();
		// "Text ##in ##bold#### ######."
		int i = 0;
		for ( String str : frag.getIterable(String.class) ) {
			switch ( i ) {
			case 0: // "Text "
				assertEquals("Text ", str);
				break;
			case 1: // "in "
				assertEquals("in ", str);
				break;
			case 2: // "bold"
				assertEquals("bold", str);
				break;
			case 3: // " "
				assertEquals(" ", str);
				break;
			case 4: // "."
				assertEquals(".", str);
				break;
			}
			i++;
		}
		assertEquals(5, i);
	}

	@Test
	public void testIteratorForPTag1 () {
		Fragment frag = new Fragment(new Unit("id").getStore(), false);
		Iterator<PCont> iter = frag.getIterable(PCont.class).iterator();
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void testIteratorForPTag2 () {
		Unit unit = new Unit("id");
		Fragment frag = unit.appendSegment().getSource();
		frag.append("Text code text code text");
		frag.getOrCreateMarker(5, 19, null, "generic").setTranslate(false);
		// --> "Text ##code text code## text"
		frag.getOrCreateMarker(12, 16, null, "generic").setTranslate(true);
		// We have now nested markers: all 'code' are set to translate='no' 
		assertEquals("Text {om:1}code {om:2}text{cm:2} code{cm:1} text", U.fmtWithIDs(frag));
		unit.hideProtectedContent();
		int i = 0;
		for ( PCont ptag : frag.getIterable(PCont.class) ) {
			if ( i == 0 ) {
				assertEquals("{oA}code ", U.fmtMarkers(ptag.getCodedText()));
			}
			else {
				assertEquals(" code{cA}", U.fmtMarkers(ptag.getCodedText()));
			}
			i++;
		}
	}
	
	@Test
	public void testCopyConstructor () {
		Fragment frag1 = new Fragment(new Unit("id1").getStore(), false);
		CTag cm1 = frag1.append(TagType.OPENING, "1", "[1]", true);
		frag1.append("text");
		frag1.append(TagType.CLOSING, "1", "[/1]", true);
		cm1.setType("fmt");
		ExtAttribute at1 = cm1.getExtAttributes().setAttribute("myNS", "attr", "val1");
		
		Unit unit2 = new Unit("id2");
		Fragment frag2 = new Fragment(frag1, unit2.getStore(), true);

		assertEquals(frag2, frag1);
		assertNotSame(frag1, frag2);
		assertEquals(frag1.getStore().getSourceTags().get(U.kOC(0)), frag2.getStore().getTargetTags().get(U.kOC(0)));
		assertNotSame(frag1.getStore().getSourceTags().get(U.kOC(0)), frag2.getStore().getTargetTags().get(U.kOC(0)));
		assertFalse(frag1.isTarget());
		assertTrue(frag2.isTarget());
		assertSame(frag2.getStore(), unit2.getStore());
		CTag cm2 = (CTag)frag2.getTag(U.kOC(0));
		assertEquals(cm1.getType(), cm2.getType());
		assertNotSame(cm1, cm2);
		ExtAttribute at2 = cm2.getExtAttributes().getAttribute("myNS", "attr");
		assertEquals(at1.getValue(), at2.getValue());
		assertNotSame(at1, at2);
	}

	@Test
	public void testCopyConstructorWithCollapsedSpan () {
		Unit unit = new Unit("id1");
		Fragment frag1 = unit.appendSegment().getSource();
		frag1.append(TagType.OPENING, "1", "[1]", true);
		frag1.openMarkerSpan("a1", MTag.TYPE_DEFAULT).setTranslate(false);
		frag1.append("no-trans");
		frag1.append(TagType.CLOSING, "1", "[/1]", true);
		frag1.closeMarkerSpan("a1");
		frag1.append("text");
		unit.hideProtectedContent();
		assertEquals("<sc id=\"1\"/><WARNING:HIDDEN-PROTECTED-CONTENT/>text",
			frag1.toXLIFF());
		
		Fragment frag2 = unit.getPart(0).getTarget(GetTarget.CLONE_SOURCE);
		unit.showProtectedContent();
		assertEquals("<sc id=\"1\"/><sm id=\"a1\" translate=\"no\"/>no-trans<ec startRef=\"1\"/><em startRef=\"a1\"/>text",
			frag2.toXLIFF());
		
	}

	@Test
	public void testReplaceFragment () {
		Unit unit = new Unit("id1");
		Fragment frag1 = unit.appendSegment().getSource();
		frag1.append(TagType.OPENING, "1", "[1]", true);
		frag1.append("text");
		frag1.append(TagType.CLOSING, "1", "[/1]", true);
		assertEquals(2, unit.getStore().getSourceTags().size());
		
		unit.getPart(0).setSource(new Fragment(unit.getStore(), false, "plain text"));
		assertEquals("plain text", unit.getPart(0).getSource().toString());
		//TODO: we should remove the unused markers
		assertEquals(2, unit.getStore().getSourceTags().size());
	}
	
	@Test
	public void testGetClosingPosition () {
		Unit unit = new Unit("id");
		Fragment frag = unit.appendSegment().getSource();
		frag.append(TagType.OPENING, "1", "[1]", true);
		frag.append(TagType.OPENING, "2", "[2]", true);
		frag.append("inside");
		frag.append(TagType.CLOSING, "1", "[/1]", true);
		frag.append(" outside.");
		// ####inside## outside
		// 01234567890123456789
		Tag opening = frag.getTag(U.kOC(0));
		assertEquals(10, frag.getClosingPosition(opening));
		opening = frag.getTag(U.kOC(1));
		assertEquals(-1, frag.getClosingPosition(opening));
	}
	
	@Test
	public void testRemoveMarker () {
		// Fragment must be part of a unit
		Unit unit = new Unit("id1");
		Fragment frag1 = unit.appendSegment().getSource();
		CTag cmo1 = frag1.append(TagType.OPENING, "1", "[1]", true);
		frag1.append("text1");
		CTag cmc1 = frag1.append(TagType.CLOSING, "1", "[/1]", true);
		CTag cmo2 = frag1.append(TagType.OPENING, "2", "[2]", true);
		Fragment frag2 = unit.appendSegment().getSource();
		frag2.append("text2");
		CTag cmc2 = frag2.append(TagType.CLOSING, "2", "[/2]", true);
		// Hidden case
		frag2.openMarkerSpan("m1", null).setTranslate(false);
		CTag ph = frag2.appendCode("ph1", "[ph1/]");
		frag2.append("protected");
		frag2.closeMarkerSpan("m1");

		assertEquals("text2{cc:2}{om:m1}{hc:ph1}protected{cm:m1}", U.fmtWithIDs(frag2));
		unit.hideProtectedContent();

		assertSame(cmo1, frag1.getTag(U.kOC(0)));
		frag1.remove(cmo1);
		assertSame(cmc1, frag1.getTag(U.kCC(0)));
		assertSame(cmo2, frag1.getTag(U.kOC(1)));
		assertSame(cmc2, frag2.getTag(U.kCC(1)));
		// Check if the index in the coded-text has been updated
		String ct = frag1.getCodedText();
		char ch1 = ct.charAt("text1".length());
		assertTrue(Fragment.isChar1(ch1));
		char ch2 = ct.charAt("text1".length()+1);
		CTag cm = (CTag)frag1.getTag(Fragment.toKey(ch1, ch2));
		assertSame(cmc1, cm);
		
		// Remove the hidden tag
		assertEquals("text2{cc:2}{$}", U.fmtWithIDs(frag2));
		frag2.remove(ph);
		frag2.showProtectedContent();
		assertEquals("text2{cc:2}{om:m1}protected{cm:m1}", U.fmtWithIDs(frag2));
	}

	@Test
	public void testClear () {
		// Fragment must be part of a unit
		Unit unit = new Unit("id1");
		Fragment frag = unit.appendSegment().getSource();
		frag.append(TagType.OPENING, "1", "[1]", true);
		frag.append("t1");
		MTag sm = frag.openMarkerSpan("m1", MTag.TYPE_DEFAULT);
		sm.setTranslate(false);
		frag.append(TagType.CLOSING, "1", "[/1]", true);
		frag.append("t2");
		frag.append(TagType.OPENING, "2", "[2]", true);
		frag.closeMarkerSpan(sm.getId());
		frag.append(TagType.STANDALONE, "3", "[3/]", true);
		frag.append("t3");
		assertEquals(6, frag.getTags().size());
		assertEquals("{oC}t1{oA}{cC}t2{oC}{cA}{hC}t3", U.fmtMarkers(frag.getCodedText()));
		unit.hideProtectedContent();
		assertEquals(6, frag.getTags().size());
		assertEquals("{oC}t1{$}{hC}t3", U.fmtMarkers(frag.getCodedText()));
		assertFalse(frag.isEmpty());
		frag.clear();
		assertTrue(frag.isEmpty());
		assertEquals(0, frag.getTags().size());
		assertEquals("", frag.getCodedText());
	}

	@Test
	public void testPlainTextPositionConversion () {
		Unit unit = new Unit("id1");
		Fragment frag = unit.appendSegment().getSource();
		frag.append(TagType.OPENING, "1", "[1]", true);
		frag.append("abc");
		frag.append(TagType.CLOSING, "1", "[/1]", true);
		frag.append("d");
		frag.appendCode("2", "[2/]");
		// ##abc##d##
		// 01234567890 L=10
		// ..012..3..4 L=4
		assertEquals(0, frag.getCodedTextPosition(0, true));
		assertEquals(2, frag.getCodedTextPosition(0, false));
		
		assertEquals(3, frag.getCodedTextPosition(1, true));
		assertEquals(3, frag.getCodedTextPosition(1, false));
		
		assertEquals(4, frag.getCodedTextPosition(2, true));
		assertEquals(4, frag.getCodedTextPosition(2, false));

		assertEquals(5, frag.getCodedTextPosition(3, true));
		assertEquals(7, frag.getCodedTextPosition(3, false));

		assertEquals(8, frag.getCodedTextPosition(4, true));
		assertEquals(10, frag.getCodedTextPosition(4, false));
		
		// Out by more than 1->coded-text length
		assertEquals(10, frag.getCodedTextPosition(5, true));
		assertEquals(10, frag.getCodedTextPosition(5, false));
	}
	
	@Test
	public void testPlainTextSpanConversion () {
		Unit unit = new Unit("id1");
		Fragment frag = unit.appendSegment().getSource();
		frag.append(TagType.OPENING, "1", "[1]", true);
		frag.append("abc");
		frag.append(TagType.CLOSING, "1", "[/1]", true);
		frag.append("d");
		frag.appendCode("2", "[2/]");
		// ##abc##d##
		// 01234567890 L=10
		// ..012..3..4 L=4
		String ct = frag.getCodedText();
		String pt = frag.getPlainText();
		// Excluding the markers
		int start = frag.getCodedTextPosition(0, false);
		int end = frag.getCodedTextPosition(3, true);
		assertEquals(ct.substring(start, end), pt.substring(0, 3));
		// Including the markers: "##abc##"
		start = frag.getCodedTextPosition(0, true);
		end = frag.getCodedTextPosition(3, false);
		assertEquals("{oC}abc{cC}", U.fmtMarkers(ct.substring(start, end)));
	}

	@Test
	public void testUnitWithNestedAnnotatorsRef () {
		Unit unit = new Unit("1");
		AnnotatorsRef unitAR = new AnnotatorsRef();
		unitAR.set(DataCategories.MTCONFIDENCE, "uLevel");
		unit.setAnnotatorsRef(unitAR);
		Fragment frag = unit.appendSegment().getSource();
		frag.append("part1 part2 part3 part4");
		ITSWriter.annotate(frag, 0, 5, new MTConfidence("p1", 0.1));
		// -> ##part1## part2 part3 part4
		ITSWriter.annotate(frag, 0, 15, new MTConfidence("p1p2", 0.2));
		// -> ####part1## part2## part3 part4
		ITSWriter.annotate(frag, 12, 25, new MTConfidence("p2p3", 0.3));
		// -> ####part1## ##part2## part3## part4
		//    | [_______] |       |       |
		//    [___________|_______]       |
		//                [_______________]
		ITSWriter.annotate(frag, 30, -1, new MTConfidence(null, 0.4));
		// -> ####part1## ##part2## part3## ##part4##
		//    | [_______] |       |       | [_______]
		//    [___________|_______]       |
		//                [_______________]

		String expected = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"1\" its:annotatorsRef=\"mt-confidence|uLevel\">\n"
			+ "<segment>\n<source>"
			+ "<sm id=\"2\" type=\"its:any\" its:mtConfidence=\"0.2\" its:annotatorsRef=\"mt-confidence|p1p2\"/>"
			+ "<mrk id=\"1\" type=\"its:any\" its:mtConfidence=\"0.1\" its:annotatorsRef=\"mt-confidence|p1\">part1</mrk> "
			+ "<sm id=\"3\" type=\"its:any\" its:mtConfidence=\"0.3\" its:annotatorsRef=\"mt-confidence|p2p3\"/>part2"
			+ "<em startRef=\"2\"/> part3<em startRef=\"3\"/> <mrk id=\"4\" type=\"its:any\" its:mtConfidence=\"0.4\">part4</mrk>"
			+ "</source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeUnit(unit, "en", "fr"));
		List<Event> events = U.getEvents(expected);
		unit = U.getUnit(events);
		List<AnnotatedSpan> spans = unit.getAnnotatedSpans(false);
		assertEquals(4, spans.size());
		assertEquals("2", spans.get(0).getId());
		assertEquals("p1p2", spans.get(0).getMarker().getITSItems().get(DataCategories.MTCONFIDENCE).getAnnotatorRef());
		assertEquals("1", spans.get(1).getId());
		assertEquals("p1", spans.get(1).getMarker().getITSItems().get(DataCategories.MTCONFIDENCE).getAnnotatorRef());
		assertEquals("3", spans.get(2).getId());
		assertEquals("p2p3", spans.get(2).getMarker().getITSItems().get(DataCategories.MTCONFIDENCE).getAnnotatorRef());
		assertEquals("4", spans.get(3).getId());
		assertEquals("uLevel", spans.get(3).getMarker().getITSItems().get(DataCategories.MTCONFIDENCE).getAnnotatorRef());
		
	}
	
	@Test
	public void testHasContentAfter () {
		Unit unit = new Unit("1");
		Fragment frag = unit.appendSegment().getSource();
		frag.append("abc");
		frag.appendCode("1", "<br/>");
		frag.openMarkerSpan("2", "comment");
		frag.closeMarkerSpan("2");
		// "abc######"
		assertTrue(Fragment.hasContentAfter(frag.getCodedText(), 0));
		assertTrue(Fragment.hasContentAfter(frag.getCodedText(), 3));
		assertFalse(Fragment.hasContentAfter(frag.getCodedText(), 5));
		assertFalse(Fragment.hasContentAfter(frag.getCodedText(), 7));
	}
	
	@Test
	public void testUnitWithOverlappingAnnotatorsRef () {
		Unit unit = new Unit("1");
		AnnotatorsRef unitAR = new AnnotatorsRef();
		unitAR.set(DataCategories.MTCONFIDENCE, "uLevel");
		unit.setAnnotatorsRef(unitAR);
		Fragment frag = unit.appendSegment().getSource();
		frag.append("part1 part2 part3 part4");
		ITSWriter.annotate(frag, 0, 17, new MTConfidence("p1p2p3", 0.1));
		// -> ##part1 part2 part3## part4
		ITSWriter.annotate(frag, 8, -1, new MTConfidence("uLevel", 0.2));
		// -> ##part1 ##part2 part3## part4##
		//    [_______|_____________]       |
		//            [_____________________]

		String expected = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"1\" its:annotatorsRef=\"mt-confidence|uLevel\">\n"
			+ "<segment>\n<source>"
			+ "<sm id=\"1\" type=\"its:any\" its:mtConfidence=\"0.1\" its:annotatorsRef=\"mt-confidence|p1p2p3\"/>part1 "
			+ "<sm id=\"2\" type=\"its:any\" its:mtConfidence=\"0.2\" its:annotatorsRef=\"mt-confidence|uLevel\"/>part2 part3"
			+ "<em startRef=\"1\"/> part4<em startRef=\"2\"/>"
			+ "</source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeUnit(unit, "en", "fr"));
	}

	@Test
	public void testAppends () {
		Fragment frag = new Unit("id").appendSegment().getSource();
		CTag ct1 = frag.openCodeSpan("b1", "<b>");
		ct1.setCanReorder(CanReorder.FIRSTNO);
		frag.append("bold");
		CTag ct2 = frag.closeCodeSpan("b1", "</b>");
		CTag ct3 = frag.appendCode("br1", "<br/>");
		
		assertEquals(ct1.getId(), ct2.getId());
		assertEquals("<b>", ct1.getData());
		assertFalse(ct1.getCanDelete()); // Was set automatically
		assertEquals("</b>", ct2.getData());
		assertEquals(CanReorder.NO, ct2.getCanReorder()); // Was set automatically
		assertSame(ct1, frag.getOpeningTag(ct2));
		assertEquals("br1", ct3.getId());
		assertEquals("<br/>", ct3.getData());
	}
	
	@Test
	public void testDelete () {
		Fragment frag = createFragment();
		// "Text ##in ##bold#### ######."
		frag.delete(0, 0);
		assertEquals("Text {oc:1}in {om:3}bold{cm:3}{cc:1} {om:m1}{hc:2}{cm:m1}.", U.fmtWithIDs(frag));
		frag.delete(18, 20); // "Text ##in ##bold##[##] ######."
		assertEquals("Text {oc:1}in {om:3}bold{cm:3} {om:m1}{hc:2}{cm:m1}.", U.fmtWithIDs(frag));
		frag.delete(5, 7); // "Text [##]in ##bold## ######."
		assertEquals("Text in {om:3}bold{cm:3} {om:m1}{hc:2}{cm:m1}.", U.fmtWithIDs(frag));
		assertEquals(5, frag.getTags().size());

		frag = createFragment();
		// "Text ##in ##bold#### ######."
		frag.delete(0, 28);
		assertTrue(frag.isEmpty());
		assertFalse(frag.hasTag());

		frag = createFragment();
		// "Text ##in ##bold#### ######."
		Unit unit = (Unit)frag.getStore().getParent();
		unit.hideProtectedContent();
		assertEquals("Text {oc:1}in {om:3}bold{cm:3}{cc:1} {$}.", U.fmtWithIDs(frag));
		frag.delete(14, 23); // "Text ##in ##bo[ld#### $$]."
		frag.showProtectedContent();
		assertEquals("Text {oc:1}in {om:3}bo.", U.fmtWithIDs(frag));
		assertEquals(2, frag.getTags().size());
	}
	
	@Test
	public void testOwnTagsStatus () {
		Fragment cont = new Fragment(new Unit("id").getStore(), true);
		Tag t1o = cont.openCodeSpan("1", "<1>"); // 2
		Tag t1c = cont.closeCodeSpan("1", "</1>"); // 2
		cont.appendCode("2", "<2/>"); // -
		Tag t3o = cont.openCodeSpan("3", "<3>"); // 1
		Tag t4o = cont.openCodeSpan("4", "<4>"); // 1
		Tag t3c = cont.closeCodeSpan("3", "</3>"); // 1
		Tag t5o = cont.openCodeSpan("5", "<5>"); // 2
		cont.openCodeSpan("6", "<6>"); // will be deleted
		Tag t6c = cont.closeCodeSpan("6", "</6>"); // will be 0
		Tag t5c = cont.closeCodeSpan("5", "</5>"); // 2
		Tag t4c = cont.closeCodeSpan("4", "</4>"); // 1
		Tag t7o = cont.openCodeSpan("7", "<7>"); // 0
		cont.delete(14, 16); // Delete t6o
		Map<Tag, Integer> map = cont.getOwnTagsStatus();
		assertEquals(2, (int)map.get(t1o));
		assertEquals(2, (int)map.get(t1c));
		assertEquals(1, (int)map.get(t3o));
		assertEquals(2, (int)map.get(t4o));
		assertEquals(1, (int)map.get(t3c));
		assertEquals(2, (int)map.get(t5o));
		assertEquals(0, (int)map.get(t6c));
		assertEquals(2, (int)map.get(t5c));
		assertEquals(2, (int)map.get(t4c));
		assertEquals(0, (int)map.get(t7o));
	}

	@Test
	public void generalDescription () {
		/* The main object used to represent the source and target content is the Fragment.
		 * It represent the content of either a segment or an non-segment. 
		 */
		Unit unit = new Unit("id");
		Fragment frag = unit.appendSegment().getSource();
		
		// Fragment implements Appendable to provide basic text operations
		frag.append("Text").append(' ').append("Just abc ", 5, 9);
		assertEquals("Text abc ", frag.toString());
		// You can also insert plain text
		frag.insert("with ", 5);
		assertEquals("Text with abc ", frag.toString());
		
		/* Each inline objects are represented in the coded text by a pair of special characters.
		 * The first character represent at the same time the type of tag (opening, closing or standalone
		 * and the type of object, i.e (original) code or (annotation) marker.
		 * Tags for codes are represented by CTag objects, while tags for markers are represented by MTag objects.
		 */
		// You can append opening and closing tags for codes:
		CTag opening = frag.openCodeSpan("c1", "<b>");
		frag.append("and bold");
		CTag closing = frag.closeCodeSpan("c1", "</b>");
		frag.append(". ");
		
		
		//=== Codes
		
		// Opening and closing tags are connected together (if they are not orphan)
		// and the fields they have in common are shared.
		// Changing a shared field from one tag affects the other tag.
		assertNull(opening.getType());
		assertNull(closing.getType());
		opening.setType("fmt");
		assertEquals("fmt", opening.getType());
		assertEquals("fmt", closing.getType());
		closing.setType("ui");
		assertEquals("ui", opening.getType());
		assertEquals("ui", closing.getType());

		/* Tags are shared across fragments.
		 * So opening and closing tags can span the full unit's content
		 */
		Fragment frag2 = unit.appendSegment().getSource();
		frag2.append(" And more...");
		/* Most methods to add tags to the fragment are helper methods that call insert().
		** That method has complex parameters, but allows to control exactly what to do. 
		*/
		// Let's append an orphan closing code in the new fragment 
		frag2.insert(TagType.CLOSING, "fmt", "c2", "</i>", -1, true, true);
		// Note: U.fmtWithIDs() is just a utility method for the tests, it shows the text
		// along with text representation of the tags (cc=closing code, cm=closing marker, etc.)
		assertEquals(" And more...{cc:c2}", U.fmtWithIDs(frag2));

		// Now let's insert (and connect) the opening tag for c2 into the previous segment
		// "Text with abc ##and bold##. "
		frag.insert(TagType.OPENING, "fmt", "c2", "<i>", 10, true, false);
		assertEquals("Text with {oc:c2}abc {oc:c1}and bold{cc:c1}. ", U.fmtWithIDs(frag));
		
		// We can also delete spans of the content
		// "Text with ##abc ##and bold##. "
		frag.delete(10, 16); // Deletes "##abc " from the fragment
		// "Text with ##and bold##. "
		frag.delete(12, 16); // Deletes "and " from the fragment
		assertEquals("Text with {oc:c1}bold{cc:c1}. ", U.fmtWithIDs(frag));
		
		/* Tags are accessible from the fragment. But keep in mind that the collection 
		 * is the one for all source (or target) tags in the unit, not just for this segment.
		 */
		assertEquals(3, frag.getTags().size()); // 2 codes in the first fragment, 1 in the second
		assertEquals(2, frag.getOwnTags().size()); // 2 codes in the first fragment

		
		//=== Markers
		
		/* Markers are a way to associate spans of content with
		 * some metadata: annotations.
		 * Like for codes, you can append them as you build a fragment.
		 */
		frag2.clear();
		frag2.append("Second ");
		frag2.openMarkerSpan("m1", MTag.TYPE_DEFAULT);
		frag2.append("Fragment");
		frag2.closeMarkerSpan("m1");
		frag2.append('.');
		assertEquals("Second {om:m1}Fragment{cm:m1}.", U.fmtWithIDs(frag2));

		// You can also annotate directly a span of content
		// Note that in this call the id of the new marker will be set automatically
		// And because we have not used auto-generated IDs yet it will be "1".
		frag2.annotate(0, -1, "my:annotation", "myData", "someURL");
		assertEquals("{om:1}Second {om:m1}Fragment{cm:m1}.{cm:1}", U.fmtWithIDs(frag2));

		/* In some cases you may want to re-use existing markers to add new metadata.
		 * There is an helper method for this: it can get an existing marker or create
		 * a new one if none was found for the given span.
		 * It is smart enough to deal with the various cases of overlapping boundaries. 
		 */
		// "##Second ##Fragment##.##"
		MTag mt = frag2.getOrCreateMarker(9, 19, MTag.TYPE_DEFAULT, "term");
		mt.setType("term");
		mt.setRef("http://en.wikipedia.org/wiki/Fragment");
		assertEquals("{om:1}Second {om:m1}Fragment{cm:m1}.{cm:1}", U.fmtWithIDs(frag2));
		MTag mt2 = frag2.getTags().getOpeningMTag("m1");
		assertSame(mt, mt2);
		

		//=== Coded text parsing

		// Add an annotation to have both code and marker in this fragment
		// "Text with ##bold##. "
		frag.annotate(12, 16, "term", "Has a strong appearance", "http://www.thefreedictionary.com/bold");

		logger.info("-- way 1:");
		String ct = frag.getCodedText();
		for ( int i=0; i<ct.length(); i++ ) {
			if ( Fragment.isChar1(ct.charAt(i)) ) {
				// A first way to access the tag:
				Tag tag = frag.getTags().get(ct, i);
				// From the tag you can access:
				// The type of tag (opening, closing, standalone), the type of the object, and its ID
				logger.info("tag-type:{}, type:{}, id:{}", tag.getTagType(), tag.getType(), tag.getId());
				if ( tag.isCode() ) logger.info(" It is a code.");
				if ( tag.isMarker() ) logger.info(" It is a marker.");
				
				// Another way is to use the key
				int key = Fragment.toKey(ct.charAt(i), ct.charAt(i+1));
				tag = frag.getTag(key);
				
				// Note that you can get the two special characters from the key
				// for example if you need to add a reference in the coded text
				String tagref = Fragment.toRef(key);
				assertEquals(tagref.charAt(0), ct.charAt(i));
				assertEquals(tagref.charAt(1), ct.charAt(i+1));
				
				// Skip over the second character of the reference
				i++; // Not doing this is OK too: that character is in the PUA
			}
		}
		
		/* Another way is to do a switch on the first character.
		 * This way you know what type of object the tag corresponds to before retrieving the object.
		 */
		logger.info("-- way 2:");
		for ( int i=0; i<ct.length(); i++ ) {
			switch ( ct.charAt(i) ) {
			case Fragment.CODE_OPENING:
			case Fragment.CODE_CLOSING:
			case Fragment.CODE_STANDALONE:
				// You can get directly the CTag object
				CTag ctag = frag.getCTag(ct, i++);
				logger.info("CTag id:{}, data:{}", ctag.getId(), ctag.getData());
				break;
			case Fragment.MARKER_OPENING:
			case Fragment.MARKER_CLOSING:
				MTag mtag = frag.getMTag(ct, i++);
				logger.info("MTag id:{}, type:{}", mtag.getId(), mtag.getType());
				break;
			case Fragment.PCONT_STANDALONE:
				// Hidden protected content
				i++;
				break;
			}
		}
		
		/* Yes, another way to process the content is to iterate over its objects
		 * You can access the string and tags directly based on their type.
		 */
		logger.info("-- way 3:");
		for ( Object obj : frag ) {
			if ( obj instanceof CTag ) {
				CTag ctag = (CTag)obj;
				logger.info("CTag id:{}, data:{}", ctag.getId(), ctag.getData());
			}
			else if ( obj instanceof MTag ) {
				MTag mtag = (MTag)obj;
				logger.info("MTag id:{} , type:{}", mtag.getId(), mtag.getType());
			}
			else if ( obj instanceof String ) {
				logger.info("Text:'{}'", obj);
			}
		}
		
		//=== Dealing with orphan tags
		
		/* Within the model orphan tags are treated just like other tags, they simply
		 * have no counterpart and share their common fields with no other tags.
		 * When doing an output where isolated tag need to be marked up in some way 
		 * one can simply check if the given tag has a counterpart.
		 * When deleting tags you do not have to update any opening/closing flags
		 * and there is no "re-balancing" to do.
		 * When adding tag, if you know the ID, you can connect existing tag with a new one. 
		 */
		// The variable frag2 has one orphan tag (the closing tag of a code with id=c2)
		frag2.clear();
		frag2.insert(TagType.CLOSING, null, "alone", "</B>", -1, false, true);
		Tag tag = frag2.getTags().get("alone", TagType.CLOSING);
		// It has no corresponding opening tag within this unit
		assertNull(frag2.getOpeningTag(tag));
		
		
		//=== Protected content
		
		/* One of the shortcomings of the current okapi inline model is that we use
		 * codes to store protected content. With XLIFF2, this is not really possible
		 * because the 'translate' property can be pretty much anywhere and nested.
		 * Besides, it is a big bonus to be able to hide/show the protected content.
		 * So for the solution selected here is to have methods to collapse protected
		 * content into a single special temporary standalone tag. 
		 */
		unit = new Unit("id");
		frag = unit.appendSegment().getSource();
		frag.append("Text code text code text");
		frag.getOrCreateMarker(5, 19, null, "generic").setTranslate(false);
		// --> "Text ##code text code## text"
		frag.getOrCreateMarker(12, 16, null, "generic").setTranslate(true);
		// We have now nested markers: all 'code' are set to translate='no' 
		assertEquals("Text {om:1}code {om:2}text{cm:2} code{cm:1} text", U.fmtWithIDs(frag));
		// We hide the protected content
		unit.hideProtectedContent();
		// It's denoted with {$} in this test output representation:
		assertEquals("Text {$}{om:2}text{cm:2}{$} text", U.fmtWithIDs(frag));
		// Let's put in upper-case all accessible text
		frag.setCodedText(frag.getCodedText().toUpperCase());
		// And expand back the protected text:
		frag.showProtectedContent();
		// None of the protected content was modified
		assertEquals("TEXT {om:1}code {om:2}TEXT{cm:2} code{cm:1} TEXT", U.fmtWithIDs(frag));
		

		//=== IDs
		
		/* The IDs for the inline objects are strings.
		 * One can specify them when creating the new tags, but you can also not specify them
		 * and let the store provide unique IDs for the store's scope. Those IDs are
		 * created by starting at "1" and incrementing.
		 */
		frag = new Unit("id").appendSegment().getSource();
		opening = frag.openCodeSpan(null, "<s>");
		assertEquals("1", opening.getId());
		closing = frag.closeCodeSpan("1", "</s>");
		assertEquals("1", closing.getId());

		// When ID are specified manually, no check is made for existing one
		// (Maybe we should???? But that would slow things)
		
		// But you can check manually if the ID is used
		int myId = 1;
		while ( frag.getStore().isIdUsed(""+myId) ) {
			myId++;
		}
		CTag standalone = frag.appendCode(""+myId, "<br/>");
		assertEquals("2", standalone.getId());
		
		// Or get auto-generated IDs
		standalone = frag.appendCode(frag.getStore().suggestId(false), "<br/>");
		assertEquals("3", standalone.getId());
		
		// Manual IDs values are not validated (they should be NMTOKEN in XLIFF)
		// (Maybe we should validate????
		assertFalse(Util.isValidNmtoken("ab][cd"));
		assertTrue(Util.isValidNmtoken("ab-12"));
		assertTrue(Util.isValidNmtoken("123"));
		
		
		//=== Cloning
		
		/* All objects of the inline content model can be cloned.
		 * This is done by using copy constructors (not clone())
		 */

		// In some cases, because tags are shared at the unit level, the constructors
		// need some extra parameters.
		frag2 = new Fragment(frag, frag2.getStore(), false);

		assertEquals(frag2, frag);
		// Get tags in the original
		CTag oc1 = (CTag)frag.getTags().get("1", TagType.OPENING);
		CTag cc1 = (CTag)frag.getClosingTag(oc1);
		// Get the same tags in the copy
		CTag oc2 = (CTag)frag2.getTags().get("1", TagType.OPENING);
		CTag cc2 = (CTag)frag2.getClosingTag(oc2);
		// Compare: The are identical but not the same ones
		assertEquals(oc2, oc1);
		assertNotSame(oc2, oc1);
		assertEquals(cc2, cc1);
		assertNotSame(cc2, cc1);
	}

//	@Test
//	public void testCodedTextWithNewTags () {
//		Unit unit = new Unit("id1");
//		Fragment frag1 = unit.appendNewSegment().getSource();
//		frag1.append(TagType.OPENING, "1", "[1]", true);
//		frag1.append(TagType.CLOSING, "1", "[/1]", true);
//		frag1.append(TagType.OPENING, "2", "[2]", true);
//		frag1.append(TagType.CLOSING, "2", "[/2]", true);
//
//		Markers newMarkers = new Markers(unit.getStore());
//		CMarker cmo1b = new CMarker(TagType.OPENING, "1b", "[1b]");
//		int keyCmo1b = newMarkers.add(cmo1b);
//		newMarkers.add(new CMarker(TagType.CLOSING, "1b", "[/1b]"));
//		newMarkers.add(new CMarker(TagType.OPENING, "2b", "[2b]"));
//		CMarker cmc2b = new CMarker(TagType.CLOSING, "2b", "[/2b]");
//		newMarkers.add(cmc2b);
//		
//		String ct = frag1.getCodedText();
//		//Not easy to do with new coded text
//		frag1.setCodedText("{{"+ct+"}}");
//		frag1.getStore().getSourceMarkers().reset(newMarkers);
//		assertEquals("1b", frag1.getMarker(keyCmo1b).getId());
//		assertEquals(false, frag1.getMarker(keyCmo1b)==cmo1b);
//		assertEquals("2b", frag1.getMarker(U.kCC(1)).getId());
//		assertEquals(false, frag1.getMarker(U.kCC(1))==cmc2b);
//		assertEquals("{{{oC}{cC}{oC}{cC}}}", Utilities.fmtMarkers(frag1.getCodedText()));
//	}

//	@Test
//	public void testProtectedContent () {
//		Unit unit = new Unit("id1");
//		Fragment frag = unit.appendNewSegment().getSource();
//		frag.append("yyy");
//		CTag cm = frag.append(TagType.OPENING, "s", "<s>", false);
//		frag.append("nnn");
//		frag.closeCodeSpan(cm.getId(), "</s>");
//		assertEquals("yyy<s>nnn</s>", frag.togetText());
//		((Unit)frag.getStore().getParent()).hideProtectedContent();
//		assertEquals("yyy<PC/>", frag.to.toText());
//		tf.getContainer().expandProtectedContent();
//		assertEquals("yyy<s>nnn</s>", tf.toText());
//	}

	private Fragment createFragment () {
		Fragment frag = new Unit("id").appendSegment().getSource();
		frag.append("Text ");
		frag.openCodeSpan("1", "<b>");
		frag.append("in bold");
		frag.closeCodeSpan("1", "</b>");
		frag.append(' ');
		frag.appendCode("2", "<br>");
		frag.append('.');
		// "Text ##in bold## ##."
		frag.annotate(10, 14, "comment", "My comment", null);
		// "Text ##in ##bold#### ##."
		MTag mtag = new MTag("m1", null); mtag.setTranslate(false);
		frag.annotate(21, 23, mtag);
		// "Text ##in ##bold#### ######."
		assertEquals("Text {oc:1}in {om:3}bold{cm:3}{cc:1} {om:m1}{hc:2}{cm:m1}.", U.fmtWithIDs(frag));
		return frag;
	}

}
