/*
 * =============================================================================
 *   Copyright (C) 2010-2013 by the Okapi Framework contributors
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

package net.sf.okapi.filters.idml;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.skeleton.ZipSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class NonModifiableSubDocument implements SubDocument {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ZipFile zipFile;
    private final ZipEntry zipEntry;

    NonModifiableSubDocument(ZipFile zipFile, ZipEntry zipEntry) {
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
    }

    @Override
    public Event open() throws IOException, XMLStreamException {
        DocumentPart documentPart = new DocumentPart(zipEntry.getName(), false);
        ZipSkeleton skeleton = new ZipSkeleton(zipFile, zipEntry);

        return new Event(EventType.DOCUMENT_PART, documentPart, skeleton);
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
        logger.trace("[[ {}: {} ]]", getClass().getSimpleName(), zipEntry.getName());
    }
}

