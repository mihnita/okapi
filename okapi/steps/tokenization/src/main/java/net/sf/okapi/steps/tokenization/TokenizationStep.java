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

package net.sf.okapi.steps.tokenization;

import static net.sf.okapi.common.LocaleId.FRENCH;
import static net.sf.okapi.common.LocaleId.ITALIAN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ListUtil;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextUnitUtil;

@UsingParameters(Parameters.class)
public class TokenizationStep extends BasePipelineStep {
    private static final Pattern APOSTROPHE = Pattern.compile("[\u2019\u0027]");
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ITokenizer tokenizer;
    private final ArrayList<Integer> positions;
    private final Parameters params;
    private LocaleId targetLocale;
    private LocaleId sourceLocale;

    public TokenizationStep() {
        super();
        params = new Parameters();
        setParameters(params);
        tokenizer = new RbbiTokenizer();
        positions = new ArrayList<>();
    }

    @Override
    protected Event handleStartDocument(Event event) {
        StartDocument sd = (StartDocument) event.getResource();
        if (sd != null) {
            sourceLocale = sd.getLocale();
        }
        return event;
    }

    @Override
    protected Event handleTextUnit(Event event) {
        event = super.handleTextUnit(event);
        if (event == null) {
            return null;
        }

        ITextUnit tu = event.getTextUnit();
        if (tu == null) {
            return event;
        }

        if (tu.isEmpty()) {
            return event;
        }
        if (!tu.isTranslatable()) {
            return event;
        }

		if (params.isTokenizeSource()) {
            tokenizeSource(tu);
        }
		if (params.isTokenizeTargets()) {
            tokenizeTargets(tu);
        }
        return event;
    }

    @Override
    public LocaleId getSourceLocale() {
        return sourceLocale;
    }

    @Override
    @StepParameterMapping(parameterType = StepParameterType.SOURCE_LOCALE)
    public void setSourceLocale(LocaleId sourceLocale) {
        this.sourceLocale = sourceLocale;
    }

    @Override
    public LocaleId getTargetLocale() {
        return targetLocale;
    }

    @Override
    @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
    public void setTargetLocale(LocaleId targetLocale) {
        this.targetLocale = targetLocale;
    }

    private Tokens tokenize(TextContainer tc, LocaleId language) {
        if (tc == null) {
            return null;
        }
        if (Util.isNullOrEmpty(language)) {
            return null;
        }

        if (positions == null) {
            return null;
        }
        positions.clear();

        Tokens tokens = new Tokens();

        // Remove codes, store to positions
        String text;
        if (tc.contentIsOneSegment()) {
            text = TextUnitUtil.getText(tc.getFirstContent(), positions);
        } else {
            text = TextUnitUtil.getText(tc.getUnSegmentedContentCopy(), positions);
        }

        tokenizer.init(text, language);
        while (tokenizer.hasNext()) {
            Token t = tokenizer.next();
            if (t != null) {
                tokens.addAll(postProcess(t, language));
            }
        }

        // Restore codes from positions
        tokens.fixRanges(positions);
        return tokens.getFilteredList(ListUtil.stringListAsArray(params.getIncludedTokenNames()));
    }

    /**
     * Various rules to make corrections to {@link RbbiTokenizer}
     *
     * @param t the {@link Token}
     * @return list of correct tokens or the original token if no changes were made
     */
    public Collection<? extends Token> postProcess(Token t, LocaleId language) {
        List<Token> tokens = new ArrayList<>();
        tokens.add(t);
        if (FRENCH.sameLanguageAs(language) || ITALIAN.sameLanguageAs(language)) {
            if (APOSTROPHE.matcher(t.getValue()).find()) {
                return apostrophe(t, language);
            }
        }

        // unaltered token
        return tokens;
    }

    /**
     * Break French and Italian words with apostrophe into three tokens WORD, PUNCTUATION, WORD
     *
     * @param token
     * @return list of transformed tokens if any
     */
    public List<Token> apostrophe(Token token, LocaleId locale) {
        List<Token> tokens;
        Matcher matcher = APOSTROPHE.matcher(token.getValue());
        matcher.find();
        int s = token.getRange().start;
        int e = token.getRange().end;

        tokens = new ArrayList<>();
        String[] words = APOSTROPHE.split(token.getValue());

        String value = words[0];
        String name = Tokens.getTokenName(token.getId());
        String description = Tokens.getTokenDescription(token.getId());
        int word1End = s + value.length();
        Token t = new Token(token.getId(), value, name, description, s, word1End);
        tokens.add(t);

        value = matcher.group();
        name = "PUNCTUATION";
        int id = Tokens.getTokenId(name);
        description = Tokens.getTokenDescription(id);
        t = new Token(id, value, name, description, word1End + 1, word1End + 2);
        tokens.add(t);

        value = words[1];
        name = Tokens.getTokenName(token.getId());
        description = Tokens.getTokenDescription(token.getId());
        t = new Token(token.getId(), value, name, description, word1End + 3, e);
        tokens.add(t);

        return tokens;
    }

    private void tokenizeSource(ITextUnit tu) {
        if (tu == null) {
            return;
        }

        Tokens tokens = tokenize(tu.getSource(), getSourceLocale());
        if (tokens == null) {
            return;
        }

        // Attach to TU
        TokensAnnotation ta = TextUnitUtil.getSourceAnnotation(tu, TokensAnnotation.class);

        if (ta == null) {
            TextUnitUtil.setSourceAnnotation(tu, new TokensAnnotation(tokens));
        } else {
            ta.addTokens(tokens);
        }
    }

    private void tokenizeTargets(ITextUnit tu) {
        if (tu == null) {
            return;
        }

        for (LocaleId language : tu.getTargetLocales()) {

            Tokens tokens = tokenize(tu.getTarget(language), language);
            if (tokens == null) {
                continue;
            }

            // Attach to TU
            TokensAnnotation ta = TextUnitUtil.getTargetAnnotation(tu, language, TokensAnnotation.class);

            if (ta == null) {
                TextUnitUtil.setTargetAnnotation(tu, language, new TokensAnnotation(tokens));
            } else {
                ta.addTokens(tokens);
            }
        }
    }

    @Override
    public String getName() {
        return "Tokenization Step";
    }

    @Override
    public String getDescription() {
        return "Extracts tokens from the text units content of a document. " + "Expects: filter events. Sends back: " +
                "filter events.";
    }
}
