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

import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.resource.TextFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_REGULAR_HYPHEN_VALUE;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.hasPreserveWhitespace;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isEndElement;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isGraphicsProperty;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isLineBreakStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isNoBreakHyphenStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isPageBreak;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isParagraphEndEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isParagraphPropertiesStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isParagraphStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isRunPropsStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isRunStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isTabStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isTextPath;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isTextStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isWhitespace;

class RunParser implements Parser<RunBuilder> {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static final String UNEXPECTED_STRUCTURE = "Unexpected structure: ";

    private static final int COMPLEX_FIELD_INITIAL_NESTING_LEVEL = 1;

    private static final QName COMPLEX_FIELD_CHARACTER_TYPE = Namespaces.WordProcessingML.getQName("fldCharType");
    private static final String COMPLEX_FIELD_CHARACTER = "fldChar";
    private static final String COMPLEX_FIELD_INSTRUCTION = "instrText";
    private static final String BEGIN = "begin";
    private static final String SEPARATE = "separate";
    private static final String END = "end";

    private final StartElementContext startElementContext;
    private final IdGenerator nestedTextualIds;
    private final StyleDefinitions styleDefinitions;
    private final StyleOptimisation styleOptimisation;
    private final String paragraphStyle;

    private final RunBuilder runBuilder;

    private final StrippableAttributes drawingRunPropertiesStrippableAttributes;
    private final StrippableAttributes wordRunRevisions;
    private final StrippableAttributes wordParagraphRevisions;

    private final SkippableElements defaultSkippableElements;
    private final SkippableElements blockPropertiesSkippableElements;
    private final RunSkippableElements runSkippableElements;
    private final BlockSkippableElements blockSkippableElements;

    private int complexFieldNestingLevel;

    RunParser(StartElementContext startElementContext, IdGenerator nestedTextualIds,
              StyleDefinitions styleDefinitions, StyleOptimisation styleOptimisation,
              String paragraphStyle, boolean hidden) {
        this.drawingRunPropertiesStrippableAttributes = new StrippableAttributes.DrawingRunProperties(
            startElementContext.getConditionalParameters(),
            startElementContext.getEventFactory()
        );
        this.wordRunRevisions = new StrippableAttributes.WordRunRevisions(
            startElementContext.getEventFactory()
        );
        this.wordParagraphRevisions = new StrippableAttributes.WordParagraphRevisions(
            startElementContext.getEventFactory()
        );

        this.startElementContext = createStartElementContext(
            this.wordRunRevisions.strip(startElementContext.getStartElement()),
            startElementContext
        );
        this.nestedTextualIds = nestedTextualIds;
        this.styleDefinitions = styleDefinitions;
        this.styleOptimisation = styleOptimisation;
        this.paragraphStyle = paragraphStyle;

        runBuilder = new RunBuilder(this.startElementContext, styleDefinitions);
        runBuilder.setHidden(hidden);

        defaultSkippableElements = new SkippableElements.Default();
        blockPropertiesSkippableElements = new SkippableElements.RevisionProperty(
            new SkippableElements.Property(
                new SkippableElements.Default(
                    SkippableElement.RunProperty.RUN_PROPERTY_RTL_DML,
                    SkippableElement.RunProperty.RUN_PROPERTY_LANGUAGE,
                    SkippableElement.RevisionProperty.RUN_PROPERTY_INSERTED_PARAGRAPH_MARK,
                    SkippableElement.RevisionProperty.RUN_PROPERTY_MOVED_PARAGRAPH_TO,
                    SkippableElement.RevisionProperty.RUN_PROPERTY_MOVED_PARAGRAPH_FROM,
                    SkippableElement.RevisionProperty.PARAGRAPH_PROPERTIES_CHANGE,
                    SkippableElement.RevisionProperty.RUN_PROPERTIES_CHANGE
                ),
                startElementContext.getConditionalParameters()
            ),
            startElementContext.getConditionalParameters()
        );
        runSkippableElements = new RunSkippableElements(startElementContext);
        blockSkippableElements = new BlockSkippableElements(startElementContext);
    }

