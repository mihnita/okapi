package net.sf.okapi.filters.openxml;

import static net.sf.okapi.filters.openxml.OpenXMLTestHelpers.textUnitSourceExtractor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiEncryptedDataException;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Miscellaneous OOXML tests.
 */
@RunWith(DataProviderRunner.class)
public class OpenXMLTest {
    private LocaleId locENUS = LocaleId.fromString("en-us");
    private final FileLocation root = FileLocation.fromClass(getClass());

    /**
     * Test to ensure the filter can handle an OOXML package in
     * which the [Content Types].xml document does not appear
     * as the first entry in the ZIP archive.
     * @throws Exception
     */
    @Test
    public void testReorderedZipPackage() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        URL url = root.in("/reordered-zip.docx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(),"UTF-8", locENUS);
        ArrayList<Event> events = getEvents(filter, doc);
        ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
        assertNotNull(tu);
        assertEquals("This is a test.", tu.getSource().getCodedText());
        tu = FilterTestDriver.getTextUnit(events, 2);
        assertEquals("Untitled document.docx", tu.getSource().toString());
    }

    /**
     * Test to ensure that the filter parses the file metadata
     * in order to present PPTX slides for translation in the order
     * they are viewed by the user.
     * @throws Exception
     */
    @Test
    public void testSlideReordering() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        URL url = root.in("/Okapi-325.pptx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        checkTu(events, 1, "Sample Presentation");
        checkTu(events, 2, "This is slide 1");
        checkTu(events, 3, "This is slide 2");
        checkTu(events, 4, "This is slide 3");
    }

    /**
     * Test that we expose document properties for PowerPoint files.
     */
    @Test
    public void testPPTXDocProperties() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(true);
        params.setTranslateComments(false);
        params.setTranslatePowerpointMasters(false);
        params.setTranslatePowerpointNotes(false);
        URL url = root.in("/DocProperties.pptx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(10, events.size());
        // The first 4 are body that we don't care about
        checkTu(events, 5, "Test of OOXML filter");
        checkTu(events, 6, "Okapi OOXML Filter");
        checkTu(events, 7, "Chase Tingley");
        checkTu(events, 8, "Okapi, filtering, OOXML, PPTX");
        checkTu(events, 9, "This is document property comment.");
        checkTu(events, 10, "Filters");
    }

    /**
     * Verify that disabling the option also works.
     */
    @Test
    public void testPPTXIgnoreDocProperties() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateComments(false);
        params.setTranslatePowerpointMasters(false);
        params.setTranslatePowerpointNotes(false);
        URL url = root.in("/DocProperties.pptx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        // Only the 4 body segments are still there
        assertEquals(4, events.size());
    }

    /**
     * Test that PPTX comments are extracted.
     */
    @Test
    public void testPPTXComments() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateComments(true);
        params.setTranslateDocProperties(false);
        params.setTranslatePowerpointMasters(false);
        params.setTranslatePowerpointNotes(false);
        URL url = root.in("/Comments.pptx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(2, events.size());
        assertEquals("Comment on the title slide", events.get(0).getTextUnit().getSource().getCodedText());
        assertEquals("This is a comment on a slide body.", events.get(1).getTextUnit().getSource().getCodedText());
    }

    /**
     * Verify that disabling the option also works.
     */
    @Test
    public void testPPTXIgnoreComments() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateComments(false);
        params.setTranslateDocProperties(false);
        params.setTranslatePowerpointMasters(false);
        params.setTranslatePowerpointNotes(false);
        URL url = root.in("/Comments.pptx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(0, events.size());
    }

    private void dump(List<Event> events) {
        for (Event e : events) {
            if (!(e.getResource() instanceof ITextUnit)) continue;
            System.out.println(e.getTextUnit().getSource().getCodedText());
            System.out.println(e.getTextUnit().getSource().toString());
        }
    }

    @Test
    public void testXLSXOnlyExtractStringsNotNumbers() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(false);
        URL url = root.in("/sample.xlsx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(10, events.size());
        checkTu(events, 1, "Lorem");
        checkTu(events, 2, "ipsum");
        checkTu(events, 3, "dolor");
        checkTu(events, 4, "sit");
        checkTu(events, 5, "amet");
        checkTu(events, 6, "consectetuer");
        checkTu(events, 7, "adipiscing");
        checkTu(events, 8, "elit");
        checkTu(events, 9, "Nunc");
        checkTu(events, 10, "at");
    }

