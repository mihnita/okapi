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
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;

import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static net.sf.okapi.filters.openxml.Relationships.mapRelsToTargets;

class VisioDocument implements Document {

    private static final String MASTERS = Namespaces.VisioDocumentRelationships.getDerivedURI("/masters");
    private static final String PAGES = Namespaces.VisioDocumentRelationships.getDerivedURI("/pages");

    private static final String MASTER = Namespaces.VisioDocumentRelationships.getDerivedURI("/master");
    private static final String PAGE = Namespaces.VisioDocumentRelationships.getDerivedURI("/page");

    private final Document.General generalDocument;
    private Enumeration<? extends ZipEntry> entries;
    private List<String> mastersAndPages;

    VisioDocument(final Document.General generalDocument) {
        this.generalDocument = generalDocument;
    }

    @Override
    public Event open() throws IOException, XMLStreamException {
        this.mastersAndPages = mastersAndPages();
        this.entries = entries();

        return this.generalDocument.startDocumentEvent();
    }

    private List<String> mastersAndPages() throws IOException, XMLStreamException {
        Relationships relationships = this.generalDocument.relationshipsFor(this.generalDocument.mainPartName());

        List<Relationships.Rel> masters = relationships.getRelByType(MASTERS);

        if (masters == null || masters.size() != 1) {
            throw new OkapiBadFilterInputException(Relationships.UNEXPECTED_NUMBER_OF_RELATIONSHIPS);
        }

        List<Relationships.Rel> pages = relationships.getRelByType(PAGES);

        if (pages == null || pages.size() != 1) {
            throw new OkapiBadFilterInputException(Relationships.UNEXPECTED_NUMBER_OF_RELATIONSHIPS);
        }

        Relationships masterRelationships = this.generalDocument.relationshipsFor(masters.get(0).target);
        Relationships pageRelationships = this.generalDocument.relationshipsFor(pages.get(0).target);

        List<String> targets = mapRelsToTargets(masterRelationships.getRelByType(MASTER));
        targets.addAll(mapRelsToTargets(pageRelationships.getRelByType(PAGE)));

        return targets;
    }

    private Enumeration<? extends ZipEntry> entries() {
        final List<? extends ZipEntry> entries = list(this.generalDocument.entries());
        entries.sort(new ZipEntryComparator(mastersAndPages));

        return enumeration(entries);
    }

    @Override
    public boolean isStyledTextPart(final ZipEntry entry) {
        return false;
    }

    @Override
    public boolean hasPostponedTranslatables() {
        return false;
    }

    @Override
    public void updatePostponedTranslatables(final String key, final String value) {
    }

    @Override
    public boolean hasNextPart() {
        return this.entries.hasMoreElements();
    }

    @Override
    public Part nextPart() {
        final ZipEntry entry = this.entries.nextElement();
        if (!isTranslatablePart(entry)) {
            return new NonModifiablePart(this.generalDocument, entry);
        }

        return new MasterAndPagePart(this.generalDocument, entry);
    }

    private boolean isTranslatablePart(final ZipEntry entry) {
        final String contentType = this.generalDocument.contentTypeFor(entry);
        return entry.getName().endsWith(".xml") && (isMasterPart(contentType) || isPagePart(contentType));
    }

    private static boolean isMasterPart(String type) {
        return ContentTypes.Types.Visio.MASTER_TYPE.equals(type);
    }

    private static boolean isPagePart(String type) {
        return ContentTypes.Types.Visio.PAGE_TYPE.equals(type);
    }

    @Override
    public void close() throws IOException {
    }
}
