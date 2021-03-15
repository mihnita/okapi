package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.INameable;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.plaintext.PlainTextFilter;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SubfilteringTest extends AbstractOpenXMLRoundtripTest {

    private final FilterConfigurationMapper filterConfigurationMapper;
    private final LocaleId sourceLocale;
    private final LocaleId targetLocale;

    private OpenXMLFilter filter;
    private FileLocation root;

    public SubfilteringTest() {
        this.filterConfigurationMapper = new FilterConfigurationMapper();
        this.filterConfigurationMapper.addConfigurations(PlainTextFilter.class.getName());
        this.filterConfigurationMapper.addConfigurations(HtmlFilter.class.getName());

        this.sourceLocale = LocaleId.ENGLISH;
        this.targetLocale = LocaleId.ENGLISH;
    }

    @Before
    public void setUp() {
        this.filter = new OpenXMLFilter();
        this.filter.setFilterConfigurationMapper(filterConfigurationMapper);
        this.root = FileLocation.fromClass(this.getClass());
    }

    @Test
    public void extractsWithoutSubfiltering() {
        final List<Event> events = FilterTestDriver.getEvents(
                filter,
                new RawDocument(
                        root.in("/subfiltering/780-excel-with-hard-line-breaks-and-html-tags.xlsx").asUri(),
                        OpenXMLFilter.ENCODING.name(),
                        sourceLocale
                ),
                new ConditionalParameters()
        );

        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
                "A1 cel",
                "B1 cell",
                "C1 cell",
                "A2 with CRLF\nin the middle",
                "B2 with CR\nin the middle",
                "C2 with LF\nin the middle",
                "A3 with <b>bold</b> CRLF\nand <i>italic</i>",
                "B3 with <b>bold</b> CR\nand <i>italic</i>",
                "C3 with <b>bold</b> LF\nand <i>italic</i>",
                "A4 <run1>styled</run1><run2> with <b>bold</b> CRLF\nand <i>italic</i></run2>",
                "B4 <run1>styled</run1><run2> with <b>bold</b> CR\nand <i>italic</i></run2>",
                "C4 <run1>styled</run1><run2> with <b>bold</b> LF\nand <i>italic</i></run2>",
                "User"
        );

        Assertions.assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) INameable::getName)
            .containsExactly(
                "Лист1!A1",
                "Лист1!B1",
                "Лист1!C1",
                "Лист1!A2",
                "Лист1!B2",
                "Лист1!C2",
                "Лист1!A3",
                "Лист1!B3",
                "Лист1!C3",
                "Лист1!A4",
                "Лист1!B4",
                "Лист1!C4",
                null);
    }

    @Test
    public void extractsWithPlainTextSubfiltering() {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setSubfilter("okf_plaintext");

        final List<Event> events = FilterTestDriver.getEvents(
                filter,
                new RawDocument(
                        root.in("/subfiltering/780-excel-with-hard-line-breaks-and-html-tags.xlsx").asUri(),
                        OpenXMLFilter.ENCODING.name(),
                        sourceLocale
                ),
                conditionalParameters
        );

        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
                "A1 cel",
                "B1 cell",
                "C1 cell",
                "A2 with CRLF",
                "in the middle",
                "B2 with CR",
                "in the middle",
                "C2 with LF",
                "in the middle",
                "A3 with <b>bold</b> CRLF",
                "and <i>italic</i>",
                "B3 with <b>bold</b> CR",
                "and <i>italic</i>",
                "C3 with <b>bold</b> LF",
                "and <i>italic</i>",
                "A4 <run1>styled</run1><run2> with <b>bold</b> CRLF\nand <i>italic</i></run2>",
                "B4 <run1>styled</run1><run2> with <b>bold</b> CR\nand <i>italic</i></run2>",
                "C4 <run1>styled</run1><run2> with <b>bold</b> LF\nand <i>italic</i></run2>",
                "User"
        );

        Assertions.assertThat(textUnits).extracting((Extractor<ITextUnit, Object>) INameable::getName)
            .containsExactly(
                "Лист1!A1_1",
                "Лист1!B1_1",
                "Лист1!C1_1",
                "Лист1!A2_1",
                "Лист1!A2_2",
                "Лист1!B2_1",
                "Лист1!B2_2",
                "Лист1!C2_1",
                "Лист1!C2_2",
                "Лист1!A3_1",
                "Лист1!A3_2",
                "Лист1!B3_1",
                "Лист1!B3_2",
                "Лист1!C3_1",
                "Лист1!C3_2",
                "Лист1!A4",
                "Лист1!B4",
                "Лист1!C4",
                null);
    }

    @Test
    public void extractsWithHtmlSubfiltering() {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setSubfilter("okf_html");

        final List<Event> events = FilterTestDriver.getEvents(
                filter,
                new RawDocument(
                        root.in("/subfiltering/780-excel-with-hard-line-breaks-and-html-tags.xlsx").asUri(),
                        OpenXMLFilter.ENCODING.name(),
                        sourceLocale
                ),
                conditionalParameters
        );

        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
                "A1 cel",
                "B1 cell",
                "C1 cell",
                "A2 with CRLF in the middle",
                "B2 with CR in the middle",
                "C2 with LF in the middle",
                "A3 with <b>bold</b> CRLF and <i>italic</i>",
                "B3 with <b>bold</b> CR and <i>italic</i>",
                "C3 with <b>bold</b> LF and <i>italic</i>",
                "A4 <run1>styled</run1><run2> with <b>bold</b> CRLF\nand <i>italic</i></run2>",
                "B4 <run1>styled</run1><run2> with <b>bold</b> CR\nand <i>italic</i></run2>",
                "C4 <run1>styled</run1><run2> with <b>bold</b> LF\nand <i>italic</i></run2>",
                "User"
        );
        Assertions.assertThat(textUnits.get(6).getSource().getCodedText())
                .isEqualTo("A3 with \uE101\uE110bold\uE102\uE111 CRLF and \uE101\uE112italic\uE102\uE113");
        Assertions.assertThat(textUnits.get(9).getSource().getCodedText())
                .isEqualTo("A4 \uE101\uE110styled\uE102\uE111\uE101\uE112 with <b>bold</b> CRLF\nand <i>italic</i>\uE102\uE113");
    }

    @Test
    public void roundtripsWithPlainTextSubfiltering() {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setSubfilter("okf_plaintext");

        runOneTest("subfiltering/780-excel-with-hard-line-breaks-and-html-tags.xlsx", true, false,
            conditionalParameters, "subfiltering/plaintext/",
            sourceLocale, targetLocale, filterConfigurationMapper);
        assertTrue(this.allGood);
    }

    @Test
    public void roundtripsWithHtmlSubfiltering() {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setSubfilter("okf_html");

        runOneTest("subfiltering/780-excel-with-hard-line-breaks-and-html-tags.xlsx", true, false,
            conditionalParameters, "subfiltering/html/",
            sourceLocale, targetLocale, filterConfigurationMapper);
        assertTrue(this.allGood);
    }
}
