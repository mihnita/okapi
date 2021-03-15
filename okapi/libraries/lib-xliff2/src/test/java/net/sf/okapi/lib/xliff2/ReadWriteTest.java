package net.sf.okapi.lib.xliff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.EventType;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.reader.XLIFFReaderException;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReadWriteTest {

	private static final String STARTDOC = "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" ";
	private FileLocation root = FileLocation.fromClass(ReadWriteTest.class);
	
	//TODO: Fix reporting of trailing WS and comments
	//@Test
	public void testSimpleRewrite1 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n<source>Source 1.</source>\n<target>Target 1.</target>\n"
			+ "</segment>\n<segment>\n<source>Source 2.</source>\n<target>Target 2.</target>\n</segment>\n</unit>\n"
			+ "</file>\n</xliff>\n\n<!--rem-->\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	//TODO: Fix reporting of WS / comments before root element
	//@Test
	public void testSimpleRewrite2 () {
		String text = "<?xml version=\"1.0\"?>\n<!--comment1-->\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<group id=\"g1\" name=\"gn\" type=\"acme:gt\">\n"
			+ "<unit id=\"id\">\n<segment>\n<source>Source 1.</source>\n<target>Target 1.</target>\n"
			+ "</segment>\n<segment>\n<source>Source 2.</source>\n<target>Target 2.</target>\n</segment>\n</unit>\n"
			+ "</group>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testWriteAndRead () {
		XLIFFWriter writer = new XLIFFWriter();
		File file = root.in("/write-read.xlf").asFile();
		writer.create(file, "en", "fr");
		Unit unit = new Unit("u1");
		Segment seg = unit.appendSegment();
		seg.getSource().append("content");
		seg.getTarget(GetTarget.CREATE_EMPTY).append("contenu");
		writer.writeUnit(unit);
		writer.close();
		// Validate
		XLIFFReader.validate(file);
		// Read back
		XLIFFReader reader = new XLIFFReader();
		reader.open(file);
		while ( reader.hasNext() ) {
			Event event = reader.next();
			if ( event.getType() == EventType.TEXT_UNIT ) {
				unit = event.getUnit();
				assertEquals("content", unit.getPart(0).getSource().toString());
				assertEquals("contenu", unit.getPart(0).getTarget().toString());
			}
		}
		reader.close();
		
	}
	
	@Test
	public void testSimpleRewriteAttributes () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<segment>\n<source>Source 1.</source>\n</segment>\n"
			+ "<segment canResegment=\"no\" state=\"final\">\n<source>Source 2.</source>\n<target>Target 2.</target>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		
		Unit unit = getUnit(read(text), 1);
		Segment seg = (Segment)unit.getPart(0);
		assertTrue(seg.getCanResegment());
		seg = (Segment)unit.getPart(1);
		assertFalse(seg.getCanResegment());
		
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testAnnotationRewrite1 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source>Source <mrk id=\"1\" translate=\"no\"><mrk id=\"m1\" type=\"term\" ref=\"someURL\">1</mrk></mrk>.</source>\n"
			+ "<target>Target <mrk id=\"1\" translate=\"no\">1</mrk>.</target>\n"
			+ "</segment>\n<segment>\n<source>Source 2.</source>\n<target>Target 2.</target>\n</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testGroupData () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<group id=\"g1\" type=\"my:type\">\n<unit id=\"id\">\n"
			+ "<segment>\n<source>Source 1.</source>\n</segment>\n"
			+ "<segment canResegment=\"no\" state=\"final\">\n<source>Source 2.</source>\n<target>Target 2.</target>\n</segment>\n"
			+ "</unit>\n</group>\n</file>\n</xliff>\n";
		
		List<Event> events = read(text);
		for ( Event event : events ) {
			if ( event.isStartGroup() ) {
				StartGroupData sgd = event.getStartGroupData();
				assertEquals("my:type", sgd.getType());
				assertEquals("g1", sgd.getId());
			}
		}
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testNamespaceInSource () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\"><pc id=\"1\" fs:fs=\"b\">text</pc></source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source><pc id=\"1\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\" fs:fs=\"b\">text</pc></source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		assertEquals(expected, result);
	}

	@Test
	public void testNamespaceDefault () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<validation xmlns=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<rule isPresent=\"term\"></rule>\n"
			+ "</validation>\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\">text</pc></source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<val:validation xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<val:rule isPresent=\"term\"/>\n"
			+ "</val:validation>\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\">text</pc></source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		assertEquals(expected, result);
	}

	@Test
	public void testWithXLIFFNamespace () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<mtc:matches xmlns:xlf=\"urn:oasis:names:tc:xliff:document:2.0\" xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
			+ "<mtc:match id=\"1\" ref=\"#m1\">\n"
			+ "<xlf:source>Text</xlf:source>\n"
			+ "<xlf:target>Texte</xlf:target>\n"
			+ "</mtc:match>\n"
			+ "</mtc:matches>\n"
			+ "<segment>\n"
			+ "<source><mrk id=\"m1\" type=\"mtc:match\">text</mrk></source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
			+ "<mtc:match ref=\"#m1\" id=\"1\">\n"
			+ "<source>Text</source>\n"
			+ "<target>Texte</target>\n"
			+ "</mtc:match>\n"
			+ "</mtc:matches>\n"
			+ "<segment>\n"
			+ "<source><mrk id=\"m1\" type=\"mtc:match\">text</mrk></source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		assertEquals(expected, result);
	}

	@Test
	public void testRewriteWithSkeleton1 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n<source>text</source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testRewriteWithSkeleton2 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<skeleton>data<?somepi somevalue?></skeleton>\n"
			+ "<unit id=\"id\">\n<segment>\n<source>text</source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testRewriteWithSkeleton3 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<skeleton href=\"skeleton.skl\"></skeleton>\n"
			+ "<unit id=\"id\">\n<segment>\n<source>text</source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

