/*
 * =============================================================================
 *   Copyright (C) 2010-2017 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =============================================================================
 */

package net.sf.okapi.filters.idml;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextFragment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(DataProviderRunner.class)
public class IDMLFilterTest {

    private IDMLFilter filter;
    private FileLocation root;
    private LocaleId locEN = LocaleId.fromString("en");

    @Before
    public void setUp() {
        filter = new IDMLFilter();
        root = FileLocation.fromClass(this.getClass());
    }

    @Test
    public void testDefaultInfo() {
        assertNotNull(filter.getParameters());
        assertNotNull(filter.getName());

        List<FilterConfiguration> filterConfigurations = filter.getConfigurations();
        assertNotNull(filterConfigurations);
        assertNotEquals(0, filterConfigurations.size());
    }

    @Test
    public void testSimpleEntry() {
        List<ITextUnit> textUnits = getTextUnits("/helloworld-1.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(0), "Hello World!");
    }

    @Test
    public void testSimpleEntry2() {
        List<ITextUnit> textUnits = getTextUnits("/Test00.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(0), "\uE101\uE110Hello \uE101\uE111World!\uE102\uE112\uE102\uE113Hello again \uE101\uE114World!\uE102\uE115");
        assertEquals("<content-1>Hello <content-2>World!</content-2></content-1>Hello again <content-3>World!</content-3>", textUnits.get(0).toString());
        assertEquals("Hello World!Hello again World!", TextFragment.getText(textUnits.get(0).getSource().getCodedText()));
    }

    @Test
    public void testWhitespaces() {
        List<ITextUnit> textUnits = getTextUnits("/tabsAndWhitespaces.idml");

        assertNotNull(textUnits);
        assertEquals(14, textUnits.size());

        assertTrue(textUnits.get(0).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(0), "Hello World.");

        assertTrue(textUnits.get(1).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(1), "Hello\tWorld with a Tab.");

        assertTrue(textUnits.get(2).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(2), "Hello \tWorld with a Tab and a white space.");

        assertTrue(textUnits.get(3).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(3), " Hello World\t.");

        assertTrue(textUnits.get(4).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(4), "Hello World.");

        assertTrue(textUnits.get(5).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(5), "Hello      World.\uE103\uE110");

        assertTrue(textUnits.get(6).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(6), "\uE103\uE110");

        assertTrue(textUnits.get(7).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(7), " Hello World\t.");

        assertTrue(textUnits.get(8).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(8), "HelloWorldwithout.");

        assertTrue(textUnits.get(9).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(9), "Hello \tWorld with a Tab and a white space.");

        assertTrue(textUnits.get(10).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(10), "m-space here.");

        assertTrue(textUnits.get(11).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(11), "n-space here.");

        assertTrue(textUnits.get(12).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(12), "another m-space\uE103\uE110here.");

        assertTrue(textUnits.get(13).preserveWhitespaces());
        assertThatTextUnitCodedTextEquals(textUnits.get(13), "another one here.");
    }

    @Test
    public void testNewline() {
        List<ITextUnit> textUnits = getTextUnits("/newline.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(0), "32");
        assertThatTextUnitCodedTextEquals(textUnits.get(1), "Hello World");
    }

    @Test
    public void testStartDocument() {
        assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(filter,
                new InputDocument(root.in("/Test01.idml").toString(), null),
                StandardCharsets.UTF_8.name(), locEN, locEN));
    }

    @Test
    public void testObjectsWithoutPathPointsAndText() {
        List<ITextUnit> textUnits = getTextUnits("/618-objects-without-path-points-and-text.idml");

        assertEquals(0, textUnits.size());
    }

    @Test
    public void testAnchoredFrameWithoutPathPoints() {
        List<ITextUnit> textUnits = getTextUnits("/618-anchored-frame-without-path-points.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(4), "Anchored");
    }

    @Test
    public void testDocumentWithoutPathPoints() {
        List<ITextUnit> textUnits = getTextUnits("/618-MBE3.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(0), "Fashion Industry In Colombia");
        assertThatTextUnitCodedTextEquals(textUnits.get(1), "\uE103\uE110");
    }

    @Test
    public void testSkipDiscretionaryHyphens() throws Exception {
        filter.getParameters().setSkipDiscretionaryHyphens(true);

        List<ITextUnit> textUnits = getTextUnits("/Bindestrich.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(0), "Ich bin ein bedingter Bindestrich.");
    }

    @Test
    public void testChangeTracking() {
        List<ITextUnit> textUnits = getTextUnits("/08-conditional-text-and-tracked-changes.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(0), "Conditional Text Sample");
        assertThatTextUnitCodedTextEquals(textUnits.get(1), "New text.");
        assertThatTextUnitCodedTextEquals(textUnits.get(2), "This simple document demonstrates controlling conditional text visibility.");
        assertThatTextUnitCodedTextEquals(textUnits.get(3), "Print Only");
        assertThatTextUnitCodedTextEquals(textUnits.get(4), "\tThis text is print only!");
        assertThatTextUnitCodedTextEquals(textUnits.get(5), "Web Only");
        assertThatTextUnitCodedTextEquals(textUnits.get(6), "\tThis text is Web only!");
        assertThatTextUnitCodedTextEquals(textUnits.get(7), "BREAKING NEWS!");
        assertThatTextUnitCodedTextEquals(textUnits.get(8), "Print and Breaking News");

        textUnits = getTextUnits("/change-tracking-3.idml");

        assertThatTextUnitCodedTextEquals(textUnits.get(0), "Text 1 ");
        assertThatTextUnitCodedTextEquals(textUnits.get(1), "Text 2");
        assertThatTextUnitCodedTextEquals(textUnits.get(2), "Text 3");
        assertThatTextUnitCodedTextEquals(textUnits.get(3), "Text 4");
        assertThatTextUnitCodedTextEquals(textUnits.get(4), "Text 5 \uE101\uE110\uE103\uE111\uE102\uE112Text 6");
        assertThatTextUnitCodedTextEquals(textUnits.get(5), "Text 7");
        assertThatTextUnitCodedTextEquals(textUnits.get(6), "Text 10");
        assertThatTextUnitCodedTextEquals(textUnits.get(7), "Text 11");
        assertThatTextUnitCodedTextEquals(textUnits.get(8), "Text 13");
        assertThatTextUnitCodedTextEquals(textUnits.get(9), "Text 14");
        assertThatTextUnitCodedTextEquals(textUnits.get(10), "Text 15");
        assertThatTextUnitCodedTextEquals(textUnits.get(11), "Text 16");
        assertThatTextUnitCodedTextEquals(textUnits.get(12), "Text 17");
        assertThatTextUnitCodedTextEquals(textUnits.get(13), "Text 18");
    }

    @Test
    public void extractsBreaksInline() {
        filter.getParameters().setExtractBreaksInline(true);
        final List<ITextUnit> textUnits = getTextUnits("/07-paragraph-breaks.idml");
        assertThatTextUnitCodedTextEquals(textUnits.get(0), "1st paragraph.\uE103\uE1102nd paragraph.\uE103\uE1113rd paragraph.\uE103\uE1124th paragraph.\t\uE103\uE1135th paragraph.\uE103\uE1146th paragraph.");
    }

    @Test
    public void doesNotMergeTagsThatDifferByKerning() {
        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-kerning.idml").get(0),
            "Kerning\uE101\uE110-25\uE102\uE111\uE101\uE112-10\uE102\uE113\uE101\uE114-5\uE102\uE115\uE101\uE116-2\uE102\uE117+0\uE101\uE118+2\uE102\uE119\uE101\uE11A+5\uE102\uE11B\uE101\uE11C+10\uE102\uE11D\uE101\uE11E+25\uE102\uE11F"
        );
    }

    @Test
    public void mergesTagsThatDifferByKerningWithEmptyIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterKerning(true);

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-kerning.idml").get(0),
            "Kerning-25-10-5-2+0+2+5+10+25"
        );
    }

    @Test
    public void mergesTagsThatDifferByKerningWithMinIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterKerning(true);
        filter.getParameters().setCharacterKerningMinIgnoranceThreshold("-2");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-kerning.idml").get(0),
            "Kerning\uE101\uE110-25\uE102\uE111\uE101\uE112-10\uE102\uE113\uE101\uE114-5\uE102\uE115-2+0+2+5+10+25"
        );
    }

    @Test
    public void mergesTagsThatDifferByKerningWithMaxIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterKerning(true);
        filter.getParameters().setCharacterKerningMaxIgnoranceThreshold("5");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-kerning.idml").get(0),
            "Kerning-25-10-5-2+0+2+5\uE101\uE110+10\uE102\uE111\uE101\uE112+25\uE102\uE113"
        );
    }

    @Test
    public void mergesTagsThatDifferByKerningWithMinAndMaxIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterKerning(true);
        filter.getParameters().setCharacterKerningMinIgnoranceThreshold("-2");
        filter.getParameters().setCharacterKerningMaxIgnoranceThreshold("5");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-kerning.idml").get(0),
            "Kerning\uE101\uE110-25\uE102\uE111\uE101\uE112-10\uE102\uE113\uE101\uE114-5\uE102\uE115-2+0+2+5\uE101\uE116+10\uE102\uE117\uE101\uE118+25\uE102\uE119"
        );
    }

    @Test
    public void doesNotMergeTagsThatDifferByTracking() {
        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-tracking.idml").get(0),
            "Tracking\uE101\uE110-25\uE102\uE111\uE101\uE112-10\uE102\uE113\uE101\uE114-5\uE102\uE115+0\uE101\uE116+5\uE102\uE117\uE101\uE118+10\uE102\uE119\uE101\uE11A+25\uE102\uE11B"
        );
    }

    @Test
    public void mergesTagsThatDifferByTrackingWithEmptyIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterTracking(true);

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-tracking.idml").get(0),
            "Tracking-25-10-5+0+5+10+25"
        );
    }

    @Test
    public void mergesTagsThatDifferByTrackingWithMinIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterTracking(true);
        filter.getParameters().setCharacterTrackingMinIgnoranceThreshold("-5");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-tracking.idml").get(0),
            "Tracking\uE101\uE110-25\uE102\uE111\uE101\uE112-10\uE102\uE113-5+0+5+10+25"
        );
    }

    @Test
    public void mergesTagsThatDifferByTrackingWithMaxIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterTracking(true);
        filter.getParameters().setCharacterTrackingMaxIgnoranceThreshold("5");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-tracking.idml").get(0),
            "Tracking-25-10-5+0+5\uE101\uE110+10\uE102\uE111\uE101\uE112+25\uE102\uE113"
        );
    }

    @Test
    public void mergesTagsThatDifferByTrackingWithMinAndMaxIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterTracking(true);
        filter.getParameters().setCharacterTrackingMinIgnoranceThreshold("-5");
        filter.getParameters().setCharacterTrackingMaxIgnoranceThreshold("5");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-tracking.idml").get(0),
            "Tracking\uE101\uE110-25\uE102\uE111\uE101\uE112-10\uE102\uE113-5+0+5\uE101\uE114+10\uE102\uE115\uE101\uE116+25\uE102\uE117"
        );
    }

    @Test
    public void doesNotMergeTagsThatDifferByLeading() {
        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-leading.idml").get(0),
            "Leading0pt\uE101\uE1102pt\uE102\uE111\uE101\uE1125pt\uE102\uE113\uE101\uE1147pt\uE102\uE115\uE101\uE11610pt\uE102\uE117"
        );
    }

    @Test
    public void mergesTagsThatDifferByLeadingWithoutIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterLeading(true);

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-leading.idml").get(0),
            "Leading0pt2pt5pt7pt10pt"
        );
    }

    @Test
    public void mergesTagsThatDifferByLeadingWithMinIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterLeading(true);
        filter.getParameters().setCharacterLeadingMinIgnoranceThreshold("4.2");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-leading.idml").get(0),
            "Leading0pt\uE101\uE1102pt\uE102\uE111\uE101\uE1125pt7pt10pt\uE102\uE113"
        );
    }

    @Test
    public void mergesTagsThatDifferByLeadingWithMaxIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterLeading(true);
        filter.getParameters().setCharacterLeadingMaxIgnoranceThreshold("7");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-leading.idml").get(0),
            "Leading0pt2pt5pt7pt\uE101\uE11010pt\uE102\uE111"
        );
    }

    @Test
    public void mergesTagsThatDifferByLeadingWithMinAndMaxIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterLeading(true);
        filter.getParameters().setCharacterLeadingMinIgnoranceThreshold("4.2");
        filter.getParameters().setCharacterLeadingMaxIgnoranceThreshold("7");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-leading.idml").get(0),
            "Leading0pt\uE101\uE1102pt\uE102\uE111\uE101\uE1125pt7pt\uE102\uE113\uE101\uE11410pt\uE102\uE115"
        );
    }

    @Test
    public void doesNotMergeTagsThatDifferByBaselineShift() {
        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-baseline-shift.idml").get(0),
            "\uE101\uE110BaselineShift\uE102\uE111\uE101\uE112-5pt\uE102\uE113\uE101\uE114-2pt\uE102\uE1150pt\uE101\uE1162pt\uE102\uE117\uE101\uE1185pt\uE102\uE119"
        );
    }

    @Test
    public void mergesTagsThatDifferByBaselineShiftWithoutIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterBaselineShift(true);

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-baseline-shift.idml").get(0),
            "BaselineShift-5pt-2pt0pt2pt5pt"
        );
    }

    @Test
    public void mergesTagsThatDifferByBaselineShiftWithMinIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterBaselineShift(true);
        filter.getParameters().setCharacterBaselineShiftMinIgnoranceThreshold("-4.2");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-baseline-shift.idml").get(0),
            "\uE101\uE110BaselineShift\uE102\uE111\uE101\uE112-5pt\uE102\uE113-2pt0pt2pt5pt"
        );
    }

    @Test
    public void mergesTagsThatDifferByBaselineShiftWithMaxIgnoranceThreshold() {
        filter.getParameters().setIgnoreCharacterBaselineShift(true);
        filter.getParameters().setCharacterBaselineShiftMaxIgnoranceThreshold("4.2");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-baseline-shift.idml").get(0),
            "BaselineShift-5pt-2pt0pt2pt\uE101\uE1105pt\uE102\uE111"
        );
    }

    @Test
    public void mergesTagsThatDifferByBaselineShiftWithMinAndMaxIgnoranceThresholds() {
        filter.getParameters().setIgnoreCharacterBaselineShift(true);
        filter.getParameters().setCharacterBaselineShiftMinIgnoranceThreshold("-4.2");
        filter.getParameters().setCharacterBaselineShiftMaxIgnoranceThreshold("4.3");

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/756-character-baseline-shift.idml").get(0),
            "\uE101\uE110BaselineShift\uE102\uE111\uE101\uE112-5pt\uE102\uE113-2pt0pt2pt\uE101\uE1145pt\uE102\uE115"
        );
    }

    @Test
    public void doesNotMergeTagsThatDifferByKerningMethod() {
        assertThatTextUnitCodedTextEquals(
            getTextUnits("/777-character-kerning-method.idml").get(0),
            "\uE101\uE110KerningMethodOptical\uE102\uE111KerningMethodMetrics\uE101\uE112KerningMethod0\uE102\uE113Kerning\uE101\uE1145\uE102\uE115\uE101\uE11610\uE102\uE117"
        );
    }

    @Test
    public void mergesTagsThatDifferByKerningMethod() {
        filter.getParameters().setIgnoreCharacterKerning(true);

        assertThatTextUnitCodedTextEquals(
            getTextUnits("/777-character-kerning-method.idml").get(0),
            "KerningMethodOpticalKerningMethodMetricsKerningMethod0Kerning510"
        );
    }

    @Test
    public void doesNotMergeTagsThatDifferByKerningInReferencesAndXmlStructures() {
        final List<ITextUnit> textUnits = getTextUnits("/779-reference-and-tag-styles.idml");
        assertThatTextUnitCodedTextEquals(
            textUnits.get(0),
            "\uE101\uE110\uE101\uE111hyperlink\uE102\uE112 \uE101\uE113c\uE102\uE114\uE101\uE115o\uE102\uE116\uE101\uE117n\uE102\uE118\uE101\uE119t\uE102\uE11A\uE101\uE11Be\uE102\uE11C\uE101\uE11Dn\uE102\uE11E\uE101\uE11Ft\uE102\uE120\uE102\uE121"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(2),
            "\uE103\uE110\t\uE101\uE111Footnote\uE102\uE112 \uE101\uE113c\uE102\uE114\uE101\uE115o\uE102\uE116\uE101\uE117n\uE102\uE118\uE101\uE119t\uE102\uE11A\uE101\uE11Be\uE102\uE11C\uE101\uE11Dn\uE102\uE11Et"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(4),
            "\uE101\uE110Cell\uE102\uE111 \uE101\uE112t\uE102\uE113\uE101\uE114e\uE102\uE115\uE101\uE116x\uE102\uE117\uE101\uE118t\uE102\uE119"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(5),
            "\uE101\uE110tagged\uE102\uE111 \uE101\uE112c\uE102\uE113\uE101\uE114o\uE102\uE115\uE101\uE116n\uE102\uE117\uE101\uE118t\uE102\uE119\uE101\uE11Ae\uE102\uE11B\uE101\uE11Cn\uE102\uE11Dt"
        );
    }

    @Test
    public void mergesTagsThatDifferByKerningInReferencesAndXmlStructures() {
        filter.getParameters().setIgnoreCharacterKerning(true);

        final List<ITextUnit> textUnits = getTextUnits("/779-reference-and-tag-styles.idml");
        assertThatTextUnitCodedTextEquals(
            textUnits.get(0),
            "\uE101\uE110hyperlink content\uE102\uE111"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(2),
            "\uE103\uE110\tFootnote content"
        );
        assertThatTextUnitCodedTextEquals(textUnits.get(4), "Cell text");
        assertThatTextUnitCodedTextEquals(textUnits.get(5),"tagged content");
    }

    @Test
    public void extractsWithLeastAvailableStyleFormattingBaselined() {
        final List<ITextUnit> textUnits = getTextUnits("/923-baselined-formatting.idml");
        assertEquals(9, textUnits.size());
        assertThatTextUnitCodedTextEquals(
            textUnits.get(0),
            "Defaults: {FontStyle: Regular, PointSize:12pt}"
        );
        assertThatTextUnitCodedTextEquals(textUnits.get(1), "Regular11");
        assertThatTextUnitCodedTextEquals(textUnits.get(2), "Italic11");
        assertThatTextUnitCodedTextEquals(
            textUnits.get(3),
            "\uE101\uE110Regular10 \uE102\uE111\uE101\uE112Regular11 \uE102\uE113Regular12"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(4),
            "Regular12 \uE101\uE110Regular11 \uE102\uE111\uE101\uE112Regular10\uE102\uE113"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(5),
            "Bold12 \uE101\uE110Bold11 \uE102\uE111Bold12"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(6),
            "\uE101\uE110Italic11 \uE102\uE111Italic12 \uE101\uE112Italic11\uE102\uE113"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(7),
            "Regular12 \uE101\uE110Bold12 \uE102\uE111\uE101\uE112BoldItalic12 \uE102\uE113\uE101\uE114Bold12 \uE102\uE115Regular12"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(8),
            "Regular12 \uE101\uE110Italic12 \uE102\uE111\uE101\uE112BoldItalic12 \uE102\uE113\uE101\uE114Italic12 \uE102\uE115Regular12"
        );
    }

    @Test
    public void pasteboardItemsWithoutAnchorPointsPositionedCorrectly() {
        final List<ITextUnit> originalTextUnits = getTextUnits("/05-complex-ordering.idml");
        final List<ITextUnit> actualTextUnits = getTextUnits("/935-complex-ordering-without-anchor-points.idml");
        assertEquals(originalTextUnits.size(), actualTextUnits.size());
        assertArrayEquals(
            originalTextUnits.subList(0, 5).stream()
                .map(tu -> tu.getSource().getCodedText())
                .toArray(),
            actualTextUnits.subList(0, 5).stream()
                .map(tu -> tu.getSource().getCodedText())
                .toArray()
        );
        assertThatTextUnitCodedTextEquals(originalTextUnits.get(6), "Text on the right of spread (-45 degrees rotation).");
        assertThatTextUnitCodedTextEquals(actualTextUnits.get(6), "Text on the left side of spread (45 degrees rotation).");
        assertThatTextUnitCodedTextEquals(originalTextUnits.get(7), "Text on the text frame 5.");
        assertThatTextUnitCodedTextEquals(actualTextUnits.get(7), "Text on the text frame path 1.");
        assertThatTextUnitCodedTextEquals(originalTextUnits.get(8), "Text on the left side of spread (45 degrees rotation).");
        assertThatTextUnitCodedTextEquals(actualTextUnits.get(8), "Text on the right of spread (-45 degrees rotation).");
        assertThatTextUnitCodedTextEquals(originalTextUnits.get(9), "Text on the text frame path 1.");
        assertThatTextUnitCodedTextEquals(actualTextUnits.get(9), "Text on the text frame 5.");
        assertArrayEquals(
            originalTextUnits.subList(10, 13).stream()
                .map(tu -> tu.getSource().getCodedText())
                .toArray(),
            actualTextUnits.subList(10, 13).stream()
                .map(tu -> tu.getSource().getCodedText())
                .toArray()
        );
    }

    @Test
    public void hiddenPasteboardItemsExtracted() {
        List<ITextUnit> textUnits = getTextUnits("/1016.idml");
        assertEquals(533, textUnits.size());
        this.filter.getParameters().setExtractHiddenPasteboardItems(true);
        textUnits = getTextUnits("/1016.idml");
        assertEquals(545, textUnits.size());
        assertThatTextUnitCodedTextEquals(
            textUnits.get(212),
            "Lighting your grill"
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(213),
            "Is it the very first time? Perform a Burn-off "
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(214),
            "Remove warming rack and run the main burners on high for \uE103\uE11030 minutes. It is normal for the grill to emit an odor the first \uE103\uE111time it is lit. This odor is caused by the “burn-off” of internal \uE103\uE112paints and lubricants used in the manufacturing process and \uE103\uE113will not occur again. "
        );
        assertThatTextUnitCodedTextEquals(
            textUnits.get(215),
            "Side Burner Lighting (if equipped) "
        );
    }

    private List<ITextUnit> getTextUnits(String testFileName) {

        return FilterTestDriver.filterTextUnits(
                FilterTestDriver.getEvents(
                        filter,
                        new RawDocument(root.in(testFileName).asUri(), StandardCharsets.UTF_8.name(), locEN),
                        null
                )
        );
    }

    private void assertThatTextUnitCodedTextEquals(ITextUnit textUnit, String expectedText) {
        assertNotNull(textUnit);
        assertEquals(expectedText, textUnit.getSource().getFirstContent().getCodedText());
    }

    @DataProvider
    public static Object[][] testDoubleExtractionProvider() {
        return new Object[][]{
                {"Test00.idml", "okf_idml@ExtractAll.fprm"},
                {"Test01.idml", "okf_idml@ExtractAll.fprm"},
                {"Test02.idml", "okf_idml@ExtractAll.fprm"},
                {"Test03.idml", "okf_idml@ExtractAll.fprm"},

                {"helloworld-1.idml", "okf_idml@ExtractAll.fprm"},
                {"ConditionalText.idml", "okf_idml@ExtractAll.fprm"},

                {"testWithSpecialChars.idml", "okf_idml@ExtractAll.fprm"},

                {"TextPathTest01.idml", "okf_idml@ExtractAll.fprm"},
                {"TextPathTest02.idml", "okf_idml@ExtractAll.fprm"},
                {"TextPathTest03.idml", "okf_idml@ExtractAll.fprm"},
                {"TextPathTest04.idml", "okf_idml@ExtractAll.fprm"},

                {"idmltest.idml", "okf_idml@ExtractAll.fprm"},
                {"idmltest.idml", null},

                {"01-pages-with-text-frames.idml", null},
                {"01-pages-with-text-frames-2.idml", null},
                {"01-pages-with-text-frames-3.idml", null},
                {"01-pages-with-text-frames-4.idml", null},
                {"01-pages-with-text-frames-5.idml", null},
                {"01-pages-with-text-frames-6.idml", null},

                {"02-island-spread-and-threaded-text-frames.idml", null},
                {"03-hyperlink-and-table-content.idml", null},
                {"04-complex-formatting.idml", null},
                {"05-complex-ordering.idml", null},

                {"06-hello-world-12.idml", null},
                {"06-hello-world-13.idml", null},
                {"06-hello-world-14.idml", null},

                {"07-paragraph-breaks.idml", null},

                {"08-conditional-text-and-tracked-changes.idml", null},
                {"change-tracking-3.idml"},
                {"08-direct-story-content.idml", null},

                {"09-footnotes.idml", null},
                {"10-tables.idml", null},

                {"11-xml-structures.idml", "okf_idml@ExtractAll.fprm"},
                {"11-xml-structures.idml", null},

                {"618-objects-without-path-points-and-text.idml", null},
                {"618-anchored-frame-without-path-points.idml", null},
                {"618-MBE3.idml", null},
                {"Bindestrich.idml", null},
                {"756-character-kerning.idml", "okf_idml@IgnoreAll.fprm"},
                {"756-character-tracking.idml", "okf_idml@IgnoreAll.fprm"},
                {"756-character-leading.idml", "okf_idml@IgnoreAll.fprm"},
                {"756-character-baseline-shift.idml", "okf_idml@IgnoreAll.fprm"},
                {"777-character-kerning-method.idml", "okf_idml@IgnoreAll.fprm"},
                {"779-reference-and-tag-styles.idml", "okf_idml@IgnoreAll.fprm"},
                {"923-baselined-formatting.idml", null},
                {"926.idml", "okf_idml@chained-font-mappings.fprm"},
        };
    }

    @Test
    @UseDataProvider("testDoubleExtractionProvider")
    public void testDoubleExtraction(String inputDocumentName, String parametersFileName) {
        List<InputDocument> list = new ArrayList<>();
        list.add(new InputDocument(root.in("/" + inputDocumentName).toString(), parametersFileName));

        RoundTripComparison rtc = new RoundTripComparison(false); // Do not compare skeleton
        assertTrue(rtc.executeCompare(filter, list, StandardCharsets.UTF_8.name(), locEN, locEN,
                                      "output"));
    }
}
