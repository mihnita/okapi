package net.sf.okapi.lib.xliff2.renderer;

import static org.junit.Assert.assertEquals;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XLIFFFragmentRendererTest {

	@Test
	public void testSimple () {
		Fragment frag = new Unit("id").appendSegment().getSource();
		frag.append("<Text ");
		CTag c = frag.openCodeSpan("1", "<b>");
		c.setDisp("[bold]");
		frag.append("in bold");
		c = frag.closeCodeSpan("1", "</b>");
		c.setDisp("[/bold]");
		frag.append(' ');
		frag.appendCode("2", "<br>");
		frag.append('.');
		// ">Text ##in bold## ##."
		frag.annotate(11, 15, "comment", "My comment", null);
		// ">Text ##in ##bold#### ##."
		MTag mtag = new MTag("m1", null); mtag.setTranslate(false);
		frag.annotate(22, 24, mtag);
		
		IFragmentRenderer r = new XLIFFFragmentRenderer(frag, null);
		int count = 0;
		for ( IFragmentObject fo : r ) {
			switch ( count ) {
			case 0:
				assertEquals("<Text ", fo.getText());
				assertEquals("&lt;Text ", fo.render());
				break;
			case 1:
				assertEquals(TagType.OPENING, fo.getCTag().getTagType());
				assertEquals("<pc id=\"1\" disp=\"[bold]\">", fo.render());
				assertEquals("<b>", fo.getCTag().getData());
				assertEquals("[bold]", fo.getCTag().getDisp());
				break;
			case 2:
				assertEquals("in ", fo.getText());
				assertEquals("in ", fo.render());
				break;
			case 3:
				assertEquals(TagType.OPENING, fo.getMTag().getTagType());
				assertEquals("comment", fo.getMTag().getType());
				assertEquals("<mrk id=\"3\" type=\"comment\" value=\"My comment\">", fo.render());
				break;
			case 4: // Then "bold"
				assertEquals("bold", fo.getText());
				assertEquals("bold", fo.render());
				break;
			case 5: // Then closing MTag
				assertEquals("</mrk>", fo.render());
				assertEquals(TagType.CLOSING, fo.getMTag().getTagType());
				assertEquals("comment", fo.getMTag().getType());
				break;
			case 6: // Then closing CTag
				assertEquals("</pc>", fo.render());
				assertEquals(TagType.CLOSING, fo.getCTag().getTagType());
				assertEquals("1", fo.getCTag().getId());
				break;
			case 7: // Then a space (let's test bad casts)
				int bad = 0;
				try { fo.getCTag(); }
				catch ( ClassCastException e ) {
					bad++;
				}
				try { fo.getMTag(); }
				catch ( ClassCastException e ) {
					bad++;
				}
				assertEquals(2, bad);
				break;
			case 8: // Then opening MTag
				assertEquals(TagType.OPENING, fo.getMTag().getTagType());
				assertEquals(MTag.TYPE_DEFAULT, fo.getMTag().getType());
				assertEquals("<mrk id=\"m1\" translate=\"no\">", fo.render());
				break;
			case 9: // Then standalone CTag
				assertEquals(TagType.STANDALONE, fo.getCTag().getTagType());
				assertEquals("2", fo.getCTag().getId());
				assertEquals("<ph id=\"2\"/>", fo.render());
				break;
			case 10: // Then closing MTag
				assertEquals("</mrk>", fo.render());
				assertEquals(TagType.CLOSING, fo.getMTag().getTagType());
				assertEquals("m1", fo.getMTag().getId());
				break;
			case 11: // Finally: the last period
				assertEquals(".", fo.getText());
				assertEquals(".", fo.render());
				break;
			}
			count++;
		}
		assertEquals(12, count); // Includes the last increment
	}

	@Test
	public void testWithOverlaps () {
		Fragment cont = new Fragment(new Unit("id").getStore(), true);
		cont.openCodeSpan("1", "<1>"); // 2
		cont.closeCodeSpan("1", "</1>"); // 2
		cont.appendCode("2", "<2/>"); // -
		cont.openCodeSpan("3", "<3>"); // 1
		cont.openCodeSpan("4", "<4>"); // 1
		cont.closeCodeSpan("3", "</3>"); // 1
		cont.openCodeSpan("5", "<5>"); // 2
		cont.openCodeSpan("6", "<6>"); // will be deleted
		cont.closeCodeSpan("6", "</6>"); // will be 0
		cont.closeCodeSpan("5", "</5>"); // 2
		cont.closeCodeSpan("4", "</4>"); // 1
		cont.openCodeSpan("7", "<7>"); // 0
		cont.delete(14, 16); // Delete t6o
		// Create the full fragment output
		StringBuilder tmp = new StringBuilder();
		IFragmentRenderer r = new XLIFFFragmentRenderer(cont, null);
		for ( IFragmentObject fo : r ) {
			tmp.append(fo.render());
		}
		// Test
		assertEquals("<pc id=\"1\"></pc><ph id=\"2\"/><sc id=\"3\" canOverlap=\"no\"/><pc id=\"4\"><ec startRef=\"3\" canOverlap=\"no\"/>"
			+ "<pc id=\"5\"><ec id=\"6\" canOverlap=\"no\" isolated=\"yes\"/></pc></pc><sc id=\"7\" canOverlap=\"no\" isolated=\"yes\"/>",
			tmp.toString());
		//assertEquals(cont.toXLIFF(), tmp.toString());
	}

	@Test
	public void testMarkers () {
		Fragment cont = new Fragment(new Unit("id").getStore(), true);
		cont.openCodeSpan("1", "<1>"); // 2
		cont.openMarkerSpan("m1", "my:type"); // 2
		cont.closeMarkerSpan("m1"); // 2
		cont.closeCodeSpan("1", "</1>"); // 2
		cont.openMarkerSpan("m2", "my:type2");
		cont.openCodeSpan("2", "<2>");
		cont.closeMarkerSpan("m2");
		cont.closeCodeSpan("2", "<2>");
		// Create the full fragment output
		StringBuilder tmp = new StringBuilder();
		IFragmentRenderer r = new XLIFFFragmentRenderer(cont, null);
		for ( IFragmentObject fo : r ) {
			tmp.append(fo.render());
		}
		// Test
		assertEquals("<pc id=\"1\"><mrk id=\"m1\" type=\"my:type\"></mrk></pc><sm id=\"m2\" type=\"my:type2\"/>"
			+ "<pc id=\"2\"><em startRef=\"m2\"/></pc>",
			tmp.toString());
		//assertEquals(cont.toXLIFF(), tmp.toString());
	}

}