    public RunBuilder parse() throws XMLStreamException {
        log("startRun: " + startElementContext.getStartElement());

        // rPr is either the first child, or not present (section 17.3.2)
        XMLEvent firstChild = startElementContext.getEventReader().nextTag();
        if (isRunPropsStartEvent(firstChild)) {
            parseRunProperties(firstChild.asStartElement());
        } else if (isEndElement(firstChild, startElementContext.getStartElement())) {
            // Empty run!
            return endRunParsing(firstChild.asEndElement());
        } else {
            // No properties section
            startRunParsing(firstChild);
            if ((paragraphStyle == null || paragraphStyle.isEmpty()) &&
                !startElementContext.getConditionalParameters().getTranslateWordInExcludeStyleMode() && !runBuilder.isHidden()) {
                runBuilder.setHidden(true);
            }
            if (!startElementContext.getConditionalParameters().getTranslateWordInExcludeHighlightMode() && !runBuilder.isHidden()) {
                runBuilder.setHidden(true);
            }
        }
        while (startElementContext.getEventReader().hasNext()) {
            XMLEvent e = startElementContext.getEventReader().nextEvent();
            log("processRun: " + e);
            if (isEndElement(e, startElementContext.getStartElement())) {
                return endRunParsing(e.asEndElement());
            } else {
                startRunParsing(e);
            }
        }
        throw new IllegalStateException("Invalid content? Unterminated run");
    }

    private RunBuilder endRunParsing(EndElement e) {
        runBuilder.flushText();
        runBuilder.flushMarkup();
        this.runBuilder.setEndEvent(e);
        // XXX This is pretty hacky.
        // Recalculate the properties now that consolidation has already happened.
        // This is required in order to properly handle the aggressive-mode trimming
        // of the vertAlign property, which is only done if there's no text in the
        // run.  Whether or not text is present can only be correctly calculated
        // -after- other run merging has already taken place.
        if (!runBuilder.hasNonWhitespaceText() && startElementContext.getConditionalParameters().getCleanupAggressively()) {
            runBuilder.setRunProperties(RunProperties.copiedRunProperties(runBuilder.getRunProperties(), true, false, false));
        }
        return this.runBuilder;
    }

    private void startRunParsing(final XMLEvent e) throws XMLStreamException {
        if (isParagraphStartEvent(e)) {
            parseNestedBlock(e.asStartElement());
        }
        // XXX I need to make sure I don't try to merge this thing
        else if (isComplexFieldBegin(e)) {
            parseComplexField(e.asStartElement());
            this.runBuilder.setContainsComplexFields(true);
        } else {
            if (!parseSkippableElements(e)) {
                parseContent(processTranslatableAttributes(e), startElementContext.getEventReader());
            }
        }
    }

    private void parseRunProperties(StartElement startElement) throws XMLStreamException {
        StartElementContext runPropertiesElementContext = createStartElementContext(startElement, runBuilder.getStartElementContext());
        final RunProperties direct = new RunPropertiesParser(runPropertiesElementContext, runSkippableElements).parse();
        final String runStyle = null == direct.getRunStyleProperty()
            ? null
            : direct.getRunStyleProperty().value();
        runBuilder.setRunProperties(
            direct.minified(
                this.styleDefinitions.combinedRunProperties(
                    this.paragraphStyle,
                    runStyle,
                    new RunProperties.Default(
                        runPropertiesElementContext.getEventFactory(),
                        startElement.getName().getPrefix(),
                        startElement.getName().getNamespaceURI(),
                        startElement.getName().getLocalPart()
                    )
                )
            )
        );
        RunProperty.HighlightRunProperty highlightRunProperty = runBuilder.getRunProperties().getHighlightProperty();
        RunProperty.ColorRunProperty colorRunProperty = runBuilder.getRunProperties().getRunColorProperty();

        // Handle Style exclusion or inclusion
        if (runStyle != null) {
            runBuilder.setRunStyle(runStyle);
            if (startElementContext.getConditionalParameters().getTranslateWordInExcludeStyleMode()) {
                runBuilder.setHidden(startElementContext.getConditionalParameters().isWordExcludedStyle(runStyle));
            } else {
                runBuilder.setHidden(!startElementContext.getConditionalParameters().isWordExcludedStyle(runStyle));
            }
        }

        // Handle Highlight exclusion or inclusion
        if (highlightRunProperty != null) {
            if (startElementContext.getConditionalParameters().getTranslateWordInExcludeHighlightMode() && !runBuilder.isHidden()) {
                runBuilder.setHidden(startElementContext.getConditionalParameters().isWordHighlightColor(highlightRunProperty.value()));
            } else {
                runBuilder.setHidden(!startElementContext.getConditionalParameters().isWordHighlightColor(highlightRunProperty.value()));
            }
        }

        // Handle Color Exclude
        if (colorRunProperty != null) {
            if (startElementContext.getConditionalParameters().getTranslateWordExcludeColors() && !runBuilder.isHidden()) {
                runBuilder.setHidden(startElementContext.getConditionalParameters().isWordExcludedColor(colorRunProperty.value()));
            }
        }
    }

