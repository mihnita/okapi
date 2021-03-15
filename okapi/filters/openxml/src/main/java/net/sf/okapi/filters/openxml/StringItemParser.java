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

import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createEndMarkupComponent;
import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createStartMarkupComponent;
import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isRunStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isStringItemEndEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isTextStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isWhitespace;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.sf.okapi.common.IdGenerator;

final class StringItemParser implements Parser<StringItem> {
    private final StartElementContext startElementContext;
    private final IdGenerator nestedBlockIdGenerator;
    private final StyleDefinitions styleDefinitions;
    private final StyleOptimisation styleOptimisation;
    private final RunBuilderSkipper runBuilderSkipper;

    private StringItem.Builder builder;
    private SkippableElements phoneticRunAndPropertySkippableElements;

    StringItemParser(
        final StartElementContext startElementContext,
        final IdGenerator nestedBlockIdGenerator,
        final StyleDefinitions styleDefinitions,
        final StyleOptimisation styleOptimisation
    ) {
        this.startElementContext = startElementContext;
        this.nestedBlockIdGenerator = nestedBlockIdGenerator;
        this.styleDefinitions = styleDefinitions;
        this.styleOptimisation = styleOptimisation;

        builder = new StringItem.Builder();
        runBuilderSkipper =  new RunBuilderSkipper();

        phoneticRunAndPropertySkippableElements = new SkippableElements.Inline(
            new SkippableElements.Default(
                SkippableElement.PhoneticInline.PHONETIC_RUN,
                SkippableElement.PhoneticInline.PHONETIC_PROPERTY
            )
        );
    }

    @Override
    public StringItem parse() throws XMLStreamException {
        builder.addToMarkup(
            new MarkupComponent.ParagraphStart(
                startElementContext.getEventFactory(),
                styleDefinitions,
                startElementContext.getStartElement()
            )
        );
        RunMerger runMerger = new RunMerger();
        XMLEvent e;
        do  {
            e = startElementContext.getEventReader().nextEvent();
            if (isRunStartEvent(e)) {
                processRun(builder, runMerger, e.asStartElement());
            } else if (isTextStartEvent(e)) {
                addRunsToBuilder(builder, runMerger);
                processText(e.asStartElement(), builder);
            } else {
                if (e.isStartElement() && phoneticRunAndPropertySkippableElements.canBeSkipped(e.asStartElement(), startElementContext.getStartElement())) {
                    phoneticRunAndPropertySkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext));
                    continue;
                }

                if (!isWhitespace(e)) {
                    // Flush any outstanding run if there's any markup
                    addRunsToBuilder(builder, runMerger);

                    // Check for end of block
                    if (e.isEndElement() && startElementContext.getStartElement().getName().equals(e.asEndElement().getName())) {
                        builder.addToMarkup(createEndMarkupComponent(e.asEndElement()));
                        builder.styleOptimisation(styleOptimisation);
                        return builder.build();
                    } else {
                        builder.addToMarkup(e);
                    }
                }
            }

        } while (startElementContext.getEventReader().hasNext() && !isStringItemEndEvent(e));
        throw new IllegalStateException("Invalid content? Unterminated string item");
    }

    private void processRun(StringItem.Builder builder, RunMerger runMerger, StartElement startEl) throws XMLStreamException {
        StartElementContext runElementContext = createStartElementContext(startEl, startElementContext);
        RunBuilder runBuilder = new RunParser(runElementContext, nestedBlockIdGenerator, styleDefinitions, styleOptimisation,null, false).parse();

        if (runBuilderSkipper.canSkip(runBuilder)) {
            return;
        }

        builder.runName(startEl.getName());
        builder.textName(runBuilder.getTextName());

        runMerger.add(runBuilder);
    }

    private void processText(StartElement startElement, StringItem.Builder builder) throws XMLStreamException {
        XMLEvent event = startElementContext.getEventReader().nextEvent();
        Characters characters;
        EndElement endElement;

        if (event.isEndElement()) {
            characters = startElementContext.getEventFactory().createCharacters("");
            endElement = event.asEndElement();
        } else {
            characters = event.asCharacters();
            endElement = startElementContext.getEventReader().nextEvent().asEndElement();
        }

        Text text = new Text(startElement, characters, endElement);
        builder.add(text);
    }

    private void addRunsToBuilder(StringItem.Builder builder, RunMerger runMerger) throws XMLStreamException {
        for (Chunk chunk : runMerger.getRuns()) {
            builder.add(chunk);
        }
        runMerger.reset();
    }
}
