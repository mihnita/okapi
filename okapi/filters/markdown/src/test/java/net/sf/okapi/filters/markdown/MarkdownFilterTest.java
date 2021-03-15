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

import static net.sf.okapi.common.filters.FilterTestUtil.assertDocumentPart;
import static net.sf.okapi.common.filters.FilterTestUtil.assertTUListContains;
import static net.sf.okapi.common.filters.FilterTestUtil.assertTUListDoesNotContain;
import static net.sf.okapi.common.filters.FilterTestUtil.assertTextUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

@RunWith(JUnit4.class)
public class MarkdownFilterTest {

    /*
     * This returns a UPA character pair representing n'th isolated place holder.
     */
    private final static String IPH(int n) {
        return new String(new char[]{TextFragment.MARKER_ISOLATED, (char)(TextFragment.CHARBASE+n)});
    }
    private final static String IPH0 = IPH(0);
    private final static String IPH1 = IPH(1);
    private final static String IPH2 = IPH(2);
    private final FileLocation root = FileLocation.fromClass(getClass());
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test
    public void testCloseWithoutInput() throws Exception {
        MarkdownFilter filter = new MarkdownFilter();
        filter.close();
    }

    @Test
    public void testEventsFromEmptyInput() {
        String snippet = "";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 2, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
            assertEquals(EventType.END_DOCUMENT, events.get(1).getEventType());
        }
    }

    @Test
    public void testAutoLink() {
        String snippet = "<https://www.google.com>";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 3, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
            assertDocumentPart(events.get(1), "<https://www.google.com>");
            assertEquals(EventType.END_DOCUMENT, events.get(2).getEventType());
        }
    }

    @Test
    public void testBlockQuoteEvents() {
        String snippet = "> Blockquote line 1\n> Blockquote line 2\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            // assertDocumentPart(events.get(i++), "> ");
            assertTextUnit(events.get(i++), "Blockquote line 1\nBlockquote line 2");
            assertDocumentPart(events.get(i++), "\n");
            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());

        }
    }

    @Test
    public void testBulletList() {
        String snippet = "* First\nelement\n\n" + "* Second element\n\n" + "* Third element\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertDocumentPart(events.get(i++), "* ");
            assertTextUnit(events.get(i++), "First\nelement");
            assertDocumentPart(events.get(i++), "\n");
            assertDocumentPart(events.get(i++), "\n");
            
            assertDocumentPart(events.get(i++), "* ");
            assertTextUnit(events.get(i++), "Second element");
            assertDocumentPart(events.get(i++), "\n");
            assertDocumentPart(events.get(i++), "\n");
            
            assertDocumentPart(events.get(i++), "* ");
            assertTextUnit(events.get(i++), "Third element");
            assertDocumentPart(events.get(i++), "\n");
            assertDocumentPart(events.get(i++), "\n");
 
            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());            
        }
    }

    @Test
    public void testCode() {
        String snippet = "`Text`";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertTextUnit(events.get(i), "`Text`", "`", "`");
            assertEquals("\uE101\uE110Text\uE102\uE111", events.get(i++).getTextUnit().getSource().getCodedText());
            assertEquals(EventType.END_DOCUMENT, events.get(i).getEventType());
        }
    }

    @Test
    public void testEmphasis() {
        String snippet = "_Italic and _Bold__";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertTextUnit(events.get(i), "_Italic and _Bold__", "_", "_", "_", "_");
            assertEquals("\uE101\uE110Italic and \uE102\uE111Bold\uE101\uE112\uE102\uE113", events.get(i++).getTextUnit().getSource().getCodedText());
            assertEquals(EventType.END_DOCUMENT, events.get(i).getEventType());
        }
    }

    @Test
    public void testCodeAndEmphasis() {
        String snippet = "`_Not Italic but Code_`";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertTextUnit(events.get(i), "`_Not Italic but Code_`", "`", "`");
            assertEquals("\uE101\uE110_Not Italic but Code_\uE102\uE111", events.get(i++).getTextUnit().getSource().getCodedText());
            assertEquals(EventType.END_DOCUMENT, events.get(i).getEventType());
        }
    }

    @Test
    public void testEmphasisAcrossLines() {
        String snippet = "Here comes _emphasized part of text that\nincludes a soft line break_.";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertTextUnit(events.get(i++), snippet, "_", "_");
            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());
        }
    }

    @Test
    public void testFencedCodeBlock() {
        String snippet = "```{java}\n"
                + "Line 1 in a fenced code block\n"
                + "Line 2 in a fenced code block\n"
                + "Line 3 in a fenced code block\n"
                + "```\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());

            assertDocumentPart(events.get(i++), "```");
            assertDocumentPart(events.get(i++), "{java}");
            assertDocumentPart(events.get(i++), "\n");
            assertTextUnit(events.get(i++), "Line 1 in a fenced code block\nLine 2 in a fenced code block\nLine 3 in a fenced code block\n");
            //assertDocumentPart(events.get(i++), "\n");
            assertDocumentPart(events.get(i++), "```");
            assertDocumentPart(events.get(i++), "\n");
            assertDocumentPart(events.get(i++), "\n");

            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());
        }
    }

    @Test
    public void testHeadingPrefix() {
        String snippet = "# Heading\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i = 0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertDocumentPart(events.get(i++), "# ");
            assertTextUnit(events.get(i++), "Heading");
            assertDocumentPart(events.get(i++), "\n");
            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());
        }
    }

    @Test
    public void testHeadingPrefixWithoutSpace() {
        // Note: This was supported by Github Markdown but is no longer supported.
        // The tokens are of type TEXT but are meant to be headers, so have the filter convert the '#'s to code
        String snippet = "#Heading 1\n\n" + "##Heading 2\n\n" + "###Heading 3";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);

            int i = 0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());

            assertDocumentPart(events.get(i++), "# ");
            assertTextUnit(events.get(i++), "Heading 1");

            assertDocumentPart(events.get(i++), "\n");
            assertDocumentPart(events.get(i++), "\n");

            assertDocumentPart(events.get(i++), "## ");
            assertTextUnit(events.get(i++), "Heading 2");
            assertDocumentPart(events.get(i++), "\n");
            assertDocumentPart(events.get(i++), "\n");

            assertDocumentPart(events.get(i++), "### ");
            assertTextUnit(events.get(i++), "Heading 3");
            assertDocumentPart(events.get(i++), "\n");// The newline is auto-inserted, due to a limitation of bug fix #687.

            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());
        }
    }

    @Test
    public void testHeadingUnderline() {
        String snippet = "Heading\n=======\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 7, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());

            assertTextUnit(events.get(1), "Heading");
            assertDocumentPart(events.get(2), "\n");
            assertDocumentPart(events.get(3), "=======");
            assertDocumentPart(events.get(4), "\n");
            assertDocumentPart(events.get(5), "\n");

            assertEquals(EventType.END_DOCUMENT, events.get(6).getEventType());
        }
    }

    @Test
    public void testHtmlTable() {
        String snippet = "<table><tr><td>Test</td></tr></table>\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "Test");
        }
    }

    @Test
    public void testHtmlBlockWithMarkdown() {
        String snippet = "<table><tr><td>**Bold** *Italic*</td></tr></table>\n";;
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "**Bold** *Italic*");
            assertTUListDoesNotContain(tus, "table");
        }
    }

    @Test
    public void testHtmlInline() {
        String snippet = "This contains <span>some inline</span> HTML\n\n";
        // Flexmark does not recognize the run of text <span>some inline</span> as one unit.
        // It only recognizes each of <span> and </span> as an HTML Inline element.

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "This contains");
            assertTUListContains(tus, "some inline");
            assertTUListContains(tus, "HTML");
            assertTUListDoesNotContain(tus, "span");
        }
    }

    @Test
    public void testHtmlInlineWithAttributes() {
        String snippet = "Sentence 1. <span class=\"foo\">Sentence 2.</span> Sentence 3.";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<ITextUnit> tus =
                    FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, snippet, null));
            assertTUListContains(tus, "Sentence 1.");
            assertTUListContains(tus, "Sentence 2.");
            assertTUListContains(tus, "Sentence 3.");
            assertTUListDoesNotContain(tus, "span");
            assertTUListDoesNotContain(tus, "foo");
            assertTUListDoesNotContain(tus, "class");
        }
    }

    @Test
    public void testHtmlBreakElement() {
        String snippet = "<p>Sentence 1.<br/>Sentence 2.</p>";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<ITextUnit> tus =
                    FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, snippet, null));
            assertEquals(1, tus.size());
            assertTextUnit(tus.get(0), "Sentence 1.<br/>Sentence 2.", "<br/>");
        }
    }

    @Test
    public void testHtmlCommentAtColumn1() {
        String snippet = "<!-- this line has only an HTML comment that won't be rendered. So it should be untranslatable. -->";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<ITextUnit> tus =
                    FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, snippet, null));
            assertTUListDoesNotContain(tus, "this");
            assertTUListDoesNotContain(tus, "untranslatable");

        }
    }

    @Test
    public void testHtmlCommentAtColumn5() {
        String snippet = "        <!-- An HTML comment in indented code block is rendered, so it should be translatable. -->\n";
        // Although it contains a piece of text that looks like an HTML comment, the whole thing
        // should be interpreted as an indented code block. Everything except for the leading spaces should be extracted.
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, snippet, null));
            assertTUListContains(tus, "An HTML comment");
            assertTUListContains(tus, "translatable");
        }
    }

    @Test
    public void testImage() {
        String snippet = "Here is an ![Image](https://www.google.com)\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 5, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
            assertEquals(EventType.TEXT_UNIT, events.get(1).getEventType());
            assertEquals("Here is an " + IPH0 + "Image" + IPH1,
                         events.get(1).getTextUnit().getSource().getCodedText());
            assertEquals(EventType.DOCUMENT_PART, events.get(2).getEventType());
            assertEquals(EventType.DOCUMENT_PART, events.get(3).getEventType());
            assertEquals(EventType.END_DOCUMENT, events.get(4).getEventType());
        }
    }

    @Test
    public void testExtractImageTitleAndAltText() {
        String snippet = "![alt text](https://github.com/adam-p/markdown-here/raw/master/src/common/images/icon48.png"
                         + " \"Logo Title Text 1\")";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, snippet, null));
            assertEquals(1, tus.size());
            assertEquals(IPH0 + "alt text" + IPH1 + "Logo Title Text 1" + IPH2, tus.get(0).getSource().getCodedText());
        }
    }

    @Test
    public void testExtractImageTitleButNotAltText() {
        String snippet = "![alt text](https://github.com/adam-p/markdown-here/raw/master/src/common/images/icon48.png"
                         + " \"Logo Title Text 1\")";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            Parameters params = filter.getParameters();
            params.setTranslateImageAltText(false);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, snippet, null));
            assertEquals(1, tus.size());
            assertEquals(IPH0 + "Logo Title Text 1" + IPH1, tus.get(0).getSource().getCodedText());
        }
    }

    @Test
    public void testImageWithTranslatableUrl() {
        String snippet = "Here is an ![Image](https://www.google.com)\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            filter.getParameters().setTranslateUrls(true);
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertEquals(1, tus.size());
            assertEquals("Here is an " + IPH0 +"Image" + IPH1 + "https://www.google.com" + IPH2,
                         tus.get(0).getSource().getCodedText());
            assertEquals(EventType.DOCUMENT_PART, events.get(2).getEventType());
            assertEquals(EventType.DOCUMENT_PART, events.get(3).getEventType());
            assertEquals(EventType.END_DOCUMENT, events.get(4).getEventType());
        }
    }

    @Test
    public void testImageRef() {
        String snippet = "![Image][A]\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertEquals(EventType.TEXT_UNIT, events.get(i++).getEventType());		// "![Image][A]" 
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// "\n"
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// "\n"
            assertEquals(EventType.END_DOCUMENT, events.get(i).getEventType());
        }
    }

    @Test
    public void testImgTagWithAlt() {
        String snippet = "Our logo looks like this:\n" +
                        "<img src=\"https://www.spartansoftwareinc.com/wp-content/uploads/2016/07/Logo-Text.png\" alt=\"Spartan logo\">\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "Our logo");
            assertTUListContains(tus, "Spartan logo");
            assertTUListDoesNotContain(tus, "Logo-Text.png");
        }
    }


    @Test // Tests if a URL is extracted when translateUrls=true
    public void testImageRefWithTranslatableUrl() {
        String snippet = "![Image][ref-text]\n\n"
        	+ "[ref-text]: https://www.google.com \"title text should be extracted\"\n\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            filter.getParameters().setTranslateUrls(true);
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "https://www.google.com");
            assertTUListDoesNotContain(tus, "ref-text");
            assertTUListContains(tus, "title text should be extracted");
        }
    }

    
    @Test
    public void testIndentedCodeBlock() {
        String snippet = "    This is text\n"
                + "    in an indented\n"
                + "    code block\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 4, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());

            assertTextUnit(events.get(1),
                      "This is text\n"
                    + "in an indented\n"
                    + "code block\n");
            assertDocumentPart(events.get(2), "\n");

            assertEquals(EventType.END_DOCUMENT, events.get(3).getEventType());
        }
    }

    @Test
    public void testTabIndentedCodeBlock() {
        String snippet = "\tThis is text\n"
                + "\tin an indented\n"
                + "\tcode block\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 4, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());

            assertTextUnit(events.get(1),
                      "This is text\n"
                    + "in an indented\n"
                    + "code block\n");
            assertDocumentPart(events.get(2), "\n");

            assertEquals(EventType.END_DOCUMENT, events.get(3).getEventType());
        }
    }

    @Test
    public void testLink() {
        String snippet = "This is a [Link](<https://www.google.com>)\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertEquals(1, tus.size());
            assertEquals("This is a \uE101\uE110Link\uE102\uE111", tus.get(0).getSource().getCodedText());
        }
    }

    @Test
    public void testLinkWithTranslatableUrl() {
        String snippet = "This is a [Link](<https://www.google.com>)\n\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            Parameters params = filter.getParameters();
            params.setTranslateUrls(true);
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertEquals(1, tus.size());
            assertEquals("This is a \uE101\uE110Link\uE102\uE111https://www.google.com\uE103\uE112", tus.get(0).getSource().getCodedText());
        }
    }
    
    @Test
    public void testLinkWithTranslatableUrlByPattern() {
        String snippet = "This is a [Link to translate](<https://www.google.com>)\n\n"
        	+ "This is a [fixed link that should not be extracted](<faq/item1.html>)\n\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            Parameters params = filter.getParameters();
            params.setTranslateUrls(true);
            params.setUrlToTranslatePattern("https?://.*");
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "www.google.com");
            assertTUListContains(tus, "fixed link");
            assertTUListDoesNotContain(tus, "faq");
        }
    }


    @Test
    public void testHtmlEntities() {
        String snippet = "It&#39;s OK &amp; legal to have HTML Entity even in [link&quot;s anchor text](<http://okapiframework.org/wiki>) \n\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "&#39;");
            assertTUListContains(tus, "&quot;");
            assertTUListContains(tus, "&amp;");
        }
    }
    
    @Test
    public void testLinkRef() {
        String snippet = "[Link][A]\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertEquals(EventType.TEXT_UNIT, events.get(i++).getEventType()); 		// "[Link][A]"
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType()); 	// \n
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType()); 	// \n
            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());
        }
    }

    @Test
    public void testReferenceDefinition() {
        String snippet = "[Link][1]\n\n[1]: https://www.google.com 'Google'\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i = 0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertEquals(EventType.TEXT_UNIT, events.get(i++).getEventType()); 		// "[Link][A]"
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType()); 	// \n
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType()); 	// \n
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// "["
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// "1"
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// "]: "
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// "https://www.google.com"
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// " '"
            assertEquals(EventType.TEXT_UNIT, events.get(i++).getEventType());		// "Google"
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// "'"
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// \n
            assertEquals(EventType.DOCUMENT_PART, events.get(i++).getEventType());	// \n
            assertEquals(EventType.END_DOCUMENT, events.get(i).getEventType());
        }
    }

    @Test
    public void testEmphasisAndStrong() {
        String snippet = "Some **strong** and *emphasized* text";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 3, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
            assertTextUnit(events.get(1), "Some **strong** and *emphasized* text",
                    "**", "**", "*", "*");
            assertEquals(EventType.END_DOCUMENT, events.get(2).getEventType());        }
    }

    @Test
    public void testStrikethroughSubscript() {
        String snippet = "Some ~~strikethrough~~ and ~subscript~ text";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            assertEquals("#events", 3, events.size());
            assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
            assertTextUnit(events.get(1), "Some ~~strikethrough~~ and ~subscript~ text",
                    "~~", "~~", "~", "~");
            assertEquals(EventType.END_DOCUMENT, events.get(2).getEventType());
        }
    }

    @Test
    public void testThematicBreak() {
        String snippet = "---\n\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertDocumentPart(events.get(i++), "---\n\n");
            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());
        }
    }

    @Test
    public void testTable1TextUnits() throws Exception {
        RawDocument rd =  new RawDocument(getFileContents("table1_original.md"), LocaleId.ENGLISH);

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, rd);
            assertEquals("#events", 4, events.size());
            assertTextUnit(events.get(0), "Command");
            assertTextUnit(events.get(1), "Description");
            assertTextUnit(events.get(2), "`git status`", "`", "`");
            assertTextUnit(events.get(3), "List all **new** or _modified_ files", "**", "**", "_", "_");
        }
    }

    @Test
    public void testTable2TextUnits() throws Exception {
        RawDocument rd =  new RawDocument(getFileContents("table2_original.md"), LocaleId.ENGLISH);

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, rd);
            assertEquals("#events", 10, events.size());
            assertTextUnit(events.get(0), "Left-aligned");
            assertTextUnit(events.get(1), "Center-aligned");
            assertTextUnit(events.get(2), "Right-aligned");
            assertTextUnit(events.get(3), "git status");
            assertTextUnit(events.get(4), "git status");
            assertTextUnit(events.get(5), "git status");
            assertTextUnit(events.get(6), "git diff");
            assertTextUnit(events.get(7), "git diff");
            assertTextUnit(events.get(8), "git diff");
            assertTextUnit(events.get(9), "[GitHub](http://github.com)", "[", "](http://github.com)");
        }
    }

    @Test
    public void testDontTranslateFencedCodeBlocks() throws Exception {
        RawDocument rd = new RawDocument(getFileContents("code_and_codeblock_tests.md"), LocaleId.ENGLISH);
        try (MarkdownFilter filter = new MarkdownFilter()) {
            filter.getParameters().setTranslateCodeBlocks(false);
            filter.getParameters().setTranslateInlineCodeBlocks(false);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getTextUnitEvents(filter, rd));
            assertTextUnit(tus.get(0), "Code and Codeblock tests");
            assertTextUnit(tus.get(1), "Code blocks");
            assertTextUnit(tus.get(2), "There are two ways to specify code blocks. One may delimit via four  tildas, like this:");
            assertTextUnit(tus.get(3), "Another is to delimit with three ticks like this:");
            assertTextUnit(tus.get(4), "One may also specify that the code is to be treated with syntax coloring like this:");
            assertTextUnit(tus.get(5), "Inline code blocks");
            assertTextUnit(tus.get(6), "Inline code contain things like `variable names` that we may want to protect.", "`variable names`");
        }
    }

    @Test
    public void testTranslateFencedCodeBlocks() throws Exception {
        RawDocument rd = new RawDocument(getFileContents("code_and_codeblock_tests.md"), LocaleId.ENGLISH);
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getTextUnitEvents(filter, rd));
            assertTextUnit(tus.get(0), "Code and Codeblock tests");
            assertTextUnit(tus.get(1), "Code blocks");
            assertTextUnit(tus.get(2), "There are two ways to specify code blocks. One may delimit via four  tildas, like this:");
            assertTextUnit(tus.get(3), "This text is within a code block and should remain in English.\n");
            assertTextUnit(tus.get(4), "Another is to delimit with three ticks like this:");
            assertTextUnit(tus.get(5), "This text is within a code block and should remain in English.\n");
            assertTextUnit(tus.get(6), "One may also specify that the code is to be treated with syntax coloring like this:");
            assertTextUnit(tus.get(7), "This text is within a code block and should remain in English.\n");
            assertTextUnit(tus.get(8), "Inline code blocks");
            assertTextUnit(tus.get(9), "Inline code contain things like `variable names` that we may want to protect.", "`", "`");
        }
    }

    @Test
    public void testDontTranslateMetadataHeader() throws Exception {
        RawDocument rd = new RawDocument(getFileContents("metadata_header.md"), LocaleId.ENGLISH);
        try (MarkdownFilter filter = new MarkdownFilter()) {
            // This is the default behavior
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getTextUnitEvents(filter, rd));
            assertTextUnit(tus.get(0), "This should be the only translatable segment.");
        }
    }

    @Test
    public void testTranslateMetadataHeader() throws Exception {
        RawDocument rd = new RawDocument(getFileContents("metadata_header.md"), LocaleId.ENGLISH);

        FilterConfigurationMapper mapper = new FilterConfigurationMapper();
        mapper.addConfigurations(net.sf.okapi.filters.markdown.MarkdownFilter.class.getName());

        try (MarkdownFilter filter = new MarkdownFilter()) {
            filter.setFilterConfigurationMapper(mapper);
            filter.getParameters().setTranslateHeaderMetadata(true);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getTextUnitEvents(filter, rd));
            assertTextUnit(tus.get(0), "value");
            assertTextUnit(tus.get(1), "value1, value2");
            assertTextUnit(tus.get(2), "This should be the only translatable segment.");
        }
    }

    @Test
    public void testInlineHtmlTag() throws Exception { // Issue 716
        String snippet = "Let's throw in a <b>tag</b> to see what happens!";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertEquals(1, tus.size());
            assertTUListContains(tus, "tag");
            assertTUListDoesNotContain(tus, "<b>");
        }
    }
    
    @Test
    public void testATagWithTitleAttr() throws Exception {
        String snippet = "\n<a href=\"google.com\" title=\"title text here\">google it!</a>\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTrue(
        	    String.format("Number of TextUnits should be 2 (preferred) or 1 (OK for now) but was %d", tus.size()),
        	    (tus.size()==2||tus.size()==1));
            assertTUListContains(tus, "title text here");
            assertTUListContains(tus, "google it!");
            assertTUListDoesNotContain(tus, "google.com");
        }
    }

    @Test
    public void testATagWithTitletWithinDiv() throws Exception {
        // Flexmark treats <div>...<div> as HTML_BLOCK even when everything is in one line.
        String snippet = "<div><a href=\"google.com\" title=\"title text here\">google it!</a></div>\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "title text here");
            assertTUListContains(tus, "google it!");
            assertTUListDoesNotContain(tus, "google.com");
        }
    }

    @Test
    public void testMathTag() throws Exception {
        // Test if the math block is handled as an untranslatable DocumentPart.
        String mathBlock =
                "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n" +
                "  <mstyle displaystyle=\"true\">\n" +
                "    <msup>\n" +
                "      <mrow>\n" +
                "        <mi> &#x03C1;<!--greek small letter rho--> </mi>\n" +
                "      </mrow>\n" +
                "      <mrow>\n" +
                "        <mi> charge </mi>\n" +
                "      </mrow>\n" +
                "    </msup>\n" +
                "  </mstyle></math>";
        String snippet = "This contains a math block\n\n" +
                mathBlock +
                "\n\n" +
                "End of the math block";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "This contains a math block");
            assertTUListDoesNotContain(tus, "charge");
        }
    }

    @Test
    public void testMathElementWithCommentBehavior() throws Exception {
        // Test if the math block is handled as an untranslatable DocumentPart.
        String snippet = "This contains a math block\n\n" +
                        "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n" +
                        "<!-- </math> doesn't confuse the parser -->\nHello,World!\n</math>\n\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "This contains a math block");
            assertTUListDoesNotContain(tus, "confuse");
            assertTUListDoesNotContain(tus, "Hello,World!");
        }
    }

    @Test
    public void testMathBlockOnSingleLine() {
        String mathBlock =
                "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">" +
                "  <mstyle displaystyle=\"true\">" +
                "    <msup>" +
                "      <mrow>" +
                "        <mi> &#x03C1;<!--greek small letter rho--> </mi>" +
                "      </mrow>" +
                "      <mrow>" +
                "        <mi> charge </mi>" +
                "      </mrow>" +
                "    </msup>" +
                "  </mstyle></math>";
        String snippet = "This contains a math block\n\n" +
                mathBlock +
                "\n\n" +
                "End of the math block";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListContains(tus, "This contains a math block");
            assertTUListDoesNotContain(tus, "charge");
        }
    }

    @Test
    public void testMathBlocksInListItems() throws Exception {
        String mathBlock1 = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n" +
                "  <mstyle displaystyle=\"true\">\n" +
                "    <mover>\n" +
                "      <mrow>\n" +
                "        <mi> H </mi>\n" +
                "      </mrow>\n" +
                "      <mrow>\n" +
                "        <mo> &#x2192;<!--rightwards arrow--> </mo>\n" +
                "      </mrow>\n" +
                "    </mover>\n" +
                "  </mstyle></math>";
        String mathBlock2 = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n" +
                "  <mstyle displaystyle=\"true\">\n" +
                "    <mover>\n" +
                "      <mrow>\n" +
                "        <mi> J </mi>\n" +
                "      </mrow>\n" +
                "      <mrow>\n" +
                "        <mo> &#x2192;<!--rightwards arrow--> </mo>\n" +
                "      </mrow>\n" +
                "    </mover>\n" +
                "  </mstyle></math>";
        String snippet = "<ul><li>\n" +
                mathBlock1 + " is the magnetic field intensity" +
                "</li><li>" +
                mathBlock2 + " is the conduction current density" +
                "</li></ul>\n\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListDoesNotContain(tus, "H");
            assertTUListDoesNotContain(tus, "J");
            assertTUListDoesNotContain(tus, "rightwards arrow");
            assertTUListContains(tus, "the magnetic field intensity");
            assertTUListContains(tus, "the conduction current density");
        }
    }

    @Test
    public void testUnderlinedTextWithinAsterisks() { // Okapi issue #684
        String snippet = "**asterisks OR _underscores_**\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertEquals(1, tus.size());
            String ct = tus.get(0).getSource().getCodedText();
            assertTrue(ct.contains("asterisks OR"));
            assertTrue(ct.contains("underscores"));
            assertFalse(ct.contains("_" ));
        }
    }

    @Test
    public void testHtmlSubfilterConfig() {
        String snippet = "Testing if HTML subfilter config overrides works.\n\n" +
                         "<pre>We use the config to prevent extraction from pre block</pre>\n\n" +
                         "<div>The text in the div block should be extracted</div>\n";
        
	FilterConfigurationMapper mapper = new FilterConfigurationMapper();
	mapper.addConfigurations(net.sf.okapi.filters.html.HtmlFilter.class.getName());
	net.sf.okapi.filters.html.Parameters htmlParams = new net.sf.okapi.filters.html.Parameters(root.in("/non-pre-extracting-html-filter-config.yml").asUrl());
	mapper.addConfiguration(
		new FilterConfiguration("okf_html@exclude-pre",
					MimeTypeMapper.HTML_MIME_TYPE,
					net.sf.okapi.filters.html.HtmlFilter.class.getName(),
					"HTML", "", null, htmlParams, ".html"));
        try (MarkdownFilter filter = new MarkdownFilter()) {
            Parameters params = filter.getParameters();
            params.setHtmlSubfilter("okf_html@exclude-pre");
            filter.setFilterConfigurationMapper(mapper);
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, new RawDocument(snippet, LocaleId.ENGLISH), params);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTUListDoesNotContain(tus, "prevent");
            assertTUListContains(tus, "the div block should be extracted");
        }
    }
    
    @Test
    public void testEmphasisAtParaStart() { // Okapi issue #701
	String snippet = "*RED WINE* is a nice beverage.";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertEquals(1, tus.size());
            assertTextUnit(tus.get(0), "*RED WINE* is a nice beverage.", "*", "*");
        }	
    }
    
    @Test
    public void testCodeFinder() {
        String snippet = "{{#test}} handle bar test {{/test}}\n\n{{stand-alone handle bar}}\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            Parameters params = filter.getParameters();
            params.setUseCodeFinder(true);
            params.getCodeFinder().reset();
            params.getCodeFinder().addRule("\\{\\{[^}]+\\}\\}");
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertTextUnit(tus.get(0), "{{#test}} handle bar test {{/test}}",
                    "{{#test}}", "{{/test}}");
            assertTextUnit(tus.get(1), "{{stand-alone handle bar}}",
                    "{{stand-alone handle bar}}");
        }
    }
    
    @Test
    public void testNeighboringMarks() { // Issue #715
        String snippet = "Okapi is a *easy-to-use* _localization_ **framework**\n";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(FilterTestDriver.getEvents(filter, snippet, null));
            assertEquals(1, tus.size());
            assertTUListContains(tus, "framework");
            assertTUListDoesNotContain(tus, "*");
        }
    }
    
    private String getFileContents(String filename) throws Exception {
        try (InputStream is = root.in(filename).asInputStream();
                Scanner scanner = new Scanner(is)) {
            return scanner.useDelimiter("\\A").next();
        }
    }
    
    @Test
    public void testNonTranslatableBlockQuotes() {
        String snippet = "This text would be translated\n> !DONOTLOCALIZE\n> This won't be localized\n\nThe text following the do not localize tag";
        try (MarkdownFilter filter = new MarkdownFilter()) {
        	Parameters params = filter.getParameters();
            params.setNonTranslateBlocks("!DONOTLOCALIZE");
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
            assertEquals(2, tus.size());
            assertTextUnit(tus.get(0), "This text would be translated");
            assertTextUnit(tus.get(1), "The text following the do not localize tag");
        }
    }

    @Test
    public void testComplexFrontmatterYaml() throws Exception {
        RawDocument rd =  new RawDocument(getFileContents("complex_frontmatter.md"), LocaleId.ENGLISH);

        FilterConfigurationMapper mapper = new FilterConfigurationMapper();
        mapper.addConfigurations(net.sf.okapi.filters.yaml.YamlFilter.class.getName());
        mapper.addConfigurations(net.sf.okapi.filters.markdown.MarkdownFilter.class.getName());
        mapper.addConfigurations(net.sf.okapi.filters.html.HtmlFilter.class.getName());

        try (MarkdownFilter filter = new MarkdownFilter()) {
            filter.setFilterConfigurationMapper(mapper);

            Parameters params = filter.getParameters();
            params.setTranslateHeaderMetadata(true);
            ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, rd);
            assertEquals("#events", 26, events.size());
            assertTextUnit(events.get(0), "Lorem Ipsum");
            assertTextUnit(events.get(1), "2018-08-23 08:57:00 +01:00");
            assertTextUnit(events.get(2), "Lorem Ipsum");
            assertTextUnit(events.get(7), "Dui faucibus in ornare quam. "
                + "A condimentum vitae sapien pellentesque. Mus mauris vitae ultricies leo. "
                + "Augue ut lectus arcu bibendum. [md link label](https://url)", "[", "](https://url)");
            assertTextUnit(events.get(8), "Et leo duis ut diam quam nulla porttitor. "
                + "Pulvinar sapien et ligula ullamcorper malesuada proin libero nunc. "
                + "Turpis cursus in hac habitasse platea dictumst quisque. ");
            assertTrue(events.get(8).getTextUnit().preserveWhitespaces());
            List<ITextUnit> textUnits = new ArrayList<>();
            textUnits.add(events.get(9).getTextUnit());
            assertTUListContains(textUnits, "anchor label");
            assertTUListDoesNotContain(textUnits, "https://url");
            assertTUListDoesNotContain(textUnits, "</a>");
            assertTextUnit(events.get(10), "John Doe");
            assertTextUnit(events.get(11), "Mary Doe");
            assertTextUnit(events.get(12), "Person");
            assertTextUnit(events.get(17), "/uploads/logo.png");
            assertTextUnit(events.get(18), "/uploads/icon-1.svg?v=2");
            assertTextUnit(events.get(19), "metric 1");
            assertTextUnit(events.get(20), "/uploads/icon-2.svg?v=2");
            assertTextUnit(events.get(21), "metric 2");
        }
    }

    @Test
    public void testComplexFrontmatterYamlHtml() throws Exception {
        RawDocument rd =  new RawDocument(getFileContents("complex_frontmatter.md"), LocaleId.ENGLISH);

        FilterConfigurationMapper mapper = new FilterConfigurationMapper();
        mapper.addConfigurations(net.sf.okapi.filters.yaml.YamlFilter.class.getName());
        mapper.addConfigurations(net.sf.okapi.filters.markdown.MarkdownFilter.class.getName());
        mapper.addConfigurations(net.sf.okapi.filters.html.HtmlFilter.class.getName());

        net.sf.okapi.filters.yaml.Parameters yamlParams = new net.sf.okapi.filters.yaml.Parameters();
        yamlParams.setSubfilter("okf_html");
        yamlParams.setUseCodeFinder(false);

        mapper.addConfiguration(
                new FilterConfiguration("okf_yaml@test",
                        MimeTypeMapper.YAML_MIME_TYPE,
                        net.sf.okapi.filters.yaml.YamlFilter.class.getName(),
                        "YAML", "", null, yamlParams, ".yml"));

        try (MarkdownFilter filter = new MarkdownFilter()) {
            filter.setFilterConfigurationMapper(mapper);

            Parameters params = filter.getParameters();
            params.setYamlSubfilter("okf_yaml@test");
            params.setTranslateHeaderMetadata(true);

            ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, rd);
            assertEquals("#events", 24, events.size());

            // Ensure list items have been separated
            assertTextUnit(events.get(8), "John Doe");
            assertTextUnit(events.get(9), "Mary Doe");
            assertTrue(events.get(9).getTextUnit().preserveWhitespaces());
        }
    }

    @Test
    public void testHardLineBreak() throws Exception {
        String snippet = "*foo  \nbar*";
        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i = 0;

            // The hard line breaks are allowed within inline markers according to
            // https://spec.commonmark.org/0.28/#example-610
            // We need to treat them as codes.
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertTextUnit(events.get(i++), snippet, "*", "  ", "*");
            assertEquals(EventType.END_DOCUMENT, events.get(i).getEventType());
        }
    }

    @Test // Issue #820
    public void testCRLF() throws Exception {
        String snippet = "foo\r\nbar\r\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i = 0;

            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            Event ev = events.get(i++);
            assertEquals(EventType.TEXT_UNIT, ev.getEventType());
            assertFalse("TU should not include CRs",
                        ev.getTextUnit().getSource().getFirstContent().getText().contains("\r"));
            assertDocumentPart(events.get(i++), "\r\n"); // DP should retain the original EOL.
            assertEquals(EventType.END_DOCUMENT, events.get(i).getEventType());
        }
    }

    @Test
    public void testRunQuotedFencedCodeBlock() throws Exception {
        String snippet = "> ```\n> Blockquote fenced code line 1\n> Blockquote fenced code line 2\n> ```\n";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            int i=0;
            assertEquals(EventType.START_DOCUMENT, events.get(i++).getEventType());
            assertDocumentPart(events.get(i++), "```");
            assertDocumentPart(events.get(i++), "\n");
            assertTextUnit(events.get(i++), "Blockquote fenced code line 1\nBlockquote fenced code line 2\n");
            assertDocumentPart(events.get(i++), "```");
            assertDocumentPart(events.get(i++), "\n");
            assertEquals(EventType.END_DOCUMENT, events.get(i++).getEventType());

        }
    }

    @Test
    public void testNativeCodeTypes() throws Exception {
        String snippet = "_Italic_ and __Bold__ and [[linkref]] and [link](http://okapiframework.org).";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
            TextFragment tf = tu.getSource().getFirstContent();
            assertEquals(8, tf.getCodes().size());
            assertEquals(Code.TYPE_ITALIC, tf.getCodes().get(0).getType());
            assertEquals(Code.TYPE_ITALIC, tf.getCodes().get(1).getType());
            assertEquals(Code.TYPE_BOLD, tf.getCodes().get(2).getType());
            assertEquals(Code.TYPE_BOLD, tf.getCodes().get(3).getType());
            assertEquals(Code.TYPE_LINK, tf.getCodes().get(4).getType());
            assertEquals(Code.TYPE_LINK, tf.getCodes().get(5).getType());
            assertEquals(Code.TYPE_LINK, tf.getCodes().get(6).getType());
            assertEquals(Code.TYPE_LINK, tf.getCodes().get(7).getType());
        }
    }

    @Test
    public void testLinkRefAsPairedCode() throws Exception {
        String snippet = "Text containing a [[linkref]].";

        try (MarkdownFilter filter = new MarkdownFilter()) {
            ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
            ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
            TextFragment tf = tu.getSource().getFirstContent();
            assertEquals(2, tf.getCodes().size());
            assertEquals(TagType.OPENING, tf.getCodes().get(0).getTagType());
            assertEquals(TagType.CLOSING, tf.getCodes().get(1).getTagType());
            assertEquals(tf.getCodes().get(0).getId(), tf.getCodes().get(1).getId());
        }
    }
}
