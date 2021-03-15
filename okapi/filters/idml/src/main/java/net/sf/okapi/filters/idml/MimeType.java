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

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;

class MimeType {
    private final ZipInput<InputStream> zipInputStream;
    private ByteArrayOutputStream outputStream;

    MimeType(final ZipInput<InputStream> zipInputStream) {
        this.zipInputStream = zipInputStream;
    }

    void from(final ZipEntry entry) throws IOException, XMLStreamException {
        final InputStream inputStream = this.zipInputStream.of(entry);
        this.outputStream = new ByteArrayOutputStream(IDMLFilter.MIME_TYPE.length());

        int result;
        while (-1 != (result = inputStream.read())) {
            this.outputStream.write((byte) result);
        }
    }

    String toString(final String charsetName) throws UnsupportedEncodingException {
        return this.outputStream.toString(charsetName);
    }
}
