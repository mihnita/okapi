package net.sf.okapi.lib.xliff2.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LocQualityIssueTest {

	@Test
	public void testSimplestValid () {
		LocQualityIssue lqi = new LocQualityIssue();
		lqi.setType("grammar");
		lqi.validate();
		lqi = new LocQualityIssue();
		lqi.setComment("some comment");
		lqi.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testInvalid () {
		LocQualityIssue lqi = new LocQualityIssue();
		lqi.validate();
	}

	@Test
	public void testLQIReference () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<its:locQualityIssues xml:id=\"lq1\">\n"
			+ " <its:locQualityIssue locQualityIssueComment=\"comment1\"/>\n"
			+ " <its:locQualityIssue locQualityIssueComment=\"comment2\"/>\n"
			+ "</its:locQualityIssues>\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"its:any\" its:locQualityIssuesRef=\"#its=lq1\">source</mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		ITSItems items = am.getITSItems();
		IITSItem item = items.get(LocQualityIssue.class);
		assertTrue(item.isGroup());
		LocQualityIssues issues = (LocQualityIssues)item;
		LocQualityIssue lqi = issues.getList().get(1);
		assertEquals("comment2", lqi.getComment());
		assertEquals(text, U.writeEvents(events));
	}

	@Test
	public void testInlineLQI () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"its:any\" its:locQualityIssueType=\"other\""
			+ " its:locQualityIssueComment=\"comment\""
			+ " its:locQualityIssueEnabled=\"no\""
			+ " its:locQualityIssueSeverity=\"12.345678\""
			+ " its:locQualityIssueProfileRef=\"profile\""
			+ ">source</mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		LocQualityIssue lqi = (LocQualityIssue)am.getITSItems().get(LocQualityIssue.class);
		assertEquals("other", lqi.getType());
		assertEquals("comment", lqi.getComment());
		assertFalse(lqi.isEnabled());
		assertEquals(12.345678, lqi.getSeverity(), 0.0);
		assertEquals("profile", lqi.getProfileRef());
		// Re-write it
		assertEquals(text, U.writeEvents(events));
	}

	@Test
	public void testAddition () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"its:any\" its:locQualityIssueType=\"other\""
			+ " its:locQualityIssueComment=\"comment\""
			+ " its:locQualityIssueEnabled=\"no\""
			+ " its:locQualityIssueSeverity=\"12.345678\""
			+ " its:locQualityIssueProfileRef=\"profile\""
			+ ">source</mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		ITSItems items = am.getITSItems();
		LocQualityIssue lqi1 = (LocQualityIssue)items.get(LocQualityIssue.class);
		assertEquals("other", lqi1.getType());
		// Add
		LocQualityIssue lqi2 = new LocQualityIssue();
		lqi2.setType("grammar");
		lqi2.setComment("new item");
		items.add(lqi2);
		// Check wheat we get in return now
		LocQualityIssues issues = (LocQualityIssues)items.get(LocQualityIssue.class);
		assertNotNull(issues.getGroupId()); // we have auto-id
		issues.setGroupId("newLqi"); // let replace it with a predictable one for the comparison
		assertEquals(1, items.size()); // Only 1 type of data category
		assertEquals(2, issues.getList().size()); // 2 instances
		LocQualityIssue lqi1new = issues.getList().get(0);
		assertTrue(lqi1 == lqi1new);
		LocQualityIssue lqi2new = issues.getList().get(1);
		assertTrue(lqi2 == lqi2new);

		// Now add another item (this time on the existing group)
		LocQualityIssue lqi3 = new LocQualityIssue();
		lqi3.setType("mistranslation");
		lqi3.setSeverity(99.99);
		items.add(lqi3);
		// Check wheat we get in return now (should still affect the items object)
		assertEquals(1, items.size()); // Only 1 type of data category
		assertEquals(3, issues.getList().size()); // 3 instances
		lqi1new = issues.getList().get(0);
		assertTrue(lqi1 == lqi1new);
		lqi2new = issues.getList().get(1);
		assertTrue(lqi2 == lqi2new);
		LocQualityIssue lqi3new = issues.getList().get(2);
		assertTrue(lqi3 == lqi3new);

		// Check the output
		String expected = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<its:locQualityIssues xml:id=\"newLqi\">\n"
			+ " <its:locQualityIssue locQualityIssueType=\"other\" locQualityIssueComment=\"comment\" locQualityIssueEnabled=\"no\" "
			+ "locQualityIssueSeverity=\"12.345678\" locQualityIssueProfileRef=\"profile\"/>\n"
			+ " <its:locQualityIssue locQualityIssueType=\"grammar\" locQualityIssueComment=\"new item\"/>\n"
			+ " <its:locQualityIssue locQualityIssueType=\"mistranslation\" locQualityIssueSeverity=\"99.99\"/>\n"
			+ "</its:locQualityIssues>\n"
			+ "<segment>\n"
			+ "<source><mrk id=\"1\" type=\"its:any\" its:locQualityIssuesRef=\"#its=newLqi\">source</mrk></source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		
		// Re-write it
		assertEquals(expected, U.writeEvents(events));
	}
	
	@Test
	public void testRemove () {
		ITSItems items = new ITSItems();
		LocQualityIssue lqi1 = new LocQualityIssue("c1"); items.add(lqi1);
		LocQualityIssue lqi2 = new LocQualityIssue("c2"); items.add(lqi2);
		LocQualityIssue lqi3 = new LocQualityIssue("c3"); items.add(lqi3);
		LocQualityIssues issues = (LocQualityIssues)items.get(LocQualityIssue.class);
		assertEquals(3, issues.getList().size());
		assertEquals(1, items.size());
		items.remove(lqi2);
		assertEquals(2, issues.getList().size());
		assertEquals(1, items.size());
		assertEquals("c1", issues.getList().get(0).getComment());
		assertEquals("c3", issues.getList().get(1).getComment());
		items.remove(issues);
		assertTrue(items.isEmpty());
	}

	// Fails because the group is moved when used by a reference
//	@Test
//	public void testFragID () {
//		String text = U.STARTDOCWITHITS
//			+ "<file id=\"f1\">\n"
//			+ "<unit id=\"id\" its:annotatorsRef='localization-quality-issue|tool1'>\n"
//			+ "<its:locQualityIssues xml:id=\"lq1\">\n"
//			+ " <its:locQualityIssue locQualityIssueComment=\"comment1\"/>\n"
//			+ " <its:locQualityIssue locQualityIssueComment=\"comment2\"/>\n"
//			+ "</its:locQualityIssues>\n"
//			+ "<segment>\n<source><mrk id=\"1\" type=\"its:any\" its:locQualityIssuesRef=\"#its=lq1\">source</mrk></source>\n</segment>\n"
//			+ "</unit>\n"
//			+ "</file>\n</xliff>\n";
//
//		Unit unit = U.getUnit(text);
//		assertTrue(unit.hasITSGroup());
//		assertEquals("lqi1", unit.getITSGroups().get(0).getGroupId());
//		
//		XLIFFDocument doc = new XLIFFDocument();
//		doc.load(text, XLIFFReader.VALIDATION_INCLUDE_FRAGIDPREFIX);
//		URIParser up = new URIParser("#f=f1/u=id/its=lq1");
//		Object obj = doc.fetchReference(up);
//		assertNotNull(obj);
//	}
	
}
