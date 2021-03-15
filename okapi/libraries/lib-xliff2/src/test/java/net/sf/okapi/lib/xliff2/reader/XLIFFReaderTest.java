package net.sf.okapi.lib.xliff2.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.xml.namespace.QName;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.URIParser;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.changeTracking.ChangeTrack;
import net.sf.okapi.lib.xliff2.changeTracking.Item;
import net.sf.okapi.lib.xliff2.changeTracking.Revision;
import net.sf.okapi.lib.xliff2.changeTracking.Revisions;
import net.sf.okapi.lib.xliff2.core.CanReorder;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.Directionality;
import net.sf.okapi.lib.xliff2.core.ExtAttribute;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.ExtChildType;
import net.sf.okapi.lib.xliff2.core.ExtContent;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.ExtElements;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.IExtChild;
import net.sf.okapi.lib.xliff2.core.InvalidMarkerOrderException;
import net.sf.okapi.lib.xliff2.core.Tags;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Note;
import net.sf.okapi.lib.xliff2.core.Note.AppliesTo;
import net.sf.okapi.lib.xliff2.core.Notes;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Skeleton;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.TargetState;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

@RunWith(JUnit4.class)
public class XLIFFReaderTest {
	private static final String STARTDOC = "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" xmlns:m=\"urn:oasis:names:tc:xliff:matches:2.0\" ";
	private static final String MYNS = "myNamespace";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private FileLocation root = FileLocation.fromClass(this.getClass());
	
	@Test
	public void testWithDefaultNamespace () {
		// XLIFF namespace is the default declaration
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file translate=\"no\" id=\"fid1\" original=\"ori\">\n"
			+ "<unit id=\"id\" canResegment=\"no\">\n<segment>\n<source>Source 1.</source><target>Target 1.</target>\n"
			+ "</segment>\n<segment>\n<source>Source 2.</source><target>Target 2.</target>\n</segment>\n</unit>\n</file></xliff>";
		verifyDocument(text);
	}
	
	@Test
	public void testWithSpecifiedNamespace () {
		// XLIFF namespace is the default declaration
		String text = "<?xml version='1.0'?>\n"
			+ "<z:xliff xmlns:z=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<z:file translate=\"no\" id=\"fid1\" original=\"ori\">\n"
			+ "<z:unit id=\"id\" canResegment=\"no\">\n<z:segment>\n<z:source>Source 1.</z:source><z:target>Target 1.</z:target>\n"
			+ "</z:segment>\n<z:segment>\n<z:source>Source 2.</z:source><z:target>Target 2.</z:target>\n</z:segment>\n</z:unit>\n</z:file></z:xliff>";
		verifyDocument(text);
	}
	
	@Test
	public void testCloseable () {
		String text = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">"
			+ "<file translate=\"no\" id=\"fid1\">\n"
			+ "<unit id=\"id\">\n<segment>\n<source>Source 1.</source>\n"
			+ "</segment>\n</unit>\n</file></xliff>";
		try ( XLIFFReader reader = new XLIFFReader() ) {
			reader.open(text);
			reader.hasNext();
			assertNotNull(reader.next());
		}

	}
	
	@Test
	public void testWithExtensionCharacters () {
		String text = "<?xml version='1.0' encoding='UTF-8'?>\n"
			+ "<xliff xmlns:=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file translate=\"no\" id=\"f1\" original=\"\u2620ori\">\n"
			+ "<unit id=\"id\u00ff\">\n"
			+ "<segment>\n"
			+ "<source>\u2620Source 1.</source>\n"
			+ "<target>\u2620Target 1.</target>\n"
			+ "</segment>\n</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertEquals("id\u00ff", unit.getId());
		assertEquals("\u2620Source 1.", unit.getPart(0).getSource().toString());
		assertEquals("\u2620Target 1.", unit.getPart(0).getTarget().toString());
	}
	
	private void verifyDocument (String data) {
		XLIFFReader reader = new XLIFFReader(XLIFFReader.VALIDATION_MAXIMAL);
		reader.open(data);
		int i = 0;
		while ( reader.hasNext() ) {
			Event e = reader.next();
			switch ( i ) {
			case 0:
				assertTrue(e.isStartDocument());
				break;
			case 1:
				assertSame(EventType.START_XLIFF, e.getType());
				assertTrue(e.isStartXliff());
				StartXliffData dd = e.getStartXliffData();
				assertNotNull(dd);
				assertEquals("en", dd.getSourceLanguage());
				assertEquals("fr", dd.getTargetLanguage());
				assertEquals("2.0", dd.getVersion());
				break;
			case 2:
				assertSame(EventType.START_FILE, e.getType());
				assertTrue(e.isStartFile());
				StartFileData sfd = e.getStartFileData();
				assertNotNull(sfd);
				assertTrue(sfd.getCanResegment());
				assertFalse(sfd.getTranslate());
				assertEquals("fid1", sfd.getId());
				break;
			case 3:
				assertSame(EventType.MID_FILE, e.getType());
				break;
			case 4:
				assertSame(EventType.TEXT_UNIT, e.getType());
				assertTrue(e.isUnit());
				Unit unit = e.getUnit();
				assertNotNull(unit);
				assertFalse(unit.getCanResegment());
				assertFalse(unit.getTranslate());
				assertEquals("id", unit.getId());
				assertEquals("Source 1.", unit.getPart(0).getSource().toString());
				assertEquals("Target 1.", unit.getPart(0).getTarget().toString());
				break;
			case 5:
				assertSame(EventType.END_FILE, e.getType());
				assertTrue(e.isEndFile());
				break;
			case 6:
				assertSame(EventType.END_XLIFF, e.getType());
				assertTrue(e.isEndXliff());
				assertNull(e.getStartXliffData());
				break;
			case 7:
				assertTrue(e.isEndDocument());
				break;
			}
			i++;
		}
		reader.close();
	}

	@Test
	public void testFileData () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"id1\" original=\"ori\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\" fs:fs=\"div\""
			+ " canResegment=\"no\" translate=\"no\" srcDir=\"rtl\" trgDir=\"rtl\">"
			+ " <mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">"
			+ "  <mda:metaGroup category=\"row_xml_attribute\">"
            + "   <mda:meta type=\"style\">head</mda:meta>"
            + "  </mda:metaGroup>"
            + " </mda:metadata>"
			+ " <x:elem xmlns:x=\"myNS\">data</x:elem>"
			+ " <notes>"
			+ "  <note>file-note</note>"
			+ " </notes>"
			+ " <unit id=\"id\"><segment><source>source</source></segment></unit>"
			+ "</file></xliff>";

		StartFileData sfd = getStartFileData(text, 1);
		assertEquals("id1", sfd.getId());
		
