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
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.openxml.XMLEventSerializer.serialize;

class RelationshipsPart extends TranslatablePart {
    private static final String RELATIONSHIPS_TAG = "Relationships";
    private static final String RELATIONSHIP_TAG = "Relationship";
    private static final QName RELATIONSHIP_TYPE = new QName("Type");
    private static final QName RELATIONSHIP_TARGET_MODE = new QName("TargetMode");
    private static final QName RELATIONSHIP_TARGET = new QName("Target");
    private final IdGenerator textUnitIdGenerator;
    private final String hyperlinkType;
    private String subDocumentId;
    private XMLEventReader xmlEventReader;
    private Iterator<Event> filterEventIterator;
    private QName relationshipTagName;

    private static final String HYPERLINK = "/hyperlink";
    private static final String TARGET_MODE_EXTERNAL = "External";

    RelationshipsPart(Document.General generalDocument, ZipEntry entry) {
        super(generalDocument, entry);
        this.textUnitIdGenerator = new IdGenerator(entry.getName(), IdGenerator.TEXT_UNIT);
        this.hyperlinkType = generalDocument.documentRelationshipsNamespace().uri().concat(HYPERLINK);
    }

    @Override
    public Event open() throws IOException, XMLStreamException {
        this.subDocumentId = this.generalDocument.nextSubDocumentId();

        xmlEventReader = generalDocument.inputFactory().createXMLEventReader(
                new InputStreamReader(new BufferedInputStream(generalDocument.inputStreamFor(entry)), StandardCharsets.UTF_8));
        handlePart();
        return createStartSubDocumentEvent(this.generalDocument.documentId(), subDocumentId);
    }

    private void handlePart() throws XMLStreamException {
        while(xmlEventReader.hasNext()) {
            XMLEvent e = xmlEventReader.nextEvent();

            if (e.isStartElement() && e.asStartElement().getName().getLocalPart().equals(RELATIONSHIPS_TAG)) {
                qualifyNames(e.asStartElement());
            }
            if (!e.isStartElement() || !e.asStartElement().getName().equals(this.relationshipTagName)) {
                filterEvents.add(new Event(EventType.DOCUMENT_PART, createDocumentPart(e)));
                continue;
            }

            Attribute type = e.asStartElement().getAttributeByName(RELATIONSHIP_TYPE);
            Attribute targetMode = e.asStartElement().getAttributeByName(RELATIONSHIP_TARGET_MODE);
            Attribute target = e.asStartElement().getAttributeByName(RELATIONSHIP_TARGET);

            if (null == type || targetMode == null || target == null
                    || !this.hyperlinkType.equals(type.getValue()) || !TARGET_MODE_EXTERNAL.equals(targetMode.getValue())
                    || target.getValue().isEmpty()) {
                filterEvents.add(new Event(EventType.DOCUMENT_PART, createDocumentPart(e)));
                continue;
            }

            TextUnit textUnit = createTextUnit(e.asStartElement());
            filterEvents.add(new Event(EventType.TEXT_UNIT, textUnit));
        }

        filterEvents.add(new Event(EventType.END_SUBDOCUMENT, new Ending(subDocumentId)));
        filterEventIterator = filterEvents.iterator();
    }

    private void qualifyNames(final StartElement startElement) {
        this.relationshipTagName = new QName(
            startElement.getName().getNamespaceURI(),
            RELATIONSHIP_TAG,
            startElement.getName().getPrefix()
        );
    }

    private DocumentPart createDocumentPart(XMLEvent e) {
        return new DocumentPart(documentPartIdGenerator.createId(), false , new GenericSkeleton(serialize(e)));
    }

    private TextUnit createTextUnit(final StartElement startElement) {
        final String[] parts = skeletonParts(XMLEventSerializer.serialize(startElement));

        GenericSkeleton skel = new GenericSkeleton();
        skel.append(parts[0]);
        TextUnit textUnit = new TextUnit(textUnitIdGenerator.createId());
        textUnit.setSourceContent(new TextFragment(parts[1]));
        skel.addContentPlaceholder(textUnit);
        skel.append(parts[2]);
        textUnit.setSkeleton(skel);

        return textUnit;
    }

    /**
     * Obtains skeleton parts from a relationship string.
     *
     * The order of the parts is the following:
     *   the first skeleton part,
     *   a value for a text unit,
     *   the last skeleton part.
     * @param string A string to process
     * @return An array of parts
     */
    private static String[] skeletonParts(final String string) {
        final String startValueMarker = RELATIONSHIP_TARGET.getPrefix().isEmpty()
            ? RELATIONSHIP_TARGET.getLocalPart() + "=\""
            : RELATIONSHIP_TARGET.getPrefix() + ":" + RELATIONSHIP_TARGET.getLocalPart() + "=\"";

        final int valueIndex = string.indexOf(startValueMarker) + startValueMarker.length();
        final int lastPartIndex = string.indexOf("\"", valueIndex);

        return new String[] {
            string.substring(0, valueIndex),
            string.substring(valueIndex, lastPartIndex),
            string.substring(lastPartIndex, string.length())
        };
    }

    @Override
    public boolean hasNextEvent() {
        return filterEventIterator.hasNext();
    }

    @Override
    public Event nextEvent() {
        return filterEventIterator.next();
    }

    @Override
    public void close() {

    }

    @Override
    public void logEvent(Event e) {

    }
}
