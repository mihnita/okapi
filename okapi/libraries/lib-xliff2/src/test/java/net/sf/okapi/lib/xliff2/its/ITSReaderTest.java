package net.sf.okapi.lib.xliff2.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ITSReaderTest {

	private static final String STARTDOC = "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\""
		+ " xmlns:its=\"http://www.w3.org/2005/11/its\" its:version=\"2.0\">";
	
	@Test
	public void testLQIStandOff () {
		String text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='localization-quality-issue|tool1'>"
			+ " <unit id=\"id\">"
			+ "  <its:locQualityIssues xml:id=\"its1\">"
			+ "   <its:locQualityIssue locQualityIssueType=\"grammar\"/>"
			+ "   <its:locQualityIssue locQualityIssueComment=\"comment\" annotatorsRef='localization-quality-issue|tool2'/>"
			+ "  </its:locQualityIssues>"
			+ "  <segment><source>source</source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		assertTrue(unit.hasITSGroup());
		assertFalse(unit.hasExtElements());
		List<DataCategoryGroup<?>> groups = unit.getITSGroups();
		LocQualityIssues lqIssues = (LocQualityIssues)groups.get(0);
		assertEquals("its1", lqIssues.getGroupId());
		LocQualityIssue lqi = lqIssues.getList().get(0);
		assertEquals("grammar", lqi.getType());
		assertEquals("tool1", lqi.getAnnotatorRef());
		lqi = lqIssues.getList().get(1);
		assertNull(lqi.getType());
		assertEquals("comment", lqi.getComment());
		assertEquals("tool2", lqi.getAnnotatorRef());
	}

	@Test
	public void testLQIAttributes () {
		String text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='localization-quality-issue|myTool1'>"
			+ " <unit id=\"id\" its:annotatorsRef='localization-quality-issue|myTool2'>"
			+ "  <segment><source><mrk id='1' type='its:its' its:locQualityIssueType='grammar' "
			+ " its:annotatorsRef='localization-quality-issue|myTool3'"
			+ " its:locQualityIssueSeverity='20' >source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertTrue(am.hasITSItem());
		ITSItems items = am.getITSItems();
		LocQualityIssue lqi = (LocQualityIssue)items.get(LocQualityIssue.class);
		assertEquals("grammar", lqi.getType());
		assertEquals(20.0, lqi.getSeverity(), 0.0);
		assertEquals("myTool3", lqi.getAnnotatorRef());
		assertFalse(am.hasExtAttribute());
	}

	@Test
	public void testProvStandOff () {
		String text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='provenance|myTool1'>"
			+ " <unit id=\"id\">"
			+ "  <its:provenanceRecords xml:id='p1'>"
			+ "   <its:provenanceRecord "
			+ "    toolRef='tool11Ref' person='person11' orgRef='org11Ref'"
			+ "    revTool='tool12' revPersonRef='person12Ref' revOrg='org12'"
			+ "    provRef='provRef11'/>"
			+ "   <its:provenanceRecord "
			+ "    tool='tool21' personRef='person21Ref' org='org21'"
			+ "    revToolRef='tool22Ref' revPerson='person22' revOrgRef='org22Ref'"
			+ "    provRef='provRef22'/>"
			+ "  </its:provenanceRecords>"
			+ "  <segment><source><mrk id='1' type='its:its' its:provenanceRecordsRef='#its=p1'"
			+ " >source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertFalse(am.hasExtAttribute());
		assertTrue(am.hasITSItem());
		Provenances provs = (Provenances)am.getITSItems().get(Provenance.class);
		assertEquals("p1", provs.getGroupId());
		assertEquals(2, provs.getList().size());
		Provenance prov = provs.getList().get(0);
		assertEquals("tool11Ref", prov.getToolRef());
		assertEquals("tool12", prov.getRevTool());
		assertEquals("person11", prov.getPerson());
		assertEquals("person12Ref", prov.getRevPersonRef());
		assertEquals("org11Ref", prov.getOrgRef());
		assertEquals("org12", prov.getRevOrg());
		assertEquals("provRef11", prov.getProvRef());
		assertEquals("myTool1", prov.getAnnotatorRef());
	}

	@Test
	public void testProvAttributes () {
		String text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='provenance|myTool1'>"
			+ " <unit id=\"id\">"
			+ "  <segment><source><mrk id='1' type='its:its'"
			+ " its:tool='tool1' its:person='person1' its:org='org1'"
			+ " its:revTool='tool2' its:revPerson='person2' its:revOrg='org2'"
			+ " its:provRef='provRef1'>source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertFalse(am.hasExtAttribute());
		assertTrue(am.hasITSItem());
		Provenance prov = (Provenance)am.getITSItems().get(Provenance.class);
		assertEquals("tool1", prov.getTool());
		assertEquals("tool2", prov.getRevTool());
		assertEquals("person1", prov.getPerson());
		assertEquals("person2", prov.getRevPerson());
		assertEquals("org1", prov.getOrg());
		assertEquals("org2", prov.getRevOrg());
		assertEquals("provRef1", prov.getProvRef());
		assertEquals("myTool1", prov.getAnnotatorRef());
	}

	@Test
	public void testProvRefAttributes () {
		String text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='provenance|myTool1'>"
			+ " <unit id=\"id\">"
			+ "  <segment><source><mrk id='1' type='its:its'"
			+ " its:toolRef='tool1' its:personRef='person1' its:orgRef='org1'"
			+ " its:revToolRef='tool2' its:revPersonRef='person2' its:revOrgRef='org2'"
			+ " >source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertFalse(am.hasExtAttribute());
		assertTrue(am.hasITSItem());
		Provenance prov = (Provenance)am.getITSItems().get(Provenance.class);
		assertEquals("tool1", prov.getToolRef());
		assertEquals("tool2", prov.getRevToolRef());
		assertEquals("person1", prov.getPersonRef());
		assertEquals("person2", prov.getRevPersonRef());
		assertEquals("org1", prov.getOrgRef());
		assertEquals("org2", prov.getRevOrgRef());
		assertEquals("myTool1", prov.getAnnotatorRef());
	}

	@Test
	public void testAnnotatorsRef () {
		String text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='mt-confidence|refOnFile'>"
			+ " <unit id='id'>"
			+ "  <segment><source><mrk id='1' type='its:its' its:mtConfidence='0.5'"
			+ " >source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		MTConfidence ann = (MTConfidence)am.getITSItems().get(MTConfidence.class);
		assertEquals(0.5, ann.getMtConfidence(), 0.0);
		assertEquals("refOnFile", ann.getAnnotatorRef());

		text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='mt-confidence|refOnFile'>"
			+ " <unit id='id' its:annotatorsRef='mt-confidence|refOnUnit'>"
			+ "  <segment><source><mrk id='1' type='its:its' its:mtConfidence='0.5'"
			+ " >source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		unit = U.getUnit(text);
		am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		ann = (MTConfidence)am.getITSItems().get(MTConfidence.class);
		assertEquals("refOnUnit", ann.getAnnotatorRef());

		text = STARTDOC
			+ "<file id=\"f1\" its:annotatorsRef='mt-confidence|refOnFile'>"
			+ " <unit id='id' its:annotatorsRef='mt-confidence|refOnUnit'>"
			+ "  <segment><source><mrk id='1' type='its:its' its:mtConfidence='0.5'"
			+ " its:annotatorsRef='mt-confidence|refOnMrk'>source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		unit = U.getUnit(text);
		am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		ann = (MTConfidence)am.getITSItems().get(MTConfidence.class);
		assertEquals("refOnMrk", ann.getAnnotatorRef());
	}

	@Test
	public void testRefToStandOff () {
		String text = STARTDOC
			+ "<file id=\"f1\">"
			+ " <unit id=\"id\">"
			+ "  <its:locQualityIssues xml:id=\"its1\">"
			+ "   <its:locQualityIssue locQualityIssueComment=\"comment1\"/>"
			+ "   <its:locQualityIssue locQualityIssueComment=\"comment2\"/>"
			+ "  </its:locQualityIssues>"
			+ "  <segment><source><mrk id='1' type='its:its' its:locQualityIssuesRef='#its=its1'>source</mrk></source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		MTag am = (MTag)unit.getPart(0).getSourceTags().get(U.kOA(0));
		assertTrue(am.hasITSItem());
		LocQualityIssues issues = (LocQualityIssues)am.getITSItems().get(LocQualityIssue.class);
		assertEquals("comment2", issues.getList().get(1).getComment());
		assertFalse(unit.hasITSGroup()); // Group was moved to annotation marker
	}

	@Test (expected = InvalidParameterException.class)
	public void textBadIdentifierInAnnotatorsRef () {
		String text = STARTDOC
			+ "<file id=\"f1\">"
			+ " <unit id=\"id\" its:annotatorsRef=\"badIdent|value\">"
			+ "<segment><source>text</source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		U.getEvents(text);
	}

	@Test (expected = InvalidParameterException.class)
	public void textMissingIdentifierInAnnotatorsRef () {
		String text = STARTDOC
			+ "<file id=\"f1\">"
			+ " <unit id=\"id\" its:annotatorsRef=\"missing-dc-identifier\">"
			+ "<segment><source>text</source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		U.getEvents(text);
	}

	@Test (expected = InvalidParameterException.class)
	public void textBadSeparatorInAnnotatorsRef () {
		String text = STARTDOC
			+ "<file id=\"f1\">"
			+ " <unit id=\"id\" its:annotatorsRef=\"translate|uri1;terminology|uri2\">"
			+ "<segment><source>text</source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		U.getEvents(text);
	}

	public void textAnnotatorsRefTwoValues () {
		String text = STARTDOC
			+ "<file id=\"f1\">"
			+ " <unit id=\"id\" its:annotatorsRef=\"provenance|tool1 translate|tool2\">"
			+ "<segment><source>text</source></segment>"
			+ " </unit>"
			+ "</file></xliff>";
		Unit unit = U.getUnit(text);
		AnnotatorsRef ar = unit.getAnnotatorsRef();
		assertEquals("tool1", ar.get(DataCategories.PROVENANCE));
		assertEquals("tool2", ar.get(DataCategories.TRANSLATE));
	}

}
