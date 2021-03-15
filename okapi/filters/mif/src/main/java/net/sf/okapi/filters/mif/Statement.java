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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Identifies a statement.
 */
interface Statement extends Token, Iterable<Token> {

    Token firstTokenOf(final Token.Type type);
    Statement firstStatementWith(final String identity);
    List<Statement> statementsWith(final String identity);
    Statement.Type statementType();

    /**
     * A default statement.
     */
    class Default implements Statement {
        private final List<Token> tokens;
        private final Statement.Type type;

        Default(final List<Token> tokens, final Statement.Type type) {
            this.tokens = tokens;
            this.type = type;
        }

        Default(final Token token, final Statement.Type type) {
            this(Collections.singletonList(token), type);
        }

        @Override
        public Iterator<Token> iterator() {
            return tokens.iterator();
        }

        @Override
        public Token firstTokenOf(final Token.Type type) {
            return this.tokens.stream()
                .filter(t -> t.type() == type)
                .findFirst()
                .orElse(new Token.Default("", type));
        }

        @Override
        public Statement firstStatementWith(final String identity) {
            return this.statementsWith(identity).stream()
                .findFirst()
                .orElse(new Statement.Default(Collections.emptyList(), Statement.Type.EMPTY));
        }

        @Override
        public List<Statement> statementsWith(final String identity) {
            return this.tokens.stream()
                .filter(t -> t.type() == Token.Type.STATEMENT)
                .map(t -> (Statement) t)
                .filter(s -> s.firstTokenOf(Token.Type.IDENTITY).toString().equals(identity))
                .collect(Collectors.toList());
        }

        @Override
        public Statement.Type statementType() {
            return this.type;
        }

        @Override
        public Token.Type type() {
            return Token.Type.STATEMENT;
        }

        @Override
        public String toString() {
            return this.tokens.stream()
                .map(t -> t.toString())
                .collect(Collectors.joining());
        }
    }

    /**
     * Statement types.
     */
    enum Type {
        EMPTY,
        COMMENT,
        MACRO,
        MARKUP
    }
}