		MidFileData mfd = getMidFileData(text, 1);
		assertNotNull(mfd);
		assertEquals(1, mfd.getNoteCount());
		assertEquals(1, mfd.getExtElements().size());
		assertTrue(mfd.hasMetadata());
		assertEquals("row_xml_attribute", mfd.getMetadata().get(0).getCategory());
		assertEquals("elem", mfd.getExtElements().get(0).getQName().getLocalPart());
	}
		
	@Test
	public void testSkeleton () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\" original=\"f1\">\n"
			+ " <skeleton href=\"abc\"/>\n"
			+ " <unit id=\"1\"><segment><source>source1</source></segment></unit>\n"
			+ "</file>\n"
			+ "<file id=\"f2\" original=\"f2\">\n"
			+ " <skeleton>data"
			+ "  <elem xmlns='myNS'>xyz</elem>data"
			+ " </skeleton>\n"
			+ " <unit id=\"1\"><segment><source>source2</source></segment></unit>\n"
			+ "</file>\n"
			+ "</xliff>";
		Skeleton sd = getSkeletonData(text, 1);
		assertEquals("abc", sd.getHref());
		assertNull(sd.getChildren());
		
		sd = getSkeletonData(text, 2);
		assertNull(sd.getHref());
		assertNotNull(sd.getChildren());
		assertEquals(3, sd.getChildren().size()); // text, element, text
	}
		
	@Test
	public void testGroupData () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">"
			+ " <group id=\"g1\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\" fs:fs=\"div\">"
			+ "  <v:validation xmlns:v=\"urn:oasis:names:tc:xliff:validation:2.0\">"
			+ "   <v:rule isNotPresent=\"store\" />"
			+ "  </v:validation>"
			+ "  <x:elem xmlns:x=\"myNS\">data</x:elem>"
			+ "  <notes>"
			+ "   <note>group1-note</note>"
			+ "  </notes>"
			+ "  <unit id=\"u1\">"
			+ "  <v:validation xmlns:v=\"urn:oasis:names:tc:xliff:validation:2.0\">"
			+ "   <v:rule isNotPresent=\"storeUNIT\" />"
			+ "  </v:validation>"
			+"<segment><source>source1</source></segment></unit>"
			+ "  <group id=\"g2\" name='n1' canResegment='no' translate='no' "
			+ "   srcDir='rtl' trgDir='rtl'>"
			+ "   <notes>"
			+ "    <note>group2-note</note>"
			+ "   </notes>"
			+ "   <unit id=\"u2\"><segment><source>source2</source></segment></unit>"
			+ "  </group>"
			+ " </group>"
			+ "</file></xliff>";

		StartGroupData sgd = getStartGroupData(text, 1);
		assertNotNull(sgd);
		assertEquals("g1", sgd.getId());
		assertNull(sgd.getName());
		assertTrue(sgd.getCanResegment());
		assertTrue(sgd.getTranslate());
		assertEquals(Directionality.AUTO, sgd.getSourceDir());
		assertEquals(Directionality.AUTO, sgd.getTargetDir());
		assertEquals("div", sgd.getExtAttributeValue(Const.NS_XLIFF_FS20, "fs"));
		assertEquals("group1-note", sgd.getNotes().get(0).getText());
		assertEquals("elem", sgd.getExtElements().get(0).getQName().getLocalPart());
		assertTrue(sgd.hasValidation());

		sgd = getStartGroupData(text, 2);
		assertNotNull(sgd);
		assertEquals("g2", sgd.getId());
		assertEquals("n1", sgd.getName());
		assertFalse(sgd.getCanResegment());
		assertFalse(sgd.getTranslate());
		assertEquals(Directionality.RTL, sgd.getSourceDir());
		assertEquals(Directionality.RTL, sgd.getTargetDir());
		assertEquals("group2-note", sgd.getNotes().get(0).getText());
	
		Unit unit = getUnit(text, 1);
		assertEquals("store", unit.getValidation().get(0).getData());
		assertEquals("storeUNIT", unit.getValidation().get(1).getData());
	}
		
	@Test
	public void testNotes () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">"
			+ " <notes>"
			+ "  <note id=\"nf1\" priority=\"2\">file-note1</note>"
			+ "  <note id=\"nf2\" priority=\"2\">file-note2</note>"
			+ " </notes>"
			+ " <group id=\"g1\">"
			+ "  <notes>"
			+ "   <note id=\"ng1\" category=\"c2\">group-note1</note>"
			+ "   <note id=\"ng2\" category=\"c2\">group-note2</note>"
			+ "  </notes>"
			+ "  <unit id=\"id\">"
			+ "   <notes xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\">" // Namespace allowed but not attributes
			+ "    <note id=\"nu1\" fs:fs=\"li\" priority=\"2\" category=\"c1\">unit-note1</note>"
			+ "    <note id=\"nu2\" fs:fs=\"li\" priority=\"2\" category=\"c1\">unit-note2</note>"
			+ "   </notes>"
			+ "   <segment>"
			+ "    <source>source</source>"
			+ "   </segment>"
			+ "  </unit>"
			+ " </group>"
			+ "</file></xliff>";

		MidFileData mfd = getMidFileData(text, 1);
		assertNotNull(mfd);
		Notes notes = mfd.getNotes();
		Note note = notes.get(0);
		assertEquals("nf1", note.getId());
		assertEquals(2, note.getPriority());
		assertNull(note.getCategory());
		assertEquals("file-note1", note.getText());
		
		StartGroupData sgd = getStartGroupData(text, 1);
		assertNotNull(sgd);
		notes = sgd.getNotes();
		note = notes.get(1);
		assertEquals("ng2", note.getId());
		assertEquals(1, note.getPriority());
		assertEquals("c2", note.getCategory());
		assertEquals("group-note2", note.getText());
		
		// test unit-level match
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		notes = unit.getNotes();
		assertEquals(2, unit.getNoteCount());
		assertTrue(notes.getExtAttributes().hasNamespace());
		note = notes.get(0);
		assertEquals("unit-note1", note.getText());
		assertEquals(AppliesTo.UNDEFINED, note.getAppliesTo());
		assertEquals("c1", note.getCategory());
		assertEquals("nu1", note.getId());
		assertEquals(2, note.getPriority());
		assertEquals("li", note.getExtAttributeValue("urn:oasis:names:tc:xliff:fs:2.0", "fs"));
	}
	
	@Test
	public void testCDATA () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source><![CDATA[Source 1]]>.</source>"
			+ "<target>Target<![CDATA[ 1.]]></target>\n"
			+ "</segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		assertEquals("Source 1.", unit.getPart(0).getSource().toString());
		assertEquals("Target 1.", unit.getPart(0).getTarget().toString());
	}

	@Test (expected=net.sf.okapi.lib.xliff2.reader.XLIFFReaderException.class)
	public void testDuplicateIds () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source><pc id='1'>a <sc id='2'/>b </pc>c <ec startRef='2'/>d <ph id='2'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=net.sf.okapi.lib.xliff2.reader.XLIFFReaderException.class)
	public void testXmlSpaceMismatch () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source xml:space='preserve'>source</source>"
			+ "<target>target</target>\n" // should have the same xml:space option
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testXmlSpaceWithOnlySource () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source xml:space='preserve'>source</source>"
			+ "</segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertTrue(unit.getPart(0).getPreserveWS());
	}

	@Test (expected=InvalidParameterException.class)
	public void testSourceLangValue () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"q\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source>source</source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=InvalidParameterException.class)
	public void testTargetLangValue () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"-en\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source>source</source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testXmlLangValue () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source xml:lang=\"a-a\">source</source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testInheritedDataContext () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id='f1' translate='no' srcDir='rtl' trgDir='ltr' canResegment='no'>"
			+ "<group id='g1' translate='yes' srcDir='ltr' trgDir='rtl' canResegment='yes'>"
			+ "<unit id='u1' translate='no' srcDir='rtl' trgDir='ltr' canResegment='no'>"
			+ "<segment><source>text1</source>"
			+ "</segment></unit></group></file>"
			+ "<file id='f2' trgDir='rtl'>"
			+ "<group id='g2' translate='no' srcDir='rtl' trgDir='ltr' canResegment='no'>"
			+ "<unit id='u2' translate='yes' srcDir='ltr' trgDir='auto' canResegment='yes'>"
			+ "<segment><source>text1</source>"
			+ "</segment></unit></group></file>"
			+ "</xliff>";
		Unit unit = getUnit(text, 1);
		assertEquals("u1", unit.getId());
		assertFalse(unit.getTranslate());
		assertEquals(Directionality.RTL, unit.getSourceDir());
		assertEquals(Directionality.LTR, unit.getTargetDir());
		assertFalse(unit.getCanResegment());
		
		StartFileData sfd = getStartFileData(text, 1);
		assertEquals("f1", sfd.getId());
		assertFalse(sfd.getTranslate());
		assertEquals(Directionality.RTL, sfd.getSourceDir());
		assertEquals(Directionality.LTR, sfd.getTargetDir());
		assertFalse(sfd.getCanResegment());
		
		StartGroupData sgd = getStartGroupData(text, 1);
		assertEquals("g1", sgd.getId());
		assertTrue(sgd.getTranslate());
		assertEquals(Directionality.LTR, sgd.getSourceDir());
		assertEquals(Directionality.RTL, sgd.getTargetDir());
		assertTrue(sgd.getCanResegment());
		
		unit = getUnit(text, 1);
		assertEquals("u1", unit.getId());
		assertFalse(unit.getTranslate());
		assertEquals(Directionality.RTL, unit.getSourceDir());
		assertEquals(Directionality.LTR, unit.getTargetDir());
		assertFalse(unit.getCanResegment());
		
		sfd = getStartFileData(text, 2);
		assertEquals("f2", sfd.getId());
		assertTrue(sfd.getTranslate());
		assertEquals(Directionality.AUTO, sfd.getSourceDir());
		assertEquals(Directionality.RTL, sfd.getTargetDir());
		assertTrue(sfd.getCanResegment());
		
		sgd = getStartGroupData(text, 2);
		assertEquals("g2", sgd.getId());
		assertFalse(sgd.getTranslate());
		assertEquals(Directionality.RTL, sgd.getSourceDir());
		assertEquals(Directionality.LTR, sgd.getTargetDir());
		assertFalse(sgd.getCanResegment());
		
		unit = getUnit(text, 2);
		assertEquals("u2", unit.getId());
		assertTrue(unit.getTranslate());
		assertEquals(Directionality.LTR, unit.getSourceDir());
		assertEquals(Directionality.AUTO, unit.getTargetDir());
		assertTrue(unit.getCanResegment());
	}

	@Test
	public void testClosingCodeWithPC () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\" fs:fs=\"b\">text</pc></source>"
			+ "</segment>"
			+ "<segment>\n"
			+ "<source><ec id=\"2\" isolated=\"yes\" fs:fs=\"b\"/>text</source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		// Closing marker in <pc> case should not have the fs attribute
		assertFalse(unit.getStore().getSourceTags().get(U.kCC(0)).hasExtAttribute());
		// Closing marker in isolated <ec> case should have the fs attribute
		assertTrue(unit.getStore().getSourceTags().get(U.kCC(1)).hasExtAttribute());
	}

	@Test
	public void testClosingCodeWithMRK () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><mrk id=\"1\" type='comment' value='comment' fs:fs=\"b\">text</mrk></source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		// Closing marker in mrk case should not have the fs attribute
		assertFalse(unit.getStore().getSourceTags().get(U.kCA(0)).hasExtAttribute());
	}

	@Test
	public void testCPElements () {
		char[] chars = Character.toChars(0x10001);
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">"
			+ "<originalData>"
			+ "<data id='1'>[<cp hex=\"0019\"/><cp hex=\"0004\"/><cp hex=\"FFFF\"/>&#x10001;]</data>" //\uD800\uDC01]</data>"
			+ "</originalData>"
			+ "<segment>\n<source>"
			+ "<ph id=\"1\" dataRef='1'/>"
			+ "<cp hex=\"0019\"/><cp hex=\"0004\"/><cp hex=\"FFFF\"/>&#x10001;"
			+ "</source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		
		assertEquals("\u0019\u0004\uFFFF"+chars[0]+chars[1], unit.getPart(0).getSource().getCodedText().substring(2));
		CTag code = (CTag)unit.getStore().getSourceTags().get(U.kSC(0));
		assertEquals("[\u0019\u0004\uFFFF"+chars[0]+chars[1]+"]", code.getData());
		assertEquals("<ph id=\"1\" dataRef=\"d1\"/><cp hex=\"0019\"/><cp hex=\"0004\"/><cp hex=\"FFFF\"/>"+chars[0]+chars[1],
			unit.getPart(0).getSource().toXLIFF(null, null, true));
	}

	@Test
	public void testInvalidCharacters () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">"
			+ "<segment>\n<source><cp hex='D801'/></source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertEquals("\uD801", unit.getPart(0).getSource().getCodedText());
	}

	@Test (expected=XLIFFReaderException.class)
	public void testBadCPValues () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source>"
			+ "<ph id=\"1\" dataRef='abc'></ph>a<cp hex=\"_bad2_\"/>z"
			+ "</source>"
			+ "</segment>"
			+ "<originalData>"
			+ "<data id='abc'>[<cp hex=\"_bad1_\"/>]</data>"
			+ "</originalData>"
			+ "</unit>\n</file></xliff>";
		getUnit(text, 1);
	}
	
	@Test
	public void testDataDir () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">"
			+ "<originalData>"
			+ "<data id='abc' dir='rtl'>data</data>"
			+ "<data id='pcs'>auto-data</data>"
			+ "<data id='pce' dir='rtl'>data</data>"
			+ "<data id='xyz' dir='ltr'>data</data>"
			+ "</originalData>"
			+ "<segment>\n"
			+ "<source><ph id='1' dataRef='abc'></ph>"
			+ "<pc id='2' dataRefStart='pcs' dataRefEnd='pce'>text</pc>"
			+ "<ph id='3' dataRef='xyz'/></source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);

		// code 1
		CTag code = (CTag)unit.getStore().getSourceTags().get(U.kSC(0));
		assertEquals("1", code.getId());
		assertEquals("data", code.getData());
		assertEquals(Directionality.RTL, code.getDataDir());
		// start code 2
		code = (CTag)unit.getStore().getSourceTags().get(U.kOC(0));
		assertEquals("2", code.getId());
		assertEquals("auto-data", code.getData());
		assertEquals(Directionality.AUTO, code.getDataDir());
		// end code 2
		code = (CTag)unit.getStore().getSourceTags().get(U.kCC(0));
		assertEquals("2", code.getId());
		assertEquals("data", code.getData());
		assertEquals(Directionality.RTL, code.getDataDir());
		// code 3
		code = (CTag)unit.getStore().getSourceTags().get(U.kSC(1));
		assertEquals("3", code.getId());
		assertEquals("data", code.getData());
		assertEquals(Directionality.LTR, code.getDataDir());

		// Note that 'd1' is re-used and that is valid
		// It is not re-used for id=3 because the direction is different
		assertEquals("<ph id=\"1\" dataRef=\"d1\"/>"
			+ "<pc id=\"2\" dataRefEnd=\"d1\" dataRefStart=\"d2\">text</pc>"
			+ "<ph id=\"3\" dataRef=\"d3\"/>",
			unit.getPart(0).getSource().toXLIFF(null, null, true));
	}
	
	@Test
	public void testInlineCodes1 () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"sv\" trgLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source>text1 <pc id='1' type='fmt' subType='xlf:b'>text2 text3</pc></source>"
			+ "<target><pc id='1' type='fmt' subType='xlf:b'>text2</pc> text1 <pc id='2' copyOf='1'>text3</pc></target>"
			+ "</segment></unit>"
			+ "<unit id=\"2\"><segment>\n"
			+ "<source>"
			+ "<sc id=\"1\" equiv=\"eq1\" disp=\"di1\"/>t1<pc id=\"2\" equivStart=\"eq2\" dispStart=\"di2\" equivEnd=\"eq2e\" dispEnd=\"de2e\">t2"
				+ "<ph id=\"3\" subFlows=\"1\" equiv=\"eq3\" disp=\"di3\"/>t3"
				+ "</pc><ec startRef=\"1\" equiv=\"eq1e\" disp=\"di1e\"/>"
			+ "</source>"
			+ "</segment></unit>\n</file></xliff>";
		
		Unit unit = getUnit(text, 1);
		CTag code = (CTag)unit.getStore().getTargetTags().get("1", TagType.OPENING);
		assertEquals("1", code.getId());
		assertEquals("fmt", code.getType());
		assertEquals("xlf:b", code.getSubType());
		code = (CTag)unit.getStore().getTargetTags().get("2", TagType.OPENING);
		assertEquals("1", code.getCopyOf());
		assertEquals("2", code.getId());
		assertEquals("", code.getEquiv());

		unit = getUnit(text, 2);
		assertNotNull(unit);
		code = (CTag)unit.getStore().getSourceTags().get(U.kOC(0));
		assertEquals("eq1", code.getEquiv());
		assertEquals("di1", code.getDisp());
		code = (CTag)unit.getStore().getSourceTags().get(U.kOC(1));
		assertEquals("eq2", code.getEquiv());
		assertEquals("di2", code.getDisp());
		code = (CTag)unit.getStore().getSourceTags().get(U.kSC(0));
		assertEquals("eq3", code.getEquiv());
		assertEquals("di3", code.getDisp());
		assertEquals("<pc id=\"1\" canOverlap=\"yes\" equivStart=\"eq1\" dispStart=\"di1\" equivEnd=\"eq1e\" dispEnd=\"di1e\">t1" +
				"<pc id=\"2\" equivStart=\"eq2\" dispStart=\"di2\" equivEnd=\"eq2e\" dispEnd=\"de2e\">t2" +
				"<ph id=\"3\" equiv=\"eq3\" disp=\"di3\" subFlows=\"1\"/>t3" +
				"</pc></pc>",
			unit.getPart(0).getSource().toXLIFF(null, null, false));
	}

	@Test
	public void testInlineCodes2 () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source>"
			+ "<sc id=\"1\" isolated=\"yes\"/>t1<ec id=\"2\" isolated=\"yes\"/>"
			+ "</source>"
			+ "</segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		
		CTag code = (CTag)unit.getStore().getSourceTags().get(U.kOC(0));
		assertEquals("1", code.getId());
		assertEquals(TagType.OPENING, code.getTagType());
		
		code = (CTag)unit.getStore().getSourceTags().get(U.kCC(0));
		assertEquals("2", code.getId());
		assertEquals(TagType.CLOSING, code.getTagType());
		
		assertEquals("<sc id=\"1\" isolated=\"yes\"/>t1<ec id=\"2\" isolated=\"yes\"/>",
			unit.getPart(0).getSource().toXLIFF(null, null, false));
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithDefaultCanCopy () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canCopy='no'/>text<ec startRef='1'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithDefaultCanDelete () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canDelete='no'/>text<ec startRef='1'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithDefaultType () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' type='fmt'/>text<ec startRef='1'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testInlineEMForType () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			// No error, as <em> does not have default
			+ "<source><sm id='1' type='x:abc'/>text<em startRef='1'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithDefaultSubType () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' type='fmt' subType='a:b'/>text<ec startRef='1' type='fmt'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testInlinePCWithReorder () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><pc id='1' canReorder='firstNo' canDelete='no' canCopy='no'>text</pc></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testInlineSCECWithReorder () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canReorder='firstNo' canDelete='no' canCopy='no'/>text<ec startRef='1' canReorder='no' canDelete='no' canCopy='no'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	// This passed without exception because we read 'no' on the ec code, not firstNo
	// Not sure this is right to do
	@Test
	public void testInlineSCECWithReorderBadEC () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canReorder='firstNo' canDelete='no' canCopy='no'/>text<ec startRef='1' canReorder='firstNo' canDelete='no' canCopy='no'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithDefaultReorder () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canReorder='firstNo' canDelete='no' canCopy='no'/>text<ec startRef='1' canDelete='no' canCopy='no'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithBadReorder1 () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canDelete='no' canCopy='no'/>text<ec startRef='1' canReorder='no' canDelete='no' canCopy='no'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithBadReorder2 () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canDelete='no' canCopy='no'/>text<ec startRef='1' canReorder='firstNo' canDelete='no' canCopy='no'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testInlineECWithAllowedReorderValuesForSCFirstNo () {
		// We allow both no and firstNo in ec if firstNo is used in sc
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' canReorder='firstNo' canDelete='no' canCopy='no'/>text<ec startRef='1' canReorder='firstNo' canDelete='no' canCopy='no'/></source>"
			+ "</segment></unit>\n"
			+ "<unit id=\"2\"><segment>"
			+ "<source><sc id='1' canReorder='firstNo' canDelete='no' canCopy='no'/>text<ec startRef='1' canReorder='no' canDelete='no' canCopy='no'/></source>"
			+ "</segment></unit>\n"
			+ "</file></xliff>";
		getUnit(text, 1);
	}

	@Test //(expected=XLIFFReaderException.class)
	public void testInlineECWithDefaultDir () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><sc id='1' dir='rtl'/>text<ec startRef='1'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=XLIFFReaderException.class)
	public void testInlineECWithDefaultCopyOf () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"1\"><segment>"
			+ "<source><pc id='1'></pc><sc id='2' copyOf='1'/>text<ec startRef='2'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testAnnotations () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source>"
			+ "<sm id=\"1\" type=\"comment\" value=\"my note\"/>t1<em startRef=\"1\"/>"
			+ "</source>"
			+ "<target>"
			+ "<mrk id=\"1\" type=\"comment\" value=\"my note trg\">t1</mrk>"
			+ "</target>"
			+ "</segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		
		MTag ann = (MTag)unit.getStore().getSourceTags().get(U.kOA(0));
		assertEquals("comment", ann.getType());
		assertEquals("my note", ann.getValue());
		assertEquals("<mrk id=\"1\" type=\"comment\" value=\"my note\">t1</mrk>",
			unit.getPart(0).getSource().toXLIFF(null, null, false));
		
		ann = (MTag)unit.getStore().getTargetTags().get(U.kCA(0));
		assertEquals("comment", ann.getType());
		assertEquals("my note trg", ann.getValue());
		assertEquals("<mrk id=\"1\" type=\"comment\" value=\"my note trg\">t1</mrk>",
			unit.getPart(0).getTarget().toXLIFF(null, null, false));
	}
	
	@Test
	public void testComments () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source><!--comment-->Source 1.</source>"
			+ "<target>Target<!--comment--> 1.</target>\n"
			+ "</segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		assertEquals("Source 1.", unit.getPart(0).getSource().toXLIFF());
		assertEquals("Target 1.", unit.getPart(0).getTarget().toXLIFF());
	}
	
	@Test
	public void testPI () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source><?myPI?>Source 1.</source>"
			+ "<target>Target<?myPI?> 1.</target>\n"
			+ "</segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		assertEquals("Source 1.", unit.getPart(0).getSource().toString());
		assertEquals("Target 1.", unit.getPart(0).getTarget().toString());
	}
	
