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
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class StringItem implements XMLEvents {
    private final List<Chunk> chunks;
    private final StyleOptimisation styleOptimisation;
    private final QName runName;
    private final QName textName;

    StringItem(
        final List<Chunk> chunks,
        final StyleOptimisation styleOptimisation,
        final QName runName,
        final QName textName
    ) {
        this.chunks = chunks;
        this.styleOptimisation = styleOptimisation;
        this.runName = runName;
        this.textName = textName;
    }

    void optimiseStyles() throws XMLStreamException {
        this.styleOptimisation.applyTo(chunks);
    }

    /**
     * Return the QName of the element that contains run data in this block.
     */
    QName getRunName() {
        return runName;
    }

    /**
     * Return the QName of the element that contains text data in this block.
     */
    QName getTextName() {
        return textName;
    }

    @Override
    public List<XMLEvent> getEvents() {
        List<XMLEvent> events = new ArrayList<>();
        for (XMLEvents chunk : chunks) {
            events.addAll(chunk.getEvents());
        }
        return events;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    Block getBlock() {
        return new Block(chunks, styleOptimisation, runName, textName, false, false, false, Collections.emptyList());
    }

    boolean isStyled() {
        for (Chunk chunk: chunks) {
            if (chunk instanceof Run) {
                return true;
            }
        }
        return false;
    }

    static class Builder {
        private List<Chunk> chunks = new ArrayList<>();
        private StyleOptimisation styleOptimisation;
        private QName name;
        private QName textName;
        private MarkupBuilder markupBuilder = new MarkupBuilder(new Block.Markup(new Markup.General(new ArrayList<>())));

        void add(final Chunk chunk) {
            flushMarkup();
            this.chunks.add(chunk);
        }

        void styleOptimisation(final StyleOptimisation styleOptimisation) {
            this.styleOptimisation = styleOptimisation;
        }

        void runName(final QName name) {
            this.name = name;
        }

        void textName(final QName textName) {
            this.textName = textName;
        }

        void addToMarkup(final XMLEvent event) {
            this.markupBuilder.add(event);
        }

        void addToMarkup(final MarkupComponent markupComponent) {
            this.markupBuilder.add(markupComponent);
        }

        void flushMarkup() {
            final Block.Markup markup = (Block.Markup) this.markupBuilder.build();
            if (!markup.components().isEmpty()) {
                this.chunks.add(markup);
                this.markupBuilder = new MarkupBuilder(new Block.Markup(new net.sf.okapi.filters.openxml.Markup.General(new ArrayList<>())));
            }
        }

        StringItem build() {
            flushMarkup();
            return new StringItem(this.chunks, this.styleOptimisation, this.name, this.textName);
        }
    }
}
