package net.sf.okapi.lib.xliff2.changeTracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.test.U;

@RunWith(JUnit4.class)
public class ChangeTrackTest {
	
	@Test
	public void testCreationByGet () {
		Unit unit = new Unit("id1");
		assertEquals(0, unit.getChangeTrack().size());
		StartGroupData sgd = new StartGroupData("id2");
		assertEquals(0, sgd.getChangeTrack().size());
		MidFileData mfd = new MidFileData();
		assertEquals(0, mfd.getChangeTrack().size());
	}
	
	@Test
	public void testDefaults () {
		ChangeTrack c = new ChangeTrack();
		assertTrue(c.isEmpty());
		Revisions revs = new Revisions();
		assertTrue(revs.isEmpty());
		revs.setAppliesTo("note");
		revs.setCurrentVersion("v1");
		Revision rev = new Revision();
		assertTrue(rev.isEmpty());
		rev.setAuthor("a");
		rev.setVersion("v1");
		final String dt = "1994-11-05T08:15:30-05:00";
		rev.setDatetime(dt);
		Item item = new Item("prop");
		item.setText("text");
		rev.add(item);
		revs.add(rev);
		c.add(revs);

		assertFalse(c.isEmpty());
		revs = c.get(0);
		assertFalse(revs.isEmpty());
		assertEquals("note", revs.getAppliesTo());
		rev = revs.get(0);
		assertFalse(rev.isEmpty());
		assertEquals("a", rev.getAuthor());
		assertEquals(dt, rev.getDatetime());
		assertEquals("v1", rev.getVersion());
		assertEquals("text", rev.get(0).getText());
		assertEquals("prop", rev.get(0).getProperty());
	}

	@Test
	public void testReadRevision () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<ctr:changeTrack xmlns:ctr=\"urn:oasis:names:tc:xliff:changetracking:2.0\" xmlns:myNS=\"mine\">\n"
			+ "<ctr:revisions appliesTo=\"target\" myNS:myAttr=\"someData1\">\n"
			+ "<ctr:revision myNS:myAttr=\"someData2\">\n"
			+ "<ctr:item property=\"content\" myNS:myAttr=\"someData3\">source</ctr:item>\n"
			+ "</ctr:revision>\n"
			+ "</ctr:revisions>\n"
			+ "</ctr:changeTrack>\n"
			+ "<segment>\n<source>source</source>\n<target>translation</target>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		ChangeTrack ct = unit.getChangeTrack();
		Revisions revs = ct.get(0);
		assertEquals("someData1", revs.getExtAttributeValue("mine", "myAttr"));
		assertEquals("target", revs.getAppliesTo());
		Revision rev = revs.get(0);
		assertEquals("someData2", rev.getExtAttributeValue("mine", "myAttr"));
		Item item = rev.get(0);
		assertEquals("content", item.getProperty());
		assertEquals("source", item.getText());
		assertEquals("someData3", item.getExtAttributeValue("mine", "myAttr"));
	}
	
	@Test
	public void testAdd () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<segment>\n<source>source<ph id=\"1\" dataRef=\"d1\"/></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);

		Revisions revs = new Revisions();
		unit.getChangeTrack().add(revs);
		revs.setAppliesTo("source");
		Revision rev = new Revision();
		rev.add(new Item("content")).setText("t");
		revs.add(rev);

		// Test output
		String expected = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<ctr:changeTrack xmlns:ctr=\"urn:oasis:names:tc:xliff:changetracking:2.0\">\n"
			+ "<ctr:revisions appliesTo=\"source\">\n"
			+ "<ctr:revision>\n"
			+ "<ctr:item property=\"content\">t</ctr:item>\n"
			+ "</ctr:revision>\n"
			+ "</ctr:revisions>\n"
			+ "</ctr:changeTrack>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<segment>\n<source>source<ph id=\"1\" dataRef=\"d1\"/></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeEvents(events));
		U.getEvents(expected); // Validate
	}

}
