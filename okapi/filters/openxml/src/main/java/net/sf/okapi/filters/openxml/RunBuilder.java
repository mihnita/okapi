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

import javax.xml.namespace.QName;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_TEXT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.createQName;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.hasPreserveWhitespace;

class RunBuilder {
    /**
     * A message when the run text is absent.
     */
    private static final String RUN_TEXT_IS_ABSENT = "The run text is absent";

    private StartElementContext startElementContext;
    private EndElement endEvent;
    private RunProperties runProperties;
    private RunProperties combinedRunProperties;
    private List<Chunk> runBodyChunks = new ArrayList<>();
    private List<Textual> nestedTextualItems = new ArrayList<>();
    private boolean isHidden = false;

    private QName textName;
    private String runStyle;
    private boolean containsNestedItems = false;
    private boolean containsComplexFields = false;
    private StyleDefinitions styleDefinitions;
    private boolean isTextPreservingWhitespace = false;
    private MarkupBuilder markupBuilder = new MarkupBuilder(new Run.Markup(new Markup.General(new ArrayList<>())));
    private Queue<XMLEvent> deferredEvents = new LinkedList<>();
    private boolean hasAnyText = false;
    private boolean hasNonWhitespaceText = false;
    private StringBuilder textContent = new StringBuilder();

    RunBuilder(StartElementContext startElementContext, StyleDefinitions styleDefinitions) {
        this.startElementContext = startElementContext;
        this.styleDefinitions = styleDefinitions;
    }

    StartElementContext getStartElementContext() {
        return startElementContext;
    }

    void setEndEvent(EndElement endEvent) {
        this.endEvent = endEvent;
    }

    List<Chunk> getRunBodyChunks() {
        return runBodyChunks;
    }

    void setRunBodyChunks(final List<Chunk> chunks) {
        runBodyChunks.clear();
        runBodyChunks.addAll(chunks);
    }

    RunProperties getRunProperties() {
        if (null == this.runProperties) {
            this.runProperties = new RunProperties.Default(
                this.startElementContext.getEventFactory(),
                this.startElementContext.getStartElement().getName().getPrefix(),
                this.startElementContext.getStartElement().getName().getNamespaceURI(),
                RunProperties.RPR
            );
        }
        return runProperties;
    }

    void setRunProperties(RunProperties runProperties) {
        this.runProperties = runProperties;
    }

    RunProperties getCombinedRunProperties(String paragraphStyle) {
        if (null == combinedRunProperties) {
            resetCombinedRunProperties(paragraphStyle);
        }

        return combinedRunProperties;
    }

    void resetCombinedRunProperties(String paragraphStyle) {
        combinedRunProperties = styleDefinitions.combinedRunProperties(
            paragraphStyle,
            runStyle,
            getRunProperties()
        );
    }

    List<Textual> getNestedTextualItems() {
        return nestedTextualItems;
    }

    void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isHidden() {
        return isHidden;
    }

    QName getTextName() {
        return textName;
    }

    String getRunStyle() {
        return runStyle;
    }

    void setRunStyle(String runStyle) {
        this.runStyle = runStyle;
    }

    boolean containsNestedItems() {
        return containsNestedItems;
    }

    void setContainsNestedItems(boolean containsNestedItems) {
        this.containsNestedItems = containsNestedItems;
    }

    boolean containsComplexFields() {
        return containsComplexFields;
    }

    void setContainsComplexFields(boolean containsComplexFields) {
        this.containsComplexFields = containsComplexFields;
    }

    StyleDefinitions getStyleDefinitions() {
        return styleDefinitions;
    }

    boolean isTextPreservingWhitespace() {
        return isTextPreservingWhitespace;
    }

    void setTextPreservingWhitespace(boolean textPreservingWhitespace) {
        isTextPreservingWhitespace = textPreservingWhitespace;
    }

    /**
     * Handle cases like <w:instrText> -- non-text elements that we may need
     * to obey xml:space="preserve" on.
     */
    boolean preservingWhitespace() {
        // Look only at the most recent element on the stack
        final XMLEvent e = this.markupBuilder.peekCurrentMarkupComponentEvent();
        if (e instanceof StartElement && hasPreserveWhitespace(e.asStartElement())) {
            return true;
        }
        return false;
    }

    boolean hasNonWhitespaceText() {
        return hasNonWhitespaceText;
    }

