package net.sf.okapi.filters.openxml;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.resource.ITextUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isParagraphStartEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(DataProviderRunner.class)
public class TestBlockParser {
	private XMLFactories factories = new XMLFactoriesForTest();
	private ConditionalParameters defaultParams = new ConditionalParameters();
	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testTab() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-tab.xml"), defaultParams);
		assertEquals(3, tus.size());
		assertEquals("<tags1/>Text after tab.", tus.get(0).getSource().toString());
		assertEquals("Text before tab.<tags1/>", tus.get(1).getSource().toString());
		assertEquals("Text before<tags1/>and after tab.", tus.get(2).getSource().toString());
	}

	@Test
	public void testTabAsChar() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setAddTabAsCharacter(true);
		List<ITextUnit> tus = parseTextUnits(getReader("document-tab.xml"), params);
		assertEquals(4, tus.size());
		assertEquals("\t", tus.get(0).getSource().toString());
		assertEquals("\tText after tab.", tus.get(1).getSource().toString());
		assertEquals("Text before tab.\t", tus.get(2).getSource().toString());
		assertEquals("Text before\tand after tab.", tus.get(3).getSource().toString());
	}

	@Test
	public void testFieldAndTab() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-field-and-tab.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals("Author:<tags1/><tags2/>", tus.get(0).getSource().toString());
	}

	@Test
	public void testFieldAndTabAsChar() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setAddTabAsCharacter(true);
		List<ITextUnit> tus = parseTextUnits(getReader("document-field-and-tab.xml"), params);
		assertEquals(1, tus.size());
		assertEquals("Author:\t<tags1/>", tus.get(0).getSource().toString());
	}

	@Test
	public void testEmptyBlock() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-empty.xml"), defaultParams);
		assertEquals(0, tus.size());
	}

	@Test
	public void testSimpleStyles() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-simplestyles.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals(
			"Here’s some text with different styles applied.",
			tus.get(0).getSource().getCodedText().toString());
	}

	@Test
	public void testOverlappingStyles() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-overlapping.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals(
			"This document has overlapping styles.",
			tus.get(0).getSource().getCodedText().toString());
		assertEquals("This <run1>document has <run2>overlapping styles</run2></run1>.",
				tus.get(0).getSource().toString());
	}

	@DataProvider
	public static Object[][] testNoBreakHyphenToCharacterConversionProvider() {
		return new Object[][] {
				{
						new ConditionalParametersBuilder()
								.replaceNoBreakHyphenTag(false)
								.build(),
						new String[] {
								"No break<tags1/>hyphen.<run2>No break<tags3/></run2><tags4/>hyphen.<run5/>"
						}
				},
				{
						new ConditionalParametersBuilder()
								.replaceNoBreakHyphenTag(true)
								.build(),
						new String[] {
								"No break-hyphen.<run1>No break-</run1>-hyphen.<run2>-</run2>"
						}
				},
		};
	}

	@Test
	@UseDataProvider("testNoBreakHyphenToCharacterConversionProvider")
	public void testNoBreakHyphenToCharacterConversion(ConditionalParameters params, String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-no-break-hyphen.xml"), params);
		checkExpected(tus, expectedTexts);
	}

	@DataProvider
	public static Object[][] testSoftHyphenIgnorationProvider() {
		return new Object[][] {
				{
						new ConditionalParametersBuilder()
								.ignoreSoftHyphenTag(false)
								.build(),
						new String[] {
								"This sentence needs to be long enough to cause some kind of line br<tags1/>eaking.<run2>This sentence needs to be long enough to cause some kind of line br<tags3/></run2><tags4/>eaking.<run5/>"
						}
				},
				{
						new ConditionalParametersBuilder()
								.ignoreSoftHyphenTag(true)
								.build(),
						new String[] {
								"This sentence needs to be long enough to cause some kind of line breaking.<run1>This sentence needs to be long enough to cause some kind of line br</run1>eaking."
						}
				},
		};
	}

	@Test
	@UseDataProvider("testSoftHyphenIgnorationProvider")
	public void testSoftHyphenIgnoration(ConditionalParameters params, String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-soft-hyphen.xml"), params);
		checkExpected(tus, expectedTexts);
	}

	@DataProvider
	public static Object[][] testLineBreakToCharacterConversionProvider() {
		return new Object[][] {
				{
						new ConditionalParametersBuilder()
								.addLineSeparatorCharacter(false)
								.build(),
						new String[] {
								"Line<tags1/> break.<run2>Line<tags3/></run2><tags4/> break.<run5/>",
								"Carriage<tags1/> return.<run2>Carriage<tags3/></run2><tags4/> return.<run5/>"
						}
				},
				{
						new ConditionalParametersBuilder()
								.addLineSeparatorCharacter(true)
								.build(),
						new String[] {
								"Line\n break.<run1>Line\n</run1>\n break.<run2>\n</run2>",
								"Carriage\n return.<run1>Carriage\n</run1>\n return.<run2>\n</run2>"
						}
				},
		};
	}

	@Test
	@UseDataProvider("testLineBreakToCharacterConversionProvider")
	public void testLineBreakToCharacterConversion(ConditionalParameters params, String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-br.xml"), params);
		checkExpected(tus, expectedTexts);
	}

	@DataProvider
	public static Object[][] testComplexScriptTagSkippingProvider() {
		return new Object[][] {
				{
						new ConditionalParametersBuilder()
								.cleanupAggressively(false)
								.build(),
						new String[] {
								"The <run1>garbage</run1><run2> disposal</run2> issue is a real headache to "
						}
				},
				{
						new ConditionalParametersBuilder()
								.cleanupAggressively(true)
								.build(),
						new String[] {
								"The garbage disposal issue is a real headache to "
						}
				},
		};
	}

	@Test
	@UseDataProvider("testComplexScriptTagSkippingProvider")
	public void testComplexScriptTagSkipping(ConditionalParameters params, String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-complex-script-skip.xml"), params);
		checkExpected(tus, expectedTexts);
	}

	@DataProvider
	public static Object[][] testHyperlinkComplexFieldCharactersProvider() {
		return new Object[][] {
				{
						new String[] {
								"Tags: Diacetyl, E-Zigarette, Pneumologie, Popcorn Workers Lung",
								"<tags1/>Jetzt nahm Joseph G. Allen, Forscher an der Harvard T.H. Chan School <tags2/>of<tags3/> Public <tags4/>Health<tags5/> in Boston, Inhaltsstoffe unter die Lupe<tags6/>. Für seine Studie erwarb er 51 E-Zigaretten von führenden US-Herstellern. ",
								"Ursprünglich ging es um Arbeiter in Popcorn-Fabriken, die lange Zeit synthetischen Aromen ausgesetzt waren („Popcorn Workers für E-Zigaretten. Europa stellt keine Ausnahme dar. Die <tags1/>European Food <tags2/>Safety<tags3/> Authority (EFSA)<tags4/> beschränkt sich momentan auf „waitandsee“.",
						},
				},
		};
	}

	@Test
	@UseDataProvider("testHyperlinkComplexFieldCharactersProvider")
	public void testHyperlinkComplexFieldCharacters(String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-hyperlink-fldChar.xml"), defaultParams);
		checkExpected(tus, expectedTexts);
	}

	@DataProvider
	public static Object[][] testNestedComplexFieldCharactersProvider() {
		return new Object[][] {
				{
						new String[] {
								"Table of Contents",
								"Computer",
								"Computer science or computing science (sometimes abbreviated CS) is the study of the theoretical foundations of information and computation, and of practical techniques for their implementation and application in computer systems.",
								"Science",
								"It is frequently described as the systematic study of algorithmic processes that create, describe, and transform information.",
								"Article",
								"Computer science has many sub-fields; some, such as computer graphics, emphasize the computation of specific results, while others, such as computational complexity theory, study the properties of computational problems.",
								"From",
								"Still others focus on the challenges in implementing computations.",
								"Wikipedia",
								"For example, programming language theory studies approaches to describe computations, while computer programming applies specific programming languages to solve specific computational problems, and human-computer interaction focuses on the challenges in making computers and computations useful, usable, and universally accessible to people.",
								"Pasted",
								"The general public sometimes confuses computer science with careers that deal with computers (such as information technology), or think that it relates to their own experience of computers, which typically involves activities such as gaming, web-browsing, and word-processing.",
								"Word",
								"However, the focus of computer science is more on understanding the properties of the programs used to implement software such as games and web-browsers, and using that understanding to create new programs or improve existing ones.",
						},
				},
		};
	}

	@Test
	@UseDataProvider("testNestedComplexFieldCharactersProvider")
	public void testNestedComplexFieldCharacters(String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-nested-fldChar.xml"), defaultParams);
		checkExpected(tus, expectedTexts);
	}

	@DataProvider
	public static Object[][] acceptsRevisionsInComplexFieldsDataProvider() {
		return new Object[][] {
			{
				"768-document.xml",
				new String[] {"The first run.<run1><tags2/>inside the ins revision<tags3/>The last run.</run1>"},
			},
			{
				"768-2-document.xml",
				new String[] {"The first run.<run1><tags2/>I<tags3/>nside<tags4/> the <tags5/>ins<tags6/> <tags7/>revision<tags8/>.The last run.</run1>"},
			},
		};
	}

	@Test
	@UseDataProvider("acceptsRevisionsInComplexFieldsDataProvider")
	public void acceptsRevisionsInComplexFields(final String resource, final String[] expectedTexts) throws Exception {
		final List<ITextUnit> textUnits = parseTextUnits(getReader(resource), defaultParams);
		checkExpected(textUnits, expectedTexts);
	}

	@DataProvider
	public static Object[][] testAlternateContentProvider() {
		return new Object[][] {
				{
						new String[] {
								"",
								"some object",
								"image2.png",
								"some object",
								"",
								"some object",
								"ONE<run1>???</run1>TWO ",
								"THREE ",
								"some object",
								"some object",
						},
				},
		};
	}

	@Test
	@UseDataProvider("testAlternateContentProvider")
	public void testAlternateContent(String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-alternate-content.xml"), defaultParams);
		checkExpected(tus, expectedTexts);
	}

	@Test
	public void testNestedBlocksIds() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("header-nested-blocks-ids.xml"), defaultParams);
		assertEquals(57, tus.size());
		assertNotEquals("P1467E3A-sub1", tus.get(30).getId());
		assertEquals("P1467E3A-sub30", tus.get(30).getId());
	}

	@Test
	public void testEmptyRunIgnoration() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-empty-run.xml"), defaultParams);
		assertEquals(0, tus.size());
	}

	@Test
	public void testComplexStyles() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-complexstyles.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals(
			"This sentence contains very complex styling.",
			tus.get(0).getSource().getCodedText().toString());
		assertEquals("This <run1>sentence <run2>contains</run2></run1><run3> <run4>very</run4></run3> complex <run5>styling.</run5>",
				tus.get(0).getSource().toString());
	}

	@Test
	public void testComplexStyles2() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-complexstyles2.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals(
				"This sentence contains some more complex styling.",
				tus.get(0).getSource().getCodedText().toString());
		assertEquals("<run1>This <run2>sentence</run2> <run3>contains</run3> <run4>some</run4> more</run1> complex styling.",
				tus.get(0).getSource().toString());
	}

	@Test
	public void testNestedSmartTag() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-smarttag.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals("MATTRESS THICKNESS:\uE101\uE110 ABOUT \uE101\uE11129 CM2\uE101\uE11229 CM3\uE102\uE113\uE101\uE114\uE101\uE115TEST1\uE102\uE116\uE101\uE117TEST2\uE102\uE118\uE102\uE11929 CM1\uE102\uE11A\uE102\uE11B",
				tus.get(0).getSource().getCodedText());
		assertEquals("MATTRESS THICKNESS:<run1> ABOUT <smartTag2>29 CM2<smartTag3>29 CM3</smartTag3><smartTag4><run5>TEST1</run5><run6>TEST2</run6></smartTag4>29 CM1</smartTag2></run1>",
				tus.get(0).getSource().toString());

	}

	@Test
	public void testHyperlink() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-hyperlink.xml"), defaultParams);
		assertEquals(5, tus.size());
		assertEquals(
				"This contains other \uE101\uE110\uE101\uE111hyperlink 1\uE102\uE112\uE101\uE113hyperlink 2\uE102\uE114\uE102\uE115\uE101\uE116.\uE102\uE117",
				tus.get(0).getSource().getCodedText());
		assertEquals("This contains other <hyperlink1><run2>hyperlink 1</run2><run3>hyperlink 2</run3></hyperlink1><run4>.</run4>",
				tus.get(0).getSource().toString());
		assertEquals(
				"This contains a \uE101\uE110hyperlink\uE102\uE111.",
				tus.get(1).getSource().getCodedText());
		assertEquals("This contains a <hyperlink1>hyperlink</hyperlink1>.",
				tus.get(1).getSource().toString());
		assertEquals(
				"\uE101\uE110This contains other \uE101\uE111\uE101\uE112hyperlink 1\uE102\uE113\uE101\uE114hyperlink 2\uE102\uE115\uE102\uE116\uE102\uE117.",
				tus.get(2).getSource().getCodedText());
		assertEquals("<run1>This contains other <hyperlink2><run3>hyperlink 1</run3><run4>hyperlink 2</run4></hyperlink2></run1>.",
				tus.get(2).getSource().toString());
		assertEquals(
				"Here’s another hyperlink that \uE101\uE110\uE101\uE111contains \uE101\uE112styled\uE102\uE113 markup\uE102\uE114\uE102\uE115.",
				tus.get(3).getSource().getCodedText());
		assertEquals("Here’s another hyperlink that <hyperlink1><run2>contains <run3>styled</run3> markup</run2></hyperlink1>.",
				tus.get(3).getSource().toString());
		assertEquals(
				"And another one\uE103\uE110.\uE103\uE111",
				tus.get(4).getSource().getCodedText());
		assertEquals("And another one<tags1/>.<tags2/>",
				tus.get(4).getSource().toString());
	}

	@Test
	public void testTextBoxInAlternateContent() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-textbox.xml"), defaultParams);
		assertEquals(3, tus.size());
		assertEquals("This is not in the text box.", tus.get(0).getSource().getCodedText());
		assertEquals("This is not in the text box.", tus.get(0).getSource().toString());
		assertEquals("Text Box 1", tus.get(1).getSource().getCodedText());
		assertEquals("This is a text box.", tus.get(2).getSource().getCodedText());
		assertEquals("This is a <run1>text box</run1>.", tus.get(2).getSource().toString());
		assertFalse(tus.get(0).isReferent());
		assertTrue(tus.get(1).isReferent());
		assertTrue(tus.get(2).isReferent());
	}

	@Test
	public void testTextBoxWithNameOptionDisabled() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateWordExcludeGraphicMetaData(true);
		List<ITextUnit> tus = parseTextUnits(getReader("document-textbox.xml"), params);
		assertEquals(2, tus.size());
	}

	@Test
	public void testTextBox() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-textboxes.xml"), defaultParams);
		assertEquals(4, tus.size());
		assertEquals("Doggy ", tus.get(0).getSource().toString());
		assertEquals("unbelievable", tus.get(1).getSource().toString());
		assertEquals("pig", tus.get(2).getSource().toString());
		assertEquals("This here is a text box.  ", tus.get(3).getSource().toString());
		assertFalse(tus.get(0).isReferent());
		assertTrue(tus.get(1).isReferent());
		assertTrue(tus.get(2).isReferent());
		assertTrue(tus.get(3).isReferent());
		assertEquals("3", tus.get(0).getProperty(TextUnitMapper.REFERENCES).getValue());
		assertEquals("0", tus.get(1).getProperty(TextUnitMapper.REFERENCES).getValue());
		assertEquals("0", tus.get(2).getProperty(TextUnitMapper.REFERENCES).getValue());
		assertEquals("0", tus.get(3).getProperty(TextUnitMapper.REFERENCES).getValue());
	}

	@Test
	public void testFindRunAndTextNames() throws Exception {
		Block block = getBlock(getReader("document-complexstyles.xml"), defaultParams);
		assertEquals(Namespaces.WordProcessingML.getURI(), block.getRunName().getNamespaceURI());
		assertEquals("r", block.getRunName().getLocalPart());
		assertEquals(Namespaces.WordProcessingML.getURI(), block.getTextName().getNamespaceURI());
		assertEquals("t", block.getTextName().getLocalPart());
	}

	@Test
	public void testTextpath() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-textpath.xml"), defaultParams);
		assertEquals(4, tus.size());
		assertEquals("Computer science", tus.get(0).getSource().toString());
		assertEquals("", tus.get(1).getSource().toString());
		assertEquals("Word art is amazing!", tus.get(2).getSource().toString());
		assertEquals("systematic", tus.get(3).getSource().toString());
	}

	@Test
	public void testSimpleFields() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("footer-fldSimple.xml"), defaultParams);
		// The TU contains only a single code, so it's made non-translatable
		assertEquals(0, tus.size());
	}

	@Test
	public void testFieldCodes() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-instrText.xml"), defaultParams);
		assertEquals(0, tus.size());
	}

	@Test
	public void testSimpleFields2() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("footer-simpleCode2.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals("Date: ", tus.get(0).getSource().getCodedText());
		assertEquals("Date: <tags1/>", tus.get(0).getSource().toString());
	}

	@Test
	public void testMultipleTabs() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-multitab.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals("A<tags1/>B", tus.get(0).getSource().toString());
	}

	@Test
	public void testNoProof() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-noproof.xml"), defaultParams);
		assertEquals("hello Δ1 world", tus.get(0).getSource().toString());
	}

	@Test
	public void testEmptyFootnotes() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("footnotes-empty.xml"), defaultParams);
		assertEquals(0, tus.size());
	}

	@Test
	public void testStyledHyperlink() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-styled-hyperlink.xml"), defaultParams);
		assertEquals(1, tus.size());
		assertEquals("This is the text with the <hyperlink1>hyperlink</hyperlink1>.", tus.get(0).getSource().toString());
	}

	@DataProvider
	public static Object[][] testRunHintsAndFontVariationProvider() {
		return new Object[][] {
			{
				new String[] {
					"T. 1, Paragraph 1: hint - disabled, same fonts themes;T. 1: Paragraph 2: hint - disabled, same fonts themes;",
					"T. 2: Paragraph 1: hint - enabled, same fonts themes;T. 2 Paragraph 2: hint - disabled, same fonts themes;",
					"T. 3 Paragraph 1: hint - disabled, same fonts themes;" +
							"T. 3: Paragraph 2: hint - enabled, same fonts themes;",
					"T. 4: Paragraph 1: hint - enabled, same to the next run, same fonts themes;" +
							"T. 4: Paragraph 2: hint - enabled, same to the previous run, same fonts themes;",
					"T. 5: Paragraph 1: hint - enabled, different to the next run, same fonts themes;" +
							"T. 5: Paragraph 2: hint - enabled, different to the previous run, same fonts themes;",
					"T. 6: Paragraph 1: hint - enabled, different to the next run, different fonts themes;" +
							"<run1>T. 6: Paragraph 2: hint - enabled, different to the previous run, different fonts themes;</run1>",
					"T. 7: Paragraph 1: hint - disabled, different to the next run, different fonts themes;" +
							"<run1>T. 7: Paragraph 2: hint - disabled, different to the previous run, different fonts themes;</run1>",
					"T. 8: Paragraph 1: hint - disabled, different to the next run, no fonts themes;<run1>T. 8: Paragraph 2: hint - disabled, different to the previous run, present font theme;</run1>"
				}
			}
		};
	}

	@Test
	@UseDataProvider("testRunHintsAndFontVariationProvider")
	public void testRunHintsAndFontVariations(String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-hint.xml"), defaultParams);
		checkExpected(tus, expectedTexts);
	}

	@Test
	public void testFieldSimple2() throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("footer-fldSimple2.xml"), defaultParams);
		assertEquals(0, tus.size());
	}

	@DataProvider
	public static Object[][] testVanishRunPropertyDataProvider() {
		return new Object[][]{
				{
						new ConditionalParametersBuilder()
								.translateWordHidden(false)
								.build(),
						new String[]{
								"Here is the [visible] <run1/>message [visible] written by the hand [rStyle Haydn] of Jeremiah [visible].",
								"Here is the message of Isaiah (with hidden pStyle FranzJosef).",
								"Here is the 5th message with direct vanish prop in pPr.",
								"Here is the 6th message with direct false vanish prop overriding vanish prop in pPr.",
								"Here is the 7th message with RunStyle1.",
								"Here is the 8th message with ParagraphStyle1.",
								"Here is the 9th message with RunStyle2.",
								"Here is the 10th message with ParagraphStyle2.",
								"Here is the 11th message with ParagraphStyle2 and RunStyle2.",
								"Here is the 12th message with RunStyleB.",
								"Here is the 13th message with ParagraphStyleB.",
								"Here is the 14th message with RunStyleC.",
								"Here is the 15th message with ParagraphStyleC.",
								"Here is the 16th message with ParagraphStyleC and RunStyleC.",
								"Here is the 17th message with direct false vanish prop overriding vanish prop in Haydn rStyle.",
								"Here is the 18th message with direct false vanish prop overriding vanish prop in FranzJosef pStyle.",
								"Here is the 19th message with direct false vanish prop overriding vanish prop in Haydn rStyle in pPr.",
								"Here is the 20th message with Haydn rStyle in pPr.",
						}
				},
				{
						new ConditionalParametersBuilder()
								.translateWordHidden(true)
								.build(),
						new String[]{
								"Here is the [visible] <run1/>message [visible] written by the hand [rStyle Haydn] of Jeremiah [visible].",
								"Here is the message of Isaiah (with hidden pStyle FranzJosef).",
								"Here is the 5th message with direct vanish prop in pPr.",
								"Here is the 6th message with direct false vanish prop overriding vanish prop in pPr.",
								"Here is the 7th message with RunStyle1.",
								"Here is the 8th message with ParagraphStyle1.",
								"Here is the 9th message with RunStyle2.",
								"Here is the 10th message with ParagraphStyle2.",
								"Here is the 11th message with ParagraphStyle2 and RunStyle2.",
								"Here is the 12th message with RunStyleB.",
								"Here is the 13th message with ParagraphStyleB.",
								"Here is the 14th message with RunStyleC.",
								"Here is the 15th message with ParagraphStyleC.",
								"Here is the 16th message with ParagraphStyleC and RunStyleC.",
								"Here is the 17th message with direct false vanish prop overriding vanish prop in Haydn rStyle.",
								"Here is the 18th message with direct false vanish prop overriding vanish prop in FranzJosef pStyle.",
								"Here is the 19th message with direct false vanish prop overriding vanish prop in Haydn rStyle in pPr.",
								"Here is the 20th message with Haydn rStyle in pPr.",
						}
				},
		};
	}

	@Test
	@UseDataProvider("testVanishRunPropertyDataProvider")
	public void testVanishRunProperty(String[] expectedTexts) throws Exception {
		List<ITextUnit> textUnits = parseTextUnits(getReader("document-hidden.xml"), defaultParams);
		checkExpected(textUnits, expectedTexts);
	}

	@DataProvider
	public static Object[][] testTableTusProvider() {
		return new Object[][] {
			{
				new String[] {
					"Text00",
					"Text11",
					"Text11:00",
					"Text11:11",
					"Text12"
				}
			}
		};
	}

	@Test
	@UseDataProvider("testTableTusProvider")
	public void testTableTus(String[] expectedTexts) throws Exception {
		List<ITextUnit> tus = parseTextUnits(getReader("document-bidi-table-properties-2.xml"), defaultParams);
		checkExpected(tus, expectedTexts);
	}

	private void dump(List<ITextUnit> tus) {
		for (ITextUnit tu : tus) {
			//System.out.println(tu.getSource().getCodedText());
			System.out.println(tu.getSource().toString());
		}
	}

	private void checkExpected(List<ITextUnit> tus, String[] expectedTexts) {
		assertEquals(expectedTexts.length, tus.size());
		for (int i = 0; i < tus.size(); i++) {
			assertEquals(expectedTexts[i], tus.get(i).getSource().toString());
		}
	}

	private XMLEventReader getReader(String resource) throws Exception {
		return factories.getInputFactory().createXMLEventReader(
				new InputStreamReader(root.in("/parts/block/" + resource).asInputStream(),
						StandardCharsets.UTF_8));
	}
	private List<ITextUnit> parseTextUnits(XMLEventReader xmlReader, ConditionalParameters params) throws XMLStreamException {
		List<ITextUnit> tus = new ArrayList<>();
		IdGenerator textUnitId = new IdGenerator("root", "tu");
		// XXX This code is a little redundant with code in StyledTextPartHandler
		while (xmlReader.hasNext()) {
			XMLEvent e = xmlReader.nextEvent();
			if (isParagraphStartEvent(e)) {
				Block block = new BlockParser(
					createStartElementContext(
						e.asStartElement(),
						xmlReader,
						this.factories.getEventFactory(),
						params
					),
					new IdGenerator(null),
					new StyleDefinitions.Empty(),
					new StyleOptimisation.Bypass()
				).parse();
				final BlockTextUnitMapper mapper = new BlockTextUnitMapper(
					textUnitId,
					this.factories.getEventFactory(),
					block
				);
				tus.addAll(mapper.map());
			}
		}
		return tus;
	}
	private Block getBlock(XMLEventReader xmlReader, ConditionalParameters params) throws XMLStreamException {
		while (xmlReader.hasNext()) {
			XMLEvent e = xmlReader.nextEvent();
			if (isParagraphStartEvent(e)) {
				return new BlockParser(createStartElementContext(e.asStartElement(), xmlReader, factories.getEventFactory(), params),
						new IdGenerator(null), new StyleDefinitions.Empty(), new StyleOptimisation.Bypass()).parse();
			}
		}
		throw new IllegalStateException();
	}
}
