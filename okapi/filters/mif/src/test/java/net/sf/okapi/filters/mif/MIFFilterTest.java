/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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

package net.sf.okapi.filters.mif;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.TestUtil;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(DataProviderRunner.class)
public class MIFFilterTest {

	private final static String STARTMIF = "<MIFFile 9.00><TextFlow <Para ";
	private final static String ENDMIF = "> # End of Para\n >\n # End of TextFlow\n# End of MIFFile\n";

	private final Common common;
	private final FileLocation root;
	private final LocaleId locEN;
	private final GenericContent fmt;
	private MIFFilter filter;

	public MIFFilterTest() {
		this.common = new Common(new Parameters());
		this.root = FileLocation.fromClass(getClass());
		this.locEN = LocaleId.fromString("en");
		this.fmt = new GenericContent();
	}

	@Before
	public void setUp() {
		filter = new MIFFilter();
	}

	@Test
	public void testDefaultInfo () {
		//Not using parameters yet: assertNotNull(filter.getParameters());
		assertNotNull(filter.getName());
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertTrue(list.size()>0);
	}

	@Test
	public void testStartDocument () {
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(filter,
			new InputDocument(root.in("/Test01.mif").toString(), null),
			null, locEN, locEN));
	}

	@Test
	public void testSimpleText () {
		final List<Event> events = getEventsFromFile("/Test01.mif", null);
		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(209, textUnits.size());
		assertEquals(
			"Line 1\nLine 2",
			fmt.setContent(textUnits.get(197).getSource().getFirstContent()).toString()
		);
		assertEquals(
			"\u00e0=agrave",
			fmt.setContent(textUnits.get(198).getSource().getFirstContent()).toString()
		);
	}

	@Test
	public void testExtractIndexMarkers () {
		Parameters params = this.common.parameters();
		
		// Extract index markers
		List<Event> list = getEventsFromFile("/TestMarkers.mif", params);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("Text of marker", fmt.setContent(tu.getSource().getFirstContent()).toString());
		assertEquals("x-index", tu.getType());
		
		// Do not extract index markers
		params.setExtractIndexMarkers(false);
		list = getEventsFromFile("/TestMarkers.mif", params);
		tu = FilterTestDriver.getTextUnit(list, 1);
		assertNotNull(tu);
		assertEquals("Text with index about some subject.", fmt.setContent(tu.getSource().getFirstContent()).toString());
	}

	@Test
	public void testExtractLinks () {
		Parameters params = this.common.parameters();

		// Do not extract links
		List<Event> list = getEventsFromFile("/TestMarkers.mif", params);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 5);
		assertNotNull(tu);
		assertEquals("text with a link to <1/>http://okapi.opentag.org/", fmt.setContent(tu.getSource().getFirstContent()).toString());
		
		// Do extract links
		params.setExtractLinks(true);
		list = getEventsFromFile("/TestMarkers.mif", params);
		tu = FilterTestDriver.getTextUnit(list, 5);
		assertNotNull(tu);
		assertEquals("http://okapi.opentag.com/", fmt.setContent(tu.getSource().getFirstContent()).toString());
		assertEquals("link", tu.getType());
	}

	@Test
	public void testBodyOnlyNoVariables () {
		Parameters params = this.common.parameters();
		final List<Event> events = getEventsFromFile("/Test01.mif", params);
		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals("Line 1\nLine 2", fmt.setContent(textUnits.get(0).getSource().getFirstContent()).toString());
		assertEquals("\u00e0=agrave", fmt.setContent(textUnits.get(1).getSource().getFirstContent()).toString());
	}

	@Test
	public void testParagraphLinesProcessing() {
		Parameters params = this.common.parameters();

		List<Event> list = getEventsFromFile("/TestParaLines.mif", params);
		ITextUnit tu = FilterTestDriver.getTextUnit(list, 1);

		assertNotNull(tu);
		assertEquals("The 1st para line. The 2nd.", fmt.setContent(tu.getSource().getFirstContent()).toString());
	}

	@Test
	public void testSimpleEntry () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <String `text \\\\ and &'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("text \\ and &", fmt.setContent(tu.getSource().getFirstContent()).toString());
		
		String expected = STARTMIF
			+ "<Unique 12345><ParaLine <String `text \\\\ and &'>> # end of ParaLine\n"
			+ ENDMIF;
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(snippet),
			filter.getEncoderManager(), locEN));
	}
	
	@Test
	public void testNoTextEntry () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <TextRectID 9> >"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertTrue(tu==null);
		String expected = STARTMIF
			+ "<Unique 12345><ParaLine <TextRectID 9> > # end of ParaLine\n"
			+ ENDMIF;
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(snippet),
			filter.getEncoderManager(), locEN));
	}

	@Test
	public void testTwoPartsEntry () {
		String snippet = STARTMIF
			+ "<Unique 12345>#EOU\n<ParaLine \n <String `Part 1'>#EOS\n>#EOPL\n<ParaLine \n <String ` and part 2'>#EOS\n>#EOPL\n"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Part 1 and part 2", fmt.setContent(tu.getSource().getFirstContent()).toString());
		
		String expected = STARTMIF
			+ "<Unique 12345>#EOU\n<ParaLine \n <String `Part 1 and part 2'>#EOS\n#EOPL\n> # end of ParaLine\n"
			+ ENDMIF;
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(snippet),
			filter.getEncoderManager(), locEN));
	}

	@Test
	public void testEmptyString () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <String `Text 1'><AFrame 1><Char ThinSpace><String `'><AFrame 2><String ` end'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Text 1<1/>\u2009<2/> end", fmt.setContent(tu.getSource().getFirstContent()).toString());
		Code code = tu.getSource().getFirstContent().getCode(0);
		assertEquals("'><AFrame 1><String `", code.getData());
		code = tu.getSource().getFirstContent().getCode(1);
		assertEquals("'><String `'><AFrame 2><String `", code.getData());
	}
	
	@Test
	public void testEmptyStringInFront () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <String `'><Font 1><String `Text'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Text", fmt.setContent(tu.getSource().getFirstContent()).toString());
	}
	
	@Test
	public void testTrimFontInFront () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <Font 1><String `Text'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Text", fmt.setContent(tu.getSource().getFirstContent()).toString());
		
		String expected = STARTMIF
			+ "<Unique 12345><ParaLine <Font 1><String `Text'>> # end of ParaLine\n"
			+ ENDMIF;
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(snippet),
			filter.getEncoderManager(), locEN));
	}
	
	@Test
	public void testTabs () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <String ` '><Var 1><Char Tab><Char Tab>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertTrue(tu==null); // No text to extract

		String expected = STARTMIF
			+ "<Unique 12345><ParaLine <String ` '><Var 1><String `\\t\\t'>> # end of ParaLine\n"
			+ ENDMIF;
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(snippet),
			filter.getEncoderManager(), locEN));
	}

	@Test
	public void testTabsAndCodes () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <Char Tab><Font 1><Var 1><Font 2><Char Tab>><ParaLine <Font 3>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertTrue(tu==null);
		DocumentPart dp = FilterTestDriver.getDocumentPart(getEvents(snippet), 2);
		assertEquals("<TextFlow <Para <Unique 12345><ParaLine <String `\\t'><Font 1><Var 1><Font 2><String `\\t'><Font 3>> # end of ParaLine\n>", dp.getSkeleton().toString());
	}

	@Test
	public void testDummyBeforeChar () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <String `Text 1'><Dummy <InDummy 2>><Char ThinSpace><String `Text 2'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Text 1<1/>\u2009Text 2", fmt.setContent(tu.getSource().getFirstContent()).toString());
		Code code = tu.getSource().getFirstContent().getCode(0);
		assertEquals("'><Dummy <InDummy 2>><String `", code.getData());
	}

	@Test
	public void testCodeAtTheFront () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <Font 1><String `text 1'><Font 2><String `text 2'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("text 1<2/>text 2", fmt.setContent(tu.getSource().getFirstContent()).toString());
		assertEquals("'><Font 2><String `", tu.getSource().getFirstContent().getCode(0).getData());
	}

	@Test
	public void testCharOnly () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <Dummy 1><Char Tab><Dummy 2>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertTrue(tu==null);
	}

	@Test
	public void testEndsInCharAndCode () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <String `aaa'><Dummy 1><Char Tab><Dummy 2>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("aaa<1/>\t", fmt.setContent(tu.getSource().getFirstContent()).toString());
	}

	@Test
	public void testDummyCharString () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <AFrame 1><Char Tab><String `aaa'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("aaa", fmt.setContent(tu.getSource().getFirstContent()).toString());
		assertEquals("<TextFlow <Para <Unique 12345><ParaLine <AFrame 1><String `\t[#$$self$]'>> # end of ParaLine\n>", tu.getSkeleton().toString());
	}

	@Test
	public void testEmptyFTag () {
		String snippet = STARTMIF
			+ "<Unique 12345><ParaLine <AFrame 1><String `Text 1'><Char ThinSpace><AFrame 2><String `text 2'><AFrame 3>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Text 1\u2009<2/>text 2", fmt.setContent(tu.getSource().getFirstContent()).toString());
		assertEquals("'><AFrame 2><String `", tu.getSource().getFirstContent().getCode(0).getData());
	}

	@Test
	public void testSoftHyphen () {
		String snippet = STARTMIF
			+ "<Unique 123><ParaLine <TextRectID 20><String `How'><Char SoftHyphen>>"
			+ "<ParaLine <String `ever.'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("However.", fmt.setContent(tu.getSource().getFirstContent()).toString());
	}

	@Test
	public void testNormalFont () {
		String snippet = STARTMIF
			+ "<Unique 123><ParaLine <Font <FTag `'><FLanguage NoLanguage><FLocked No>> # end of Font\n<String `Text'>>"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Text", fmt.setContent(tu.getSource().getFirstContent()).toString());
	}
	
	@Test
	public void testEmptyParaLine () {
		String snippet = STARTMIF
			+ "<Unique 123>\n <ParaLine > # end of ParaLine\n"
			+ ENDMIF;
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertTrue(tu==null);
		DocumentPart dp = FilterTestDriver.getDocumentPart(getEvents(snippet), 2);
		assertEquals("<TextFlow <Para <Unique 123>\n <ParaLine > # end of ParaLine\n>", dp.getSkeleton().toString());
	}

	@Test
	public void testSlashCodes () {
		String snippet = "<MIFFile 10.0> # Generated by FrameMaker 10.0.0.388\n"
			+ "<VariableFormats\n" 
			+ "<VariableFormat\n" 
			+ "<VariableName `Running H/F 4'>\n"
			+ "<VariableDef `<zBold\\><$paranum[LBN.LabNumber]\\><Default Z Font\\>\\x14 \\x05 \\x0b <$paratext[LBT.LabTitle]\\>'>\n"
			+ "> # end of VariableFormat\n"
			+ "> # end of VariableFormats\n";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertTrue(tu!=null);
		assertEquals("<zBold><1/><Default Z Font> \u200D",
			fmt.setContent(tu.getSource().getFirstContent()).toString());
	}

	@Test
	public void testSlashCodesOutput () {
		String snippet = "<MIFFile 9.00> # Generated by FrameMaker 10.0.0.388\n"
			+ "<VariableFormats\n" 
			+ "<VariableFormat\n" 
			+ "<VariableName `Running H/F 4'>\n"
			+ "<VariableDef `<zBold\\><$paranum[LBN.LabNumber]\\><Default Z Font\\>\\x14 \\x05 \\x0b <$paratext[LBT.LabTitle]\\>'>\n"
			+ "> # end of VariableFormat\n"
			+ "> # end of VariableFormats\n";
		String expected = "<MIFFile 9.00> # Generated by FrameMaker 10.0.0.388\n"
			+ "<VariableFormats\n" 
			+ "<VariableFormat\n" 
			+ "<VariableName `Running H/F 4'>\n"
			+ "<VariableDef `<zBold\\><$paranum[LBN.LabNumber]\\><Default Z Font\\> \u200D\\x0b <$paratext[LBT.LabTitle]\\>'>\n"
			+ "> # end of VariableFormat\n"
			+ "> # end of VariableFormats\n";
		assertEquals(expected, FilterTestDriver.generateOutput(getEvents(snippet),
			filter.getEncoderManager(), locEN));
	}

	@Test
	public void testV10IsUsingV9Encoding () {
		List<Event> eventsV9 = getEventsFromFile("/TestEncoding-v9.mif", null);
		List<Event> eventsV10 = getEventsFromFile("/TestEncoding-v10.mif", null);

		assertTrue("Content of both files should be the same.", eventsV9.size() == eventsV10.size());
		
		for (int i = 0; i < eventsV9.size(); i++) {
			Event eventV9 = eventsV9.get(i);
			Event eventV10 = eventsV10.get(i);
			if ( eventV9.getEventType() != eventV10.getEventType() ) {
				// Trigger assert and allow easy debug
				assertTrue("Content of both files should be the same.", false);
			}
			if ( eventV9.getEventType() == EventType.TEXT_UNIT ) {
				ITextUnit tu1 = eventV9.getTextUnit();
				ITextUnit tu2 = eventV10.getTextUnit();
				assertEquals(tu1.getSource().getFirstContent().getText(),
					tu2.getSource().getFirstContent().getText());
			}
		}
	}

    @Test
    public void processesSupportedVersions() {
		String snippet = "<MIFFile 8.00>";
		List<Event> events = getEvents(snippet);
		assertEquals(3, events.size());

        snippet = "<MIFFile 2015>";
        events = getEvents(snippet);
        assertEquals(3, events.size());
    }

	@Test
	public void doesNotProcessUnsupportedVersions() {
		String snippet = "<MIFFile 7.00>";
		try {
			getEvents(snippet);
		} catch (OkapiBadFilterInputException expected) {
			assertEquals("Unsupported document version: 7.00", expected.getMessage());
		}
	}

	@Test
	public void extractsBodyPageRelatedInformationOnly() {
		final List<Event> events = getEventsFromFile("/893.mif", this.common.parameters());
		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(1, textUnits.size());
		assertEquals("Goes over the PgfCatalog.", textUnits.get(0).getSource().getCodedText());
	}

	@Test
	public void extractsMultipleTextFramesPerPage() {
		final List<Event> events = getEventsFromFile("/895.mif", this.common.parameters());
		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(7, textUnits.size());
		assertEquals("LOGO", textUnits.get(0).getSource().getCodedText());
		assertEquals("A structured letter.", textUnits.get(1).getSource().getCodedText());
		assertEquals("Company Name", textUnits.get(2).getSource().getCodedText());
		assertEquals("1000 Main Street ", textUnits.get(3).getSource().getCodedText());
		assertEquals("City, State 99999", textUnits.get(4).getSource().getCodedText());
		assertEquals("444.555.1212", textUnits.get(5).getSource().getCodedText());
		assertEquals("Fax 444.555.2222", textUnits.get(6).getSource().getCodedText());
	}

	@Test
	public void extractsNumberedParagraphFormats() {
		final Parameters parameters = this.common.parameters();
		List<Event> events = getEventsFromFile("/896.mif", parameters);
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(8, textUnits.size());
		assertEquals(".", textUnits.get(0).getSource().getCodedText());
		assertEquals("Prepending autonumber:", textUnits.get(1).getSource().getCodedText());
		assertEquals("Paragraph 1.", textUnits.get(2).getSource().getCodedText());
		assertEquals("Prepending autonumber:", textUnits.get(3).getSource().getCodedText());
		assertEquals("Paragraph 2.", textUnits.get(4).getSource().getCodedText());
		assertEquals("another autonumber:", textUnits.get(5).getSource().getCodedText());
		assertEquals("Paragraph 3.", textUnits.get(6).getSource().getCodedText());
		assertEquals("Paragraph 4.", textUnits.get(7).getSource().getCodedText());

		events = getEventsFromFile("/896-changed.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(8, textUnits.size());
		assertEquals("CHANGED Numbered: \uE103\uE110.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Prepending autonumber:", textUnits.get(1).getSource().getCodedText());
		assertEquals("Paragraph 1.", textUnits.get(2).getSource().getCodedText());
		assertEquals("Prepending autonumber:", textUnits.get(3).getSource().getCodedText());
		assertEquals("Paragraph 2.", textUnits.get(4).getSource().getCodedText());
		assertEquals("CHANGED autonumber: ", textUnits.get(5).getSource().getCodedText());
		assertEquals("Paragraph 3.", textUnits.get(6).getSource().getCodedText());
		assertEquals("Paragraph 4.", textUnits.get(7).getSource().getCodedText());

		events = getEventsFromFile("/896-autonumber-building-blocks.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(3, textUnits.size());
		assertEquals("Paragraph 1.", textUnits.get(2).getSource().getCodedText());

		parameters.setExtractPgfNumFormatsInline(true);

		events = getEventsFromFile("/896.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(5, textUnits.size());
		assertEquals(".", textUnits.get(0).getSource().getCodedText());
		assertEquals("Prepending autonumber:\uE103\uE110Paragraph 1.", textUnits.get(1).getSource().getCodedText());
		assertEquals("Prepending autonumber:\uE103\uE110Paragraph 2.", textUnits.get(2).getSource().getCodedText());
		assertEquals("another autonumber: \uE103\uE110Paragraph 3.", textUnits.get(3).getSource().getCodedText());
		assertEquals("Paragraph 4.", textUnits.get(4).getSource().getCodedText());

		events = getEventsFromFile("/896-changed.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(5, textUnits.size());
		assertEquals("CHANGED Numbered: \uE103\uE110.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Prepending autonumber:\uE103\uE110Paragraph 1.", textUnits.get(1).getSource().getCodedText());
		assertEquals("Prepending autonumber:\uE103\uE110Paragraph 2.", textUnits.get(2).getSource().getCodedText());
		assertEquals("CHANGED autonumber: \uE103\uE110Paragraph 3.", textUnits.get(3).getSource().getCodedText());
		assertEquals("Paragraph 4.", textUnits.get(4).getSource().getCodedText());

		events = getEventsFromFile("/896-autonumber-building-blocks.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(3, textUnits.size());
		assertEquals("Paragraph 1.", textUnits.get(2).getSource().getCodedText());
	}

	@Test
	public void extractsNumberedParagraphFormatInTableCells() {
		final Parameters parameters = this.common.parameters();
		final List<Event> events = getEventsFromFile("/904.mif", parameters);
		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(17, textUnits.size());
		assertEquals(".", textUnits.get(0).getSource().getCodedText());
		assertEquals("Table \uE103\uE110:", textUnits.get(1).getSource().getCodedText());
		assertEquals("3x3 table", textUnits.get(2).getSource().getCodedText());
		assertEquals("Custom format:", textUnits.get(3).getSource().getCodedText());
		assertEquals("another title", textUnits.get(4).getSource().getCodedText());
		assertEquals("Col heading 0", textUnits.get(5).getSource().getCodedText());
		assertEquals("Custom: ", textUnits.get(6).getSource().getCodedText());
		assertEquals("Col. heading 1", textUnits.get(7).getSource().getCodedText());
		assertEquals("Col. heading 2", textUnits.get(8).getSource().getCodedText());
		assertEquals("c00", textUnits.get(9).getSource().getCodedText());
		assertEquals("c01", textUnits.get(10).getSource().getCodedText());
		assertEquals("c02", textUnits.get(11).getSource().getCodedText());
		assertEquals("c11", textUnits.get(12).getSource().getCodedText());
		assertEquals("Custom:", textUnits.get(13).getSource().getCodedText());
		assertEquals("c20", textUnits.get(14).getSource().getCodedText());
		assertEquals("c22", textUnits.get(15).getSource().getCodedText());
		assertEquals("Paragraph 1.", textUnits.get(16).getSource().getCodedText());
	}

	@Test
	public void extractsAnchoredFramesContent() {
		final Parameters parameters = this.common.parameters();
		List<Event> events = getEventsFromFile("/902-1.mif", parameters);
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(2, textUnits.size());
		assertEquals("And a text frame.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Paragraph 1.", textUnits.get(1).getSource().getCodedText());

		events = getEventsFromFile("/902-2.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(1, textUnits.size());
		assertEquals("Paragraph 1.", textUnits.get(0).getSource().getCodedText());

		events = getEventsFromFile("/902-3.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(8, textUnits.size());
		assertEquals("At top of column.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Text line 2.", textUnits.get(1).getSource().getCodedText());
		assertEquals("Text frame 1.", textUnits.get(2).getSource().getCodedText());
		assertEquals("Paragraph 1.", textUnits.get(3).getSource().getCodedText());
		assertEquals("Paragraph\uE103\uE110 2.", textUnits.get(4).getSource().getCodedText());
		assertEquals("Paragraph 3.", textUnits.get(5).getSource().getCodedText());
		assertEquals("Text frame at top of col.", textUnits.get(6).getSource().getCodedText());
		assertEquals("Text frame 2.", textUnits.get(7).getSource().getCodedText());
	}

	@Test
	public void extractsNestedAnchoredFrames() {
		final Parameters parameters = this.common.parameters();
		List<Event> events = getEventsFromFile("/909-1.mif", parameters);
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(14, textUnits.size());
		assertEquals("Text line 1.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Text line 2.", textUnits.get(1).getSource().getCodedText());
		assertEquals("Text line 3.", textUnits.get(2).getSource().getCodedText());
		assertEquals("Text line 4.", textUnits.get(3).getSource().getCodedText());
		assertEquals("Text line 5.", textUnits.get(4).getSource().getCodedText());
		assertEquals("Text line 6.", textUnits.get(5).getSource().getCodedText());
		assertEquals("Text line 0.", textUnits.get(6).getSource().getCodedText());
		assertEquals("Paragraph 0.", textUnits.get(7).getSource().getCodedText());
		assertEquals("In anchored frame 1.", textUnits.get(8).getSource().getCodedText());
		assertEquals("In anchored frame 2.", textUnits.get(9).getSource().getCodedText());
		assertEquals("In anchored frame 3.", textUnits.get(10).getSource().getCodedText());
		assertEquals("In anchored frame 4.", textUnits.get(11).getSource().getCodedText());
		assertEquals("In anchored frame 5.", textUnits.get(12).getSource().getCodedText());
		assertEquals("In anchored frame 6.", textUnits.get(13).getSource().getCodedText());

		events = getEventsFromFile("/909-2.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(5, textUnits.size());
		assertEquals("Paragraph 0.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Run into paragraph 1.", textUnits.get(1).getSource().getCodedText());
		assertEquals("Below current line 1.", textUnits.get(2).getSource().getCodedText());
		assertEquals("Below current line 11.", textUnits.get(3).getSource().getCodedText());
		assertEquals("Below current line 12.", textUnits.get(4).getSource().getCodedText());

		events = getEventsFromFile("/909-3.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(4, textUnits.size());
		assertEquals("Table \uE103\uE110:", textUnits.get(0).getSource().getCodedText());
		assertEquals("C02", textUnits.get(1).getSource().getCodedText());
		assertEquals("Paragraph 1.", textUnits.get(2).getSource().getCodedText());
		assertEquals("A frame in a table cell.", textUnits.get(3).getSource().getCodedText());
	}

	@Test
	public void sequentialParagraphFormatsExtracted() {
		List<Event> events = getEventsFromFile("/940.mif", this.common.parameters());
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(4, textUnits.size());
		assertEquals("Numbered \uE103\uE110.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Para 1", textUnits.get(1).getSource().getCodedText());
		assertEquals("Numbered1 \uE103\uE110.", textUnits.get(2).getSource().getCodedText());
		assertEquals("Para 2 with another style", textUnits.get(3).getSource().getCodedText());
	}

	@Test
	public void referenceFormatsExtracted() {
		final Parameters parameters = this.common.parameters();
		parameters.setExtractReferenceFormats(true);
		List<Event> events = getEventsFromFile("/938-1.mif", parameters);
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(7, textUnits.size());
		assertEquals("“\uE103\uE110” on page ", textUnits.get(0).getSource().getCodedText());
		assertEquals("“Para 1.” on page 1", textUnits.get(1).getSource().getCodedText());

		events = getEventsFromFile("/938-2.mif", parameters);
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(7, textUnits.size());
		assertEquals(
			"Refer to“\uE103\uE110” \uE103\uE111 page \uE103\uE112-",
			textUnits.get(0).getSource().getCodedText()
		);
	}

	@Test
	public void textLinesExtracted() {
		List<Event> events = getEventsFromFile("/942-1.mif", this.common.parameters());
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(5, textUnits.size());
		assertEquals("A text line on an anchored frame.", textUnits.get(0).getSource().getCodedText());
		assertEquals("A text line on an inner anchored frame without para.", textUnits.get(1).getSource().getCodedText());
		assertEquals("A text line on a page frame.", textUnits.get(2).getSource().getCodedText());
		assertEquals("Para 1.", textUnits.get(3).getSource().getCodedText());
		assertEquals("Para 2.", textUnits.get(4).getSource().getCodedText());

		events = getEventsFromFile("/942-2.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(12, textUnits.size());
		assertEquals("TextLine on anchored > graphic frame", textUnits.get(0).getSource().getCodedText());
		assertEquals("TextLine on anchored > textual > graphic frame", textUnits.get(1).getSource().getCodedText());
		assertEquals("TextLine on anchored frame", textUnits.get(2).getSource().getCodedText());
		assertEquals("TextLine on anchored > textual > anchored frame", textUnits.get(3).getSource().getCodedText());
		assertEquals("Text line 1.0.", textUnits.get(4).getSource().getCodedText());
		assertEquals("TextLine 4.0.", textUnits.get(5).getSource().getCodedText());
		assertEquals("TextLine 3.0.", textUnits.get(6).getSource().getCodedText());
		assertEquals("TextLine 3.1.", textUnits.get(7).getSource().getCodedText());
		assertEquals("TextLine 2.0.", textUnits.get(8).getSource().getCodedText());
		assertEquals("TextLine 2.1.", textUnits.get(9).getSource().getCodedText());
		assertEquals("Para 1.", textUnits.get(10).getSource().getCodedText());
		assertEquals("Para 2.", textUnits.get(11).getSource().getCodedText());
	}

	@Test
	public void nestedTextFramesExtracted() {
		List<Event> events = getEventsFromFile("/943.mif", this.common.parameters());
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(5, textUnits.size());
		assertEquals("Para 0.", textUnits.get(0).getSource().getCodedText());
		assertEquals("Anchored > Graphic > Graphic > Text frame", textUnits.get(1).getSource().getCodedText());
		assertEquals("Page > Graphic > Text frame", textUnits.get(2).getSource().getCodedText());
		assertEquals("Page > Graphic > Graphic > Text frame", textUnits.get(3).getSource().getCodedText());
		assertEquals("Anchored > Graphic > Text frame", textUnits.get(4).getSource().getCodedText());
	}

	@Test
	public void hardReturnsFormNewTransUnits() {
		this.common.parameters().setExtractHardReturnsAsText(false);
		List<Event> events = getEventsFromFile("/987.mif", this.common.parameters());
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(3, textUnits.size());
		assertEquals("Para 1.", textUnits.get(0).getSource().getCodedText());
		assertEquals("2.1 and", textUnits.get(1).getSource().getCodedText());
		assertEquals("2.2", textUnits.get(2).getSource().getCodedText());

		events = getEventsFromFile("/990-marker.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(9, textUnits.size());
		assertEquals("Text of ", textUnits.get(0).getSource().getCodedText());
		assertEquals("marker", textUnits.get(1).getSource().getCodedText());

		events = getEventsFromFile("/990-pgf-num-format-1.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(11, textUnits.size());
		assertEquals("CHANGED ", textUnits.get(0).getSource().getCodedText());
		assertEquals("Numbered: \uE103\uE110.", textUnits.get(1).getSource().getCodedText());
		assertEquals("Prepending ", textUnits.get(2).getSource().getCodedText());
		assertEquals("autonumber:", textUnits.get(3).getSource().getCodedText());
		assertEquals("Paragraph 1.", textUnits.get(4).getSource().getCodedText());

		events = getEventsFromFile("/990-pgf-num-format-2.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(6, textUnits.size());
		assertEquals("Numbered ", textUnits.get(0).getSource().getCodedText());
		assertEquals(".", textUnits.get(1).getSource().getCodedText());
		assertEquals("Para 1", textUnits.get(2).getSource().getCodedText());

		this.common.parameters().setExtractReferenceFormats(true);
		events = getEventsFromFile("/990-ref-format-1.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(9, textUnits.size());
		assertEquals("“\uE103\uE110” ", textUnits.get(0).getSource().getCodedText());
		assertEquals("on page \uE103\uE110.", textUnits.get(1).getSource().getCodedText());
		assertEquals("“Para 1.”", textUnits.get(2).getSource().getCodedText());
		assertEquals("on page 1", textUnits.get(3).getSource().getCodedText());

		events = getEventsFromFile("/990-ref-format-2.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(12, textUnits.size());
		assertEquals("Refer to", textUnits.get(0).getSource().getCodedText());
		assertEquals("“\uE103\uE110” \uE103\uE111 page \uE103\uE112-", textUnits.get(1).getSource().getCodedText());

		events = getEventsFromFile("/990-text-line.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(6, textUnits.size());
		assertEquals("A text line on ", textUnits.get(2).getSource().getCodedText());
		assertEquals("a page frame.", textUnits.get(3).getSource().getCodedText());
	}

	@Test
	public void consequentialStringsInXRefsExtractedWithHardReturnsBetweenThem() {
		this.common.parameters().setExtractHardReturnsAsText(false);
		List<Event> events = getEventsFromFile("/991.mif", this.common.parameters());
		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(10, textUnits.size());
		assertEquals("Refer to“", textUnits.get(1).getSource().getCodedText());
		assertEquals("Para 2.", textUnits.get(2).getSource().getCodedText());
		assertEquals("” \uE103\uE110 page -1 : 1 \uE103\uE111 afer {Emphasis} \uE103\uE112 after EquationVariables", textUnits.get(3).getSource().getCodedText());
		assertEquals("Refer to", textUnits.get(4).getSource().getCodedText());
		assertEquals("“Refer to“Para 2.” \uE103\uE110 page -1 : 1 \uE103\uE111 afer {Emphasis} \uE103\uE112 after EquationVariables\uE103\uE113” \uE103\uE114 page -1 : 1 \uE103\uE115 afer ", textUnits.get(5).getSource().getCodedText());
		assertEquals("{Emphasis} \uE103\uE110 after EquationVariables", textUnits.get(6).getSource().getCodedText());

		this.common.parameters().setExtractReferenceFormats(true);
		events = getEventsFromFile("/991.mif", this.common.parameters());
		textUnits = FilterTestDriver.filterTextUnits(events);
		assertEquals(12, textUnits.size());
	}

	@DataProvider
	public static Object[][] hardReturnsAsNonTextualProvider() {
		return new Object[][]{
			{"987.mif"},
			{"990-marker.mif"},
			{"990-pgf-num-format-1.mif"},
			{"990-pgf-num-format-2.mif"},
			{"990-ref-format-1.mif"},
			{"990-ref-format-2.mif"},
			{"990-text-line.mif"},
			{"991.mif"},
		};
	}

	@Test
	@UseDataProvider("hardReturnsAsNonTextualProvider")
	public void hardReturnsAsNonTextualRoundTripped(final String documentName) {
		final List<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/" + documentName).toString(), "okf_mif@non-textual-hard-returns.fprm"));

		final RoundTripComparison rtc = new RoundTripComparison(true); // Do compare skeletons
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN, "output"));
	}

	@DataProvider
	public static Object[][] roundTripsWithDifferentParametersProvider() {
		return new Object[][] {
			{"893.mif"},
			{"895.mif"},
			{"896.mif"},
			{"896-changed.mif"},
			{"896-autonumber-building-blocks.mif"},
			{"902-1.mif"},
			{"902-2.mif"},
			{"902-3.mif"},
			{"904.mif"},
			{"909-1.mif"},
			{"909-2.mif"},
			{"909-3.mif"},
			{"942-1.mif"},
			{"942-2.mif"},
			{"945.mif"},
			{"987.mif"},
			{"991.mif"},

			{"ImportedText.mif"},
			{"JATest.mif"},
			{"Test01.mif"},
			{"Test01-v8.mif"},
			{"Test02-v9.mif"},
			{"Test03.mif"},
			{"Test04.mif"},
			{"TestEncoding-v9.mif"},
			{"TestEncoding-v10.mif"},
			{"TestFootnote.mif"},
			{"TestMarkers.mif"},
			{"TestParaLines.mif"},
		};
	}

	@Test
	@UseDataProvider("roundTripsWithDifferentParametersProvider")
	public void roundTripsWithDifferentParameters(final String documentName) {
		final List<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/" + documentName).toString(), null));
		list.add(new InputDocument(root.in("/" + documentName).toString(), "okf_mif@common.fprm"));
		list.add(new InputDocument(root.in("/" + documentName).toString(), "okf_mif@inline-pgf-num-formats.fprm"));

		final RoundTripComparison rtc = new RoundTripComparison(false); // Do not compare skeleton
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN, "output"));
	}

	private List<Event> getEventsFromFile(String filename, Parameters params) {
		return FilterTestDriver.getEvents(filter,  new RawDocument(root.in(filename).asUri(), null, locEN), params);
	}

	private ArrayList<Event> getEvents(String snippet) {
		return FilterTestDriver.getEvents(filter, snippet, locEN);
	}
}
