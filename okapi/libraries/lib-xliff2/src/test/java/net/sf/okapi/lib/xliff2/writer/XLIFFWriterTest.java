/*===========================================================================
  Copyright (C) 2013-2017 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.lib.xliff2.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringWriter;

import javax.xml.namespace.QName;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.changeTracking.ChangeTrack;
import net.sf.okapi.lib.xliff2.changeTracking.Item;
import net.sf.okapi.lib.xliff2.changeTracking.Revision;
import net.sf.okapi.lib.xliff2.changeTracking.Revisions;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.Directionality;
import net.sf.okapi.lib.xliff2.core.ExtContent;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.ExtElements;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Note;
import net.sf.okapi.lib.xliff2.core.Note.AppliesTo;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.document.GroupNode;
import net.sf.okapi.lib.xliff2.document.UnitNode;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.glossary.GlossEntry;
import net.sf.okapi.lib.xliff2.glossary.Term;
import net.sf.okapi.lib.xliff2.matches.Match;
import net.sf.okapi.lib.xliff2.matches.Matches;
import net.sf.okapi.lib.xliff2.metadata.Meta;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.test.U;
import net.sf.okapi.lib.xliff2.validation.Rule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class XLIFFWriterTest {
	
	private static final String STARTDOC = "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" ";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testEmptyDoc () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		writer.writeStartDocument(null, null);
		writer.writeStartGroup(null);
		writer.writeEndGroup();
		writer.writeEndDocument();
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n<group id=\"g1\">\n</group>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testDirectoryCreation () {
		FileLocation root = FileLocation.fromClass(this.getClass());
		File file = root.out("/dir1/dir two/file with spaces.xlf").asFile();
		file.delete();
		file.getParentFile().delete();
		
		XLIFFWriter writer = new XLIFFWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(file, "en");
		writer.writeStartDocument(null, null);
		writer.writeStartGroup(null);
		writer.writeEndGroup();
		writer.writeEndDocument();
		writer.close();
		assertTrue(file.exists());
		XLIFFReader.validate(file);
	}

	@Test
	public void testPresettingFileId () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		writer.writeStartDocument(null, null);
		StartFileData sfd = new StartFileData("myFileId");
		writer.setStartFileData(sfd);
		// writeStartFile() is called automatically
		writer.writeStartGroup(null);
		writer.writeEndGroup();
		writer.writeEndDocument();
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"myFileId\">\n<group id=\"g1\">\n</group>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testPresettingFileStartButNoId () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		writer.writeStartDocument(null, null);
		StartFileData sfd = new StartFileData(null);
		sfd.setOriginal("original");
		writer.setStartFileData(sfd);
		// writeStartFile() is called automatically
		writer.writeStartGroup(null);
		writer.writeEndGroup();
		writer.writeEndDocument();
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"f1\" original=\"original\">\n<group id=\"g1\">\n</group>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testWithExtensionAttributes () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		StartXliffData dd = new StartXliffData(null);
		dd.setNamespace("its", Const.NS_ITS);
		dd.getExtAttributes().setAttribute(Const.NS_ITS, "version", "2.0");
		writer.writeStartDocument(dd, "comment");
		writer.writeStartGroup(null);
		writer.writeEndGroup();
		writer.writeEndDocument();
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\""
			+ " xmlns:its=\"http://www.w3.org/2005/11/its\" its:version=\"2.0\">\n"
			+ "<!-- comment -->\n"
			+ "<file id=\"f1\">\n<group id=\"g1\">\n</group>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testCloseable () {
		Unit unit = new Unit("id");
		unit.appendSegment().setSource("text");
		StringWriter strWriter = new StringWriter();
		try ( XLIFFWriter writer = new XLIFFWriter() ) {
			writer.create(strWriter, "fr-CA");
			writer.writeUnit(unit);
		}
		Unit ures = U.getUnit(U.getEvents(strWriter.toString()));
		assertEquals("text", ures.getPart(0).getSource().toString());
	}
	
	@Test
	public void testOneUnitWithEmpties () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		// Empty so no output
		writer.writeUnit(new Unit("id1")); 
		// Empty part, so output
		Unit unit = new Unit("id2");
		unit.appendIgnorable();
		writer.writeUnit(unit);
		// One empty segment so output
		unit = new Unit("id3");
		unit.appendSegment(); 
		writer.writeUnit(unit);
		writer.writeEndDocument();
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id2\">\n"
			+ "<ignorable>\n"
			+ "<source></source>\n"
			+ "</ignorable>\n"
			+ "<segment>\n"
			+ "<source></source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "<unit id=\"id3\">\n"
			+ "<segment>\n"
			+ "<source></source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n"
			+ "</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testTwoSegments () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.create(strWriter, "en");
		
		Unit unit = new Unit("id");
		unit.appendSegment().setSource("Source 1.");
		unit.appendSegment().setSource("Source 2.");
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source>Source 1.</source>\n"
			+ "</segment>\n"
			+ "<segment>\n"
			+ "<source>Source 2.</source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testDirectionality () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.create(strWriter, "en", "fr");
		
		StartFileData sd = new StartFileData("idsd");
		sd.setSourceDir(Directionality.RTL);
		sd.setTargetDir(Directionality.RTL);
		writer.writeStartFile(sd);
		// Unit 1 -> same directionality as the file
		Unit unit = new Unit("id1", sd);
		unit.appendSegment().setSource("text1");
		assertEquals(Directionality.RTL, unit.getSourceDir());
		assertEquals(Directionality.RTL, unit.getTargetDir());
		writer.writeUnit(unit);
		// Unit 2 -> unit srcDir as LTR
		unit = new Unit("id2", sd);
		unit.appendSegment().setSource("text2");
		unit.setSourceDir(Directionality.LTR);
		assertEquals(Directionality.LTR, unit.getSourceDir());
		assertEquals(Directionality.RTL, unit.getTargetDir());
		assertEquals(Directionality.LTR, unit.getPart(0).getSource().getDir(true));
		// Unit 2, segment 2 -> source as RTL, target as RTL
		Segment seg = unit.appendSegment();
		seg.setSource("text3");
		seg.getSource().setDir(Directionality.RTL);
		seg.setTarget("trgText3");
		seg.getTarget().setDir(Directionality.RTL);
		// Check that the source dir is different for the two segments
		assertEquals(Directionality.RTL, unit.getPart(1).getSource().getDir(true));
		assertEquals(Directionality.LTR, unit.getPart(0).getSource().getDir(true));
		// Output
		writer.writeUnit(unit);
//TODO: the output is valid but does not reflect the intent as the source/target don't have dir anymore
// See https://code.google.com/p/okapi-xliff-toolkit/issues/detail?id=5
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"idsd\" srcDir=\"rtl\" trgDir=\"rtl\">\n"
			+ "<unit id=\"id1\">\n"
			+ "<segment>\n"
			+ "<source>text1</source>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "<unit id=\"id2\" srcDir=\"ltr\">\n"
			+ "<segment>\n"
			+ "<source>text2</source>\n"
			+ "</segment>\n"
			+ "<segment>\n"
			+ "<source>text3</source>\n"
			+ "<target>trgText3</target>\n" // RTL is inherited from file (not changed by unit)
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testIsolatedAttributeSameUnit () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setWithOriginalData(false);
		
		Unit unit = new Unit("id");
		Segment seg = unit.appendSegment();
		seg.getSource().append(TagType.OPENING, "B1", "<B>", false); // End in next segment
		seg.getSource().append(TagType.OPENING, "U1", "<U>", false); // End in this segment
		seg.getSource().append(TagType.CLOSING, "I1", "</I>", false); // No start
		seg.getSource().append("s1. ");
		seg.getSource().append(TagType.CLOSING, "U1", "</U>", false);
		seg = unit.appendSegment();
		seg.getSource().append("s2.");
		seg.getSource().append(TagType.CLOSING, "B1", "</B>", false);
		// No end of Italic
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><sc id=\"B1\" canOverlap=\"no\"/><sc id=\"U1\" canOverlap=\"no\"/><ec id=\"I1\" isolated=\"yes\" canOverlap=\"no\"/>s1. <ec startRef=\"U1\" canOverlap=\"no\"/></source>\n"
			+ "</segment>\n"
			+ "<segment>\n"
			+ "<source>s2.<ec startRef=\"B1\" canOverlap=\"no\"/></source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

//	@Test
//	public void testSegmentWithMatches () {
//		XLIFFWriter writer = new XLIFFWriter();
//		StringWriter strWriter = new StringWriter();
//		writer.create(strWriter, "en");
//		writer.setLineBreak("\n");
//		writer.setInlineStyle(OriginalDataStyle.OUTSIDE);
//		
//		Unit unit = new Unit("id");
//		Segment seg = unit.appendNewSegment();
//		seg.setSource("Source 1.");
//		seg.addCandidate(createAlternate("seg", 77.54f));
//		unit.addCandidate(createAlternate("unit", 99.00f));
//		writer.writeUnit(unit);
//		
//		writer.close();
//		assertEquals("<?xml version=\"1.0\"?>\n"
//			+ STARTDOC+"srcLang=\"en\">\n"
//			+ "<file id=\"f1\">\n"
//			+ "<unit id=\"id\">\n"
//			+ "<segment>\n"
//			+ "<source>Source 1.</source>\n"
//			+ "<m:matches>\n"
//			+ "<m:match similarity=\"77.54\" type=\"tm\" origin=\"ori\">\n"
//			+ "<source>seg-text<ph id=\"1\" dataRef=\"d1\"/></source>\n"
//			+ "<target>SEG-TEXT<ph id=\"1\" dataRef=\"d1\"/></target>\n"
//			+ "<originalData>\n"
//			+ "<data id=\"d1\">&lt;br/></data>\n"
//			+ "</originalData>\n"
//			+ "</m:match>\n"
//			+ "</m:matches>\n"
//			+ "</segment>\n"
//			+ "<m:matches>\n"
//			+ "<m:match similarity=\"99\" type=\"tm\" origin=\"ori\">\n"
//			+ "<source>unit-text<ph id=\"1\" dataRef=\"d1\"/></source>\n"
//			+ "<target>UNIT-TEXT<ph id=\"1\" dataRef=\"d1\"/></target>\n"
//			+ "<originalData>\n"
//			+ "<data id=\"d1\">&lt;br/></data>\n"
//			+ "</originalData>\n"
//			+ "</m:match>\n"
//			+ "</m:matches>\n"
//			+ "</unit>\n</file>\n</xliff>\n",
//			strWriter.toString());
//	}

	@Test
	public void testNotes () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setUseIndentation(true);
		writer.setWithOriginalData(true);
		
		MidFileData midFileData = new MidFileData();
		midFileData.addNote(new Note("File note", AppliesTo.UNDEFINED));
		writer.writeMidFile(midFileData);

		Note note = new Note("Group note", AppliesTo.SOURCE);
		note.setId("n1");
		note.setPriority(2);
		note.setCategory("myCat");
		StartGroupData startGroupData = new StartGroupData("g1");
		startGroupData.addNote(note);
		writer.writeStartGroup(startGroupData);
		
		Unit unit = new Unit("id");
		Segment seg = unit.appendSegment();
		seg.setSource("Source").appendCode("1", "[br/]");
		unit.addNote(new Note("Unit note1", AppliesTo.SOURCE));
		unit.addNote(new Note("Unit note2", AppliesTo.UNDEFINED));
		writer.writeUnit(unit);
		
		writer.writeEndGroup();

		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\">\n"
			+ " <file id=\"f1\">\n"
			+ "  <notes>\n"
			+ "   <note>File note</note>\n"
			+ "  </notes>\n"
			+ "  <group id=\"g1\">\n"
			+ "   <notes>\n"
			+ "    <note id=\"n1\" appliesTo=\"source\" priority=\"2\" category=\"myCat\">Group note</note>\n"
			+ "   </notes>\n"
			+ "   <unit id=\"id\">\n"
			+ "    <notes>\n"
			+ "     <note appliesTo=\"source\">Unit note1</note>\n"
			+ "     <note>Unit note2</note>\n"
			+ "    </notes>\n"
			+ "    <originalData>\n"
			+ "     <data id=\"d1\">[br/]</data>\n"
			+ "    </originalData>\n"
			+ "    <segment>\n"
			+ "     <source>Source<ph id=\"1\" dataRef=\"d1\"/></source>\n"
			+ "    </segment>\n"
			+ "   </unit>\n"
			+ "  </group>\n"
			+ " </file>\n"
			+ "</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}
	
	@Test
	public void testChangeTrack(){
		
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setUseIndentation(true);
		writer.setWithOriginalData(true);
		
		Unit unit = new Unit("id");
		Segment seg = unit.appendSegment();
		seg.setSource("Source").appendCode("1", "[br/]");
		Note note = new Note("Unit note1", AppliesTo.SOURCE);
		note.setId("n1");
		unit.addNote(note);
		note = new Note("Unit note2", AppliesTo.UNDEFINED);
		note.setId("n2");
		unit.addNote(note);
		ChangeTrack chTrack = new ChangeTrack();
		Revisions revisions = new Revisions("note");
		revisions.setRef("n1");
		chTrack.add(revisions);
		Revision revision = new Revision();
		revision.setAuthor("system");
		revision.setDatetime("2015-10-21T09:00:00+00:00");
		revisions.add(revision);
		Item item = new Item("content");
		item.setText("old note");
		revision.add(item);
		unit.setChangeTrack(chTrack);
		
		writer.writeUnit(unit);
		writer.close();
		logger.info(strWriter.toString());
		String expectedString = "<?xml version=\"1.0\"?>\n"
				+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
				+ " <file id=\"f1\">\n"
				+ "  <unit id=\"id\">\n"
				+ "   <ctr:changeTrack xmlns:ctr=\"urn:oasis:names:tc:xliff:changetracking:2.0\">\n"
				+ "    <ctr:revisions appliesTo=\"note\" ref=\"n1\">\n"
				+ "     <ctr:revision author=\"system\" datetime=\"2015-10-21T09:00:00+00:00\">\n"
				+ "      <ctr:item property=\"content\">old note</ctr:item>\n"
				+ "     </ctr:revision>\n"
				+ "    </ctr:revisions>\n"
				+ "   </ctr:changeTrack>\n"
				+ "   <notes>\n"
				+ "    <note id=\"n1\" appliesTo=\"source\">Unit note1</note>\n"
				+ "    <note id=\"n2\">Unit note2</note>\n"
				+ "   </notes>\n"
				+ "   <originalData>\n"
				+ "    <data id=\"d1\">[br/]</data>\n"
				+ "   </originalData>\n"
				+ "   <segment>\n"
				+ "    <source>Source<ph id=\"1\" dataRef=\"d1\"/></source>\n"
				+ "   </segment>\n"
				+ "  </unit>\n"
				+ " </file>\n"
				+ "</xliff>\n";
		assertEquals(expectedString, strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

//	@Test
//	public void testUnitWithcustomProperties () {
//		XLIFFWriter writer = new XLIFFWriter();
//		StringWriter strWriter = new StringWriter();
//		writer.create(strWriter, "en");
//		writer.setLineBreak("\n");
//		writer.setInlineStyle(OriginalDataStyle.OUTSIDE);
//		
//		Unit unit = new Unit("id");
//		unit.getCustomProperties().put("up1", "upv1");
//		unit.getCustomProperties().put("up2", "upv2");
//		ISegment seg = unit.appendNewSegment();
//		seg.setSource("Source.");
//		seg.getCustomProperties().put("sp1", "spv1");
//		seg.getCustomProperties().put("sp2", "spv2");
//		IPart ign = unit.appendNewIgnorable();
//		ign.setSource(" ign");
//		ign.getCustomProperties().put("ip1", "ipv1");
//		ign.getCustomProperties().put("ip2", "ipv2");
//		
//		writer.writeUnit(unit);
//		writer.close();
//
//		assertEquals("<?xml version=\"1.0\"?>\n"
//			+ STARTDOC+"srcLang=\"en\">\n"
//			+ "<file id=\"f1\">\n"
//			+ "<unit id=\"id\">\n"
//			+ "<segment>\n"
//			+ "<source>Source.</source>\n"
//			+ "<p:metadata xmlns:p=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
//			+ "<p:meta type=\"sp1\">spv1</p:meta>\n"
//			+ "<p:meta type=\"sp2\">spv2</p:meta>\n"
//			+ "</p:metadata>\n"
//			+ "</segment>\n"
//			+ "<ignorable>\n"
//			+ "<source> ign</source>\n"
//			+ "<p:metadata xmlns:p=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
//			+ "<p:meta type=\"ip1\">ipv1</p:meta>\n"
//			+ "<p:meta type=\"ip2\">ipv2</p:meta>\n"
//			+ "</p:metadata>\n"
//			+ "</ignorable>\n"
//			+ "<p:metadata xmlns:p=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
//			+ "<p:meta type=\"up1\">upv1</p:meta>\n"
//			+ "<p:meta type=\"up2\">upv2</p:meta>\n"
//			+ "</p:metadata>\n"
//			+ "</unit>\n</file>\n</xliff>\n",
//			strWriter.toString());
//	}

	@Test
	public void testSegmentOutputWithCodesOutside () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en", "fr");
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		
		Unit unit = new Unit("id");
		createSegment(unit);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">&lt;b></data>\n"
			+ "<data id=\"d2\">&lt;/b></data>\n"
			+ "<data id=\"d3\">&lt;![CDATA[..&lt;c/>..]]&gt;</data>\n"
			+ "<data id=\"d4\">&lt;/B></data>\n"
			+ "</originalData>\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\" dataRefEnd=\"d2\" dataRefStart=\"d1\">source</pc><ph id=\"2\" dataRef=\"d3\"/></source>\n"
			+ "<target><pc id=\"1\" dataRefEnd=\"d4\" dataRefStart=\"d1\">target</pc><ph id=\"2\" dataRef=\"d3\"/></target>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testSegmentOutputWithEmptyCodesOutside () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		
		Unit unit = new Unit("id");
		Segment seg = unit.appendSegment();
		seg.getSource().append(TagType.STANDALONE, "ph1", "[br/]", false);
		seg.getSource().append(TagType.STANDALONE, "ph2", "", false);
		seg.getSource().append(TagType.STANDALONE, "ph3", null, false);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\">[br/]</data>\n"
			+ "</originalData>\n"
			+ "<segment>\n"
			+ "<source><ph id=\"ph1\" dataRef=\"d1\"/><ph id=\"ph2\"/><ph id=\"ph3\"/></source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testSegmentOutputWithoutCodes () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en", "fr");
		writer.setLineBreak("\n");
		writer.setWithOriginalData(false);
		
		Unit unit = new Unit("id");
		createSegment(unit);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\">source</pc><ph id=\"2\"/></source>\n"
			+ "<target><pc id=\"1\">target</pc><ph id=\"2\"/></target>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

//	@Test
//	public void testGlossary () {
//		XLIFFWriter writer = new XLIFFWriter();
//		StringWriter strWriter = new StringWriter();
//		writer.create(strWriter, "en");
//		writer.setLineBreak("\n");
//		writer.setUseIndentation(true);
//		
//		Unit unit = new Unit("id");
//		unit.appendNewSegment().setSource("text");
//		IGlossEntry entry = new GlossEntry("term", "trans");
//		entry.setDefinition("def");
//		unit.getGlossaryEntries().add(entry);
//		writer.writeUnit(unit);
//		
//		writer.close();
//		assertEquals("<?xml version=\"1.0\"?>\n"
//			+ STARTDOC+"srcLang=\"en\">\n"
//			+ " <file id=\"f1\">\n"
//			+ "  <unit id=\"id\">\n"
//			+ "   <segment>\n"
//			+ "    <source>text</source>\n"
//			+ "   </segment>\n"
//			+ "   <g:glossary xmlns:g=\"urn:oasis:names:tc:xliff:glossary:2.0\">\n"
//			+ "    <g:glossEntry>\n"
//			+ "     <g:term>term</g:term>\n"
//			+ "     <g:translation>trans</g:translation>\n"
//			+ "     <g:definition>def</g:definition>\n"
//			+ "    </g:glossEntry>\n"
//			+ "   </g:glossary>\n"
//			+ "  </unit>\n"
//			+ " </file>\n"
//			+ "</xliff>\n",
//			strWriter.toString());
//	}

	@Test
	public void testSegmentOutputWithoutOriginalData () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en", "fr");
		writer.setLineBreak("\n");
		writer.setWithOriginalData(false);
		
		Unit unit = new Unit("id");
		createSegment(unit);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source><pc id=\"1\">source</pc><ph id=\"2\"/></source>\n"
			+ "<target><pc id=\"1\">target</pc><ph id=\"2\"/></target>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testExtensionElements () {
		Unit unit = new Unit("id");
		Segment seg = unit.appendSegment();
		seg.setSource("Source.");
		
		// Add an extension element
		unit.setExtElements(new ExtElements())
			.add(new ExtElement(new QName("myNamespaceURI", "myElement", "x1")))
				.addChild(new ExtContent("The content of the extension element."));

		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en");
		writer.setLineBreak("\n");
		writer.writeUnit(unit);
		writer.close();
		
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<x1:myElement xmlns:x1=\"myNamespaceURI\">The content of the extension element.</x1:myElement>\n"
			+ "<segment>\n"
			+ "<source>Source.</source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}
	
	@Test
	public void testSegmentOrder () {
		Unit unit = new Unit("id");
		// Source = "Source A. Source B."
		// Target = "Target B. Target A."
		Segment seg = unit.appendSegment();
		seg.setSource("Source A.");
		seg.setTarget("Target A.");
		seg.setTargetOrder(3);
		unit.appendIgnorable().setSource(" ");
		seg = unit.appendSegment();
		seg.setSource("Source B.");
		seg.setTarget("Target B");
		seg.setTargetOrder(1);
		
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "en", "fr");
		writer.setLineBreak("\n");
		writer.writeUnit(unit);
		writer.close();
		
		assertEquals("<?xml version=\"1.0\"?>\n"
			+ STARTDOC+"srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n"
			+ "<source>Source A.</source>\n"
			+ "<target order=\"3\">Target A.</target>\n"
			+ "</segment>\n"
			+ "<ignorable>\n"
			+ "<source> </source>\n"
			+ "</ignorable>\n"
			+ "<segment>\n"
			+ "<source>Source B.</source>\n"
			+ "<target order=\"1\">Target B</target>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testInvalidCharacters () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en");

		Unit unit = new Unit("1");
		unit.appendIgnorable().getSource().append("\u0019\uFFFE");
		Fragment frag = unit.appendSegment().getSource();
		frag.append("\u0019\uFFFE");
		frag.append(TagType.STANDALONE, "1", "\u0019\uFFFE", true);
		writer.writeUnit(unit);
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\">\n"
			+ "<file id=\"f1\">\n<unit id=\"1\">\n"
			+ "<originalData>\n"
			+ "<data id=\"d1\"><cp hex=\"0019\"/><cp hex=\"FFFE\"/></data>\n"
			+ "</originalData>\n"
			+ "<ignorable>\n"
			+ "<source><cp hex=\"0019\"/><cp hex=\"FFFE\"/></source>\n"
			+ "</ignorable>\n"
			+ "<segment>\n"
			+ "<source><cp hex=\"0019\"/><cp hex=\"FFFE\"/><ph id=\"1\" dataRef=\"d1\"/></source>\n"
			+ "</segment>\n"
			+ "</unit>\n</file>\n"
			+ "</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testRewrite () {
		String input = "<?xml version='1.0'?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file translate=\"no\" id=\"fid1\" original=\"ori\">\n"
			+ "<unit id=\"id\" canResegment=\"no\">\n<segment>\n<source>Source 1.</source>\n<target>Target 1.</target>\n"
			+ "</segment>\n<segment>\n<source>Source 2.</source>\n<target>Target 2.</target>\n</segment>\n</unit>\n</file>\n</xliff>";
		XLIFFReader reader = new XLIFFReader(XLIFFReader.VALIDATION_MAXIMAL);
		XLIFFWriter writer = new XLIFFWriter();
		reader.open(input);
		StringWriter strWriter = new StringWriter();
		writer.create(strWriter, "fr");
		writer.setLineBreak("\n"); //TODO: need to detect automatically
		while ( reader.hasNext() ) {
			writer.writeEvent(reader.next());
		}
		writer.close();
		reader.close();

		String expected = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">\n"
			+ "<file id=\"fid1\" translate=\"no\" original=\"ori\">\n"
			+ "<unit id=\"id\" canResegment=\"no\">\n"
			+ "<segment>\n"
			+ "<source>Source 1.</source>\n"
			+ "<target>Target 1.</target>\n"
			+ "</segment>\n"
			+ "<segment>\n"
			+ "<source>Source 2.</source>\n"
			+ "<target>Target 2.</target>\n"
			+ "</segment>\n</unit>\n</file>\n</xliff>\n";
		assertEquals(expected, strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	@Test
	public void testCreateWriteVerify () {
		XLIFFDocument doc = new XLIFFDocument(new StartXliffData(null));
		doc.setLineBreak("\n");
		doc.getStartXliffData().setSourceLanguage("en");
		doc.getStartXliffData().setTargetLanguage("fr");
		FileNode fn = doc.addFileNode("f1");
		
		final String myNS = "http://myNamespaceURI";
		fn.getStartData().getExtAttributes().setAttribute(myNS, "attr", "value");

		MidFileData mfd = new MidFileData();
		fn.setMidData(mfd);

		ExtElement xe = mfd.getExtElements().add(myNS, "elem", "my");
		xe.addContent("Some text");
		xe.addContent(" and more data");
		xe.getExtAttributes().setAttribute(myNS, "theAttr", "the Value");
		xe.getExtAttributes().setAttribute("anotherAttr", "another Value");
		xe = xe.addElement("teste2");
		xe.addContent("Content part 2");
		xe = xe.addElement("emptyElem");

		Note note = new Note("Note for f1");
		note.setPriority(9);
		note.getExtAttributes().setAttribute(myNS, "attr", "value-in-note");
		note.getExtAttributes().setAttribute(net.sf.okapi.lib.xliff2.Const.NS_XLIFF_FS20, "fs", "b");
		mfd.addNote(note);

		mfd.getValidation().add(new Rule("isPresent", "*"));
		
		GroupNode gn = fn.addGroupNode("g1");
		gn.get().getValidation().add(new Rule("isPresent", "s"));
		gn.get().addNote(new Note("Note for g1"));
		gn.get().getExtAttributes().setAttribute(myNS, "attr1", "value1");
		gn.get().getExtAttributes().setAttribute(myNS, "attr2", "value2");
		
		// Add metadata
		MetaGroup mg = new MetaGroup("cat-mda1");
		mg.setId("mda1");
		Meta meta = new Meta("type-meta1", "value-meta1");
		mg.add(meta);
		gn.get().getMetadata().addGroup(mg);
		mg = new MetaGroup();
		mg.setId("mda2");
		mg.setAppliesTo(net.sf.okapi.lib.xliff2.metadata.MetaGroup.AppliesTo.SOURCE);
		MetaGroup mg2 = new MetaGroup();
		mg2.setId("mda2-2");
		meta = new Meta("type-meta2", "value-meta2");
		mg2.add(meta);
		meta = new Meta("type-meta3", "value-meta3");
		mg2.add(meta);
		mg.addGroup(mg2);
		gn.get().getMetadata().addGroup(mg);

		UnitNode un = gn.addUnitNode("g1u1");
		Segment seg = un.get().appendSegment();
		seg.getSource().append("source g1u1");
		seg.getTarget(GetTarget.CREATE_EMPTY).append("target g1u1");
		un.get().addNote(new Note("Note for g1u1"));
		un.get().getValidation().add(new Rule("isPresent", "r"));
		
		// Add a unit
		un = gn.addUnitNode("g1u2");
		Fragment frag = un.get().appendSegment().getSource();
		frag.append("abc ");
		CTag ctag = frag.openCodeSpan("c1", "<B>");
		frag.append("text");
		frag.closeCodeSpan("c1", "</B>");
		ctag.getExtAttributes().setAttribute(net.sf.okapi.lib.xliff2.Const.NS_XLIFF_FS20, "fs", "b");
		MTag mtag = frag.getOrCreateMarker(0, -1, null, "term");
		mtag.getExtAttributes().setAttribute(myNS, "someAttr", "someValue");
		xe = un.get().getExtElements().add("some-uri", "localName", "prf");
		xe.addContent("Some text: <>/&\", etc.");
		
		GlossEntry ge = un.get().getGlossary().add(new GlossEntry());
		Term term = new Term("my term");
		term.setSource("term source");
		term.getExtAttributes().setAttribute(myNS, "attr", "value");
		ge.setTerm(term);
		ge.addTranslation("Translation1").setId("tid1");
		ge.addTranslation("Translation2").setSource("tsource2");
		
		Matches matches = un.get().getMatches();
		Match match = matches.add(new Match());
		match.setId("mtc1");
		match.setRef("#"+mtag.getId());
		match.setSource(new Fragment(match.getStore(), false, "source match"));
		match.setTarget(new Fragment(match.getStore(), true, "target match"));
		match.setSimilarity(50.0);
		match = matches.add(new Match());
		match.setId("mtc2");
		match.setRef("#"+mtag.getId());
		frag = new Fragment(match.getStore(), false, "Match ");
		frag.openCodeSpan("c1", "<U>");
		frag.append("source 2");
		frag.closeCodeSpan("c1", "</U>");
		match.setSource(frag);
		frag = new Fragment(match.getStore(), true, "Match ");
		frag.openCodeSpan("c1", "<U>");
		frag.append("target 2");
		frag.closeCodeSpan("c1", "</U>");
		match.setTarget(frag);
		match.setSimilarity(34.567);
		match.setMatchQuality(12.34);
		match.setOrigin("match 2 origin");

		// First output
		StringWriter sw = new StringWriter();
		doc.save(sw);
		String output1 = sw.toString();
		
		doc.load(output1, XLIFFReader.VALIDATION_MAXIMAL);
		un = doc.getUnitNode("f1", "g1u1");
		assertEquals("target g1u1", un.get().getSegment(0).getTarget().toString());

		sw = new StringWriter();
		doc.save(sw);
		String output2 = sw.toString();
		assertEquals(output1, output2);
	}
	
	@Test
	public void testExtendedChars () {
		XLIFFWriter writer = new XLIFFWriter();
		StringWriter strWriter = new StringWriter();
		writer.setLineBreak("\n");
		writer.setWithOriginalData(true);
		writer.create(strWriter, "en", "ja");

		writer.writeStartDocument(null, null);
		Unit unit = new Unit("id");
		unit.setName("\uFF21, \uFF80, \u1EE8, \u0950");
		Segment seg = unit.appendSegment();
		seg.getSource().append("\uFF21, \uFF80, \u1EE8, \u0950");
		seg.getTarget(GetTarget.CREATE_EMPTY).append("\uFF21, \uFF80, \u1EE8, \u0950");
		writer.writeUnit(unit);
		writer.writeEndDocument();
		
		writer.close();
		assertEquals("<?xml version=\"1.0\"?>\n<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"ja\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\" name=\"\uFF21, \uFF80, \u1EE8, \u0950\">\n" 
			+ "<segment>\n" 
			+ "<source>\uFF21, \uFF80, \u1EE8, \u0950</source>\n" 
			+ "<target>\uFF21, \uFF80, \u1EE8, \u0950</target>\n" 
			+ "</segment>\n" 
			+ "</unit>\n" 
			+ "</file>\n</xliff>\n",
			strWriter.toString());
		XLIFFReader.validate(strWriter.toString(), null);
	}

	private void createSegment (Unit unit) {
		Segment seg = unit.appendSegment();
		seg.getSource().append(TagType.OPENING, "1", "<b>", false);
		seg.getSource().append("source");
		seg.getSource().append(TagType.CLOSING, "1", "</b>", false);
		seg.getSource().append(TagType.STANDALONE, "2", "<![CDATA[..<c/>..]]>", false);
		Fragment frag = seg.getTarget(GetTarget.CREATE_EMPTY);
		frag.append(TagType.OPENING, "1", "<b>", false);
		frag.append("target");
		frag.append(TagType.CLOSING, "1", "</B>", false);
		frag.append(TagType.STANDALONE, "2", "<![CDATA[..<c/>..]]>", false);
	}
	
//	private Candidate createAlternate (String prefix, float value) {
//		Candidate alt = new Candidate();
//		alt.setSimilarity(value);
//		alt.setType("tm");
//		alt.setOrigin("ori");
//		Fragment frag = new Fragment(alt.getDataStore());
//		frag.append(prefix+"-text");
//		frag.appendPlaceholder("1", "<br/>");
//		alt.setSource(frag);
//		
//		frag = new Fragment(alt.getDataStore());
//		frag.append(prefix.toUpperCase()+"-TEXT");
//		frag.appendPlaceholder("1", "<br/>");
//		alt.setTarget(frag);
//		
//		return alt;
//	}
	
}
