package net.sf.okapi.lib.xliff2.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.lib.xliff2.URIParser;
import net.sf.okapi.lib.xliff2.core.ExtContent;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.Note;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.ProcessingInstruction;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Skeleton;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.writer.ExtensionsWriter;

@RunWith(JUnit4.class)
public class XLIFFDocumentTest {

	private FileLocation root = FileLocation.fromClass(this.getClass());
	
	@Test
	public void testSimpleRead () {
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file translate=\"no\" id=\"f1\" original=\"ori\">\n"
			+ "<group id='g1'>"
			+ "<unit id=\"u1\" canResegment=\"no\">\n<segment>\n<source>Source 1.</source><target>Target 1.</target>\n"
			+ "</segment>\n<segment>\n<source>Source 2.</source><target>Target 2.</target>\n</segment>\n</unit>\n</group></file></xliff>";
		
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		FileNode fn = doc.getFileNode("f1");
		assertNotNull(fn);
		GroupNode gn = fn.getGroupNode("g1");
		assertNotNull(gn);
		assertNotNull(doc.getGroupNode("f1", "g1"));
		assertNotNull(fn.getUnitNode("u1"));
		assertNotNull(gn.getUnitNode("u1"));
		assertNotNull(doc.getUnitNode("f1", "u1"));
	}
	
	@Test
	public void testDoubleIteration () throws IOException {
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en'>"
			+ "<file id='f1'>"
			+ "<group id='g11'>"
			+ "<unit id='u111'><segment><source>Source 1.</source></segment></unit>"
			+ "</group>"
			+ "</file>"
			+ "<file id='f2'>"
			+ "<skeleton>data</skeleton>"
			+ "<my:elem xmlns:my='myNS'>extension</my:elem>"
			+ "<group id='g21'>"
			+ "<unit id='u211'><segment><source>Source 2.</source></segment></unit>"
			+ "<group id='g22'>"
			+ "<unit id='u221'><segment><source>Source 3.</source></segment></unit>"
			+ "</group>"
			+ "</group>"
			+ "<unit id='u1'><segment><source>Source 4.</source></segment></unit>"
			+ "</file>"
			+ "</xliff>";
		
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		checkIteration(doc);
		
		// Save this document to a new string
		StringWriter sw = new StringWriter();
		doc.save(sw);
		sw.close();
		// Make sure we use a new document instance
		doc = new XLIFFDocument();
		doc.load(sw.getBuffer().toString(), XLIFFReader.VALIDATION_MAXIMAL);
		checkIteration(doc);
	}
		
	@Test
	public void testFragmentIdentifierAccess () throws IOException {
		XLIFFDocument doc = loadDocument1();
		URIParser up = new URIParser(root.in("/extra-prefixes.properties").asFile());

		Object obj = doc.fetchReference(up.setURL("#f=f1/u=u1/s1"));
		assertTrue(obj instanceof Segment);
		
		obj = doc.fetchReference(up.setURL("#f=f1/u=u1/i1"));
		assertTrue(obj instanceof Part);
		
		obj = doc.fetchReference(up.setURL("#f=f1/g=g1/n=n1"));
		assertTrue(obj instanceof Note);
		assertEquals("g1-note", ((Note)obj).getText());

		obj = doc.fetchReference(up.setURL("#f=f1/res=r1"));
		assertTrue(obj instanceof ExtElement);

		obj = doc.fetchReference(up.setURL("#f=f1/g=g1/my=x1"));
		assertTrue(obj instanceof ExtElement);
	}
		
	@Test
	public void testSpaces () throws IOException {
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en'>"
			+ "<file id='f1'>"
			+ "<group id='g1'>"
			+ "<unit id='u1'><segment><source>[   ]</source></segment></unit>"
			+ "</group>"
			+ "<unit id='u2'><ignorable><source>[   ]</source></ignorable><segment><source>[   ]</source></segment></unit>"
			+ "</file>"
			+ "</xliff>";
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		
		// Save this document to a new string
		doc.setLineBreak("\n");
		StringWriter sw = new StringWriter();
		doc.save(sw);
		sw.close();
		String expected = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<group id=\"g1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<segment>\n"
			+ "<source>[   ]</source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "<unit id=\"u2\">\n"
			+ "<ignorable>\n"
			+ "<source>[   ]</source>\n"
			+ "</ignorable>\n"
			+ "<segment>\n"
			+ "<source>[   ]</source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n"
			+ "</xliff>\n";
		assertEquals(expected, sw.toString());
	}
		
