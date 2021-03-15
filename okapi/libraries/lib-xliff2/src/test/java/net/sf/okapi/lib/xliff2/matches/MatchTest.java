package net.sf.okapi.lib.xliff2.matches;

import java.util.List;

import net.sf.okapi.lib.xliff2.URIParser;
import net.sf.okapi.lib.xliff2.test.U;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.metadata.Meta;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class MatchTest {
	
	@Test
	public void testDefaults () {
		Match m = new Match();
		assertEquals("tm", m.getType());
		assertNull(m.getId());
		assertNull(m.getMatchQuality());
		assertNull(m.getMatchSuitability());
		assertNull(m.getRef());
		assertNull(m.getSimilarity());
		assertNull(m.getSource());
		assertNull(m.getTarget());
		assertFalse(m.isReference());
	}

	@Test
	public void testSetGet () {
		Match m = new Match();
		m.setType("mt");
		assertEquals("mt", m.getType());
		m.setId("mtc1");
		assertEquals("mtc1", m.getId());
		m.setMatchQuality(12.34);
		assertEquals(12.34, m.getMatchQuality(), 0.0);
		m.setMatchSuitability(23.45);
		assertEquals(23.45, m.getMatchSuitability(), 0.0);
		m.setRef("#m1");
		assertEquals("#m1", m.getRef());
		m.setSimilarity(99.9);
		assertEquals(99.9, m.getSimilarity(), 0.0);
		m.setReference(true);
		assertTrue(m.isReference());
		
		Fragment frag = new Fragment(m.getStore(), false);
		frag.append("src");
		m.setSource(frag);
		assertEquals("src", m.getSource().toString());
		
		frag = new Fragment(m.getStore(), true);
		frag.append("trg");
		m.setTarget(frag);
		assertEquals("trg", m.getTarget().toString());
	}

	@Test
	public void testReadReferenceMatch () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
			+ "<mtc:match ref=\"#2\" reference=\"yes\" similarity=\"100.0\" matchQuality=\"88.0\">\n"
			+ "<source>source<ph id=\"1\"/></source>\n"
			+ "<target xml:lang=\"fr-CA\">target-Canadian</target>\n"
			+ "</mtc:match>\n"
			+ "</mtc:matches>\n"
			+ "<segment>\n<source><mrk id=\"2\" type=\"mtc:match\">source<ph id=\"1\"/></mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		Match match = unit.getMatches().get(0);
		assertTrue(match.isReference());
	}
	
	@Test
	public void testAddMatches () {
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
		// For now we create the match before so we can clone the source without the annotation
		//TODO: an option for not doing deep-copy on some annotations
		Fragment frag = unit.getPart(0).getSource();
		
		Match match = new Match();
		match.setSource(new Fragment(frag, match.getStore(), false));
		Match.annotate(frag, 0, -1, match);
		match.setTarget(new Fragment(match.getStore(), true, "target"));
		match.setSimilarity(100.0);
		match.setMatchQuality(88.0);
		match.setType("mt");
		match.setSubType("acme:test");

		match = new Match(match); // Test the copy constructor a bit too
		match.setMatchQuality(77.0);
		match.setType("tm");
		match.setSubType("acme:test2");
		unit.getMatches().add(match);
		
		// Check we have the matches
		for ( Match m : unit.getMatches() ) {
			if ( m.getType().equals("mt") ) {
				assertEquals("acme:test", m.getSubType());
			}
			else if ( m.getType().equals("tm") ) {
				assertEquals("acme:test2", m.getSubType());
			}
		}

		// Test output
		String expected = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
			+ "<mtc:match ref=\"#2\" type=\"mt\" subType=\"acme:test\" similarity=\"100.0\" matchQuality=\"88.0\">\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<source>source<ph id=\"1\" dataRef=\"d1\"/></source>\n"
			+ "<target>target</target>\n"
			+ "</mtc:match>\n"
			+ "<mtc:match ref=\"#2\" type=\"tm\" subType=\"acme:test2\" similarity=\"100.0\" matchQuality=\"77.0\">\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<source>source<ph id=\"1\" dataRef=\"d1\"/></source>\n"
			+ "<target>target</target>\n"
			+ "</mtc:match>\n"
			+ "</mtc:matches>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<segment>\n<source><mrk id=\"2\" type=\"mtc:match\">source<ph id=\"1\" dataRef=\"d1\"/></mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeEvents(events));
		U.getEvents(expected); // Validate
	}

	@Test
	public void testReadMatchesThenWrite () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
			+ "<mtc:match ref=\"#2\" type=\"mt\" subType=\"acme:test\" similarity=\"100.0\" matchQuality=\"88.0\">\n"
			+ "<mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
			+ "<mda:metaGroup id=\"mg1\">\n<mda:meta type=\"blah\">data</mda:meta>\n</mda:metaGroup>\n"
			+ "</mda:metadata>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<source>source<ph id=\"1\" dataRef=\"d1\"/></source>\n"
			+ "<target>target</target>\n"
			+ "</mtc:match>\n"
			+ "</mtc:matches>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<segment>\n<source><mrk id=\"2\" type=\"mtc:match\">source<ph id=\"1\" dataRef=\"d1\"/></mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		assertTrue(unit.hasMatch());
		Matches matches = unit.getMatches();
		Match match = matches.get(0);
		assertEquals("mt", match.getType());
		assertEquals("acme:test", match.getSubType());
		assertEquals("#2", match.getRef());
		assertEquals(100.0, match.getSimilarity(), 0.0);
		assertEquals(88.0, match.getMatchQuality(), 0.0);
		assertEquals("source{hC}", U.fmtMarkers(match.getSource().getCodedText()));
		assertEquals("target", U.fmtMarkers(match.getTarget().getCodedText()));
		assertTrue(match.hasMetadata());
		assertEquals("mg1", match.getMetadata().get(0).getId());
		assertEquals("blah", ((Meta) match.getMetadata().get(0).get(0)).getType());

		assertEquals(text, U.writeEvents(events));
		U.getEvents(text); // Validate
	}

	@Test
	public void testFetchMatchInDocument () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
			+ "<mtc:match id=\"r1\" ref=\"#2\" type=\"mt\" subType=\"acme:test\" similarity=\"100.0\" matchQuality=\"88.0\">\n"
			+ "<mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
			+ "<mda:metaGroup>\n<mda:meta type=\"blah\">data</mda:meta>\n</mda:metaGroup>\n"
			+ "</mda:metadata>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<source>source<ph id=\"1\" dataRef=\"d1\"/></source>\n"
			+ "<target>target</target>\n"
			+ "</mtc:match>\n"
			+ "</mtc:matches>\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br]</data>\n"
			+ "</originalData>\n"
			+ "<segment>\n<source><mrk id=\"2\" type=\"mtc:match\">source<ph id=\"1\" dataRef=\"d1\"/></mrk></source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		URIParser up = new URIParser("#/f=f1/u=id/mtc=r1");
		Object obj = doc.fetchReference(up);
		assertNotNull(obj);
		assertEquals("r1", ((Match)obj).getId());
	}
	
}
