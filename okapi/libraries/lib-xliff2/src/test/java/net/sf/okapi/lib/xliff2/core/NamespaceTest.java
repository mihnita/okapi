package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import javax.xml.namespace.QName;

import net.sf.okapi.lib.xliff2.test.U;
import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NamespaceTest {

	@Test
	public void testAddNamespace () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		StartXliffData dd = new StartXliffData(null);
		dd.getExtAttributes().setNamespace("abc", "myNS");
		
		writer.writeStartDocument(dd, null);
		Unit unit = new Unit("u1");
		unit.appendSegment().getSource().append("content");
		writer.writeUnit(unit);
		writer.close();

		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" "
			+ "srcLang=\"en\" xmlns:abc=\"myNS\">\n"
			+ "<file id=\"f1\">\n<unit id=\"u1\">\n"
			+ "<segment>\n<source>content</source>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}
	
	@Test
	public void testAddNamespaceAndUseIt () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		StartXliffData dd = new StartXliffData(null);
		dd.getExtAttributes().setNamespace("abc", "myNS");
		
		writer.writeStartDocument(dd, null);
		Unit unit = new Unit("u1");
		Fragment frag = unit.appendSegment().getSource();
		frag.append("content");
		frag.annotate(0, -1, "abc:type", "abcValue", null);
		Tag bm = frag.getTag(U.kOA(0));
		bm.getExtAttributes().setAttribute("myNS", "attr", "attr-value");
		
		writer.writeUnit(unit);
		writer.close();

		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" "
			+ "srcLang=\"en\" xmlns:abc=\"myNS\">\n"
			+ "<file id=\"f1\">\n<unit id=\"u1\">\n"
			+ "<segment>\n<source>"
			+ "<mrk id=\"1\" type=\"abc:type\" value=\"abcValue\" abc:attr=\"attr-value\">content</mrk>"
			+ "</source>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}
	
	@Test
	public void testAutoAddWhenUsed () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		Unit unit = new Unit("u1");
		Fragment frag = unit.appendSegment().getSource();
		frag.append("content");
		frag.annotate(0, -1, "abc:type", "abcValue", null);
		Tag bm = frag.getTag(U.kOA(0));
		bm.getExtAttributes().setAttribute("myNS", "attr", "attr-value");
		
		writer.writeUnit(unit);
		writer.close();

		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" "
			+ "srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n<unit id=\"u1\">\n"
			+ "<segment>\n<source>"
			+ "<mrk id=\"1\" type=\"abc:type\" value=\"abcValue\" xmlns:x1=\"myNS\" x1:attr=\"attr-value\">content</mrk>"
			+ "</source>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}
	
	@Test
	public void testAutoAddWithExtensions () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.create(strWriter, "en");

		StartXliffData dd = new StartXliffData(null);
		dd.getExtAttributes().setNamespace("abc", "myNS");
		writer.writeStartDocument(dd, null);

		Unit unit = new Unit("u1");
		Segment seg = unit.appendSegment();
		seg.setId("s1");
		Fragment frag = seg.getSource();
		frag.append("content");
		unit.getExtAttributes().setAttribute("myNS", "attr", "attr-value");
		
		ExtElement matches = new ExtElement(new QName(Const.NS_XLIFF_MATCHES20, "matches"));
		matches.getExtAttributes().setNamespace("xlf", Const.NS_XLIFF_CORE20);
		unit.getExtElements().add(matches);
		// First match
		ExtElement match = new ExtElement(new QName(Const.NS_XLIFF_MATCHES20, "match"));
		match.getExtAttributes().setAttribute("", "ref", "#s1");
		matches.addChild(match);
		ExtElement elem = (ExtElement)match.addChild(new ExtElement(new QName(Const.NS_XLIFF_CORE20, "source")));
		elem.addChild(new ExtContent("s"));
		elem = (ExtElement)match.addChild(new ExtElement(new QName(Const.NS_XLIFF_CORE20, "target")));
		elem.addChild(new ExtContent("t"));
		// Second match (with prefixed XLIFF namespace
		match = new ExtElement(new QName(Const.NS_XLIFF_MATCHES20, "match"));
		match.getExtAttributes().setAttribute("", "ref", "#s1");
		matches.addChild(match);
		elem = (ExtElement)match.addChild(new ExtElement(new QName(Const.NS_XLIFF_CORE20, "source", "xlf")));
		elem.addChild(new ExtContent("s"));
		elem = (ExtElement)match.addChild(new ExtElement(new QName(Const.NS_XLIFF_CORE20, "target", "xlf")));
		elem.addChild(new ExtContent("t"));
		
		writer.writeUnit(unit);
		writer.close();

		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" "
			+ "srcLang=\"en\" xmlns:abc=\"myNS\">\n"
			+ "<file id=\"f1\">\n<unit id=\"u1\" abc:attr=\"attr-value\">\n"
			+ "<matches xmlns=\"urn:oasis:names:tc:xliff:matches:2.0\" xmlns:xlf=\"urn:oasis:names:tc:xliff:document:2.0\">"
			+ "<match ref=\"#s1\">"
			+ "<source xmlns=\"urn:oasis:names:tc:xliff:document:2.0\">s</source>"
			+ "<target xmlns=\"urn:oasis:names:tc:xliff:document:2.0\">t</target>"
			+ "</match>"
			+ "<match ref=\"#s1\">"
			+ "<xlf:source>s</xlf:source>"
			+ "<xlf:target>t</xlf:target>"
			+ "</match>"
			+ "</matches>\n"
			+ "<segment id=\"s1\">\n"
			+ "<source>content</source>\n</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		
		XLIFFReader.validate(strWriter.toString(), null);
	}
	
}