	@Test
	public void testObjectAccess () throws IOException {
		XLIFFDocument doc = loadDocument1();
		
		UnitNode un1 = doc.getUnitNode("f1", "u1");
		assertEquals("u1", un1.get().getId());
		
		GroupNode gn1 = doc.getGroupNode("f1", "g1");
		assertEquals("g1", gn1.get().getId());
		
		FileNode fn = doc.getFileNode("f1");
		assertEquals("f1", fn.getStartData().getId());

		GroupNode gn2 = fn.getGroupNode("g1");
		assertSame(gn1, gn2);
		
		UnitNode un2 = gn2.getUnitNode("u1");
		assertSame(un1, un2);
	}
		
	private void checkIteration (XLIFFDocument doc) {
		Iterator<Event> iter = doc.createEventIterator();
		int count = 0;
		while ( iter.hasNext() ) {
			Event event = iter.next(); count++;
			switch ( count ) {
			case 1:
				assertTrue(event.isStartDocument());
				break;
			case 2:
				assertTrue(event.isStartXliff());
				break;
			case 3:
				assertTrue(event.isStartFile());
				assertEquals("f1", event.getStartFileData().getId());
				break;
			case 4:
				assertTrue(event.isMidFile());
				break;
			case 5:
				assertTrue(event.isStartGroup());
				assertEquals("g11", event.getStartGroupData().getId());
				break;
			case 6:
				assertTrue(event.isUnit());
				assertEquals("u111", event.getUnit().getId());
				break;
			case 7:
				assertTrue(event.isEndGroup());
				break;
			case 8:
				assertTrue(event.isEndFile());
				break;
			case 9:
				assertTrue(event.isStartFile());
				assertEquals("f2", event.getStartFileData().getId());
				break;
			case 10:
				assertTrue(event.isSkeleton());
				break;
			case 11:
				assertTrue(event.isMidFile());
				break;
			case 12:
				assertTrue(event.isStartGroup());
				assertEquals("g21", event.getStartGroupData().getId());
				break;
			case 13:
				assertTrue(event.isUnit());
				assertEquals("u211", event.getUnit().getId());
				break;
			case 14:
				assertTrue(event.isStartGroup());
				assertEquals("g22", event.getStartGroupData().getId());
				break;
			case 15:
				assertTrue(event.isUnit());
				assertEquals("u221", event.getUnit().getId());
				break;
			case 16:
				assertTrue(event.isEndGroup());
				break;
			case 17:
				assertTrue(event.isEndGroup());
				break;
			case 18:
				assertTrue(event.isUnit());
				assertEquals("u1", event.getUnit().getId());
				break;
			case 19:
				assertTrue(event.isEndFile());
				break;
			case 20:
				assertTrue(event.isEndXliff());
				break;
			case 21:
				assertTrue(event.isEndDocument());
				break;
			}
		}
	}
	
	@Test
	public void testGetUnits () {
		XLIFFDocument doc = loadDocument1();
		for ( Unit unit : doc.getUnits() ) {
			assertEquals("u1", unit.getId());
		}
		doc = loadDocument2();
		int n = 0;
		for ( Unit unit : doc.getUnits() ) {
			switch ( n ) {
			case 0: assertEquals("u1", unit.getId()); break;
			case 1: assertEquals("u2", unit.getId()); break;
			case 2: assertEquals("u3", unit.getId()); break;
			}
			n++;
		}
	}