//TODO	@Test
	public void testRewriteWithXMLLangSpace () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\" xml:space=\"preserve\" xml:lang=\"ru\">\n"
			+ "<file id=\"f1\" xml:space=\"default\" xml:lang=\"zh\">\n"
			+ "<group id=\"g1\" xml:space=\"preserve\" xml:lang=\"ja\">\n"
			+ "<unit id=\"id\" xml:space=\"default\" xml:lang=\"pl\">\n"
			+ "<segment>\n"
			+ "<source xml:space=\"preserve\" xml:lang=\"en\">en</source>\n"
			+ "<target xml:space=\"preserve\" xml:lang=\"fr\">fr</target>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "</file>\n"
			+ "</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testRewriteWithExtensionElements () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<my:elem-file xmlns:my=\"myNS\">text</my:elem-file>\n"
			+ "<group id=\"g1\" name=\"gn\" type=\"z:gt\">\n"
			+ "<unit id=\"id\">\n"
			+ "<my:elem-unit xmlns:my=\"myNS\"><?mypi pivalue?></my:elem-unit>\n"
			+ "<segment>\n<source>text</source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "</file>\n"
			+ "</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testXLIFFElementInModule () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<elem1 xmlns=\"myNS1\"><x:source xmlns:x=\"urn:oasis:names:tc:xliff:document:2.0\">data</x:source></elem1>\n"
			+ "<mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
            + "<mtc:match id=\"m1\" ref=\"#s1\" matchQuality=\"88.9\">\n"
            + "<source>text</source>\n"
            + "<target>texte</target>\n"
            + "</mtc:match>\n</mtc:matches>\n"
			+ "<segment id=\"s1\">\n<source>text</source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		// Note the difference in order for the module/extensions in unit
		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<mtc:matches xmlns:mtc=\"urn:oasis:names:tc:xliff:matches:2.0\">\n"
            + "<mtc:match ref=\"#s1\" id=\"m1\" matchQuality=\"88.9\">\n"
            + "<source>text</source>\n"
            + "<target>texte</target>\n"
            + "</mtc:match>\n</mtc:matches>\n"
			+ "<elem1 xmlns=\"myNS1\" xmlns:x=\"urn:oasis:names:tc:xliff:document:2.0\"><x:source>data</x:source></elem1>\n"
			+ "<segment id=\"s1\">\n<source>text</source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testExtensionAttributeInline () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\" xmlns:slr=\"urn:oasis:names:tc:xliff:sizerestriction:2.0\">\n"
			+ "<unit id=\"id\">\n<segment>\n<source>"
			+ "<pc id=\"1\" xmlns:test1=\"uriTest1\" slr:equivStorage=\"7\" slr:sizeRestriction=\"25\">text</pc>\n"
			+ "<sm id=\"m1\" translate=\"yes\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\" fs:fs=\"b\"/>text <sc id=\"2\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\" fs:fs=\"b\"/>"
			+ "<em startRef=\"m1\"/>text"
			+ "<ec startRef=\"2\"/>"
			+ "</source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}
	
	@Test(expected=XLIFFReaderException.class)
	public void testInvalidExtensionAttributeInline () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\" xmlns:slr=\"urn:oasis:names:tc:xliff:sizerestriction:2.0\">\n"
			+ "<unit id=\"id\">\n<segment>\n<source>"
			+ "<pc id=\"1\" xmlns:test1=\"uriTest1\" slr:equivStorage=\"7\" slr:sizeRestriction=\"25\">text</pc>\n"
			+ "<sm id=\"m1\" xmlns:test2=\"uriTest2\" test2:attr=\"val2\"/>text <sc id=\"2\" xmlns:test3=\"uriTest3\" test3:attr=\"val\"/>"
			+ "<em startRef=\"m1\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\" fs:fs=\"b\"/>text"
			+ "<ec startRef=\"2\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\" fs:fs=\"i\"/>"
			+ "</source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}
	
	//@Test
	//TODO: Fix reporting of trailing doc WS
