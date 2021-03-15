/*
 * =============================================================================
 *   Copyright (C) 2010-2020 by the Okapi Framework contributors
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
package net.sf.okapi.filters.mif;

import net.sf.okapi.common.exceptions.OkapiIllegalFilterOperationException;
import net.sf.okapi.common.exceptions.OkapiNotImplementedException;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

final class Statements {

    private static final String UNEXPECTED_END_OF_STREAM = "Unexpected end of stream.";

    private static final char COMMENT_START = '#';
    private static final char MARKUP_START = '<';
    private static final char MARKUP_END = '>';
    private static final char MACRO_DEFINE_START = 'd';
    private static final char MACRO_INCLUDE_START = 'i';
    private static final char MARKUP_STRING_LITERAL_START = '`';

    private static final Set<Character> COMMENT_OR_MARKUP_STARTS = new HashSet<>(
        Arrays.asList(COMMENT_START, MARKUP_START)
    );
    private static final Set<String> MACRO_STARTS = new HashSet<>(
        Arrays.asList("define", "include")
    );
    private static final Set<Character> WHITESPACES = new HashSet<>(Arrays.asList(' ', '\t', '\r', '\n'));
    private static final Set<Character> COMMENT_ENDS = new HashSet<>(Arrays.asList('\r', '\n'));
    private static final Set<Character> MACRO_LITERAL_ENDS = Collections.singleton(')');
    private static final Set<Character> MARKUP_STRING_LITERAL_ENDS = Collections.singleton('\'');
    private static final Set<Character> WHITESPACES_AND_MARKUP_ENDS = new HashSet<>(WHITESPACES);
    static {
        WHITESPACES_AND_MARKUP_ENDS.add(MARKUP_END);
    }

    private static final String IMPORT_OBJECT = "ImportObject";
    private static final char IMPORT_OBJECT_FACET_START = '=';
    private static final String IMPORT_OBJECT_FACET_END = "=EndInset";
    private static final Set<Character> IMPORT_OBJECT_FACET_DATA_ENDS = COMMENT_ENDS;

    private final Reader reader;

    Statements(final Reader reader) {
        this.reader = reader;
    }

    Statement partialMarkup() throws IOException {
        final List<Token> tokens = new LinkedList<>();
        final StringBuilder sb = new StringBuilder();
        char ch;
        while (true) {
            ch = readWhileCharactersPresent(sb, WHITESPACES);
            tokens.add(new Token.Default(sb, Token.Type.WHITESPACE));
            if (MARKUP_END == ch) {
                tokens.add(new Token.Default(ch, Token.Type.END));
                break;
            }
            if (MARKUP_STRING_LITERAL_START == ch) {
                tokens.add(new Token.Default(ch, Token.Type.START));
                sb.setLength(0);
                ch = readWhileNoCharactersPresent(sb, MARKUP_STRING_LITERAL_ENDS);
                tokens.add(new Token.Default(sb, Token.Type.LITERAL));
                tokens.add(new Token.Default(ch, Token.Type.END));
                sb.setLength(0);
                continue;
            }
            while (true) {
                sb.setLength(0);
                sb.append(ch);
                ch = readWhileNoCharactersPresent(sb, WHITESPACES_AND_MARKUP_ENDS);
                tokens.add(new Token.Default(sb, Token.Type.LITERAL));
                if (MARKUP_END == ch) {
                    tokens.add(new Token.Default(ch, Token.Type.END));
                    break;
                }
                sb.setLength(0);
                sb.append(ch);
                ch = readWhileCharactersPresent(sb, WHITESPACES);
                tokens.add(new Token.Default(sb, Token.Type.WHITESPACE));
            }
            break;
        }
        return new Statement.Default(tokens, Statement.Type.MARKUP);
    }

    Statement empty(final StringBuilder stringBuilder) {
        return new Statement.Default(
            new Token.Default(stringBuilder.toString(), Token.Type.WHITESPACE),
            Statement.Type.EMPTY
        );
    }

    Statement fromFirstCharacter(final char firstCharacter) throws IOException {
        switch (firstCharacter) {
            case COMMENT_START:
                return comment(firstCharacter);
            case MACRO_DEFINE_START:
            case MACRO_INCLUDE_START:
                return macro(firstCharacter);
            case MARKUP_START:
                return markup(firstCharacter);
            default:
                throw new OkapiNotImplementedException("An unsupported statement starts with '" + firstCharacter + "'");
        }
    }

    private Statement comment(final char firstCharacter) throws IOException {
        final List<Token> tokens = new LinkedList<>();
        tokens.add(new Token.Default(firstCharacter, Token.Type.START));

        final StringBuilder sb = new StringBuilder();
        final char ch = readWhileNoCharactersPresent(sb, COMMENT_ENDS);

        tokens.add(new Token.Default(sb, Token.Type.LITERAL));
        tokens.add(new Token.Default(ch, Token.Type.END));

        return new Statement.Default(tokens, Statement.Type.COMMENT);
    }

    private Statement macro(final char firstCharacter) throws IOException {
        final StringBuilder sb = new StringBuilder();

        sb.append(firstCharacter);
        char ch = readWhileNoCharactersPresent(sb, WHITESPACES);

        return macro(sb, ch);
    }

    private Statement macro(final StringBuilder sb, final char nextCharacter) throws IOException {
        final List<Token> tokens = new LinkedList<>();
        tokens.add(new Token.Default(sb, Token.Type.IDENTITY));

        sb.setLength(0);
        sb.append(nextCharacter);
        char ch = readWhileCharactersPresent(sb, WHITESPACES);
        tokens.add(new Token.Default(sb, Token.Type.WHITESPACE));

        sb.setLength(0);
        tokens.add(new Token.Default(ch, Token.Type.START));
        ch = readWhileNoCharactersPresent(sb, MACRO_LITERAL_ENDS);
        tokens.add(new Token.Default(sb, Token.Type.LITERAL));
        tokens.add(new Token.Default(ch, Token.Type.END));

        return new Statement.Default(tokens, Statement.Type.MACRO);
    }

    private Statement markup(final char firstCharacter) throws IOException {
        final List<Token> tokens = new LinkedList<>();
        tokens.add(new Token.Default(firstCharacter, Token.Type.START));

        final StringBuilder sb = new StringBuilder();
        char ch;
        boolean importObject = false;
        while (true) {
            ch = readWhileCharactersPresent(sb, WHITESPACES);
            tokens.add(new Token.Default(sb, Token.Type.WHITESPACE));

            if (MARKUP_END == ch) {
                tokens.add(new Token.Default(ch, Token.Type.END));
                break;
            }
            if (COMMENT_OR_MARKUP_STARTS.contains(ch)) {
                tokens.add(fromFirstCharacter(ch));
                sb.setLength(0);
                continue;
            }
            sb.setLength(0);
            sb.append(ch);
            ch = readWhileNoCharactersPresent(sb, WHITESPACES);

            if (MACRO_STARTS.contains(sb.toString())) {
                tokens.add(macro(sb, ch));
                sb.setLength(0);
                continue;
            }
            if (importObject && IMPORT_OBJECT_FACET_START == sb.charAt(0)) {
                tokens.addAll(importObjectFacets(sb, ch));
                sb.setLength(0);
                continue;
            }
            if (tokens.stream().map(t -> t.type()).anyMatch(v -> v == Token.Type.IDENTITY)) {
                tokens.add(new Token.Default(sb, Token.Type.LITERAL));
            } else {
                tokens.add(new Token.Default(sb, Token.Type.IDENTITY));
                if (IMPORT_OBJECT.equals(sb.toString())) {
                    importObject = true;
                }
            }
            sb.setLength(0);
            sb.append(ch);
            ch = readWhileCharactersPresent(sb, WHITESPACES);
            tokens.add(new Token.Default(sb, Token.Type.WHITESPACE));

            if (MARKUP_END == ch) {
                tokens.add(new Token.Default(ch, Token.Type.END));
                break;
            }
            if (COMMENT_OR_MARKUP_STARTS.contains(ch)) {
                tokens.add(fromFirstCharacter(ch));
                sb.setLength(0);
                continue;
            }
            if (MARKUP_STRING_LITERAL_START == ch) {
                sb.setLength(0);
                tokens.add(new Token.Default(ch, Token.Type.START));
                ch = readWhileNoCharactersPresent(sb, MARKUP_STRING_LITERAL_ENDS);
                tokens.add(new Token.Default(sb, Token.Type.LITERAL));
                tokens.add(new Token.Default(ch, Token.Type.END));
                sb.setLength(0);
                continue;
            }
            while (true) {
                sb.setLength(0);
                sb.append(ch);
                ch = readWhileNoCharactersPresent(sb, WHITESPACES_AND_MARKUP_ENDS);
                tokens.add(new Token.Default(sb, Token.Type.LITERAL));
                if (MARKUP_END == ch) {
                    tokens.add(new Token.Default(ch, Token.Type.END));
                    break;
                }
                sb.setLength(0);
                sb.append(ch);
                ch = readWhileCharactersPresent(sb, WHITESPACES);
                tokens.add(new Token.Default(sb, Token.Type.WHITESPACE));
                if (MARKUP_END == ch) {
                    tokens.add(new Token.Default(ch, Token.Type.END));
                    break;
                }
            }
            break;
        }
        return new Statement.Default(tokens, Statement.Type.MARKUP);
    }

    private List<Token> importObjectFacets(final StringBuilder sb, final char nextCharacter) throws IOException {
        final List<Token> tokens = new LinkedList<>();
        tokens.add(new Token.Default(sb, Token.Type.START));
        sb.setLength(0);
        sb.append(nextCharacter);

        char ch;
        while (true) {
            ch = readWhileCharactersPresent(sb, IMPORT_OBJECT_FACET_DATA_ENDS);
            tokens.add(new Token.Default(sb, Token.Type.WHITESPACE));
            sb.setLength(0);
            sb.append(ch);
            ch = readWhileNoCharactersPresent(sb, IMPORT_OBJECT_FACET_DATA_ENDS);
            if (IMPORT_OBJECT_FACET_END.equals(sb.toString())) {
                tokens.add(new Token.Default(sb, Token.Type.END));
                tokens.add(new Token.Default(ch, Token.Type.WHITESPACE));
                break;
            }
            tokens.add(
                new Token.Default(
                    sb,
                    IMPORT_OBJECT_FACET_START == sb.charAt(0) ? Token.Type.START : Token.Type.LITERAL
                )
            );
            sb.setLength(0);
            sb.append(ch);
        }
        return tokens;
    }

    private char readWhileNoCharactersPresent(final StringBuilder stringBuilder, final Set<Character> delimiters) throws IOException {
        int ch;
        while (true) {
            ch = this.reader.read();
            if (-1 == ch) {
                throw new OkapiIllegalFilterOperationException(UNEXPECTED_END_OF_STREAM);
            }
            if (!delimiters.contains((char) ch)) {
                stringBuilder.append((char) ch);
                continue;
            }
            break;
        }
        return (char) ch;
    }

    private char readWhileCharactersPresent(final StringBuilder stringBuilder, final Set<Character> delimiters) throws IOException {
        int ch;
        while (true) {
            ch = this.reader.read();
            if (-1 == ch) {
                throw new OkapiIllegalFilterOperationException(UNEXPECTED_END_OF_STREAM);
            }
            if (delimiters.contains((char) ch)) {
                stringBuilder.append((char) ch);
                continue;
            }
            break;
        }
        return (char) ch;
    }
}
