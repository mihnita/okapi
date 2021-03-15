package net.sf.okapi.lib.xliff2.glossary;

import java.util.List;

import net.sf.okapi.lib.xliff2.URIParser;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class GlossaryTest {

	@Test
	public void testCreation () {
		Glossary gloss = new Glossary();
		assertTrue(gloss.isEmpty());
	}
	
	@Test
	public void testCopyConstruct () {
		Glossary gloss1 = new Glossary();
		gloss1.add(createEntry());
		// Copy
		Glossary gloss2 = new Glossary(gloss1);
		assertNotSame(gloss2, gloss1);
		assertEquals(gloss1.get(0).getTerm().getText(),
			gloss2.get(0).getTerm().getText());
	}
	
	@Test
	public void testReadGlossaryThenWrite () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<gls:glossary xmlns:gls=\"urn:oasis:names:tc:xliff:glossary:2.0\" xmlns:my=\"myNamespace\">\n"
			+ "<gls:glossEntry id=\"g1\" ref=\"#m1\" xmlns:my=\"myNamespace\" my:attr=\"value\">\n"
			+ "<gls:term source=\"term1-source\">term1</gls:term>\n"
			+ "<gls:definition>def</gls:definition>\n"
			+ "</gls:glossEntry>\n"
			+ "<gls:glossEntry id=\"g2\" ref=\"#m1\" my:attr1=\"value1\">\n"
			+ "<gls:term source=\"term2-source\">term2</gls:term>\n"
			+ "<gls:translation id=\"g2-t1\" ref=\"#t=m2\" source=\"some-place\" my:attr2=\"value2\">trans2-1</gls:translation>\n"
			+ "<gls:definition source=\"def2-source\">def2</gls:definition>\n"
			+ "<my:elem xmlns:my=\"myNamespace\">data</my:elem>\n"
			+ "</gls:glossEntry>\n"
			+ "</gls:glossary>\n"
			+ "<segment>\n<source><mrk id=\"m1\" type=\"term\">Text</mrk></source>\n"
			+ "<target><mrk id=\"m2\" type=\"term\">trans2-1</mrk></target>"
			+ "\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		Unit unit = U.getUnit(events);
		
		Glossary gloss = unit.getGlossary();
		GlossEntry entry1 = gloss.get(0);
		assertEquals("term1", entry1.getTerm().getText());
		assertEquals("term1-source", entry1.getTerm().getSource());

		GlossEntry entry2 = gloss.get(1);
		assertEquals("term2", entry2.getTerm().getText());
		assertEquals("term2-source", entry2.getTerm().getSource());
		
		GlossEntry entry3 = new GlossEntry(entry2); // Clone the second entry
		entry3.setId("g2-clone");
		entry3.getTranslations().get(0).setId("g2-t1-clone");
		gloss.add(entry3);

		String output = U.writeEvents(events);
		String expected = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<gls:glossary xmlns:gls=\"urn:oasis:names:tc:xliff:glossary:2.0\">\n"
			+ "<gls:glossEntry id=\"g1\" ref=\"#m1\" xmlns:my=\"myNamespace\" my:attr=\"value\">\n"
			+ "<gls:term source=\"term1-source\">term1</gls:term>\n"
			+ "<gls:definition>def</gls:definition>\n"
			+ "</gls:glossEntry>\n"
			+ "<gls:glossEntry id=\"g2\" ref=\"#m1\" xmlns:my=\"myNamespace\" my:attr1=\"value1\">\n"
			+ "<gls:term source=\"term2-source\">term2</gls:term>\n"
			+ "<gls:translation id=\"g2-t1\" ref=\"#t=m2\" source=\"some-place\" my:attr2=\"value2\">trans2-1</gls:translation>\n"
			+ "<gls:definition source=\"def2-source\">def2</gls:definition>\n"
			+ "<my:elem>data</my:elem>\n"
			+ "</gls:glossEntry>\n"
			+ "<gls:glossEntry id=\"g2-clone\" ref=\"#m1\" xmlns:my=\"myNamespace\" my:attr1=\"value1\">\n"
			+ "<gls:term source=\"term2-source\">term2</gls:term>\n"
			+ "<gls:translation id=\"g2-t1-clone\" ref=\"#t=m2\" source=\"some-place\" my:attr2=\"value2\">trans2-1</gls:translation>\n"
			+ "<gls:definition source=\"def2-source\">def2</gls:definition>\n"
			+ "<my:elem>data</my:elem>\n"
			+ "</gls:glossEntry>\n"
			+ "</gls:glossary>\n"
			+ "<segment>\n<source><mrk id=\"m1\" type=\"term\">Text</mrk></source>\n"
			+ "<target><mrk id=\"m2\" type=\"term\">trans2-1</mrk></target>"
			+ "\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, output);
		U.getEvents(output); // Validate
	}

	@Test
	public void testGlossaryInDocument () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<gls:glossary xmlns:gls=\"urn:oasis:names:tc:xliff:glossary:2.0\" xmlns:my=\"myNamespace\">\n"
			+ "<gls:glossEntry id=\"g1\" ref=\"#m1\" xmlns:my=\"myNamespace\" my:attr=\"value\">\n"
			+ "<gls:term source=\"term1-source\">term1</gls:term>\n"
			+ "<gls:definition>def</gls:definition>\n"
			+ "</gls:glossEntry>\n"
			+ "<gls:glossEntry id=\"g2\" ref=\"#m1\" my:attr1=\"value1\">\n"
			+ "<gls:term source=\"term2-source\">term2</gls:term>\n"
			+ "<gls:translation id=\"g2-t1\" ref=\"#t=m2\" source=\"some-place\" my:attr2=\"value2\">trans2-1</gls:translation>\n"
			+ "<gls:definition source=\"def2-source\">def2</gls:definition>\n"
			+ "<my:elem xmlns:my=\"myNamespace\">data</my:elem>\n"
			+ "</gls:glossEntry>\n"
			+ "</gls:glossary>\n"
			+ "<segment>\n<source><mrk id=\"m1\" type=\"term\">Text</mrk></source>\n"
			+ "<target><mrk id=\"m2\" type=\"term\">trans2-1</mrk></target>"
			+ "\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		URIParser up = new URIParser("#/f=f1/u=id/gls=g1");
		Object obj = doc.fetchReference(up);
		assertNotNull(obj);
		GlossEntry entry = (GlossEntry)obj;
		assertEquals("g1", entry.getId());
		up.setURL("#/f=f1/u=id/gls=g2-t1");
		obj = doc.fetchReference(up);
		assertNotNull(obj);
		Translation trans = (Translation)obj;
		assertEquals("g2-t1", trans.getId());
	}	
	
	@Test
	public void testCreateTranslations () {
		GlossEntry ge = new GlossEntry();
		ge.setId("id");
		ge.setTerm(new Term("term")).setSource("term-source");
		ge.getTranslations().add(new Translation("trans1"));
		ge.addTranslation("trans2").setRef("ref2");
		assertEquals("term", ge.getTerm().getText());
		assertEquals("term-source", ge.getTerm().getSource());
		assertEquals("trans1", ge.getTranslations().get(0).getText());
		assertEquals("trans2", ge.getTranslations().get(1).getText());
		assertEquals("ref2", ge.getTranslations().get(1).getRef());
		Translation t = ge.getTranslations().get(1);
		t.setText("newTrans");
		assertEquals("newTrans", ge.getTranslations().get(1).getText());
	}

	private GlossEntry createEntry () {
		GlossEntry ge = new GlossEntry();
		ge.setId("id");
		ge.setRef("ref");
		ge.getExtAttributes().setAttribute("myNS", "attr1", "val1");
		// Term
		ge.setTerm(new Term("term"));
		ge.getTerm().setSource("termSource");
		ge.getExtAttributes().setAttribute("myNS", "termAttr1", "termVal1");
		// Definition
		ge.setDefinition(new Definition("definition"));
		ge.getDefinition().setSource("defSource");
		ge.getDefinition().getExtAttributes().setAttribute("myNS", "defAttr1", "defVal1");
		// Translations
		ge.getTranslations().add(new Translation("trans1"));
		Translation trans = ge.getTranslations().get(0);
		trans.setId("trans1Id");
		trans.setRef("trans1Ref");
		trans.getExtAttributes().setAttribute("myNS", "trans1Attr1", "trans1Val1");
		// Result
		return ge;
	}

}
