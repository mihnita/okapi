package net.sf.okapi.filters.openxml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import net.sf.okapi.common.DefaultLocalePair;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiUnexpectedRevisionException;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;

import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import org.custommonkey.xmlunit.Diff;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

@RunWith(JUnit4.class)
public class TestStyledTextUnitWriter {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private XMLFactories factories = new XMLFactoriesForTest();

	@Test
	public void testSimpleStyles() throws Exception {
		checkFile("document-simplestyles.xml", new ConditionalParameters());
	}

	@Test
	public void testComplexStyles() throws Exception {
		checkFile("document-complexstyles.xml", new ConditionalParameters());
	}

	@Test
	public void testComplexStyles2() throws Exception {
		checkFile("document-complexstyles2.xml", new ConditionalParameters());
	}

	@Test
	public void testTab() throws Exception {
		checkFile("document-tab.xml", new ConditionalParameters());
	}

	@Test
	public void testHyperlink() throws Exception {
		checkFile("document-hyperlink.xml", new ConditionalParameters());
	}

	@Test
	public void testSmartTag() throws Exception {
		checkFile("document-smarttag.xml", new ConditionalParameters());
	}

	@Test
	public void testEmpty() throws Exception {
		checkFile("document-empty.xml", new ConditionalParameters());
	}

	@Test
	public void testOverlapping() throws Exception {
		checkFile("document-overlapping.xml", new ConditionalParameters());
	}

	@Test
	public void testHidden() throws Exception {
		checkFile("document-hidden.xml", new ConditionalParameters());
	}

	@Test
	public void testTextbox() throws Exception {
		checkFile("document-textbox.xml", new ConditionalParameters());
	}

	@Test
	public void testTextbox2() throws Exception {
		checkFile("document-textboxes.xml", new ConditionalParameters());
	}

	@Test
	public void testEscaping() throws Exception {
		checkFile("document-escaping.xml", new ConditionalParameters());
	}

	@Test
	public void testTextpath() throws Exception {
		checkFile("document-textpath.xml", new ConditionalParameters());
	}

	@Test
	public void testNoBreakHyphenToCharacterConversion() throws Exception {
		checkFile("document-no-break-hyphen.xml", new ConditionalParametersBuilder().replaceNoBreakHyphenTag(true).build());
	}

	@Test
	public void testSoftHyphenIgnoration() throws Exception {
		checkFile("document-soft-hyphen.xml", new ConditionalParametersBuilder().ignoreSoftHyphenTag(true).build());
	}

	@Test
	public void testLineBreakToCharacterConversion() throws Exception {
		checkFile("document-br.xml", new ConditionalParametersBuilder().addLineSeparatorCharacter(true).build());
	}

	@Test
	public void testBcsSkip() throws Exception {
		checkFile("document-complex-script-skip.xml", new ConditionalParametersBuilder().cleanupAggressively(true).build());
	}

	@Test
	public void testHyperlinkComplexFieldCharacters() throws Exception {
		checkFile("document-hyperlink-fldChar.xml", new ConditionalParameters());
	}

	@Test
	public void testNestedComplexFieldCharacters() throws Exception {
		checkFile("document-nested-fldChar.xml", new ConditionalParameters());
	}

	@Test
	public void testAlternateContent() throws Exception {
		checkFile("document-alternate-content.xml", new ConditionalParameters());
	}

	@Test
	public void testEmptyRunIgnoration() throws Exception {
		checkFile("document-empty-run.xml", new ConditionalParameters());
	}

	@Test
	public void testAttributesStripping() throws Exception {
		checkFile("slide-strippable-attributes.xml", new ConditionalParameters());
	}

	@Test
	public void testBidirectionality() throws Exception {
		final ConditionalParameters cp = new ConditionalParameters();
		checkFile("document-bidi-rtl.xml", cp, LocaleId.ENGLISH, LocaleId.ARABIC);
		checkFile("document-bidi-rtl-2.xml", cp, LocaleId.ENGLISH, LocaleId.ENGLISH);
		checkFile("document-bidi-rtl-3.xml", cp, LocaleId.ARABIC, LocaleId.ENGLISH);
		checkFile("document-bidi-rtl-4.xml", cp, LocaleId.ENGLISH, LocaleId.ARABIC);
		checkFile("document-bidi-rtl-5.xml", cp, LocaleId.ARABIC, LocaleId.ARABIC);
		checkFile("document-bidi-rtl-lang.xml", cp, LocaleId.ENGLISH, LocaleId.HEBREW);
		checkFile("document-bidi-table-properties-1.xml", cp, LocaleId.ARABIC, LocaleId.ENGLISH);
		checkFile("document-bidi-table-properties-2.xml", cp, LocaleId.ENGLISH, LocaleId.ARABIC);
		checkFile("slide-bidi-table-and-text-body-attributes-en.xml", cp, LocaleId.ENGLISH, LocaleId.ENGLISH);
		checkFile("slide-bidi-table-and-text-body-attributes-ar.xml", cp, LocaleId.ENGLISH, LocaleId.ARABIC);
	}