//	@Test
//	public void testTranslatable () {
//		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
//			+ "<file id=\"f1\">\n<unit id=\"id\">"
//			+ "<segment>"
//			+ "<source>translatable</source>"
//			+ "</segment>"
//			+ "<segment>"
//			+ "<source>non-translatable</source>"
//			+ "</segment>"
//			+ "<segment translate=\"yes\">"
//			+ "<source>translatable</source>"
//			+ "</segment>"
//			+ "</unit>\n</file></xliff>";
//		Unit unit = getUnit(text, 1);
//		assertNotNull(unit);
//		assertTrue(((Segment)unit.getPart(0)).getTranslate());
//		assertFalse(((Segment)unit.getPart(1)).getTranslate());
//		assertTrue(((Segment)unit.getPart(2)).getTranslate());
//	}
	
	@Test
	public void testInputStreamUsedTwice () {
		String text = "<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source>Source.</source>"
			+ "</segment></unit>\n</file></xliff>";
		InputStream is = new ByteArrayInputStream(text.getBytes());
		XLIFFReader reader = new XLIFFReader(XLIFFReader.VALIDATION_MAXIMAL);
		reader.open(is);
		while ( reader.hasNext() ) {
			Event event = reader.next();
			if ( event.getType() == EventType.TEXT_UNIT ) {
				assertEquals("Source.", event.getUnit().getPart(0).getSource().toString());
			}
		}
		reader.close();
	}
	
	@Test
	public void testWhiteSpaces () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">"
			+ "<originalData>"
			+ "<data id='a'>  A  \t B  </data>"
			+ "</originalData>"
			+ "<segment>"
			+ "<source>a1  b \t c <ph id=\"1\" dataRef='a'></ph></source>"
			+ "</segment>"
			+ "<segment>"
			+ "<source xml:space=\"preserve\">a2  b \t c <ph id=\"2\" dataRef='a'/></source>"
			+ "</segment>"
			+ "<segment>"
			+ "<source xml:space=\"default\">a3  b \t c <ph id=\"3\" dataRef='a'></ph></source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);

		Segment seg = (Segment)unit.getPart(0);
		assertEquals("a1  b \t c <ph id=\"1\" dataRef=\"d1\"/>", seg.getSource().toXLIFF(null, null, true));
		assertFalse(seg.getPreserveWS());
		CTag cm = (CTag)seg.getSource().getTag(U.kSC(0));
		assertEquals("  A  \t B  ", cm.getData());
		
		seg = (Segment)unit.getPart(1);
		assertEquals("a2  b \t c <ph id=\"2\" dataRef=\"d1\"/>", seg.getSource().toXLIFF(null, null, true));
		assertTrue(seg.getPreserveWS());
		
		seg = (Segment)unit.getPart(2);
		assertEquals("a3  b \t c <ph id=\"3\" dataRef=\"d1\"/>", seg.getSource().toXLIFF(null, null, true));
		assertFalse(seg.getPreserveWS());
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testDuplicatedUnitIdValues () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"_testDupId_\"><segment><source>a</source></segment></unit>"
			+ "<unit id=\"id2\"><segment><source>a</source></segment></unit>"
			+ "<unit id=\"id3\"><segment><source>a</source></segment></unit>"
			+ "<unit id=\"_testDupId_\"><segment><source>a</source></segment></unit>"
			+ "</file></xliff>";
		getUnit(text, 4); // Read until the end to trigger the error
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testMissingType () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">"
			+ "<unit id='1'><segment><source>"
			+ "<ph id='1' subType='my:value'/>"
			+ "</source></segment></unit>"
			+ "</file></xliff>";
		getUnit(text, 4); // Read until the end to trigger the error
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testDuplicatedOrderValues () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">"
			+ "<segment>"
			+ "<source>a</source><target order='2'>A</target>"
			+ "</segment>"
			+ "<segment>"
			+ "<source>b</source><target order='2'>B</target>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testBadOrderValues () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">"
			+ "<segment>"
			+ "<source>a</source><target order='5'>A</target>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testBadSourceLanguage () {
		String text = "<?xml version='1.0'?><xliff version='2.0' xmlns='urn:oasis:names:tc:xliff:document:2.0' srcLang='en'>"
			+ "<file id=\"f1\"><unit id='id'>"
			+ "<segment>"
			+ "<source xml:lang='en-us'>a</source>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testBadTargetLanguage () {
		String text = "<?xml version='1.0'?><xliff version='2.0' xmlns='urn:oasis:names:tc:xliff:document:2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\"><unit id='id'>"
			+ "<segment>"
			+ "<source xml:lang='en'>a</source>"
			+ "<target xml:lang='fr-fr'>b</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testMissingTargetLanguage () {
		String text = "<?xml version='1.0'?><xliff version='2.0' xmlns='urn:oasis:names:tc:xliff:document:2.0' srcLang='en'>"
			+ "<file id=\"f1\"><unit id='id'>"
			+ "<segment>"
			+ "<source xml:lang='en'>a</source>"
			+ "<target xml:lang='fr'>b</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testBadInheritedTargetLanguage () {
		String text = "<?xml version='1.0'?><xliff version='2.0' xmlns='urn:oasis:names:tc:xliff:document:2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\" xml:lang='ru'><unit id='id'>"
			+ "<segment>"
			+ "<source xml:lang='en'>a</source>"
			+ "<target>b</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test
	public void testGoodInheritedTargetLanguage () {
		String text = "<?xml version='1.0'?><xliff version='2.0' xmlns='urn:oasis:names:tc:xliff:document:2.0' srcLang='en' trgLang='fr'>"
			+ "<file id=\"f1\" xml:lang='ru'><unit id='id'>"
			+ "<segment>"
			+ "<source xml:lang='en'>a</source>"
			+ "<target xml:lang='fr'>b</target>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test (expected=XLIFFReaderException.class)
	public void testBadInheritedSourceLanguage () {
		String text = "<?xml version='1.0'?><xliff version='2.0' xmlns='urn:oasis:names:tc:xliff:document:2.0' srcLang='en'>"
			+ "<file id=\"f1\"><unit id='id' xml:lang='zh'>"
			+ "<segment>"
			+ "<source>a</source>"
			+ "</segment>"
			+ "</unit></file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
	}
	
	@Test
	public void testIgnorables () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><ignorable>\n"
			+ "<source>  \t</source>"
			+ "<target>\t\t </target>\n"
			+ "</ignorable><segment><source/></segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		assertEquals("  \t", unit.getPart(0).getSource().toString());
		assertEquals("\t\t ", unit.getPart(0).getTarget().toString());
	}

	@Test
	public void testInline () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\" xmlns:fs=\"urn:oasis:names:tc:xliff:fs:2.0\">\n<unit id=\"1\">"
			+ "<originalData>"
			+ "<data id='d1'>[br/]</data>"
			+ "<data id='d2s'>[b]</data>"
			+ "<data id='d2e'>[/b]</data>"
			+ "</originalData>"
			+ "<segment>\n"
			+ "<source>"
			+ "<ph id='1' dataRef='d1' canCopy='no' canDelete='no' canReorder='firstNo' disp='disp1' equiv='equiv1' "
			+ " type='fmt' subType='a:xyz' fs:fs=\"b\"/>"
			+ "<pc id='2' dataRefStart='d2s' dataRefEnd='d2e' canCopy='no' canDelete='no' canReorder='no' dispStart='disp2' equivStart='equiv2' "
			+ " type='fmt' subType='a:xyz' dispEnd='disp2e' equivEnd='equiv2e' fs:fs=\"em\">text</pc>"
			+ "<sc id='3' dataRef='d2s' canCopy='no' canDelete='no' canReorder='no' disp='disp3' equiv='equiv3' "
			+ " type='fmt' subType='a:xyz' fs:fs=\"i\"/>text"
			+ "<ec startRef='3' dataRef='d2e' canCopy='no' canDelete='no' canReorder='no' type='fmt' subType='a:xyz' />"
			+ "</source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		Fragment frag = unit.getPart(0).getSource();
		CTag cm = (CTag)frag.getStore().getSourceTags().get(U.kSC(0)); // ph
		assertEquals("1", cm.getId());
		assertFalse(cm.getCanCopy());
		assertFalse(cm.getCanDelete());
		assertEquals(CanReorder.FIRSTNO, cm.getCanReorder());
		assertFalse(cm.getCanOverlap());
		assertEquals("disp1", cm.getDisp());
		assertEquals("equiv1", cm.getEquiv());
		assertEquals("d1", cm.getDataRef());
		assertEquals("b", cm.getExtAttributeValue(Const.NS_XLIFF_FS20, "fs"));

		cm = (CTag)frag.getStore().getSourceTags().get(U.kOC(0)); // start pc
		assertEquals("2", cm.getId());
		assertFalse(cm.getCanCopy());
		assertFalse(cm.getCanDelete());
		assertEquals(CanReorder.NO, cm.getCanReorder());
		assertFalse(cm.getCanOverlap());
		assertEquals("disp2", cm .getDisp());
		assertEquals("equiv2", cm.getEquiv());
		assertEquals("d2s", cm.getDataRef());
		assertEquals("em", cm.getExtAttributeValue(Const.NS_XLIFF_FS20, "fs"));
	
		cm = (CTag)frag.getStore().getSourceTags().get(U.kCC(0)); // end pc
		assertEquals("2", cm.getId());
		assertFalse(cm.getCanCopy());
		assertFalse(cm.getCanDelete());
		assertEquals(CanReorder.NO, cm.getCanReorder());
		assertFalse(cm.getCanOverlap());
		assertEquals("disp2e", cm .getDisp());
		assertEquals("equiv2e", cm.getEquiv());
		assertEquals("d2e", cm.getDataRef());
		// Modules attributes should not be copied (except for isolated ec)
		assertNull(cm.getExtAttributeValue(Const.NS_XLIFF_FS20, "fs"));
	}

	@Test
	public void testOriginalDataStyles () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">\n"
			+ "<originalData>"
			+ "<data id='1'>[c]</data>"
			+ "<data id='2'>[/c]</data>"
			+ "<data id='3'>[b/]</data>"
			+ "</originalData>"
			+ "<segment><source><pc id='1' dataRefStart='1' dataRefEnd='2'>text</pc></source></segment>"
			+ "<segment><source><sc id='2'></sc>text<ec startRef='2'/></source></segment>"
			+ "<segment><source><ph id='3' dataRef='3'/></source></segment>"
			+ "<segment><source><ph id='4'></ph>text<ph id='5'/></source></segment>"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		Fragment frag = unit.getPart(0).getSource();
		Tags mrks = frag.getStore().getSourceTags();
		assertTrue(((CTag) mrks.get("1", TagType.OPENING)).isInitialWithData());
		assertTrue(((CTag) mrks.get("1", TagType.CLOSING)).isInitialWithData());
		assertFalse(((CTag) mrks.get("2", TagType.OPENING)).isInitialWithData());
		assertFalse(((CTag) mrks.get("2", TagType.CLOSING)).isInitialWithData());
		assertTrue(((CTag) mrks.get("3", TagType.STANDALONE)).isInitialWithData());
		assertFalse(((CTag) mrks.get("4", TagType.STANDALONE)).isInitialWithData());
		assertFalse(((CTag) mrks.get("5", TagType.STANDALONE)).isInitialWithData());
	}

	@Test
	public void testOutsideData () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\">"
			+ "<originalData>"
			+ "<data id='d1'>[1/]</data>"
			+ "<data id='d2'>[2]</data>"
			+ "<data id='d3'>[/2]</data>"
			+ "</originalData>"
			+ "<segment>\n"
			+ "<source>source <ph id='1' dataRef='d1'/> and <pc id='2' dataRefStart='d2' dataRefEnd='d3'>bold</pc></source>"
			+ "</segment>\n"
			+ "</unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		// Test segment-level match
		Segment seg = (Segment)unit.getPart(0);
		assertEquals("source <ph id=\"1\" dataRef=\"d1\"/> and <pc id=\"2\" dataRefEnd=\"d3\" dataRefStart=\"d2\">bold</pc>",
			seg.getSource().toXLIFF(null, null, true));
	}

	@Test
	public void testChangeTrack () {
		String text = "<?xml version=\"1.0\"?>\n"
		        + "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" xmlns:ctr=\"urn:oasis:names:tc:xliff:changetracking:2.0\" version=\"2.0\" srcLang=\"en-US\">\n"
		        + "<file id=\"f1\">\n"
		        + "<unit id=\"unit1\">\n"
		        + "<ctr:changeTrack>\n"
		        + "<ctr:revisions appliesTo=\"note\" ref=\"n1\">\n"
		        + "<ctr:revision author=\"system\" datetime=\"2015-10-21T09:00:00+00:00\">\n"
		        + "<ctr:item property=\"content\">old note</ctr:item>\n"
		        + "</ctr:revision>\n" + "</ctr:revisions>\n"
		        + "<ctr:revisions appliesTo=\"note\" ref=\"n2\">\n"
		        + "<ctr:revision author=\"system2\" datetime=\"2015-10-21T09:00:00+00:00\">\n"
		        + "<ctr:item property=\"content\">old note n2</ctr:item>\n"
		        + "</ctr:revision>\n" + "</ctr:revisions>\n"
		        + "</ctr:changeTrack>\n" 
		        + "<notes>\n"
		        + "<note id=\"n1\">new note</note>\n"
		        + "<note id=\"n2\">another note</note>\n"
		        + "</notes>\n"
		        + "<originalData>\n"
				+ "<data id=\"d1\">&lt;B></data>\n"
				+ "<data id=\"d2\">&lt;/B></data>\n"
				+ "</originalData>"
		        + "<segment>\n"
		        + "<source>Hello<pc id=\"1\" dataRefEnd=\"d2\" dataRefStart=\"d1\">World!</pc>\n"
		        + "</source>\n" + "</segment>\n" + "</unit>\n" + "</file>\n"
		        + "</xliff>";
		Unit unit = getUnit(text, 1);
		ChangeTrack changeTrack = unit.getChangeTrack();
		assertNotNull(changeTrack);

		Revisions revisions = changeTrack.get(0);
		assertNotNull(revisions.getAppliesTo());
		assertEquals("note", revisions.getAppliesTo());
		assertNotNull(revisions.getRef());
		assertEquals("n1", revisions.getRef());
		assertNull(revisions.getCurrentVersion());
		assertFalse(revisions.hasExtAttribute());
		assertEquals(1, revisions.size());
		Revision revision = revisions.get(0);
		assertEquals("system", revision.getAuthor());
		assertEquals("2015-10-21T09:00:00+00:00", revision.getDatetime());
		assertNull(revision.getVersion());
		assertFalse(revision.hasExtAttribute());
		assertEquals(1, revision.size());
		Item item = revision.get(0);
		assertEquals("content", item.getProperty());
		assertFalse(item.hasExtAttribute());
		assertEquals("old note", item.getText());

		revisions = changeTrack.get(1);
		assertNotNull(revisions.getAppliesTo());
		assertEquals("note", revisions.getAppliesTo());
		assertNotNull(revisions.getRef());
		assertEquals("n2", revisions.getRef());
		assertNull(revisions.getCurrentVersion());
		assertFalse(revisions.hasExtAttribute());
		assertEquals(1, revisions.size());
		revision = revisions.get(0);
		assertEquals("system2", revision.getAuthor());
		assertEquals("2015-10-21T09:00:00+00:00", revision.getDatetime());
		assertNull(revision.getVersion());
		assertFalse(revision.hasExtAttribute());
		assertEquals(1, revision.size());
		item = revision.get(0);
		assertEquals("content", item.getProperty());
		assertFalse(item.hasExtAttribute());
		assertEquals("old note n2", item.getText());
	}
	
	@Test(expected = net.sf.okapi.lib.xliff2.reader.XLIFFReaderException.class)
	public void testChangeTrackWrongCurrRevision(){
		String text = "<?xml version=\"1.0\"?>\n"
		        + "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" xmlns:ctr=\"urn:oasis:names:tc:xliff:changetracking:2.0\" version=\"2.0\" srcLang=\"en-US\">\n"
		        + "<file id=\"f1\">\n"
		        + "<unit id=\"unit1\">\n"
		        + "<ctr:changeTrack>\n"
		        + "<ctr:revisions appliesTo=\"note\" ref=\"n1\" currentVersion=\"rev1\">\n"
		        + "<ctr:revision author=\"system\" datetime=\"2015-10-21T09:00:00+00:00\">\n"
		        + "<ctr:item property=\"content\">old note</ctr:item>\n"
		        + "</ctr:revision>\n" + "</ctr:revisions>\n"
		        + "</ctr:changeTrack>\n" 
		        + "<notes>\n"
		        + "<note id=\"n1\">new note</note>\n"
		        + "<note id=\"n2\">another note</note>\n"
		        + "</notes>\n"
		        + "<originalData>\n"
				+ "<data id=\"d1\">&lt;B></data>\n"
				+ "<data id=\"d2\">&lt;/B></data>\n"
				+ "</originalData>"
		        + "<segment>\n"
		        + "<source>Hello<pc id=\"1\" dataRefEnd=\"d2\" dataRefStart=\"d1\">World!</pc>\n"
		        + "</source>\n" + "</segment>\n" + "</unit>\n" + "</file>\n"
		        + "</xliff>";
		getUnit(text, 1);
	}
	@Test
	public void testExtensionElementCount () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n"
			+ "<group id=\"g1\">"
			+ "<mda:metadata>"
			+ "<mda:metaGroup category=\"document_xml_attribute\">"
			+ "<mda:meta type=\"version\">3</mda:meta>"
			+ "<mda:meta type=\"phase\">draft</mda:meta>"
			+ "</mda:metaGroup>"
			+ "</mda:metadata>"
			+ "<group id=\"g2\" name=\"table\">"
			+ "<group id=\"g3\" name=\"row\">"
			+ "<mda:metadata>"
			+ "<mda:metaGroup category=\"row_xml_attribute\">"
			+ "<mda:meta type=\"style\">head</mda:meta>"
			+ "</mda:metaGroup>"
			+ "</mda:metadata>"
			+ "<unit id=\"u1\" name=\"cell\">"
			+ "<segment>"
			+ "<source>Name</source>"
			+ "</segment>"
			+ "</unit></group></group></group>"
			+ "</file></xliff>";
		XLIFFReader.validate(text, null);
	}
	
	@Test
	public void testExtensionAttributes () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" xmlns:x=\"abc\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">"
			+ "<unit id=\"id\" x:a1=\"v1\" x:a2=\"v2\">"
			+ "<segment>"
			+ "<source>src</source>"
			+ "<target>trg</target>\n"
			+ "</segment></unit>\n</file></xliff>";
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		ExtAttributes atts = unit.getExtAttributes();
		assertNotNull(atts);
		ExtAttribute att = atts.getAttribute("abc", "a1");
		assertNotNull(att);
		assertEquals("abc", att.getNamespaceURI());
		assertEquals("x", att.getPrefix());
		assertEquals("a1", att.getLocalPart());
		assertEquals("v1", att.getValue());
	}

	@Test
	public void testExtensionElements () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " xmlns:x=\"myNS\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">"
			+ " <x:elem3 attr1='value1'>file-extension<x:elem2>content2f</x:elem2></x:elem3>"
			+ " <group id='g1'>"
			+ "  <x:elem3 id='x1'><![CDATA[group-extension1]]></x:elem3>" // TODO: This should be reported as CDATA
			+ "  <x:elem3 id='x2'>group-extension2</x:elem3>"
			+ "  <unit id=\"id\">"
			+ "   <x:elem1 attr1='value1'>unit-extension<x:elem2>content2u</x:elem2></x:elem1>"
			+ "   <segment>"
			+ "    <source>source</source>"
			+ "   </segment>"
			+ "  </unit>"
			+ " </group>"
			+ "</file></xliff>";
		
		MidFileData mfd = getMidFileData(text, 1);
		assertNotNull(mfd);
		assertTrue(mfd.hasExtElements());
		ExtElements xelems = mfd.getExtElements();
		assertEquals(1, xelems.size());
		List<ExtElement> res = xelems.find("myNS", "elem3");
		assertTrue(res.size()>0);
		ExtElement elem = res.get(0);
		IExtChild child1 = elem.getChildren().get(0);
		assertSame(ExtChildType.TEXT, child1.getType()); //TODO: revisit parser settings for reporting CDATA
		assertEquals("file-extension", ((ExtContent)child1).getText());
		assertSame(child1, elem.getFirstContent());
		IExtChild child2 = elem.getChildren().get(1);
		assertSame(ExtChildType.ELEMENT, child2.getType());
		assertEquals(new QName("myNS", "elem2"), ((ExtElement)child2).getQName());
		assertSame(child2, elem.getFirstElement());
		
		StartGroupData sgd = getStartGroupData(text, 1);
		assertNotNull(sgd);
		assertTrue(sgd.hasExtElements());
		xelems = sgd.getExtElements();
		assertEquals(2, xelems.size());
		res = xelems.find("myNS", "elem3");
		assertTrue(res.size()>0);
		elem = res.get(0);
		child1 = elem.getChildren().get(0);
		assertSame(ExtChildType.TEXT, child1.getType());
		assertEquals("group-extension1", ((ExtContent)child1).getText());
		elem = res.get(1);
		child1 = elem.getChildren().get(0);
		assertSame(ExtChildType.TEXT, child1.getType());
		assertEquals("group-extension2", ((ExtContent)child1).getText());
		
		Unit unit = getUnit(text, 1);
		assertNotNull(unit);
		assertTrue(unit.hasExtElements());
		xelems = unit.getExtElements();
		assertEquals(1, xelems.size());
		res = xelems.find("myNS", "elem1");
		assertTrue(res.size()>0);
		elem = res.get(0);
		child1 = elem.getChildren().get(0);
		assertSame(ExtChildType.TEXT, child1.getType());
		assertEquals("unit-extension", ((ExtContent)child1).getText());
		assertSame(child1, elem.getFirstContent());
		child2 = elem.getChildren().get(1);
		assertSame(ExtChildType.ELEMENT, child2.getType());
		assertEquals(new QName("myNS", "elem2"), ((ExtElement)child2).getQName());
		assertSame(child2, elem.getFirstElement());
	}

	@Test (expected=XLIFFReaderException.class)
	public void testDuplicateIdOnExtensionElements () {
		String text = "<?xml version='1.0'?>\n<xliff version=\"2.0\" xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " xmlns:x=\"myNS\" srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">"
			+ " <group>"
			+ "  <x:elem3 id='x1Dup'>group-extension1</x:elem3>"
			+ "  <x:elem3 id='x1Dup'>group-extension2</x:elem3>"
			+ "  <unit id=\"id\">"
			+ "   <segment>"
			+ "    <source>source</source>"
			+ "   </segment>"
			+ "  </unit>"
			+ " </group>"
			+ "</file></xliff>";
		getUnit(text, 1);
	}

	@Test
	public void testCommentWithRef () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<notes><note id=\"n1\">note</note></notes>\n"
			+ "<segment>\n"
			+ "<source><mrk id=\"1\" type='comment' ref=\"file.xlf#/f=f1/u=id/n=n1\">text</mrk></source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		getUnit(text, 1); // The ref is OK for sure after #, no check for before #
	}

	@Test (expected=net.sf.okapi.lib.xliff2.reader.XLIFFReaderException.class)
	public void testCommentWithBadRef () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<notes><note id=\"n1\">note</note></notes>\n"
			+ "<segment>\n"
			+ "<source><mrk id=\"1\" type='comment' ref=\"#n=bad\">text</mrk></source>"
			+ "</segment>"
			+ "</unit>\n</file></xliff>";
		getUnit(text, 1); // The ref is OK for sure after #, no check for before #
	}

	@Test
	public void testDisableExternalDTDs () {
		boolean preventedXXE = false;
		String text = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
				"  <!DOCTYPE foo [  \n" +
				"   <!ELEMENT foo ANY >\n" +
				"   <!ENTITY xxe SYSTEM \"file:///dev/random\" >]><foo>&xxe;</foo>";
		try {
			getUnit(text, 1); // dummy operation to trigger validation
		}
		catch (XLIFFException e) {
			// If we block the attempt to load the external resource, we'll get a parsing
			// error. If we don't block it, we'll get some sort of IOException (MalformedByteSequence,
			// FileNotFound, etc, depending on platform).
			assertEquals(SAXParseException.class, e.getCause().getClass());
			preventedXXE = true;
		}
		assertTrue(preventedXXE);
	}

	@Test
	public void testCompleteInput () {
		// Get the external file
		XLIFFReader reader = new XLIFFReader(XLIFFReader.VALIDATION_MAXIMAL);
		reader.open(root.in("/valid/everything-core.xlf").asFile());
		int unitCount = 0;
		// Read it
		while ( reader.hasNext() ) {
			Event event = reader.next();
			switch ( event.getType() ) {
			case START_XLIFF:
				StartXliffData docData = event.getStartXliffData();
				assertEquals("en", docData.getSourceLanguage());
				assertEquals("fr", docData.getTargetLanguage());
				assertEquals("2.0", docData.getVersion());
				assertEquals("value1", docData.getExtAttributeValue(MYNS, "attr"));
				break;
				
			case START_FILE:
				StartFileData sfd = event.getStartFileData();
				assertEquals("f1", sfd.getId());
				assertFalse(sfd.getCanResegment());
				assertFalse(sfd.getTranslate());
				assertEquals("myfile", sfd.getOriginal());
				assertEquals(Directionality.RTL, sfd.getSourceDir());
				assertEquals(Directionality.RTL, sfd.getTargetDir());
				assertEquals("value2", sfd.getExtAttributeValue(MYNS, "attr"));
				break;
			
			case SKELETON:
				Skeleton skelData = event.getSkeletonData();
				assertNull(skelData.getHref());
				List<IExtChild> list = skelData.getChildren();
				assertEquals(1, list.size());
				assertEquals(ExtChildType.TEXT, list.get(0).getType());
				break;

			case MID_FILE:
//				MidFileData mfd = event.getMidFileData();
//				assertEquals(1, mfd.getNoteCount());
//				assertEquals(MDANS, mfd.getExtensionElements().get(0).getQName().getNamespaceURI());
//				assertEquals("metadata", mfd.getExtensionElements().get(0).getQName().getLocalPart());
				break;
				
			case TEXT_UNIT:
				unitCount++;
				Unit unit = event.getUnit();
				if ( unitCount == 1 ) {
					assertEquals("tu1", unit.getId());
					assertEquals("unit1", unit.getName());
					assertEquals(Directionality.LTR, unit.getSourceDir());
					assertEquals(Directionality.LTR, unit.getTargetDir());
					assertTrue(unit.getTranslate());
					assertTrue(unit.getCanResegment());
					assertEquals(3, unit.getPartCount());
					// First segment
					Segment seg = (Segment)unit.getPart(0);
					assertEquals("1", seg.getId());
					assertFalse(seg.getCanResegment());
					assertEquals(TargetState.TRANSLATED, seg.getState());
					assertEquals("my:state", seg.getSubState());
					Fragment frag = seg.getSource();
					assertEquals("Sample segment.", frag.toString());
					assertEquals(Directionality.LTR, frag.getDir(true));
					assertTrue(seg.getPreserveWS());
					assertEquals(3, seg.getTargetOrder());
					assertTrue(seg.hasTarget());
					frag = seg.getTarget();
					assertEquals("Exemple de segment.", frag.toString());
					assertEquals(Directionality.LTR, frag.getDir(true));
					
					// Ignorable
					Part part = unit.getPart(1);
					assertEquals(" ", part.getSource().toString());
					
					// Second segment
					seg = (Segment)unit.getPart(2);
					assertEquals("2", seg.getId());
					assertTrue(seg.getCanResegment());
					assertEquals(TargetState.INITIAL, seg.getState());
					assertNull(seg.getSubState());
					assertFalse(seg.getPreserveWS());
					assertEquals(1, seg.getTargetOrder());
					frag = seg.getSource();
					assertEquals("Segment's content.", frag.toString());
					assertEquals(Directionality.LTR, frag.getDir(true));
					assertTrue(seg.hasTarget());
					frag = seg.getTarget();
					assertEquals("Contenu du segment.", frag.toString());
					assertEquals(Directionality.LTR, frag.getDir(true));
				}
				break;
				
			case END_GROUP:
				break;
			case END_FILE:
				break;
			case END_DOCUMENT:
				break;
			case END_XLIFF:
				break;
			case INSIGNIFICANT_PART:
				break;
			case START_GROUP:
				StartGroupData sgd = event.getStartGroupData();
				assertEquals(1, sgd.getNoteCount());
				Notes notes = sgd.getNotes();
				Note note = notes.get(0);
				assertEquals("note-g1", note.getId());
				assertEquals(AppliesTo.SOURCE, note.getAppliesTo());
				assertEquals("mycat", note.getCategory());
				assertEquals(2, note.getPriority());
				assertEquals("Text of note-g1", note.getText());
				assertEquals("value3", note.getExtAttributeValue(MYNS, "attr"));
				break;
			case START_DOCUMENT:
				break;
			}
		}
		reader.close();
	}
	
	@Test
	public void testInvalidFiles () {
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidLoneSm.xlf"));
		assertEquals(4, processFile(true, true, true, "invalid/bad_InvalidLoneEm.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NotCorrespondingCode1.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NotCorrespondingCode2.xlf"));
		assertEquals(4, processFile(true, true, true, "invalid/bad_EmBeforeSm.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentCanOverlapInScAndEc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentDirInScAndEc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentTypeInScAndEc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentCopyOfInScAndEc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentCanCopyInScAndEc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentSubTypeInScAndEc.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_SrcLangNotWellFormed.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_TrgLangNotWellFormed.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_XmlLangNotWellFormed.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_CommentWithValueAndRef.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidXmlLangInheritedFromFile.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidXmlLangInheritedFromGroup.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidXmlLangInheritedFromUnit.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_WrongLangOnTarget.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdPrefixNotNmtoken.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdPrefixTooShort.xlf"));
		assertEquals(0, processFile(false, false, false, "invalid/bad_InvalidFragIdUnknownPrefix.xlf")); // Test: no error on unknown prefix
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdUnknownPrefix.xlf")); // Test: error on unknown prefix
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdSyntax.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidHexRangeOnCp.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_DataRefWithoutOriginalData.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidTypeValue.xlf")); // Check non-schema error
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidTypeValue.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdMissplacedLeaf.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_MissingNonRemovable1.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_MissingNonRemovable2.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdNoSingleLeaf.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdBadOrder.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidFragIdDuplicatedPrefix.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidNoteRefInUnit.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_GroupWithoutId.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_GroupWithoutId.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_ConfusedIsolatedOnEc.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidIsolatedOnEc.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidIsolatedOnSc.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_MissingIsolatedOnEc.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_MissingIsolatedOnSc.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidCommentAnnotation4.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidCommentAnnotation3.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidCommentAnnotation2.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidCommentAnnotation1.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_SubFlowWithInvalidValue.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_DuplicateExtElemIdsInFile.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_DuplicateExtElemIdsInGroup.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_DuplicateExtElemIdsInUnit.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_DuplicateNoteIdsInFile.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_DuplicateNoteIdsInGroup.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_DuplicateNoteIdsInUnit.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_SubFlowWithInvalidReference.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_CopyOfWithNoCopyReference.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_CopyOfWithOriginalData.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_CopyOfWithBadReference.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_SubStateWithoutState.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidDataRef.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidDataRefStart.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidDataRefEnd.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_YesCanReorderInEcForFirstNoInSc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidDirAttributeOnSource.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidDirAttributeOnSource.xlf"));
		assertEquals(2, processFile(true, true, true, "invalid/bad_InvalidTypeSubTypeValues.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_NoUnitOrGroupInFile.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NoUnitOrGroupInFile.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidExtensionElementInFile.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionElementInFile.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidNotesInFile.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidNotesInFile.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidNotesInGroup.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidNotesInGroup.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/bad_InvalidNotesInUnit.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidNotesInUnit.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidFSAttribute.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidFSAttributeValue.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidId1.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidId2.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidId3.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NoTrgLang.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_FileIdNotUnique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NonEmptySkeletonWithHref.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_EmptySkeletonWithoutHref.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_UnitWithoutSegment.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_SegmentWithoutSource.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_IgnorableWithoutSource.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NotesWithoutNote.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidHexValueOnCp.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_RefAndValueInComment.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_PartIdNotUnique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DataIdNotUnique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_WrongTargetLang.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_WrongSourceLang.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentXmlSpace.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_IsolatedEcWithId.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_GroupIdNotUnique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_UnknownDataRefValue.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_UnknownDataRefStartValue.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_UnknownDataRefEndValue.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_SubTypeWithoutType.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidStateValue.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/bad_InvalidStateValue.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NoFile.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_OriginalDataWithoutData.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentCanDeleteInScAndEc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_DifferentCanReorderInScAndEc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionElementOutsideFile.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionElementInSegment.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionElementInOriginalData.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionElementInData.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NoTrgLangWithIgnorable.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_IgnorableIdNotUnique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_SegmentIdNotUnique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_TwoSourceInUnit.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_EcBeforeSc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_NonIsolatedEcWithStartRef.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_canReorderContext1.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_canReorderContext2.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_canReorderContext3.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_OrderNotUnique1.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_OrderNotUnique2.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_WrongReordering1.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_WrongReordering2.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_MissingReorderFirstNo.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidTranslateInSegment.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionAttributeOnSegment.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionAttributeOnSource.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionAttributeOnTarget.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidExtensionAttributeOnPc.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidValidation.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/bad_InvalidFSAttributeOnEc.xlf"));
	}
	
