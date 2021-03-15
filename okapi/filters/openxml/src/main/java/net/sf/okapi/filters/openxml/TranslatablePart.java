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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.skeleton.ZipSkeleton;

import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

abstract class TranslatablePart implements Part {
    protected final Document.General generalDocument;
    protected final ZipEntry entry;
    final List<Event> filterEvents;
    final IdGenerator documentPartIdGenerator;

    List<XMLEvent> documentPartEvents;

    TranslatablePart(Document.General generalDocument, ZipEntry entry) {
        this.generalDocument = generalDocument;
        this.entry = entry;
        this.filterEvents = new ArrayList<>();
        this.documentPartIdGenerator = new IdGenerator(entry.getName(), IdGenerator.DOCUMENT_PART);

        this.documentPartEvents = new ArrayList<>();
    }

    Event createStartSubDocumentEvent(String documentId, String subDocumentId) {
        StartSubDocument sd = new StartSubDocument(documentId, subDocumentId);
        sd.setName(this.entry.getName());
        if (generalDocument != null) { // XXX This null check is a hack for testing
            ZipSkeleton zs = new ZipSkeleton(generalDocument.zipFile(), entry);
            sd.setSkeleton(zs);
        }
        sd.setFilterId(OpenXMLFilter.FILTER_ID);
        sd.setFilterParameters(generalDocument.conditionalParameters());
        return new Event(EventType.START_SUBDOCUMENT, sd);
    }

    void addEventToDocumentPart(XMLEvent e) {
        documentPartEvents.add(e);
    }
}
