/*
 * =============================================================================
 *   Copyright (C) 2010-2019 by the Okapi Framework contributors
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
package net.sf.okapi.filters.openxml;

import javax.xml.XMLConstants;

interface Namespace {
    String PREFIX_EMPTY = XMLConstants.DEFAULT_NS_PREFIX;
    String PREFIX_A = "a";
    String PREFIX_P = "p";
    String PREFIX_R = "r";
    String PREFIX_W = "w";

    String EMPTY = XMLConstants.NULL_NS_URI;
    String DOCUMENT_RELATIONSHIPS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    String STRICT_DOCUMENT_RELATIONSHIPS = "http://purl.oclc.org/ooxml/officeDocument/relationships";
    String VISIO_DOCUMENT_RELATIONSHIPS = "http://schemas.microsoft.com/visio/2010/relationships";

    String prefix();
    String uri();

    class Default implements Namespace {
        private final String prefix;
        private final String uri;

        Default(final String prefix, final String uri) {
            this.prefix = prefix;
            this.uri = uri;
        }

        Default(final String uri) {
            this(PREFIX_EMPTY, uri);
        }

        Default() {
            this(PREFIX_EMPTY, EMPTY);
        }

        @Override
        public String prefix() {
            return this.prefix;
        }

        @Override
        public String uri() {
            return this.uri;
        }
    }
}