//	public void testRewriteInsignificantParts () {
//		String text = "<?xml version=\"1.0\"?>\n"
//			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
//			+ " <file id=\"f1\">\n"
//			+ "  <unit id=\"id\">\n"
//			+ "   <segment>\n"
//			+ "    <source>text</source>\n"
//			+ "   </segment>\n"
//			+ "  </unit>\n"
//			+ " </file>\n"
//			+ "</xliff>\n";
//		String result = rewrite(text);
//		assertEquals(text, result);
//	}

	@Test
	public void testGlossaryRewrite () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<gls:glossary xmlns:gls=\""+Const.NS_XLIFF_GLOSSARY20+"\">\n"
			+ "<gls:glossEntry>\n"
			+ "<gls:term>term</gls:term>\n"
			+ "<gls:translation>trans</gls:translation>\n"
			+ "<gls:definition>def</gls:definition>\n"
			+ "</gls:glossEntry>\n"
			+ "</gls:glossary>\n"
			+ "<segment>\n"
			+ "<source>Source <mrk id=\"1\" translate=\"no\"><mrk id=\"m1\" type=\"term\" ref=\"someURL\">1</mrk></mrk>.</source>\n"
			+ "<target>Target <mrk id=\"1\" translate=\"no\">1</mrk>.</target>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testNameAndType () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<group id=\"g1\" name=\"gn\" type=\"z:gt\">\n"
			+ "<unit id=\"id\" name=\"un\" type=\"z:ut\">\n<segment>\n"
			+ "<source>Source.</source>\n"
			+ "<target>Target.</target>\n"
			+ "</segment>\n"
			+ "</unit>\n</group>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testCustomPropertiesRewrite1 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\" xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<mda:metadata>\n"
			+ "<mda:metaGroup>\n"
			+ "<mda:meta type=\"ip1\">ipv1</mda:meta>\n"
			+ "<mda:meta type=\"ip2\">ipv2</mda:meta>\n"
			+ "</mda:metaGroup>\n"
			+ "</mda:metadata>\n"
			+ "<segment>\n"
			+ "<source>Source.</source>\n"
			+ "<target>Target.</target>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testCustomPropertiesRewrite2 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<mda:metadata id=\"md1\" xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
			+ "<mda:metaGroup>\n"
			+ "<mda:meta type=\"ip1\">ipv1</mda:meta>\n"
			+ "<mda:meta type=\"ip2\">ipv2</mda:meta>\n"
			+ "</mda:metaGroup>\n"
			+ "</mda:metadata>\n"
			+ "<segment>\n"
			+ "<source>Source.</source>\n"
			+ "<target>Target.</target>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}

	@Test
	public void testAnnotationRewrite2 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source><mrk id=\"m1\" translate=\"no\">in-mrk <sc id=\"1\"/>in-both</mrk> in-pc<ec startRef=\"1\"/>.</source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source><sm id=\"m1\" translate=\"no\"/>in-mrk <sc id=\"1\"/>in-both<em startRef=\"m1\"/> in-pc<ec startRef=\"1\"/>.</source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(expected, result);
	}

	@Test
	public void testSimpleRewrite3 () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source><ph id=\"1\" canDelete=\"no\"/><pc id=\"2\" canCopy=\"no\" canDelete=\"no\" canReorder=\"firstNo\">a</pc></source>\n"
			+ "<target><ph id=\"1\" canDelete=\"no\"/><pc id=\"2\" canCopy=\"no\" canDelete=\"no\" canReorder=\"firstNo\">A</pc></target>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(text, result);
	}
	
	@Test
	public void testAnnotationRewrite () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source><pc id=\"1\">in-pc <sm id=\"m1\" translate=\"no\"/>in-both </pc>in-mrk<em startRef=\"m1\"/>.</source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n<segment>\n"
			+ "<source><sc id=\"1\" canOverlap=\"no\"/>in-pc <sm id=\"m1\" translate=\"no\"/>in-both <ec startRef=\"1\" canOverlap=\"no\"/>in-mrk<em startRef=\"m1\"/>.</source>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		String result = rewrite(text);
		assertEquals(expected, result);
	}

	@Test
	public void testSkeletonMixedRewrite () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<skeleton>Text&amp;=amp.<x:elem xmlns:x=\"myNS\">Text</x:elem></skeleton>\n"
			+ "<unit id=\"id\">\n<segment>\n"
			+ "<source>src</source>\n"
			+ "<target>trg</target>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		
		String result = rewrite(text);
		assertEquals(text, result);
	}
	
	//TODO: Implement support for CDATA separation 
	//@Test
