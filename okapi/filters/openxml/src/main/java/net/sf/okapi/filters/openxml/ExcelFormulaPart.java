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
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.skeleton.GenericSkeleton;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.isExcelFormula;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isTableColumnEvent;

/**
 * Here is a basic logic that was brought from OpenXMLContentFilter(general mechanism left in OpenXMLContentFilter the
 *  removing could be dangerously) which is handling string data in tableN.xml and sheetN.xml files
 * it works with column names and references to the columns in formulas.
 */
class ExcelFormulaPart extends TranslatablePart {
    private static final String NAME = "name";
    private static Pattern FORMULA_PATTERN = Pattern.compile("(.*?[\\[\"]{1})([a-zA-Z0-9 \\*]+?)([\\]\"]{1}.*)", Pattern.DOTALL);

    private final Map<String, String> sharedStrings;
    private final InputStream inputStream;
    private String subDocumentId;
    private Iterator<Event> filterEventIterator;

    private enum FORMULA_MATCHER_GROUPS {
        NON_TRANSLATABLE(1),
        TRANSLATABLE(2),
        UNPROCESSED(3);

        private final int value;

        FORMULA_MATCHER_GROUPS(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

    ExcelFormulaPart(Document.General generalDocument, ZipEntry entry, Map<String, String> sharedStrings, InputStream inputStream) {
        super(generalDocument, entry);
        this.inputStream = inputStream;
        this.sharedStrings = sharedStrings;
    }

    String getModifiedContent() throws XMLStreamException, IOException {
        StringWriter sw = new StringWriter();
        XMLEventWriter xmlEventWriter = this.generalDocument.outputFactory().createXMLEventWriter(sw);
        XMLEventReader xmlReader = createXMLReader();

        while (xmlReader.hasNext()) {
            XMLEvent e = xmlReader.nextEvent();
            if (isTableColumnEvent(e)) {
                e = rewriteNameAttribute(e);
            } else if (isExcelFormula(e)) {
                xmlEventWriter.add(e);
                e = xmlReader.nextEvent();
                if (e.isCharacters()) {
                    e = this.generalDocument.eventFactory().createCharacters(
                        updateTextualReferencesInParsedCharacterData(e.asCharacters())
                    );
                }
            }
            xmlEventWriter.add(e);
        }
        return sw.toString();
    }

    private XMLEventReader createXMLReader() throws XMLStreamException, IOException {
        return this.generalDocument.inputFactory().createXMLEventReader(
            new InputStreamReader(new BufferedInputStream(this.inputStream))
        );
    }

    private XMLEvent rewriteNameAttribute(XMLEvent e) {
        String value = e.asStartElement().getAttributeByName(new QName(NAME)).getValue();
        String translatedSharedString = sharedStrings.get(value);

        if (translatedSharedString == null) {
            return e;
        } else {
            Attribute attr = this.generalDocument.eventFactory().createAttribute(NAME, translatedSharedString);
            Iterator attributeIterator = e.asStartElement().getAttributes();
            ArrayList<Attribute> updatedAttributes = new ArrayList<>();
            while (attributeIterator.hasNext()) {
                Attribute attribute = (Attribute) attributeIterator.next();
                if (NAME.equalsIgnoreCase(attribute.getName().toString())) {
                    updatedAttributes.add(attr);
                } else {
                    updatedAttributes.add(attribute);
                }
            }
            return this.generalDocument.eventFactory().createStartElement(e.asStartElement().getName(), updatedAttributes.iterator(), e.asStartElement().getNamespaces());
        }
    }

    private String updateTextualReferencesInParsedCharacterData(Characters pcdata) {
        StringBuilder result = new StringBuilder();
        String text = pcdata.getData();
        Matcher formulaMatcher = FORMULA_PATTERN.matcher(text);
        String formulaPart = null;

        if (!formulaMatcher.find()) {
            return text;
        }

        do {
            if (formulaMatcher.groupCount() != FORMULA_MATCHER_GROUPS.values().length) {
                break;
            }
            result.append(formulaMatcher.group(FORMULA_MATCHER_GROUPS.NON_TRANSLATABLE.getValue()));
            String textUnitPart = formulaMatcher.group(FORMULA_MATCHER_GROUPS.TRANSLATABLE.getValue());

            if (sharedStrings.get(textUnitPart) == null) {
                result.append(textUnitPart);
            } else {
                result.append(sharedStrings.get(textUnitPart));
            }

            formulaPart = formulaMatcher.group(FORMULA_MATCHER_GROUPS.UNPROCESSED.getValue());
            formulaMatcher.reset(formulaPart);
        } while (formulaMatcher.find());
        
        if (formulaPart != null) {
            result.append(formulaPart);
        }
        return result.toString();
    }

    @Override
    public Event open() throws IOException, XMLStreamException {
        this.subDocumentId = this.generalDocument.nextSubDocumentId();
        process();

        return createStartSubDocumentEvent(this.generalDocument.documentId(), subDocumentId);
    }

    private void process() throws XMLStreamException, IOException {
        DocumentPart documentPart = new DocumentPart(documentPartIdGenerator.createId(), false);
        documentPart.setSkeleton(new GenericSkeleton(getModifiedContent()));

        filterEvents.add(new Event(EventType.DOCUMENT_PART, documentPart));
        filterEvents.add(new Event(EventType.END_SUBDOCUMENT, new Ending(subDocumentId)));
        filterEventIterator = filterEvents.iterator();
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
