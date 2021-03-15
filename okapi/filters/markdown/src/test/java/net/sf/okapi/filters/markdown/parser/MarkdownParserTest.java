/*===========================================================================
  Copyright (C) 2018-2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.markdown.parser;

import static net.sf.okapi.filters.markdown.parser.MarkdownTokenType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.filters.markdown.Parameters;

@RunWith(JUnit4.class)
public class MarkdownParserTest {
    private static final String NEWLINE = "\n";
    private MarkdownParser parser;

    @Before
    public void setup() {
        parser = new MarkdownParser(new Parameters(), NEWLINE);
    }

    @Test
    public void testAutoLink() {
        parser.parse("<https://www.google.com>");
        assertNextToken(parser, "<https://www.google.com>", false, AUTO_LINK);
    }

    @Test
    public void testBlockQuote1() {
        parser.parse("> Blockquote");
        assertNextToken(parser, "> ", false, LINE_PREFIX);
        assertNextToken(parser, "Blockquote", true, TEXT);
        assertNextToken(parser, "", false, LINE_PREFIX);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testBlockQuote2() {
        parser.parse("> Blockquote" + NEWLINE + "across multiple lines");
        assertNextToken(parser, "> ", false, LINE_PREFIX);
        assertNextToken(parser, "Blockquote" + NEWLINE + "across multiple lines", true, TEXT);
        assertNextToken(parser, "", false, LINE_PREFIX);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testBulletList1() {
        parser.parse("* First" + NEWLINE + "* Second" + NEWLINE + "* Third" + NEWLINE);
        assertNextToken(parser, "* ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "First", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);

        assertNextToken(parser, "* ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "Second", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);

        assertNextToken(parser, "* ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "Third", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testBulletList2() {
        parser.parse("* First" + NEWLINE + "element" + NEWLINE
                + "* Second element" + NEWLINE + NEWLINE + NEWLINE + NEWLINE
                + "* Third element");
        assertNextToken(parser, "* ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "First" + NEWLINE + "element", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);


        assertNextToken(parser, "* ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "Second element", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);

        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertNextToken(parser, "* ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "Third element", true, TEXT);
        assertNextToken(parser, "", false, LINE_PREFIX);

        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testCode1() {
        parser.parse("`Code content`");
        assertNextToken(parser, "`", false, CODE);
        assertNextToken(parser, "Code content", true, TEXT);
        assertNextToken(parser, "`", false, CODE);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testCode2() {
        parser.parse("`Code content" + NEWLINE + "across multiple` lines");
        assertNextToken(parser, "`", false, CODE);
        assertNextToken(parser, "Code content" + NEWLINE + "across multiple", true, TEXT);
        assertNextToken(parser, "`", false, CODE);
        assertNextToken(parser, " lines", true, TEXT);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testEmphasis1() {
        parser.parse("_Italics_");
        assertNextToken(parser, "_", false, EMPHASIS);
        assertNextToken(parser, "Italics", true, TEXT);
        assertNextToken(parser, "_", false, EMPHASIS);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testEmphasis2() {
        parser.parse("*Italics*");
        assertNextToken(parser, "*", false, EMPHASIS);
        assertNextToken(parser, "Italics", true, TEXT);
        assertNextToken(parser, "*", false, EMPHASIS);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testFencedCodeBlock() {
        parser.parse("```" + NEWLINE
                + "Content in a" + NEWLINE + "fenced code block" + NEWLINE
                + "```");
        assertNextToken(parser, "```", false, FENCED_CODE_BLOCK);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "Content in a" + NEWLINE + "fenced code block" + NEWLINE, true, TEXT);
        assertNextToken(parser, "```", false, FENCED_CODE_BLOCK);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testFencedCodeBlockWithInfo() {
        parser.parse("```python" + NEWLINE
                + "Content in a" + NEWLINE + "fenced code block" + NEWLINE
                + "```");
        assertNextToken(parser, "```", false, FENCED_CODE_BLOCK);
        assertNextToken(parser, "python", false, FENCED_CODE_BLOCK_INFO);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "Content in a" + NEWLINE + "fenced code block" + NEWLINE, true, TEXT);
        assertNextToken(parser, "```", false, FENCED_CODE_BLOCK);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHeading1A() {
        parser.parse("# Heading");
        assertNextToken(parser, "# ", false, HEADING_PREFIX);
        assertNextToken(parser, "Heading", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK); // Heading always ends with a newline.
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHeading1AX() {
        parser.parse("# Heading" + NEWLINE);
        assertNextToken(parser, "# ", false, HEADING_PREFIX);
        assertNextToken(parser, "Heading", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHeading1B() {
        parser.parse("Heading" + NEWLINE + "=======");
        assertNextToken(parser, "Heading", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "=======", false, HEADING_UNDERLINE);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHeading2A() {
        parser.parse("## Heading");
        assertNextToken(parser, "## ", false, HEADING_PREFIX);
        assertNextToken(parser, "Heading", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHeading2B() {
        parser.parse("Heading" + NEWLINE + "-------");
        assertNextToken(parser, "Heading", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "-------", false, HEADING_UNDERLINE);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHtmlBlock1() {
        parser.parse("<table><tr><td>Test</td></tr></table>");
        assertNextToken(parser, "<table><tr><td>Test</td></tr></table>", true, HTML_BLOCK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHtmlBlock2() {
        parser.parse("<![CDATA[Lorem ipsum" + NEWLINE + " dolor sit amet]]>");
        assertNextToken(parser, "<![CDATA[Lorem ipsum" + NEWLINE + " dolor sit amet]]>", true, HTML_BLOCK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHtmlBlockWithMarkdown() {
        parser.parse("<table><tr><td>" + NEWLINE + NEWLINE
                + "**Bold**" + NEWLINE + NEWLINE + "*Italic*" + NEWLINE + NEWLINE + "</td></tr></table>");
        
        assertNextToken(parser, "<table><tr><td>", true, HTML_BLOCK);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertNextToken(parser, "**", false, STRONG_EMPHASIS);
        assertNextToken(parser, "Bold", true, TEXT);
        assertNextToken(parser, "**", false, STRONG_EMPHASIS);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertNextToken(parser, "*", false, EMPHASIS);
        assertNextToken(parser, "Italic", true, TEXT);
        assertNextToken(parser, "*", false, EMPHASIS);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertNextToken(parser, "</td></tr></table>", true, HTML_BLOCK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHtmlCommentBlock() {
        parser.parse("<!-- HTML" + NEWLINE + " comment -->");
        assertNextToken(parser, "<!-- HTML" + NEWLINE + " comment -->", false, HTML_COMMENT_BLOCK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHtmlEntity() { // Currently, HTML entities are pass through, treated as regular text.
        parser.parse("&gt; &amp;" + NEWLINE + "&quot;");
        /* Maybe this is right?
        assertNextToken(parser, "&gt;", true, TEXT);
        assertNextToken(parser, " ", false, TEXT);
        assertNextToken(parser, "&amp;", true, TEXT);
        assertNextToken(parser, NEWLINE, true, SOFT_LINE_BREAK);
        assertNextToken(parser, "&quot;", true, TEXT);
        */
        assertNextToken(parser, "&gt; &amp;" + NEWLINE + "&quot;", true, TEXT);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHtmlInline() {
        parser.parse("This contains <span>some inline</span> HTML");
        assertNextToken(parser, "This contains ", true, TEXT);
        assertNextToken(parser, "<span>", false, HTML_INLINE);
        assertNextToken(parser, "some inline", true, TEXT);
        assertNextToken(parser, "</span>", false, HTML_INLINE);
        assertNextToken(parser, " HTML", true, TEXT);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testImage() {
        parser.parse("![Image](https://www.google.com)");
        assertNextToken(parser, "![", false, IMAGE);
        assertNextToken(parser, "Image", true, TEXT);
        assertNextToken(parser, "](https://www.google.com)", false, IMAGE);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testImageRef() {
        parser.parse("![Image][A]");
        assertNextToken(parser, "![", false, IMAGE_REF);
        assertNextToken(parser, "Image", true, TEXT);
        assertNextToken(parser, "]", false, IMAGE_REF);
        assertNextToken(parser, "[", false, IMAGE_REF);
        assertNextToken(parser, "A", false, IMAGE_REF);
        assertNextToken(parser, "]", false, IMAGE_REF);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testIndentedCodeBlock() {
        parser.parse("    This is text" + NEWLINE
                + "    in an indented" + NEWLINE
                + "    code block");
        assertNextToken(parser, "    ", false, LINE_PREFIX);
        assertNextToken(parser, "This is text" + NEWLINE
                                        + "in an indented" + NEWLINE
                                        + "code block", true, TEXT);
        assertNextToken(parser, "", false, END_TEXT_UNIT);
        assertNextToken(parser, "", false, LINE_PREFIX);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testHardLineBreak() {
        parser.parse("This is  " + NEWLINE + "separated by a hard line break");
        assertNextToken(parser, "This is", true, TEXT);
        assertNextToken(parser, "  ", false, HARD_LINE_BREAK);
        //assertNextToken(parser, NEWLINE, true, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE + "separated by a hard line break", true, TEXT);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLink1() {
        parser.parse("[Link](https://www.google.com 'Title')");
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "](https://www.google.com '", false, LINK);
        assertNextToken(parser, "Title", true, TEXT);
        assertNextToken(parser, "')", false, LINK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLink2() {
        parser.parse("[Link](https://www.google.com)");
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "](https://www.google.com)", false, LINK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLink3() {
        parser.parse("[Link]()");
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "]()", false, LINK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLink4() {
        parser.parse("[Link](<https://www.google.com>)");
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "](<https://www.google.com>)", false, LINK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLink5() {
        parser.parse("[Link](\\(foo\\))");
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "](\\(foo\\))", false, LINK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLink6() {
        parser.parse("[Link](\\(foo\\))");
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "](\\(foo\\))", false, LINK);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLinkRef() {
        parser.parse("[Link][B]");
        assertNextToken(parser, "[", false, LINK_REF);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "]", false, LINK_REF);
        assertNextToken(parser, "[", false, LINK_REF);
        assertNextToken(parser, "B", false, LINK_REF);
        assertNextToken(parser, "]", false, LINK_REF);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testParagraph() {
        parser.parse("A paragraph" + NEWLINE + NEWLINE);
        assertNextToken(parser, "A paragraph", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testOrderedList() {
        parser.parse("1. First" + NEWLINE + "2. Second" + NEWLINE + "3. Third");
        assertNextToken(parser, "1. ", false, ORDERED_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "First", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);
        assertNextToken(parser, "2. ", false, ORDERED_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "Second", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);
        assertNextToken(parser, "3. ", false, ORDERED_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "Third", true, TEXT);
        assertNextToken(parser, "", false, LINE_PREFIX);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testReferenceDefinition1() {
        parser.parse("[1]: http://google.com/ 'Google'");
        assertNextToken(parser, "[", false, REFERENCE);
        assertNextToken(parser, "1", false, REFERENCE);
        assertNextToken(parser, "]: ", false, REFERENCE);
        assertNextToken(parser, "http://google.com/", false, REFERENCE);
        assertNextToken(parser, " '", false, REFERENCE);
        assertNextToken(parser, "Google", false, REFERENCE); // Title for the unused reference doesn't get extracted.
        assertNextToken(parser, "'", false, REFERENCE);
        assertNextToken(parser, NEWLINE, false, REFERENCE);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testReferenceDefinition1plus() { // Used reference.
        parser.parse("[Link][1]\n\n[1]: http://google.com/ 'Google'");
        assertNextToken(parser, "[", false, LINK_REF);
        assertNextToken(parser, "Link", true, TEXT);
        assertNextToken(parser, "]", false, LINK_REF);
        assertNextToken(parser, "[", false, LINK_REF);
        assertNextToken(parser, "1", false, LINK_REF);
        assertNextToken(parser, "]", false, LINK_REF);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, "[", false, REFERENCE);
        assertNextToken(parser, "1", false, REFERENCE);
        assertNextToken(parser, "]: ", false, REFERENCE);
        assertNextToken(parser, "http://google.com/", false, REFERENCE);
        assertNextToken(parser, " '", false, REFERENCE);
        assertNextToken(parser, "Google", true, REFERENCE); 
        assertNextToken(parser, "'", false, REFERENCE);
        assertNextToken(parser, NEWLINE, false, REFERENCE);
        assertFalse(parser.hasNextToken());
    }
    
    @Test
    public void testReferenceDefinition2() {
        parser.parse("[1]: <http://google.com/>");
        assertNextToken(parser, "[", false, REFERENCE);
        assertNextToken(parser, "1", false, REFERENCE);
        assertNextToken(parser, "]: ", false, REFERENCE);
        assertNextToken(parser, "<", false, REFERENCE);
        assertNextToken(parser, "http://google.com/", false, REFERENCE);
        assertNextToken(parser, ">", false, REFERENCE);
        assertNextToken(parser, NEWLINE, false, REFERENCE);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testSoftLineBreak() {
        parser.parse("This is" + NEWLINE + "a test");
        assertNextToken(parser, "This is" + NEWLINE + "a test", true, TEXT);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testStrongEmphasis1() {
        parser.parse("__Bold__");
        assertNextToken(parser, "__", false, STRONG_EMPHASIS);
        assertNextToken(parser, "Bold", true, TEXT);
        assertNextToken(parser, "__", false, STRONG_EMPHASIS);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testStrongEmphasis2() {
        parser.parse("**Bold**");
        assertNextToken(parser, "**", false, STRONG_EMPHASIS);
        assertNextToken(parser, "Bold", true, TEXT);
        assertNextToken(parser, "**", false, STRONG_EMPHASIS);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testThematicBreak() {
        parser.parse("Hello world\n\n---\n\nHello again world");
        assertNextToken(parser, "Hello world", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, "---", false, THEMATIC_BREAK);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, "Hello again world", true, TEXT);
        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testCommonMarkTokens() throws Exception {
        parser.parse(getFileContents("commonmark.md"));

        assertNextToken(parser, "## ", false, HEADING_PREFIX);
        assertNextToken(parser, "Try CommonMark", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertNextToken(parser, "You can try CommonMark here.  This dingus is powered by" + NEWLINE,
                true, TEXT);
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "commonmark.js", true, TEXT);
        assertNextToken(parser, "](https://github.com/jgm/commonmark.js)", false, LINK);
        assertNextToken(parser, ", the" + NEWLINE + "JavaScript reference implementation.", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertNextToken(parser, "1. ", false, ORDERED_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "item one", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "", false, LINE_PREFIX);

        assertNextToken(parser, "2. ", false, ORDERED_LIST_ITEM);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "item two", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);

        assertNextToken(parser, "- ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "      ", false, LINE_PREFIX);

        assertNextToken(parser, "sublist", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "- ", false, BULLET_LIST_ITEM);
        assertNextToken(parser, "      ", false, LINE_PREFIX);

        assertNextToken(parser, "sublist", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, "   ", false, LINE_PREFIX);
        assertNextToken(parser, "", false, LINE_PREFIX);

        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testTable1Tokens() throws Exception {
        parser.parse(getFileContents("table1.md"));

        // Table header row
        assertNextToken(parser, "| ", false, TABLE_PIPE);
        assertNextToken(parser, "Command", true, TEXT);
        assertNextToken(parser, " ", false, WHITE_SPACE);
        assertNextToken(parser, "| ", false, TABLE_PIPE);
        assertNextToken(parser, "Description", true, TEXT);
        assertNextToken(parser, " ", false, WHITE_SPACE);
        assertNextToken(parser, "|", false, TABLE_PIPE);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);

        // Table separator row
        assertNextToken(parser, "| --- | ---: |", false, TABLE_SEPARATOR);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);

        // Table body row
        assertNextToken(parser, "| ", false, TABLE_PIPE);
        assertNextToken(parser, "`", false, CODE);
        assertNextToken(parser, "git status", true, TEXT);
        assertNextToken(parser, "`", false, CODE);
        assertNextToken(parser, " ", false, WHITE_SPACE);
        assertNextToken(parser, "| ", false, TABLE_PIPE);
        assertNextToken(parser, "List all ", true, TEXT);
        assertNextToken(parser, "**", false, STRONG_EMPHASIS);
        assertNextToken(parser, "new", true, TEXT);
        assertNextToken(parser, "**", false, STRONG_EMPHASIS);
        assertNextToken(parser, " or ", true, TEXT);
        assertNextToken(parser, "_", false, EMPHASIS);
        assertNextToken(parser, "modified", true, TEXT);
        assertNextToken(parser, "_", false, EMPHASIS);
        assertNextToken(parser, " files", true, TEXT);
        assertNextToken(parser, " ", false, WHITE_SPACE);
        assertNextToken(parser, "|", false, TABLE_PIPE);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);

        assertFalse(parser.hasNextToken());
    }

    @Test
    public void testLinkWithText() throws Exception {
        parser.parse("Welcome to the [Okapi Framework](http://okapiframework.org/)");
        assertNextToken(parser, "Welcome to the ", true, TEXT);
        assertNextToken(parser, "[", false, LINK);
        assertNextToken(parser, "Okapi Framework", true, TEXT);
        assertNextToken(parser, "](http://okapiframework.org/)", false, LINK);
    }

    @Test
    public void testMathlm() throws Exception {
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
        parser.parse(snippet);
        assertNextToken(parser, "This contains a math block", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, mathBlock, true, MarkdownTokenType.HTML_BLOCK);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, "End of the math block", true, TEXT);
    }

    @Test
    public void testMathlmSingleLine() throws Exception {
        String snippet =
                "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"> <mstyle displaystyle=\"true\"> " +
                  "<mo> &#x2207;<!--nabla--> </mo><mo> &#x00D7;<!--multiplication sign--> </mo></mo> " +
                  "<mfenced> <mrow> " +
                    "<mover> <mrow> <mi> H </mi> </mrow> <mrow> <mo> &#x2192;<!--rightwards arrow--> </mo> </mrow> </mover> " +
                  "</mrow> </mfenced> " +
                  "<mo> = </mo> " +
                  "<mover> <mrow> <mi> J </mi> </mrow> <mrow> <mo> &#x2192;<!--rightwards arrow--> </mo> </mrow> </mover> " +
                  "<mo> + </mo> " +
                  "<mfrac> " +
                   "<mrow> " +
                    "<mi> d </mi> " +
                    "<mover> " +
                     "<mrow> <mi> D </mi> </mrow> " +
                     "<mrow> <mo> &#x2192;<!--rightwards arrow--> </mo> </mrow> " +
                   "</mover> </mrow> " +
                 "<mrow> <mo> &#x2202;<!--partial differential--> </mo> <mi> t </mi> </mrow> " +
                "</mfrac> " +
                "</mstyle></math>(from Ampere's law)";
        parser.parse(snippet);
        assertNextToken(parser, snippet, true, HTML_BLOCK);
    }

    @Test
    public void testMathlmInListItem() throws Exception {
        String ulMathBlock =
                "<ul>\n" +
                "<li><math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n" +
                "  <mstyle displaystyle=\"true\">\n" +
                "    <mover>\n" +
                "      <mrow>\n" +
                "        <mi> H </mi>\n" +
                "      </mrow>\n" +
                "      <mrow>\n" +
                "        <mo> &#x2192;<!--rightwards arrow--> </mo>\n" +
                "      </mrow>\n" +
                "    </mover>\n" +
                "  </mstyle></math> is the magnetic field intensity</li>\n" +
                "</ul>";
        String snippet = "This list contains a math block\n\n" +
                ulMathBlock;
        parser.parse(snippet);
        assertNextToken(parser, "This list contains a math block", true, TEXT);
        assertNextToken(parser, NEWLINE, false, SOFT_LINE_BREAK);
        assertNextToken(parser, NEWLINE, false, BLANK_LINE);
        assertNextToken(parser, ulMathBlock, true, HTML_BLOCK); // It's translatable because of the text after </math>.
    }

 
    
    private String getFileContents(String filename) throws Exception {
        try (InputStream is = FileLocation.fromClass(getClass()).in(filename).asInputStream();
                Scanner scanner = new Scanner(is)) {
            return scanner.useDelimiter("\\A").next().replaceAll(System.lineSeparator(), NEWLINE);
        }
    }

    private void assertNextToken(MarkdownParser parser, String content, boolean isTranslatable,
            MarkdownTokenType type) {
        assertTrue(parser.hasNextToken());
        MarkdownToken token = parser.getNextToken();
        assertEquals(content, token.getContent());
        assertEquals(isTranslatable, token.isTranslatable());
        assertEquals(type, token.getType());
    }

}
