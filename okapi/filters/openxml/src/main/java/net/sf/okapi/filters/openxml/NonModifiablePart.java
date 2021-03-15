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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.skeleton.ZipSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.zip.ZipEntry;

class NonModifiablePart extends NonTranslatablePart {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final Document.General generalDocument;
    private final ZipEntry entry;

    NonModifiablePart(final Document.General generalDocument, final ZipEntry entry) {
        this.generalDocument = generalDocument;
        this.entry = entry;
    }

    @Override
    public Event open() throws IOException, XMLStreamException {
        DocumentPart dp = new DocumentPart(this.entry.getName(), false);
        ZipSkeleton skel = new ZipSkeleton(this.generalDocument.zipFile(), this.entry);
        return new Event(EventType.DOCUMENT_PART, dp, skel);
    }

    @Override
    public void logEvent(Event e) {
        LOGGER.trace("[[ {}: {} ]]", getClass().getSimpleName(), entry.getName());
    }
}
