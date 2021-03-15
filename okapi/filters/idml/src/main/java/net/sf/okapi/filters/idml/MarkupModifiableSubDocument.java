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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.skeleton.ZipSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class MarkupModifiableSubDocument implements SubDocument {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final ZipFile zipFile;
    private final ZipEntry entry;
    private final XMLOutputFactory outputFactory;
    private final String encoding;
    private final Markup markup;

    MarkupModifiableSubDocument(
        final ZipFile zipFile,
        final ZipEntry entry,
        final XMLOutputFactory outputFactory,
        final String encoding,
        final Markup markup
    ) {
        this.zipFile = zipFile;
        this.entry = entry;
        this.outputFactory = outputFactory;
        this.encoding = encoding;
        this.markup = markup;
    }

    @Override
    public Event open() throws IOException, XMLStreamException {
        final DocumentPart dp = new DocumentPart(this.entry.getName(), false);
        final ZipSkeleton skel = new MarkupZipSkeleton(
            this.zipFile,
            this.entry,
            this.outputFactory,
            this.encoding,
            this.markup
        );
        return new Event(EventType.DOCUMENT_PART, dp, skel);
    }

    @Override
    public boolean hasNextEvent() {
        return false;
    }

    @Override
    public Event nextEvent() {
        throw new IllegalStateException();
    }

    @Override
    public void close() {
    }

    @Override
    public void logEvent(Event e) {
        LOGGER.trace("[[ {}: {} ]]", getClass().getSimpleName(), entry.getName());
    }
}
