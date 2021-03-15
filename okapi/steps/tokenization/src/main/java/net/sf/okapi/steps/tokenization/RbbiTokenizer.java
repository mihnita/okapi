/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.StringUtil;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.TreeMap;

public class RbbiTokenizer implements ITokenizer {
    private static final String RBBI_RULES_NAME = "okapi~rbbi";
    private static final String RBBI_RULES_SUFFIX = ".brk";

    // Cache for iterators reuse
    private final TreeMap<LocaleId, RuleBasedBreakIterator> iterators = new TreeMap<>();

    private RuleBasedBreakIterator iterator = null;
    private LocaleId language;
    private int start;
    private int end;
    private String text;

    public RbbiTokenizer() {
    }

    @Override
    public boolean hasNext() {
        return end != BreakIterator.DONE;
    }

    @Override
	public net.sf.okapi.steps.tokenization.Token next() {
        end = iterator.next();
        if (end == BreakIterator.DONE) {
            return null;
        }
        if (start >= end) {
            return null;
        }

        // get token id from RBBI
        int tokenId = iterator.getRuleStatus();
        String value = text.substring(start, end);
        String name = Tokens.getTokenName(tokenId);
        String description = Tokens.getTokenDescription(tokenId);
		net.sf.okapi.steps.tokenization.Token token = new net.sf.okapi.steps.tokenization.Token(tokenId, value, name,
				description, start, end);
        // Prepare for the next iteration
        start = end;

        return token;
    }

    @Override
    public void init(String text, LocaleId language) {
        this.language = language;
        this.text = text;
        if (Util.isEmpty(this.text)) {
            return;
        }

        if (iterators.containsKey(language)) {
            iterator = iterators.get(language);
        } else {
            try {
                // Ideally we should compile the rule file outside and store it, but
                // this way you don't have to worry about ICU version compatibility
                // and running ICU4C genbrk tool
                Path temp = Files.createTempFile(RBBI_RULES_NAME, RBBI_RULES_SUFFIX);
                String rules = StringUtil.readString(RbbiTokenizer.class.getResource("/rbbi.txt"));
                OutputStream out = Files.newOutputStream(temp, StandardOpenOption.CREATE);
                RuleBasedBreakIterator.compileRules(rules, out);
                InputStream is = Files.newInputStream(temp);
                iterator = RuleBasedBreakIterator.getInstanceFromCompiledRules(is);
            } catch (IOException | NullPointerException e) {
                throw new OkapiBadFilterInputException("Cannot load compiled break rules.", e);
            }
            iterators.put(language, iterator);
        }

        if (iterator == null) {
            return;
        }
        iterator.setText(this.text);

        // Sets the current iteration position to the beginning of the text
        start = iterator.first();
        end = start;
    }
}
