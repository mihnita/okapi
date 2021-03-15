/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

// This test case uses parts of the code presented by Sujit Pal at http://sujitpal.blogspot.com/2008/05/tokenizing-text-with-icu4js.html

package net.sf.okapi.steps.tokenization;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Range;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnitUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

@RunWith(JUnit4.class)
public class TokenizationTest {
    private final LocaleId locENUS = LocaleId.fromString("en-us");
    private final LocaleId locFR = LocaleId.fromString("fr");
    private TokenizationStep ts;
    private Tokens tokens;
    private final String text = "Jaguar \uD83D\uDC7D will sell its new XJ-6 model in the U.S. for " +
            "a small fortune :-). Expect to pay around USD 120ks ($120,000.00 on 05/30/2007 at 12.30PM). Custom options " +
            "can set you back another few 10,000 dollars. For details, go to " +
            "<a href=\"http://www.jaguar.com/sales\" alt=\"Click here\">" +
            "Jaguar Sales</a> or contact xj-6@jaguar.com."+
            " See http://www.jaguar.com/sales, www.jaguar.com, AT&T, P&G, Johnson&Johnson, 192.168.0.5 for info 3.5pct.";

    private Tokens tokenizeText() {
        Tokens res = new Tokens();
        ts.handleEvent(new Event(EventType.START_BATCH)); // Calls component_init();

        StartDocument startDoc = new StartDocument("tokenization");
        startDoc.setLocale(locENUS);
        startDoc.setMultilingual(false);
        Event event = new Event(EventType.START_DOCUMENT, startDoc);
        ts.handleEvent(event);

        ITextUnit tu = TextUnitUtil.buildGenericTU(text);
        event = new Event(EventType.TEXT_UNIT, tu);
        ts.handleEvent(event);

        // Move tokens from the event's annotation to result
        TokensAnnotation ta = TextUnitUtil.getSourceAnnotation(tu, TokensAnnotation.class);
        if (ta != null) {
            res.addAll(ta.getTokens());
        }

        ts.handleEvent(new Event(EventType.END_BATCH)); // Calls component_done();
        return res;
    }

    @Before
    public void setUp() {
        ts = new TokenizationStep();
    }

    @Test
    public void testTS() {
        ts = new TokenizationStep();

        ITextUnit tu = TextUnitUtil.buildGenericTU(text);
        Event event = new Event(EventType.TEXT_UNIT, tu);

        ts.handleEvent(new Event(EventType.START_BATCH));
        ts.handleEvent(event);
        ts.handleEvent(new Event(EventType.END_BATCH));
    }

    @Test
    public void listTokenizerOutput() {
        // All tokens
        Tokens tokens =
                Tokenizer.tokenize("NASDAQ :-) hypen-word www.google.com is a U.S. stock 1.0006 100 exchange" + ".",
                        locENUS);
        assertEquals(22, tokens.size());
    }

    @Test
    public void testTokenizer1() {
        Tokens tokens = Tokenizer.tokenize(text, locENUS);
        assertEquals(132, tokens.size());
        assertEquals("Jaguar", tokens.get(0).getValue());
        assertEquals(" ", tokens.get(1).getValue());
        assertEquals("\uD83D\uDC7D", tokens.get(2).getValue());
    }

    @Test
    public void testTokenizer2() {
        Tokens tokens = Tokenizer.tokenize("word word word", locENUS, "WORD");
        assertEquals(3, tokens.size());
        assertEquals("word", tokens.get(0).getValue());
        assertEquals("word", tokens.get(1).getValue());
        assertEquals("word", tokens.get(2).getValue());
    }

    @Test
    public void hyphenatedWords() {
        Tokens tokens = Tokenizer.tokenize("word-word-word", locENUS, "HYPHENATED_WORD");
        assertEquals(1, tokens.size());
        assertEquals("word-word-word", tokens.get(0).getValue());
    }

    @Test
    public void allTokens() {
        Tokens tokens = Tokenizer.tokenize("12:00pm 03/12/192 11:45 $300", locENUS);
        assertEquals(11, tokens.size());
    }

    @Test
    public void testRange() {
        Range r1 = new Range(1, 5);
        Range r2 = new Range(1, 5);
        assertNotSame(r1, r2);
        assertNotEquals(r1, r2);
        assertNotEquals(r1.hashCode(), r2.hashCode());
        assertNotSame(r1.toString(), r2.toString());
    }
}