//	@Test
//	public void testInvalidCTRFiles () {
//		//TODO
//		// Specific to the Change Tracking module
//		assertEquals(1, processFile(true, true, true, "invalid/ctr/Bad-ctr_appliesTo-not-using-ref-to-resolvableID.xlf"));
//		assertEquals(1, processFile(true, true, true, "invalid/ctr/Bad-ctr_property-not-content-or-valid-attribute-ref.xlf"));
//		assertEquals(1, processFile(true, true, true, "invalid/ctr/Bad-ctr_ref-not-pointed-to-resolvableID.xlf"));
//		assertEquals(1, processFile(true, true, true, "invalid/ctr/Bad-ctr_revisions-not-using-ref-to-resolvableID.xlf"));
//		assertEquals(1, processFile(true, true, true, "invalid/ctr/Bad-ctr-property-not-legit-category.xlf"));
//	}
	
	@Test
	public void testInvalidMTCFiles () {
		// Specific to the Translation Candidates module
		assertEquals(1, processFile(true, true, true, "invalid/mtc/Bad-mtc_match-has-xml_lang.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mtc/Bad-mtc_match-ID-not-unique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mtc/Bad-mtc_subType-w-o-type-match.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mtc/Bad-mtc_type-value-not-in-list.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/mtc/Bad-mtc_type-value-not-in-list.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mtc/Bad-mtc_wrong-ref-value.xlf"));
		assertEquals(2, processFile(true, true, true, "invalid/mtc/Bad-mtc_wrong-ref-syntax.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mtc/Bad-mtc_id-not-nmtoken.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/mtc/Bad-mtc_id-not-nmtoken.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mtc/Bad-mtc_similarity-invalid.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/mtc/Bad-mtc_similarity-invalid.xlf"));
	}
	
	@Test
	public void testInvalidGLSFiles () {
		// Specific to the Glossary module
		assertEquals(1, processFile(true, true, true, "invalid/gls/Bad-gls_glossEntry-and-translation-not-unique-in-glossary.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/gls/Bad-gls_glossEntry-w-o-translation-or-definition.xlf"));
	}
	
	@Test
	public void testInvalidMDAFiles () {
		// Specific to the Metadata module
		assertEquals(1, processFile(true, true, true, "invalid/mda/Bad-mda_metadata-id-not-nmtoken.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/mda/Bad-mda_metadata-id-not-nmtoken.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mda/Bad-mda_metaGroup-id-not-nmtoken.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/mda/Bad-mda_metaGroup-id-not-nmtoken.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mda/Bad-mda_meta-missing-type.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/mda/Bad-mda_meta-missing-type.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mda/Bad-mda_missing-metaGroup.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/mda/Bad-mda_missing-metaGroup.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mda/Bad-mda_metaGroup-id-not-unique.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mda/Bad-mda_metaGroup-invalid-appliesTo.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/mda/Bad-mda_metaGroup-invalid-appliesTo.xlf"));
		assertEquals(1, processFile(true, true, true, "invalid/mda/Bad-mda_meta-missplaced-appliesTo.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/mda/Bad-mda_meta-missplaced-appliesTo.xlf"));
	}
	
	@Test
	public void testInvalidVALFiles () {
		// Specific to the Validation module
		assertEquals(1, processFile(true, false, true, "invalid/val/Bad-val_ExactlyOneAttributeOnRule.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/val/Bad-val_existsInSourcePatternOnRule.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/val/Bad-val_invalid-occurs.xlf"));
		assertEquals(2, processFile(true, false, true, "invalid/val/Bad-val_invalid-normalization.xlf"));
		assertEquals(1, processFile(true, false, true, "invalid/val/Bad-val_invalid-caseSensitive.xlf"));
	}
	
	@Test
	public void testFilesWithWarnings () {
		assertEquals(-1, processFile(false, false, true, "warning/no-target-state-not-initial.xlf"));
		assertEquals(-1, processFile(false, false, true, "warning/empty-target-state-not-initial.xlf"));
	}
	
	@Test
	public void testValidFiles () {
		//assertEquals(0, processFile(false, true, true, "valid/correspondingCodes.xlf"));
		File dir = root.in("/valid").asFile();
		File prefixes = root.in("/extra-prefixes.properties").asFile();
		URIParser uriParser = new URIParser(prefixes);
		for ( File file : dir.listFiles() ) {
			if ( file.isDirectory() ) continue;
			String fname = file.getName();
			assertEquals("file:"+fname, 0, processFile(false, true, true, uriParser, "valid/"+fname));
		}
	}
	
	@Test
	public void testValidMTCFiles () {
		File dir = root.in("/valid/mtc").asFile();
		File prefixes = root.in("/extra-prefixes.properties").asFile();
		URIParser uriParser = new URIParser(prefixes);
		for ( File file : dir.listFiles() ) {
			String fname = file.getName();
			assertEquals("file:"+fname, 0, processFile(false, true, true, uriParser, "valid/mtc/"+fname));
		}
	}

	@Test
	public void testValidGLSFiles () {
		File dir = root.in("/valid/gls").asFile();
		File prefixes = root.in("/extra-prefixes.properties").asFile();
		URIParser uriParser = new URIParser(prefixes);
		for ( File file : dir.listFiles() ) {
			String fname = file.getName();
			assertEquals("file:"+fname, 0, processFile(false, true, true, uriParser, "valid/gls/"+fname));
		}
	}

	@Test
	public void testValidVALFiles () {
		File dir = root.in("/valid/val").asFile();
		File prefixes = root.in("/extra-prefixes.properties").asFile();
		URIParser uriParser = new URIParser(prefixes);
		for ( File file : dir.listFiles() ) {
			String fname = file.getName();
			assertEquals("file:"+fname, 0, processFile(false, true, true, uriParser, "valid/val/"+fname));
		}
	}

	@Test
	public void testValidCTRFiles () {
		File dir = root.in("/valid/ctr").asFile();
		File prefixes = root.in("/extra-prefixes.properties").asFile();
		URIParser uriParser = new URIParser(prefixes);
		for ( File file : dir.listFiles() ) {
			String fname = file.getName();
			assertEquals("file:"+fname, 0, processFile(false, true, true, uriParser, "valid/ctr/"+fname));
		}
	}

	@Test (expected=net.sf.okapi.lib.xliff2.reader.XLIFFReaderException.class)
	public void testBadHex1 () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source><cp hex='7FFFFF'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	@Test (expected=net.sf.okapi.lib.xliff2.reader.XLIFFReaderException.class)
	public void testBadHex2 () {
		String text = "<?xml version='1.0'?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">"
			+ "<file id=\"f1\">\n<unit id=\"id\"><segment>\n"
			+ "<source><cp hex='wrong'/></source>"
			+ "</segment></unit>\n</file></xliff>";
		getUnit(text, 1);
	}

	private int processFile (boolean exceptionExpected,
		boolean doSchemaValidation,
		boolean doFragIdPrefixValidation,
		String filename)
	{
		return processFile(exceptionExpected, doSchemaValidation, doFragIdPrefixValidation, null, filename);
	}
	
	private int processFile (boolean exceptionExpected,
		boolean doSchemaValidation,
		boolean doFragIdPrefixValidation,
		URIParser uriParser,
		String filename)
	{
		File file = root.in("/" + filename).asFile();
		if ( exceptionExpected )
			logger.info("Testing (Expected error): {}", filename);
		else
			logger.info("Testing: {}", filename);
		// Process validation
		int validation = 0;
		if ( doSchemaValidation ) validation |= XLIFFReader.VALIDATION_INCLUDE_SCHEMAS;
		if ( doFragIdPrefixValidation ) validation |= XLIFFReader.VALIDATION_INCLUDE_FRAGIDPREFIX;
		// Process the file
		int warnings = 0;
		try ( XLIFFReader reader = new XLIFFReader(validation, uriParser) ) {
			reader.open(file);
			while ( reader.hasNext() ) {
				reader.next();
			}
			warnings = reader.getWarningCount();
		}
		catch ( XLIFFReaderException e ) {
			logger.info(exceptionExpected ? "(Expected error): {}" : "ERROR: {}", e.getMessage());
			return 1;
		}
		catch ( InvalidParameterException e ) {
			logger.info(exceptionExpected ? "(Expected error): {}" : "ERROR: {}", e.getMessage());
			return 2;
		}
		catch ( InvalidMarkerOrderException e ) {
			logger.info(exceptionExpected ? "(Expected error): {}" : "ERROR: {}", e.getMessage());
			return 3;
		}
		catch ( XLIFFException e ) {
			logger.info(exceptionExpected ? "(Expected error): {}" : "ERROR: {}", e.getMessage());
			return 4;
		}
		if ( warnings > 0 ) return -1*warnings;
		else return 0; // No error
	}
	
	/**
	 * Gets a specific object from the parsed data.
	 * @param data the data to process.
	 * @param index the index of the object of the given type to retrieve
	 * @param type the type of object 1=file, 2=end-group, 3=unit, 4=end-file
	 * @return the object or null.
	 */
	private Object getObject (String data,
		int index,
		int type)
	{
		try ( XLIFFReader reader = new XLIFFReader(XLIFFReader.VALIDATION_MAXIMAL) ) {
			reader.open(data);
			int f = 0;
			int u = 0;
			int s = 0;
			int sg = 0;
			int mf = 0;
			while ( reader.hasNext() ) {
				Event e = reader.next();
				if (( type == 3 ) && e.isUnit() ) {
					u++;
					if ( u == index ) {
						return e.getUnit();
					}
				}
				// No more end-group-data
//				else if (( type == 2 ) && e.isEndGroup() ) {
//					g++;
//					if ( g == index ) {
//						return e.getEndGroupData();
//					}
//				}
				else if (( type == 1 ) && e.isStartFile() ) {
					f++;
					if ( f == index ) {
						return e.getStartFileData();
					}
				}
				// No more end-file-data
//				else if (( type == 4 ) && e.isEndFile() ) {
//					f++;
//					if ( f == index ) {
//						return e.getEndFileData();
//					}
//				}
				else if (( type == 5 ) && e.isSkeleton() ) {
					s++;
					if ( s == index ) {
						return e.getSkeletonData();
					}
				}
				else if (( type == 6 ) && e.isStartGroup() ) {
					sg++;
					if ( sg == index ) {
						return e.getStartGroupData();
					}
				}
				else if (( type == 7 ) && e.isMidFile() ) {
					mf++;
					if ( mf == index ) {
						return e.getMidFileData();
					}
				}
			}
		}
		return null;
	}
	
	private Skeleton getSkeletonData (String data,
		int index)
	{
		return (Skeleton)getObject(data, index, 5);
	}
	
	private StartFileData getStartFileData (String data,
		int index)
	{
		return (StartFileData)getObject(data, index, 1);
	}
	
	private MidFileData getMidFileData (String data,
		int index)
	{
		return (MidFileData)getObject(data, index, 7);
	}
	
	private StartGroupData getStartGroupData (String data,
		int index)
	{
		return (StartGroupData)getObject(data, index, 6);
	}
	
	private Unit getUnit (String data,
		int index)
	{
		return (Unit)getObject(data, index, 3);
	}

}