	@Test
	public void testFileAndUnitNodes () {
		XLIFFDocument doc = loadDocument3();
		List<String> fids = doc.getFileNodeIds();
		int f = 0;
		for ( String id : fids ) {
			FileNode fn = doc.getFileNode(id);
			List<UnitNode> nodes = fn.getUnitNodes();
			if ( f == 0 ) {
				assertEquals("f1", fn.getStartData().getId());
				assertEquals(1, nodes.size());
			}
			else if ( f == 1 ) {
				assertEquals("f2", fn.getStartData().getId());
				assertEquals(6, nodes.size());
				assertEquals("f2g1g1u1", nodes.get(2).get().getId());
			}
			else {
				assertTrue("Too many file nodes", true);
			}
			f++;
		}
	}
	
	@Test
	public void testCreation ()
		throws IOException
	{
		XLIFFDocument doc = new XLIFFDocument("en", null);
		IWithGroupOrUnitNode fn1 = doc.addFileNode("f1");
		FileNode fn2 = doc.getFileNode("f1");
		assertSame(fn1, fn2);
		
		// Add unit
		UnitNode un1 = fn1.addUnitNode("u1");
		UnitNode un2 = fn1.getUnitNode("u1");
		assertSame(un1, un2);
		un1.get().appendSegment().getSource().append("source1");
		
		// Add group
		IWithGroupOrUnitNode gn1 = fn1.addGroupNode("g1");
		GroupNode gn2 = fn1.getGroupNode("g1");
		assertSame(gn1, gn2);
		// Add unit to the group
		un1 = gn1.addUnitNode("u2");
		un2 = gn1.getUnitNode("u2");
		assertSame(un1, un2);
		Segment seg = un1.get().appendSegment();
		seg.getSource().append("source2");
	
		// Save this document to a new string
		doc.setLineBreak("\n");
		StringWriter sw = new StringWriter();
		doc.save(sw);
		sw.close();
		String expected = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<segment>\n"
			+ "<source>source1</source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "<group id=\"g1\">\n"
			+ "<unit id=\"u2\">\n"
			+ "<segment>\n"
			+ "<source>source2</source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "</file>\n"
			+ "</xliff>\n";
		assertEquals(expected, sw.toString());
	}

	@Test
	public void testSkeleton ()
		throws IOException
	{
		XLIFFDocument doc = new XLIFFDocument("en", "2.0");
		FileNode fn = doc.addFileNode("f1");
		fn.addUnitNode("u1").get().appendSegment().getSource().append("Text");
		
		Skeleton skel = new Skeleton();
		skel.addChild(new ExtContent("Text 1\n\t  Text 2."));
		skel.addChild(new ProcessingInstruction("<?mystuff abc?>"));
		skel.addChild(new ExtContent("\n"));
		skel.addChild(new ExtContent("<>cdata-stuff", true));
		skel.addChild(new ExtContent("\n"));
		ExtElement xe = new ExtElement(new QName("myStuff", "elem", "mine"));
		xe.addContent("Text Content");
		xe.addChild(new ProcessingInstruction("<?extra pi?>"));
		skel.addChild(xe);
		fn.setSkeletonData(skel);
		
		StringWriter sw = new StringWriter();
		doc.setLineBreak("\n");
		doc.save(sw);
		sw.close();
		
		String sklOut = "Text 1\n\t  Text 2.<?mystuff abc?>\n"
			+ "<![CDATA[<>cdata-stuff]]>\n"
			+ "<mine:elem xmlns:mine=\"myStuff\">Text Content<?extra pi?></mine:elem>";
		
		String expected = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<skeleton>"+sklOut+"</skeleton>\n"
			+ "<unit id=\"u1\">\n"
			+ "<segment>\n"
			+ "<source>Text</source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n"
			+ "</xliff>\n";
		
		ExtensionsWriter xew = new ExtensionsWriter("\n");
		assertEquals(sklOut, xew.buildExtChildren(skel.getChildren(), null));
		
		assertEquals(expected, sw.toString());
		// Re-load to double check the output syntax
		doc.load(sw.toString(), XLIFFReader.VALIDATION_MAXIMAL);
	}
	
