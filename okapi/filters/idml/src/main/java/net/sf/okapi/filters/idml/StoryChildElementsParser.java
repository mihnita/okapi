/*
 * =============================================================================
 *   Copyright (C) 2010-2017 by the Okapi Framework contributors
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

import net.sf.okapi.filters.idml.ParsingIdioms.StyledStoryChildElement;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.sf.okapi.filters.idml.ParsingIdioms.UNEXPECTED_STRUCTURE;
import static net.sf.okapi.filters.idml.StyleRange.getDefaultCharacterStyleRange;
import static net.sf.okapi.filters.idml.StyleRange.getDefaultParagraphStyleRange;

class StoryChildElementsParser {

    private static final QName CHANGE_TYPE = Namespaces.getDefaultNamespace().getQName("ChangeType");
    private static final String DELETED_TEXT = "DeletedText";

    private static final QName ROW = Namespaces.getDefaultNamespace().getQName("Row");
    private static final QName COLUMN = Namespaces.getDefaultNamespace().getQName("Column");
    private static final QName CELL = Namespaces.getDefaultNamespace().getQName("Cell");

    private static final EnumSet<StyledStoryChildElement> PARAGRAPH_STYLE_RANGE_STYLED_ELEMENTS = EnumSet.complementOf(
            EnumSet.of(StyledStoryChildElement.UNSUPPORTED, StyledStoryChildElement.PARAGRAPH_STYLE_RANGE));

    private final StartElement startElement;
    private final Parameters parameters;
    private final XMLEventFactory eventFactory;
    private final XMLEventReader eventReader;

    private StyleRange currentParagraphStyleRange;
    private StyleRange currentCharacterStyleRange;

    StoryChildElementsParser(StartElement startElement, Parameters parameters, XMLEventFactory eventFactory, XMLEventReader eventReader) {
        this.startElement = startElement;
        this.parameters = parameters;
        this.eventFactory = eventFactory;
        this.eventReader = eventReader;
    }

    List<StoryChildElement> parse() throws XMLStreamException {

        if (!StyledStoryChildElement.PARAGRAPH_STYLE_RANGE.getName().equals(startElement.getName())) {

            if (PARAGRAPH_STYLE_RANGE_STYLED_ELEMENTS.contains(StyledStoryChildElement.fromName(startElement.getName()))) {
                return parseAsFromParagraphStyleRange(startElement);
            }

            return parseFromUnstyledRange(startElement);
        }

        return parseFromParagraphStyleRange();
    }

    private List<StoryChildElement> parseWithStyleRanges(StyleRanges styleRanges) throws XMLStreamException {
        currentParagraphStyleRange = styleRanges.getParagraphStyleRange();
        currentCharacterStyleRange = styleRanges.getCharacterStyleRange();

        return parse();
    }

    private List<StoryChildElement> parseFromUnstyledRange(StartElement startElement) throws XMLStreamException {
        StoryChildElement.Builder storyChildElementBuilder = new StoryChildElement.Builder();

        return singletonList(new StoryChildElementParser(startElement, eventReader, eventFactory).parse(storyChildElementBuilder));
    }

    private List<StoryChildElement> parseAsFromParagraphStyleRange(StartElement startElement) throws XMLStreamException {

        StyleRange paragraphStyleRange = null == currentParagraphStyleRange
                ? getDefaultParagraphStyleRange(eventFactory)
                : currentParagraphStyleRange;

        if (!StyledStoryChildElement.CHARACTER_STYLE_RANGE.getName().equals(startElement.getName())) {
            return parseAsFromCharacterStyleRange(startElement, paragraphStyleRange);
        }

        return parseFromCharacterStyleRange(startElement, paragraphStyleRange, null);
    }

    private List<StoryChildElement> parseFromParagraphStyleRange() throws XMLStreamException {
        List<StoryChildElement> storyChildElements = new ArrayList<>();

        StyleRange paragraphStyleRange = new StyleRangeParser(startElement, eventReader, eventFactory).parse();
        currentParagraphStyleRange = paragraphStyleRange;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextTag();

            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                return storyChildElements;
            }

            if (!event.isStartElement()) {
                throw new IllegalStateException(UNEXPECTED_STRUCTURE);
            }

            if (!StyledStoryChildElement.CHARACTER_STYLE_RANGE.getName().equals(event.asStartElement().getName())) {
                storyChildElements.addAll(parseAsFromCharacterStyleRange(event.asStartElement(), paragraphStyleRange));
                continue;
            }

            storyChildElements.addAll(parseFromCharacterStyleRange(event.asStartElement(), paragraphStyleRange, null));
        }

        throw new IllegalStateException(UNEXPECTED_STRUCTURE);
    }

    private List<StoryChildElement> parseAsFromCharacterStyleRange(StartElement startElement, StyleRange paragraphStyleRange) throws XMLStreamException {

        StyleRange characterStyleRange = null == currentCharacterStyleRange
                ? getDefaultCharacterStyleRange(eventFactory)
                : currentCharacterStyleRange;

        StyleRanges styleRanges = new StyleRanges(paragraphStyleRange, characterStyleRange);

        if (parameters.getUntagXmlStructures()) {
            if (StyledStoryChildElement.XML_ATTRIBUTE.getName().equals(startElement.getName())
                    || StyledStoryChildElement.XML_COMMENT.getName().equals(startElement.getName())
                    || StyledStoryChildElement.XML_INSTRUCTION.getName().equals(startElement.getName())) {

                skipRange(startElement);

                return emptyList();
            }

            if (StyledStoryChildElement.XML_ELEMENT.getName().equals(startElement.getName())) {
                return parseFromElementRange(startElement, styleRanges);
            }
        }

        if (StyledStoryChildElement.HYPERLINK_TEXT_SOURCE.getName().equals(startElement.getName())) {
            return singletonList(parseHyperlinkTextSource(startElement, styleRanges));
        }

        if (StyledStoryChildElement.FOOTNOTE.getName().equals(startElement.getName())) {
            return singletonList(parseFootnote(startElement, styleRanges));
        }

        if (StyledStoryChildElement.NOTE.getName().equals(startElement.getName())) {
            return singletonList(parseNote(startElement, styleRanges));
        }

        if (StyledStoryChildElement.TABLE.getName().equals(startElement.getName())) {
            return singletonList(parseTable(startElement, styleRanges));
        }

        if (StyledStoryChildElement.CHANGE.getName().equals(startElement.getName())) {
            return parseFromChangedRange(startElement, styleRanges);
        }

        if (StyledStoryChildElement.CONTENT.getName().equals(startElement.getName())) {
            return singletonList(parseContent(startElement, styleRanges));
        }

        if (StyledStoryChildElement.BREAK.getName().equals(startElement.getName())) {
            return singletonList(parseBreak(startElement, styleRanges));
        }

        return singletonList(parseFromStyledRange(startElement, styleRanges));
    }

    private List<StoryChildElement> parseFromCharacterStyleRange(
        StartElement startElement,
        StyleRange paragraphStyleRange,
        StyleRange defaultCharacterStyleRange
    ) throws XMLStreamException {
        StyleRange characterStyleRange = null == defaultCharacterStyleRange
                ? new StyleRangeParser(startElement, eventReader, eventFactory).parse()
                : defaultCharacterStyleRange;

        currentCharacterStyleRange = characterStyleRange;

        StyleRanges styleRanges = new StyleRanges(paragraphStyleRange, characterStyleRange);

        List<StoryChildElement> storyChildElements = new ArrayList<>();

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextTag();

            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                return storyChildElements;
            }

            if (!event.isStartElement()) {
                throw new IllegalStateException(UNEXPECTED_STRUCTURE);
            }

            if (parameters.getUntagXmlStructures()) {
                if (StyledStoryChildElement.XML_ATTRIBUTE.getName().equals(event.asStartElement().getName())
                        || StyledStoryChildElement.XML_COMMENT.getName().equals(event.asStartElement().getName())
                        || StyledStoryChildElement.XML_INSTRUCTION.getName().equals(event.asStartElement().getName())) {

                    skipRange(event.asStartElement());
                    continue;
                }

                if (StyledStoryChildElement.XML_ELEMENT.getName().equals(event.asStartElement().getName())) {
                    storyChildElements.addAll(parseFromElementRange(event.asStartElement(), styleRanges));
                    continue;
                }
            }

            if (StyledStoryChildElement.HYPERLINK_TEXT_SOURCE.getName().equals(event.asStartElement().getName())) {
                storyChildElements.add(parseHyperlinkTextSource(event.asStartElement(), styleRanges));
                continue;
            }

            if (StyledStoryChildElement.FOOTNOTE.getName().equals(event.asStartElement().getName())) {
                storyChildElements.add(parseFootnote(event.asStartElement(), styleRanges));
                continue;
            }

            if (StyledStoryChildElement.NOTE.getName().equals(event.asStartElement().getName())) {
                storyChildElements.add(parseNote(event.asStartElement(), styleRanges));
                continue;
            }

            if (StyledStoryChildElement.TABLE.getName().equals(event.asStartElement().getName())) {
                storyChildElements.add(parseTable(event.asStartElement(), styleRanges));
                continue;
            }

            if (StyledStoryChildElement.CHANGE.getName().equals(event.asStartElement().getName())) {
                storyChildElements.addAll(parseFromChangedRange(event.asStartElement(), styleRanges));
                continue;
            }

            if (StyledStoryChildElement.CONTENT.getName().equals(event.asStartElement().getName())) {
                storyChildElements.add(parseContent(event.asStartElement(), styleRanges));
                continue;
            }

            if (StyledStoryChildElement.BREAK.getName().equals(event.asStartElement().getName())) {
                storyChildElements.add(parseBreak(event.asStartElement(), styleRanges));
                continue;
            }

            storyChildElements.add(parseFromStyledRange(event.asStartElement(), styleRanges));
        }

        return storyChildElements;
    }

    private List<StoryChildElement> parseFromElementRange(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        List<StoryChildElement> storyChildElements = new ArrayList<>();

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextTag();

            if (event.isEndElement() && startElement.getName().equals(event.asEndElement().getName())) {
                break;
            }

            if (!event.isStartElement()) {
                throw new IllegalStateException(UNEXPECTED_STRUCTURE);
            }

            storyChildElements.addAll(new StoryChildElementsParser(event.asStartElement(), parameters, eventFactory, eventReader).parseWithStyleRanges(styleRanges));
        }

        return new StoryChildElementsMerger(parameters, eventFactory).merge(storyChildElements);
    }

    private StoryChildElement parseHyperlinkTextSource(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        StoryChildElement.StyledTextReferenceElement.HyperlinkTextSource.HyperlinkTextSourceBuilder hyperlinkTextSourceBuilder = new StoryChildElement.StyledTextReferenceElement.HyperlinkTextSource.HyperlinkTextSourceBuilder();

        return new StyledTextReferenceElementParser(startElement, styleRanges, parameters, eventFactory, eventReader).parse(hyperlinkTextSourceBuilder);
    }

    private StoryChildElement parseFootnote(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        StoryChildElement.StyledTextReferenceElement.Footnote.FootnoteBuilder footnoteBuilder = new StoryChildElement.StyledTextReferenceElement.Footnote.FootnoteBuilder();

        return new StyledTextReferenceElementParser(startElement, styleRanges, parameters, eventFactory, eventReader).parse(footnoteBuilder);
    }

    private StoryChildElement parseNote(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        StoryChildElement.StyledTextReferenceElement.Note.NoteBuilder noteBuilder = new StoryChildElement.StyledTextReferenceElement.Note.NoteBuilder();

        return new StyledTextReferenceElementParser(startElement, styleRanges, parameters, eventFactory, eventReader).parse(noteBuilder);
    }

    private StoryChildElement parseTable(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        return new TableParser(startElement, styleRanges, parameters, eventFactory, eventReader).parse();
    }

    private List<StoryChildElement> parseFromChangedRange(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        String changeTypeValue = startElement.getAttributeByName(CHANGE_TYPE).getValue();

        if (DELETED_TEXT.equals(changeTypeValue)) {
            skipRange(startElement);
            return emptyList();
        }

        return acceptChanges(startElement, styleRanges);
    }

    private void skipRange(StartElement startElement) throws XMLStreamException {
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                return;
            }
        }

        throw new IllegalStateException(UNEXPECTED_STRUCTURE);
    }

    private List<StoryChildElement> acceptChanges(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        return parseFromElementRange(startElement, styleRanges);
    }

    private StoryChildElement parseContent(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        StoryChildElement.StyledTextElement.Content.ContentBuilder contentBuilder = new StoryChildElement.StyledTextElement.Content.ContentBuilder();
        contentBuilder.setStyleRanges(styleRanges);

        return new StoryChildElementParser(startElement, eventReader, eventFactory).parse(contentBuilder);
    }

    private StoryChildElement parseBreak(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        StoryChildElement.StyledTextElement.Break.BreakBuilder breakBuilder = new StoryChildElement.StyledTextElement.Break.BreakBuilder();
        breakBuilder.setStyleRanges(styleRanges);

        return new StoryChildElementParser(startElement, eventReader, eventFactory).parse(breakBuilder);
    }

    private StoryChildElement parseFromStyledRange(StartElement startElement, StyleRanges styleRanges) throws XMLStreamException {
        StoryChildElement.StyledTextElement.StyledTextElementBuilder styledTextElementBuilder = new StoryChildElement.StyledTextElement.StyledTextElementBuilder();
        styledTextElementBuilder.setStyleRanges(styleRanges);

        return new StoryChildElementParser(startElement, eventReader, eventFactory).parse(styledTextElementBuilder);
    }

    private static class StoryChildElementParser extends ElementParser {

        StoryChildElementParser(StartElement startElement, XMLEventReader eventReader, XMLEventFactory eventFactory) {
            super(startElement, eventReader, eventFactory);
        }

        StoryChildElement parse(StoryChildElement.Builder storyChildElementBuilder) throws XMLStreamException {
            return (StoryChildElement) super.parse(storyChildElementBuilder);
        }
    }

    private static class StyledTextReferenceElementParser {

        private final StartElement startElement;
        private final StyleRanges styleRanges;
        private final Parameters parameters;
        private final XMLEventFactory eventFactory;
        private final XMLEventReader eventReader;

        private StyledTextReferenceElementParser(StartElement startElement, StyleRanges styleRanges, Parameters parameters, XMLEventFactory eventFactory, XMLEventReader eventReader) {
            this.startElement = startElement;
            this.styleRanges = styleRanges;
            this.parameters = parameters;
            this.eventFactory = eventFactory;
            this.eventReader = eventReader;
        }

        StoryChildElement.StyledTextReferenceElement parse(StoryChildElement.StyledTextReferenceElement.StyledTextReferenceElementBuilder styledTextReferenceElementBuilder) throws XMLStreamException {

            final List<StoryChildElement> storyChildElements = new ArrayList<>();

            styledTextReferenceElementBuilder.setStyleRanges(styleRanges);
            styledTextReferenceElementBuilder.setStartElement(startElement);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextTag();

                if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                    final StyleRangeEventsGenerator styleRangeEventsGenerator = new StyleRangeEventsGenerator(eventFactory);
                    final StoryChildElementsMerger storyChildElementsMerger = new StoryChildElementsMerger(parameters, eventFactory);

                    styledTextReferenceElementBuilder.setStyleRangeEventsGenerator(styleRangeEventsGenerator)
                            .setStoryChildElementsWriter(new StoryChildElementsWriter(styleRangeEventsGenerator))
                            .addStoryChildElements(storyChildElementsMerger.merge(storyChildElements))
                            .setEndElement(event.asEndElement());

                    return styledTextReferenceElementBuilder.build();
                }

                if (!event.isStartElement()) {
                    throw new IllegalStateException(UNEXPECTED_STRUCTURE);
                }

                if (Properties.NAME.equals(event.asStartElement().getName())) {
                    styledTextReferenceElementBuilder.setProperties(
                        new PropertiesParser(event.asStartElement(), eventReader, eventFactory).parse()
                    );
                    continue;
                }

                storyChildElements.addAll(new StoryChildElementsParser(event.asStartElement(), parameters, eventFactory, eventReader).parse());
            }

            throw new IllegalStateException(UNEXPECTED_STRUCTURE);
        }
    }

    private static class TableParser {

        private final StartElement startElement;
        private final StyleRanges styleRanges;
        private final Parameters parameters;
        private final XMLEventFactory eventFactory;
        private final XMLEventReader eventReader;

        private TableParser(StartElement startElement, StyleRanges styleRanges, Parameters parameters, XMLEventFactory eventFactory, XMLEventReader eventReader) {
            this.startElement = startElement;
            this.styleRanges = styleRanges;
            this.parameters = parameters;
            this.eventFactory = eventFactory;
            this.eventReader = eventReader;
        }

        StoryChildElement.StyledTextReferenceElement.Table parse() throws XMLStreamException {
            StoryChildElement.StyledTextReferenceElement.Table.TableBuilder tableBuilder = new StoryChildElement.StyledTextReferenceElement.Table.TableBuilder();

            tableBuilder.setStyleRanges(styleRanges);
            tableBuilder.setStartElement(startElement);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextTag();

                if (event.isEndElement() && event.asEndElement().getName().equals(startElement.getName())) {
                    tableBuilder.setEndElement(event.asEndElement());

                    return tableBuilder.build();
                }

                if (!event.isStartElement()) {
                    throw new IllegalStateException(UNEXPECTED_STRUCTURE);
                }

                if (Properties.NAME.equals(event.asStartElement().getName())) {
                    tableBuilder.setProperties(
                        new PropertiesParser(event.asStartElement(), eventReader, eventFactory).parse()
                    );
                    continue;
                }

                if (ROW.equals(event.asStartElement().getName()) || COLUMN.equals(event.asStartElement().getName())) {
                    tableBuilder.addMarkupRangeElement(
                        new ElementParser(event.asStartElement(), eventReader, eventFactory).parse(new Element.Builder())
                    );
                    continue;
                }

                if (!CELL.equals(event.asStartElement().getName())) {
                    throw new IllegalStateException(UNEXPECTED_STRUCTURE);
                }

                StoryChildElement.StyledTextReferenceElement.Table.Cell.CellBuilder cellBuilder = new StoryChildElement.StyledTextReferenceElement.Table.Cell.CellBuilder();
                tableBuilder.addCell((StoryChildElement.StyledTextReferenceElement.Table.Cell) new StyledTextReferenceElementParser(event.asStartElement(), null, parameters, eventFactory, eventReader).parse(cellBuilder));
            }

            throw new IllegalStateException(UNEXPECTED_STRUCTURE);
        }
    }
}
