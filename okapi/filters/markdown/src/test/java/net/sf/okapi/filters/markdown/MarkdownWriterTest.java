/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.FilterUtil;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import net.sf.okapi.filters.yaml.YamlFilter;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@RunWith(JUnit4.class)
public class MarkdownWriterTest {

    private static final Locale TARGET_LOCALE = Locale.FRENCH;
    private static final LocaleId TARGET_LOCALE_ID = new LocaleId(TARGET_LOCALE);

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void writeDocumentParts() throws Exception {
        Path path = tempFolder.newFile().toPath();
        String snippet = "[Link](<https://www.google.com>)\n\n";

        try (MarkdownFilter filter = new MarkdownFilter();
                IFilterWriter writer = filter.createFilterWriter();
                OutputStream os = Files.newOutputStream(path);
                RawDocument rawDoc = new RawDocument(snippet, null, null)) {

            writer.setOutput(os);
            writer.setOptions(TARGET_LOCALE_ID, StandardCharsets.UTF_8.name());

            filter.open(rawDoc);

            while (filter.hasNext()) {
                writer.handleEvent(filter.next());
            }
        }

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(snippet, outputData);
    }

    @Test
    public void writeTextUnitsAndDocumentPartsText() throws Exception {
        Path path = tempFolder.newFile().toPath();
        String snippet = "First text unit\n\nSecond text unit";

        try (MarkdownFilter filter = new MarkdownFilter();
                IFilterWriter writer = filter.createFilterWriter();
                OutputStream os = Files.newOutputStream(path);
                RawDocument rawDoc = new RawDocument(snippet, null, null)) {

            writer.setOutput(os);
            writer.setOptions(TARGET_LOCALE_ID, StandardCharsets.UTF_8.name());

            filter.open(rawDoc);

            while (filter.hasNext()) {
                Event event = filter.next();
                FilterUtil.logDebugEvent( event, "", LOGGER );
                if (event.isTextUnit()) {
                    ITextUnit tu = event.getTextUnit();
                    TextContainer tc = tu.createTarget(TARGET_LOCALE_ID, false, IResource.COPY_ALL);
                    TextFragment tf = tc.getFirstContent();
                    tf.setCodedText(tf.getCodedText().toUpperCase(TARGET_LOCALE));
                }
                writer.handleEvent(event);
            }
        }

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(snippet.toUpperCase(TARGET_LOCALE), outputData);
    }

    @Test
    public void writeTextUnitsAndDocumentPartsHtml() throws Exception {
        Path path = tempFolder.newFile().toPath();
        String snippet = "This contains <span>some inline</span> HTML\n\n";
        String expected = "THIS CONTAINS <span>SOME INLINE</span> HTML\n\n";

        try (MarkdownFilter filter = new MarkdownFilter();
                IFilterWriter writer = filter.createFilterWriter();
                OutputStream os = Files.newOutputStream(path);
                RawDocument rawDoc = new RawDocument(snippet, null, null)) {

            writer.setOutput(os);
            writer.setOptions(TARGET_LOCALE_ID, StandardCharsets.UTF_8.name());

            filter.open(rawDoc);

            while (filter.hasNext()) {
                Event event = filter.next();
                if (event.isTextUnit()) {
                    ITextUnit tu = event.getTextUnit();
                    TextContainer tc = tu.createTarget(TARGET_LOCALE_ID, false, IResource.COPY_ALL);
                    TextFragment tf = tc.getFirstContent();
                    tf.setCodedText(tf.getCodedText().toUpperCase(TARGET_LOCALE));
                }
                writer.handleEvent(event);
            }
        }

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(expected, outputData);
    }

    @Test
    public void writeTextUnitsAndDocumentPartsList() throws Exception {
        Path path = tempFolder.newFile().toPath();
        String snippet = "This is a list:\n\n" + "* First\nelement\n\n"
                + "* Second element\n\n" + "End of the list";

        try (MarkdownFilter filter = new MarkdownFilter();
                IFilterWriter writer = filter.createFilterWriter();
                OutputStream os = Files.newOutputStream(path);
                RawDocument rawDoc = new RawDocument(snippet, null, null)) {

            writer.setOutput(os);
            writer.setOptions(TARGET_LOCALE_ID, StandardCharsets.UTF_8.name());

            filter.open(rawDoc);

            while (filter.hasNext()) {
                Event event = filter.next();
                if (event.isTextUnit()) {
                    ITextUnit tu = event.getTextUnit();
                    TextContainer tc = tu.createTarget(TARGET_LOCALE_ID, false, IResource.COPY_ALL);
                    TextFragment tf = tc.getFirstContent();
                    tf.setCodedText(tf.getCodedText().toUpperCase(TARGET_LOCALE));
                }
                writer.handleEvent(event);
            }
        }

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(snippet.toUpperCase(TARGET_LOCALE).replace(" ", ""), outputData.replace(" ", "")); // There's extra spaces that won't change the meaning.
        //assertEquals(snippet.toUpperCase(TARGET_LOCALE), outputData);
    }