	private XLIFFDocument loadDocument1 () {
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'"
			+ " xmlns:res='urn:oasis:names:tc:xliff:resourcedata:2.0'>"
			+ "<file id='f1'>"
			+ "<res:resourceData>"
			+ "<res:resourceItem id='r1' mimeType='text/xml' context='no'>"
			+ "<res:source href='resources/en/registryconfig.resources.xml'/>"
			+ "<res:target href='resources/de/registryconfig.resources.xml'/>"
			+ "</res:resourceItem>"
			+ "</res:resourceData>"
			+ "<group id='g1'>"
			+ "<my:elem xmlns:my='myNS'><my:elem2 xml:id='x1'/></my:elem>"
			+ "<notes><note id='n1'>g1-note</note></notes>"
			+ "<unit id='u1'>"
			+ "<notes><note id='n1'>u1-note</note></notes>"
			+ "<segment id='s1'><source>Source 1.</source></segment>"
			+ "<ignorable id='i1'><source> </source></ignorable>"
			+ "<segment id='s2'><source>Source 2.</source></segment>"
			+ "<ignorable><source> </source></ignorable>"
			+ "<segment><source>Source 3.</source></segment>"
			+ "</unit>"
			+ "</group>"
			+ "</file>"
			+ "</xliff>";
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		return doc;
	}

	private XLIFFDocument loadDocument2 () {
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'"
			+ " xmlns:res='urn:oasis:names:tc:xliff:resourcedata:2.0'>"
			+ "<file id='f1'>"
			+ "<group id='g1'>"
			+ "<my:elem xmlns:my='myNS'><my:elem2 xml:id='x1'/></my:elem>"
			+ "<notes><note id='n1'>g1-note</note></notes>"
			+ "<unit id='u1'>"
			+ "<segment id='s1'><source>Source 11. </source></segment>"
			+ "<segment id='s2'><source>Source 12.</source></segment>"
			+ "</unit>"
			+ "<unit id='u2'>"
			+ "<segment id='s1'><source>Source 21. </source></segment>"
			+ "<segment id='s2'><source>Source 22.</source></segment>"
			+ "</unit>"
			+ "<unit id='u3'>"
			+ "<segment id='s1'><source>Source 31. </source></segment>"
			+ "<segment id='s2'><source>Source 32.</source></segment>"
			+ "</unit>"
			+ "</group>"
			+ "</file>"
			+ "</xliff>";
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		return doc;
	}

	private XLIFFDocument loadDocument3 () {
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'"
			+ " xmlns:res='urn:oasis:names:tc:xliff:resourcedata:2.0'>"
			+ "<file id='f1'>"
			+ " <group id='f1g1'>"
			+ "  <unit id='f1g1u1'>"
			+ "   <segment id='s1'><source>Source</source></segment>"
			+ "  </unit>"
			+ " </group>"
			+ "</file>"
			// File 2
			+ "<file id='f2'>"
			+ " <unit id='f2u1'>"
			+ "  <segment id='s1'><source>Source</source></segment>"
			+ " </unit>"
			+ " <group id='f2g1'>"
			+ "  <unit id='f2g1u1'>"
			+ "   <segment id='s1'><source>Source</source></segment>"
			+ "  </unit>"
			+ "  <group id='f2g1g1'>"
			+ "   <unit id='f2g1g1u1'>"
			+ "    <segment id='s1'><source>Source</source></segment>"
			+ "   </unit>"
			+ "  </group>"
			+ "  <unit id='f2g1u2'>"
			+ "   <segment id='s1'><source>Source</source></segment>"
			+ "  </unit>"
			+ "  <group id='f2g1g2'>"
			+ "   <unit id='f2g1g2u1'>"
			+ "    <segment id='s1'><source>Source</source></segment>"
			+ "   </unit>"
			+ "  </group>"
			+ " </group>"
			+ " <unit id='f2u2'>"
			+ "  <segment id='s1'><source>Source</source></segment>"
			+ " </unit>"
			+ "</file>"
			+ "</xliff>";
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(text, XLIFFReader.VALIDATION_MAXIMAL);
		return doc;
	}

}