	@Test
	public void acceptsRevisions() throws Exception {
		checkFile("document-revision-information-stripping.xml", new ConditionalParameters());
		checkFile("768-document.xml", new ConditionalParameters());
		checkFile("768-2-document.xml", new ConditionalParameters());
	}

	@Test(expected=OkapiUnexpectedRevisionException.class)
	public void testRevisionInformationIsNotStripped() throws Exception {
		checkFile("document-revision-information-stripping.xml", new ConditionalParametersBuilder().automaticallyAcceptRevisions(false).build());
	}

	@Test
	public void testWatermarkQuoteEscaping() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		List<Event> events = parseEvents(getReaderBlockPart("header-watermark.xml"), params);

		List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
		ITextUnit watermarkTU = tus.get(1);
		// Make sure ordering hasn't changed and this is still the TU I expect
		assertEquals("DRAFT", watermarkTU.getSource().toString());
		watermarkTU.setTarget(LocaleId.FRENCH, new TextContainer(new TextFragment("DRAFT\"")));

		writeAndCompare("header-watermark.xml", events, params, LocaleId.ENGLISH, LocaleId.FRENCH);
	}

	private void checkFile(String name, ConditionalParameters params) throws Exception {
		checkFile(name, params, LocaleId.FRENCH, LocaleId.FRENCH);
	}

	private void checkFile(String name, ConditionalParameters params, LocaleId sourceLocale, LocaleId targetLocale) throws Exception {
		List<Event> events = parseEvents(getReaderBlockPart(name), params);
		writeAndCompare(name, events, params, sourceLocale, targetLocale);
	}

	private void writeAndCompare(
		String name,
		List<Event> events,
		ConditionalParameters params,
		LocaleId sourceLocale,
		LocaleId targetLocale
	) throws Exception {
		StyledTextSkeletonWriter skelWriter = new StyledTextSkeletonWriter(
			"test",
            sourceLocale,
			targetLocale,
			params,
			factories.getEventFactory(),
			params.fontMappings().applicableTo(new DefaultLocalePair(sourceLocale, targetLocale)),
			new GenericSkeletonWriter()
		);
		Path temp = Files.createTempFile(name, ".xml");
		//System.out.println("Checking " + name + " against " + temp);
		Writer w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);
		for (Event event : events) {
			w.write(handleEvent(skelWriter, event, targetLocale));
		}
		w.close();

		Path goldPath = FileLocation.fromClass(getClass()).in("/gold/parts/block/" + name).asPath();
		String goldContent = new String(Files.readAllBytes(goldPath), StandardCharsets.UTF_8);
		String tempContent = new String(Files.readAllBytes(temp), StandardCharsets.UTF_8);

		Diff diff = new Diff(goldContent, tempContent);
		if (!diff.similar()) {
			StringBuffer sb = new StringBuffer("'" + goldPath + "' gold file does not match " + temp + ":");
			diff.appendMessage(sb);
			LOGGER.warn(sb.toString());
			assertThat(tempContent).isXmlEqualTo(goldContent);
		}

		Files.delete(temp);
	}

	private String handleEvent(StyledTextSkeletonWriter skelWriter, Event event, LocaleId targetLocale) {
		switch ( event.getEventType() ) {
		case START_DOCUMENT:
			return skelWriter.processStartDocument(targetLocale, "UTF-8", null, null, event.getStartDocument());
		case END_DOCUMENT:
			return skelWriter.processEndDocument(event.getEnding());
		case START_SUBDOCUMENT:
			return skelWriter.processStartSubDocument(event.getStartSubDocument());
		case END_SUBDOCUMENT:
			return skelWriter.processEndSubDocument(event.getEnding());
		case START_GROUP:
			return skelWriter.processStartGroup(event.getStartGroup());
		case END_GROUP:
			return skelWriter.processEndGroup(event.getEnding());
		case TEXT_UNIT:
			return skelWriter.processTextUnit(event.getTextUnit());
		case DOCUMENT_PART:
			return skelWriter.processDocumentPart(event.getDocumentPart());
		default:
			return "";
		}
	}

	private XMLEventReader getReaderBlockPart(String resource) throws Exception {
		final InputStream inputStream = FileLocation.fromClass(getClass()).in("/parts/block/" + resource).asInputStream();
		return factories.getInputFactory().createXMLEventReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
	}

	private List<Event> parseEvents(XMLEventReader xmlReader, ConditionalParameters params) throws IOException, XMLStreamException {
        final Document.General document = Mockito.mock(Document.General.class);
        Mockito.when(document.eventFactory()).thenReturn(factories.getEventFactory());
        Mockito.when(document.inputFactory()).thenReturn(factories.getInputFactory());
		Mockito.when(document.conditionalParameters()).thenReturn(params);
        final ZipEntry entry = Mockito.mock(ZipEntry.class);
        Mockito.when(entry.getName()).thenReturn("part");

		List<Event> events = new ArrayList<>();
		StyledTextPart part = new StyledTextPart(
            document,
            entry,
            new StyleDefinitions.Empty(),
            new StyleOptimisation.Bypass()
        );
		events.add(part.open("testDoc", "testSubDoc", xmlReader));
		while (part.hasNextEvent()) {
			events.add(part.nextEvent());
		}
		return events;
	}
}