    void setNonWhitespaceText(boolean hasNonWhitespaceText) {
        this.hasNonWhitespaceText = hasNonWhitespaceText;
    }

    /**
     * Provides the first {@link net.sf.okapi.filters.openxml.Run.RunText}
     * found in thr run body chunks.
     *
     * @return The run text or {@code null} if the run text has not been found
     */
    Run.RunText firstRunText() {
        final ListIterator<Chunk> iterator = chunkIteratorForFirstRunText();
        return null == iterator
            ? null
            : (Run.RunText) iterator.previous();
    }

    /**
     * Provides a chunk iterator for the first {@link net.sf.okapi.filters.openxml.Run.RunText}.
     *
     * @return The chunk iterator or {@code null} if the run text has not been found
     */
    private ListIterator<Chunk> chunkIteratorForFirstRunText() {
        final ListIterator<Chunk> iterator = getRunBodyChunks().listIterator();
        while (iterator.hasNext()) {
            final Chunk chunk = iterator.next();
            if (chunk instanceof Run.RunText) {
                return iterator;
            }
        }
        return null;
    }

    /**
     * Adds text to the first {@link Run.RunText} in the run builder.
     *
     * Recreates characters with appending to the end of the fist found
     * in the run text.
     *
     * @param text The text to add
     */
    void addToFirstRunText(String text) {
        final ListIterator<Chunk> iterator = chunkIteratorForFirstRunText();

        if (iterator == null) {
            throw new IllegalStateException(RUN_TEXT_IS_ABSENT);
        }

        final Run.RunText runText = (Run.RunText) iterator.previous();
        iterator.set(
            new Run.RunText(
                runText.startElement(),
                startElementContext.getEventFactory().createCharacters(runText.characters().getData().concat(text)),
                runText.endElement()
            )
        );
    }

    void addText(String text, StartElement startElement) {
        hasAnyText = true;
        textContent.append(text);

        if (textName == null) {
            textName = createQName(LOCAL_TEXT, startElement.getName());
        }
    }

    void flushText() {
        // It seems like there may be a bug where presml runs need to have
        // an empty <a:t/> at a minimum.
        if (hasAnyText) {
            runBodyChunks.add(new Run.RunText(createRunTextStartElement(),
                    startElementContext.getEventFactory().createCharacters(textContent.toString()),
                    startElementContext.getEventFactory().createEndElement(textName, null)));
            textContent.setLength(0);
            hasAnyText = false;
        }
    }

    private StartElement createRunTextStartElement() {
        return startElementContext.getEventFactory().createStartElement(textName,
                // DrawingML <a:t> does not use the xml:space="preserve" attribute
                isTextPreservingWhitespace && !Namespaces.DrawingML.containsName(textName) ?
                        java.util.Collections.singleton(
                                startElementContext.getEventFactory().createAttribute("xml", Namespaces.XML.getURI(), "space", "preserve"))
                                .iterator() : null,
                null);
    }

    void addToMarkup(final XMLEvent event) {
        this.markupBuilder.add(event);
    }

    void addToMarkup(final MarkupComponent markupComponent) {
        this.markupBuilder.add(markupComponent);
    }

    void flushMarkup() {
        final Run.Markup markup = (Run.Markup) this.markupBuilder.build();
        if (!markup.components().isEmpty()) {
            this.runBodyChunks.add(markup);
            this.markupBuilder = new MarkupBuilder(new Run.Markup(new Markup.General(new ArrayList<>())));
        }
    }

    boolean containsMergeableMarkup() {
        return runBodyChunks.stream()
            .filter(c -> c instanceof Markup)
            .map(m -> ((Markup) m).components())
            .flatMap(mcl -> mcl.stream())
            .filter(mc -> mc instanceof ParagraphBlockProperties)
            .anyMatch(p -> ((ParagraphBlockProperties) p).containsRunPropertyDeletedParagraphMark());
    }

    void addDeferredEvents(final Queue<XMLEvent> events) {
        this.deferredEvents.addAll(events);
    }

    Queue<XMLEvent> deferredEvents() {
        return this.deferredEvents;
    }

    Chunk build() {
        return new Run(
            startElementContext.getStartElement(),
            endEvent,
            getRunProperties(),
            combinedRunProperties,
            runBodyChunks,
            nestedTextualItems,
            isHidden
        );
    }

    @Override
    public String toString() {
        return "RunBuilder for " + build().toString();
    }
}