    @Test
    public void writeTextUnitsAndDocumentPartsHardLineBreak() throws Exception {
        Path path = tempFolder.newFile().toPath();
        String snippet = "First text unit    \nstill part of the text unit.";

        try (MarkdownFilter filter = new MarkdownFilter();
             IFilterWriter writer = filter.createFilterWriter();
             OutputStream os = Files.newOutputStream(path);
             RawDocument rawDoc = new RawDocument(snippet, null, null)) {

            writer.setOutput(os);
            writer.setOptions(TARGET_LOCALE_ID, StandardCharsets.UTF_8.name());

            filter.open(rawDoc);

            while (filter.hasNext()) {
                Event event = filter.next();
                FilterUtil.logDebugEvent( event, "", LOGGER );
                if (event.isTextUnit()) {
                    ITextUnit tu = event.getTextUnit();
                    TextContainer tc = tu.createTarget(TARGET_LOCALE_ID, false, IResource.COPY_ALL);
                    TextFragment tf = tc.getFirstContent();
                    tf.setCodedText(tf.getCodedText().toUpperCase(TARGET_LOCALE));
                }
                writer.handleEvent(event);
            }
        }

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(snippet.toUpperCase(TARGET_LOCALE), outputData);
    }

    @Test
    public void testCommonMarkRoundTrip() throws Exception {
        testRoundTrip("commonmark_original.md");
    }

    @Test
    public void testCommonMarkChangedOutput() throws Exception {
        testChangedOutput("commonmark_original.md", "commonmark_changed.md");
    }

    @Test
    public void testListsRoundTrip() throws Exception {
        testRoundTripIgnoringEmptyLines("lists_original.md");
        //testRoundTrip("lists_original.md");
    }

    @Test
    public void testBQInListItemRoundTrip() throws Exception {
        testRoundTrip("block-quote-in-list-item.md");
    }

    @Test
    public void testListChangedOutput() throws Exception {
       testChangedOutput("lists_original.md", "lists_changed.md");
    }

    @Test
    public void testNestedListWithBlankLines() throws Exception {
        testRoundTrip("nested_list_with_blank_lines.md");
    }

    @Test
    public void testTable1RoundTrip() throws Exception {
        testRoundTrip("table1_original.md");
    }

    @Test
    public void testTable1ChangedOutput() throws Exception {
        testChangedOutput("table1_original.md", "table1_changed.md");
    }

    @Test
    public void testTable2RoundTrip() throws Exception {
        testRoundTrip("table2_original.md");
    }

    @Test
    public void testTable2ChangedOutput() throws Exception {
        testChangedOutput("table2_original.md", "table2_changed.md");
    }

    @Test
    public void testMinimalMathRoundTrip() throws Exception {
        testRoundTripIgnoringWhitespacesNearHtmlTagsAndQuotes("min_math_original.md");
    }

    @Test
    public void testComplexMathRoundTrip() throws Exception {
        testRoundTripIgnoringWhitespacesNearHtmlTagsAndQuotes("regressing_test_single_page.md");
    }

    @Test
    public void testImgWithAltRoundTrip() throws Exception {
        testRoundTrip("img_w_alt_attr_original.md");
    }

    @Test
    public void testHtmlListRoundTrip() throws Exception {
        testRoundTrip("html_list_original.md");
    }

    @Test
    public void testHtmlListChangedOutput() throws Exception {
        testChangedOutput("html_list_original.md", "html_list_changed.md");
    }

    @Test
    public void testHtmlTable1RoundTrip() throws Exception {
        testRoundTrip/*IgnoringEmptyLines*/("html_table1_original.md");
    }

    @Test
    public void testQuotedPara() throws Exception { // Okapi issue #686
        testRoundTripIgnoringEmptyLines("quoted-para.md");
    }

