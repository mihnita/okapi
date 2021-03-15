/*===========================================================================
  Copyright (C) 2014-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xliff2;

import junit.framework.AssertionFailedError;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextPart;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class XLIFF2FilterTest {

	private final Logger logger = LoggerFactory.getLogger(XLIFF2FilterTest.class);

	private XLIFF2Filter filter;
	private FileLocation fl;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");

	@Before
	public void setUp() {
		filter = new XLIFF2Filter();
		fl = FileLocation.fromClass(this.getClass());
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreComments(true);
	}

	@Test
	public void testSimple() {
		String snippet = "<?xml version='1.0'?>\n"
				+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
				+ "<file id='f1'>\n" + "<unit id='u1'>" + "<segment id='s1'>\n" + "<source>Text.</source>"
				+ "</segment>\n" + "<ignorable><source> </source></ignorable>" + "<segment id='s2'>\n"
				+ "<source>src2</source>" + "<target>trg2</target>" + "</segment>\n" + "</unit>" + "</file>\n"
				+ "</xliff>\n";
		List<Event> events = FilterTestDriver.getEvents(filter, snippet, locEN, locFR);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		TextContainer stc = tu.getSource();
		assertEquals("Text.", stc.getFirstContent().toText());
		TextContainer ttc = tu.getTarget(locFR);
		assertTrue(ttc.getParts().get(0).getContent().isEmpty());
		assertEquals(" ", stc.getParts().get(1).getContent().getCodedText());
		assertEquals("src2", tu.getSource().getSegments().get("s2").getContent().toString());
		assertEquals("trg2", tu.getTarget(locFR).getSegments().get("s2").getContent().toString());
	}

	@Test
	public void testSimpleMeta() {
		String snippet = "<?xml version='1.0'?>\n"
				+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
				+ "<file id='f1'>\n" + "<unit id='u1'>"
				+ " <mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">\n"
				+ "               <mda:metaGroup category=\"subtitle\">\n"
				+ "                    <mda:meta type=\"startTimeStamp\">00:00:02,000</mda:meta>\n"
				+ "                    <mda:meta type=\"endTimeStamp\">00:00:04,000</mda:meta>\n"
				+ "               </mda:metaGroup>\n" + "          </mda:metadata>\n" + "" + "<segment id='s1'>\n"
				+ "<source>Text.</source>" + "</segment>\n" + "<ignorable><source> </source></ignorable>"
				+ "<segment id='s2'>\n" + "<source>src2</source>" + "<target>trg2</target>" + "</segment>\n" + "</unit>"
				+ "</file>\n" + "</xliff>\n";
		List<Event> events = FilterTestDriver.getEvents(filter, snippet, locEN, locFR);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		TextContainer stc = tu.getSource();
		assertEquals("Text.", stc.getFirstContent().toText());
		TextContainer ttc = tu.getTarget(locFR);
		assertTrue(ttc.getParts().get(0).getContent().isEmpty());
		assertEquals(" ", stc.getParts().get(1).getContent().getCodedText());
		assertEquals("src2", tu.getSource().getSegments().get("s2").getContent().toString());
		assertEquals("trg2", tu.getTarget(locFR).getSegments().get("s2").getContent().toString());
	}

	@Test
	public void testInline() {
		String snippet = "<?xml version='1.0'?>\n"
				+ "<xliff xmlns='urn:oasis:names:tc:xliff:document:2.0' version='2.0' srcLang='en' trgLang='fr'>"
				+ "<file id='f1'>\n" + "<unit id='u1'>" + "<segment id='s1'>\n"
				+ "<source><pc id='1' canDelete='no' dispStart='SC' dispEnd='EC'>"
				+ "<ph id='ph1' canCopy='no'/></pc></source>"
				+ "<target><pc id='1' canDelete='no' dispStart='SC' dispEnd='EC'>"
				+ "<ph id='ph1' canCopy='no'/></pc></target>" + "</segment>\n" + "</unit>" + "</file>\n" + "</xliff>\n";
		List<Event> events = FilterTestDriver.getEvents(filter, snippet, locEN, locFR);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		TextFragment frag = tu.getSource().getFirstContent();
		assertEquals(3, frag.getCodes().size());
		// Start pc
		Code code = frag.getCode(0);
		assertEquals(49, code.getId());
		assertEquals("1", code.getOriginalId());
		assertEquals(TagType.OPENING, code.getTagType());
		assertEquals("SC", code.getDisplayText());
		assertTrue(code.isCloneable());
		assertFalse(code.isDeleteable());
		// Placeholder
		code = frag.getCode(1);
		assertEquals(110905, code.getId());
		assertEquals("ph1", code.getOriginalId());
		assertEquals(TagType.PLACEHOLDER, code.getTagType());
		assertFalse(code.isCloneable());
		assertTrue(code.isDeleteable());
		// End pc
		code = frag.getCode(2);
		assertEquals(49, code.getId());
		assertEquals(TagType.CLOSING, code.getTagType());
		assertEquals("EC", code.getDisplayText());
		assertTrue(code.isCloneable());
		assertFalse(code.isDeleteable());
	}

	@Test
	public void testFromFile() {
		ITextUnit tu = FilterTestDriver.getTextUnit(filter, new InputDocument(fl.in("/test01.xlf").toString(), null),
				"UTF-8", locEN, locFR, 4);
		assertNotNull(tu);

		TextFragment tf = tu.getSource().getFirstContent();
		// TODO: should the \n be there?
		assertEquals("special text and more\n.", tf.getText());
		Code c = tf.getCode(0);
		assertEquals(49, c.getId());
	}

	@Test
	public void testFromFile2() {
		ITextUnit tu = FilterTestDriver.getTextUnit(filter, new InputDocument(fl.in("/test02.xlf").toString(), null),
				"UTF-8", locEN, locFR, 1);
		assertNotNull(tu);

		TextFragment tf = tu.getSource().getFirstContent();
		assertEquals("Sample segment.", tf.getText());
		assertNotNull(tu.getSkeleton());
	}

	@Test
	public void testFromEscapedFile() {
		ITextUnit tu = FilterTestDriver.getTextUnit(filter, new InputDocument(fl.in("/escaped.xlf").toString(), null),
				"UTF-8", locEN, locFR, 1);
		assertNotNull(tu);
		TextFragment tf = tu.getSource().getFirstContent();
		assertEquals("<p>This is a value that <em>I want</em> to be correctly translated.</p>", tf.getText());
		@SuppressWarnings("resource")
		List<Event> events = FilterTestDriver.getEvents(filter,
				new RawDocument(fl.in("/escaped.xlf").asInputStream(), "UTF-8", locEN, locFR), null);
		String result = FilterTestDriver.generateOutput(events, locFR, filter.createSkeletonWriter(),
				filter.getEncoderManager());
		assertTrue(result.contains("&lt;p>"));
	}

	@Test
	public void testGroupHandling() throws Exception {
		roundTripTest("/test01.xlf", "/gold/test01.xliff2.xlf", LocaleId.ENGLISH, LocaleId.FRENCH);
	}

	@Test
	public void testWriteXLIFF2AsXliff12() throws Exception {
		RawDocument rd = new RawDocument(fl.in("/test01.xlf").asUri(), "UTF-8", LocaleId.ENGLISH);
		rd.setTargetLocale(LocaleId.FRENCH);
		List<Event> events = FilterTestDriver.getEvents(filter, rd, null);

		Path temp = Files.createTempFile("okapi~xliff2~2", ".xlf");
		try (IFilterWriter w = new XLIFFWriter();
				OutputStream os = Files.newOutputStream(temp, StandardOpenOption.CREATE)) {
			w.setOptions(LocaleId.FRENCH, "UTF-8");
			w.setOutput(os);
			events.forEach(w::handleEvent);
		}
		try (Reader g = new InputStreamReader(fl.in("/gold/test01.xlf.xlf").asInputStream(), StandardCharsets.UTF_8);
				Reader o = Files.newBufferedReader(temp, StandardCharsets.UTF_8)) {
			logger.warn("{}", temp.toAbsolutePath());
			assertXMLEqual(g, o);
		}
		//Files.delete(temp);
		rd.close();
	}

	@Test
	public void testIgnoreable() {
		String snippet = "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\""
						+ " srcLang=\"en\" trgLang=\"fr\">"
						+ " <file id=\"f1\" original=\"Graphic Example.psd\">"
						+ "  <unit id=\"1\" canResegment=\"yes\">"
						+ "   <ignorable>"
						+ "<source xml:space=\"preserve\"> </source>"
						+ "<target xml:space=\"preserve\"> </target>"
						+ "   </ignorable>"
						+ "   <segment>"
						+ "<source>Quetzal. The great.</source>"
						+ "<target>Quetzal. Le Grand.</target>"
						+ "   </segment>"
						+ "   <ignorable>"
						+ "<source xml:space=\"preserve\"> </source>"
						+ "<target xml:space=\"preserve\"> </target>"
						+ "   </ignorable>"
						+ "  </unit>"
						+ " </file>"
						+ "</xliff>";

		List<Event> events = FilterTestDriver.getEvents(filter, snippet, locEN, locFR);
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
		assertNotNull(tu);
		TextContainer tc = tu.getSource();
		assertEquals(" Quetzal. The great. ", tc.toString());
		assertEquals(" ", tc.getFirstContent().toText());
		tc = tu.getTarget(locFR);
		assertEquals(" Quetzal. Le Grand. ", tc.toString());
		assertEquals(" ", tc.getFirstContent().toText());
	}

	@Test
	public void roundTripTests() throws Exception {
		roundTripTest("/roundtrips/ignorable.xlf", "/roundtrips/ignorable.xlf", LocaleId.ENGLISH,
				LocaleId.FRENCH);
		roundTripTest("/roundtrips/simple_input.xlf", "/roundtrips/simple_output.xlf", LocaleId.ENGLISH,
				LocaleId.GERMAN);
		roundTripTest("/roundtrips/simple_version_input.xlf", "/roundtrips/simple_version_output.xlf", LocaleId.ENGLISH,
				LocaleId.GERMAN);
		roundTripTest("/roundtrips/placeholders_input.xlf", "/roundtrips/placeholders_output.xlf", LocaleId.ENGLISH,
				LocaleId.GERMAN);
		roundTripTest("/roundtrips/empty_placeholder_input.xlf", "/roundtrips/empty_placeholder_output.xlf",
				LocaleId.ENGLISH, LocaleId.FRENCH);
		roundTripTest("/roundtrips/comprehensive_data_input.xlf", "/roundtrips/comprehensive_data_output.xlf",
				LocaleId.ENGLISH, LocaleId.FRENCH);
		roundTripTest("/roundtrips/multiple_placeholders_same_data_input.xlf",
				"/roundtrips/multiple_placeholders_same_data_output.xlf", LocaleId.ENGLISH, LocaleId.GERMAN);
	}

	@Test
	public void updateTarget() throws Exception {
		roundTripTest("/roundtrips/update_target_input.xlf", "/roundtrips/update_target_output.xlf", LocaleId.ENGLISH,
				LocaleId.GERMAN, (e) -> {
					if (e.isTextUnit()) {
						final ITextUnit textUnit = e.getTextUnit();
						final List<TextPart> parts = textUnit.getSource().getParts();
						final TextContainer targetTextContainer = textUnit.setTarget(LocaleId.GERMAN,
								new TextContainer(""));
						for (TextPart part : parts) {
							final TextFragment sourceContent = part.getContent();
							final TextFragment targetContent = new TextFragment();
							final String sourceText = sourceContent.getText();
							if (part.isSegment()) {
								final Segment segment = (Segment) part;
								switch (sourceText) {
								case "How is your day?":
									targetContent.append("Wie geht ");
									targetContent.append(checkCodeContents(sourceContent, 0, "<b>"));
									targetContent.append("es");
									targetContent.append(checkCodeContents(sourceContent, 1, "</b>"));
									targetContent.append(" Heute?");
									break;
								case "I wish the Ring had never come to me.":
									targetContent.append("Ich wünschte, der ");
									targetContent
											.append(checkCodeContents(sourceContent, 0, "<span class=\"emphasis\">"));
									targetContent.append("Ring");
									targetContent.append(checkCodeContents(sourceContent, 1, "</span>"));
									targetContent.append(" wäre nie zu mir gekommen.");
									break;
								case "I wish none of this had happened.":
									targetContent.append("Ich");
									targetContent
											.append(checkCodeContents(sourceContent, 0, "<img src=\"icon.svg\"/>"));
									targetContent.append("wünschte, nichts");
									targetContent.append(
											checkCodeContents(sourceContent, 1, "<img src=\"some_icon2.svg\"/>"));
									targetContent.append(" davon wäre passiert.");
									break;
								case "So do all who live to see such times, but that is not for them to decide.":
									targetContent
											.append("So machen es alle, die solche Zeiten erleben, aber das ist nicht "
													+ "für sie zu entscheiden.");
									break;
								case "All we have to decide is what to do with the time that is given to us.":
									targetContent.append("Wir müssen uns nur entscheiden, ");
									targetContent
											.append(checkCodeContents(sourceContent, 0, "<span class=\"emphasis\">"));
									targetContent.append(
											"was wir mit der uns zur Verfügung gestellten Zeit anfangen sollen");
									targetContent.append(checkCodeContents(sourceContent, 1, "</span>"));
									targetContent.append(".");
									break;
								default:
									targetContent.append(sourceContent);
								}
								targetTextContainer.append(new Segment(segment.getId(), targetContent));
							} else {
								targetTextContainer.append(part.clone());
							}
						}
					}
				});
	}

	@Test
	public void handleInvalidCodeTypes() throws Exception {

		final String invalidCodeType = "some_invalid_type";

		roundTripTest("/roundtrips/placeholder_code_type_input.xlf", "/roundtrips/placeholder_code_type_output.xlf",
				LocaleId.ENGLISH, LocaleId.FRENCH, (e) -> {
					if (e.isTextUnit()) {
						// Set the types of the codes to some invalid type, where the XLIFF Toolkit
						// would give an error
						// if the CTag was set to this value.
						e.getTextUnit().getSource().getParts()
								.forEach(part -> part.getContent().getCodes().forEach(c -> c.setType(invalidCodeType)));
					}

				});

	}

	@Test
	public void testDoubleExtraction() {
		final LocaleId fr = new LocaleId("fr");
		final LocaleId en = new LocaleId("en");
		List<InputDocument> list = new ArrayList<>();
		list.add(inputDocumentToTest("/roundtrips/multiple_placeholders_same_data_input.xlf", null));
		list.add(inputDocumentToTest("/roundtrips/comprehensive_data_input.xlf", null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", en, fr));

	}

	InputDocument inputDocumentToTest(String fileName, String paramFile) {
		return new InputDocument(fl.in("/" + fileName).toString(), paramFile);
	}

	/**
	 * Gets a code from the TextFragment and verifies that the data stored in the
	 * code matches the expected string.
	 *
	 * @param codedText    The content containing
	 * @param position
	 * @param expectedCode
	 * @return
	 */
	private Code checkCodeContents(TextFragment codedText, int position, String expectedCode) {
		assertTrue(codedText.hasCode());
		// Verified we're not going out of bounds
		assertTrue(position < codedText.getCodes().size());
		final Code code = codedText.getCode(position);
		assertEquals(code.getData(), expectedCode);
		return code;
	}

	/**
	 * Filters the inputXliff2 file from resources, then saves the filtered data
	 * back out to a temporary file and compares it with the compareXliff2 file from
	 * resources. If a lambda is provided for the last argument, it will perform
	 * that function on each event.
	 *
	 * @param inputXliff2    The input XLIFF 2.0 file from resources.
	 * @param compareXliff2  The gold output XLIFF 2.0 file from resources.
	 * @param sourceLocale   The source locale that should match the XLIFF 2.0
	 *                       file's srcLang.
	 * @param targetLocale   The source locale that should match the XLIFF 2.0
	 *                       file's trgLang.
	 * @param eventOperation The function to apply to every {@link Event}. If null,
	 *                       no action will be performed.
	 */
	private void roundTripTest(String inputXliff2, String compareXliff2, LocaleId sourceLocale, LocaleId targetLocale,
			EventOperation eventOperation) throws Exception {
		RawDocument rd = new RawDocument(fl.in(inputXliff2).asUri(), "UTF-8", sourceLocale);
		rd.setTargetLocale(targetLocale);
		List<Event> events = FilterTestDriver.getEvents(filter, rd, null);

		Path temp = Files.createTempFile("okapi~xliff2~2", ".xlf");
		try (IFilterWriter w = filter.createFilterWriter();
				OutputStream os = Files.newOutputStream(temp, StandardOpenOption.CREATE)) {
			w.setOptions(targetLocale, "UTF-8");
			w.setOutput(os);
			if (eventOperation != null) {
				events.forEach(eventOperation::actOnEvent);
			}
			events.forEach(w::handleEvent);
		}

		try (Reader g = new InputStreamReader(fl.in(compareXliff2).asInputStream(), StandardCharsets.UTF_8);
				Reader o = Files.newBufferedReader(temp, StandardCharsets.UTF_8)) {
			try {
				assertXMLEqual(g, o);
			} catch (AssertionFailedError | Exception exception) {
				logger.error(
						"XML Comparison failure. Compare the generated Temp file with the Gold file to find the "
								+ "problem. Whitespaces and comments are ignored.\n" + "Input File:       {}\n"
								+ "Output Gold File: {}\n" + "Output Temp File: {}",
						inputXliff2, compareXliff2, temp.toString());
				throw exception;
			}
		} finally {
			Files.delete(temp);
			rd.close();
		}
	}

	@Test
	public void testMetadataXLIFF2intoXliff12() throws Exception {
		RawDocument rd = new RawDocument(fl.in("/test02.xlf").asUri(), "UTF-8", LocaleId.ENGLISH);
		rd.setTargetLocale(LocaleId.FRENCH);
		List<Event> events = FilterTestDriver.getEvents(filter, rd, null);

		Path temp = Files.createTempFile("okapi~xliff2~", ".xlf");
		try (IFilterWriter w = new XLIFFWriter();
				OutputStream os = Files.newOutputStream(temp, StandardOpenOption.CREATE)) {
			w.setOptions(LocaleId.FRENCH, "UTF-8");
			w.setOutput(os);
			events.forEach(w::handleEvent);
		}
		try (Reader g = new InputStreamReader(fl.in("/gold/test02.xlf.xlf").asInputStream(), StandardCharsets.UTF_8);
				Reader o = Files.newBufferedReader(temp, StandardCharsets.UTF_8)) {
			logger.warn("{}", temp.toAbsolutePath());
			assertXMLEqual(g, o);
		} finally {
			Files.delete(temp);
			rd.close();
		}
	}

	/**
	 * @see #roundTripTest(String, String, LocaleId, LocaleId, EventOperation)
	 */
	private void roundTripTest(String inputXliff2, String compareXliff2, LocaleId sourceLocale, LocaleId targetLocale)
			throws Exception {
		roundTripTest(inputXliff2, compareXliff2, sourceLocale, targetLocale, null);
	}

	private interface EventOperation {
		void actOnEvent(Event event);
	}
}
