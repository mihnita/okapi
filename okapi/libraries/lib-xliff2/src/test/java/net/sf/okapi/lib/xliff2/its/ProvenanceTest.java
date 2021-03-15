package net.sf.okapi.lib.xliff2.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class ProvenanceTest {

	@Test (expected = XLIFFException.class)
	public void testProvenanceToolToolRefConflict () {
		// tool and toolRef cannot be set at the same time
		Provenance prov = new Provenance();
		prov.setTool("a");
		prov.setToolRef("b");
		prov.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testProvenanceOrgOrgRefConflict () {
		// org and orgRef cannot be set at the same time
		Provenance prov = new Provenance();
		prov.setOrg("a");
		prov.setOrgRef("b");
		prov.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testProvenancePersonPersonRefConflict () {
		// person and personRef cannot be set at the same time
		Provenance prov = new Provenance();
		prov.setPerson("a");
		prov.setPersonRef("b");
		prov.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testProvenanceRevToolRevToolRefConflict () {
		// revTool and revToolRef cannot be set at the same time
		Provenance prov = new Provenance();
		prov.setRevTool("a");
		prov.setRevToolRef("b");
		prov.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testProvenanceRevOrgRevOrgRefConflict () {
		// revOrg and revOrgRef cannot be set at the same time
		Provenance prov = new Provenance();
		prov.setRevOrg("a");
		prov.setRevOrgRef("b");
		prov.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testProvenanceRevPersonRevPersonRefConflict () {
		// revPerson and revPersonRef cannot be set at the same time
		Provenance prov = new Provenance();
		prov.setRevPerson("a");
		prov.setRevPersonRef("b");
		prov.validate();
	}

	@Test
	public void testInlineProvenance1 () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"its:any\" its:tool=\"tool1\""
			+ " its:org=\"org1\""
			+ " its:person=\"person1\""
			+ " its:revTool=\"revtool1\""
			+ " its:revOrg=\"revorg1\""
			+ " its:revPerson=\"revperson1\""
			+ ">source</mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		Provenance prov = (Provenance)am.getITSItems().get(Provenance.class);
		assertEquals("tool1", prov.getTool());
		assertEquals("org1", prov.getOrg());
		assertEquals("person1", prov.getPerson());
		assertEquals("revtool1", prov.getRevTool());
		assertEquals("revorg1", prov.getRevOrg());
		assertEquals("revperson1", prov.getRevPerson());
		// Re-write it
		assertEquals(text, U.writeEvents(events));
	}

	@Test
	public void testInlineProvenance2 () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"its:any\" its:toolRef=\"tool1\""
			+ " its:orgRef=\"org1\""
			+ " its:personRef=\"person1\""
			+ " its:revToolRef=\"revtool1\""
			+ " its:revOrgRef=\"revorg1\""
			+ " its:revPersonRef=\"revperson1\""
			+ ">source</mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		Provenance prov = (Provenance)am.getITSItems().get(Provenance.class);
		assertEquals("tool1", prov.getToolRef());
		assertEquals("org1", prov.getOrgRef());
		assertEquals("person1", prov.getPersonRef());
		assertEquals("revtool1", prov.getRevToolRef());
		assertEquals("revorg1", prov.getRevOrgRef());
		assertEquals("revperson1", prov.getRevPersonRef());
		// Re-write it
		assertEquals(text, U.writeEvents(events));
	}

	@Test
	public void testProvStandOff () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\" its:annotatorsRef=\"provenance|myTool1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<its:provenanceRecords xml:id=\"p1\">\n"
			+ " <its:provenanceRecord "
			+ "toolRef=\"tool11Ref\" orgRef=\"org11Ref\" person=\"person11\" "
			+ "revTool=\"tool12\" revOrg=\"org12\" revPersonRef=\"person12Ref\" "
			+ "provRef=\"provRef11\"/>\n"
			+ " <its:provenanceRecord "
			+ "tool=\"tool21\" org=\"org21\" personRef=\"person21Ref\" "
			+ "revToolRef=\"tool22Ref\" revOrgRef=\"org22Ref\" revPerson=\"person22\" "
			+ "provRef=\"provRef22\"/>\n"
			+ "</its:provenanceRecords>\n"
			+ "<segment>\n<source><mrk id=\"1\" type=\"its:any\" its:provenanceRecordsRef=\"#its=p1\""
			+ ">source</mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertFalse(am.hasExtAttribute());
		assertTrue(am.hasITSItem());
		Provenances provs = (Provenances)am.getITSItems().get(Provenance.class);
		assertEquals("p1", provs.getGroupId());
		assertEquals(2, provs.getList().size());
		Provenance prov = provs.getList().get(0);
		assertEquals("tool11Ref", prov.getToolRef());
		assertEquals("myTool1", prov.getAnnotatorRef());
		// Re-write it
		assertEquals(text, U.writeEvents(events));
	}

	@Test
	public void testProvStandOffOnUnit () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\" its:annotatorsRef=\"provenance|myTool1\">\n"
			+ "<unit id=\"id\" its:provenanceRecordsRef=\"#its=pr1\">\n"
			+ "<its:provenanceRecords xml:id=\"pr1\">\n"
			+ " <its:provenanceRecord orgRef=\"orgRef\"/>\n"
			+ " <its:provenanceRecord person=\"person\"/>\n"
			+ "</its:provenanceRecords>\n"
			+ "<segment>\n<source"
			+ ">source</source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		assertTrue(unit.hasITSItem());
		assertFalse(unit.hasExtAttribute());
		Provenances provs = (Provenances)unit.getITSItems().get(Provenance.class);
		assertEquals("pr1", provs.getGroupId());
		assertEquals(2, provs.getList().size());
		Provenance prov = provs.getList().get(0);
		assertEquals("orgRef", prov.getOrgRef());
		prov = provs.getList().get(1);
		assertEquals("person", prov.getPerson());
		// Re-write it
		assertEquals(text, U.writeEvents(events));
	}

}
