package net.sf.okapi.filters.openxml;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.custommonkey.xmlunit.Diff;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.FileLocation;

@RunWith(JUnit4.class)
public class TestParagraphSimplifier {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private ConditionalParameters defaultParameters = new ConditionalParametersBuilder()
			.cleanupAggressively(false)
			.addTabAsCharacter(false)
			.lineSeparatorAsChar(false)
			.build();
	private XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
	private XMLEventFactory eventFactory = XMLEventFactory.newInstance();
	private StyleDefinitions styleDefinitions = new StyleDefinitions.Empty();
	private StyleOptimisation bypassStyleOptimisation = new StyleOptimisation.Bypass();
	private StyleOptimisation powerpointStyleOptimisation = new StyleOptimisation.Default(
		new StyleOptimisation.Bypass(),
		this.defaultParameters,
		this.eventFactory,
		Namespaces.DrawingML.getQName("pPr", Namespace.PREFIX_A),
		Namespaces.DrawingML.getQName("defRPr", Namespace.PREFIX_A),
		Collections.emptyList(),
		styleDefinitions
	);
	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testSimplifier() throws Exception {
		simplifyAndCheckFile("document-simple.xml", bypassStyleOptimisation);
		simplifyAndCheckFileAggressive("document-simple.xml", bypassStyleOptimisation);
	}

	@Test
	public void testDontMergeWhenPropertiesDontMatch() throws Exception {
		simplifyAndCheckFile("document-prop_mismatch.xml", bypassStyleOptimisation);
	}

	@Test
	public void testWithTabs() throws Exception {
		simplifyAndCheckFile("document-multiple_tabs.xml", bypassStyleOptimisation);
	}

	@Test
	public void testHeaderWithConsecutiveTabs() throws Exception {
		simplifyAndCheckFile("header-tabs.xml", bypassStyleOptimisation);
	}

	@Test
	public void testTextBoxes() throws Exception {
		simplifyAndCheckFile("document-textboxes.xml", bypassStyleOptimisation);
	}

	@Test
	public void testRuby() throws Exception {
		simplifyAndCheckFile("document-ruby.xml", bypassStyleOptimisation);
	}

	@Test
	public void testSlide() throws Exception {
		simplifyAndCheckFile("slide-sample.xml", powerpointStyleOptimisation);
	}

	@Test
	public void testInstrText() throws Exception {
		simplifyAndCheckFile("document-instrText.xml", bypassStyleOptimisation);
	}

	@Test
	public void testAltContent() throws Exception {
		simplifyAndCheckFile("document-altcontent.xml", bypassStyleOptimisation);
	}

	@Test
	public void testPreserveSpaceReset() throws Exception {
		simplifyAndCheckFile("document-preserve.xml", bypassStyleOptimisation);
	}

	@Test
	public void testStripLastRenderedPagebreak() throws Exception {
		simplifyAndCheckFile("document-pagebreak.xml", bypassStyleOptimisation);
	}

	@Test
	public void testStripSpellingGrammarError() throws Exception {
		simplifyAndCheckFile("document-spelling.xml", bypassStyleOptimisation);
	}

	@Test
	public void testLangAttributeAndEmptyRunPropertyMerging() throws Exception {
		simplifyAndCheckFile("document-lang.xml", bypassStyleOptimisation);
	}

	@Test
	public void testDontConsolidateMathRuns() throws Exception {
		simplifyAndCheckFile("slide-formulas.xml", powerpointStyleOptimisation);
	}

	@Test
	public void testAggressiveSpacingTrimming() throws Exception {
		simplifyAndCheckFile("document-spacing.xml", bypassStyleOptimisation);
		simplifyAndCheckFileAggressive("document-spacing.xml", bypassStyleOptimisation);
	}

	@Test
	public void testAggressiveVertAlignTrimming() throws Exception {
		simplifyAndCheckFileAggressive("document-vertAlign.xml", bypassStyleOptimisation);
	}

	@Test
	public void testGoBackBookmark() throws Exception {
		simplifyAndCheckFile("document-goback.xml", bypassStyleOptimisation);
	}

	@Test
	public void testTab() throws Exception {
		simplifyAndCheckFileTabAsChar("document-tab.xml", bypassStyleOptimisation);
	}

	@Test
	public void testFonts() throws Exception {
		simplifyAndCheckFile("document-fonts.xml", bypassStyleOptimisation);
	}

