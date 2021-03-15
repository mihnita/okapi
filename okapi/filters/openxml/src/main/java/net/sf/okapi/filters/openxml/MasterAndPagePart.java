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
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipEntry;

import static net.sf.okapi.common.resource.TextFragment.TagType.CLOSING;
import static net.sf.okapi.common.resource.TextFragment.TagType.OPENING;
import static net.sf.okapi.common.resource.TextFragment.TagType.PLACEHOLDER;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isEndElement;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isStartElement;
import static net.sf.okapi.filters.openxml.XMLEventSerializer.serialize;

class MasterAndPagePart extends TranslatablePart {

    private static final String UNEXPECTED_NUMBER_OF_XML_EVENTS = "Unexpected number of XML events";

    private static final String TEXT = "Text";
    private static final String CHARACTER_PROPERTIES_MARKER = "cp";
    private static final String TEXT_FIELD_INSERTION_POINT = "fld";
    private static final String PARAGRAPH_PROPERTIES_MARKER = "pp";
    private static final String TAB_PROPERTIES_MARKER = "tp";

    private final IdGenerator textUnitIdGenerator;
    private String subDocumentId;
    private XMLEventReader xmlEventReader;
    private Iterator<Event> filterEventIterator;

    MasterAndPagePart(Document.General generalDocument, ZipEntry entry) {
        super(generalDocument, entry);
        textUnitIdGenerator = new IdGenerator(entry.getName(), IdGenerator.TEXT_UNIT);
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

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();

            if (!isStartElement(xmlEvent, TEXT)) {
                addEventToDocumentPart(xmlEvent);
                continue;
            }

            flushDocumentPart();
            TextUnit textUnit = parseText(xmlEvent);
            filterEvents.add(new Event(EventType.TEXT_UNIT, textUnit));
        }

        flushDocumentPart();
        filterEvents.add(new Event(EventType.END_SUBDOCUMENT, new Ending(subDocumentId)));
        filterEventIterator = filterEvents.iterator();
    }

    private void flushDocumentPart() {
        DocumentPart documentPart = new DocumentPart(documentPartIdGenerator.createId(), false, new GenericSkeleton(serialize(documentPartEvents)));
        filterEvents.add(new Event(EventType.DOCUMENT_PART, documentPart));

        documentPartEvents = new ArrayList<>();
    }

    private TextUnit parseText(XMLEvent event) throws XMLStreamException {

        GenericSkeleton skeleton = new GenericSkeleton(serialize(event));

        TextUnit textUnit = new TextUnit(textUnitIdGenerator.createId());
        textUnit.setPreserveWhitespaces(true);

        TextFragment textFragment = new TextFragment();

        XMLEvent nextEvent = null;

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = null == nextEvent ? xmlEventReader.nextEvent() : nextEvent;

            if (isEndElement(xmlEvent, event.asStartElement())) {
                textUnit.setSourceContent(textFragment);

                skeleton.addContentPlaceholder(textUnit);
                skeleton.append(serialize(xmlEvent));

                textUnit.setSkeleton(skeleton);

                return textUnit;
            }

            if (isStartElement(xmlEvent, CHARACTER_PROPERTIES_MARKER)
                    || isStartElement(xmlEvent, TEXT_FIELD_INSERTION_POINT)
                    || isStartElement(xmlEvent, PARAGRAPH_PROPERTIES_MARKER)
                    || isStartElement(xmlEvent, TAB_PROPERTIES_MARKER)) {

                String codeType = xmlEvent.asStartElement().getName().getLocalPart();

                if (!xmlEventReader.hasNext()) {
                    throw new OkapiBadFilterInputException(UNEXPECTED_NUMBER_OF_XML_EVENTS);
                }

                nextEvent = xmlEventReader.nextEvent();

                if (!nextEvent.isEndElement() || !nextEvent.asEndElement().getName().equals(xmlEvent.asStartElement().getName())) {
                    Code code = new Code(OPENING, codeType, serialize(xmlEvent));
                    textFragment.append(code);

                    continue;
                }

                Code code = new Code(PLACEHOLDER, codeType, serialize(Arrays.asList(xmlEvent, nextEvent)));
                textFragment.append(code);
                nextEvent = null;

                continue;
            }

            if (isEndElement(xmlEvent, CHARACTER_PROPERTIES_MARKER)
                    || isEndElement(xmlEvent, TEXT_FIELD_INSERTION_POINT)
                    || isEndElement(xmlEvent, PARAGRAPH_PROPERTIES_MARKER)
                    || isEndElement(xmlEvent, TAB_PROPERTIES_MARKER)) {

                String codeType = xmlEvent.asEndElement().getName().getLocalPart();

                Code code = new Code(CLOSING, codeType, serialize(xmlEvent));
                textFragment.append(code);
                nextEvent = null;

                continue;
            }

            textFragment.append(serialize(xmlEvent));
            nextEvent = null;
        }

        throw new OkapiBadFilterInputException();
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