    private void parseNestedBlock(final StartElement startElement) throws XMLStreamException {
        log("Nested block start event: " + startElement);
        runBuilder.flushText();
        StartElementContext blockElementContext = createStartElementContext(startElement, startElementContext);

        final StyleDefinitions styleDefinitions;
        final StyleOptimisation styleOptimisation;
        if (Namespace.PREFIX_A.equals(startElement.getName().getPrefix())) {
            styleDefinitions = new StyleDefinitions.Empty();
            styleOptimisation = new StyleOptimisation.Default(
                new StyleOptimisation.Bypass(),
                this.startElementContext.getConditionalParameters(),
                this.startElementContext.getEventFactory(),
                new QName(startElement.getName().getNamespaceURI(), ParagraphBlockProperties.PPR, startElement.getName().getPrefix()),
                new QName(startElement.getName().getNamespaceURI(), RunProperties.DEF_RPR, startElement.getName().getPrefix()),
                Collections.emptyList(),
                styleDefinitions
            );
        } else {
            styleDefinitions = runBuilder.getStyleDefinitions();
            styleOptimisation = this.styleOptimisation;
        }
        BlockParser nestedBlockParser = new BlockParser(blockElementContext, nestedTextualIds,
                styleDefinitions, styleOptimisation);
        Block nested = nestedBlockParser.parse();
        nested.optimiseStyles();
        runBuilder.setContainsNestedItems(true);
        if (nested.hasVisibleRunContent()) {
            // Create a reference to mark the location of the nested block
            runBuilder.addToMarkup(startElementContext.getEventFactory().createCharacters(
                    TextFragment.makeRefMarker(nestedTextualIds.createId())));
            runBuilder.getNestedTextualItems().add(nested);
        } else {
            // Empty block, we don't need to expose it after all
            for (XMLEvent nestedEvent : nested.getEvents()) {
                runBuilder.addToMarkup(nestedEvent);
            }
            // However, we do need to preserve anything it references that's translatable
            for (Chunk chunk : nested.getChunks()) {
                if (chunk instanceof Run) {
                    runBuilder.getNestedTextualItems().addAll(((Run) chunk).getNestedTextualItems());
                } else if (chunk instanceof RunContainer) {
                    for (Chunk nestedChunk : ((RunContainer) chunk).getChunks()) {
                        if (nestedChunk instanceof Run) {
                            runBuilder.getNestedTextualItems().addAll(((Run) nestedChunk).getNestedTextualItems());
                        }
                    }
                }
            }
        }
    }

    private void parseComplexField(final StartElement startElement) throws XMLStreamException {
        boolean extractable = false;
        boolean atComplexFieldResult = false;
        final Deque<XMLEvent> deferredEvents = new LinkedList<>();

        this.complexFieldNestingLevel++;
        parseComplexFieldBegin(startElement);

        while (startElementContext.getEventReader().hasNext()) {
            final XMLEvent e = startElementContext.getEventReader().nextEvent();
            if (isComplexFieldEnd(e)) {
                if (!extractable) {
                    parseSimpleElement(e.asStartElement());
                } else {
                    endComplexFieldParsing(e.asStartElement(), deferredEvents);
                }
                this.complexFieldNestingLevel--;
                return;
            }
            if (isComplexFieldInstruction(e)) {
                extractable = parseComplexFieldCode(e.asStartElement());
                continue;
            }
            if (isComplexFieldSeparate(e)) {
                parseSimpleElement(e.asStartElement());
                atComplexFieldResult = true;
                continue;
            }
            if (isComplexFieldBegin(e)) {
                if (extractable && !deferredEvents.isEmpty()) {
                    parseContentFrom(deferredEvents);
                }
                parseComplexField(e.asStartElement());
                continue;
            }
            if (!extractable || !atComplexFieldResult) {
                if (parseSkippableElements(e)) {
                    continue;
                }
                this.runBuilder.addToMarkup(stripRevisionAttributes(e));
                continue;
            }
            if (isParagraphEndEvent(e)) {
                // stick to the latest available paragraph
                if (!deferredEvents.isEmpty()) {
                    parseContentFrom(deferredEvents);
                }
                deferredEvents.add(e);
                continue;
            }
            if (!deferredEvents.isEmpty()) {
                if (isParagraphPropertiesStartEvent(e)) {
                    StartElementContext blockPropertiesElementContext = createStartElementContext(
                        e.asStartElement(),
                        this.startElementContext
                    );
                    final ParagraphBlockProperties blockProperties = new MarkupComponentParser()
                            .parseParagraphBlockProperties(
                                blockPropertiesElementContext,
                                this.drawingRunPropertiesStrippableAttributes,
                                this.blockPropertiesSkippableElements
                            );
                    deferredEvents.addAll(blockProperties.getEvents());
                } else {
                    if (parseSkippableElements(e)) {
                        continue;
                    }
                    deferredEvents.add(e);
                }
                continue;
            }
            parseContent(
                processTranslatableAttributes(stripRevisionAttributes(e)),
                startElementContext.getEventReader()
            );
        }
    }

