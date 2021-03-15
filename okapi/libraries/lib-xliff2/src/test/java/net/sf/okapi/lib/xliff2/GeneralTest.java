package net.sf.okapi.lib.xliff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import net.sf.okapi.lib.xliff2.core.Tag;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.ExtElements;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeneralTest {

	@Test
	public void testAddTarget () {
		Unit unit = createSimpleUnit();
		Segment segment = unit.getSegment(0);
		Fragment frag =  segment.getTarget(GetTarget.CLONE_SOURCE);
		assertEquals(frag, segment.getSource());
	}
	
	@Test
	public void testAccessMarkers () {
		Unit unit = createSimpleUnit();
		Segment segment = unit.getSegment(0);
		Fragment fragment = segment.getSource();
		String ct = fragment.getCodedText();
		for (int i=0; i<ct.length(); i++ ) {
		   if ( Fragment.isChar1(ct.charAt(i)) ) {
		      int key = Fragment.toKey(ct.charAt(i), ct.charAt(++i));
		      Tag bm = fragment.getTag(key);
		      // Do something with the marker...
			   assertNotNull(bm);
		   }
		}	}
	
	@Test
	public void testAccessUnsupportedModule () {
		Unit unit = createSimpleUnit();
		List<ExtElement> elems = unit.getExtElements().find(Const.NS_XLIFF_GLOSSARY20, "glossary");
		assertEquals(1, elems.size());
	}
	
	private Unit createSimpleUnit () {
		Unit unit = new Unit("id");
		Fragment src = unit.appendSegment().getSource();
		src.append("Hello ");
		src.append(TagType.OPENING, "1", "<B>", false);
		src.append("World");
		src.append(TagType.CLOSING, "1", "</B>", false);
		src.append("!");
		
		unit.getExtAttributes().setAttribute(Const.NS_XLIFF_FS20, "fs", "p");
		ExtElements elems = unit.getExtElements();
		ExtElement glossary = elems.add(Const.NS_XLIFF_GLOSSARY20, "glossary", Const.PREFIX_GLOSSARY);
		ExtElement glossEntry = glossary.addElement("glossEntry");
		ExtElement term = glossEntry.addElement("term");
		term.addContent("my term");
		return unit;
	}

}
