package net.sf.okapi.lib.xliff2.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.List;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup.AppliesTo;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.test.U;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetadataTest {
	
	@Test
	public void testSimple () {
		Metadata md  = new Metadata();
		MetaGroup mg = md.add(new MetaGroup("cat1"));
		mg.add(new Meta("t1", "d1"));
		mg.add(new MetaGroup("cat2"));
		mg = md.get(0);
		mg = md.get(0);
		assertEquals("cat1", mg.getCategory());
		Meta m1 = (Meta)mg.get(0);
		assertEquals("t1", m1.getType());
		assertEquals("d1", m1.getData());
	}

	@Test
	public void testWriter1 () {
		try ( XLIFFWriter writer = new XLIFFWriter() ) {
			StringWriter sr = new StringWriter();
			writer.setLineBreak("\n");
			writer.create(sr, "en", "fr");
			// Mid-file data
			MidFileData mfd = new MidFileData();
			mfd.setMetadata(createMetadata("F"));
			writer.writeMidFile(mfd);
			// Start of group
			StartGroupData sgd = new StartGroupData("g1");
			sgd.setMetadata(createMetadata("g"));
			writer.writeStartGroup(sgd);
			// Unit
			Unit unit = new Unit("u1");
			unit.setMetadata(createMetadata("u"));
			unit.appendSegment().getSource().append("source");
			writer.writeUnit(unit);
			writer.close();
			
			// Done: compare with expected output
			String expected = U.STARTDOC
				+ "<file id=\"f1\">\n"
				+ "<mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
				+ "<mda:metaGroup id=\"Fmg1\" category=\"Fcat1\" appliesTo=\"source\">\n"
				+ "<mda:meta type=\"Ft1\">Fd1</mda:meta>\n"
				+ "<mda:metaGroup category=\"Fcat2\" appliesTo=\"target\">\n"
				+ "<mda:meta type=\"Ft11\">Fd11</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "<mda:meta type=\"Ft2\">Fd2</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "</mda:metadata>\n"
				+ "<group id=\"g1\">\n"
				+ "<mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
				+ "<mda:metaGroup id=\"gmg1\" category=\"gcat1\" appliesTo=\"source\">\n"
				+ "<mda:meta type=\"gt1\">gd1</mda:meta>\n"
				+ "<mda:metaGroup category=\"gcat2\" appliesTo=\"target\">\n"
				+ "<mda:meta type=\"gt11\">gd11</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "<mda:meta type=\"gt2\">gd2</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "</mda:metadata>\n"
				+ "<unit id=\"u1\">\n"
				+ "<mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
				+ "<mda:metaGroup id=\"umg1\" category=\"ucat1\" appliesTo=\"source\">\n"
				+ "<mda:meta type=\"ut1\">ud1</mda:meta>\n"
				+ "<mda:metaGroup category=\"ucat2\" appliesTo=\"target\">\n"
				+ "<mda:meta type=\"ut11\">ud11</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "<mda:meta type=\"ut2\">ud2</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "</mda:metadata>\n"
				+ "<segment>\n<source>source</source>\n</segment>\n"
				+ "</unit>\n</group>\n"
				+ "</file>\n</xliff>\n";
			assertEquals(expected, sr.toString());
			XLIFFReader.validate(sr.toString(), null);
		}
	}
	
	@Test
	public void testWriter2 () {
		try ( XLIFFWriter writer = new XLIFFWriter() ) {
			StringWriter sr = new StringWriter();
			writer.setLineBreak("\n");
			writer.create(sr, "en");
			StartXliffData sxd = new StartXliffData(null);
			sxd.setNamespace(Const.PREFIX_METADATA, Const.NS_XLIFF_METADATA20);
			writer.writeStartDocument(sxd, null);
			// Unit
			Unit unit = new Unit("u1");
			unit.setMetadata(createMetadata("U"));
			unit.appendSegment().getSource().append("source");
			writer.writeUnit(unit);
			writer.close();
			
			// Done: compare with expected output
			String expected = "<?xml version=\"1.0\"?>\n"
				+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
				+ " version=\"2.0\" srcLang=\"en\" xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
				+ "<file id=\"f1\">\n"
				+ "<unit id=\"u1\">\n"
				+ "<mda:metadata>\n"
				+ "<mda:metaGroup id=\"Umg1\" category=\"Ucat1\" appliesTo=\"source\">\n"
				+ "<mda:meta type=\"Ut1\">Ud1</mda:meta>\n"
				+ "<mda:metaGroup category=\"Ucat2\" appliesTo=\"target\">\n"
				+ "<mda:meta type=\"Ut11\">Ud11</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "<mda:meta type=\"Ut2\">Ud2</mda:meta>\n"
				+ "</mda:metaGroup>\n"
				+ "</mda:metadata>\n"
				+ "<segment>\n<source>source</source>\n</segment>\n"
				+ "</unit>\n"
				+ "</file>\n</xliff>\n";
			assertEquals(expected, sr.toString());
			XLIFFReader.validate(sr.toString(), null);
		}
	}
	
	@Test
	public void testReaderWriter () {
		String snippet = U.STARTDOC
			+ "<file id=\"f1\">\n"
			+ "<mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
			+ "<mda:metaGroup id=\"Fmg1\" category=\"Fcat1\" appliesTo=\"source\">\n"
			+ "<mda:meta type=\"Ft1\">Fd1</mda:meta>\n"
			+ "<mda:metaGroup category=\"Fcat2\" appliesTo=\"target\">\n"
			+ "<mda:meta type=\"Ft11\">Fd11</mda:meta>\n"
			+ "</mda:metaGroup>\n"
			+ "<mda:meta type=\"Ft2\">Fd2</mda:meta>\n"
			+ "</mda:metaGroup>\n"
			+ "</mda:metadata>\n"
			+ "<group id=\"g1\" xmlns:p=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
			+ "<p:metadata>\n"
			+ "<p:metaGroup id=\"Gmg1\" category=\"Gcat1\" appliesTo=\"source\">\n"
			+ "<p:meta type=\"Gt1\">Gd1</p:meta>\n"
			+ "<p:metaGroup category=\"Gcat2\" appliesTo=\"target\">\n"
			+ "<p:meta type=\"Gt11\">Gd11</p:meta>\n"
			+ "</p:metaGroup>\n"
			+ "<p:meta type=\"Gt2\">Gd2</p:meta>\n"
			+ "</p:metaGroup>\n"
			+ "</p:metadata>\n"
			+ "<unit id=\"u1\">\n"
			+ "<p:metadata>\n"
			+ "<p:metaGroup id=\"Umg1\" category=\"Ucat1\" appliesTo=\"source\">\n"
			+ "<p:meta type=\"Ut1\">Ud1</p:meta>\n"
			+ "<p:metaGroup category=\"Ucat2\" appliesTo=\"target\">\n"
			+ "<p:meta type=\"Ut11\">Ud11</p:meta>\n"
			+ "</p:metaGroup>\n"
			+ "<p:meta type=\"Ut2\">Ud2</p:meta>\n"
			+ "</p:metaGroup>\n"
			+ "</p:metadata>\n"
			+ "<segment>\n<source>source</source>\n</segment>\n"
			+ "</unit>\n</group>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(snippet);
		for ( Event event : events ) {
			switch ( event.getType() ) {
			case MID_FILE:
				MidFileData mfd = event.getMidFileData();
				assertTrue(mfd.hasMetadata());
				Metadata md = mfd.getMetadata();
				MetaGroup mg1 = md.get(0);
				assertEquals("Fmg1", mg1.getId());
				Meta m = (Meta)mg1.get(0);
				assertEquals("Ft1", m.getType());
				assertEquals("Fd1", m.getData());
				break;
			case START_GROUP:
				StartGroupData sgd = event.getStartGroupData();
				assertTrue(sgd.hasMetadata());
				md = sgd.getMetadata();
				mg1 = md.get(0);
				assertEquals("Gmg1", mg1.getId());
				m = (Meta)mg1.get(0);
				assertEquals("Gt1", m.getType());
				assertEquals("Gd1", m.getData());
				break;
			case TEXT_UNIT:
				Unit unit = event.getUnit();
				assertTrue(unit.hasMetadata());
				md = unit.getMetadata();
				mg1 = md.get(0);
				assertEquals("Umg1", mg1.getId());
				m = (Meta)mg1.get(0);
				assertEquals("Ut1", m.getType());
				assertEquals("Ud1", m.getData());
				break;
			default:
				break;
			}
		}
		
		// Rewrite
		String output = U.writeEvents(events);
		assertEquals(snippet, output);
	}

	private Metadata createMetadata (String prefix) {
		Metadata md = new Metadata();
		MetaGroup mg1 = md.add(new MetaGroup(prefix+"cat1"));
		mg1.setId(prefix+"mg1");
		mg1.setAppliesTo(AppliesTo.SOURCE);
		mg1.add(new Meta(prefix+"t1", prefix+"d1"));
		MetaGroup mg2 = (MetaGroup)mg1.add(new MetaGroup(prefix+"cat2"));
		mg2.setAppliesTo(AppliesTo.TARGET);
		mg2.add(new Meta(prefix+"t11", prefix+"d11"));
		mg1.add(new Meta(prefix+"t2", prefix+"d2"));
		return md;
	}
}
