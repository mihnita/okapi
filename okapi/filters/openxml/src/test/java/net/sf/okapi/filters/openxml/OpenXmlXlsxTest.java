package net.sf.okapi.filters.openxml;

import static net.sf.okapi.filters.openxml.CodePeekTranslator.locENUS;
import static net.sf.okapi.filters.openxml.OpenXMLTestHelpers.textUnitSourceExtractor;
import static org.assertj.core.api.Assertions.assertThat;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.INameable;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jpmaas
 * @since 17.08.2017
 */
@RunWith(JUnit4.class)
public class OpenXmlXlsxTest {

	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

    @Test
    public void testTextFields() throws Exception {
        ConditionalParameters parameters = new ConditionalParameters();
        parameters.setTranslateExcelDrawings(true);
        parameters.setTranslateDocProperties(false);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(parameters);

        RawDocument doc = new RawDocument(root.in("/textfield.xlsx").asUri(), "UTF-8", locENUS);

        ArrayList<Event> actual = getEvents(filter, doc);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(actual);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> input.getSource().toString()).containsExactly(
                "Hallo Welt!",
                "Ich bin ein Textfeld!");
    }

    @Test
    public void testExcelWorksheetTransUnitProperty() throws Exception {
        ConditionalParameters parameters = new ConditionalParameters();
        parameters.setTranslateExcelDrawings(true);
        parameters.setTranslateDocProperties(false);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(parameters);

        RawDocument doc = new RawDocument(root.in("/textfield.xlsx").asUri(), "UTF-8", locENUS);

        ArrayList<Event> actual = getEvents(filter, doc);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(actual);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> input.hasProperty(
            ExcelWorksheetTransUnitProperty.CELL_REFERENCE.getKeyName())).containsExactly(
            true,
            false);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> String.valueOf(input.getProperty(
            ExcelWorksheetTransUnitProperty.CELL_REFERENCE.getKeyName()))).containsExactly(
            "A1",
            "null");
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> String.valueOf(input.getProperty(
            ExcelWorksheetTransUnitProperty.SHEET_NAME.getKeyName()))).containsExactly(
            "Tabelle1",
            "null");
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) INameable::getName).containsExactly(
            "Tabelle1!A1",
            null);
    }

    @Test
    public void testSmartArt() throws Exception {
        ConditionalParameters parameters = new ConditionalParameters();
        parameters.setTranslateExcelDiagramData(true);
        parameters.setTranslateDocProperties(false);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(parameters);

        RawDocument doc = new RawDocument(root.in("/smartart.xlsx").asUri(), "UTF-8", locENUS);

        ArrayList<Event> actual = getEvents(filter, doc);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(actual);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> input.getSource().toString()).containsExactly(
            "Hallo Welt!", "Ich", "bin", "ein", "Smart", "Art"
        );
    }

    @Test
    public void testSmartArtHidden() throws Exception {
        ConditionalParameters parameters = new ConditionalParameters();
        parameters.setTranslateExcelDiagramData(true);
        parameters.setTranslateDocProperties(false);
        parameters.setTranslateExcelHidden(false);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(parameters);

        RawDocument doc = new RawDocument(root.in("/SmartArt3Sheets.xlsx").asUri(), "UTF-8", locENUS);

        ArrayList<Event> actual = getEvents(filter, doc);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(actual);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> input.getSource().toString()).containsExactly(
            "Zelle 1", "Zelle 3", "Smart Art 1", "Smart Art 3"
        );
    }

    @Test
    public void testTextFieldsHidden() throws Exception {
        ConditionalParameters parameters = new ConditionalParameters();
        parameters.setTranslateExcelDrawings(true);
        parameters.setTranslateDocProperties(false);
        parameters.setTranslateExcelHidden(false);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(parameters);

        RawDocument doc = new RawDocument(root.in("/Textfeld3Sheets.xlsx").asUri(), "UTF-8", locENUS);

        ArrayList<Event> actual = getEvents(filter, doc);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(actual);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> input.getSource().toString()).containsExactly(
                "Zelle 1",
                "Zelle 3",
                "Textfeld 1",
                "Textfeld 3");
    }

    @Test
    public void testSheetNamesHiddenExclude() throws Exception {
        ConditionalParameters parameters = new ConditionalParameters();
        parameters.setTranslateExcelSheetNames(true);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(parameters);

        RawDocument doc = new RawDocument(root.in("/SheetNameHidden.xlsx").asUri(), "UTF-8", locENUS);

        ArrayList<Event> actual = getEvents(filter, doc);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(actual);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> input.getSource().toString()).containsExactly(
                "Cell Visible",
                "Sheet Visible");
    }

    @Test
    public void testSheetNamesHiddenInclude() throws Exception {
        ConditionalParameters parameters = new ConditionalParameters();
        parameters.setTranslateExcelSheetNames(true);
        parameters.setTranslateExcelHidden(true);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(parameters);

        RawDocument doc = new RawDocument(root.in("/SheetNameHidden.xlsx").asUri(), "UTF-8", locENUS);

        ArrayList<Event> actual = getEvents(filter, doc);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(actual);
        assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) input -> input.getSource().toString()).containsExactly(
                "Cell Visible",
                "Cell Hidden",
                "Sheet Visible",
                "Sheet Hidden"
        );
    }

    @Test
    public void testFormattings() throws Exception {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        params.setTranslatePowerpointMasters(false);

        OpenXMLFilter filter = new OpenXMLFilter();
        filter.setParameters(params);

        RawDocument doc = new RawDocument(root.in("/Formattings.xlsx").asUri(), "UTF-8", locENUS);
        ArrayList<Event> events = getEvents(filter, doc);

        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
                "This is a <run1>bold formatting</run1>",
                "This is an <run1>italics formatting</run1>",
                "This is an <run1>underlined formatting</run1>",
                "This is a hyperlink"
        );

        assertThat(
                textUnits.get(0).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(2).extracting("type").containsExactly(
                "x-bold;",
                "x-bold;"
        );
        assertThat(
                textUnits.get(1).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(2).extracting("type").containsExactly(
                "x-italic;",
                "x-italic;"
        );
        assertThat(
                textUnits.get(2).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(2).extracting("type").containsExactly(
                "x-underline:single;",
                "x-underline:single;"
        );
        assertThat(
                textUnits.get(3).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(0);
    }

    @SuppressWarnings("Duplicates")
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