    private static boolean isComplexFieldBegin(XMLEvent e) {
        return XMLEventHelpers.isStartElement(e, COMPLEX_FIELD_CHARACTER)
                && BEGIN.equals(XMLEventHelpers.getAttributeValue(e.asStartElement(), COMPLEX_FIELD_CHARACTER_TYPE));
    }

    private static boolean isComplexFieldInstruction(XMLEvent e) {
        return XMLEventHelpers.isStartElement(e, COMPLEX_FIELD_INSTRUCTION);
    }

    private static boolean isComplexFieldSeparate(XMLEvent e) {
        return XMLEventHelpers.isStartElement(e, COMPLEX_FIELD_CHARACTER)
                && SEPARATE.equals(XMLEventHelpers.getAttributeValue(e.asStartElement(), COMPLEX_FIELD_CHARACTER_TYPE));
    }

    private static boolean isComplexFieldEnd(XMLEvent e) {
        return XMLEventHelpers.isStartElement(e, COMPLEX_FIELD_CHARACTER)
                && END.equals(XMLEventHelpers.getAttributeValue(e.asStartElement(), COMPLEX_FIELD_CHARACTER_TYPE));
    }

    private void parseComplexFieldBegin(final StartElement startElement) throws XMLStreamException {
        this.runBuilder.addToMarkup(startElement);
        while (this.startElementContext.getEventReader().hasNext()) {
            final XMLEvent event = this.startElementContext.getEventReader().nextEvent();
            this.runBuilder.addToMarkup(event);
            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                return;
            }
        }
        throw new IllegalStateException(UNEXPECTED_STRUCTURE + startElement);
    }

    private void parseSimpleElement(final StartElement startElement) throws XMLStreamException {
        this.runBuilder.addToMarkup(startElement);
        final XMLEvent event = this.startElementContext.getEventReader().nextEvent();

        if (!event.isEndElement() || !event.asEndElement().getName().equals(startElement.getName())) {
            throw new IllegalStateException(UNEXPECTED_STRUCTURE + event);
        }
        this.runBuilder.addToMarkup(event);
    }

    private boolean parseSkippableElements(final XMLEvent e) throws XMLStreamException {
        if (runSkippableElements.skip(e)) {
            return true;
        } else if (blockSkippableElements.skip(e)) {
            return true;
        }
        return false;
    }

    private void endComplexFieldParsing(final StartElement startElement, final Deque<XMLEvent> deferredEvents) throws XMLStreamException {
        if (goesAfterAnotherRun(deferredEvents)) {
            // preserve the boundary place
            parseContentFrom(deferredEvents);
            parseSimpleElement(startElement);
        } else {
            final Collection<XMLEvent> subtractedEvents = subtractAllFromLastOpeningRun(deferredEvents);
            parseContentFrom(subtractedEvents);
            parseSimpleElement(startElement);
            if (COMPLEX_FIELD_INITIAL_NESTING_LEVEL == this.complexFieldNestingLevel) {
                this.runBuilder.addDeferredEvents(deferredEvents);
            } else {
                parseToFirstClosingRun();
                parseContentFrom(deferredEvents);
            }
        }
    }

    private boolean goesAfterAnotherRun(final Deque<XMLEvent> deferredEvents) {
        final Iterator<XMLEvent> deferredEventsIterator = deferredEvents.descendingIterator();

        while (deferredEventsIterator.hasNext()) {
            final XMLEvent event = deferredEventsIterator.next();

            if (event.isEndElement() && XMLEventHelpers.LOCAL_RUN.equals(event.asEndElement().getName().getLocalPart())) {
                return true;
            }
        }

        return false;
    }

    private void parseContentFrom(final Collection<XMLEvent> deferredEvents) throws XMLStreamException {
        final XMLEventReader eventReader = new ConsumableXMLEventsReader(new XMLEventsReader(deferredEvents));
        while (eventReader.hasNext()) {
            parseContent(
                    processTranslatableAttributes(stripRevisionAttributes(eventReader.nextEvent())),
                    eventReader
            );
        }
    }

    private Deque<XMLEvent> subtractAllFromLastOpeningRun(final Deque<XMLEvent> deferredEvents) {
        final Deque<XMLEvent> subtraction = new LinkedList<>();
        final Iterator<XMLEvent> deferredEventsIterator = deferredEvents.descendingIterator();

        while (deferredEventsIterator.hasNext()) {
            final XMLEvent event = deferredEventsIterator.next();
            subtraction.addFirst(event);
            deferredEventsIterator.remove();
            if (isRunStartEvent(event)) {
                break;
            }
        }

        return subtraction;
    }

    private void parseToFirstClosingRun() throws XMLStreamException {
        while (this.startElementContext.getEventReader().hasNext()) {
            final XMLEvent event = this.startElementContext.getEventReader().nextEvent();
            parseContent(
                processTranslatableAttributes(stripRevisionAttributes(event)),
                this.startElementContext.getEventReader()
            );
            if (event.isEndElement()
                    && XMLEventHelpers.LOCAL_RUN.equals(event.asEndElement().getName().getLocalPart())) {
                return;
            }
        }
    }

    private boolean parseComplexFieldCode(final StartElement startElement) throws XMLStreamException {
        this.runBuilder.addToMarkup(startElement);

        boolean extractable = false;

        while (this.startElementContext.getEventReader().hasNext()) {
            final XMLEvent event = this.startElementContext.getEventReader().nextEvent();
            this.runBuilder.addToMarkup(event);

            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                return extractable;
            }

            if (!extractable && event.isCharacters()) {
                extractable = isComplexFieldCodeExtractable(event.asCharacters());
            }
        }

        throw new IllegalStateException(UNEXPECTED_STRUCTURE + startElement);
    }

    private boolean isComplexFieldCodeExtractable(Characters characters) {
        // get the field definition out of the field code string
        String data = characters.getData().trim();
        int fieldCodeNameLength = data.indexOf(" ");

        String fieldCodeName;
        if (fieldCodeNameLength > 0) {
            fieldCodeName = data.substring(0, fieldCodeNameLength);
        } else {
            fieldCodeName = data;
        }

        return startElementContext.getConditionalParameters().tsComplexFieldDefinitionsToExtract.contains(fieldCodeName);
    }

    private XMLEvent stripRevisionAttributes(XMLEvent e) {
        if (isParagraphStartEvent(e)) {
            return wordParagraphRevisions.strip(e.asStartElement());
        } else if (isRunStartEvent(e)) {
            return wordRunRevisions.strip(e.asStartElement());
        } else {
            return e;
        }
    }

    private void parseContent(final XMLEvent e, final XMLEventReader eventReader) throws XMLStreamException {

        if (isTextStartEvent(e)) {

            runBuilder.flushMarkup();
            parseText(e.asStartElement(), eventReader);

        } else if (startElementContext.getConditionalParameters().getAddTabAsCharacter() && isTabStartEvent(e) && !runBuilder.isHidden()) {

            runBuilder.flushMarkup();
            runBuilder.addText("\t", e.asStartElement());
            defaultSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext.getEventReader(), null, startElementContext.getConditionalParameters()));

        } else if (startElementContext.getConditionalParameters().getAddLineSeparatorCharacter() && isLineBreakStartEvent(e) && !isPageBreak(e.asStartElement()) && !runBuilder.isHidden()) {

            runBuilder.flushMarkup();
            char replacement = startElementContext.getConditionalParameters().getLineSeparatorReplacement();
            runBuilder.addText(String.valueOf(replacement), e.asStartElement());
            defaultSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext.getEventReader(), null, startElementContext.getConditionalParameters()));

        } else if (startElementContext.getConditionalParameters().getReplaceNoBreakHyphenTag() && isNoBreakHyphenStartEvent(e)) {

            runBuilder.flushMarkup();
            runBuilder.addText(LOCAL_REGULAR_HYPHEN_VALUE, e.asStartElement());
            defaultSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext.getEventReader(), null, startElementContext.getConditionalParameters()));

        } else if (!isWhitespace(e) || runBuilder.preservingWhitespace()) {

            // Real text should have been handled above.  Most whitespace is ignorable,
            // but if we're in a preserve-whitespace section, we need to save it (eg
            // for w:instrText, which isn't translatable but needs to be preserved).
            runBuilder.flushText();
            runBuilder.setTextPreservingWhitespace(false);

            if (XMLEventHelpers.isParagraphStartEvent(e)) {
                runBuilder.addToMarkup(
                    new MarkupComponent.ParagraphStart(
                        startElementContext.getEventFactory(),
                        styleDefinitions,
                        e.asStartElement()
                    )
                );
            } else if (XMLEventHelpers.isParagraphPropertiesStartEvent(e)) {
                runBuilder.addToMarkup(
                    new MarkupComponentParser().parseParagraphBlockProperties(
                        createStartElementContext(
                            e.asStartElement(),
                            eventReader,
                            startElementContext.getEventFactory(),
                            startElementContext.getConditionalParameters()
                        ),
                        this.drawingRunPropertiesStrippableAttributes,
                        this.blockPropertiesSkippableElements
                    )
                );
            } else if (XMLEventHelpers.isParagraphEndEvent(e)) {
                runBuilder.addToMarkup(
                    MarkupComponentFactory.createEndMarkupComponent(
                        e.asEndElement()
                    )
                );
            } else if (!parseSkippableElements(e)) {
                runBuilder.addToMarkup(e);
            }
        }
    }

    private void parseText(StartElement startEvent, XMLEventReader events) throws XMLStreamException {
        // Merge the preserve whitespace flag
        runBuilder.setTextPreservingWhitespace(runBuilder.isTextPreservingWhitespace() || hasPreserveWhitespace(startEvent));

        while (events.hasNext()) {
            XMLEvent e = events.nextEvent();
            if (isEndElement(e, startEvent)) {
                return;
            } else if (e.isCharacters()) {
                String text = e.asCharacters().getData();
                if (text.trim().length() > 0) {
                    runBuilder.setNonWhitespaceText(true);
                }
                runBuilder.addText(text, startEvent);
            }
        }
    }

    // translatable attributes:
    // wp:docPr/@name  if that option isn't set
    // v:textpath/@string
    private XMLEvent processTranslatableAttributes(XMLEvent e) {
        if (!e.isStartElement()) return e;
        StartElement startEl = e.asStartElement();
        // I will need to
        // - extract translatable attribute
        // - create a new start event with all the attributes except for that one, which is replaced
        if (isGraphicsProperty(startEl) && !startElementContext.getConditionalParameters().getTranslateWordExcludeGraphicMetaData()) {
            startEl = processTranslatableAttribute(startEl, "name");
        } else if (isTextPath(startEl)) {
            startEl = processTranslatableAttribute(startEl, "string");
        }
        return startEl;
    }

    private StartElement processTranslatableAttribute(StartElement startEl, String attrName) {
        List<Attribute> newAttrs = new ArrayList<>();
        Iterator<?> it = startEl.getAttributes();
        boolean dirty = false;
        while (it.hasNext()) {
            Attribute a = (Attribute) it.next();
            if (a.getName().getLocalPart().equals(attrName)) {
                runBuilder.setContainsNestedItems(true);
                runBuilder.getNestedTextualItems().add(new TranslatableAttributeText(a.getValue()));
                newAttrs.add(startElementContext.getEventFactory().createAttribute(a.getName(),
                        TextFragment.makeRefMarker(nestedTextualIds.createId())));
                dirty = true;
            } else {
                newAttrs.add(a);
            }
        }
        return dirty ?
                startElementContext.getEventFactory().createStartElement(startEl.getName(), newAttrs.iterator(), startEl.getNamespaces()) :
                startEl;
    }

    private void log(String s) {
        LOGGER.debug(s);
    }
}