//	public void testSkeletonCDATARewrite () {
//		String text = "<?xml version=\"1.0\"?>\n"
//			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
//			+ "<file id=\"f1\">\n"
//			+ "<skeleton><x:elem xmlns:x=\"myNS\">Text</x:elem><![CDATA[Text&=amp.]]></skeleton>"
//			+ "<unit id=\"id\">\n<segment>\n"
//			+ "<source>src</source>\n"
//			+ "<target>trg</target>\n"
//			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
//		
//		String result = rewrite(text);
//		assertEquals(text, result);
//	}
	
	
	@Test
	public void testSimpleChanges () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<segment>\n<source>Source 1.</source>\n<target>Target 1.</target>\n</segment>\n"
			+ "<segment>\n<source>Source 2.</source>\n<target>Target 2.</target>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		
		List<Event> list = read(text);
		Unit unit = getUnit(list, 1);
		assertNotNull(unit);
		unit.setId("NEWID");
		
		Part part = unit.getPart(0);
		part.getSource().append('Z');
		part.getTarget().append("XYZ");

		Segment seg = (Segment)part;
		seg.setPreserveWS(true);

		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"NEWID\">\n"
			+ "<segment>\n"
			+ "<source xml:space=\"preserve\">Source 1.Z</source>\n<target xml:space=\"preserve\">Target 1.XYZ</target>\n"
			+ "</segment>\n"
			+ "<segment>\n<source>Source 2.</source>\n<target>Target 2.</target>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		assertEquals(expected, write(list));
	}
	
	@Test
	public void testAddTarget () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<segment>\n<source>Source 1.</source>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		
		List<Event> list = read(text);
		for ( Event event : list ) {
			if ( event.isStartXliff() ) {
				StartXliffData dd = event.getStartXliffData();
				dd.setTargetLanguage("FR");
			}
			else if ( event.isUnit() ) {
				Unit unit = event.getUnit();
				unit.getPart(0).getTarget(GetTarget.CLONE_SOURCE);
			}
		}
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"FR\">\n"
				+ "<file id=\"f1\">\n<unit id=\"id\">\n"
				+ "<segment>\n<source>Source 1.</source>\n<target>Source 1.</target>\n</segment>\n"
				+ "</unit>\n</file>\n</xliff>\n";
		assertEquals(expected, write(list));
	}
	
	@Test
	public void testJoiningSegments () {
		String text = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<segment>\n<source>a </source>\n<target>A1 </target>\n</segment>\n"
			+ "<segment>\n<source>b </source>\n<target order=\"4\">B4 </target>\n</segment>\n"
			+ "<segment>\n<source>c </source>\n<target>C3 </target>\n</segment>\n"
			+ "<segment>\n<source>d </source>\n<target order=\"2\">D2 </target>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
		List<Event> list = read(text);
		Unit unit = getUnit(list, 1);
		assertNotNull(unit);
		unit.join(1, 2, true, false); // Join D2 and C3 
		
		String expected = "<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<segment>\n<source>a </source>\n<target>A1 </target>\n</segment>\n"
			+ "<segment>\n<source>b </source>\n<target order=\"3\">B4 </target>\n</segment>\n"
			+ "<segment>\n<source>d c </source>\n<target order=\"2\">D2 C3 </target>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n";
 		assertEquals(expected, write(list));
	}
	
	@Test
	public void testRewriteFiles () {
		read(readFileThenWrite("valid/everything-core.xlf"));
	}
	