    /**
     * This test now captures the intended ordering behavior of the
     * string table, which is to expose strings in the order they appear
     * to the user, not the order in which they appear in the original
     * string table.
     */
    @Test
    public void testXLSXOrdering() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(false);
        URL url = root.in("/ordering.xlsx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(7, events.size());
        checkTu(events, 1, "Cell A2");
        checkTu(events, 2, "Cell B2");
        checkTu(events, 3, "Cell C3");
        checkTu(events, 4, "Sheet 2, Cell A1");
        checkTu(events, 5, "Sheet2, Cell B2");
        checkTu(events, 6, "Sheet2, Cell A3");
        checkTu(events, 7, "Sheet 3, Cell A1");
    }

    /**
     * Test for Excel column excludes.
     */
    @Test
    public void testXLSXExcludeAllColumns() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateExcelExcludeColumns(true);
        params.tsExcelExcludedColumns = new TreeSet<>();
        params.tsExcelExcludedColumns.add("1A");
        params.tsExcelExcludedColumns.add("1B");
        URL url = root.in("/columns.xlsx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        // Current behavior seems to be exposing them as placeholders
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(0, events.size());
        // Make sure it also works on styled text
        RawDocument rd2 = new RawDocument(root.in("/cell_styling.xlsx").asUri(), "UTF-8", locENUS);
        events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(0, events.size());
        rd2.close();
    }

    /**
     * Test for Excel Sheet Name Translation.
     */
    @Test
    public void testXLSXTranslateSheetNames() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        URL url = root.in("/sheet_names.xlsx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);

        // Test default no translation of sheet names
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(2, events.size());
        checkTu(events, 1, "Text in sheet 1");
        checkTu(events, 2, "Text in sheet 2");

        // Now with set to true to translate sheet names
        params.setTranslateExcelSheetNames(true);
        events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(4, events.size());
        checkTu(events, 3, "Sheet One");
        checkTu(events, 4, "Sheet Two");
    }

    /**
     * Test the case where the same string occurs in both excluded and non-excluded
     * contexts.
     */
    @Test
    public void testPartialExclusionFromColumns() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(false);

        // Parse once with default params, we should get both cells
        URL url = root.in("/shared_string_in_two_columns.xlsx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(2, events.size());
        assertEquals("Danger", events.get(0).getTextUnit().getSource().toString());
        assertEquals("Danger", events.get(1).getTextUnit().getSource().toString());

        // Now with excludes set, we only get one
        params.setTranslateExcelExcludeColumns(true);
        params.tsExcelExcludedColumns = new TreeSet<>();
        params.tsExcelExcludedColumns.add("1A");
        doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(1, events.size());
        assertEquals("Danger", events.get(0).getTextUnit().getSource().toString());
    }

    @Test
    public void testSmartQuotes() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(false);
        URL url = root.in("/smartquotes.docx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(1, events.size());
        assertEquals("“Smart quotes”", events.get(0).getTextUnit().getSource().toString());
    }

    @Test
    public void testTabAsCharacter() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setAddTabAsCharacter(true);
        params.setTranslateDocProperties(false);
        URL url = root.in("/Document-with-tabs.docx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(1, events.size());
        assertEquals("Before\tafter.", events.get(0).getTextUnit().getSource().getCodedText());
    }

    @Test
    public void testTabAsCharacter2() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setAddTabAsCharacter(true);
        params.setTranslateDocProperties(false);
        URL url = root.in("/Document-with-tabs-2.docx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(1, events.size());
        assertEquals("Before\tafter.", events.get(0).getTextUnit().getSource().getCodedText());
    }

    @Test
    public void testTabAsTag() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setAddTabAsCharacter(false);
        params.setTranslateDocProperties(false);
        URL url = root.in("/Document-with-tabs.docx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(1, events.size());
        assertEquals("Beforeafter.", events.get(0).getTextUnit().getSource().getCodedText());
    }

    @Test
    public void testLineBreakAsCharacter() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setAddLineSeparatorCharacter(true);
        params.setTranslateDocProperties(false);
        URL url = root.in("/Document-with-soft-linebreaks.docx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(1, events.size());
        assertEquals("First line\nsecond line.", events.get(0).getTextUnit().getSource().getCodedText());
    }

    @Test
    public void testLineBreakAsTag() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setAddLineSeparatorCharacter(false);
        params.setTranslateDocProperties(false);
        URL url = root.in("/Document-with-soft-linebreaks.docx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(1, events.size());
        assertEquals("First line\uE103\uE110second line.", events.get(0).getTextUnit().getSource().getCodedText());
    }