	@Test
	public void testLineSeparatorSlide() throws Exception {
		final ConditionalParameters conditionalParameters = new ConditionalParametersBuilder()
			.cleanupAggressively(true)
			.addTabAsCharacter(false)
			.lineSeparatorAsChar(true)
			.lineSeparatorReplacement('\n')
			.build();
		simplifyAndCheckFileLineSeparatorAsChar(
			"slide-linebreak.xml",
			conditionalParameters,
			new StyleOptimisation.Default(
				new StyleOptimisation.Bypass(),
				conditionalParameters,
				this.eventFactory,
				Namespaces.DrawingML.getQName("pPr"),
				Namespaces.DrawingML.getQName("defRPr"),
				Collections.emptyList(),
				styleDefinitions
			)
		);
	}

	@Test
	public void testLineSeparatorSlide2028() throws Exception {
		final ConditionalParameters conditionalParameters = new ConditionalParametersBuilder()
			.cleanupAggressively(true)
			.addTabAsCharacter(false)
			.lineSeparatorAsChar(true)
			.lineSeparatorReplacement('\u2028')
			.build();
		simplifyAndCheckFileLineSeparatorAsChar(
			"slide-linebreak-2028.xml",
			conditionalParameters,
			new StyleOptimisation.Default(
				new StyleOptimisation.Bypass(),
				conditionalParameters,
				this.eventFactory,
				Namespaces.DrawingML.getQName("pPr"),
				Namespaces.DrawingML.getQName("defRPr"),
				Collections.emptyList(),
				styleDefinitions
			)
		);
	}

	// Simplify
	//   src/test/resources/parts/simplifier/[name]
	// And compare to
	//   src/test/resources/gold/parts/simplifier/[name]
	private void simplifyAndCheckFile(String name, StyleOptimisation styleOptimisation) throws Exception {
		simplifyAndCheckFile(name, "/gold/parts/simplifier/", this.defaultParameters,
				styleOptimisation);
	}
	private void simplifyAndCheckFileAggressive(String name, StyleOptimisation styleOptimisation) throws Exception {
		simplifyAndCheckFile(name, "/gold/parts/simplifier/aggressive/", new ConditionalParametersBuilder()
				.cleanupAggressively(true)
				.addTabAsCharacter(false)
				.lineSeparatorAsChar(false)
				.build(),
				styleOptimisation);
	}
	private void simplifyAndCheckFileTabAsChar(String name, StyleOptimisation styleOptimisation) throws Exception {
		simplifyAndCheckFile(name, "/gold/parts/simplifier/tabAsChar/", new ConditionalParametersBuilder()
				.cleanupAggressively(false)
				.addTabAsCharacter(true)
				.lineSeparatorAsChar(false)
				.build(),
				styleOptimisation);
	}

	private void simplifyAndCheckFileLineSeparatorAsChar(String name, ConditionalParameters conditionalParameters, StyleOptimisation styleOptimisation) throws Exception {
		simplifyAndCheckFile(name, "/gold/parts/simplifier/lbAsChar/",
				conditionalParameters, styleOptimisation);
	}

	private void simplifyAndCheckFile(String name, String goldDir, ConditionalParameters params,
									  StyleOptimisation styleOptimisation) throws Exception {
		final Path temp = simplifyFile(name, params, styleOptimisation);

		final Path goldFile = FileLocation.fromClass(getClass()).in(goldDir + name).asPath();
		final String goldContent = new String(Files.readAllBytes(goldFile), StandardCharsets.UTF_8);
		final String tempContent = new String(Files.readAllBytes(temp), StandardCharsets.UTF_8);

		final Diff diff = new Diff(goldContent, tempContent);
		if (!diff.similar()) {
			StringBuffer sb = new StringBuffer("'" + name + "' gold file does not match " + temp + ":");
			diff.appendMessage(sb);
			LOGGER.warn(sb.toString());
			assertEquals(goldContent, tempContent);
		}

		Files.delete(temp);
	}

	private Path simplifyFile(String name, ConditionalParameters params,
							  StyleOptimisation styleOptimisation) throws Exception {
		XMLEventReader xmlReader = inputFactory.createXMLEventReader(
				root.in("/parts/simplifier/" + name).asInputStream(), "UTF-8");
		Path temp = Files.createTempFile("simplify", ".xml");
		//System.out.println("Writing simplified " + name + " (aggressive=" + aggressiveTrimming + ") to " + temp);
		XMLEventWriter xmlWriter = outputFactory.createXMLEventWriter(
				Files.newBufferedWriter(temp, StandardCharsets.UTF_8));

		ParagraphSimplifier simplifier = new ParagraphSimplifier(xmlReader, xmlWriter, eventFactory, params,
				this.styleDefinitions, styleOptimisation);

		simplifier.process();
		xmlReader.close();
		xmlWriter.close();
		return temp;
	}
}