    @Test
    public void testQuotedList() throws Exception { // Okapi issue #686
        testRoundTripIgnoringEmptyLines("quoted-list.md");
    }

    @Test
    public void testUlInTable() throws Exception { // Okapi issue #685
        testRoundTrip("ul-in-table.md");
    }

    @Test
    public void testTbodyTdInTable() throws Exception { // Okapi issue #685?
        testRoundTripIgnoringWhitespacesNearHtmlTagsAndQuotes("DirectShape.md" );
    }

    @Test
    public void testHtmlBlockWithEmptyLines() throws Exception { // Okapi issue #685?
        testRoundTripIgnoringWhitespacesNearHtmlTagsAndQuotes("html-table-w-empty-lines.md" );
    }

    @Test
    public void testHeadingsAfterList() throws Exception { // Okapi issue #708
        testRoundTrip("heading-after-list.md");
    }

    @Test @Ignore // TODO: Fix #687
    public void testSpaces() throws Exception { // Okapi #687
        testRoundTrip("space-test.md");
    }

    @Test
    public void testReferencedLinkAndImage() throws Exception { // Okapi #711
        testChangedOutput("ref-links.md", "ref-links-uppercased.md");
    }

    @Test
    public void testLinkAndImage() throws Exception { // Okapi #711 fix regression
        testChangedOutput("direct-links.md", "direct-links-uppercased.md");
    }

    @Test
    public void testDeadLinkRef() throws Exception { // Okapi #711 special case where a reference is defined but not used
        testChangedOutput("dead-ref-link.md", "dead-ref-link-uppercased.md");
    }

    @Test
    public void testTooManyTUs() throws Exception { // Verify that a side effect of fixing #716 is gone.
        // This does not test if #716 is fixed. It tests a side effect that
        // was observed during the development is no longer there.
        testRoundTrip("multiple-segments.md");
    }

    @Test
    public void testQuotesAfterHtmlInTableCell() throws Exception { // Verify another side effect fixing #716 is gone.
        // The side effect was:
        // When there is <ul><li>text</li></ul> in a Markdown table cell, any single quote (') after
        // the cell is written back as the HTML numeric entity, &#39; when merged.
        testRoundTrip("quotes-after-html-in-table.md");
    }

    @Test
    public void testCdata() throws Exception { // Test if a CData section is properly handled.
        testChangedOutput("html-cdata-sample.md", "html-cdata-sample-uppercased.md");
    }

    @Test @Ignore // Fails on Linux. See https://bitbucket.org/okapiframework/okapi/issues/816
    public void testCdataCRLF() throws Exception { // Test if a CData section is properly handled when the file has CR/LF endings.
        testChangedOutput("html-cdata-sample_crlf.md", "html-cdata-sample-uppercased.md");
    }

    @Test
    public void testImageWoAlt() throws Exception { // Image without alt text, i.e. ![](url/to/image.jpg)
        testRoundTrip("image-wo-alt.md");
    }

    @Test
    public void testComplexFrontMatterIgnoredByDefault() throws Exception { // Complex Front Matter
        testRoundTrip("complex_frontmatter.md");
    }

    @Test @Ignore // Fails on Windows. See https://bitbucket.org/okapiframework/okapi/issues/816
    public void testComplexFrontMatterIncludedDefaultFilter() throws Exception { // Complex Front Matter
        testRoundTripWithHeaders("complex_frontmatter.md");
    }

    private void testRoundTrip(String originalFile) throws Exception {
        testRoundTripWithParameters(originalFile, new Parameters());
    }

    private void testRoundTripWithParameters(String originalFile, Parameters parameters) throws Exception {
        try (MarkdownFilter filter = new MarkdownFilter()) {
            filter.setParameters(parameters);
            String contents = getFileContents(originalFile);
            ISkeletonWriter sw = filter.getFilterWriter().getSkeletonWriter();
            List<Event> events = FilterTestDriver.getEvents(filter, contents, null, TARGET_LOCALE_ID);
            FilterUtil.logDebugEvents(events, LOGGER);

            EncoderManager em = new EncoderManager();
            em.setAllKnownMappings();
            assertEquals(contents, FilterTestDriver.generateOutput(events, TARGET_LOCALE_ID, sw, em));
        }
    }

