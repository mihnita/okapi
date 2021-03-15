/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.skeleton.ZipSkeleton;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provide a markup zip skeleton.
 */
class MarkupZipSkeleton extends ZipSkeleton {
    private final XMLOutputFactory outputFactory;
    private final String encoding;
    private final Markup markup;

    MarkupZipSkeleton(
        final ZipFile original,
        final ZipEntry entry,
        final XMLOutputFactory outputFactory,
        final String encoding,
        final Markup markup
    ) {
        super(original, entry);
        this.outputFactory = outputFactory;
        this.encoding = encoding;
        this.markup = markup;
    }

    Markup getMarkup() {
        return markup;
    }

    public String getModifiedContents() {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final XMLEventWriter writer = this.outputFactory.createXMLEventWriter(outputStream);
            for (final XMLEvent event : this.markup.getEvents()) {
                writer.add(event);
            }
            writer.close();
            return outputStream.toString(this.encoding);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Markup writing failed", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unexpected encoding provided: ".concat(this.encoding), e);
        }
    }
}
