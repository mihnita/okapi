/*
 * =============================================================================
 * Copyright (C) 2010-2020 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================================
 */
package net.sf.okapi.filters.idml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provides a zip input.
 */
interface ZipInput<T> {
    T of(final ZipEntry zipEntry) throws IOException, XMLStreamException;

    class Stream implements ZipInput<InputStream> {
        private final ZipFile zipFile;

        Stream(final ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        @Override
        public InputStream of(final ZipEntry entry) throws IOException, XMLStreamException {
            if (null == entry) {
                throw new IllegalArgumentException("The provided zip entry is null");
            }
            return new BufferedInputStream(zipFile.getInputStream(entry));
        }
    }

    class Reader implements ZipInput<XMLEventReader> {
        private final ZipInput<InputStream> zipInputStream;
        private final String encoding;
        private final XMLInputFactory inputFactory;

        Reader(
            final ZipInput<InputStream> zipInputStream,
            final String encoding,
            final XMLInputFactory inputFactory
        ) {
            this.zipInputStream = zipInputStream;
            this.encoding = encoding;
            this.inputFactory = inputFactory;
        }

        @Override
        public XMLEventReader of(final ZipEntry entry) throws IOException, XMLStreamException {
            return this.inputFactory.createXMLEventReader(
                new InputStreamReader(
                    this.zipInputStream.of(entry),
                    this.encoding
                )
            );
        }
    }
}