    private void testRoundTripWithHeaders(String originalFile) throws Exception {
        try (MarkdownFilter filter = new MarkdownFilter()) {
            FilterConfigurationMapper mapper = new FilterConfigurationMapper();
            mapper.addConfigurations(MarkdownFilter.class.getName());
            filter.setFilterConfigurationMapper(mapper);
            filter.getParameters().setTranslateHeaderMetadata(true);
            String contents = getFileContents(originalFile);
            ISkeletonWriter sw = filter.getFilterWriter().getSkeletonWriter();

            List<Event> events = FilterTestDriver.getEvents(filter, contents, null, TARGET_LOCALE_ID);
            FilterUtil.logDebugEvents(events, LOGGER);

            EncoderManager em = new EncoderManager();
            em.setAllKnownMappings();
            assertEquals(contents, FilterTestDriver.generateOutput(events, TARGET_LOCALE_ID, sw, em));
        }
    }

    @Test
    public void testCustomConfigurationFromString() throws Exception
    {
        FilterConfigurationMapper mapper = new FilterConfigurationMapper();
        mapper.addConfigurations(MarkdownFilter.class.getName());
        FilterConfiguration defaultFilterConfiguration = mapper.getConfiguration("okf_markdown");
        assertNotNull(defaultFilterConfiguration);

        FilterConfiguration config = mapper.getConfiguration("okf_markdown");
        IParameters parameters = mapper.getParameters(config);
        String paramsAsString = Util.normalizeNewlines(getFileContents("okf_markdown@custom_markdown.fprm"));
        parameters.fromString(paramsAsString);
        assertEquals("IParamaters", paramsAsString, parameters.toString());
    }

    @Test
    public void testRoundTripWithHeadersCustomConfiguration() throws Exception
    {
        try (MarkdownFilter filter = new MarkdownFilter()) {
            FilterConfigurationMapper mapper = new FilterConfigurationMapper();

            IParameters markdownParameters = new Parameters();
            String markdownParamsAsString = Util.normalizeNewlines(getFileContents("okf_markdown@custom_markdown.fprm"));
            markdownParameters.fromString(markdownParamsAsString);

            IParameters yamlParameters = new net.sf.okapi.filters.yaml.Parameters();
            String yamlParamsAsString = Util.normalizeNewlines(getFileContents("okf_yaml@custom_markdown.fprm"));
            yamlParameters.fromString(yamlParamsAsString);

            mapper.addConfiguration(
                new FilterConfiguration("okf_markdown@custom_markdown",
                    MimeTypeMapper.MARKDOWN_MIME_TYPE,
                    MarkdownFilter.class.getName(),
                    "Markdown", "", null, markdownParameters, ".md"));

            mapper.addConfiguration(
                new FilterConfiguration("okf_yaml@custom_markdown",
                    MimeTypeMapper.YAML_MIME_TYPE,
                    YamlFilter.class.getName(),
                    "Yaml", "", null, yamlParameters, ".yml"));

            filter.setFilterConfigurationMapper(mapper);

            String contents = getFileContents("complex_frontmatter.md");
            ISkeletonWriter sw = filter.getFilterWriter().getSkeletonWriter();

            List<Event> events = FilterTestDriver.getEvents(filter, contents, null, TARGET_LOCALE_ID);
            FilterUtil.logDebugEvents(events, LOGGER);

            EncoderManager em = new EncoderManager();
            em.setAllKnownMappings();
            assertEquals(contents, FilterTestDriver.generateOutput(events, TARGET_LOCALE_ID, sw, em));
        }
    }

    @Test // Extra spaces are added at the beginning but that won't change the meaning.
    public void testHardLineBreak() throws Exception { // From Issue #695
        testRoundTrip("bullet-para.md");
    }

    @Test
    public void testHardLineBreakVarious() throws Exception {
        testRoundTrip("hard-line-break-various.md");
    }

    @Test
    public void testHardLineBreakWithCRLF() throws Exception {
        testRoundTrip("hard-line-break_crlf.md"); // Windows/DOS style CRLF ending
    }

    @Test
    public void testHardLineBreakBetweenInlineMarkupPair() throws Exception {
        testRoundTrip("hard-line-break-inline.md");
    }

    @Test // Issue #820 related (but not exactly the reported issue)
    public void testIndentedCodeBlockWithCRLF() throws Exception {
        testRoundTrip("indented-code-block-simple_crlf.md");
    }

    @Test
    public void testQuotedCodeBlocks() throws Exception { // Issue #704
        testRoundTrip("quoted-code-blocks.md");
    }