    @Test
    public void testExcludeAllColors() throws Exception {
        OpenXMLFilter filter = new OpenXMLFilter();
        ConditionalParameters params = filter.getParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateExcelExcludeColors(true);
        params.tsExcelExcludedColors = new TreeSet<>();
        params.tsExcelExcludedColors.add("FF800000"); // dark red
        params.tsExcelExcludedColors.add("FFFF0000"); // red
        params.tsExcelExcludedColors.add("FFFF6600"); // orange
        params.tsExcelExcludedColors.add("FFFFFF00"); // yellow
        params.tsExcelExcludedColors.add("FFCCFFCC"); // light green
        params.tsExcelExcludedColors.add("FF008000"); // green
        params.tsExcelExcludedColors.add("FF3366FF"); // light blue
        params.tsExcelExcludedColors.add("FF0000FF"); // blue
        params.tsExcelExcludedColors.add("FF000090"); // dark blue
        params.tsExcelExcludedColors.add("FF660066"); // purple
        URL url = root.in("/standardcolors.xlsx").asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, doc);
        assertEquals(0, events.size());
    }

    @DataProvider
    public static Object[][] testHiddenTextExtractionProvider() {
        return new Object[][] {
                { //0
                        new String[] {
                                "Here is the [visible] <run1/>message [visible] <run2/>of Jeremiah [visible].",
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
                        },
                        false,
                        new String[] {},
                },
                { //1
                        new String[] {
                                "Here is the [visible] <run1>hidden [direct vanish] </run1>message [visible] <run2>written by the hand [rStyle Haydn] </run2>of Jeremiah [visible].",
                                "Here is the message of Isaiah (with hidden pStyle FranzJosef).",
                                "Here is the message of Daniel (with both direct vanish props).",
                                "Here is the message of Peter, James & John (with simple direct vanish prop).",
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
                        },
                        true,
                        new String[] {},
                },
                { //2
                        new String[] {
                                "Here is the [visible] <run1/>message [visible] <run2/>of Jeremiah [visible].",
                                "Here is the 5th message with direct vanish prop in pPr.",
                                "Here is the 6th message with direct false vanish prop overriding vanish prop in pPr.",
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
                        },
                        false,
                        new String[] {
                                "RunStyle1",
                        },
                },
                { //3
                        new String[] {
                                "Here is the [visible] <run1/>message [visible] <run2/>of Jeremiah [visible].",
                                "Here is the 5th message with direct vanish prop in pPr.",
                                "Here is the 6th message with direct false vanish prop overriding vanish prop in pPr.",
                                "Here is the 7th message with RunStyle1.",
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
                        },
                        false,
                        new String[] {
                                "ParagraphStyle1",
                        },
                },
                { //4
                        new String[] {
                                "Here is the [visible] <run1/>message [visible] <run2/>of Jeremiah [visible].",
                                "Here is the 5th message with direct vanish prop in pPr.",
                                "Here is the 6th message with direct false vanish prop overriding vanish prop in pPr.",
                                "Here is the 7th message with RunStyle1.",
                                "Here is the 8th message with ParagraphStyle1.",
                                "Here is the 9th message with RunStyle2.",
                                "Here is the 10th message with ParagraphStyle2.",
                                "Here is the 11th message with ParagraphStyle2 and RunStyle2.",
                                "Here is the 14th message with RunStyleC.",
                                "Here is the 15th message with ParagraphStyleC.",
                                "Here is the 16th message with ParagraphStyleC and RunStyleC.",
                                "Here is the 17th message with direct false vanish prop overriding vanish prop in Haydn rStyle.",
                                "Here is the 18th message with direct false vanish prop overriding vanish prop in FranzJosef pStyle.",
                                "Here is the 19th message with direct false vanish prop overriding vanish prop in Haydn rStyle in pPr.",
                                "Here is the 20th message with Haydn rStyle in pPr.",
                        },
                        false,
                        new String[] {
                                "RunStyleB",
                                "ParagraphStyleB",
                        },
                },
                { //5
                        new String[] {
                                "Here is the [visible] <run1/>message [visible] <run2/>of Jeremiah [visible].",
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
                        },
                        false,
                        new String[] {
                                "Normal",
                        },
                },
                { //6
                        new String[] {
                                "Here is the [visible] <run1/>message [visible] <run2/>of Jeremiah [visible].",
                                "Here is the 5th message with direct vanish prop in pPr.",
                                "Here is the 6th message with direct false vanish prop overriding vanish prop in pPr.",
                                "Here is the 8th message with ParagraphStyle1.",
                                "Here is the 13th message with ParagraphStyleB.",
                                "Here is the 14th message with RunStyleC.",
                                "Here is the 17th message with direct false vanish prop overriding vanish prop in Haydn rStyle.",
                                "Here is the 18th message with direct false vanish prop overriding vanish prop in FranzJosef pStyle.",
                                "Here is the 19th message with direct false vanish prop overriding vanish prop in Haydn rStyle in pPr.",
                                "Here is the 20th message with Haydn rStyle in pPr.",
                        },
                        false,
                        new String[] {
                                "RunStyle1",
                                "RunStyle2",
                                "RunStyleB",
                                "ParagraphStyle2",
                                "ParagraphStyleA",
                                "ParagraphStyleC",
                        },
                },
        };
    }

    @Test
    @UseDataProvider("testHiddenTextExtractionProvider")
    public void testHiddenTextExtraction(String[] expectedTexts, boolean translateWordHiddenParameter, String[] excludedStyles) throws Exception {
        ConditionalParameters params = new ConditionalParametersBuilder()
                .translateDocProperties(false)
                .translateWordHidden(translateWordHiddenParameter)
                .build();
        params.tsExcludeWordStyles.addAll(Arrays.stream(excludedStyles).collect(Collectors.toSet()));
        List<Event> textUnitEvents = getTextUnitEventsFromFile("/HiddenExcluded.docx", params);
        assertEquals(expectedTexts.length, textUnitEvents.size());
        for (int i = 0; i < textUnitEvents.size(); i++) {
            Assert.assertThat(getStringFromTextUnitEvent(textUnitEvents.get(i)), equalTo(expectedTexts[i]));
        }
    }

    @Test
    public void testDocxStylesInclude() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateComments(false);
        params.setTranslateWordHidden(false);
        params.setTranslateWordInExcludeStyleMode(false);
        params.tsExcludeWordStyles.add("Title");

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/styles.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(1, textUnits.size());
        assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactly(
            "Title"
        );
    }

    @Test
    public void testDocxStylesExclude() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.tsExcludeWordStyles.add("Title");

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/styles.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(5, textUnits.size());
        for (ITextUnit tu : textUnits) {
            assertNotEquals("Title", tu.getSource().toString());
        }
    }

    @Test
    public void testDocxStylesIncludeWithExcludedColor() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateComments(false);
        params.setTranslateWordHidden(false);
        params.setTranslateWordInExcludeStyleMode(false);
        params.setTranslateWordExcludeColors(true);
        params.tsExcludeWordStyles.add("Emphasis");
        params.tsWordExcludedColors.add(Color.RED.getWordColorCode());

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/styles_color.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(1, textUnits.size());
        assertThat(textUnits).extracting(textUnitSourceExtractor()).contains(
            "Emphasis <run1/> style"
        );
        assertThat(textUnits).extracting(textUnitSourceExtractor()).doesNotContain(
            "color"
        );
    }

    @Test
    public void testDocxHighlightsExclude() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateWordInExcludeHighlightMode(true);
        params.tsWordHighlightColors.add(Color.YELLOW.getName());

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/highlights.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(2, textUnits.size());
        assertEquals("Test 4..5..6", textUnits.get(0).toString());
        assertEquals("I am <run1/> in a sentence", textUnits.get(1).toString());
    }

    @Test
    public void testDocxHighlightsExcludeBlock() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateWordInExcludeHighlightMode(true);
        params.tsWordHighlightColors.add(Color.YELLOW.getName());

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/highlights_block.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(2, textUnits.size());
        assertThat(textUnits).extracting(textUnitSourceExtractor()).contains(
            "<run1>Run 1.<run2/></run1>"
        );
        assertThat(textUnits).extracting(textUnitSourceExtractor()).anyMatch(o -> o.toString().contains("Frame"));
    }

    @Test
    public void testDocxHighlightsInclude() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateWordInExcludeHighlightMode(false);
        params.tsWordHighlightColors.add(Color.YELLOW.getName());

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/highlights.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(2, textUnits.size());
        assertEquals("Test 1..2..3", textUnits.get(0).toString());
        assertEquals("<run1>highlighted<run2/></run1>", textUnits.get(1).toString());
    }

    @Test
    public void testDocxHighlightsIncludeInStyle() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateWordInExcludeStyleMode(false);
        params.setTranslateWordInExcludeHighlightMode(true);
        params.tsExcludeWordStyles.add("Heading1");
        params.tsWordHighlightColors.add(Color.YELLOW.getName());

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/highlight_in_style.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(1, textUnits.size());
        assertEquals("T<run1>est</run1> 4..5..6", textUnits.get(0).toString());
    }

    @Test
    public void testDocxHighlightsIncludeColorExcludeInStyle() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateWordInExcludeStyleMode(false);
        params.setTranslateWordInExcludeHighlightMode(true);
        params.setTranslateWordExcludeColors(true);
        params.tsExcludeWordStyles.add("Heading1");
        params.tsWordHighlightColors.add(Color.YELLOW.getName());
        params.tsWordExcludedColors.add(Color.RED.getWordColorCode());

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/highlight_in_style.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(1, textUnits.size());
        assertEquals("T<run1/> 4..5..6", textUnits.get(0).toString());
    }

    @Test
    public void testDocxColorExclude() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateWordExcludeColors(true);
        params.tsWordExcludedColors.add(Color.RED.getWordColorCode()); // Red

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/colors.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(2, textUnits.size());
        assertThat(textUnits).extracting(textUnitSourceExtractor()).doesNotContain(
            "I am red"
        );
    }

    @Test
    public void testDocxColorExcludeBlock() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslateWordExcludeColors(true);
        params.tsWordExcludedColors.add(Color.RED.getWordColorCode());

        OpenXMLFilter filter = new OpenXMLFilter();
        RawDocument doc = new RawDocument(root.in("/highlights_block.docx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertEquals(2, textUnits.size());
        assertThat(textUnits).extracting(textUnitSourceExtractor()).doesNotContain(
            "<run1>Run 1.<run2/></run1>"
        );
        assertThat(textUnits).extracting(textUnitSourceExtractor()).doesNotContain(
            "<run1>Run 2.</run1>"
        );
        assertThat(textUnits).extracting(textUnitSourceExtractor()).doesNotContain(
            "<run1>Framed 1</run1>"
        );
        assertThat(textUnits).extracting(textUnitSourceExtractor()).anyMatch(o -> o.toString().contains("Frame"));

    }

    private String getStringFromTextUnitEvent(Event textUnitEvent) {
        return textUnitEvent.getTextUnit().getSource().toString();
    }

    @Test(expected = OkapiEncryptedDataException.class)
    public void testOkapiEncryptedDataException() throws Exception {
        getTextUnitEventsFromFile("/encrypted/encrypted.docx", new ConditionalParameters());
    }

	@Test
    public void testLibreOfficeDocWithAbsolutePartPaths() throws Exception {
        getTextUnitEventsFromFile("/pokemon.xlsx", new ConditionalParameters());
    }

    @Test
    public void extractsExternalHyperlinks() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setExtractExternalHyperlinks(true);

        List<Event> events = getTextUnitEventsFromFile("/external_hyperlink.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "https://bitbucket.org/okapiframework/okapi/issues?status=new&amp;status=open",
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlik",
            "https://bitbucket.org/okapiframework/okapi/issues?status=new&amp;status=open",
            "This contains a <hyperlink1>h1yperlink</hyperlink1> and <hyperlink2>another one</hyperlink2> to its schema.",
            "Here’s a hyperlink that <hyperlink1><run2>contains <run3>styled</run3> markup</run2></hyperlink1><run4>2</run4>.",
            "Chase Tingley"
        );

        events = getTextUnitEventsFromFile("/external_hyperlink.pptx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "A <run1>hyperlink</run1> with a query and <run2>another one</run2> to its schema.",
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlik",
            "https://bitbucket.org/okapiframework/okapi/issues?status=new&amp;status=open",
            "Click to edit Master title style",
            "Click to edit Master text styles",
            "Second level",
            "Third level",
            "Fourth level",
            "Fifth level",
            "Click to edit Master title style",
            "Click to edit Master subtitle style",
            "Click to edit Master title style",
            "Click to edit Master text styles",
            "Second level",
            "Third level",
            "Fourth level",
            "Fifth level",
            "PowerPoint Presentation",
            "Chase Tingley"
        );
    }

    @Test
    public void extractsNestedContentInTheExpectedOrder() throws Exception {
        List<Event> events = getTextUnitEventsFromFile("/798.docx", new ConditionalParameters());
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Paragraph 1.",
            "Надпись 2",
            "Textbox 6.",
            "Поле 5",
            "Textbox 5.",
            "Поле 4",
            "Textbox 4.",
            "Поле 3",
            "Textbox 3.",
            "Paragraph 2.",
            "Поле 2",
            "Textbox 2.",
            "Поле 1",
            "Textbox 1.",
            "Paragraph 3.",
            "User"
        );
    }

    @Test
    public void extractsComplexFieldsWithRefinedBoundaries() throws Exception {
        List<Event> events = getTextUnitEventsFromFile("/830-1.docx", new ConditionalParameters());
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Paragraph <tags2/>1<tags3/>.<tags4/>",
            "Paragraph 2.",
            "User"
        );

        events = getTextUnitEventsFromFile("/830-2.docx", new ConditionalParameters());
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Field character: separate<tags2/>",
            "Some content.",
            "User"
        );
    }

    @Test
    public void extractsComplexFieldsWithRefinedBoundariesFromMinifiedDocument() throws Exception {
        List<Event> events = getTextUnitEventsFromFile("/830-6.docx", new ConditionalParameters());
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Text 1.",
            "<tags1/>Hyperlink 1<tags2/>",
            "Text 2.",
            "User",
            "comments"
        );
    }

    @Test
    public void extractsNestedComplexFieldsWithRefinedBoundaries() throws Exception {
        // default conditional parameters
        List<Event> events = getTextUnitEventsFromFile("/830-3.docx", new ConditionalParameters());
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Field character: <tags2/>separate with nested (<tags3/>) complex field<tags4/>",
            "Some content.",
            "User",
            "comments"
        );

        events = getTextUnitEventsFromFile("/830-4.docx", new ConditionalParameters());
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Nested f<tags2/>ield character: <tags3/>hyperlink<tags4/>.<run5> </run5>",
            "Some content.",
            "User",
            "Comments across some paragraphs"
        );

        events = getTextUnitEventsFromFile("/830-5.docx", new ConditionalParameters());
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Nested f<tags2/>ield character: <tags3/>hyperlink<tags4/><run5> </run5>",
            "Some content.",
            "User",
            "Comments across some paragraphs"
        );

        // tuned conditional parameters
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.tsComplexFieldDefinitionsToExtract.add("COMMENTS");
        conditionalParameters.tsComplexFieldDefinitionsToExtract.add("TITLE");

        events = getTextUnitEventsFromFile("/830-3.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Field character: <tags2/>separate with nested (<tags3/>COMMENTS<tags4/>) complex field<tags5/>",
            "Some content.",
            "User",
            "comments"
        );

        events = getTextUnitEventsFromFile("/830-4.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Comments with<tags2/> <tags3/>Nested f<tags4/>ield character: <tags5/>hyperlink<tags6/>  <tags7/>across<tags8/> some<tags9/> <tags10/>paragraphs<tags11/>.<run12> </run12>",
            "Some content.",
            "User",
            "Comments across some paragraphs"
        );

        events = getTextUnitEventsFromFile("/830-5.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<tags1/>Comments with<tags2/> <tags3/>Nested f<tags4/>ield character: <tags5/>hyperlink<tags6/>  <tags7/>across<tags8/> some<tags9/> <tags10/>paragraphs<tags11/>.<tags12/>",
            " ",
            "Some content.",
            "User",
            "Comments across some paragraphs"
        );
    }

    @Test
    public void extractsStructuralDocumentTagsAsRunContainers() throws Exception {
        List<Event> events = getTextUnitEventsFromFile("/834.docx", new ConditionalParameters());
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Text 1<run1/>.",
            "Text 2<run1/>.",
            " <hyperlink1>An <sdt2><run3>[<run4>sdt</run4> 1<sdt5><run6>[</run6><run7>sdt 2</run7>]</sdt5> inside]</run3></sdt2>  footnote</hyperlink1><run8> 1.</run8>",
            " A footnote 2 with <sdt1>sdt 1</sdt1> and <sdt2>sdt 2</sdt2>.",
            "Место для ввода текста.",
            "User"
        );
    }

    @Test
    public void extractsNoneReorderedNotesAndComments() throws Exception {
        final List<Event> events = getTextUnitEventsFromFile("/835.pptx", new ConditionalParameters());
        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Slide 1. Title 1.",
            "Slide 1. Subtitle 1.",
            "Slide 2.",
            "Slide 2. Text.",
            "Box message 1.",
            "Box message 2.",
            "Slide 2. Note 1.",
            "Slide 2. Note 2.",
            "Образец заголовка",
            "Образец текста",
            "Второй уровень",
            "Третий уровень",
            "Четвертый уровень",
            "Пятый уровень",
            "Slide 1. Note 1.",
            "Slide 1. Note 2.",
            "Образец заголовка",
            "Образец текста",
            "Второй уровень",
            "Третий уровень",
            "Четвертый уровень",
            "Пятый уровень",
            "Образец заголовка",
            "Образец подзаголовка",
            "Slide 2. Comment 1.",
            "Slide 2. Comment 2.",
            "Slide 2. Comment 3.",
            "Slide 2. Comment 4.",
            "Образец текста",
            "Второй уровень",
            "Третий уровень",
            "Четвертый уровень",
            "Пятый уровень",
            "Slide 1. Subtitle1. Comment 1.",
            "Slide 1. Title 1. Comment 2.",
            "Slide 1. Subtitle 1. Comment 3.",
            "Slide 1. Title 1.",
            "User"
        );
    }

    @Test
    public void extractsReorderedNotesAndComments() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setReorderPowerpointNotesAndComments(true);
        final List<Event> events = getTextUnitEventsFromFile("/835.pptx", conditionalParameters);
        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Slide 1. Title 1.",
            "Slide 1. Subtitle 1.",
            "Slide 1. Note 1.",
            "Slide 1. Note 2.",
            "Slide 1. Subtitle1. Comment 1.",
            "Slide 1. Title 1. Comment 2.",
            "Slide 1. Subtitle 1. Comment 3.",
            "Slide 2.",
            "Slide 2. Text.",
            "Box message 1.",
            "Box message 2.",
            "Slide 2. Note 1.",
            "Slide 2. Note 2.",
            "Slide 2. Comment 1.",
            "Slide 2. Comment 2.",
            "Slide 2. Comment 3.",
            "Slide 2. Comment 4.",
            "Образец заголовка",
            "Образец текста",
            "Второй уровень",
            "Третий уровень",
            "Четвертый уровень",
            "Пятый уровень",
            "Образец заголовка",
            "Образец текста",
            "Второй уровень",
            "Третий уровень",
            "Четвертый уровень",
            "Пятый уровень",
            "Образец заголовка",
            "Образец подзаголовка",
            "Образец текста",
            "Второй уровень",
            "Третий уровень",
            "Четвертый уровень",
            "Пятый уровень",
            "Slide 1. Title 1.",
            "User"
        );
    }

    @Test
    public void extractsReorderedNotesAndCommentsWithNoCommentsPart() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setReorderPowerpointNotesAndComments(true);
        final List<Event> events = getTextUnitEventsFromFile("/835-2.pptx", conditionalParameters);
        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Hello world",
            "Chase Tingley",
            "PowerPoint Presentation",
            "Click to move the slide",
            "Click to edit the notes format",
            "<header>",
            "<date/time>",
            "<footer>"
        );
    }

    @Test
    public void extractsMovedInlineContent() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        final List<Event> events = getTextUnitEventsFromFile("/843-1.docx", conditionalParameters);
        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Moved text. Text 1. ",
            "User"
        );
    }

    @Test
    public void extractsMovedParagraphContent() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        final List<Event> events = getTextUnitEventsFromFile("/843-2.docx", conditionalParameters);
        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Moved paragraph.",
            "Paragraph 1.",
            "User"
        );
    }

    @Test
    public void extractsMovedContent() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/843-31.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Moved paragraph and inserted text.",
            "Plus another paragraph ().",
            "Paragraph 1.",
            "User"
        );

        events = getTextUnitEventsFromFile("/843-32.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Moved paragraph and inserted text.",
            "Plus another paragraph ().",
            "Paragraph 1.",
            "User"
        );

        events = getTextUnitEventsFromFile("/843-33.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Moved paragraph and inserted text.",
            "Plus another paragraph ().",
            "Paragraph 1.",
            "User"
        );

        events = getTextUnitEventsFromFile("/843-34.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Moved paragraph and inserted text.",
            "Plus another paragraph ().",
            "Paragraph 1.",
            "User"
        );
    }

    @Test
    public void extractsInStrictMode() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/858.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Saving as OOXML Strict in MS Office 2013.",
            "User"
        );

        events = getTextUnitEventsFromFile("/859.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Saving as OOXML Strict in MS Office 2013.<tags1/> New text for tracking changes.",
            "0",
            "1",
            "2",
            "3",
            "<hyperlink1>Hyperlink</hyperlink1>",
            "",
            "Text Box 2",
            "Text box 1.",
            "User"
        );

        events = getTextUnitEventsFromFile("/859.pptx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Title 1",
            "Subtitle 1",
            "Title 2",
            "Text",
            "Another text",
            "Click to edit Master title style",
            "Click to edit Master text styles",
            "Second level",
            "Third level",
            "Fourth level",
            "Fifth level",
            "Click to edit Master title style",
            "Click to edit Master subtitle style",
            "Click to edit Master title style",
            "Click to edit Master text styles",
            "Second level",
            "Third level",
            "Fourth level",
            "Fifth level",
            "Title 1",
            "User"
        );

        events = getTextUnitEventsFromFile("/859.xlsx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Cell 1A",
            "Cell 2B",
            "<run1>Cell</run1> 3C",
            "User"
        );
    }

    @Test
    public void extractsWithOptimisedWordStyles() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/853-all-common.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Run 1.Run 2.Run 3.",
            "User"
        );

        events = getTextUnitEventsFromFile("/884.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Run 12pt.",
            "<run1>Run 13pt.</run1><run2>Run 12pt.</run2>Run default.",
            "Run default.<run1>Run 12pt.</run1><run2>Run 13pt.</run2>",
            "<run1>Run 12pt.</run1>Run default.<run2>Run 13pt.</run2>",
            "Run 13pt.<run1>Run 12pt.</run1><run2>Run 14pt.</run2>",
            "User"
        );
    }

    @Test
    public void extractsWithRunFontsHintRespect() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/851.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "<run1>East-Asian and special symbols 1 (国</run1><run2>际</run2>Ω <run3>§¶</run3>∑商。).",
            "East-Asian and special symbols 2 (国际<run1>Ω</run1><run2> §¶</run2>∑商。).",
            "User"
        );
    }

    @Test
    public void extractsWithImplicitFormatting() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/887.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Run 13pt.<run1>Run 12pt.</run1><run2>Run 14pt.</run2>",
            "User"
        );
    }

    @Test
    public void extractsWithAcceptedDeletedParagraphMarkRevision() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/847-1.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            " Added after deletion.",
            "User"
        );

        events = getTextUnitEventsFromFile("/847-2.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "P1",
            ". Added after deletion.<run1>P3</run1>",
            "User"
        );

        events = getTextUnitEventsFromFile("/847-3.docx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "P1.<run1><tags2/>I<tags3/>ns<tags4/>erted<tags5/> revision<tags6/>.The last run.</run1>",
            "Denis Konovalyenko"
        );
    }

    @Test
    public void insertedAndDeletedTableRowRevisionsAccepted() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/848.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Table 1.",
            "Table 2.",
            "00",
            "01",
            "02",
            "10",
            "An inserted row.",
            "20",
            "21",
            "22",
            "Table 3.",
            "User"
        );
    }

    @Test
    public void extractsUnmergedRunsWithDifferentRunFonts() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/888.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "\uF0B10.000（草坪或地面）",
            "Administrator"
        );
    }

    @Test
    public void extractsRunsWithMinifiedRunProperties() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/948-1.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Run1 Run3",
            "Fully-minified1",
            "Semi-minified1",
            "Non-minified1"
        );

        events = getTextUnitEventsFromFile("/948-2.pptx", conditionalParameters);
        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "A title with <run1>formatting</run1>",
            "A sub title with <run1>formatting</run1>",
            "A <run1>formatted</run1> note <run2>1</run2>.",
            "Click to edit Master title style",
            "Click to edit Master subtitle style",
            "Click to edit Master title style",
            "Click to edit Master text styles",
            "Second level",
            "Third level",
            "Fourth level",
            "Fifth level",
            "Click to edit Master text styles",
            "Second level",
            "Third level",
            "Fourth level",
            "Fifth level",
            "A title",
            "User"        );
    }

    @Test
    public void extractsRunsFollowedByEmptyParagraph() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/956.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "The 1st para.",
            "Run1 with <tags1/>a hyperlink<tags2/> and a missed extraction.",
            "User",
            "© <run1/>   <sdt2><run3/></sdt2>",
            "© <run1/> <sdt2><run3/></sdt2>"
        );
    }

    @Test
    public void extractsTextEncodingOkapiMarkersPptx() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/OkapiMarkers.pptx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
                " Content",
                "Click to edit Master title style",
                "Click to edit Master text styles",
                "Second level",
                "Third level",
                "Fourth level",
                "Fifth level",
                "Click to edit Master title style",
                "Click to edit Master subtitle style"
        );
    }

    @Test
    public void extractsTextEncodingOkapiMarkerDocx() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/OkapiMarkers.docx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
                "<run1>\uE101\uE102\uE103 </run1>Content"
        );
    }


    @Test
    public void extractsTextEncodingOkapiMarkerXlsx() throws Exception {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        List<Event> events = getTextUnitEventsFromFile("/OkapiMarkers.xlsx", conditionalParameters);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
                " Content"
        );
    }


    private List<Event> getTextUnitEventsFromFile(String path, ConditionalParameters params) throws Exception{
        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(params);
        URL url = root.in(path).asUrl();
        RawDocument doc = new RawDocument(url.toURI(), "UTF-8", locENUS);
        return FilterTestDriver.getTextUnitEvents(filter, doc);
    }

    private void checkTu(ArrayList<Event> events, int i, String gold) {
        ITextUnit tu = FilterTestDriver.getTextUnit(events, i);
        assertNotNull(tu);
        assertEquals(gold, tu.getSource().toString());
    }

    private ArrayList<Event> getEvents(OpenXMLFilter filter, RawDocument doc) {
        ArrayList<Event> list = new ArrayList<>();
        filter.open(doc, false);
        while (filter.hasNext()) {
            Event event = filter.next();
            list.add(event);
        }
        filter.close();
        return list;
    }
}
