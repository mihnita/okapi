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
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.SubFilter;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isStringItemStartEvent;

class SharedStringsPart extends StyledTextPart {
    private final EncoderManager encoderManager;
    private final IFilter subfilter;
    private final SharedStringMap sharedStringMap;

    private int sharedStringIndex = 0;

    SharedStringsPart(Document.General generalDocument, ZipEntry entry, StyleDefinitions styleDefinitions,
                      StyleOptimisation styleOptimisation, EncoderManager encoderManager, IFilter subfilter, SharedStringMap sharedStringMap) {
        super(generalDocument, entry, styleDefinitions, styleOptimisation);

        this.encoderManager = encoderManager;
        this.subfilter = subfilter;
        this.sharedStringMap = sharedStringMap;
    }

    /**
     * Opens this part and performs any initial processing.  Returns the
     * first event for this part.  In this case, it's a START_SUBDOCUMENT
     * event.
     *
     * @return Event
     * @throws IOException
     * @throws XMLStreamException
     */
    @Override
    public Event open() throws IOException, XMLStreamException {
        this.documentId = this.generalDocument.documentId();
        this.subDocumentId = this.generalDocument.nextSubDocumentId();
        this.sourceLocale = this.generalDocument.sourceLocale();

        SharedStringsDenormalizer deno = new SharedStringsDenormalizer(generalDocument.eventFactory(), sharedStringMap);
        XMLEventReader reader = generalDocument.inputFactory().createXMLEventReader(
                new InputStreamReader(generalDocument.inputStreamFor(entry), StandardCharsets.UTF_8));
        final File rewrittenStringsTable = File.createTempFile("sharedStrings", ".xml");
        XMLEventWriter writer = generalDocument.outputFactory().createXMLEventWriter(
                new OutputStreamWriter(new FileOutputStream(rewrittenStringsTable), StandardCharsets.UTF_8));
        deno.process(reader, writer);

        InputStream is = new BufferedInputStream(new FileInputStream(rewrittenStringsTable));

        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        OpenXMLFilter.configureInputFactory(inputFactory);
        this.eventReader = inputFactory.createXMLEventReader(is);
        try {
            process();
        } finally {
            if (eventReader != null) {
                eventReader.close();
            }
            rewrittenStringsTable.delete();
        }
        return createStartSubDocumentEvent(documentId, subDocumentId);
    }

    private void process() throws XMLStreamException {
        XMLEvent e = null;
        while (eventReader.hasNext()) {
            e = eventReader.nextEvent();
            if (isStringItemStartEvent(e) && sharedStringMap.isStringVisible(sharedStringIndex++)) {
                flushDocumentPart();
                StartElementContext startElementContext = createStartElementContext(
                    e.asStartElement(),
                    eventReader,
                    generalDocument.eventFactory(),
                    this.generalDocument.conditionalParameters()
                );
                StringItem stringItem = new StringItemParser(startElementContext, nestedBlockId, styleDefinitions, styleOptimisation).parse();
                stringItem.optimiseStyles();
                String cell = sharedStringMap.getStringCell(sharedStringIndex-1);
                String worksheet = sharedStringMap.getStringWorksheet(sharedStringIndex-1);

                final List<ITextUnit> textUnits = new StringItemTextUnitMapper(
                    this.textUnitId,
                    this.generalDocument.eventFactory(),
                    stringItem,
                    cell,
                    worksheet
                ).map();
                if (textUnits.isEmpty()) {
                    addBlockChunksToDocumentPart(stringItem.getChunks());
                } else {
                    if (subfilter != null && !stringItem.isStyled()) {
                        addSubfilteredEvents(textUnits);
                    } else {
                        addTextUnitEvents(textUnits);
                    }
                }
            } else {
                addEventToDocumentPart(e);
            }
        }
        flushDocumentPart();
        filterEvents.add(new Event(EventType.END_SUBDOCUMENT, new Ending(subDocumentId)));
        filterEventIterator = filterEvents.iterator();
    }

    private void addTextUnitEvents(final List<ITextUnit> textUnits) {
        for (final ITextUnit tu : textUnits) {
            filterEvents.add(new Event(EventType.TEXT_UNIT, tu));
        }
    }

    private void addSubfilteredEvents(final List<ITextUnit> textUnits) {
        int subfilterIndex = 0;
        for (final ITextUnit tu : textUnits) {
            try (final SubFilter sf = new SubFilter(subfilter, encoderManager.getEncoder(), ++subfilterIndex, tu.getId(), tu.getName())) {
                filterEvents.addAll(sf.getEvents(new RawDocument(tu.getSource().getFirstContent().getText(), sourceLocale)));
                filterEvents.add(sf.createRefEvent(tu));
            }
        }
    }
}