//	@Test
//	public void testRewriteAndCompare ()
//		throws MalformedURLException, IOException, SAXException
//	{
//		rewriteFileAndCompare("valid/allExtensions.xlf", "valid/allExtensions.xlf.out");
//	}
	
	private List<Event> read (String document) {
		XLIFFReader reader = new XLIFFReader(XLIFFReader.VALIDATION_MAXIMAL);
		reader.open(document);
		reader.setReportUnsingnificantParts(true);
		List<Event> list = new ArrayList<>();
		while ( reader.hasNext() ) {
			list.add(reader.next());
		}
		reader.close();
		return list;
	}
	
	private String write (List<Event> list) {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "to-be-reset-by-writeEvent");
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		//writer.setUseInsignificantParts(true);
		for ( Event event : list ) {
			writer.writeEvent(event);
		}
		writer.close();
		return strWriter.toString();
	}
	
	private List<Event> readFile (String filename) {
		File file = root.in("/" + filename).asFile();
		XLIFFReader reader = null;
		ArrayList<Event> list = new ArrayList<>();
		try {
			reader = new XLIFFReader(XLIFFReader.VALIDATION_MAXIMAL);
			reader.setReportUnsingnificantParts(true);
			reader.open(file);
			while ( reader.hasNext() ) {
				list.add(reader.next());
			}
		}
		finally {
			if ( reader != null ) reader.close();
		}
		return list;
	}
	
//	private void writeFile (List<Event> list,
//		String outPath)
//	{
//		XLIFFWriter writer = new XLIFFWriter();
//		writer.create(new File(root+"/"+outPath), "to-be-reset-by-writeEvent");
//		writer.setLineBreak("\n");
//		writer.setWithOriginalData(true);
//		//writer.setUseInsignificantParts(true);
//		for ( Event event : list ) {
//			writer.writeEvent(event);
//		}
//		writer.close();
//	}
	
	private String rewrite (String document) {
		return write(read(document));
	}
	
//	private void rewriteFileAndCompare (String inpPath,
//		String outPath)
//		throws MalformedURLException, IOException, SAXException
//	{
//		writeFile(readFile(inpPath), outPath);
//		File ori = new File(root+"/"+inpPath);
//		File out = new File(root+"/"+outPath);
//
//		InputSource original = new InputSource(new BufferedInputStream(ori.toURI().toURL().openStream()));
//		InputSource output = new InputSource(new BufferedInputStream(out.toURI().toURL().openStream()));
//		Diff diff = new Diff(control, test);
//		diff.s
//		XMLAssert.assertXMLEqual(original, output);
//	}
	
	private String readFileThenWrite (String filename) {
		return write(readFile(filename));
	}
	
	private Unit getUnit (List<Event> list,
		int oneBasedIndex)
	{
		int count = 0;
		for ( Event event : list ) {
			if ( event.isUnit() ) {
				count++;
				if ( count == oneBasedIndex ) {
					return event.getUnit();
				}
			}
		}
		return null;
	}

}