    @Test
    public void roundTripsCodesAndCodeBlocks() throws Exception {
        final Parameters parameters = new Parameters();
        parameters.setTranslateCodeBlocks(false);
        parameters.setTranslateInlineCodeBlocks(false);
        testRoundTripWithParameters("code_and_codeblock_tests.md", parameters);
        testRoundTrip("code_and_codeblock_tests.md");
    }

    @Test
    public void roundTripsEmphasis() throws Exception { // Issue #704
        testRoundTrip("emphasis.md");
    }

    /*
     * A variant of testRoundTrip() that ignores empty lines.
     * The filter sometimes remove or insert empty lines.
     * This should eventually be fixed but for the time being we tolerate that.
     * Note: A line that has spaces and tabs are not ignored.
     * Note: The last line must ends with a newline character for this to work.
     * TODO: Remove me after fixing Okapi issue 687.
     */
    private void testRoundTripIgnoringEmptyLines(String originalFile) throws Exception {
        try (MarkdownFilter filter = new MarkdownFilter()) {
            String contents = getFileContents(originalFile);
            ISkeletonWriter sw = filter.getFilterWriter().getSkeletonWriter();

            List<Event> events = FilterTestDriver.getEvents(filter, contents, null, TARGET_LOCALE_ID);
            FilterUtil.logDebugEvents(events, LOGGER);

            EncoderManager em = new EncoderManager();
            em.setAllKnownMappings();
            assertEquals(compactSpacesAndRemoveBlankLines(contents),
                         compactSpacesAndRemoveBlankLines(FilterTestDriver.generateOutput(events, TARGET_LOCALE_ID, sw, em)));
        }
    }

    /*
     * A variant of testRoundTrip() that ignores surface differences in HTML
     * that don't change the meaning.
     * The HTML sub filter that Markdown uses tends to remove white spaces
     * which are meaningless in the HTML text unless they are in the <pre> block.
     * It also converts all literal quotes to named entities (&quot;) or numeric entities (&#39;)
     * (or the reverse, depending on the HTML filter configuration).
     * This test method ignores such differences.
     */
    private void testRoundTripIgnoringWhitespacesNearHtmlTagsAndQuotes(String originalFile) throws Exception {
        try (MarkdownFilter filter = new MarkdownFilter()) {
            String contents = getFileContents(originalFile);
            ISkeletonWriter sw = filter.getFilterWriter().getSkeletonWriter();

            List<Event> events = FilterTestDriver.getEvents(filter, contents, null, TARGET_LOCALE_ID);
            FilterUtil.logDebugEvents(events, LOGGER);

            EncoderManager em = new EncoderManager();
            em.setAllKnownMappings();
            assertEqualsIgnoringWhitespacesNearHtmlTagsAndQuotes(contents, FilterTestDriver.generateOutput(events, TARGET_LOCALE_ID, sw, em));

        }
    }

    private void assertEqualsIgnoringWhitespacesNearHtmlTagsAndQuotes(String expected, String actual) {
        assertEquals(transformStringToIgnoreWhitespacesAndQuoteDifferences(expected),
                     transformStringToIgnoreWhitespacesAndQuoteDifferences(actual));
    }

    private String transformStringToIgnoreWhitespacesAndQuoteDifferences(String in) {
        in = Util.normalizeNewlines(in); // Deal with the Linux vs. Windows line-break difference
        return in.replaceAll(">\\s*", ">").replaceAll("\\s*<", "<").replaceAll("&quot;", "\"").replaceAll("&#39;", "'");
    }

    private String compactSpacesAndRemoveBlankLines(String in) { // Note this won't preserve the hard line break
        in = Util.normalizeNewlines(in); // Deal with the Linux vs. Windows line-break difference
        return in.replaceAll("[ \t]", " ").replaceAll(" \n", "\n").replaceAll("\n\n+", "\n");
    }


    private void testChangedOutput(String originalFile, String changedFile) throws Exception {
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<Event> events = FilterTestDriver.getEvents(filter,
                    getFileContents(originalFile), null, TARGET_LOCALE_ID);

            EncoderManager em = new EncoderManager();
            em.setAllKnownMappings();
            assertEquals(getFileContents(changedFile),
                    FilterTestDriver.generateOutput(events, TARGET_LOCALE_ID, filter.getFilterWriter().getSkeletonWriter(), em, true));
        }
    }

    private String getFileContents(String filename) throws Exception {
        Path path = FileLocation.fromClass(getClass()).in(filename).asPath();
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return content; // return Util.normalizeNewlines(content);
    }
}
