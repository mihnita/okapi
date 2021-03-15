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

import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiIOException;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies a MIF document.
 */
interface Document extends Iterator<Statement> {
    /**
     * Assembles a markup statement from the current reader position, when the start
     * and identifier tokens have been already read.
     * @todo Remove this temporary solution in favour of Iterator<Statement> methods
     * @return An incomplete statement (without start and identifier tokens)
     */
    Statement currentMarkup();

    /**
     * A default implementation of the document.
     * @todo Accomplish the implementation and use it for traversing through the document
     */
    class Default implements Document {
        private static final Set<Character> WHITESPACES = new HashSet<>(Arrays.asList(' ', '\t', '\r', '\n'));

        private final Statements statements;
        private final Reader reader;
        private final Queue<Statement> readStatements;

        Default(final Statements statements, final Reader reader, final Queue<Statement> readStatements) {
            this.statements = statements;
            this.reader = reader;
            this.readStatements = readStatements;
        }

        @Override
        public boolean hasNext() {
            if (null != this.readStatements.peek()) {
                return true;
            }
            try {
                final StringBuilder sb = new StringBuilder();
                int ch;
                while (true) {
                    ch = reader.read();
                    if (-1 == ch) {
                        if (0 == sb.length()) {
                            return false;
                        } else {
                            this.readStatements.add(this.statements.empty(sb));
                            return true;
                        }
                    }
                    if (WHITESPACES.contains((char) ch)) {
                        sb.append((char) ch);
                        continue;
                    }
                    break;
                }
                if (0 != sb.length()) {
                    this.readStatements.add(this.statements.empty(sb));
                }
                this.readStatements.add(this.statements.fromFirstCharacter((char) ch));
                return true;
            } catch (IOException e) {
                throw new OkapiIOException("I/O error at reading: ".concat(e.getMessage()), e);
            }
        }

        @Override
        public Statement next() {
            return this.readStatements.poll();
        }

        @Override
        public Statement currentMarkup() {
            try {
                return this.statements.partialMarkup();
            } catch (IOException e) {
                throw new OkapiIOException("I/O error at reading: ".concat(e.getMessage()), e);
            }
        }
    }

    final class Version {
        private static final String UNSUPPORTED_DOCUMENT_VERSION = "Unsupported document version: ";
        /**
         * Older versions: NN.00
         * Newer versions: 2015
         */
        private static Pattern PATTERN = Pattern.compile("^(\\d+\\.?\\d{0,2})");
        private static final double MIN_SUPPORTED_VERSION = 8.0;
        private final String value;

        Version(final String value) {
            this.value = value;
        }

        void validate() {
            final Matcher m = PATTERN.matcher(this.value);
            if (!m.lookingAt()) {
                throw new OkapiBadFilterInputException(UNSUPPORTED_DOCUMENT_VERSION.concat(this.value));
            }
            if (Double.valueOf(this.value) < MIN_SUPPORTED_VERSION) {
                throw new OkapiBadFilterInputException(UNSUPPORTED_DOCUMENT_VERSION.concat(this.value));
            }
        }
    }
}
