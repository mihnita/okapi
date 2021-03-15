package net.sf.okapi.lib.xliff2.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.lib.xliff2.core.AnnotatedSpan;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.matches.Match;
import net.sf.okapi.lib.xliff2.matches.Matches;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GeneralTest {

	private FileLocation root = FileLocation.fromClass(GeneralTest.class);

	@Test
	public void testTranslate () {
		XLIFFDocument doc = load("translate.xlf");
		
		Unit unit = doc.getUnitNode("f1", "u1").get();
		assertFalse(unit.getTranslate());
		unit.setTranslate(true); // Change the flag
		
		unit = doc.getUnitNode("f1", "u2").get();
		assertTrue(unit.getTranslate());

		Fragment frag = unit.getSegment(0).getSource();
		assertEquals("Text {oA}DATA {oA}text {cA}DATA {cA} text.", U.fmtMarkers(frag.getCodedText()));
		unit.hideProtectedContent();
		assertEquals("Text {$}{oA}text {cA}{$} text.", U.fmtMarkers(frag.getCodedText()));
		unit.showProtectedContent();
		assertEquals("Text {oA}DATA {oA}text {cA}DATA {cA} text.", U.fmtMarkers(frag.getCodedText()));

		// Save and reload
		doc = saveAndReload(doc);
		unit = doc.getUnitNode("f1", "u1").get();
		
		// Check modified entry
		assertTrue(unit.getTranslate());
	}
	
	@Test
	public void testTerminology () {
		XLIFFDocument doc = load("terminology.xlf");
		Unit unit = doc.getUnitNode("f1", "u1").get();
		
		assertEquals("myTool", unit.getAnnotatorsRef().get(DataCategories.TERMINOLOGY));
		
		List<AnnotatedSpan> spans = unit.getAnnotatedSpans(false);
		assertEquals(5, spans.size());
		
		TermTag tm = (TermTag)spans.get(0).getMarker();
		assertEquals("m1", tm.getId());
		assertTrue(tm.isTerm());
		assertEquals("myTool", tm.getAnnotatorRef());
		
		tm = (TermTag)spans.get(1).getMarker();
		assertEquals("m2", tm.getId());
		assertFalse(tm.isTerm());
		assertEquals("myTool", tm.getAnnotatorRef());
		
		tm = (TermTag)spans.get(2).getMarker();
		assertEquals("m3", tm.getId());
		assertEquals("http://en.wikipedia.org/wiki/Bilberry", tm.getRef());
		assertEquals("myTool", tm.getAnnotatorRef());
		
		tm = (TermTag)spans.get(3).getMarker();
		assertEquals("m4", tm.getId());
		assertEquals(0.9, tm.getTermConfidence(), 0.0);
		assertEquals("anotherTool", tm.getAnnotatorRef());
		assertEquals("squashberry", spans.get(3).getPlainText());
		
		tm = (TermTag)spans.get(4).getMarker();
		assertEquals("m5", tm.getId());
		assertEquals("The exact kind of man needed for a specific purpose", tm.getValue());
		assertEquals("myTool", tm.getAnnotatorRef());
		assertEquals("huckleberry", spans.get(4).getPlainText());
		
		// Make some changes in the last span
		tm.setRef("http://dbpedia.org/page/Huckleberry");
		tm.setValue(null);
		tm.setAnnotatorRef("newTool");

		// Save and reload
		doc = saveAndReload(doc);
		unit = doc.getUnitNode("f1", "u1").get();

		spans = unit.getAnnotatedSpans(false);

		// Check modified entry
		tm = (TermTag)spans.get(4).getMarker();
		assertEquals("m5", tm.getId());
		assertNull(tm.getValue());
		assertEquals("newTool", tm.getAnnotatorRef());
		assertEquals("http://dbpedia.org/page/Huckleberry", tm.getRef());
	}
	
	@Test
	public void testDomain () {
		XLIFFDocument doc = load("domain.xlf");
		Unit unit = doc.getUnitNode("f1", "u1").get();
	
		Domain dc1 = (Domain)unit.getITSItems().get(DataCategories.DOMAIN);
		assertEquals("travel", dc1.getDomain());
		assertEquals("myTool", dc1.getAnnotatorRef());

		List<AnnotatedSpan> list = unit.getAnnotatedSpans(false);
		Domain dc2 = (Domain)list.get(0).getMarker().getITSItems().get(Domain.class);
		assertEquals("software", dc2.getDomain());
		assertEquals("myTool", dc2.getAnnotatorRef());

		// Modify entries
		dc1.setDomain("newDomain");
		dc1.setAnnotatorRef("newTool");
		dc2.setDomain("newDomainMrk");
		dc2.setAnnotatorRef("newToolMrk");
		
		// Save and reload
		doc = saveAndReload(doc);
		unit = doc.getUnitNode("f1", "u1").get();

		// Check modified entries
		dc1 = (Domain)unit.getITSItems().get(DataCategories.DOMAIN);
		assertEquals("newDomain", dc1.getDomain());
		assertEquals("newTool", dc1.getAnnotatorRef());
		list = unit.getAnnotatedSpans(false);
		dc2 = (Domain)list.get(0).getMarker().getITSItems().get(Domain.class);
		assertEquals("newDomainMrk", dc2.getDomain());
		assertEquals("newToolMrk", dc2.getAnnotatorRef());
	}
	
	@Test
	public void testTextAnalysis () {
		XLIFFDocument doc = load("text-analysis.xlf");
		Unit unit = doc.getUnitNode("f1", "u1").get();

		List<AnnotatedSpan> spans = unit.getAnnotatedSpans(false);
		TextAnalysis ta = (TextAnalysis)spans.get(0).getMarker().getITSItems().get(TextAnalysis.class);
		
		assertEquals("myTool", ta.getAnnotatorRef());
		assertEquals("http://nerd.eurecom.fr/ontology#Place", ta.getTaClassRef());
		assertEquals("http://dbpedia.org/resource/Arizona", ta.getTaIdentRef());
		assertEquals(0.8, ta.getTaConfidence(), 0.0);
		
		// Modify an entry
		ta.setTaIdentRef("http://www.wikidata.org/wiki/Q816");
		ta.setAnnotatorRef("newTool");
		
		// Add an entry
		Fragment frag = unit.getSegment(0).getSource();
		ta = (TextAnalysis)ITSWriter.annotate(frag, 22, 29, new TextAnalysis());
		ta.setAnnotatorRef("newTool");
		ta.setTaIdent("301467919");
		ta.setTaSource("Wordnet3.0");
		ta.setTaConfidence(0.7);
		
		// save and reload
		doc = saveAndReload(doc);
		unit = doc.getUnitNode("f1", "u1").get();
		spans = unit.getAnnotatedSpans(false);

		// Check modified entry
		ta = (TextAnalysis)spans.get(0).getMarker().getITSItems().get(TextAnalysis.class);
		assertEquals("newTool", ta.getAnnotatorRef());
		assertEquals("http://www.wikidata.org/wiki/Q816", ta.getTaIdentRef());

		// Check added entry
		ta = (TextAnalysis)spans.get(1).getMarker().getITSItems().get(TextAnalysis.class);
		assertEquals("newTool", ta.getAnnotatorRef());
		assertEquals("301467919", ta.getTaIdent());
		assertEquals("Wordnet3.0", ta.getTaSource());
		assertEquals(0.7, ta.getTaConfidence(), 0.0);
	}
	
	@Test
	public void testProvenance () {
		XLIFFDocument doc = load("provenance.xlf");
		Unit unit = doc.getUnitNode("f1", "u1").get();

		Provenance prov = (Provenance)unit.getITSItems().get(DataCategories.PROVENANCE);
		assertEquals("Gladys Overwith", prov.getPerson());
		assertEquals("Kent B. Goode", prov.getRevPerson());
		assertEquals("myTool", prov.getAnnotatorRef());
		
		unit = doc.getUnitNode("f1", "u2").get();
		Provenances provs = (Provenances)unit.getITSItems().get(DataCategories.PROVENANCE);
		assertEquals("prov1", provs.getGroupId());
		prov = provs.getList().get(0);
		assertEquals("Justin Kase", prov.getPerson());
		assertEquals("someTool", prov.getTool());
		assertEquals("someAnnotator", prov.getAnnotatorRef());
		prov = provs.getList().get(1);
		assertEquals("Dusty Rhodes", prov.getRevPerson());
		assertEquals("anotherTool", prov.getRevTool());
		assertEquals("anotherAnnotator", prov.getAnnotatorRef());

		unit = doc.getUnitNode("f1", "u3").get();
		List<AnnotatedSpan> spans = unit.getAnnotatedSpans(true);
		prov = (Provenance)spans.get(0).getMarker().getITSItems().get(Provenance.class);
		assertEquals("someOrg", prov.getOrg());
		assertEquals("someRevOrg", prov.getRevOrg());
		assertEquals("anAnnotator", prov.getAnnotatorRef());
		provs = (Provenances)spans.get(1).getMarker().getITSItems().get(Provenance.class);
		assertEquals("prov2", provs.getGroupId());
		prov = provs.getList().get(0);
		assertEquals("Turner Luce", prov.getPerson());
		assertEquals("http://www.omegat.org/", prov.getToolRef());
		prov = provs.getList().get(1);
		assertEquals("Hugo First", prov.getRevPerson());
		assertEquals("http://virtaal.translatehouse.org/", prov.getRevToolRef());
		assertEquals("anAnnotator", prov.getAnnotatorRef());

		//TODO: add/remove/modify
		
		// Save and reload
		doc = saveAndReload(doc);
		unit = doc.getUnitNode("f1", "u1").get();
	}
	
	@Test
	public void testLocalizationQualityIssue () {
		XLIFFDocument doc = load("localization-quality-issue.xlf");

		Unit unit = doc.getUnitNode("f1", "u1").get();
		List<AnnotatedSpan> spans = unit.getAnnotatedSpans(true);
		LocQualityIssue lqi = (LocQualityIssue)spans.get(0).getMarker().getITSItems().get(DataCategories.LOCQUALITYISSUE);
		assertEquals("misspelling", lqi.getType());
		assertEquals("'c'es' is unknown. Could be 'c'est'", lqi.getComment());
		assertEquals(50.0, lqi.getSeverity(), 0.0);
		assertEquals("someAnnotator", lqi.getAnnotatorRef());
		
		unit = doc.getUnitNode("f1", "u2").get();
		spans = unit.getAnnotatedSpans(true);
		LocQualityIssues lqis = (LocQualityIssues)spans.get(0).getMarker().getITSItems().get(DataCategories.LOCQUALITYISSUE);
		assertEquals("lqi1", lqis.getGroupId());
		lqi = lqis.getList().get(0);
		assertEquals("misspelling", lqi.getType());
		assertEquals("'c'es' is unknown. Could be 'c'est'", lqi.getComment());
		assertEquals(50.0, lqi.getSeverity(), 0.0);
		assertNull(lqi.getAnnotatorRef());
		lqi = lqis.getList().get(1);
		assertEquals("grammar", lqi.getType());
		assertEquals("Sentence is not capitalized", lqi.getComment());
		assertEquals(20.0, lqi.getSeverity(), 0.0);
		assertNull(lqi.getAnnotatorRef());
		
		// Remove an issue from mrk of the second unit
		lqis.getList().remove(0);
		lqi.setAnnotatorRef("newTool");
		
		// Add an issue to first mrk of the unit u1 (forcing it to stand-off annotation)
		
		
		// Save and reload
		doc = saveAndReload(doc);

		unit = doc.getUnitNode("f1", "u2").get();
		spans = unit.getAnnotatedSpans(true);
		lqis = (LocQualityIssues)spans.get(0).getMarker().getITSItems().get(DataCategories.LOCQUALITYISSUE);
		lqi = lqis.getList().get(0);
		assertEquals(1, lqis.getList().size());
		assertEquals("grammar", lqi.getType());
		assertEquals("Sentence is not capitalized", lqi.getComment());
		assertEquals(20.0, lqi.getSeverity(), 0.0);
//FIXME: annotatorsRef for LQI
//		assertEquals("newTool", lqi.getAnnotatorRef());
		
	}
	
	@Test
	public void testMTConfidence () {
		XLIFFDocument doc = load("mt-confidence.xlf");
		Unit unit = doc.getUnitNode("f1", "u1").get();
		Matches matches = unit.getMatches();

		Match match = matches.get(0);
		assertEquals(89.82, match.getMatchQuality(), 0.0);
		assertEquals("MTServices-XYZ", match.getAnnotatorRef());
		
		match = matches.get(1);
		assertEquals(67.8, match.getMatchQuality(), 0.0);
		assertEquals("MTProvider-ABC", match.getAnnotatorRef());
		
		match = matches.get(2);
		assertEquals(65.0, match.getMatchQuality(), 0.0);
		assertEquals("MTProvider-JKL", match.getAnnotatorRef());
		
		match = matches.get(3);
		assertEquals(89.82, match.getMatchQuality(), 0.0);
		assertEquals("MTServices-XYZ", match.getAnnotatorRef());
		
		unit = doc.getUnitNode("f1", "u2").get();
		List<AnnotatedSpan> spans = unit.getAnnotatedSpans(true);
		MTConfidence mtc = (MTConfidence)spans.get(0).getMarker().getITSItems().get(DataCategories.MTCONFIDENCE);
		assertEquals("MTServices-XYZ", mtc.getAnnotatorRef());
		assertEquals(0.8982, mtc.getMtConfidence(), 0.0);
		
		// Add a match in second unit
		match = new Match();
		match.setSource(new Fragment(match.getStore(), false, "source"));
		match.setTarget(new Fragment(match.getStore(), true, "target"));
		match.setAnnoatorRef("newTool");
		match.setMTConfidence(0.9);
		Match.annotate(unit.getSegment(0).getSource(), 0, -1, match);
		// Update match-quality/mt-confidence in span of second unit
		mtc.setAnnotatorRef("newTool");
		mtc.setMtConfidence(1.0);

		// Save and reload
		doc = saveAndReload(doc);

		unit = doc.getUnitNode("f1", "u2").get();
		spans = unit.getAnnotatedSpans(true);
		mtc = (MTConfidence)spans.get(0).getMarker().getITSItems().get(DataCategories.MTCONFIDENCE);
		assertEquals(1.0, mtc.getMtConfidence(), 0.0);
		assertEquals("newTool", mtc.getAnnotatorRef());

		match = unit.getMatches().get(0);
		assertEquals("newTool", match.getAnnotatorRef());
		assertEquals(0.9, match.getMTConfidence(), 0.0);
		assertEquals(90.0, match.getMatchQuality(), 0.0);
	}

	private XLIFFDocument load (String filename) {
		File file = root.in("/valid/its/" + filename).asFile();
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(file);
		return doc;
	}

	private XLIFFDocument saveAndReload (XLIFFDocument doc) {
		File out = root.out(doc.getFile().getName()).asFile();
		doc.saveAs(out);
		XLIFFDocument newDoc = new XLIFFDocument();
		newDoc.load(out);
		return newDoc;
	}

}
